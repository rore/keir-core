import hashlib
from io import BytesIO
from pathlib import Path
import runpy
import tempfile
import unittest
import warnings
from zipfile import ZipFile

from release_maven import maven_command

verifier = runpy.run_path(Path(__file__).with_name("verify-core-release.py"))
verify_assets = verifier["verify_assets"]


class ReleaseAssetTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
