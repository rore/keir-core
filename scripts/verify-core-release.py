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
      case VALUE_NULL -> null;
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

NUMERIC_NODE_CHECKS = r'''
for (const spec of JSON.parse(readFileSync(process.argv[3])).normalization) {
  const input = Object.hasOwn(spec, 'repeat') ? spec.repeat.repeat(spec.count) : spec.input;
  const normalize = spec.kind === 'integer' ? normalizeInteger : normalizeDecimal;
  if (spec.expected === 'ERROR') assert.throws(() => normalize(input), TypeError);
  else assert.equal(normalize(input), spec.expected);
}
'''

NUMERIC_JAVA_CHECKS = r'''
    for (Object raw : list(map(json(Files.readAllBytes(Path.of(args[1])))).get("normalization"))) {
      var spec = map(raw);
      Object input = spec.containsKey("repeat") ? ((String)spec.get("repeat")).repeat(((Number)spec.get("count")).intValue()) : spec.get("input");
      // Java has only String overloads: a numeric argument is rejected by the Java type checker.
      if (input != null && !(input instanceof String)) { check("ERROR".equals(spec.get("expected"))); continue; }
      try {
        String result = "integer".equals(spec.get("kind")) ? KeirRuntime.normalizeInteger((String)input) : KeirRuntime.normalizeDecimal((String)input);
        check(result.equals(spec.get("expected")));
      } catch (IllegalArgumentException invalid) { check("ERROR".equals(spec.get("expected"))); }
    }
'''


def guarded_smoke_inputs(source, compiler, work, version="0.4.0"):
    """Compile new inputs once; retain exact historical artifacts and full expectations."""
    fixture = source / "conformance/keir-3"
    cases = []
    for group in ("guarded-result", "guarded-result-read", "guarded-result-handler", "guarded-result-unused"):
        model_name = "dcm-guarded-result-read" if group.endswith("-read") else "dcm-guarded-result"
        artifact = work / f"{group}.json"
        produced = run(["java", "-jar", str(compiler), str(fixture / f"source-{group}.json"),
                        str(fixture / f"{model_name}.json")], cwd=work, capture_output=True, timeout=30).stdout
        historical = (fixture / f"artifact-{group}.json").read_bytes()
        pin = (fixture / f"artifact-{group}.sha256").read_text().strip()
        if "sha256:" + hashlib.sha256(historical).hexdigest() != pin:
            raise ValueError("Historical guarded-result fixture pin differs")
        expected = historical
        if version == "0.5.0":
            retained = json.loads(historical)
            retained.update(format="keir-artifact/4", compiler="keir-compiler-java/0.5.0")
            expected = canonical(retained)
        if produced != expected or json.loads(produced)["compiler"] != f"keir-compiler-java/{version}":
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
    historical_cases = [
            (2, "artifact", "dcm", "main/normal"),
            (3, "artifact", "dcm", "main/normal"),
            (3, "artifact-guard", "dcm-guard", "guard/guard-deny"),
            (3, "compatibility/0.3.0/event", "dcm", "main/normal"),
            (3, "compatibility/0.3.0/decision", "dcm-guard", "guard-input/guard-input-allow")]
    if version == "0.5.0":
        historical_cases.append((3, "artifact-guarded-result", "dcm-guarded-result", "guarded-result/true"))
    for revision, artifact, model, scenario in historical_cases:
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


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def pinned(directory, name):
    data = (directory / f"{name}.json").read_bytes()
    if (directory / f"{name}.sha256").read_text().strip() != "sha256:" + hashlib.sha256(data).hexdigest():
        raise ValueError(f"Numeric fixture pin differs: {name}")
    return data


def numeric_smoke_inputs(source, compiler, work):
    """Reuse frozen compiler bytes and independent oracles; never import a source runtime."""
    corpus = source / "conformance/keir-4"
    cases = []

    def row(name, model, artifact, input_value, outcome, *, stubs=(), bindings=None, event_source="owner1"):
        model_path, artifact_path = work / f"{name}-model.json", work / f"{name}-artifact.json"
        model_path.write_bytes(canonical(model)); artifact_path.write_bytes(canonical(artifact))
        return {"model": str(model_path), "artifact": str(artifact_path), "bindings": bindings or {},
                "event": {"id": "event1", "source": event_source},
                "scenario": {"id": name, "given": {"input": input_value, "fixture": "normal"}, "stubs": list(stubs),
                             "expect": {"calls": [{key: value for key, value in stub.items() if key != "response"} for stub in stubs],
                                        "outcome": outcome}}}

    def compile_exact(name, model, source_value, expected):
        model_path, source_path = work / f"{name}-model.json", work / f"{name}-source.json"
        model_path.write_bytes(canonical(model)); source_path.write_bytes(canonical(source_value))
        produced = run(["java", "-jar", str(compiler), str(source_path), str(model_path)],
                       cwd=work, capture_output=True, timeout=30).stdout
        if produced != expected:
            raise ValueError(f"Packaged compiler differs from numeric fixture: {name}")
        return json.loads(produced)

    matrix = json.loads(pinned(corpus, "engine-matrix"))["cases"]
    if len(matrix) != 97:
        raise ValueError("Numeric engine matrix inventory changed")
    for spec in matrix:
        # Same independent complete-trace oracle used by both runtime integration suites.
        ids = ["a", "b", "i", "j", "flag", "text"] + (["d"] if spec["id"] in ("bind", "expression-only-size") else [])
        trace = [{"event": "INVOCATION_START"}]
        for name in ids:
            trace.extend(({"event": "STEP_START", "step": name}, {"event": "VALUE_BOUND", "step": name}))
        outcome = {"classification": "COMPLETED", "result": {"value": spec["expected"]}, "effects": []}
        if spec["expected"] in ("ERROR", "TIME_ERROR"):
            code = "BUDGET_EXCEEDED" if spec["expected"] == "TIME_ERROR" else "EXPRESSION_FAILED"
            outcome = {"classification": "LIMIT_EXCEEDED" if code == "BUDGET_EXCEEDED" else "FAILED",
                       "error": {"code": code, "path": "/return"}, "effects": []}
            trace.append({"event": "TERMINATED", "code": code})
        else:
            trace.extend(({"event": "RETURNED"}, {"event": "TERMINATED"}))
        outcome["trace"] = [{"sequence": i, **event} for i, event in enumerate(trace)]
        cases.append(row("matrix-" + spec["id"], spec["dcm"], spec["artifact"], spec["input"], outcome))

    retained = json.loads(pinned(corpus, "retained-names"))
    authoring = {"language": "keir/2", "domain": {**retained["dcm"]["domain"], "fingerprint": retained["artifact"]["modelRef"]["digest"]},
                 "entrypoint": "Run", "name": "numeric-rule", "steps": [
                     {"id": "value", "let": {"literal": "not-a-number", "type": "Integer"}}],
                 "return": {"record": {"value": {"cel": "value == input.integer"}}}}
    compile_exact("retained-names", retained["dcm"], authoring, canonical(retained["artifact"]))
    trace = [{"event": "INVOCATION_START"}, {"event": "STEP_START", "step": "value"},
             {"event": "VALUE_BOUND", "step": "value"}, {"event": "RETURNED"}, {"event": "TERMINATED"}]
    cases.append(row("retained-names", retained["dcm"], retained["artifact"], retained["input"],
                     {"classification": "COMPLETED", "result": {"value": True}, "effects": [],
                      "trace": [{"sequence": i, **event} for i, event in enumerate(trace)]}))

    for name in ("stock", "quote"):
        directory = source / "examples/numeric" / name
        model = json.loads(pinned(directory, "dcm"))
        artifact = compile_exact(name, model, json.loads((directory / "source.json").read_bytes()), pinned(directory, "artifact"))
        scenario = json.loads((directory / "scenarios/normal.json").read_bytes())
        entry = row(name, model, artifact, scenario["given"]["input"], scenario["expect"]["outcome"],
                    stubs=scenario["stubs"], bindings={"actorId": "user1"} if name == "stock" else {},
                    event_source="item1" if name == "stock" else "quote1")
        if name == "quote":
            entry["event"]["id"] = "attempt1"
        cases.append(entry)

    boundary = json.loads((corpus / "host-boundaries.json").read_bytes())
    artifact = compile_exact("boundaries", boundary["dcm"], boundary["source"], pinned(corpus, "host-boundaries-artifact"))
    for spec in boundary["cases"]:
        stubs = []
        for step, capability, args, response in (
                ("read", "numbers.read", {key: boundary["input"][key] for key in ("integer", "decimal")}, spec.get("read", boundary["read"])),
                ("written", "numbers.write", spec.get("args", {}), spec.get("write", boundary["write"]))):
            if len(stubs) < spec["calls"]:
                stubs.append({"step": step, "capability": capability, "version": "1", "args": args,
                              "protectedArgs": boundary["bindings"], "response": response})
        outcome = {**spec["expect"], "trace": [{"sequence": i, **event} for i, event in enumerate(boundary["traces"][spec["trace"]])]}
        cases.append(row("boundary-" + spec["id"], boundary["dcm"], artifact, spec.get("input", boundary["input"]), outcome,
                         stubs=stubs, bindings=spec.get("bindings", boundary["bindings"])))
    if len(cases) != 109:
        raise ValueError("Numeric installed-consumer inventory changed")
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

        if version in ("0.4.0", "0.5.0"):
            cases = guarded_smoke_inputs(source, compiler, work, version)
            if version == "0.5.0":
                cases += numeric_smoke_inputs(source, compiler, work)
            manifest = work / "guarded-cases.json"
            manifest.write_text(json.dumps(cases), encoding="utf-8")
            java_source, node_source = GUARDED_JAVA_SMOKE, GUARDED_NODE_SMOKE
            extra_args = []
            if version == "0.5.0":
                java_source = java_source.replace('  public static void main(String[] args) throws Exception {',
                                                  '  public static void main(String[] args) throws Exception {' + NUMERIC_JAVA_CHECKS)
                node_source = node_source.replace('invokeProgram }', 'invokeProgram, normalizeInteger, normalizeDecimal }') + NUMERIC_NODE_CHECKS
                extra_args = [str(source / "conformance/numeric-values.json")]
            java_probe = work / "GuardedResultSmoke.java"
            java_probe.write_text(java_source, encoding="utf-8")
            node_probe = example / "guarded-result-smoke.mjs"
            node_probe.write_text(node_source, encoding="utf-8")
            java_result = run(["java", "-cp", os.pathsep.join([str(runtime), classpath.read_text().strip()]),
                               str(java_probe), str(manifest), *extra_args], cwd=work, capture_output=True, text=True, timeout=45)
            node_result = run(["node", str(node_probe), str(manifest), *extra_args], cwd=work,
                              capture_output=True, text=True, timeout=45)
            java_rows = [json.loads(line) for line in java_result.stdout.splitlines()]
            node_rows = [json.loads(line) for line in node_result.stdout.splitlines()]
            if java_rows != node_rows or java_rows != [row["scenario"]["expect"]["outcome"] for row in cases]:
                raise ValueError("Packaged numeric/guarded/historical complete outcomes differ")
            print(f"Packaged numeric/guarded/historical smoke matched {len(cases)} complete outcomes per runtime.", flush=True)


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
