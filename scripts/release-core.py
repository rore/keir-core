"""Build the Keir Core GitHub Release assets from a pinned Git commit."""

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import tarfile
import tempfile
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

from release_maven import maven_command


SCRIPTS = Path(__file__).resolve().parent
REPO = SCRIPTS.parent
NOTICES = SCRIPTS / "release-core-notices"
MAVEN_POMS = ("pom.xml", "compiler-java/pom.xml", "runtime-java/pom.xml", "examples/java-host/pom.xml")
WRAPPER_PROPERTIES = ".mvn/wrapper/maven-wrapper.properties"
MAVEN_PACKAGING_FILES = {*MAVEN_POMS, WRAPPER_PROPERTIES, "mvnw", "mvnw.cmd"}


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def run(command, cwd, **kwargs):
    return subprocess.run(command, cwd=cwd, check=True, **kwargs)


def stage_source(revision, version, scratch):
    # Core 0.2.0 used CRLF conversion for Git archive text files.
    git = ["git", "-c", "core.autocrlf=true", "--no-replace-objects"]
    if run([*git, "cat-file", "-t", revision], REPO, capture_output=True).stdout.strip() != b"commit":
        raise ValueError("Release revision must identify a commit")
    source = scratch / "source"
    archive_path = scratch / "source.tar"
    run([*git, "archive", "--format=tar", f"--output={archive_path}", revision], REPO)
    with tarfile.open(archive_path) as archive:
        archive.extractall(source, filter="data")
    if os.name != "nt":
        wrapper = source / "mvnw"
        wrapper.write_bytes(wrapper.read_bytes().replace(b"\r\n", b"\n"))

    compiler = source / "compiler-java/src/main/java/dev/keir/compiler/KeirCompiler.java"
    if f'public static final String IDENTITY = "keir-compiler-java/{version}";' not in compiler.read_text(encoding="utf-8"):
        raise ValueError(f"Compiler identity does not match release version {version}")
    target = f"<version>{version}</version>".encode()
    snapshot = b"<version>0.1.0-SNAPSHOT</version>"
    for name in MAVEN_POMS:
        pom = source / name
        data = pom.read_bytes()
        if data.count(snapshot) == 1 and target not in data:
            pom.write_bytes(data.replace(snapshot, target, 1))
        elif snapshot in data or data.count(target) != 1:
            raise ValueError(f"Unexpected Maven version in {name}")
    return source


def verify_maven_inputs(source, version, reviewed):
    # A dependency tree alone misses Shade configuration such as extraJars.
    # Review packaging changes and update this lock before building a release.
    wrapper_files = {p.relative_to(source).as_posix() for p in (source / ".mvn").rglob("*") if p.is_file()}
    if wrapper_files != {WRAPPER_PROPERTIES} or set(reviewed) != MAVEN_PACKAGING_FILES:
        raise ValueError("Maven packaging inputs differ from the reviewed file set")
    for name, expected in reviewed.items():
        data = (source / name).read_bytes().replace(b"\r\n", b"\n")
        if name.endswith(".xml"):
            data = data.replace(f"<version>{version}</version>".encode(), b"<version>RELEASE</version>", 1)
        if hashlib.sha256(data).hexdigest() != expected:
            raise ValueError(f"Unreviewed Maven packaging input: {name}")


def verified_dependencies(tree, maven_repo, lock):
    coordinates = []
    for line in tree.read_text(encoding="utf-8").splitlines()[1:]:
        match = re.fullmatch(r"[ |]*[+\\]- ([\w.\-:]+)", line)
        if not match:
            raise ValueError(f"Unexpected Maven dependency tree entry: {line}")
        fields = match[1].split(":")
        if fields[-1] in ("test", "provided"):
            continue
        if len(fields) != 5 or fields[2] != "jar" or fields[-1] not in ("compile", "runtime"):
            raise ValueError(f"Unsupported bundled dependency: {match[1]}")
        group, module, _, version, _ = fields
        coordinates.append(f"{group}:{module}:{version}")
    if coordinates != list(lock):
        raise ValueError("Resolved compiler dependencies differ from the reviewed notice inventory")

    verified = {}
    for coordinate, expected in lock.items():
        group, module, version = coordinate.split(":")
        path = maven_repo.joinpath(*group.split("."), module, version, f"{module}-{version}.jar")
        if digest(path) != expected:
            raise ValueError(f"Unreviewed compiler dependency bytes: {coordinate}")
        verified[coordinate] = path
    return verified


def write_zip_entry(archive, name, data):
    if ("\\" in name or ":" in name or "\x00" in name or any(part in ("", ".", "..") for part in name.split("/"))
            or name in archive.namelist()):
        raise ValueError(f"Unsafe or duplicate release ZIP member: {name!r}")
    # This timestamp and file mode reproduce the published Core 0.2.0 ZIP format.
    entry = ZipInfo(name, date_time=(2026, 11, 5, 0, 0, 0))
    entry.create_system = 3
    entry.external_attr = 0o100644 << 16
    entry.compress_type = ZIP_DEFLATED
    archive.writestr(entry, data)


def package_java(source, output, version, dependencies, lock, license_bytes):
    cli = f"keir-compiler-java-{version}-cli.jar"
    compiler = source / "compiler-java/target" / cli
    cli_zip = output / f"keir-compiler-cli-{version}.zip"
    inventory = ["Bundled compiler dependencies", ""]
    inventory.extend(f"{coordinate}  sha256:{value}" for coordinate, value in lock["dependencies"].items())
    inventory.extend(["", "Exact upstream redistribution texts:"])
    inventory.extend(f"{name}: {info['url']}" for name, info in lock["upstream"].items())
    inventory.extend(["", "Other Apache-licensed components are covered by the included Apache 2.0 license. "
                      "The dependency-specific copies and notices included in their original JARs are also provided here.",
                      "The listenablefuture placeholder contains no compiled classes.", ""])
    with ZipFile(cli_zip, "w") as archive:
        write_zip_entry(archive, cli, compiler.read_bytes())
        write_zip_entry(archive, "LICENSE", license_bytes)
        write_zip_entry(archive, "THIRD-PARTY.txt", "\n".join(inventory).encode())
        for name, info in lock["upstream"].items():
            notice = NOTICES / name
            if digest(notice) != info["sha256"]:
                raise ValueError(f"Unreviewed notice bytes: {name}")
            write_zip_entry(archive, f"third-party/{name}", notice.read_bytes())
        for coordinate, path in dependencies.items():
            with ZipFile(path) as dependency:
                for name in dependency.namelist():
                    member = PurePosixPath(name)
                    if (not name.endswith("/") and member.suffix in ("", ".txt", ".md")
                            and re.search(r"license|notice|copying|third.party", member.name, re.I)):
                        prefix = coordinate.replace(":", "/")
                        write_zip_entry(archive, f"third-party/{prefix}/{name}", dependency.read(name))

    maven_zip = output / f"keir-java-runtime-maven-{version}.zip"
    jar = f"keir-runtime-java-{version}.jar"
    with ZipFile(maven_zip, "w") as archive:
        write_zip_entry(archive, "LICENSE", license_bytes)
        for module, file in (("keir-parent", "pom.xml"), ("keir-runtime-java", "runtime-java/pom.xml")):
            name = f"maven/dev/keir/{module}/{version}/{module}-{version}.pom"
            write_zip_entry(archive, name, (source / file).read_bytes())
        name = f"maven/dev/keir/keir-runtime-java/{version}/{jar}"
        write_zip_entry(archive, name, (source / "runtime-java/target" / jar).read_bytes())
    return cli_zip, maven_zip


def package_node(source, output, version, npm_cache, offline, license_bytes):
    package = source / "runtime-node"
    path = package / "package.json"
    manifest = json.loads(path.read_bytes())
    if (manifest.get("name") != "@keir/runtime-node" or manifest.get("version") != version
            or manifest.pop("private", None) is not True):
        raise ValueError("Node source manifest does not match the release version or package")
    manifest["license"] = "Apache-2.0"
    path.write_bytes((json.dumps(manifest, indent=2) + "\n").encode())
    (package / "LICENSE").write_bytes(license_bytes)
    npm = shutil.which("npm.cmd" if os.name == "nt" else "npm")
    if npm is None:
        raise ValueError("npm is required")
    command = [npm, "pack", "--json", "--ignore-scripts", "--cache", str(npm_cache), "--pack-destination", str(output)]
    if offline:
        command.append("--offline")
    run(command, package, stdout=subprocess.PIPE)

    tgz = output / f"keir-runtime-node-{version}.tgz"
    with tarfile.open(tgz) as archive:
        files = set()
        for entry in archive:
            relative = PurePosixPath(entry.name)
            if not entry.isfile() or relative.parts[:1] != ("package",) or ".." in relative.parts:
                raise ValueError(f"Unexpected npm archive member: {entry.name}")
            member = relative.relative_to("package")
            if member in files or archive.extractfile(entry).read() != (package / member).read_bytes():
                raise ValueError(f"Unexpected npm archive bytes: {entry.name}")
            files.add(member)
        required = {PurePosixPath(name) for name in ("package.json", "README.md", "LICENSE", "src/index.js")}
        if not required <= files:
            raise ValueError("npm archive is missing required runtime files")
    return tgz


def build(args, scratch, output):
    source = stage_source(args.revision, args.version, scratch)
    license_bytes = (source / "LICENSE").read_bytes()
    lock = json.loads((NOTICES / "lock.json").read_bytes())
    verify_maven_inputs(source, args.version, lock["maven"])
    maven_command_line, maven_env = maven_command(source, args.maven_repo, args.offline)
    tree = source / "compiler-java/dependency-tree.txt"
    tree.unlink(missing_ok=True)  # Always read the resolved tree, never the tracked reference.
    run([*maven_command_line, "-pl", "compiler-java", "dependency:tree", "-DoutputFile=dependency-tree.txt"],
        source, env=maven_env)
    dependencies = verified_dependencies(tree, args.maven_repo, lock["dependencies"])
    run([*maven_command_line, "-pl", "compiler-java,runtime-java", "-am", "-Dmaven.test.skip=true", "package"],
        source, env=maven_env)
    cli, maven = package_java(source, output, args.version, dependencies, lock, license_bytes)
    node = package_node(source, output, args.version, args.npm_cache, args.offline, license_bytes)
    license_path = output / "LICENSE"
    license_path.write_bytes(license_bytes)
    assets = (cli, maven, node, license_path)
    if set(output.iterdir()) != set(assets):
        raise ValueError("Unexpected file in staged release")
    (output / "SHA256SUMS").write_bytes("".join(f"{digest(asset)}  {asset.name}\n" for asset in assets).encode())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--revision", required=True, help="Full commit SHA")
    parser.add_argument("--version", required=True, help="Version declared by the compiler and Node package")
    parser.add_argument("--output", type=Path, required=True, help="New directory for the five release assets")
    parser.add_argument("--maven-repo", type=Path, required=True, help="Maven dependency cache")
    parser.add_argument("--npm-cache", type=Path, required=True, help="npm package cache")
    parser.add_argument("--offline", action="store_true", help="Use the cached Maven distribution and dependencies")
    args = parser.parse_args()
    if not re.fullmatch(r"[a-f0-9]{40}", args.revision):
        parser.error("--revision requires a full lowercase Git commit SHA")
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        parser.error("--version requires x.y.z")
    output = args.output.absolute()
    if os.path.lexists(output):
        parser.error("--output already exists")
    args.maven_repo = args.maven_repo.resolve()
    args.npm_cache = args.npm_cache.resolve()
    try:
        output.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix=".keir-release-", dir=output.parent) as temporary:
            scratch = Path(temporary)
            assets = scratch / "assets"
            assets.mkdir()
            build(args, scratch, assets)
            if os.path.lexists(output):
                raise ValueError("--output was created during the build")
            assets.rename(output)
    except (OSError, ValueError, subprocess.CalledProcessError, tarfile.TarError) as error:
        parser.exit(1, f"Release build failed: {error}\n")
    print(f"Built Keir Core {args.version} at {args.revision}: {output}")


if __name__ == "__main__":
    main()
