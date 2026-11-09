#!/usr/bin/env python3
"""Verify a Core release asset set and optionally smoke-test its installed packages."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import tarfile
import tempfile
from zipfile import BadZipFile, ZipFile
from xml.etree import ElementTree

from release_maven import maven_command

REPO = Path(__file__).resolve().parents[1]
NOTICE_LOCK = Path(__file__).resolve().with_name("release-core-notices") / "lock.json"
OUTCOMES = [
    "task1 CREATED CONFIRMED | Follow up",
    "task2 NO_OP -",
    "task3 UNCERTAIN UNKNOWN",
]
DECISION_OUTCOMES = [
    "allow ALLOWED | native attempts=1",
    "deny DENIED | native attempts=0 | Operation restricted.",
    "unavailable BLOCKED | native attempts=0",
]


def verify_assets(version, assets, checksums):
    manifest = checksums.read_bytes()
    expected = {
        "LICENSE",
        f"keir-compiler-cli-{version}.zip",
        f"keir-java-runtime-maven-{version}.zip",
        f"keir-runtime-node-{version}.tgz",
    }
    pins = {}
    for line in manifest.decode("ascii").splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  ([A-Za-z0-9_.-]+)", line)
        if not match:
            raise ValueError("Invalid SHA256SUMS entry")
        digest, name = match.groups()
        if name not in expected or name in pins:
            raise ValueError(f"Unexpected or duplicate checksum entry: {name}")
        pins[name] = digest
    if pins.keys() != expected or {path.name for path in assets.iterdir()} != expected | {"SHA256SUMS"}:
        raise ValueError("Release asset inventory differs from the expected set")
    if (assets / "SHA256SUMS").read_bytes() != manifest:
        raise ValueError("Release SHA256SUMS differs from the trusted manifest")
    for name, digest in pins.items():
        if hashlib.sha256((assets / name).read_bytes()).hexdigest() != digest:
            raise ValueError(f"Release digest mismatch: {name}")


def run(args, cwd=REPO, **kwargs):
    return subprocess.run(args, cwd=cwd, check=True, **kwargs)


def check_zip(archive, required, *, notices=False):
    names = set()
    for member in archive.infolist():
        name = member.filename
        mode = stat.S_IFMT(member.external_attr >> 16)
        if (name != member.orig_filename or name in names or mode not in (0, stat.S_IFREG) or "\\" in name or ":" in name
                or any(part in ("", ".", "..") for part in name.split("/"))):
            raise ValueError(f"Invalid ZIP member: {name}")
        names.add(name)
    extra = names - required
    if required - names or (extra and not (notices and all(name.startswith("third-party/") for name in extra))):
        raise ValueError("ZIP contents differ from the expected release layout")


def check_compiler_notices(archive, compiler_name):
    lock = json.loads(NOTICE_LOCK.read_bytes())["upstream"]
    notices = {f"third-party/{name}": info["sha256"] for name, info in lock.items()}
    check_zip(archive, {compiler_name, "LICENSE", "THIRD-PARTY.txt", *notices}, notices=True)
    for name, expected in notices.items():
        if hashlib.sha256(archive.read(name)).hexdigest() != expected:
            raise ValueError(f"Compiler ZIP notice differs from the reviewed bytes: {name}")


def check_maven_coordinates(parent, runtime, version):
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    parent_xml, runtime_xml = (ElementTree.fromstring(data) for data in (parent, runtime))
    expected_parent = ["dev.keir", "keir-parent", version]
    fields = ("groupId", "artifactId", "version")
    if ([parent_xml.findtext(f"m:{field}", namespaces=namespace) for field in fields] != expected_parent
            or [runtime_xml.findtext(f"m:parent/m:{field}", namespaces=namespace) for field in fields] != expected_parent
            or runtime_xml.findtext("m:artifactId", namespaces=namespace) != "keir-runtime-java"
            or runtime_xml.findtext("m:groupId", "dev.keir", namespace) != "dev.keir"
            or runtime_xml.findtext("m:version", version, namespace) != version):
        raise ValueError("Packaged Maven coordinates differ from the release version")


def smoke(version, assets, revision, maven_repo, npm_cache, offline):
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Use a full Git commit ID for --smoke-revision")
    git = ["git", "--no-replace-objects"]
    if run([*git, "cat-file", "-t", revision], capture_output=True, text=True).stdout.strip() != "commit":
        raise ValueError("Smoke revision is not a Git commit")
    scratch = REPO / ".tools"
    scratch.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="release-smoke-", dir=scratch) as directory:
        work = Path(directory)
        source = work / "source"
        source_tar = work / "source.tar"
        run([*git, "archive", "--format=tar", f"--output={source_tar}", revision])
        with tarfile.open(source_tar) as archive:
            archive.extractall(source, filter="data")

        compiler_name = f"keir-compiler-java-{version}-cli.jar"
        compiler = work / compiler_name
        license_bytes = (assets / "LICENSE").read_bytes()
        with ZipFile(assets / f"keir-compiler-cli-{version}.zip") as archive:
            check_compiler_notices(archive, compiler_name)
            if archive.read("LICENSE") != license_bytes:
                raise ValueError("Compiler ZIP license differs from the release license")
            compiler.write_bytes(archive.read(compiler_name))
        example = source / "examples" / "node-host"
        programs = [("follow-up", "FollowUp.java", OUTCOMES)]
        if json.loads((example / "follow-up/dcm.json").read_bytes())["format"] == "keir-dcm/3":
            programs.append(("pre-operation", "BeforeOperation.java", DECISION_OUTCOMES))
        artifacts = {}
        for name, _, _ in programs:
            artifacts[name] = work / f"{name}-artifact.json"
            with artifacts[name].open("wb") as output:
                run(["java", "-jar", str(compiler), str(example / name / "source.yaml"),
                     str(example / name / "dcm.json")], cwd=source, stdout=output)

        runtime_name = f"keir-runtime-java-{version}.jar"
        runtime = work / runtime_name
        runtime_pom = source / "runtime-java" / "pom.xml"
        with ZipFile(assets / f"keir-java-runtime-maven-{version}.zip") as archive:
            prefix = f"maven/dev/keir/keir-runtime-java/{version}"
            jar_path = f"{prefix}/{runtime_name}"
            pom_path = f"{prefix}/keir-runtime-java-{version}.pom"
            parent_path = f"maven/dev/keir/keir-parent/{version}/keir-parent-{version}.pom"
            check_zip(archive, {"LICENSE", jar_path, pom_path, parent_path})
            if archive.read("LICENSE") != license_bytes:
                raise ValueError("Maven ZIP license differs from the release license")
            parent_bytes, pom_bytes = archive.read(parent_path), archive.read(pom_path)
            check_maven_coordinates(parent_bytes, pom_bytes, version)
            (source / "pom.xml").write_bytes(parent_bytes)
            runtime_pom.write_bytes(pom_bytes)
            runtime.write_bytes(archive.read(jar_path))
        classpath = work / "classpath.txt"
        maven, maven_env = maven_command(source, maven_repo, offline)
        run([*maven, "-f", str(runtime_pom), "dependency:build-classpath", "-DincludeScope=runtime",
             f"-Dmdep.outputFile={classpath}"], cwd=source, env=maven_env)
        java_results = {}
        for name, launcher, _ in programs:
            java_results[name] = run(["java", "-cp", os.pathsep.join([str(runtime), classpath.read_text().strip()]),
                                      str(example / name / launcher), str(artifacts[name])],
                                     cwd=source, capture_output=True, text=True)

        package_name = f"keir-runtime-node-{version}.tgz"
        package = assets / package_name
        package_dir = source / ".tools" / "packages"
        package_dir.mkdir(parents=True)
        shutil.copyfile(package, package_dir / package_name)
        lock_path = example / "package-lock.json"
        lock = json.loads(lock_path.read_bytes())
        locked_runtime = lock["packages"]["node_modules/@keir/runtime-node"]
        if locked_runtime["resolved"] != f"file:../../.tools/packages/{package_name}" or locked_runtime["version"] != version:
            raise ValueError("The teaching host lock does not select this runtime release")
        integrity = base64.b64encode(hashlib.sha512(package.read_bytes()).digest()).decode("ascii")
        locked_runtime["integrity"] = f"sha512-{integrity}"
        lock_path.write_text(json.dumps(lock), encoding="utf-8")
        npm = "npm.cmd" if os.name == "nt" else "npm"
        run([npm, "ci", *(["--offline"] if offline else []), "--cache", str(npm_cache),
             "--ignore-scripts", "--no-audit", "--no-fund"], cwd=example)
        for name, _, expected in programs:
            node_result = run(["node", str(example / name / "run.mjs"), str(artifacts[name])],
                              cwd=source, capture_output=True, text=True)
            for label, result in (("Java", java_results[name]), ("Node", node_result)):
                if result.stdout.splitlines() != expected:
                    raise ValueError(f"{label} package produced unexpected {name} outcomes: {result.stdout!r}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--assets", required=True, type=Path)
    parser.add_argument("--checksums", type=Path, help="Trusted SHA256SUMS from the staged build")
    parser.add_argument("--smoke-revision", help="Full source commit for the packaged host examples")
    parser.add_argument("--maven-repo", type=Path)
    parser.add_argument("--npm-cache", type=Path)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        parser.error("--version must be major.minor.patch")
    if args.smoke_revision and not (args.maven_repo and args.npm_cache):
        parser.error("--smoke-revision requires --maven-repo and --npm-cache")
    assets = args.assets.resolve()
    try:
        verify_assets(args.version, assets, args.checksums or assets / "SHA256SUMS")
        print("Verified Core asset inventory and checksums.", flush=True)
        if args.smoke_revision:
            smoke(args.version, assets, args.smoke_revision,
                  args.maven_repo.resolve(), args.npm_cache.resolve(), args.offline)
            print("Packaged compiler, Java and Node passed the host example smoke tests.")
    except subprocess.CalledProcessError as error:
        if error.stderr:
            print(error.stderr, file=sys.stderr)
        parser.exit(1, f"{error}\n")
    except (OSError, KeyError, ValueError, BadZipFile, tarfile.TarError, ElementTree.ParseError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
