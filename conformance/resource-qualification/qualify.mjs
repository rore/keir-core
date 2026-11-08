import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { delimiter, dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';

const root = fileURLToPath(new URL('../../', import.meta.url));
const here = dirname(fileURLToPath(import.meta.url));
const fixture = join(root, 'conformance/keir-2');
const compiler = join(root, 'compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar');
const host = join(root, 'examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar');
const jdk = name => process.env.JAVA_HOME
  ? join(process.env.JAVA_HOME, 'bin', name + (process.platform === 'win32' ? '.exe' : '')) : name;
const MB = 1_048_576;
const DOC = 65_536;
const start = performance.now();
const args = process.argv.slice(2);
assert.ok(args.length === 0 || args.length === 1 && ['--soak', '--dense'].includes(args[0]), 'Use --soak or --dense for a serial workload');
const dense = args.includes('--dense');
const soak = args.length ? {
  warmups: 100, blocks: 5, perBlock: dense ? 1000 : 200, heapMiB: 256, peakWorkingSetBytes: 512 * MB, sampleIntervalMs: 100,
} : null;
if (soak) assert.equal(process.platform, 'win32', 'The serial workload uses the Windows JVM working-set counter');
const limits = { documentBytes: DOC, artifactBytes: MB, expressionSourceBytes: 512, expressionNodes: 64,
  expressionDepth: 16, aggregateNodes: 512, stringBytes: 256, valueBytes: 8192, steps: 8, calls: 4,
  warmups: 5, repeats: 30, handles: 4, adapterMs: 500, maxConcurrentChildren: soak ? 2 : 1, childMs: 45_000, captureBytes: MB,
  probeMs: 1000, probeBytes: 16_384, ...(soak ? { soak } : {}) };

function child(executable, args, { timeout = limits.childMs, cap = limits.captureBytes, cancelOnReady = false, started } = {}) {
  return new Promise((done, reject) => {
    const start = performance.now();
    const proc = spawn(executable, args, { cwd: root, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
    const stdout = [], stderr = [];
    let reason = 'exit', kept = 0, error;
    const stop = why => {
      if (reason !== 'exit') return;
      reason = why;
      if (proc.exitCode === null && proc.signalCode === null) proc.kill('SIGKILL');
    };
    const timer = setTimeout(() => stop('timeout'), timeout);
    for (const [stream, chunks] of [[proc.stdout, stdout], [proc.stderr, stderr]]) stream.on('data', chunk => {
      if (reason !== 'exit') return;
      const remaining = cap - kept;
      if (chunk.length > remaining) {
        if (remaining) chunks.push(chunk.subarray(0, remaining));
        kept = cap;
        stop('output-cap');
      } else {
        chunks.push(chunk);
        kept += chunk.length;
        if (cancelOnReady && stream === proc.stdout && Buffer.concat(stdout).includes('ready\n')) stop('cancel');
      }
    });
    proc.once('error', failure => { error = failure; });
    proc.once('close', (code, signal) => {
      clearTimeout(timer);
      if (error) return reject(error);
      done({ reason, code, signal, elapsedMs: performance.now() - start, kept,
        stdout: Buffer.concat(stdout), stderr: Buffer.concat(stderr) });
    });
    if (started && proc.pid) started(proc.pid);
  });
}

const metrics = ({ reason, code, signal, elapsedMs, kept }) => ({ reason, code, signal, elapsedMs, capturedBytes: kept });
function requireExit(run, code = 0) {
  assert.equal(run.reason, 'exit', JSON.stringify(metrics(run)));
  assert.equal(run.code, code, run.stderr.toString());
}

async function runJvm(args, { timeout = limits.childMs } = {}) {
  if (!soak) return child(jdk('java'), args, { timeout });
  let sampler, sampled;
  const run = await child(jdk('java'), args, { timeout, started(pid) {
    sampler = child('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', [
      "$ErrorActionPreference = 'Stop'",
      "$clock = [Diagnostics.Stopwatch]::StartNew()",
      "$samples = [Collections.Generic.List[object]]::new()",
      "$proc = Get-Process -Id " + pid,
      "if ($proc.ProcessName -ne 'java') { throw 'Unexpected sample target' }",
      "for ($i = 0; $i -lt " + Math.ceil(timeout / soak.sampleIntervalMs) + "; $i++) { $proc.Refresh(); if ($proc.HasExited) { break };",
      "$samples.Add(@{offsetMs=$clock.ElapsedMilliseconds; workingSetBytes=$proc.WorkingSet64; peakWorkingSetBytes=$proc.PeakWorkingSet64});",
      ...(dense ? ["if ($proc.PeakWorkingSet64 -gt " + soak.peakWorkingSetBytes + ") { $proc.Kill(); throw 'Observed JVM process memory exceeded the dense budget' }"] : []),
      "Start-Sleep -Milliseconds " + soak.sampleIntervalMs + " }",
      "@{samples=$samples} | ConvertTo-Json -Depth 4 -Compress",
    ].join('\n')]).then(result => ({ result }), error => ({ error }));
  } }).finally(async () => { sampled = await sampler; });
  if (sampled?.error) throw sampled.error;
  requireExit(run);
  assert.ok(sampled, 'JVM sampler failed to start');
  requireExit(sampled.result);
  const { samples } = JSON.parse(sampled.result.stdout);
  assert.ok(samples.length >= 2 && samples.every(row => row.workingSetBytes > 0 && row.peakWorkingSetBytes >= row.workingSetBytes),
    'JVM working-set samples missing or invalid');
  const peakWorkingSetBytes = Math.max(...samples.map(row => row.peakWorkingSetBytes));
  assert.ok(peakWorkingSetBytes <= soak.peakWorkingSetBytes, 'Observed JVM working set exceeded the qualification budget');
  return { ...run, workingSet: { counter: 'Get-Process PeakWorkingSet64', intervalMs: soak.sampleIntervalMs,
    peakWorkingSetBytes, samples, sampler: metrics(sampled.result) } };
}
function padded(bytes, size) {
  assert.ok(bytes.length < size);
  return Buffer.concat([bytes, Buffer.alloc(size - bytes.length, 32)]);
}
function tree(leaves, first = 'true') {
  if (leaves === 1) return first;
  const half = Math.floor(leaves / 2);
  return '(' + tree(half, first) + '&&' + tree(leaves - half) + ')';
}

assert.ok(process.versions.node.startsWith('24.'), 'Use Node 24');
assert.ok(existsSync(compiler) && existsSync(host), 'Package Java modules before running qualification');
const build = join(root, 'build');
mkdirSync(build, { recursive: true });
const resolvedBuild = realpathSync(build);
assert.equal(resolvedBuild, join(realpathSync(root), 'build'), 'Use an in-checkout build directory');
const temporary = mkdtempSync(join(resolvedBuild, 'resource-'));
const output = { limits, platform: process.platform, arch: process.arch, compiler: {}, runtimes: {}, children: {} };
try {
  if (dense) {
    const { runDenseQualification } = await import('./dense.mjs');
    output.dense = {};
    await runDenseQualification({ here, fixture, compiler, host, jdk, temporary, limits,
      child, runJvm, requireExit, metrics, output: output.dense });
  } else {
  const write = (name, bytes) => { writeFileSync(join(temporary, name), bytes); return name; };
  const pinned = name => {
    const bytes = readFileSync(join(fixture, name + '.json'));
    assert.equal(digest(bytes), readFileSync(join(fixture, name + '.sha256'), 'ascii').trim(), name);
    return bytes;
  };
  const source = readFileSync(join(fixture, 'source-no-call.yaml'));
  const dcm = pinned('dcm');
  const model = JSON.parse(dcm);
  const program = (steps, returned = { literal: 'NO_OP' }, dcmBytes = dcm) => canonicalBytes({
    language: 'keir/2', domain: { ...model.domain, fingerprint: digest(dcmBytes) }, name: 'resource-probe',
    entrypoint: 'TaskCompleted', steps: steps.map((binding, index) => ({ id: 'step' + index, let: binding })),
    return: { record: { status: returned } },
  });
  const expr = (cel) => ({ cel });
  const nodes63 = tree(32), nodes64 = '!' + nodes63, nodes65 = '(' + nodes63 + '&&true)';
  let deep = 'true';
  for (let i = 0; i < 15; i++) deep = '(true&&' + deep + ')';
  const aggregate = Array.from({ length: 8 }, () => expr(nodes64));
  const cases = [];
  function compile(id, input, modelName, expected = '') {
    cases.push({ id, source: write(id + '.source', input), model: modelName, expected });
    return id + '.json';
  }
  const dcmName = write('base.dcm', dcm);
  const titleDcm = pinned('dcm-title');
  const titleModel = write('title.dcm', titleDcm);
  const dcmMax = write('dcm-max.dcm', padded(dcm, DOC));
  const dcmOver = write('dcm-over.dcm', padded(dcm, DOC + 1));
  compile('source-max', padded(source, DOC), dcmName);
  compile('source-over', padded(source, DOC + 1), dcmName, '/source');
  compile('dcm-max', source, dcmMax);
  compile('dcm-over', source, dcmOver, '/dcm');
  for (const [id, max, over] of [
    ['expression-text', expr('true' + ' '.repeat(508)), expr('true' + ' '.repeat(509))],
    ['nodes', expr(nodes64), expr(nodes65)],
    ['depth', expr(deep), expr('(true&&' + deep + ')')],
    ['string', { literal: 'é'.repeat(128) }, { literal: 'é'.repeat(128) + 'x' }],
  ]) {
    compile(id + '-max', program([max]), dcmName);
    compile(id + '-over', program([over]), dcmName, '/source/steps/0/let/' + (id === 'string' ? 'literal' : 'cel'));
  }
  const aggregateMax = compile('aggregate-max', program(aggregate), dcmName);
  compile('aggregate-over', program(aggregate, expr('"NO_OP"')), dcmName, '/source/steps');
  const donor = compile('aggregate-donor', program([expr(nodes63), ...aggregate.slice(1)], expr('"NO_OP"')), dcmName);
  if (soak) {
    const comparison = '("' + 'x'.repeat(200) + '" == "y")';
    const expression = '!' + tree(31, comparison);
    assert.ok(Buffer.byteLength(expression) <= limits.expressionSourceBytes);
    const large = JSON.parse(program(Array.from({ length: 8 }, () => expr(expression))));
    large.steps.forEach((step, i) => { step.id = 'step' + i + 'x'.repeat(59); });
    compile('embedded-large', canonicalBytes(large), dcmName);
    compile('embedded-large-over', canonicalBytes({ ...large, return: { record: { status: expr('"NO_OP"') } } }),
      dcmName, '/source/steps');
    compile('steps-over', canonicalBytes({ ...large, steps: [...large.steps, { id: 'ninth', let: { literal: true } }] }),
      dcmName, '/source/steps');
    for (const count of [4, 5]) {
      const callSteps = Array.from({ length: count }, (_, i) => ({ id: 'read' + i, call: 'tasks.get', args: { taskId: expr('input.taskId') } }));
      compile('calls-' + count, canonicalBytes({ ...large, steps: callSteps }), dcmName, count === 5 ? '/source/steps' : '');
    }
  }
  const wide = structuredClone(model), value = { taskId: 'task1', sixth: '' };
  const fields = wide.types.TaskCompletedInput.fields;
  fields.sixth = 'String';
  for (const field of ['first', 'second', 'third', 'fourth', 'fifth']) {
    fields[field] = 'String';
    value[field] = String.fromCharCode(1).repeat(256);
  }
  const remaining = limits.valueBytes - canonicalBytes(value).length;
  value.sixth = String.fromCharCode(1).repeat(Math.floor(remaining / 6)) + 'x'.repeat(remaining % 6);
  assert.ok(value.sixth.length < limits.stringBytes);
  assert.equal(canonicalBytes(value).length, limits.valueBytes);
  write('value-max.json', canonicalBytes(value));
  value.sixth += 'x';
  assert.equal(canonicalBytes(value).length, limits.valueBytes + 1);
  write('value-over.json', canonicalBytes(value));
  const wideBytes = canonicalBytes(wide);
  const wideModel = write('wide.dcm', wideBytes);
  compile('value', program([{ literal: false }], undefined, wideBytes), wideModel);
  for (const [id, file, modelName] of [
    ['title', 'title', titleModel], ['concat', 'title-max', titleModel], ['boolean', 'title-composed', titleModel],
    ['main', '', dcmName], ['confirmed', 'after-write-read', dcmName],
  ]) compile(id, readFileSync(join(fixture, 'source' + (file ? '-' + file : '') + '.yaml')), modelName);

  const probe = join(here, 'ResourceProbe.java');
  const cp = [compiler, host].join(delimiter);
  requireExit(await child(jdk('javac'), ['-cp', cp, '-d', temporary, probe]));
  const java = (...jars) => ['-Xmx256m', '-cp', [temporary, ...jars].join(delimiter), 'dev.keir.runtime.ResourceProbe'];
  const compilerCases = write('compiler-cases.json', JSON.stringify(cases));
  const measured = await runJvm([...java(compiler, host), 'compiler', temporary, compilerCases]);
  requireExit(measured);
  output.compiler = { process: metrics(measured), workingSet: measured.workingSet, ...JSON.parse(measured.stdout) };

  const cli = await child(jdk('java'), ['-Xmx256m', '-jar', compiler, join(temporary, 'source-max.source'),
    join(temporary, dcmName)]);
  requireExit(cli);
  const read = name => readFileSync(join(temporary, name));
  assert.deepEqual(cli.stdout, read('source-max.json'));
  assert.deepEqual(cli.stdout, pinned('artifact-no-call'));
  for (const [id, file] of [['title', 'title'], ['concat', 'title-max'], ['boolean', 'title-composed'], ['main', ''], ['confirmed', 'after-write-read']])
    assert.deepEqual(read(id + '.json'), pinned('artifact' + (file ? '-' + file : '')), id);
  output.compiler.cli = metrics(cli);

  if (soak) {
    // Slots are LETs, agent arguments, handler fields and return fields for these exact catalogs.
    const geometry = document => {
      assert.deepEqual(Object.keys(document.entrypoints), ['TaskCompleted']);
      assert.ok(canonicalBytes(document.domain).length <= 512);
      const fields = Object.keys(document.types[document.entrypoints.TaskCompleted.output].fields).length;
      const capabilities = Object.values(document.capabilities);
      assert.equal(capabilities.length, 2);
      const slots = effect => {
        const cap = capabilities.find(candidate => candidate.effect === effect);
        assert.equal(cap?.version, '1');
        return Object.values(cap.parameters).filter(parameter => parameter.binding === 'agent').length + cap.errors.length * fields;
      };
      return [fields, slots('READ'), slots('WRITE')];
    };
    const [fields, readSlots, writeSlots] = geometry(model);
    assert.deepEqual([fields, readSlots, writeSlots], [1, 3, 5]);
    assert.deepEqual(geometry(JSON.parse(titleDcm)), [fields, readSlots, writeSlots]);
    const bindingSlots = limits.steps - limits.calls + (limits.calls - 1) * readSlots + writeSlots + fields;
    const expressionBytes = 4 * Math.ceil(16_384 / 3) + 9 * (64 + 64 + 6) + 2 + 128;
    const artifactBytes = bindingSlots * (expressionBytes + 2048) + 16_384;
    const accepted = read('embedded-large.json');
    assert.ok(accepted.length > read(aggregateMax).length && accepted.length <= artifactBytes);
    output.artifactBound = { catalogs: [digest(dcm), digest(titleDcm)], bindingSlots, artifactBytes,
      validFixtureBytes: accepted.length, validFixtureDigest: digest(accepted) };
  }

  const largest = JSON.parse(read(aggregateMax)), extra = JSON.parse(read(donor));
  const returnId = extra.return.fields.status.expr;
  assert.ok(!Object.hasOwn(largest.expressions, returnId));
  largest.return = extra.return;
  largest.expressions[returnId] = extra.expressions[returnId];
  write('aggregate-forged.json', canonicalBytes(largest));

  const raw = { ...JSON.parse(read('source-max.json')), extra: '' };
  raw.extra = 'x'.repeat(MB - canonicalBytes(raw).length);
  // Preserve nested fields and canonical ordering when probing the raw artifact ceiling.
  const ordered = JSON.stringify(Object.fromEntries(Object.keys(raw).sort().map(key => [key, raw[key]])));
  assert.equal(Buffer.byteLength(ordered), MB);
  write('artifact-max.json', ordered);
  write('artifact-over.json', ordered + ' ');

  const manifest = write('runtime-cases.json', JSON.stringify({
    limits, dcmName, dcmMax, dcmOver, titleModel, wideModel,
    admissible: ['source-max', 'expression-text-max', 'nodes-max', 'depth-max', 'string-max', 'aggregate-max',
      ...(soak ? ['embedded-large'] : [])],
  }));
  const nodeArgs = ['--expose-gc', '--max-old-space-size=256', join(here, 'runtime-node.mjs'), temporary, manifest];
  for (const [engine, executable, args] of [
    ['java', jdk('java'), [...java(host), 'runtime', temporary, manifest]],
    ['node', process.execPath, nodeArgs],
  ]) {
    const run = await (engine === 'java' ? runJvm(args) : child(executable, args));
    requireExit(run);
    output.runtimes[engine] = { process: metrics(run), workingSet: run.workingSet, ...JSON.parse(run.stdout) };
  }
  assert.deepEqual(output.runtimes.java.facts, output.runtimes.node.facts);
  if (soak) assert.ok(output.runtimes.node.peakRssBytes <= soak.peakWorkingSetBytes, 'Observed Node RSS exceeded the qualification budget');

  const hold = 'console.log("ready"); setInterval(() => {}, 1000);';
  for (const [id, reason, script] of [
    ['timeout', 'timeout', hold],
    ['cancel', 'cancel', hold],
    ['output-cap', 'output-cap', 'process.stdout.write(Buffer.alloc(12000)); process.stderr.write(Buffer.alloc(12000)); setInterval(() => {}, 1000);'],
    ['output-cap-exit', 'output-cap', 'process.stdout.write(Buffer.alloc(32768));'],
  ]) {
    const run = await child(process.execPath, ['-e', script],
      { timeout: limits.probeMs, cap: limits.probeBytes, cancelOnReady: reason === 'cancel' });
    assert.equal(run.reason, reason);
    if (id !== 'output-cap-exit') assert.ok(run.signal || run.code !== 0, 'Expected child termination');
    if (reason === 'output-cap') assert.equal(run.kept, limits.probeBytes);
    else assert.equal(run.stdout.toString(), 'ready\n');
    output.children[id] = metrics(run);
  }
  }
} catch (error) {
  if (!dense) throw error;
  output.status = 'failed';
  output.error = String(error.message).slice(0, 2000);
  output.lastCheckpoints = Object.fromEntries(['java', 'node'].map(engine => {
    const path = join(temporary, 'dense-' + engine + '-progress.json');
    return [engine, existsSync(path) ? JSON.parse(readFileSync(path)) : null];
  }));
  process.exitCode = 1;
} finally {
  assert.equal(dirname(realpathSync(temporary)), resolvedBuild);
  if (!dense) {
    rmSync(temporary, { recursive: true });
    assert.ok(!existsSync(temporary));
  }
}
console.log(JSON.stringify({ ...output, elapsedMs: performance.now() - start,
  cleanup: { directChildrenClosed: true, temporaryDirectoryRemoved: !dense },
  ...(dense ? { evidenceDirectory: relative(root, temporary).replaceAll('\\', '/') } : {}) }, null, 2));
