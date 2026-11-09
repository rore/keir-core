import json
import sys
from importlib.metadata import version
from pathlib import Path

from jsonschema import Draft202012Validator


revision = sys.argv[1] if len(sys.argv) == 2 else "2"
if len(sys.argv) > 2 or revision not in ("2", "3"):
    raise ValueError("Scenario revision must be 2 or 3")
schema_path = Path(__file__).resolve().parents[2] / f"schemas/scenario-{revision}/scenario.schema.json"
schema = json.loads(schema_path.read_bytes())
if schema["$id"] != f"urn:keir:schema:scenario:{revision}" or version("jsonschema") != "4.26.0":
    raise ValueError("Use the shared scenario schema and pinned jsonschema version")
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema)
json.dump([validator.is_valid(document) for document in json.load(sys.stdin.buffer)], sys.stdout)
