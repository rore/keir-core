"""Offline decoded-structure checks; native validation remains authoritative."""
import json
from pathlib import Path
import sys
from jsonschema import Draft202012Validator
from referencing import Registry
from referencing.exceptions import NoSuchResource

ROOT = Path(__file__).resolve().parents[2]


def deny(uri):
    raise NoSuchResource(ref=uri)


def main():
    validators = {}
    for name in ("scenario", "report"):
        schema = json.loads((ROOT / "schemas/read-2" / f"simulation-{name}.schema.json").read_bytes())
        Draft202012Validator.check_schema(schema)
        validators[name] = Draft202012Validator(schema, registry=Registry(retrieve=deny))
    count = 0
    for path in (ROOT / "conformance/read-simulation").glob("*/*.json"):
        value = json.loads(path.read_bytes())
        if value.get("format") == "keir-read-simulation/1":
            validators["scenario"].validate(value)
            count += 1
    example = json.loads((ROOT / "conformance/read-simulation/task-advice/open.json").read_bytes())
    for field in ("bindings", "authorize", "invokers", "fixture", "program", "path"):
        assert not validators["scenario"].is_valid(dict(example, **{field: {}}))
    for change in ({"extra": True}, {"stubs": [{}] * 5}, {"input": {"taskId": None}}):
        assert not validators["scenario"].is_valid(dict(example, **change))
    reports = list((ROOT / ".tools/read-simulation-reports").glob("[0-9]*.json"))
    if not reports:
        raise ValueError("Run packaged parity before report schema qualification")
    for path in reports:
        report = json.loads(path.read_bytes())
        validators["report"].validate(report)
        for change in ({"args": {}}, {"mismatches": [{}] * 9}, {"status": "OTHER"}, {"identity": {"modelDigest": "private"}}):
            assert not validators["report"].is_valid(dict(report, **change))
        count += 1
    try:
        validators["scenario"].evolve(schema={"$ref": "https://invalid.example/blocked"}).validate({})
        raise AssertionError("External schema retrieval was not blocked")
    except Exception as error:
        assert "blocked" in str(error)
    print(f"Offline simulation schemas: {count} valid fixtures/reports; negative authority, bounds, null, report and retrieval checks pass")
    return 0


if __name__ == "__main__":
    sys.exit(main())
