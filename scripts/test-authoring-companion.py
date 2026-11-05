"""Offline checks for the authoring companion command line."""

import hashlib
import json
import os
from pathlib import Path
import posixpath
import re
import runpy
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import warnings
import zipfile


SCRIPT = Path(__file__).with_name("authoring-companion.py")
V1_PIN = "sha256:8e365ff0d14fb0c479e43ace36e941562c94577f793a86b720d54e20622547b6"
V2_GUIDE_PIN = "sha256:22f3aba081b5653f7f5d4fff8861afe606c840ce56d9b26d7464ffc08ee8ab1d"


def command(*args):
    return subprocess.run([sys.executable, SCRIPT, *args], capture_output=True, text=True)


def git(*args):
    return subprocess.check_output(["git", *args], cwd=SCRIPT.parent.parent)


def digest(data):
    return "sha256:" + hashlib.sha256(data).hexdigest()


class AuthoringCompanionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        root = SCRIPT.resolve().parents[1]
        build = root / "build"
        build.mkdir(exist_ok=True)
        if build.resolve() != build:
            raise AssertionError("Test output directory must be inside the checkout.")
        directory = tempfile.TemporaryDirectory(dir=build)
        cls.addClassCleanup(directory.cleanup)
        cls.path = Path(directory.name) / "companion.zip"
        result = command("build", cls.path)
        if result.returncode:
            raise AssertionError(result.stderr)
        cls.data = cls.path.read_bytes()
        cls.pin = result.stdout.strip()

    def test_build_and_verify_exact_bytes(self):
        second = self.path.with_name("second.zip")
        result = command("build", second, "--version", "1")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), self.pin)
        self.assertEqual(second.read_bytes(), self.data)
        self.assertEqual(self.pin, digest(self.data))
        self.assertEqual(self.pin, V1_PIN)
        result = command("verify", self.path, "--sha256", self.pin)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), "Verified effect/4 authoring companion.")
        self.assertNotEqual(command("build", self.path).returncode, 0)
        self.assertEqual(self.path.read_bytes(), self.data)

        with zipfile.ZipFile(self.path) as archive:
            manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(len(archive.namelist()), 9)
            guide = archive.read("customer-authoring-guide-v1.md")
            self.assertEqual(digest(guide), "sha256:577055d5888c79735092a92588c377d7fab8815c6014a31e0421e8c09b0efe95")
            cases = json.loads(archive.read("examples/follow-up/scenarios.json"))
            self.assertEqual([case["id"] for case in cases], ["normal", "other-project", "lost-response"])
            for name, metadata in manifest["files"].items():
                data = archive.read(name)
                self.assertEqual(metadata, {"bytes": len(data), "digest": digest(data)})

    def test_v2_is_explicit_and_reproducible(self):
        path = self.path.with_name("v2.zip")
        second = self.path.with_name("v2-second.zip")
        for output in (path, second):
            result = command("build", output, "--version", "2")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(result.stdout.strip(), digest(output.read_bytes()))
        data = path.read_bytes()
        self.assertEqual(second.read_bytes(), data)
        pin = digest(data)
        self.assertNotEqual(pin, V1_PIN)
        result = command("verify", path, "--version", "2", "--sha256", pin)
        self.assertEqual(result.returncode, 0, result.stderr)

        with zipfile.ZipFile(self.path) as v1, zipfile.ZipFile(path) as v2:
            guide = "customer-authoring-guide-v2.md"
            self.assertEqual(digest(v2.read(guide)), V2_GUIDE_PIN)
            manifest = json.loads(v2.read("manifest.json"))
            self.assertEqual(manifest["format"], "keir-authoring-companion/2")
            self.assertEqual(manifest["compatibility"], json.loads(v1.read("manifest.json"))["compatibility"])
            for name in v2.namelist():
                if name not in (guide, "manifest.json"):
                    self.assertEqual(v2.read(name), v1.read(name), name)

        for archive, version, expected in ((path, "1", pin), (self.path, "2", V1_PIN)):
            with self.subTest(version=version):
                result = command("verify", archive, "--version", version, "--sha256", expected)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("inventory", result.stderr)
        self.assertNotEqual(command("verify", path, "--sha256", pin).returncode, 0)

        malformed = path.with_name("invalid-v2.zip")
        malformed.write_bytes(data[:8])
        result = command("verify", malformed, "--version", "2", "--sha256", pin)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("digest does not match", result.stderr)
        malformed.write_bytes(data + b"trailing bytes")
        result = command("verify", malformed, "--version", "2", "--sha256", digest(malformed.read_bytes()))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("manifest does not match", result.stderr)

    def test_requires_an_independent_pin(self):
        self.assertNotEqual(command("verify", self.path).returncode, 0)
        result = command("verify", self.path, "--sha256", digest(b"different archive"))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("digest does not match", result.stderr)

    def test_rejects_malformed_archives_with_matching_pins(self):
        bad = self.path.with_name("invalid.zip")
        for name in ("extra.txt", "customer-authoring-guide-v1.md"):
            with self.subTest(name=name):
                bad.write_bytes(self.data)
                with warnings.catch_warnings(), zipfile.ZipFile(bad, "a") as archive:
                    warnings.simplefilter("ignore", UserWarning)
                    archive.writestr(name, b"unexpected")
                result = command("verify", bad, "--sha256", digest(bad.read_bytes()))
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("inventory", result.stderr)

        with zipfile.ZipFile(self.path) as original, zipfile.ZipFile(bad, "w") as changed:
            for entry in original.infolist():
                data = original.read(entry)
                if entry.filename == "customer-authoring-guide-v1.md":
                    data += b"\n"
                changed.writestr(entry, data)
        result = command("verify", bad, "--sha256", digest(bad.read_bytes()))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("manifest does not match", result.stderr)

        for data in (self.data + b"trailing bytes", self.data[:-1], bytes(256 * 1024 + 1)):
            bad.write_bytes(data)
            self.assertNotEqual(command("verify", bad, "--sha256", digest(data)).returncode, 0)


class AuthoringCompanionV3Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        build = SCRIPT.resolve().parents[1] / "build"
        build.mkdir(exist_ok=True)
        directory = tempfile.TemporaryDirectory(dir=build)
        cls.addClassCleanup(directory.cleanup)
        cls.directory = Path(directory.name)
        cls.revision = git("rev-parse", "HEAD").decode().strip()
        cls.path = cls.directory / "companion-v3.zip"
        assets = os.environ.get("KEIR_COMPANION_CORE_ASSETS")
        cls.build_flags = ("--version", "3", "--revision", cls.revision, *(("--core-assets", assets) if assets else ()))
        result = command("build", cls.path, *cls.build_flags)
        if result.returncode:
            raise AssertionError(result.stderr)
        cls.data = cls.path.read_bytes()
        cls.pin = result.stdout.strip()

    def test_committed_inventory_links_and_offline_verification(self):
        second = self.directory / "repeated.zip"
        result = command("build", second, *self.build_flags)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(second.read_bytes(), self.data)
        self.assertEqual(result.stdout.strip(), self.pin)
        self.assertEqual(self.pin, digest(self.data))
        result = subprocess.run(
            [sys.executable, SCRIPT, "verify", self.path, "--version", "3", "--sha256", self.pin],
            cwd=self.directory, env={**os.environ, "PATH": ""}, capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr)

        with zipfile.ZipFile(self.path) as archive:
            manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(manifest["format"], "keir-authoring-companion/3")
            self.assertEqual(manifest["sourceRevision"], self.revision)
            self.assertEqual(manifest["compatibility"], json.loads(git(
                "show", f"{self.revision}:docs/agents/authoring-companion-v3-compatibility.json")))
            self.assertEqual(manifest["contracts"], {
                "language": "keir/1", "dcm": "keir-dcm/1",
                "artifact": "keir-artifact/1", "scenario": "keir-scenario/1",
            })
            guide = manifest["guide"]
            self.assertEqual(guide, "docs/agents/customer-authoring-guide-v3.md")
            links = re.findall(r"\]\(([^)]+)\)", archive.read(guide).decode("utf-8"))
            self.assertEqual(len(links), 10)
            resolved = {posixpath.normpath(posixpath.join(posixpath.dirname(guide), link)) for link in links}
            self.assertEqual(resolved, set(manifest["files"]) - {guide})
            self.assertEqual(archive.namelist(), sorted([*manifest["files"], "manifest.json"]))
            for name, metadata in manifest["files"].items():
                data = archive.read(name)
                self.assertEqual(data, git("show", f"{self.revision}:{name}"), name)
                self.assertEqual(metadata, {"bytes": len(data), "digest": digest(data)})
            artifact = json.loads(archive.read("conformance/keir-1/artifact.json"))
            self.assertEqual(manifest["producer"], artifact["compiler"])
            self.assertEqual(manifest["environment"], artifact["environment"])

    def test_rejects_wrong_pin_revision_and_archive_metadata(self):
        bad = self.directory / "invalid-v3.zip"
        self.assertNotEqual(command("build", bad, "--version", "3").returncode, 0)
        self.assertNotEqual(command("build", bad, "--version", "3", "--revision", "HEAD").returncode, 0)
        self.assertNotEqual(command("verify", self.path, "--version", "3", "--sha256", digest(b"old")).returncode, 0)
        self.assertNotEqual(command("verify", self.path, "--version", "2", "--sha256", self.pin).returncode, 0)
        bad.write_bytes(self.data)
        with zipfile.ZipFile(bad, "a") as archive:
            archive.writestr("extra.txt", b"unexpected")
        result = command("verify", bad, "--version", "3", "--sha256", digest(bad.read_bytes()))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("inventory", result.stderr)
        with zipfile.ZipFile(self.path) as original, zipfile.ZipFile(bad, "w") as changed:
            for entry in original.infolist():
                data = original.read(entry)
                if entry.filename == "manifest.json":
                    manifest = json.loads(data)
                    manifest["producer"] = "unrelated"
                    data = json.dumps(manifest).encode()
                changed.writestr(entry, data)
        result = command("verify", bad, "--version", "3", "--sha256", digest(bad.read_bytes()))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("manifest does not match", result.stderr)

    def test_release_record_requires_all_exact_assets(self):
        module = runpy.run_path(str(SCRIPT))
        release = {
            "status": "release", "coreRevision": self.revision,
            "packages": {
                "compiler": {"version": "0.1.0", "assets": {"keir-compiler.jar": digest(b"compiler")}},
                "javaRuntime": {"version": "0.1.0", "assets": {
                    "keir-runtime.jar": digest(b"java"), "keir-parent.pom": digest(b"pom")}},
                "nodeRuntime": {"version": "0.1.0", "assets": {"keir-runtime.tgz": digest(b"node")}},
            },
        }
        assets = self.directory / "core-assets"
        assets.mkdir()
        for name, value in (("keir-compiler.jar", b"compiler"), ("keir-runtime.jar", b"java"),
                            ("keir-parent.pom", b"pom"), ("keir-runtime.tgz", b"node")):
            (assets / name).write_bytes(value)
        with zipfile.ZipFile(self.path) as archive:
            payload = {name: archive.read(name) for name in module["V3_FILES"]}
        def read_blob(_revision, name):
            return json.dumps(release).encode() if name == module["V3_COMPATIBILITY"] else payload[name]

        build = module["build_v3"]
        with patch.dict(build.__globals__, read_blob=read_blob):
            with self.assertRaisesRegex(ValueError, "requires --core-assets"):
                build(self.revision, None)
            archive = self.directory / "release-v3.zip"
            archive.write_bytes(build(self.revision, assets))
        pin = digest(archive.read_bytes())
        for flags in ((), ("--core-assets", assets)):
            result = command("verify", archive, "--version", "3", "--sha256", pin, *flags)
            self.assertEqual(result.returncode, 0, result.stderr)
        (assets / "keir-parent.pom").write_bytes(b"changed")
        result = command("verify", archive, "--version", "3", "--sha256", pin, "--core-assets", assets)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Core asset digest does not match", result.stderr)
        del release["packages"]["nodeRuntime"]
        with self.assertRaisesRegex(ValueError, "all three"):
            module["validate_compatibility"](release)


if __name__ == "__main__":
    unittest.main()
