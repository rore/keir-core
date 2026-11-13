import hashlib
import json
from io import BytesIO
from pathlib import Path
import runpy
import tempfile
import unittest
from unittest.mock import patch
import warnings
from zipfile import ZipFile

from release_maven import maven_command

verifier = runpy.run_path(Path(__file__).with_name("verify-core-release.py"))
verify_assets = verifier["verify_assets"]


class ReleaseAssetTest(unittest.TestCase):
    def test_numeric_package_selection_keeps_old_bytes_and_complete_oracles(self):
        from subprocess import CompletedProcess
        root = Path(__file__).resolve().parents[1]
        select = verifier["numeric_smoke_inputs"]
        corpus = root / "conformance/keir-4"
        calls = []

        def fixture_compiler(args, **kwargs):
            # Unit transport stub only; real packaged compilation remains a hosted smoke obligation.
            name = Path(args[-2]).name.removesuffix("-source.json")
            calls.append(name)
            if name == "retained-names":
                data = verifier["canonical"](json.loads((corpus / "retained-names.json").read_bytes())["artifact"])
            elif name == "boundaries":
                data = (corpus / "host-boundaries-artifact.json").read_bytes()
            else:
                data = (root / "examples/numeric" / name / "artifact.json").read_bytes()
            return CompletedProcess(args, 0, stdout=data)

        with tempfile.TemporaryDirectory() as directory, patch.dict(select.__globals__, {"run": fixture_compiler}):
            rows = select(root, Path("compiler.jar"), Path(directory))
            self.assertEqual(calls, ["retained-names", "stock", "quote", "boundaries"])
            self.assertEqual(len(rows), 109)
            by_id = {row["scenario"]["id"]: row for row in rows}
            self.assertEqual(by_id["matrix-expression-only-size"]["scenario"]["expect"]["outcome"]["result"], {"value": True})
            retained = by_id["retained-names"]
            self.assertEqual(json.loads(Path(retained["model"]).read_bytes())["types"]["Integer"], {"base": "String", "kind": "nominal"})
            self.assertEqual(retained["scenario"]["given"]["input"]["integer"], "not-a-number")
            self.assertEqual(by_id["stock"]["scenario"]["expect"]["calls"][1]["args"], {"itemId": "item1", "quantity": "3"})
            self.assertEqual(by_id["quote"]["event"], {"id": "attempt1", "source": "quote1"})
            self.assertEqual(by_id["boundary-bad-protected-integer"]["scenario"]["expect"]["calls"], [])
            invalid = by_id["boundary-unused-confirmed-invalid"]["scenario"]["expect"]["outcome"]
            self.assertEqual(invalid["error"]["code"], "ADAPTER_INVALID")
            self.assertEqual(invalid["effects"][0]["state"], "CONFIRMED")
            handler = by_id["boundary-handler-failure"]["scenario"]["expect"]["outcome"]
            self.assertEqual(handler["error"]["cause"], {"code": "DENIED", "path": "/steps/4"})
        with tempfile.TemporaryDirectory() as directory, patch.dict(select.__globals__, {
                "run": lambda *args, **kwargs: CompletedProcess(args, 0, stdout=b"changed")}):
            with self.assertRaisesRegex(ValueError, "Packaged compiler differs from numeric fixture"):
                select(root, Path("compiler.jar"), Path(directory))

    def test_new_guarded_authoring_does_not_relabel_historical_package_inputs(self):
        from subprocess import CompletedProcess
        root = Path(__file__).resolve().parents[1]
        fixture = root / "conformance/keir-3"
        select = verifier["guarded_smoke_inputs"]
        before = {path: path.read_bytes() for path in fixture.glob("artifact-guarded*.json")}

        def current_compiler(args, **kwargs):
            path = fixture / Path(args[-2]).name.replace("source-", "artifact-", 1)
            artifact = json.loads(path.read_bytes())
            artifact.update(format="keir-artifact/4", compiler="keir-compiler-java/0.5.0")
            return CompletedProcess(args, 0, stdout=verifier["canonical"](artifact))

        with tempfile.TemporaryDirectory() as directory, patch.dict(select.__globals__, {"run": current_compiler}):
            rows = select(root, Path("compiler.jar"), Path(directory), "0.5.0")
            self.assertEqual(len(rows), 21)
            producers = {json.loads(Path(row["artifact"]).read_bytes())["compiler"] for row in rows}
            self.assertEqual(producers, {f"keir-compiler-java/{version}" for version in ("0.2.0", "0.3.0", "0.3.1", "0.4.0", "0.5.0")})
            self.assertEqual([row["scenario"]["given"]["fixture"] for row in rows].count("response-expired"), 1)
        self.assertEqual(before, {path: path.read_bytes() for path in before})

    def test_guarded_smoke_keeps_exact_compiled_and_historical_inputs(self):
        root = Path(__file__).resolve().parents[1]
        fixture = root / "conformance/keir-3"
        self.assertIn("guarded_smoke_inputs", verifier)
        compile_inputs = verifier["guarded_smoke_inputs"]
        calls = []

        def fixture_compiler(args, **kwargs):
            # Explicit unit stub for inventory/pin validation, never package evidence.
            from subprocess import CompletedProcess
            calls.append(args)
            name = Path(args[-2]).name.replace("source-", "artifact-", 1)
            return CompletedProcess(args, 0, stdout=(fixture / name).read_bytes())

        with tempfile.TemporaryDirectory() as directory, patch.dict(compile_inputs.__globals__, {"run": fixture_compiler}):
            cases = compile_inputs(root, Path("compiler.jar"), Path(directory))
            self.assertEqual(len(cases), 20)
            self.assertEqual(len(calls), 4)
            self.assertEqual(cases[-1]["bindings"], {})
            self.assertEqual(cases[0]["bindings"], {"actorId": "user1"})
            producers = {json.loads(Path(row["artifact"]).read_bytes())["compiler"] for row in cases}
            self.assertEqual(producers, {f"keir-compiler-java/{version}" for version in ("0.2.0", "0.3.0", "0.3.1", "0.4.0")})
            self.assertTrue(any(row["scenario"]["given"]["fixture"] == "response-expired" for row in cases))
            self.assertEqual(sum(row["scenario"]["expect"]["outcome"]["classification"] == "UNCERTAIN" for row in cases), 1)
        from subprocess import CompletedProcess
        with tempfile.TemporaryDirectory() as directory, patch.dict(compile_inputs.__globals__, {
                "run": lambda *args, **kwargs: CompletedProcess(args, 0, stdout=b"changed") }):
            with self.assertRaisesRegex(ValueError, "Packaged compiler differs"):
                compile_inputs(root, Path("compiler.jar"), Path(directory))

    def test_rejects_changed_missing_and_extra_assets_or_changed_manifest(self):
        files = (
            "LICENSE",
            "keir-compiler-cli-0.2.0.zip",
            "keir-java-runtime-maven-0.2.0.zip",
            "keir-runtime-node-0.2.0.tgz",
        )
        scratch = Path(__file__).resolve().parents[1] / ".tools"
        scratch.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as directory:
            root = Path(directory)
            assets = root / "assets"
            assets.mkdir()
            manifest = "".join(f"{hashlib.sha256(name.encode()).hexdigest()}  {name}\n" for name in files)
            checksums = root / "trusted"
            checksums.write_text(manifest, encoding="ascii")
            (assets / "SHA256SUMS").write_bytes(checksums.read_bytes())
            for name in files:
                (assets / name).write_bytes(name.encode())
            verify_assets("0.2.0", assets, checksums)

            asset = assets / files[-1]
            asset.write_bytes(b"changed")
            with self.assertRaisesRegex(ValueError, "digest mismatch"):
                verify_assets("0.2.0", assets, checksums)
            asset.unlink()
            with self.assertRaisesRegex(ValueError, "inventory"):
                verify_assets("0.2.0", assets, checksums)
            asset.write_bytes(files[-1].encode())
            extra = assets / "extra.zip"
            extra.touch()
            with self.assertRaisesRegex(ValueError, "inventory"):
                verify_assets("0.2.0", assets, checksums)
            extra.unlink()
            (assets / "SHA256SUMS").write_bytes(b"changed")
            with self.assertRaisesRegex(ValueError, "trusted manifest"):
                verify_assets("0.2.0", assets, checksums)

    def test_offline_smoke_requires_cached_maven(self):
        scratch = Path(__file__).resolve().parents[1] / ".tools"
        scratch.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as directory:
            source = Path(directory)
            properties = source / ".mvn/wrapper/maven-wrapper.properties"
            properties.parent.mkdir(parents=True)
            properties.write_text("distributionUrl=https://example.test/apache-maven-3.9.0-bin.zip\n", encoding="ascii")
            with self.assertRaisesRegex(ValueError, "cached copy"):
                maven_command(source, source / "repository", offline=True)

    def test_rejects_invalid_zip_members(self):
        for names in (("LICENSE", "LICENSE"), ("LICENSE", "../escape"), ("LICENSE", "other")):
            with self.subTest(names=names), warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                content = BytesIO()
                with ZipFile(content, "w") as archive:
                    for name in names:
                        archive.writestr(name, b"")
                with ZipFile(content) as archive, self.assertRaises(ValueError):
                    verifier["check_zip"](archive, {"LICENSE"})

    def test_rejects_compiler_zip_missing_reviewed_notice(self):
        content = BytesIO()
        with ZipFile(content, "w") as archive:
            for name in ("compiler.jar", "LICENSE", "THIRD-PARTY.txt", "third-party/unrelated.txt"):
                archive.writestr(name, b"")
        with ZipFile(content) as archive, self.assertRaisesRegex(ValueError, "expected release layout"):
            verifier["check_compiler_notices"](archive, "compiler.jar")


def source_probe_check():
    """Opt-in real source-build check of probe code; not installed-package qualification."""
    import os
    import subprocess
    from xml.etree import ElementTree
    root = Path(__file__).resolve().parents[1]
    assert "GUARDED_JAVA_SMOKE" in verifier and "GUARDED_NODE_SMOKE" in verifier, "Missing guarded consumer probes"
    report = ElementTree.parse(root / "runtime-java/target/surefire-reports/TEST-dev.keir.runtime.PublicScenarioTest.xml")
    classpath = next(row.attrib["value"] for row in report.findall("./properties/property") if row.get("name") == "java.class.path")
    jars = [str(root / "runtime-java/target/keir-runtime-java-0.1.0-SNAPSHOT.jar"),
            *(part for part in classpath.split(os.pathsep) if part.endswith(".jar"))]
    with tempfile.TemporaryDirectory() as directory:
        work = Path(directory)
        assert not work.resolve().is_relative_to(root.resolve())
        version = json.loads((root / "runtime-node/package.json").read_bytes())["version"]
        cases = verifier["guarded_smoke_inputs"](root, root / "compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar", work, version)
        guarded_count = len(cases)
        fixture = root / "conformance/keir-3"
        source = json.loads((fixture / "source-guarded-result.json").read_bytes())
        fields = source["return"]["record"]
        types = {"at": "Timestamp", "enabled": "Boolean", "flag": "Boolean", "message": "String", "state": "RecordState"}
        for field in sorted(types):
            name = "bound" + field[0].upper() + field[1:]
            source["steps"].append({"id": name, "let": fields[field]})
            fields[field] = {"cel": name}
        source_path = work / "scalar-bindings-source.json"
        source_path.write_text(json.dumps(source, ensure_ascii=False, sort_keys=True, separators=(",", ":")), encoding="utf-8")
        compiled = subprocess.run(["java", "-jar", str(root / "compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar"),
                                   str(source_path), str(fixture / "dcm-guarded-result.json")],
                                  cwd=work, capture_output=True, timeout=45)
        assert compiled.returncode == 0, compiled.stderr
        artifact = json.loads(compiled.stdout)
        assert len(artifact["steps"]) == 8 and artifact["compiler"] == f"keir-compiler-java/{version}"
        for field, kind in types.items():
            name = "bound" + field[0].upper() + field[1:]
            assert next(step["resultType"] for step in artifact["steps"] if step["id"] == name) == kind
        artifact_path = work / "scalar-bindings-artifact.json"; artifact_path.write_bytes(compiled.stdout)
        for name in ("true", "false"):
            scenario = json.loads((fixture / f"scenarios/guarded-result/{name}.json").read_bytes())
            trace = scenario["expect"]["outcome"]["trace"][:-2]
            for step in source["steps"][3:]:
                for event in ("STEP_START", "VALUE_BOUND"):
                    trace.append({"sequence": len(trace), "event": event, "step": step["id"]})
            for event in ("RETURNED", "TERMINATED"):
                trace.append({"sequence": len(trace), "event": event})
            scenario["expect"]["outcome"]["trace"] = trace
            cases.append({**cases[0], "artifact": str(artifact_path), "scenario": scenario})
        manifest = work / "cases.json"; manifest.write_text(json.dumps(cases), encoding="utf-8")
        java = work / "GuardedResultSmoke.java"; java.write_text(verifier["GUARDED_JAVA_SMOKE"], encoding="utf-8")
        node = work / "smoke.mjs"
        node.write_text(verifier["GUARDED_NODE_SMOKE"].replace("'@keir/runtime-node'", repr((root / "runtime-node/src/index.js").as_uri())), encoding="utf-8")
        outcomes = []
        commands = (["java", "-cp", os.pathsep.join(jars), str(java), str(manifest)], ["node", str(node), str(manifest)])
        for args in commands:
            result = subprocess.run(args, cwd=work, capture_output=True, text=True, timeout=45)
            assert result.returncode == 0, result.stderr
            rows = [json.loads(line) for line in result.stdout.splitlines()]
            assert rows == [row["scenario"]["expect"]["outcome"] for row in cases]
            outcomes.append(rows)
        assert outcomes[0] == outcomes[1] and len(outcomes[0]) == guarded_count + 2
        cases[0]["scenario"]["expect"]["outcome"]["classification"] = "WRONG"
        manifest.write_text(json.dumps(cases), encoding="utf-8")
        for args in commands:
            assert subprocess.run(args, cwd=work, capture_output=True, timeout=45).returncode != 0
        print(f"Source-build probes matched {len(outcomes[0])} complete outcomes per runtime and rejected wrong expectations; packages remain unqualified.")


if __name__ == "__main__":
    import sys
    if sys.argv[1:] == ["--source-probes"]:
        source_probe_check()
    else:
        unittest.main()
