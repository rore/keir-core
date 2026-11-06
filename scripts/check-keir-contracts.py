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
REVISIONS = (1, 2)


def deny_retrieval(uri):
    raise NoSuchResource(ref=uri)


def validators(revision):
    if version("jsonschema") != "4.26.0":
        raise RuntimeError("Use the pinned development requirements in an isolated environment.")
    schemas = {
        name: read_document(ROOT / "schemas" / f"{name}-{revision}" / f"{name}.schema.json")
        for name in NAMES
    }
    for name, schema in schemas.items():
        if schema["$id"] != f"urn:keir:schema:{name}:{revision}":
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


def check_fixtures(schemas, revision):
    fixtures = ROOT / "conformance" / f"keir-{revision}"
    patterns = {
        "source": "source-handler.json" if revision == 1 else "source*.json",
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
    if revision == 2:
        mutants.extend(workflow_mutants(samples))
    for name, changes in mutants:
        if schemas[name].is_valid({**samples[name], **changes}):
            raise ValueError(f"{name} accepted invalid fields {', '.join(changes)}")

    rejections = len(mutants) + check_scenario_rejections(schemas["scenario"], fixtures)
    print(f"Validated {count} public /{revision} fixtures and {rejections} structural rejections. Native checks remain required.")


def workflow_mutants(samples):
    model_ref = samples["artifact"]["modelRef"]
    types = samples["dcm"]["types"]
    mutants = [
        ("source", {"language": "keir/1"}),
        ("dcm", {"format": "keir-dcm/1"}),
        ("artifact", {"format": "keir-artifact/1"}),
        ("artifact", {"compiler": "keir-compiler-java/0.1.0"}),
        ("artifact", {"modelRef": {**model_ref, "format": "keir-dcm/1"}}),
        ("artifact", {"modelRef": {**model_ref, "descriptorRecipe": "keir-record-descriptors/1"}}),
        ("scenario", {"format": "keir-scenario/1"}),
    ]
    for bound in (-1, 17, 1.5):
        probe = {"kind": "list", "element": "String", "maxItems": bound}
        mutants.append(("dcm", {"types": {**types, "ProbeList": probe}}))
    for field in ({"type": "Boolean"}, {"type": "Boolean", "optional": False},
                  {"type": "Boolean", "optional": True, "extra": True}):
        probe = {"kind": "record", "fields": {"flag": field}}
        mutants.append(("dcm", {"types": {**types, "ProbeRecord": probe}}))
    capabilities = samples["dcm"]["capabilities"]
    capability_id, capability = next(iter(capabilities.items()))
    for binding, optional in (("agent", False), ("host", True)):
        probe = {"type": "String", "binding": binding, "optional": optional}
        parameters = {**capability["parameters"], "probe": probe}
        changed = {**capability, "parameters": parameters}
        mutants.append(("dcm", {"capabilities": {**capabilities, capability_id: changed}}))
    return mutants


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--revision", type=int, choices=REVISIONS,
                        help="Schema revision (default: 1 for a document, both for fixture checks)")
    parser.add_argument("schema", nargs="?", choices=NAMES)
    parser.add_argument("document", nargs="?", type=Path)
    args = parser.parse_args()
    if bool(args.schema) != bool(args.document):
        parser.error("Supply both schema name and JSON document, or neither to run checks.")
    if not args.schema:
        revisions = (args.revision,) if args.revision else REVISIONS
        try:
            for revision in revisions:
                check_fixtures(validators(revision), revision)
        except (OSError, ValueError) as error:
            print(error, file=sys.stderr)
            return 1
        return 0
    validator = validators(args.revision or 1)[args.schema]
    try:
        errors = diagnostics(validator, read_document(args.document))
    except (OSError, ValueError, RecursionError):
        errors = [{"code": "DOCUMENT_INVALID", "path": "", "message": "Provide one bounded decoded JSON document."}]
    print(json.dumps({"valid": not errors, "diagnostics": errors}))
    return bool(errors)


if __name__ == "__main__":
    sys.exit(main())
