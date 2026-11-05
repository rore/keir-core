import json
import sys
from importlib.metadata import version
from pathlib import Path

from jsonschema import Draft202012Validator


schema_path = Path(__file__).resolve().parents[2] / "schemas/scenario-1/scenario.schema.json"
schema = json.loads(schema_path.read_bytes())
if schema["$id"] != "urn:keir:schema:scenario:1" or version("jsonschema") != "4.26.0":
    raise ValueError("Use the shared scenario schema and pinned jsonschema version")
Draft202012Validator.check_schema(schema)
validator = Draft202012Validator(schema)
json.dump([validator.is_valid(document) for document in json.load(sys.stdin.buffer)], sys.stdout)
