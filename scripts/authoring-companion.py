"""Build and verify a Keir authoring companion."""

import argparse
import hashlib
import hmac
import io
import json
from pathlib import Path
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
    members = {**payload, "manifest.json": json_bytes(manifest)}
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as archive:
        for name in sorted(members):
            entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            entry.create_system = 3
            entry.external_attr = 0o100644 << 16
            archive.writestr(entry, members[name])
    return output.getvalue()


def build(version):
    revision = SOURCE_REVISIONS[version]
    payload = {name: read_blob(revision, path) for name, path in source_paths(version).items()}
    rows = {row["id"]: row for row in json.loads(read_blob(revision, f"{FIXTURES}/scenarios.json"))}
    selected = [rows[case_id] for case_id in SCENARIO_IDS]
    if any(row["given"]["program"] != "main" for row in selected):
        raise ValueError("The selected cases must use the bundled example artifact.")
    payload[SCENARIOS] = json_bytes(selected)
    data = make_archive(version, payload)
    if len(data) > MAX_ARCHIVE_BYTES:
        raise ValueError("Archive exceeds its size limit.")
    return data


def verify(version, path, expected_digest):
    with path.open("rb") as stream:
        data = stream.read(MAX_ARCHIVE_BYTES + 1)
    if len(data) > MAX_ARCHIVE_BYTES:
        raise ValueError("Archive exceeds its size limit.")
    if not hmac.compare_digest(digest(data), expected_digest):
        raise ValueError("Archive digest does not match the trusted pin.")
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        entries = archive.infolist()
        if [entry.filename for entry in entries] != sorted([*source_paths(version), SCENARIOS, "manifest.json"]):
            raise ValueError("Unexpected archive inventory.")
        for entry in entries:
            if (entry.compress_type != zipfile.ZIP_STORED or entry.flag_bits & 1
                    or entry.compress_size != entry.file_size or entry.file_size > len(data)):
                raise ValueError("Unsupported archive entry.")
        payload = {entry.filename: archive.read(entry) for entry in entries if entry.filename != "manifest.json"}
    if make_archive(version, payload) != data:
        raise ValueError("Archive encoding or manifest does not match its payload.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("build")
    create.add_argument("archive", type=Path)
    check = commands.add_parser("verify")
    check.add_argument("archive", type=Path)
    check.add_argument("--sha256", required=True, help="Trusted archive pin in sha256:<hex> form")
    for command in (create, check):
        command.add_argument("--version", type=int, choices=SOURCE_REVISIONS, default=1,
                             help="Companion version (default: 1)")
    args = parser.parse_args()
    try:
        if args.command == "build":
            data = build(args.version)
            with args.archive.open("xb") as stream:
                stream.write(data)
            print(digest(data))
        else:
            if not args.sha256.isascii():
                raise ValueError("Archive pin must be ASCII.")
            verify(args.version, args.archive, args.sha256)
            if args.version == 1:
                print("Verified effect/4 authoring companion.")
            else:
                print("Verified Keir authoring companion v2.")
    except (OSError, ValueError, KeyError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
