"""Build and verify a Keir authoring companion."""

import argparse
import hashlib
import hmac
import io
import json
from pathlib import Path
import re
import subprocess
import sys
import zipfile


SOURCE_REVISIONS = {
    1: "57a74a57e6c5fec2e74d3561d4b2ad1da2e650c1",
    2: "74d128f04a5fd76d744e3b7705f6614e64846b38",
}
COMPATIBLE_REVISION = "98d190104b1978b076437ec38bde6e7502e12012"
MAX_ARCHIVE_BYTES = 256 * 1024
FIXTURES = "conformance/generic-hooks/effect4"
EXAMPLE = "examples/follow-up"
SCENARIOS = f"{EXAMPLE}/scenarios.json"
SCENARIO_IDS = ("normal", "other-project", "lost-response")
SOURCES = {
    "schemas/source.schema.json": "schemas/effect-4/source.schema.json",
    "schemas/dcm.schema.json": "schemas/effect-4/dcm.schema.json",
    "schemas/artifact.schema.json": "schemas/effect-4/artifact.schema.json",
    f"{EXAMPLE}/source.json": "conformance/effect4-structural/source.json",
    f"{EXAMPLE}/dcm.json": f"{FIXTURES}/dcm.json",
    f"{EXAMPLE}/artifact.json": f"{FIXTURES}/artifact.json",
}
V3_GUIDE = "docs/agents/customer-authoring-guide-v3.md"
V3_COMPATIBILITY = "docs/agents/authoring-companion-v3-compatibility.json"
V3_FIXTURES = "conformance/keir-1"
V3_MODEL = "examples/node-host/follow-up/dcm.json"
V3_ARTIFACT = f"{V3_FIXTURES}/artifact.json"
V3_FORMATS = {
    "language": "keir/1",
    "dcm": "keir-dcm/1",
    "artifact": "keir-artifact/1",
    "scenario": "keir-scenario/1",
}
V3_FILES = (
    V3_GUIDE,
    "schemas/source-1/source.schema.json",
    "schemas/dcm-1/dcm.schema.json",
    "schemas/artifact-1/artifact.schema.json",
    "schemas/scenario-1/scenario.schema.json",
    V3_MODEL,
    f"{V3_FIXTURES}/source.yaml",
    V3_ARTIFACT,
    *(f"{V3_FIXTURES}/scenarios/main/{case}.json" for case in SCENARIO_IDS),
)
PACKAGE_ROLES = {"compiler", "javaRuntime", "nodeRuntime"}
REQUIRED_ASSETS = {"compiler": {".jar"}, "javaRuntime": {".jar", ".pom"}, "nodeRuntime": {".tgz"}}


def source_paths(version):
    guide = f"customer-authoring-guide-v{version}.md"
    return {guide: f"docs/agents/{guide}", **SOURCES}


def digest(data):
    return "sha256:" + hashlib.sha256(data).hexdigest()


def json_bytes(document):
    return (json.dumps(document, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def read_blob(revision, path):
    return subprocess.check_output(
        ["git", "cat-file", "blob", f"{revision}:{path}"],
        cwd=Path(__file__).resolve().parents[1],
    )


def make_archive(version, payload):
    manifest = {
        "format": f"keir-authoring-companion/{version}",
        "sourceRevision": SOURCE_REVISIONS[version],
        "profile": "keir-effect/4",
        "dcmFormat": "keir-dcm/effect-4",
        "artifactFormat": "keir-artifact/effect-4",
        "compatibility": {
            "developmentCoreRevision": COMPATIBLE_REVISION,
            "compiler": "keir-compiler-java/0.1.0-SNAPSHOT",
            "javaRuntime": "keir-runtime-java/0.1.0-SNAPSHOT",
            "nodeRuntime": "@keir/runtime-node@0.0.0",
        },
        "files": {name: {"bytes": len(data), "digest": digest(data)} for name, data in payload.items()},
    }
    return encode_archive(payload, manifest)


def encode_archive(payload, manifest):
    members = {**payload, "manifest.json": json_bytes(manifest)}
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
        for name in sorted(members):
            entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            entry.create_system = 3
            entry.external_attr = 0o100644 << 16
            archive.writestr(entry, members[name])
    return output.getvalue()


def matches(pattern, value):
    return isinstance(value, str) and re.fullmatch(pattern, value) is not None


def validate_compatibility(record):
    if (not isinstance(record, dict) or record.keys() != {"status", "coreRevision", "packages"}
            or not matches(r"[0-9a-f]{40}", record["coreRevision"]) or not isinstance(record["packages"], dict)):
        raise ValueError("Invalid Core compatibility record.")
    packages = record["packages"]
    if record["status"] == "candidate" and not packages:
        return
    if record["status"] != "release" or packages.keys() != PACKAGE_ROLES:
        raise ValueError("Record either a Core source candidate or all three Core release packages.")
    names = set()
    for role, package in packages.items():
        if (not isinstance(package, dict) or package.keys() != {"version", "assets"}
                or not matches(r"[0-9][0-9A-Za-z.+-]{0,63}", package["version"])
                or not isinstance(package["assets"], dict) or not package["assets"]):
            raise ValueError("Every Core release package needs a version and asset pins.")
        for name, pin in package["assets"].items():
            if (not matches(r"keir-[0-9A-Za-z._+-]{1,120}\.(?:jar|pom|tgz)", name)
                    or name.lower() in names or not matches(r"sha256:[0-9a-f]{64}", pin)):
                raise ValueError("Invalid or duplicate Core release asset pin.")
            names.add(name.lower())
        if not REQUIRED_ASSETS[role] <= {Path(name).suffix for name in package["assets"]}:
            raise ValueError(f"Missing required Core assets for {role}.")


def check_core_assets(record, directory):
    if record["status"] != "release":
        raise ValueError("A source candidate has no Core release assets to check.")
    for package in record["packages"].values():
        for name, pin in package["assets"].items():
            with (directory / name).open("rb") as stream:
                actual = "sha256:" + hashlib.file_digest(stream, "sha256").hexdigest()
            if not hmac.compare_digest(actual, pin):
                raise ValueError(f"Core asset digest does not match: {name}.")


def make_v3_archive(revision, compatibility, payload):
    if not matches(r"[0-9a-f]{40}", revision):
        raise ValueError("Edition 3 requires a full Git commit.")
    validate_compatibility(compatibility)
    artifact = json.loads(payload[V3_ARTIFACT])
    if (not isinstance(artifact, dict) or artifact.get("format") != V3_FORMATS["artifact"]
            or not isinstance(artifact.get("compiler"), str) or not isinstance(artifact.get("environment"), dict)):
        raise ValueError("Invalid edition 3 artifact metadata.")
    manifest = {
        "format": "keir-authoring-companion/3",
        "sourceRevision": revision,
        "guide": V3_GUIDE,
        "contracts": V3_FORMATS,
        "producer": artifact["compiler"],
        "environment": artifact["environment"],
        "compatibility": compatibility,
        "files": {name: {"bytes": len(data), "digest": digest(data)} for name, data in payload.items()},
    }
    return encode_archive(payload, manifest)


def build_v3(revision, core_assets):
    if not matches(r"[0-9a-f]{40}", revision):
        raise ValueError("Edition 3 requires --revision with a full Git commit.")
    root = Path(__file__).resolve().parents[1]
    actual = subprocess.check_output(["git", "cat-file", "-t", revision], cwd=root, text=True).strip()
    if actual != "commit":
        raise ValueError("Edition 3 source revision must be a Git commit.")
    compatibility = json.loads(read_blob(revision, V3_COMPATIBILITY))
    validate_compatibility(compatibility)
    core = compatibility["coreRevision"]
    actual = subprocess.check_output(["git", "cat-file", "-t", core], cwd=root, text=True).strip()
    if actual != "commit":
        raise ValueError("Core compatibility revision must be a Git commit.")
    comparison = subprocess.run(
        ["git", "diff", "--quiet", core, revision, "--", *V3_FILES[1:]], cwd=root,
    )
    if comparison.returncode:
        raise ValueError("Edition 3 schemas or example files differ from the recorded Core revision.")
    if compatibility["status"] == "release" and core_assets is None:
        raise ValueError("Building a release row requires --core-assets.")
    if core_assets is not None:
        check_core_assets(compatibility, core_assets)
    payload = {name: read_blob(revision, name) for name in V3_FILES}
    return make_v3_archive(revision, compatibility, payload)


def build_historical(version):
    revision = SOURCE_REVISIONS[version]
    payload = {name: read_blob(revision, path) for name, path in source_paths(version).items()}
    rows = {row["id"]: row for row in json.loads(read_blob(revision, f"{FIXTURES}/scenarios.json"))}
    selected = [rows[case_id] for case_id in SCENARIO_IDS]
    if any(row["given"]["program"] != "main" for row in selected):
        raise ValueError("The selected cases must use the bundled example artifact.")
    payload[SCENARIOS] = json_bytes(selected)
    return make_archive(version, payload)


def build(version, revision=None, core_assets=None):
    data = build_v3(revision, core_assets) if version == 3 else build_historical(version)
    if len(data) > MAX_ARCHIVE_BYTES:
        raise ValueError("Archive exceeds its size limit.")
    return data


def verify(version, path, expected_digest, core_assets=None):
    with path.open("rb") as stream:
        data = stream.read(MAX_ARCHIVE_BYTES + 1)
    if len(data) > MAX_ARCHIVE_BYTES:
        raise ValueError("Archive exceeds its size limit.")
    if not hmac.compare_digest(digest(data), expected_digest):
        raise ValueError("Archive digest does not match the trusted pin.")
    files = V3_FILES if version == 3 else (*source_paths(version), SCENARIOS)
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        entries = archive.infolist()
        if [entry.filename for entry in entries] != sorted([*files, "manifest.json"]):
            raise ValueError("Unexpected archive inventory.")
        for entry in entries:
            if (entry.compress_type != zipfile.ZIP_STORED or entry.flag_bits & 1
                    or entry.compress_size != entry.file_size or entry.file_size > len(data)):
                raise ValueError("Unsupported archive entry.")
        payload = {entry.filename: archive.read(entry) for entry in entries if entry.filename != "manifest.json"}
        manifest = json.loads(archive.read("manifest.json")) if version == 3 else None
    if version == 3:
        if not isinstance(manifest, dict):
            raise ValueError("Invalid edition 3 manifest.")
        canonical = make_v3_archive(manifest.get("sourceRevision"), manifest.get("compatibility"), payload)
    else:
        canonical = make_archive(version, payload)
    if canonical != data:
        raise ValueError("Archive encoding or manifest does not match its payload.")
    if core_assets is not None:
        check_core_assets(manifest["compatibility"], core_assets)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("build")
    create.add_argument("archive", type=Path)
    create.add_argument("--revision", help="Full Git commit containing the edition 3 inputs")
    check = commands.add_parser("verify")
    check.add_argument("archive", type=Path)
    check.add_argument("--sha256", required=True, help="Trusted archive pin in sha256:<hex> form")
    for command in (create, check):
        command.add_argument("--version", type=int, choices=(1, 2, 3), default=1,
                             help="Companion version (default: 1)")
        command.add_argument("--core-assets", type=Path, help="Directory containing the pinned Core release assets")
    args = parser.parse_args()
    try:
        revision = getattr(args, "revision", None)
        if args.version != 3 and (revision is not None or args.core_assets is not None):
            raise ValueError("--revision and --core-assets apply only to edition 3.")
        if args.command == "build":
            data = build(args.version, revision, args.core_assets)
            with args.archive.open("xb") as stream:
                stream.write(data)
            print(digest(data))
        else:
            if not args.sha256.isascii():
                raise ValueError("Archive pin must be ASCII.")
            verify(args.version, args.archive, args.sha256, args.core_assets)
            if args.version == 1:
                print("Verified effect/4 authoring companion.")
            else:
                print(f"Verified Keir authoring companion v{args.version}.")
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
