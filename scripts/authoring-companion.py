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
V4_GUIDE = "docs/agents/customer-authoring-guide-v4.md"
V4_COMPATIBILITY = "docs/agents/authoring-companion-v4-compatibility.json"
V4_FIXTURES = "conformance/keir-2"
V4_MODEL = f"{V4_FIXTURES}/dcm-inventory.json"
V4_ARTIFACT = f"{V4_FIXTURES}/artifact-inventory.json"
V4_FORMATS = {
    "language": "keir/2",
    "dcm": "keir-dcm/2",
    "artifact": "keir-artifact/2",
    "scenario": "keir-scenario/2",
}
V4_FILES = (
    V4_GUIDE,
    "docs/specs/workflow-expressions.md",
    "schemas/source-2/source.schema.json",
    "schemas/dcm-2/dcm.schema.json",
    "schemas/artifact-2/artifact.schema.json",
    "schemas/scenario-2/scenario.schema.json",
    V4_MODEL,
    f"{V4_FIXTURES}/dcm-inventory.sha256",
    f"{V4_FIXTURES}/source-inventory.json",
    V4_ARTIFACT,
    f"{V4_FIXTURES}/artifact-inventory.sha256",
    *(f"{V4_FIXTURES}/scenarios/inventory/inventory-{case}.json" for case in (
        "adjusted", "handler-omits-due", "lazy-overflow", "max-no-match", "optional-false", "selected-overflow",
    )),
)
PINNED_EDITIONS = {
    3: (V3_COMPATIBILITY, V3_GUIDE, V3_ARTIFACT, V3_FORMATS, V3_FILES),
    4: (V4_COMPATIBILITY, V4_GUIDE, V4_ARTIFACT, V4_FORMATS, V4_FILES),
}
RELEASE_ASSETS = {
    "compiler": "keir-compiler-cli-{}.zip",
    "javaRuntime": "keir-java-runtime-maven-{}.zip",
    "nodeRuntime": "keir-runtime-node-{}.tgz",
}


def source_paths(version):
    guide = f"customer-authoring-guide-v{version}.md"
    return {guide: f"docs/agents/{guide}", **SOURCES}


def digest(data):
    return "sha256:" + hashlib.sha256(data).hexdigest()


def json_bytes(document):
    return (json.dumps(document, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def read_blob(revision, path):
    return subprocess.check_output(
        ["git", "--no-replace-objects", "cat-file", "blob", f"{revision}:{path}"],
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
    if record["status"] != "release" or packages.keys() != RELEASE_ASSETS.keys():
        raise ValueError("Record either a Core source candidate or all three Core release packages.")
    for role, package in packages.items():
        if (not isinstance(package, dict) or package.keys() != {"version", "assets"}
                or not matches(r"[0-9][0-9A-Za-z.+-]{0,63}", package["version"])
                or not isinstance(package["assets"], dict)):
            raise ValueError("Every Core release package needs a version and asset pins.")
        asset = RELEASE_ASSETS[role].format(package["version"])
        pins = package["assets"]
        if pins.keys() != {asset} or not matches(r"sha256:[0-9a-f]{64}", pins[asset]):
            raise ValueError(f"Expected a SHA-256 pin for {asset}.")


def check_core_assets(record, directory):
    if record["status"] != "release":
        raise ValueError("A source candidate has no Core release assets to check.")
    for package in record["packages"].values():
        for name, pin in package["assets"].items():
            with (directory / name).open("rb") as stream:
                actual = "sha256:" + hashlib.file_digest(stream, "sha256").hexdigest()
            if not hmac.compare_digest(actual, pin):
                raise ValueError(f"Core asset digest does not match: {name}.")


def make_pinned_archive(version, revision, compatibility, payload):
    _, guide, artifact_path, formats, _ = PINNED_EDITIONS[version]
    if not matches(r"[0-9a-f]{40}", revision):
        raise ValueError("A pinned edition requires a full Git commit.")
    validate_compatibility(compatibility)
    artifact = json.loads(payload[artifact_path])
    if (not isinstance(artifact, dict) or artifact.get("format") != formats["artifact"]
            or not isinstance(artifact.get("compiler"), str) or not isinstance(artifact.get("environment"), dict)):
        raise ValueError("Invalid pinned artifact metadata.")
    if version == 4:
        model_pin = payload[f"{V4_FIXTURES}/dcm-inventory.sha256"].decode("ascii").strip()
        artifact_pin = payload[f"{V4_FIXTURES}/artifact-inventory.sha256"].decode("ascii").strip()
        model_ref = artifact.get("modelRef")
        if (not isinstance(model_ref, dict) or model_ref.get("format") != formats["dcm"]
                or model_ref.get("digest") != model_pin or digest(payload[V4_MODEL]) != model_pin
                or digest(payload[V4_ARTIFACT]) != artifact_pin
                or artifact["compiler"] != "keir-compiler-java/0.2.0"):
            raise ValueError("Invalid edition 4 example or compiler pin.")
        if compatibility["status"] == "release" and any(
                package["version"] != "0.2.0" for package in compatibility["packages"].values()):
            raise ValueError("Edition 4 requires Core 0.2.0 packages.")
    manifest = {
        "format": f"keir-authoring-companion/{version}",
        "sourceRevision": revision,
        "guide": guide,
        "contracts": formats,
        "producer": artifact["compiler"],
        "environment": artifact["environment"],
        "compatibility": compatibility,
        "files": {name: {"bytes": len(data), "digest": digest(data)} for name, data in payload.items()},
    }
    return encode_archive(payload, manifest)


def build_pinned(version, revision, core_assets):
    compatibility_path, _, _, _, files = PINNED_EDITIONS[version]
    if not matches(r"[0-9a-f]{40}", revision):
        raise ValueError("A pinned edition requires --revision with a full Git commit.")
    root = Path(__file__).resolve().parents[1]
    actual = subprocess.check_output(
        ["git", "--no-replace-objects", "cat-file", "-t", revision], cwd=root, text=True,
    ).strip()
    if actual != "commit":
        raise ValueError("Source revision must be a Git commit.")
    compatibility = json.loads(read_blob(revision, compatibility_path))
    validate_compatibility(compatibility)
    core = compatibility["coreRevision"]
    actual = subprocess.check_output(
        ["git", "--no-replace-objects", "cat-file", "-t", core], cwd=root, text=True,
    ).strip()
    if actual != "commit":
        raise ValueError("Core compatibility revision must be a Git commit.")
    comparison = subprocess.run(
        ["git", "--no-replace-objects", "diff", "--quiet", core, revision, "--", *files[1:]], cwd=root,
    )
    if comparison.returncode:
        raise ValueError("Companion inputs differ from the recorded Core revision.")
    if compatibility["status"] == "release" and core_assets is None:
        raise ValueError("Building a release row requires --core-assets.")
    if core_assets is not None:
        check_core_assets(compatibility, core_assets)
    payload = {name: read_blob(revision, name) for name in files}
    return make_pinned_archive(version, revision, compatibility, payload)


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
    data = build_pinned(version, revision, core_assets) if version in PINNED_EDITIONS else build_historical(version)
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
    pinned = version in PINNED_EDITIONS
    files = PINNED_EDITIONS[version][4] if pinned else (*source_paths(version), SCENARIOS)
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        entries = archive.infolist()
        if [entry.filename for entry in entries] != sorted([*files, "manifest.json"]):
            raise ValueError("Unexpected archive inventory.")
        for entry in entries:
            if (entry.compress_type != zipfile.ZIP_STORED or entry.flag_bits & 1
                    or entry.compress_size != entry.file_size or entry.file_size > len(data)):
                raise ValueError("Unsupported archive entry.")
        payload = {entry.filename: archive.read(entry) for entry in entries if entry.filename != "manifest.json"}
        manifest = json.loads(archive.read("manifest.json")) if pinned else None
    if pinned:
        if not isinstance(manifest, dict):
            raise ValueError("Invalid pinned manifest.")
        canonical = make_pinned_archive(version, manifest.get("sourceRevision"), manifest.get("compatibility"), payload)
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
    create.add_argument("--revision", help="Full Git commit containing the pinned edition inputs")
    check = commands.add_parser("verify")
    check.add_argument("archive", type=Path)
    check.add_argument("--sha256", required=True, help="Trusted archive pin in sha256:<hex> form")
    for command in (create, check):
        command.add_argument("--version", type=int, choices=(1, 2, 3, 4), default=1,
                             help="Companion version (default: 1)")
        command.add_argument("--core-assets", type=Path, help="Directory containing the pinned Core release assets")
    args = parser.parse_args()
    try:
        revision = getattr(args, "revision", None)
        if args.version not in PINNED_EDITIONS and (revision is not None or args.core_assets is not None):
            raise ValueError("--revision and --core-assets apply only to pinned editions.")
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
