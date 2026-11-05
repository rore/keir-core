#!/usr/bin/env python3
"""Check decoded Keir structure without native parsing or authority checks."""
import argparse
import json
from importlib.metadata import version
from pathlib import Path
import sys
import time
import unittest

from jsonschema import Draft202012Validator
from referencing import Registry, Resource
from referencing.exceptions import NoSuchResource

ROOT = Path(__file__).resolve().parents[1]
NAMES = ("source", "dcm", "artifact")
PROFILES = {"keir-read/2": "read-2", "keir-effect/4": "effect-4"}


def deny_retrieval(uri):
    raise NoSuchResource(ref=uri)


def validators(profile="keir-read/2"):
    if version("jsonschema") != "4.26.0":
        raise RuntimeError("Use the pinned development requirements in an isolated environment.")
    directory = ROOT / "schemas" / PROFILES[profile]
    schemas = {name: json.loads((directory / (name + ".schema.json")).read_text(encoding="utf-8")) for name in NAMES}
    for schema in schemas.values():
        Draft202012Validator.check_schema(schema)
    registry = Registry(retrieve=deny_retrieval).with_resources(
        (schema["$id"], Resource.from_contents(schema)) for schema in schemas.values()
    )
    return {name: Draft202012Validator(schema, registry=registry) for name, schema in schemas.items()}


def diagnostics(validator, value):
    errors = sorted(validator.iter_errors(value), key=lambda error: (tuple(map(str, error.absolute_path)), str(error.validator)))
    return [{"code": "STRUCTURE_INVALID", "path": "".join("/" + str(part).replace("~", "~0").replace("/", "~1") for part in error.absolute_path),
             "keyword": error.validator, "message": "Value does not satisfy the structural schema."} for error in errors]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=PROFILES, help="Select the profile for one document (default: keir-read/2).")
    parser.add_argument("schema", nargs="?", choices=NAMES)
    parser.add_argument("document", nargs="?", type=Path)
    args = parser.parse_args()
    if bool(args.schema) != bool(args.document):
        parser.error("Supply both schema name and JSON document, or neither to run checks.")
    if args.profile and not args.schema:
        parser.error("--profile requires a schema name and JSON document.")
    if args.schema:
        try:
            with args.document.open("rb") as stream:
                raw = stream.read(1_048_577)
            if len(raw) > 1_048_576:
                raise ValueError("Tool input bound")
            errors = diagnostics(validators(args.profile or "keir-read/2")[args.schema], json.loads(raw))
        except (OSError, ValueError):
            print(json.dumps({"valid": False, "diagnostics": [{"code": "DOCUMENT_INVALID", "path": "", "message": "Provide one bounded decoded JSON document."}]}))
            return 1
        print(json.dumps({"valid": not errors, "diagnostics": errors}))
        return bool(errors)
    succeeded = True
    for profile, directory in [("read/2", "read2-structural"), ("effect/4", "effect4-structural")]:
        start = time.perf_counter()
        suite = unittest.TestLoader().discover(str(ROOT / "conformance" / directory), pattern="test_*.py")
        result = unittest.TextTestRunner(verbosity=1).run(suite)
        print(f"{profile} structural checks: {result.testsRun} tests in {time.perf_counter() - start:.3f}s. Native checks remain required.")
        succeeded = succeeded and result.wasSuccessful()
    return not succeeded


if __name__ == "__main__":
    sys.exit(main())
