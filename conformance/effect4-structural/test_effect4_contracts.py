"""Check decoded effect/4 structure and the inputs shared with native tests."""
import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "conformance/generic-hooks/effect4"
FIXTURES = Path(__file__).resolve().parent
CHECKER = ROOT / "scripts/check-read2-contracts.py"
PROFILE = "keir-effect/4"
spec = importlib.util.spec_from_file_location("check_read2_contracts", CHECKER)
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


class Effect4Contracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.validators = check.validators(PROFILE)
        cls.source = load(FIXTURES / "source-handler.json")
        cls.dcm = load(SHARED / "dcm.json")
        cls.artifact = load(FIXTURES / "artifact-handler.json")

    def valid(self, schema, value, expected=True):
        self.assertEqual(self.validators[schema].is_valid(value), expected)

    def test_current_documents(self):
        for name in ["source.json", "source-handler.json"]:
            self.valid("source", load(FIXTURES / name))
        for name in ["dcm.json", "dcm-title.json"]:
            self.valid("dcm", load(SHARED / name))
        for path in sorted(SHARED.glob("artifact*.json")):
            with self.subTest(artifact=path.name):
                self.valid("artifact", load(path))
        self.valid("artifact", self.artifact)

    def test_closed_envelopes_and_profile_pins(self):
        for schema, value, fields in [
            ("source", self.source, {"profile": PROFILE, "approval": {}}),
            ("dcm", self.dcm, {"format": "keir-dcm/effect-3", "assurance": "HOST_QUALIFIED_SINGLE_WRITE", "qualification": {}}),
            ("artifact", self.artifact, {"format": "keir-artifact/read-2", "profile": "keir-effect/3",
                                         "compiler": "unreviewed/1", "dcm": {}, "descriptors": "AAAA", "approval": {}}),
        ]:
            for field, bad in fields.items():
                with self.subTest(schema=schema, field=field):
                    self.valid(schema, {**value, field: bad}, False)
            for field in value:
                with self.subTest(schema=schema, missing=field):
                    self.valid(schema, {key: item for key, item in value.items() if key != field}, False)
        for field in self.artifact["environment"]:
            changed = {**self.artifact, "environment": {**self.artifact["environment"], field: "0.0.0"}}
            self.valid("artifact", changed, False)
        for field, bad in [("format", "keir-dcm/read-2"), ("descriptorRecipe", "other/1")]:
            self.valid("artifact", {**self.artifact, "modelRef": {**self.artifact["modelRef"], field: bad}}, False)

    def test_event_and_policy_shapes(self):
        self.valid("dcm", {**self.dcm, "capabilities": {}})
        for grant in [0, 1, 2, None, False]:
            dcm = copy.deepcopy(self.dcm)
            dcm["entrypoints"]["TaskCompleted"].update(effectGrant=grant, capabilityPolicy={"deny": []})
            self.valid("dcm", dcm, type(grant) is int and grant in (0, 1))
        for field, bad in [("trigger", "REQUEST"), ("placement", "BEFORE_COMMIT"), ("grants", {}),
                           ("event", {"idType": "EventId"}), ("consumer", {"id": "sample", "version": "1", "kind": "DISPLAY"})]:
            dcm = copy.deepcopy(self.dcm)
            dcm["entrypoints"]["TaskCompleted"][field] = bad
            self.valid("dcm", dcm, False)
        for bad in [{"source": "eventId", "type": "Key"}, {"source": "requestKey", "type": ""}]:
            self.valid("dcm", {**self.dcm, "hostKey": bad}, False)

    def test_handler_shapes_and_local_bounds(self):
        source_handler = self.source["return"]
        artifact_handler = self.artifact["return"]
        for schema, base, handler, other in [
            ("source", self.source, source_handler, artifact_handler),
            ("artifact", self.artifact, artifact_handler, source_handler),
        ]:
            for handlers, expected in [
                ({}, False),
                ({"CREATE_REJECTED": other}, False),
                ({"lowercase": handler}, False),
                ({"CREATE_REJECTED\n": handler}, False),
                ({f"ERROR_{i}": handler for i in range(16)}, True),
                ({f"ERROR_{i}": handler for i in range(17)}, False),
            ]:
                changed = copy.deepcopy(base)
                changed["steps"][-1]["onError"] = handlers
                self.valid(schema, changed, expected)
            for field, bad in [("when", "not-valid"), ("onError", None)]:
                changed = copy.deepcopy(base)
                changed["steps"][-1][field] = bad
                self.valid(schema, changed, False)
            for field, value in [("when", "eligible"), ("onError", {"CREATE_REJECTED": handler})]:
                changed = copy.deepcopy(base)
                changed["steps"][0][field] = value
                self.valid(schema, changed, False)
            for count in [0, 8, 9]:
                self.valid(schema, {**base, "steps": [base["steps"][0]] * count}, count == 8)
        mixed = copy.deepcopy(self.source)
        mixed["steps"][0]["let"] = {"literal": "x", "cel": "input"}
        self.valid("source", mixed, False)

    def test_native_residuals_remain_structural_positives(self):
        for row in load(FIXTURES / "residuals.json"):
            with self.subTest(id=row["id"]):
                document = copy.deepcopy(self.artifact)
                target = document
                for key in row["path"][:-1]:
                    target = target[key]
                target[row["path"][-1]] = row["value"]
                self.valid("artifact", document)

    def test_cli_selects_effect4_explicitly(self):
        result = subprocess.run(
            [sys.executable, "-B", str(CHECKER), "--profile", PROFILE, "artifact", str(FIXTURES / "artifact-handler.json")],
            cwd=ROOT, capture_output=True, text=True, timeout=10, check=True,
        )
        self.assertEqual(json.loads(result.stdout), {"valid": True, "diagnostics": []})


if __name__ == "__main__":
    unittest.main()
