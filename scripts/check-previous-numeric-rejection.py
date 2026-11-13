"""Hosted-only negative consumers of the authentic published 0.4 runtime packages."""
import hashlib
import json
import os
from pathlib import Path
import runpy
import subprocess
import sys
from xml.etree import ElementTree
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
PINS = {
    "keir-java-runtime-maven-0.4.0.zip": (86454, "63c07892b1d8e8107a87b8c18e49de664286a4640838e178630abdbe9c1bcec9"),
    "keir-runtime-node-0.4.0.tgz": (22047, "b3e9b7fe31437d2fa2795cccc4758ebf1184d0eca6d15cfa93898d2591a67962"),
}
JAVA_MAIN = r'''
  public static void main(String[] args) throws Exception {
    int[] calls = {0};
    for (Object raw : list(json(Files.readAllBytes(Path.of(args[0]))))) {
      var spec = map(raw); byte[] modelBytes = Files.readAllBytes(Path.of((String)spec.get("model")));
      var dcm = map(json(modelBytes)); var adapters = new HashMap<String,KeirRuntime.BoundEffectAdapter>();
      for (var entry : map(dcm.get("capabilities")).entrySet()) adapters.put(entry.getKey(),
        new KeirRuntime.BoundEffectAdapter((String)map(entry.getValue()).get("version"), "simulation",
          (String)map(dcm.get("hostKey")).get("type"), (a,p,r,c) -> { calls[0]++; throw new AssertionError("Old host dispatched a new profile"); }));
      var bound = KeirRuntime.bindModel(modelBytes, adapters);
      String code;
      if (spec.get("expected").equals("ADMISSION_INVALID")) { check(!bound.succeeded()); code = bound.diagnostic().code(); }
      else { check(bound.succeeded()); var result = KeirRuntime.prepareProgram(Files.readAllBytes(Path.of((String)spec.get("artifact"))), bound.boundModel());
        check(!result.succeeded()); code = result.diagnostic().code(); }
      if (!code.equals(spec.get("expected"))) throw new AssertionError("Old runtime expected " + spec.get("expected") + " but returned " + code);
      System.out.println(code);
    }
    check(calls[0] == 0);
  }
}
'''
NODE = r'''
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram } from '@keir/runtime-node';
assert.equal(JSON.parse(readFileSync(new URL('node_modules/@keir/runtime-node/package.json', import.meta.url))).version, '0.4.0');
let calls = 0;
for (const spec of JSON.parse(readFileSync(process.argv[2]))) {
  const modelBytes = readFileSync(spec.model), dcm = JSON.parse(modelBytes);
  const adapters = Object.fromEntries(Object.entries(dcm.capabilities).map(([id, cap]) => [id, {
    version: cap.version, mode: 'simulation', keyType: dcm.hostKey.type,
    invoke() { calls++; assert.fail('Old host dispatched a new profile'); },
  }]));
  const bound = bindModel(modelBytes, adapters);
  let code;
  if (spec.expected === 'ADMISSION_INVALID') { assert.equal(bound.boundModel, null); code = bound.diagnostic.code; }
  else { assert.equal(bound.diagnostic, null); const result = prepareProgram(readFileSync(spec.artifact), bound.boundModel);
    assert.equal(result.prepared, null); code = result.diagnostic.code; }
  assert.equal(code, spec.expected); console.log(code);
}
assert.equal(calls, 0);
'''


def main():
    work = Path(sys.argv[1]).resolve()
    assert work.is_dir() and not work.is_symlink()
    assert {p.name for p in work.iterdir()} == set(PINS)
    for name, (size, pin) in PINS.items():
        path = work / name
        assert path.is_file() and not path.is_symlink() and path.stat().st_size == size
        assert hashlib.sha256(path.read_bytes()).hexdigest() == pin
    with ZipFile(work / "keir-java-runtime-maven-0.4.0.zip") as bundle:
        runtime = work / "keir-runtime-java-0.4.0.jar"
        runtime.write_bytes(bundle.read("maven/dev/keir/keir-runtime-java/0.4.0/keir-runtime-java-0.4.0.jar"))
    # Reuse third-party dependencies, never current runtime or test/source classes.
    xml = ElementTree.parse(ROOT / "runtime-java/target/surefire-reports/TEST-dev.keir.runtime.NumericIntegrationTest.xml")
    classpath = xml.find(".//property[@name='java.class.path']").attrib["value"].split(os.pathsep)
    cache = Path(os.environ["MAVEN_USER_HOME"]).resolve() / "repository"
    jars = [Path(p).resolve() for p in classpath if Path(p).suffix == ".jar" and "keir-runtime-java-" not in Path(p).name]
    assert jars and all(p.is_relative_to(cache) and p.is_file() and "/dev/keir/" not in p.as_posix() for p in jars)
    verifier = runpy.run_path(str(ROOT / "scripts/verify-core-release.py"))
    prefix = verifier["GUARDED_JAVA_SMOKE"].split("  public static void main(String[] args) throws Exception {", 1)[0]
    (work / "GuardedResultSmoke.java").write_text(prefix + JAVA_MAIN, encoding="utf-8")
    (work / "previous.mjs").write_text(NODE, encoding="utf-8")
    rows = []
    for name, expected in (("host-boundaries", "ADMISSION_INVALID"), ("retained-names", "ARTIFACT_INVALID")):
        fixture = json.loads((ROOT / f"conformance/keir-4/{name}.json").read_bytes())
        model = work / f"{name}-model.json"; model.write_text(json.dumps(fixture["dcm"]), encoding="utf-8")
        artifact = work / f"{name}-artifact.json"
        artifact.write_bytes((ROOT / "conformance/keir-4/host-boundaries-artifact.json").read_bytes() if name == "host-boundaries"
                             else json.dumps(fixture["artifact"], sort_keys=True, separators=(",", ":")).encode())
        rows.append({"model": str(model), "artifact": str(artifact), "expected": expected})
    manifest = work / "cases.json"; manifest.write_text(json.dumps(rows), encoding="utf-8")
    subprocess.run(["npm", "install", "--prefix", str(work), "--cache", os.environ["NPM_CONFIG_CACHE"], "--ignore-scripts",
                    "--no-audit", "--no-fund", "--package-lock=false", str(work / "keir-runtime-node-0.4.0.tgz")], check=True, timeout=30)
    commands = [["java", "-Xmx256m", "-cp", os.pathsep.join(map(str, [runtime, *jars])), str(work / "GuardedResultSmoke.java"), str(manifest)],
                ["node", str(work / "previous.mjs"), str(manifest)]]
    for command in commands:
        result = subprocess.run(command, cwd=work, capture_output=True, text=True, timeout=30)
        assert len(result.stdout) + len(result.stderr) <= 65536
        if result.returncode:
            print(result.stdout, end=""); print(result.stderr, end="", file=sys.stderr)
            result.check_returncode()
        assert result.stdout.splitlines() == ["ADMISSION_INVALID", "ARTIFACT_INVALID"]
    print(json.dumps({"status": "passed", "runtimeVersion": "0.4.0", "profilesPerRuntime": 2, "callbacks": 0,
                      "assets": {name: pin for name, (_, pin) in PINS.items()}}))


if __name__ == "__main__":
    main()
