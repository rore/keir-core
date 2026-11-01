"""Decoded structure checks. Native residuals have separate Java/Node tests."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("check_read2_contracts", ROOT / "scripts/check-read2-contracts.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)


def load(path):
    return json.loads((ROOT / path).read_text(encoding="utf-8"))


class Contracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.validators = check.validators()
        cls.source = load("conformance/read2-structural/source.json")
        cls.dcm = load("conformance/generic-hooks/read/dcm.json")
        cls.artifact = load("conformance/generic-hooks/read/artifact.json")

    def subvalidator(self, schema, pointer):
        validator = self.validators[schema]
        return validator.evolve(schema={"$ref": validator.schema["$id"] + "#" + pointer})

    def valid(self, validator, value, expected=True):
        self.assertEqual(validator.is_valid(value), expected)

    def test_current_positives_and_literal_variants(self):
        for suffix in ["", "-repeated", "-alternate", "-literals"]:
            self.valid(self.validators["source"], load(f"conformance/read2-structural/source{suffix}.json"))
        self.valid(self.validators["dcm"], self.dcm)
        for suffix in ["", "-repeated", "-alternate"]:
            self.valid(self.validators["artifact"], load(f"conformance/generic-hooks/read/artifact{suffix}.json"))
        self.valid(self.validators["artifact"], load("conformance/read2-structural/artifact-literals.json"))
        for policy in [{}, {"maxCalls": {}}, {"allow": []}, {"allow": ["tasks.get", "tasks.create"], "maxCalls": {"tasks.create": 4}}]:
            value = copy.deepcopy(self.dcm)
            value["entrypoints"]["ClassifyTask"]["capabilityPolicy"] = policy
            self.valid(self.validators["dcm"], value)

    def test_every_closed_record_rejects_unknown_and_missing_fields(self):
        s, d, a = self.source, self.dcm, self.artifact
        cases = [("source", "", s), ("source", "/properties/domain", s["domain"]),
                 ("source", "/properties/return", s["return"]), ("source", "/$defs/call", s["steps"][0]),
                 ("source", "/$defs/let", s["steps"][1])]
        for binding in [{"cel": "input.taskId"}, {"literal": True}, {"literal": ""}, {"literal": "project1", "type": "ProjectId"}]:
            cases.append(("source", "/$defs/binding", binding))
        cases += [("dcm", "", d), ("dcm", "/properties/domain", d["domain"]),
                  *[("dcm", "/$defs/type", d["types"][name]) for name in ["TaskId", "TaskStatus", "Task"]],
                  ("dcm", "/$defs/capability", d["capabilities"]["tasks.get"]),
                  ("dcm", "/$defs/parameter", {"type": "TaskId", "binding": "agent"}),
                  ("dcm", "/$defs/consumer", d["entrypoints"]["ClassifyTask"]["consumer"]),
                  ("dcm", "/$defs/entrypoint", d["entrypoints"]["ClassifyTask"]), ("dcm", "/$defs/policy", {})]
        cases += [("artifact", "", a), ("artifact", "/properties/environment", a["environment"]),
                  ("artifact", "/properties/modelRef", a["modelRef"]), ("artifact", "/properties/return", a["return"]),
                  ("artifact", "/$defs/expression", a["expressions"]["e0"]),
                  ("artifact", "/$defs/call", a["steps"][0]), ("artifact", "/$defs/bind", a["steps"][1]),
                  ("artifact", "/$defs/binding", a["steps"][0]["args"]["taskId"]),
                  ("artifact", "/$defs/binding", {"literal": False, "type": "Boolean"})]
        for schema, pointer, value in cases:
            validator = self.subvalidator(schema, pointer)
            with self.subTest(schema=schema, pointer=pointer, value=value):
                self.valid(validator, value)
                self.valid(validator, {**value, "unexpected": True}, False)
                for field in value:
                    # Optional policy has no required fields; these examples otherwise use required fields only.
                    if pointer == "/$defs/policy":
                        continue
                    missing = {key: item for key, item in value.items() if key != field}
                    self.valid(validator, missing, schema == "source" and pointer == "/$defs/binding" and field == "type")

    def test_map_and_array_ceilings_at_boundary_and_plus_one(self):
        binding = {"literal": "x"}
        lowered = {"literal": "x", "type": "String"}
        cap = self.dcm["capabilities"]["tasks.get"]
        entry = self.dcm["entrypoints"]["ClassifyTask"]
        expr = self.artifact["expressions"]["e0"]
        maps = [("source", "/$defs/bindings", 16, binding), ("dcm", "/properties/types", 16, {"kind": "nominal", "base": "String"}),
                ("dcm", "/properties/capabilities", 8, cap), ("dcm", "/properties/entrypoints", 4, entry),
                ("dcm", "/$defs/type/oneOf/2/properties/fields", 16, "String"),
                ("dcm", "/$defs/capability/properties/parameters", 16, {"type": "String", "binding": "agent"}),
                ("dcm", "/$defs/policy/properties/maxCalls", 8, 4), ("artifact", "/$defs/bindings", 16, lowered)]
        for schema, pointer, ceiling, value in maps:
            validator = self.subvalidator(schema, pointer)
            self.valid(validator, {}, pointer not in ["/properties/types", "/properties/entrypoints", "/$defs/type/oneOf/2/properties/fields"])
            for count in [ceiling, ceiling + 1]:
                with self.subTest(schema=schema, pointer=pointer, count=count):
                    self.valid(validator, {f"N{i}": value for i in range(count)}, count == ceiling)
        for schema, pointer, ceiling, item in [
            ("source", "/properties/steps", 8, self.source["steps"][0]),
            ("artifact", "/properties/steps", 8, self.artifact["steps"][0]),
        ]:
            validator = self.subvalidator(schema, pointer)
            for count in [0, ceiling, ceiling + 1]:
                self.valid(validator, [item] * count, count == ceiling)
        validator = self.subvalidator("artifact", "/properties/expressions")
        for count in [0, 512, 513]:
            self.valid(validator, {f"e{i}": expr for i in range(count)}, count <= 512)
        validator = self.subvalidator("artifact", "/$defs/expression/properties/variables")
        self.valid(validator, {"input": "Input", **{f"N{i}": "String" for i in range(8)}})
        self.valid(validator, {"input": "Input", **{f"N{i}": "String" for i in range(9)}}, False)
        for pointer, ceiling, values in [("/$defs/type/oneOf/1/properties/values", 16, [f"V{i}" for i in range(17)]),
                                         ("/$defs/capability/properties/errors", 16, [f"E{i}" for i in range(17)]),
                                         ("/$defs/ids", 8, [f"cap{i}" for i in range(9)])]:
            validator = self.subvalidator("dcm", pointer)
            self.valid(validator, values[:ceiling]); self.valid(validator, values, False)
            self.valid(validator, values[:1] * 2, False)
            self.valid(validator, [], pointer != "/$defs/type/oneOf/1/properties/values")

    def test_identifier_digest_and_base64_patterns(self):
        for schema, definition, ceiling in [("source", "name", 64), ("source", "id", 100), ("dcm", "reference", 64), ("artifact", "name", 64)]:
            validator = self.subvalidator(schema, "/$defs/" + definition)
            self.valid(validator, "A" * ceiling)
            for bad in ["", "9first", "a-b", "é", "A" * (ceiling + 1), "valid\n"]:
                self.valid(validator, bad, False)
        for schema in ["source", "artifact"]:
            pointer = "/properties/domain/properties/fingerprint" if schema == "source" else "/$defs/digest"
            validator = self.subvalidator(schema, pointer)
            self.valid(validator, "sha256:" + "a" * 64)
            for bad in ["sha256:" + "a" * 63, "sha256:" + "A" * 64, "sha256:" + "a" * 64 + "\n"]:
                self.valid(validator, bad, False)
        validator = self.subvalidator("artifact", "/$defs/expression/properties/checked")
        self.valid(validator, "AAAA" * 5462)
        self.valid(validator, "AAAA" * 5463, False)
        for good in ["AA==", "AAA=", "AAAA", "AA+/AA=="]:
            self.valid(validator, good)
        for bad in ["", "AA", "AA=", "_AAA", "AAAA\n", "A===", " AAAA", "AA==AAAA"]:
            self.valid(validator, bad, False)

    def test_binding_kinds_versions_and_policy(self):
        binding = self.subvalidator("source", "/$defs/binding")
        for bad in [{"literal": None}, {"literal": 1}, {"literal": False, "type": "ProjectId"}, {"cel": "x", "type": "ProjectId"}, {"cel": "x", "literal": "x"}]:
            self.valid(binding, bad, False)
        for field, value in [("grants", {}), ("effectGrant", 0), ("event", {}), ("placement", "AFTER_COMMIT")]:
            dcm = copy.deepcopy(self.dcm); dcm["entrypoints"]["ClassifyTask"][field] = value
            self.valid(self.validators["dcm"], dcm, False)
        for value in [None, {"allow": None}, {"deny": ["x", "x"]}, {"unknown": 1}, {"maxCalls": {"x": -1}}, {"maxCalls": {"x": 5}}, {"maxCalls": {"x": 0.5}}, {"maxCalls": {"x": True}}]:
            self.valid(self.subvalidator("dcm", "/$defs/policy"), value, False)
        for field, value in [("format", "keir-artifact/read-1"), ("profile", "keir-effect/2"), ("dcm", {}), ("descriptors", "AAAA"), ("dcmDigest", "sha256:" + "0" * 64)]:
            artifact = copy.deepcopy(self.artifact); artifact[field] = value
            self.valid(self.validators["artifact"], artifact, False)
        for field in ["when", "onError"]:
            source = copy.deepcopy(self.source); source["steps"][0][field] = {}
            self.valid(self.validators["source"], source, False)
        self.valid(self.subvalidator("artifact", "/$defs/expression/properties/variables"), {"other": "Input"}, False)
        for field, value in [("descriptorRecipe", "unknown/1"), ("format", "keir-dcm/read-1")]:
            artifact = copy.deepcopy(self.artifact); artifact["modelRef"][field] = value
            self.valid(self.validators["artifact"], artifact, False)

    def test_string_codepoint_bounds_and_native_utf8_residual(self):
        for schema, pointer, maximum in [("source", "/$defs/binding/oneOf/0/properties/cel", 512),
                                         ("source", "/$defs/binding/oneOf/1/properties/literal", 256),
                                         ("dcm", "/$defs/text", 256), ("artifact", "/$defs/text", 256)]:
            validator = self.subvalidator(schema, pointer)
            self.valid(validator, "x" * maximum)
            self.valid(validator, "x" * (maximum + 1), False)
            self.valid(validator, "é" * 129)  # 258 UTF-8 bytes: the native boundary rejects this.

    def test_residual_documents_are_structural_positives(self):
        for row in load("conformance/read2-structural/residuals.json"):
            with self.subTest(id=row["id"]):
                self.valid(self.validators[row["schema"]], row["document"])
                if "raw" in row:
                    self.assertEqual(json.loads(row["raw"]), row["document"])
        self.valid(self.subvalidator("dcm", "/$defs/policy"), {"maxCalls": {"tasks.get": 1.0}})

    def test_unknown_reference_is_explicitly_denied_without_network(self):
        with patch.object(check, "deny_retrieval", wraps=check.deny_retrieval) as denied:
            validator = check.validators()["source"].evolve(schema={"$ref": "https://invalid.example/not-registered"})
            with patch("socket.socket", side_effect=AssertionError("No network allowed")):
                with self.assertRaises(Exception) as failure:
                    validator.validate({})
            self.assertIn("Unresolvable", type(failure.exception.__cause__).__name__)
            denied.assert_called_once_with("https://invalid.example/not-registered")

    def test_environment_snapshot_matches_native_export_and_fixture(self):
        result = subprocess.run(["node", "--input-type=module", "-e", "import {ENVIRONMENT} from './runtime-node/src/validation.js'; console.log(JSON.stringify(ENVIRONMENT));"], cwd=ROOT, capture_output=True, text=True, timeout=10, check=True)
        expected = {key: value["const"] for key, value in self.validators["artifact"].schema["properties"]["environment"]["properties"].items()}
        self.assertEqual(json.loads(result.stdout), expected)
        self.assertEqual(self.artifact["environment"], expected)

    def test_cli_reports_structure_only(self):
        result = subprocess.run([sys.executable, "-B", str(ROOT / "scripts/check-read2-contracts.py"), "source", str(ROOT / "conformance/read2-structural/source.json")], cwd=ROOT, capture_output=True, text=True, timeout=10, check=True)
        self.assertEqual(json.loads(result.stdout), {"valid": True, "diagnostics": []})
        value = copy.deepcopy(self.source); value["credential"] = "private-value-never-echo"
        errors = check.diagnostics(self.validators["source"], value)
        self.assertTrue(errors)
        self.assertNotIn("private-value-never-echo", json.dumps(errors))


if __name__ == "__main__":
    unittest.main()
