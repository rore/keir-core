import hashlib
import json
import os
from pathlib import Path
import runpy
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch


builder = runpy.run_path(str(Path(__file__).with_name("release-core.py")))
build = builder["build"]


class ReleaseBuildTest(unittest.TestCase):
    def setUp(self):
        scratch = Path(__file__).resolve().parents[1] / ".tools"
        scratch.mkdir(exist_ok=True)
        temporary = tempfile.TemporaryDirectory(prefix="release-core-unit-", dir=scratch)
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.source = self.root / "source"
        self.output = self.root / "assets"
        self.output.mkdir()
        self.args = SimpleNamespace(revision="unit-only", version="0.4.0", offline=False,
                                    maven_repo=self.root / "maven/repository", npm_cache=self.root / "npm")
        self.coordinate = "dev.cel:cel:0.14.0"
        self.jar = self.args.maven_repo / "dev/cel/cel/0.14.0/cel-0.14.0.jar"
        self.jar_bytes = b"unit dependency bytes, not a real JAR"
        self.tree_entry = "dev.cel:cel:jar:0.14.0:compile"
        self.goals = []
        self.commands = []
        self.packaged = []
        reviewed = {}
        for name in builder["MAVEN_PACKAGING_FILES"]:
            path = self.source / name
            path.parent.mkdir(parents=True, exist_ok=True)
            if name.endswith(".xml"):
                data = b"<project><version>0.4.0</version></project>\n"
            elif name == builder["WRAPPER_PROPERTIES"]:
                data = b"distributionUrl=https://example.test/apache-maven-unit-bin.zip\n"
            else:
                data = b"unit wrapper marker, never executed\n"
            path.write_bytes(data)
            normalized = data.replace(b"<version>0.4.0</version>", b"<version>RELEASE</version>", 1)
            reviewed[name] = hashlib.sha256(normalized).hexdigest()
        (self.source / "LICENSE").write_bytes(b"unit license")
        self.notices = self.root / "notices"
        self.notices.mkdir()
        lock = {"maven": reviewed, "dependencies": {self.coordinate: hashlib.sha256(self.jar_bytes).hexdigest()}}
        (self.notices / "lock.json").write_text(json.dumps(lock), encoding="utf-8")
        (self.source / "compiler-java/dependency-tree.txt").write_text("stale tree", encoding="utf-8")

    def maven(self, command, cwd, **kwargs):
        # Maven/network boundary only: tree writes metadata, resolve supplies bytes.
        self.assertEqual(cwd, self.source)
        tree = self.source / "compiler-java/dependency-tree.txt"
        if not self.commands:
            self.assertFalse(tree.exists(), "Builder must discard the stale tree before Maven")
        self.commands.append(command)
        for goal in command:
            if goal not in ("dependency:resolve", "dependency:tree", "package"):
                continue
            self.goals.append(goal)
            if goal == "dependency:resolve":
                if "-o" in command:
                    raise subprocess.CalledProcessError(1, command, stderr="unit offline artifact unavailable")
                self.jar.parent.mkdir(parents=True, exist_ok=True)
                self.jar.write_bytes(self.jar_bytes)
                tree.write_text("unit resolved-artifact report\n", encoding="utf-8")
            elif goal == "dependency:tree":
                tree.write_text("dev.keir:compiler:jar:0.4.0\n\\- " + self.tree_entry + "\n", encoding="utf-8")
        return subprocess.CompletedProcess(command, 0)

    def java_package(self, source, output, version, dependencies, lock, license_bytes):
        self.assertEqual(dependencies, {self.coordinate: self.jar})
        self.packaged.append("java")
        paths = (output / f"keir-compiler-cli-{version}.zip", output / f"keir-java-runtime-maven-{version}.zip")
        for path in paths:
            path.write_bytes(b"unit Java packaging boundary")
        return paths

    def node_package(self, source, output, version, npm_cache, offline, license_bytes):
        self.packaged.append("node")
        path = output / f"keir-runtime-node-{version}.tgz"
        path.write_bytes(b"unit Node packaging boundary")
        return path

    def build(self):
        # Keep real build orchestration, Maven-input checks and dependency digests.
        with patch("subprocess.run", side_effect=AssertionError("Unexpected real external process")), patch.dict(build.__globals__, {
                "stage_source": lambda *args: self.source, "NOTICES": self.notices,
                "run": self.maven, "package_java": self.java_package, "package_node": self.node_package}):
            build(self.args, self.root, self.output)

    def test_cold_repository_resolves_artifacts_before_hashing(self):
        self.assertFalse(self.jar.exists())
        try:
            self.build()
        except FileNotFoundError as error:
            self.fail(f"Cold tree-only build hashed an unresolved dependency: {error}")
        self.assertIn("dependency:resolve", self.goals)
        self.assertEqual(self.packaged, ["java", "node"])
        sums = (self.output / "SHA256SUMS").read_text(encoding="ascii").splitlines()
        self.assertEqual(len(sums), 4)
        for line in sums:
            digest, name = line.split("  ")
            self.assertEqual(digest, hashlib.sha256((self.output / name).read_bytes()).hexdigest())
        self.assertEqual(len(list(self.output.iterdir())), 5)

    def test_rejects_unlocked_coordinates_before_packaging(self):
        self.tree_entry = "dev.cel:other:jar:0.14.0:compile"
        with self.assertRaisesRegex(ValueError, "differ from the reviewed notice inventory"):
            self.build()
        self.assertEqual(self.packaged, [])
        self.assertNotIn("package", self.goals)

    def test_rejects_changed_jar_bytes_before_packaging(self):
        self.jar_bytes += b"changed"
        with self.assertRaisesRegex(ValueError, "Unreviewed compiler dependency bytes: dev.cel:cel:0.14.0"):
            self.build()
        self.assertEqual(self.packaged, [])
        self.assertNotIn("package", self.goals)

    def test_offline_without_distribution_stops_before_maven(self):
        self.args.offline = True
        with self.assertRaisesRegex(ValueError, "cached copy of the pinned Maven distribution"):
            self.build()
        self.assertEqual(self.commands, [])
        self.assertEqual(self.packaged, [])

    def test_offline_missing_artifact_stops_without_online_fallback(self):
        self.args.offline = True
        marker = self.args.maven_repo.parent / "wrapper/dists/apache-maven-unit/unit/bin"
        marker.mkdir(parents=True)
        (marker / ("mvn.cmd" if os.name == "nt" else "mvn")).write_bytes(b"unit marker, never executed")
        with self.assertRaises(subprocess.CalledProcessError):
            self.build()
        self.assertEqual(self.goals, ["dependency:resolve"])
        self.assertTrue(all("-o" in command for command in self.commands))
        self.assertEqual(self.packaged, [])
        self.assertFalse(self.jar.exists())
        self.assertFalse((self.output / "SHA256SUMS").exists())


if __name__ == "__main__":
    unittest.main()
