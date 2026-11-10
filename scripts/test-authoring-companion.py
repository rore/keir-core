"""Offline checks for the authoring companion command line."""

import hashlib
from copy import deepcopy
import io
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
V2_PIN = "sha256:f073feaefe99e9dfe3b1dd818347f97cdca46c22a780bfb7e70992913aceac04"
V3_RELEASE_REVISION = "e298e7a4015ea527fca107972aaf2054fd77255b"
V4_RELEASE_REVISION = "c6a6d171982514569c29c06de7a429d301d25b2a"
V4_PIN = "sha256:4a9f4f30212ff1d722cf6c993902d0802bee7e7ffebc00304102a5964b5bec63"
V3_PIN = "sha256:fab92ec08409e9ccf64d5b4db5f58d874b6aabe0bc80be0a81182dc468a6ec39"
V5_RELEASE_REVISION = "f988c2b147933fcdd8b8f74ce7920f78acd4eca5"
V5_PIN = "sha256:20249307192494acb1cf07cc002e7203bda4f6baf8a8c2257dd752ce1d8cfe44"


def command(*args):
    return subprocess.run([sys.executable, SCRIPT, *args], capture_output=True, text=True)


def git(*args):
    if args[0] == "show" and ":" in args[1]:
        revision, path = args[1].split(":", 1)
        rows = json.loads(SCRIPT.with_name("release-source-revisions.json").read_bytes())["revisions"]
        revision = rows.get(revision, {}).get("reachableRevision", revision)
        args = (args[0], f"{revision}:{path}", *args[2:])
    return subprocess.check_output(["git", *args], cwd=SCRIPT.parent.parent)


def digest(data):
    return "sha256:" + hashlib.sha256(data).hexdigest()


def historical_pinned_archive(version, revision):
    # Reconstruct the historical byte recipe, not a qualified release build.
    # Production build_pinned still requires all original pinned Core assets.
    module = runpy.run_path(str(SCRIPT))
    compatibility_path, _, _, _, files = module["PINNED_EDITIONS"][version]
    payload = {name: module["read_blob"](revision, name) for name in files}
    compatibility = json.loads(module["read_blob"](revision, compatibility_path))
    return module["make_pinned_archive"](version, revision, compatibility, payload)


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
        self.assertEqual(pin, V2_PIN)
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
        cls.revision = V3_RELEASE_REVISION
        cls.path = cls.directory / "companion-v3.zip"
        assets = os.environ.get("KEIR_COMPANION_CORE_ASSETS")
        cls.data = historical_pinned_archive(3, cls.revision)
        cls.path.write_bytes(cls.data)
        cls.pin = digest(cls.data)
        if assets:
            live_path = cls.directory / "qualified-build.zip"
            result = command("build", live_path, "--version", "3", "--revision", cls.revision,
                             "--core-assets", assets)
            if result.returncode or live_path.read_bytes() != cls.data:
                raise AssertionError(result.stderr)

    def test_committed_inventory_links_and_offline_verification(self):
        self.assertEqual(historical_pinned_archive(3, self.revision), self.data)
        self.assertEqual(self.pin, V3_PIN)
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
        result = command("build", bad, "--version", "3", "--revision", self.revision)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("requires --core-assets", result.stderr)
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
        files = {
            "compiler": ("keir-compiler-cli-0.1.0.zip", b"compiler"),
            "javaRuntime": ("keir-java-runtime-maven-0.1.0.zip", b"java"),
            "nodeRuntime": ("keir-runtime-node-0.1.0.tgz", b"node"),
        }
        release = {
            "status": "release", "coreRevision": self.revision,
            "packages": {
                role: {"version": "0.1.0", "assets": {name: digest(value)}}
                for role, (name, value) in files.items()
            },
        }
        assets = self.directory / "core-assets"
        assets.mkdir()
        for name, value in files.values():
            (assets / name).write_bytes(value)
        with zipfile.ZipFile(self.path) as archive:
            payload = {name: archive.read(name) for name in module["V3_FILES"]}
        def read_blob(_revision, name):
            return json.dumps(release).encode() if name == module["V3_COMPATIBILITY"] else payload[name]

        build = module["build_pinned"]
        with patch.dict(build.__globals__, read_blob=read_blob):
            with self.assertRaisesRegex(ValueError, "requires --core-assets"):
                build(3, self.revision, None)
            archive = self.directory / "release-v3.zip"
            archive.write_bytes(build(3, self.revision, assets))
        pin = digest(archive.read_bytes())
        for flags in ((), ("--core-assets", assets)):
            result = command("verify", archive, "--version", "3", "--sha256", pin, *flags)
            self.assertEqual(result.returncode, 0, result.stderr)
        (assets / "keir-java-runtime-maven-0.1.0.zip").write_bytes(b"changed")
        result = command("verify", archive, "--version", "3", "--sha256", pin, "--core-assets", assets)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Core asset digest does not match", result.stderr)
        release["packages"]["javaRuntime"]["assets"] = {"keir-parent-0.1.0.pom": digest(b"pom")}
        with self.assertRaisesRegex(ValueError, "keir-java-runtime-maven"):
            module["validate_compatibility"](release)
        del release["packages"]["nodeRuntime"]
        with self.assertRaisesRegex(ValueError, "all three"):
            module["validate_compatibility"](release)


class AuthoringCompanionV4Test(unittest.TestCase):
    def test_published_archive_bytes_remain_exact(self):
        module = runpy.run_path(str(SCRIPT))
        payload = {name: module["read_blob"](V4_RELEASE_REVISION, name) for name in module["V4_FILES"]}
        compatibility = json.loads(module["read_blob"](V4_RELEASE_REVISION, module["V4_COMPATIBILITY"]))
        data = module["make_pinned_archive"](4, V4_RELEASE_REVISION, compatibility, payload)
        self.assertEqual(digest(data), V4_PIN)

    def test_inventory_and_exact_example_pins(self):
        module = runpy.run_path(str(SCRIPT))
        root = SCRIPT.resolve().parents[1]
        payload = {name: (root / name).read_bytes() for name in module["V4_FILES"]}
        revision = git("rev-parse", "HEAD").decode().strip()
        compatibility = {"status": "candidate", "coreRevision": revision, "packages": {}}
        make_archive = module["make_pinned_archive"]
        data = make_archive(4, revision, compatibility, payload)
        self.assertEqual(make_archive(4, revision, compatibility, payload), data)
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(manifest["format"], "keir-authoring-companion/4")
            self.assertEqual(manifest["contracts"]["scenario"], "keir-scenario/2")
            self.assertEqual(manifest["producer"], "keir-compiler-java/0.2.0")
            self.assertEqual(archive.namelist(), sorted([*payload, "manifest.json"]))
        payload[module["V4_MODEL"]] += b" "
        with self.assertRaisesRegex(ValueError, "example or compiler pin"):
            make_archive(4, revision, compatibility, payload)


class AuthoringCompanionV5Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.module = runpy.run_path(str(SCRIPT))
        root = SCRIPT.resolve().parents[1]
        build = root / "build"
        build.mkdir(exist_ok=True)
        directory = tempfile.TemporaryDirectory(dir=build)
        cls.addClassCleanup(directory.cleanup)
        cls.directory = Path(directory.name)
        cls.payload = {name: (root / name).read_bytes() for name in cls.module["V5_FILES"]}
        cls.revision = git("rev-parse", "HEAD").decode().strip()
        cls.compatibility = {"status": "candidate", "coreRevision": cls.revision, "packages": {}}
        cls.data = cls.module["make_pinned_archive"](5, cls.revision, cls.compatibility, cls.payload)
        cls.path = cls.directory / "v5.zip"
        cls.path.write_bytes(cls.data)

    def test_inventory_determinism_and_offline_verification(self):
        self.assertEqual(self.module["make_pinned_archive"](5, self.revision, self.compatibility, self.payload), self.data)
        self.assertLessEqual(len(self.data), self.module["MAX_ARCHIVE_BYTES"])
        with zipfile.ZipFile(self.path) as archive:
            manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(manifest["format"], "keir-authoring-companion/5")
            self.assertEqual(manifest["producer"], "keir-compiler-java/0.3.0")
            self.assertEqual(manifest["contracts"], {
                "language": "keir/2", "dcm": "keir-dcm/3", "artifact": "keir-artifact/3", "scenario": "keir-scenario/3",
            })
            self.assertEqual(manifest["compatibility"], self.compatibility)
            self.assertEqual(archive.namelist(), sorted([*self.payload, "manifest.json"]))
            for name, data in self.payload.items():
                self.assertEqual(archive.read(name), data)
                self.assertEqual(manifest["files"][name], {"bytes": len(data), "digest": digest(data)})
        result = subprocess.run(
            [sys.executable, SCRIPT, "verify", self.path, "--version", "5", "--sha256", digest(self.data)],
            cwd=self.directory, env={**os.environ, "PATH": ""}, capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotEqual(command("verify", self.path, "--version", "4", "--sha256", digest(self.data)).returncode, 0)
        changed = self.directory / "changed.zip"
        changed.write_bytes(self.data + b"trailing")
        for pin in (digest(self.data), digest(changed.read_bytes())):
            result = command("verify", changed, "--version", "5", "--sha256", pin)
            self.assertNotEqual(result.returncode, 0)

    def test_rejects_every_example_tuple_mismatch(self):
        module = self.module
        make_archive = module["make_pinned_archive"]
        stem = f'{module["V5_FIXTURES"]}/artifact-guard-input'
        for field, value in (
            ("format", "keir-artifact/2"), ("compiler", "keir-compiler-java/0.2.0"),
            ("modelRef.format", "keir-dcm/2"), ("modelRef.digest", digest(b"wrong model")),
            ("modelRef.descriptorRecipe", "keir-record-descriptors/1"),
            *((f"environment.{name}", "wrong") for name in ("celJava", "celNode", "protobufJava", "protobufNode")),
        ):
            with self.subTest(field=field):
                payload = dict(self.payload)
                artifact = json.loads(payload[f"{stem}.json"])
                target = artifact
                parts = field.split(".")
                for part in parts[:-1]:
                    target = target[part]
                target[parts[-1]] = value
                payload[f"{stem}.json"] = module["json_bytes"](artifact)
                payload[f"{stem}.sha256"] = digest(payload[f"{stem}.json"]).encode()
                with self.assertRaisesRegex(ValueError, "example or producer tuple"):
                    make_archive(5, self.revision, self.compatibility, payload)
        for path in (module["V5_MODEL"], f"{stem}.json"):
            payload = dict(self.payload)
            payload[path] += b" "
            with self.subTest(pin=path), self.assertRaises(ValueError):
                make_archive(5, self.revision, self.compatibility, payload)
        for path, field, value in (
            (f'{module["V5_FIXTURES"]}/source-guard.json', "language", "keir/1"),
            (module["V5_SCENARIOS"][0], "format", "keir-scenario/2"),
        ):
            payload = dict(self.payload)
            document = json.loads(payload[path])
            document[field] = value
            payload[path] = module["json_bytes"](document)
            with self.subTest(document=path), self.assertRaises(ValueError):
                make_archive(5, self.revision, self.compatibility, payload)
        payload = {**self.payload, module["V5_SCENARIOS"][0]: b"[]"}
        with self.assertRaisesRegex(ValueError, "scenario format"):
            make_archive(5, self.revision, self.compatibility, payload)

    def test_rejects_changed_source_with_unchanged_artifact(self):
        for case in self.module["V5_PROGRAMS"]:
            path = f'{self.module["V5_FIXTURES"]}/source-{case}.json'
            payload = dict(self.payload)
            source = json.loads(payload[path])
            source["name"] = "changed-without-recompilation"
            payload[path] = self.module["json_bytes"](source)
            with self.subTest(source=path), self.assertRaisesRegex(ValueError, "example or producer tuple"):
                self.module["make_pinned_archive"](5, self.revision, self.compatibility, payload)

    def test_build_provenance_and_exact_release_assets(self):
        module = self.module
        assets = self.directory / "assets"
        assets.mkdir()
        release = {"status": "release", "coreRevision": self.revision, "packages": {}}
        for role, template in module["RELEASE_ASSETS"].items():
            name = template.format("0.3.0")
            data = role.encode()
            (assets / name).write_bytes(data)
            release["packages"][role] = {"version": "0.3.0", "assets": {name: digest(data)}}
        for role, template in module["RELEASE_ASSETS"].items():
            record = deepcopy(release)
            record["packages"][role] = {"version": "0.2.0", "assets": {template.format("0.2.0"): digest(b"old")}}
            with self.subTest(package=role), self.assertRaisesRegex(ValueError, "Core 0.3.0"):
                module["make_pinned_archive"](5, self.revision, record, self.payload)

        def read_blob(_revision, name):
            return module["json_bytes"](release) if name == module["V5_COMPATIBILITY"] else self.payload[name]

        build = module["build_pinned"]
        with patch.dict(build.__globals__, read_blob=read_blob):
            with self.assertRaisesRegex(ValueError, "full Git commit"):
                build(5, "HEAD", None)
            blob = git("rev-parse", "HEAD:README.md").decode().strip()
            with self.assertRaisesRegex(ValueError, "must be a Git commit"):
                build(5, blob, None)
            with self.assertRaisesRegex(ValueError, "requires --core-assets"):
                build(5, self.revision, None)
            release_data = build(5, self.revision, assets)
            path = self.directory / "release.zip"
            path.write_bytes(release_data)
            module["verify"](5, path, digest(release_data), assets)
            release["coreRevision"] = V3_RELEASE_REVISION
            with self.assertRaisesRegex(ValueError, "inputs differ"):
                build(5, self.revision, assets)
            release["coreRevision"] = self.revision
            name = module["RELEASE_ASSETS"]["nodeRuntime"].format("0.3.0")
            (assets / name).write_bytes(b"changed")
            with self.assertRaisesRegex(ValueError, "digest does not match"):
                build(5, self.revision, assets)
            (assets / name).unlink()
            with self.assertRaises(FileNotFoundError):
                build(5, self.revision, assets)


class SourceRevisionTest(unittest.TestCase):
    def test_mapped_trees_and_original_archive_provenance(self):
        module = runpy.run_path(str(SCRIPT))
        rows = json.loads(SCRIPT.with_name("release-source-revisions.json").read_bytes())["revisions"]
        for original, row in rows.items():
            with self.subTest(original=original):
                self.assertEqual(module["source_revision"](original), row["reachableRevision"])
                self.assertEqual(git("rev-parse", row["reachableRevision"] + "^{tree}").decode().strip(), row["tree"])
        data = historical_pinned_archive(5, V5_RELEASE_REVISION)
        self.assertEqual(digest(data), V5_PIN)
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(manifest["sourceRevision"], V5_RELEASE_REVISION)
            self.assertEqual(manifest["compatibility"]["coreRevision"], "b2329440dd2a25acc547f3ebfea214748d912022")

    def test_unknown_missing_revision_and_changed_tree_receipt_rejected(self):
        module = runpy.run_path(str(SCRIPT))
        with self.assertRaisesRegex(ValueError, "unavailable in this clone"):
            module["source_revision"]("0" * 40)
        receipt = json.loads(SCRIPT.with_name("release-source-revisions.json").read_bytes())
        receipt["revisions"][V5_RELEASE_REVISION]["tree"] = "0" * 40
        with patch.object(Path, "read_bytes", return_value=json.dumps(receipt).encode()):
            with self.assertRaisesRegex(ValueError, "original tree"):
                module["source_revision"](V5_RELEASE_REVISION)


class AuthoringCompanionV6Test(unittest.TestCase):
    def test_license_and_existing_contracts_are_verified_offline(self):
        module = runpy.run_path(str(SCRIPT))
        root = SCRIPT.parent.parent
        files = module["PINNED_EDITIONS"][6][4]
        payload = {name: (root / name).read_bytes() for name in files}
        payload["LICENSE"] = git("show", "HEAD:LICENSE")
        revision = git("rev-parse", "HEAD").decode().strip()
        compatibility = {"status": "candidate", "coreRevision": revision, "packages": {}}
        data = module["make_pinned_archive"](6, revision, compatibility, payload)
        self.assertEqual(data, module["make_pinned_archive"](6, revision, compatibility, payload))
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            manifest = json.loads(archive.read("manifest.json"))
            self.assertEqual(manifest["format"], "keir-authoring-companion/6")
            self.assertEqual(archive.read("LICENSE"), git("show", "HEAD:LICENSE"))
            self.assertEqual(manifest["files"]["LICENSE"]["digest"], module["LICENSE_PIN"])
            self.assertEqual(manifest["contracts"], module["V5_FORMATS"])
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "candidate.zip"
            path.write_bytes(data)
            result = subprocess.run([sys.executable, SCRIPT, "verify", path, "--version", "6",
                                     "--sha256", digest(data)], env={**os.environ, "PATH": ""},
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            for license_data in (None, b"incomplete license"):
                with zipfile.ZipFile(io.BytesIO(data)) as original, zipfile.ZipFile(path, "w") as changed:
                    for entry in original.infolist():
                        value = original.read(entry)
                        if entry.filename == "LICENSE":
                            if license_data is None:
                                continue
                            value = license_data
                        changed.writestr(entry, value)
                with self.assertRaisesRegex(ValueError, "inventory|LICENSE"):
                    module["verify"](6, path, digest(path.read_bytes()))
        altered = dict(payload)
        altered["LICENSE"] = b"incomplete license"
        with self.assertRaisesRegex(ValueError, "LICENSE"):
            module["make_pinned_archive"](6, revision, compatibility, altered)
        altered = dict(payload)
        altered["conformance/keir-3/source-guard.json"] += b"\n"
        with self.assertRaisesRegex(ValueError, "example or producer tuple"):
            module["make_pinned_archive"](6, revision, compatibility, altered)


if __name__ == "__main__":
    unittest.main()
