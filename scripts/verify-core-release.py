#!/usr/bin/env python3
"""Verify a Core release asset set and optionally smoke-test its installed packages."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import tarfile
import tempfile
from zipfile import BadZipFile, ZipFile
from xml.etree import ElementTree

from release_maven import maven_command

REPO = Path(__file__).resolve().parents[1]
NOTICE_LOCK = Path(__file__).resolve().with_name("release-core-notices") / "lock.json"
OUTCOMES = [
    "task1 CREATED CONFIRMED | Follow up",
    "task2 NO_OP -",
    "task3 UNCERTAIN UNKNOWN",
]
DECISION_OUTCOMES = [
    "allow ALLOWED | native attempts=1",
    "deny DENIED | native attempts=0 | Operation restricted.",
    "unavailable BLOCKED | native attempts=0",
]


# Test-only public-API consumers, materialized in the isolated smoke directory.
GUARDED_NODE_SMOKE = r'''
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram, invokeProgram } from '@keir/runtime-node';
const cases = JSON.parse(readFileSync(process.argv[2]));
for (const row of cases) {
  const modelBytes = readFileSync(row.model), model = JSON.parse(modelBytes), scenario = row.scenario;
  const key = Object.freeze({}), calls = [];
  let now = 0, callbackError;
  const binding = bindModel(modelBytes, Object.fromEntries(Object.entries(model.capabilities).map(([id, capability]) => [id, {
    version: capability.version, mode: 'simulation', keyType: model.hostKey.type,
    invoke(args, protectedArgs, remaining, context) {
      try {
        const stub = scenario.stubs[calls.length];
        assert.equal(id, stub.capability); assert.equal(context.requestKey, key);
        assert.deepEqual(context.event, row.event); assert.ok(remaining > 0);
        assert.deepEqual({ ...args }, stub.args); assert.deepEqual({ ...protectedArgs }, stub.protectedArgs);
        calls.push({ step: stub.step, capability: id, version: capability.version,
          args: { ...args }, protectedArgs: { ...protectedArgs } });
        if (scenario.given.fixture === 'response-expired') now = 10000;
        return stub.response;
      } catch (error) { callbackError = error; throw error; }
    },
  }])));
  assert.equal(binding.diagnostic, null);
  const prepared = prepareProgram(readFileSync(row.artifact), binding.boundModel);
  assert.equal(prepared.diagnostic, null);
  const outcome = await invokeProgram(prepared.prepared, scenario.given.input, {
    bindings: row.bindings, requestKey: key, event: row.event,
    deadline: { clock: { nowMs: () => now }, expiresAtMs: 10000 },
  });
  if (callbackError) throw callbackError;
  assert.deepEqual(calls, scenario.expect.calls);
  assert.deepEqual(JSON.parse(JSON.stringify(outcome)), scenario.expect.outcome);
  console.log(JSON.stringify(outcome));
}
'''

GUARDED_JAVA_SMOKE = r'''
import dev.keir.runtime.KeirRuntime;
import dev.keir.runtime.KeirRuntime.*;
import com.fasterxml.jackson.core.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
class GuardedResultSmoke {
  static void check(boolean value) { if (!value) throw new AssertionError("Packaged outcome/context mismatch"); }
  @SuppressWarnings("unchecked") static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
  @SuppressWarnings("unchecked") static List<Object> list(Object value) { return (List<Object>)value; }
  static Object json(byte[] bytes) throws Exception {
    try (var parser = new JsonFactory().createParser(bytes)) { parser.nextToken(); return value(parser); }
  }
  static Object value(JsonParser parser) throws Exception {
    return switch (parser.currentToken()) {
      case START_OBJECT -> {
        var result = new LinkedHashMap<String,Object>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
          String key = parser.currentName(); parser.nextToken(); result.put(key, value(parser));
        }
        yield result;
      }
      case START_ARRAY -> {
        var result = new ArrayList<Object>();
        while (parser.nextToken() != JsonToken.END_ARRAY) result.add(value(parser));
        yield result;
      }
      case VALUE_STRING -> parser.getText();
      case VALUE_TRUE -> true;
      case VALUE_FALSE -> false;
      case VALUE_NUMBER_INT -> parser.getLongValue();
      default -> throw new AssertionError("Unexpected fixture token");
    };
  }
  public static void main(String[] args) throws Exception {
    for (Object raw : list(json(Files.readAllBytes(Path.of(args[0]))))) {
      var row = map(raw); var scenario = map(row.get("scenario")); var stubs = list(scenario.get("stubs"));
      byte[] modelBytes = Files.readAllBytes(Path.of((String)row.get("model"))); var model = map(json(modelBytes));
      var key = new Object(); var calls = new ArrayList<Object>(); long[] now = {0};
      var adapters = new LinkedHashMap<String,BoundEffectAdapter>();
      for (var entry : map(model.get("capabilities")).entrySet()) {
        String id = entry.getKey(); var capability = map(entry.getValue());
        adapters.put(id, new BoundEffectAdapter((String)capability.get("version"), "simulation",
          (String)map(model.get("hostKey")).get("type"), (business, protectedArgs, remaining, context) -> {
            var stub = map(stubs.get(calls.size()));
            check(id.equals(stub.get("capability")) && context.requestKey() == key && remaining > 0);
            check(context.event().equals(row.get("event")) && business.equals(stub.get("args"))
              && protectedArgs.equals(stub.get("protectedArgs")));
            calls.add(Map.of("step", stub.get("step"), "capability", id, "version", capability.get("version"),
              "args", business, "protectedArgs", protectedArgs));
            if ("response-expired".equals(map(scenario.get("given")).get("fixture"))) now[0] = 10000;
            return CompletableFuture.completedFuture(stub.get("response"));
          }));
      }
      var bound = KeirRuntime.bindModel(modelBytes, adapters); check(bound.succeeded());
      var prepared = KeirRuntime.prepareProgram(Files.readAllBytes(Path.of((String)row.get("artifact"))), bound.boundModel());
      check(prepared.succeeded());
      var outcome = KeirRuntime.invokeBoundEffectProgram(prepared.prepared(), map(scenario.get("given")).get("input"),
        new BoundEffectInvocation(map(row.get("bindings")), new Deadline(() -> now[0], 10000), key, map(row.get("event"))))
        .toCompletableFuture().join();
      check(calls.equals(map(scenario.get("expect")).get("calls")));
      if (!json(outcome.toJson()).equals(map(scenario.get("expect")).get("outcome")))
        throw new AssertionError("Unexpected complete outcome for " + scenario.get("id") + ": "
          + new String(outcome.toJson(), java.nio.charset.StandardCharsets.UTF_8));
      System.out.write(outcome.toJson()); System.out.println();
    }
  }
}
'''


def guarded_smoke_inputs(source, compiler, work):
    """Compile new inputs once; retain exact historical artifacts and full expectations."""
    fixture = source / "conformance/keir-3"
    cases = []
    for group in ("guarded-result", "guarded-result-read", "guarded-result-handler", "guarded-result-unused"):
        model_name = "dcm-guarded-result-read" if group.endswith("-read") else "dcm-guarded-result"
        artifact = work / f"{group}.json"
        produced = run(["java", "-jar", str(compiler), str(fixture / f"source-{group}.json"),
                        str(fixture / f"{model_name}.json")], cwd=work, capture_output=True).stdout
        expected = (fixture / f"artifact-{group}.json").read_bytes()
        pin = (fixture / f"artifact-{group}.sha256").read_text().strip()
        if (produced != expected or "sha256:" + hashlib.sha256(produced).hexdigest() != pin
                or json.loads(produced)["compiler"] != "keir-compiler-java/0.4.0"):
            raise ValueError("Packaged compiler differs from the guarded-result fixture")
        artifact.write_bytes(produced)
        scenarios = sorted((fixture / "scenarios" / group).glob("*.json"))
        if not scenarios:
            raise ValueError("Missing required guarded-result package scenarios")
        for scenario in scenarios:
            cases.append({"model": str(fixture / f"{model_name}.json"), "artifact": str(artifact),
                          "scenario": json.loads(scenario.read_bytes())})
    if len(cases) != 15:
        raise ValueError("Guarded-result package scenario inventory changed")
    for revision, artifact, model, scenario in (
            (2, "artifact", "dcm", "main/normal"),
            (3, "artifact", "dcm", "main/normal"),
            (3, "artifact-guard", "dcm-guard", "guard/guard-deny"),
            (3, "compatibility/0.3.0/event", "dcm", "main/normal"),
            (3, "compatibility/0.3.0/decision", "dcm-guard", "guard-input/guard-input-allow")):
        corpus = source / f"conformance/keir-{revision}"
        cases.append({"model": str(corpus / f"{model}.json"), "artifact": str(corpus / f"{artifact}.json"),
                      "scenario": json.loads((corpus / f"scenarios/{scenario}.json").read_bytes())})
    for row in cases:
        model = json.loads(Path(row["model"]).read_bytes())
        artifact = json.loads(Path(row["artifact"]).read_bytes())
        decision = model["entrypoints"][artifact["entrypoint"]]["consumer"]["kind"] == "DECISION"
        protected = {name for step in artifact["steps"] if step["op"] == "CALL"
                     for name, parameter in model["capabilities"][step["capability"]]["parameters"].items()
                     if parameter["binding"] == "host"}
        if protected - {"actorId"}:
            raise ValueError("Smoke fixture needs an explicit protected-value mapping")
        row["bindings"] = {name: "user1" for name in protected}
        row["event"] = {"id": "attempt1", "source": "scope1"} if decision else {"id": "event1", "source": "task1"}
    return cases


def verify_assets(version, assets, checksums):
    manifest = checksums.read_bytes()
    expected = {
        "LICENSE",
        f"keir-compiler-cli-{version}.zip",
        f"keir-java-runtime-maven-{version}.zip",
        f"keir-runtime-node-{version}.tgz",
    }
    pins = {}
    for line in manifest.decode("ascii").splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  ([A-Za-z0-9_.-]+)", line)
        if not match:
            raise ValueError("Invalid SHA256SUMS entry")
        digest, name = match.groups()
        if name not in expected or name in pins:
            raise ValueError(f"Unexpected or duplicate checksum entry: {name}")
        pins[name] = digest
    if pins.keys() != expected or {path.name for path in assets.iterdir()} != expected | {"SHA256SUMS"}:
        raise ValueError("Release asset inventory differs from the expected set")
    if (assets / "SHA256SUMS").read_bytes() != manifest:
        raise ValueError("Release SHA256SUMS differs from the trusted manifest")
    for name, digest in pins.items():
        if hashlib.sha256((assets / name).read_bytes()).hexdigest() != digest:
            raise ValueError(f"Release digest mismatch: {name}")


def run(args, cwd=REPO, **kwargs):
    return subprocess.run(args, cwd=cwd, check=True, **kwargs)


def check_zip(archive, required, *, notices=False):
    names = set()
    for member in archive.infolist():
        name = member.filename
        mode = stat.S_IFMT(member.external_attr >> 16)
        if (name != member.orig_filename or name in names or mode not in (0, stat.S_IFREG) or "\\" in name or ":" in name
                or any(part in ("", ".", "..") for part in name.split("/"))):
            raise ValueError(f"Invalid ZIP member: {name}")
        names.add(name)
    extra = names - required
    if required - names or (extra and not (notices and all(name.startswith("third-party/") for name in extra))):
        raise ValueError("ZIP contents differ from the expected release layout")


def check_compiler_notices(archive, compiler_name):
    lock = json.loads(NOTICE_LOCK.read_bytes())["upstream"]
    notices = {f"third-party/{name}": info["sha256"] for name, info in lock.items()}
    check_zip(archive, {compiler_name, "LICENSE", "THIRD-PARTY.txt", *notices}, notices=True)
    for name, expected in notices.items():
        if hashlib.sha256(archive.read(name)).hexdigest() != expected:
            raise ValueError(f"Compiler ZIP notice differs from the reviewed bytes: {name}")


def check_maven_coordinates(parent, runtime, version):
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    parent_xml, runtime_xml = (ElementTree.fromstring(data) for data in (parent, runtime))
    expected_parent = ["dev.keir", "keir-parent", version]
    fields = ("groupId", "artifactId", "version")
    if ([parent_xml.findtext(f"m:{field}", namespaces=namespace) for field in fields] != expected_parent
            or [runtime_xml.findtext(f"m:parent/m:{field}", namespaces=namespace) for field in fields] != expected_parent
            or runtime_xml.findtext("m:artifactId", namespaces=namespace) != "keir-runtime-java"
            or runtime_xml.findtext("m:groupId", "dev.keir", namespace) != "dev.keir"
            or runtime_xml.findtext("m:version", version, namespace) != version):
        raise ValueError("Packaged Maven coordinates differ from the release version")


def smoke(version, assets, revision, maven_repo, npm_cache, offline):
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Use a full Git commit ID for --smoke-revision")
    git = ["git", "--no-replace-objects"]
    if run([*git, "cat-file", "-t", revision], capture_output=True, text=True).stdout.strip() != "commit":
        raise ValueError("Smoke revision is not a Git commit")
    with tempfile.TemporaryDirectory(prefix="keir-release-smoke-") as directory:
        work = Path(directory)
        if work.resolve().is_relative_to(REPO.resolve()):
            raise ValueError("Package smoke must run outside the source checkout")
        source = work / "source"
        source_tar = work / "source.tar"
        run([*git, "archive", "--format=tar", f"--output={source_tar}", revision])
        with tarfile.open(source_tar) as archive:
            archive.extractall(source, filter="data")

        compiler_name = f"keir-compiler-java-{version}-cli.jar"
        compiler = work / compiler_name
        license_bytes = (assets / "LICENSE").read_bytes()
        with ZipFile(assets / f"keir-compiler-cli-{version}.zip") as archive:
            check_compiler_notices(archive, compiler_name)
            if archive.read("LICENSE") != license_bytes:
                raise ValueError("Compiler ZIP license differs from the release license")
            compiler.write_bytes(archive.read(compiler_name))
        example = source / "examples" / "node-host"
        programs = [("follow-up", "FollowUp.java", OUTCOMES)]
        if json.loads((example / "follow-up/dcm.json").read_bytes())["format"] == "keir-dcm/3":
            programs.append(("pre-operation", "BeforeOperation.java", DECISION_OUTCOMES))
        artifacts = {}
        for name, _, _ in programs:
            artifacts[name] = work / f"{name}-artifact.json"
            with artifacts[name].open("wb") as output:
                run(["java", "-jar", str(compiler), str(example / name / "source.yaml"),
                     str(example / name / "dcm.json")], cwd=source, stdout=output)

        runtime_name = f"keir-runtime-java-{version}.jar"
        runtime = work / runtime_name
        runtime_pom = source / "runtime-java" / "pom.xml"
        with ZipFile(assets / f"keir-java-runtime-maven-{version}.zip") as archive:
            prefix = f"maven/dev/keir/keir-runtime-java/{version}"
            jar_path = f"{prefix}/{runtime_name}"
            pom_path = f"{prefix}/keir-runtime-java-{version}.pom"
            parent_path = f"maven/dev/keir/keir-parent/{version}/keir-parent-{version}.pom"
            check_zip(archive, {"LICENSE", jar_path, pom_path, parent_path})
            if archive.read("LICENSE") != license_bytes:
                raise ValueError("Maven ZIP license differs from the release license")
            parent_bytes, pom_bytes = archive.read(parent_path), archive.read(pom_path)
            check_maven_coordinates(parent_bytes, pom_bytes, version)
            (source / "pom.xml").write_bytes(parent_bytes)
            runtime_pom.write_bytes(pom_bytes)
            runtime.write_bytes(archive.read(jar_path))
        classpath = work / "classpath.txt"
        maven, maven_env = maven_command(source, maven_repo, offline)
        run([*maven, "-f", str(runtime_pom), "dependency:build-classpath", "-DincludeScope=runtime",
             f"-Dmdep.outputFile={classpath}"], cwd=source, env=maven_env)
        java_results = {}
        for name, launcher, _ in programs:
            java_results[name] = run(["java", "-cp", os.pathsep.join([str(runtime), classpath.read_text().strip()]),
                                      str(example / name / launcher), str(artifacts[name])],
                                     cwd=source, capture_output=True, text=True)

        package_name = f"keir-runtime-node-{version}.tgz"
        package = assets / package_name
        package_dir = source / ".tools" / "packages"
        package_dir.mkdir(parents=True)
        shutil.copyfile(package, package_dir / package_name)
        lock_path = example / "package-lock.json"
        lock = json.loads(lock_path.read_bytes())
        locked_runtime = lock["packages"]["node_modules/@keir/runtime-node"]
        if locked_runtime["resolved"] != f"file:../../.tools/packages/{package_name}" or locked_runtime["version"] != version:
            raise ValueError("The teaching host lock does not select this runtime release")
        integrity = base64.b64encode(hashlib.sha512(package.read_bytes()).digest()).decode("ascii")
        locked_runtime["integrity"] = f"sha512-{integrity}"
        lock_path.write_text(json.dumps(lock), encoding="utf-8")
        npm = "npm.cmd" if os.name == "nt" else "npm"
        run([npm, "ci", *(["--offline"] if offline else []), "--cache", str(npm_cache),
             "--ignore-scripts", "--no-audit", "--no-fund"], cwd=example)
        for name, _, expected in programs:
            node_result = run(["node", str(example / name / "run.mjs"), str(artifacts[name])],
                              cwd=source, capture_output=True, text=True)
            for label, result in (("Java", java_results[name]), ("Node", node_result)):
                if result.stdout.splitlines() != expected:
                    raise ValueError(f"{label} package produced unexpected {name} outcomes: {result.stdout!r}")

        if version == "0.4.0":
            cases = guarded_smoke_inputs(source, compiler, work)
            manifest = work / "guarded-cases.json"
            manifest.write_text(json.dumps(cases), encoding="utf-8")
            java_probe = work / "GuardedResultSmoke.java"
            java_probe.write_text(GUARDED_JAVA_SMOKE, encoding="utf-8")
            node_probe = example / "guarded-result-smoke.mjs"
            node_probe.write_text(GUARDED_NODE_SMOKE, encoding="utf-8")
            java_result = run(["java", "-cp", os.pathsep.join([str(runtime), classpath.read_text().strip()]),
                               str(java_probe), str(manifest)], cwd=work, capture_output=True, text=True, timeout=45)
            node_result = run(["node", str(node_probe), str(manifest)], cwd=work,
                              capture_output=True, text=True, timeout=45)
            java_rows = [json.loads(line) for line in java_result.stdout.splitlines()]
            node_rows = [json.loads(line) for line in node_result.stdout.splitlines()]
            if java_rows != node_rows or java_rows != [row["scenario"]["expect"]["outcome"] for row in cases]:
                raise ValueError("Packaged guarded-result complete outcomes differ")
            print(f"Packaged guarded-result and historical smoke matched {len(cases)} complete outcomes per runtime.", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--assets", required=True, type=Path)
    parser.add_argument("--checksums", type=Path, help="Trusted SHA256SUMS from the staged build")
    parser.add_argument("--smoke-revision", help="Full source commit for the packaged host examples")
    parser.add_argument("--maven-repo", type=Path)
    parser.add_argument("--npm-cache", type=Path)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", args.version):
        parser.error("--version must be major.minor.patch")
    if args.smoke_revision and not (args.maven_repo and args.npm_cache):
        parser.error("--smoke-revision requires --maven-repo and --npm-cache")
    assets = args.assets.resolve()
    try:
        verify_assets(args.version, assets, args.checksums or assets / "SHA256SUMS")
        print("Verified Core asset inventory and checksums.", flush=True)
        if args.smoke_revision:
            smoke(args.version, assets, args.smoke_revision,
                  args.maven_repo.resolve(), args.npm_cache.resolve(), args.offline)
            print("Packaged compiler, Java and Node passed the host example smoke tests.")
    except subprocess.CalledProcessError as error:
        if error.stderr:
            print(error.stderr, file=sys.stderr)
        parser.exit(1, f"{error}\n")
    except (OSError, KeyError, ValueError, BadZipFile, tarfile.TarError, ElementTree.ParseError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
