#!/usr/bin/env python3
"""Check decoded read/2 structure; this is not a native parser or authority gate."""
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


def deny_retrieval(uri):
    raise NoSuchResource(ref=uri)


def validators():
    if version("jsonschema") != "4.26.0":
        raise RuntimeError("Use the pinned development requirements in an isolated environment.")
    schemas = {name: json.loads((ROOT / "schemas/read-2" / (name + ".schema.json")).read_text(encoding="utf-8")) for name in NAMES}
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
    parser.add_argument("schema", nargs="?", choices=NAMES)
    parser.add_argument("document", nargs="?", type=Path)
    args = parser.parse_args()
    if bool(args.schema) != bool(args.document):
        parser.error("Supply both schema name and JSON document, or neither to run checks.")
    if args.schema:
        try:
            with args.document.open("rb") as stream:
                raw = stream.read(1_048_577)
            if len(raw) > 1_048_576:
                raise ValueError("Tool input bound")
            errors = diagnostics(validators()[args.schema], json.loads(raw))
        except (OSError, ValueError):
            print(json.dumps({"valid": False, "diagnostics": [{"code": "DOCUMENT_INVALID", "path": "", "message": "Provide one bounded decoded JSON document."}]}))
            return 1
        print(json.dumps({"valid": not errors, "diagnostics": errors}))
        return bool(errors)
    start = time.perf_counter()
    suite = unittest.defaultTestLoader.discover(str(ROOT / "conformance/read2-structural"), pattern="test_*.py")
    result = unittest.TextTestRunner(verbosity=1).run(suite)
    print(f"read/2 structural checks: {result.testsRun} tests in {time.perf_counter() - start:.3f}s; native checks remain required")
    return not result.wasSuccessful()


if __name__ == "__main__":
    sys.exit(main())
