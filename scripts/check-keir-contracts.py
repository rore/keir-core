#!/usr/bin/env python3
"""Check decoded public Keir structure. Native validation remains required."""
import argparse
from importlib.metadata import version
import json
from pathlib import Path
import sys

from jsonschema import Draft202012Validator
from referencing import Registry, Resource
from referencing.exceptions import NoSuchResource

ROOT = Path(__file__).resolve().parents[1]
NAMES = ("source", "dcm", "artifact", "scenario")


def deny_retrieval(uri):
    raise NoSuchResource(ref=uri)


def validators():
    if version("jsonschema") != "4.26.0":
        raise RuntimeError("Use the pinned development requirements in an isolated environment.")
    schemas = {
        name: read_document(ROOT / "schemas" / f"{name}-1" / f"{name}.schema.json")
        for name in NAMES
    }
    for name, schema in schemas.items():
        if schema["$id"] != f"urn:keir:schema:{name}:1":
            raise ValueError(f"Unexpected {name} schema ID")
        Draft202012Validator.check_schema(schema)
    registry = Registry(retrieve=deny_retrieval).with_resources(
        (schema["$id"], Resource.from_contents(schema)) for schema in schemas.values()
    )
    return {name: Draft202012Validator(schema, registry=registry) for name, schema in schemas.items()}


def diagnostics(validator, value):
    errors = sorted(validator.iter_errors(value), key=lambda error: (tuple(map(str, error.absolute_path)), str(error.validator)))
    return [{"code": "STRUCTURE_INVALID", "path": "".join("/" + str(part).replace("~", "~0").replace("/", "~1") for part in error.absolute_path),
             "keyword": error.validator, "message": "Value does not satisfy the structural schema."} for error in errors]


def read_document(path):
    with path.open("rb") as stream:
        raw = stream.read(1_048_577)
    if len(raw) > 1_048_576:
        raise ValueError("Tool input bound")
    return json.loads(raw)


def check_scenario_rejections(validator, fixtures):
    vectors = read_document(fixtures / "scenario-vectors.json")["vectors"]
    structural = [vector for vector in vectors if vector["expect"] == "structural"]
    if not structural:
        raise ValueError("No shared structural scenario rejections")
    for vector in structural:
        if "utf8" in vector:
            raw = vector["utf8"]
        else:
            path = fixtures / "scenarios" / (vector["scenario"] + ".json")
            raw = path.read_text(encoding="utf-8")
            before, after = vector["replace"]
            if before not in raw:
                raise ValueError(f"Scenario vector target is missing: {vector['id']}")
            raw = raw.replace(before, after, 1)
        if validator.is_valid(json.loads(raw)):
            raise ValueError(f"Schema accepted scenario vector {vector['id']}")
    return len(structural)


def check_fixtures(schemas):
    fixtures = ROOT / "conformance" / "keir-1"
    patterns = {
        "source": "source-handler.json",
        "dcm": "dcm*.json",
        "artifact": "artifact*.json",
        "scenario": "scenarios/*/*.json",
    }
    samples = {}
    count = 0
    for name, pattern in patterns.items():
        paths = sorted(fixtures.glob(pattern))
        if not paths:
            raise ValueError(f"No {name} fixtures matched {pattern}")
        for path in paths:
            document = read_document(path)
            errors = diagnostics(schemas[name], document)
            if errors:
                raise ValueError(f"{path.relative_to(ROOT)}: {json.dumps(errors)}")
            samples.setdefault(name, document)
            count += 1

    mutants = [
        ("source", {"language": "keir/v0"}),
        ("dcm", {"format": "keir-dcm/effect-4"}),
        ("dcm", {"assurance": "HOST_BOUND_SINGLE_WRITE"}),
        ("artifact", {"format": "keir-artifact/effect-4"}),
        ("artifact", {"profile": "keir-effect/4"}),
        ("artifact", {"compiler": "keir-compiler-java/0.1.0-SNAPSHOT"}),
        ("artifact", {"modelRef": {**samples["artifact"]["modelRef"], "format": "keir-dcm/effect-4"}}),
    ]
    for name, changes in mutants:
        if schemas[name].is_valid({**samples[name], **changes}):
            raise ValueError(f"{name} accepted invalid fields {', '.join(changes)}")

    rejections = len(mutants) + check_scenario_rejections(schemas["scenario"], fixtures)
    print(f"Validated {count} public fixtures and {rejections} structural rejections. Native checks remain required.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("schema", nargs="?", choices=NAMES)
    parser.add_argument("document", nargs="?", type=Path)
    args = parser.parse_args()
    if bool(args.schema) != bool(args.document):
        parser.error("Supply both schema name and JSON document, or neither to run checks.")
    schemas = validators()
    if not args.schema:
        try:
            check_fixtures(schemas)
        except (OSError, ValueError) as error:
            print(error, file=sys.stderr)
            return 1
        return 0
    try:
        errors = diagnostics(schemas[args.schema], read_document(args.document))
    except (OSError, ValueError, RecursionError):
        errors = [{"code": "DOCUMENT_INVALID", "path": "", "message": "Provide one bounded decoded JSON document."}]
    print(json.dumps({"valid": not errors, "diagnostics": errors}))
    return bool(errors)


if __name__ == "__main__":
    sys.exit(main())
