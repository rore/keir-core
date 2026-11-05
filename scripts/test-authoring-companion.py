"""Offline checks for the authoring companion command line."""

import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import warnings
import zipfile


SCRIPT = Path(__file__).with_name("authoring-companion.py")
V1_PIN = "sha256:8e365ff0d14fb0c479e43ace36e941562c94577f793a86b720d54e20622547b6"
V2_GUIDE_PIN = "sha256:22f3aba081b5653f7f5d4fff8861afe606c840ce56d9b26d7464ffc08ee8ab1d"


def command(*args):
    return subprocess.run([sys.executable, SCRIPT, *args], capture_output=True, text=True)


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


if __name__ == "__main__":
    unittest.main()
