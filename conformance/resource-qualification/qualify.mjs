import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { delimiter, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';

const root = fileURLToPath(new URL('../../', import.meta.url));
const here = dirname(fileURLToPath(import.meta.url));
const fixture = join(root, 'conformance/generic-hooks/effect4');
const compiler = join(root, 'compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar');
const host = join(root, 'examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar');
const jdk = name => process.env.JAVA_HOME
  ? join(process.env.JAVA_HOME, 'bin', name + (process.platform === 'win32' ? '.exe' : '')) : name;
const MB = 1_048_576;
const DOC = 65_536;
const start = performance.now();
const limits = { documentBytes: DOC, artifactBytes: MB, expressionSourceBytes: 512, expressionNodes: 64,
  expressionDepth: 16, aggregateNodes: 512, stringBytes: 256, valueBytes: 8192, steps: 8, calls: 4,
  warmups: 5, repeats: 30, handles: 4, adapterMs: 500, maxConcurrentChildren: 1, childMs: 45_000, captureBytes: MB,
  probeMs: 1000, probeBytes: 16_384 };

function child(executable, args, { timeout = limits.childMs, cap = limits.captureBytes, cancelOnReady = false } = {}) {
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
  });
}

const metrics = ({ reason, code, signal, elapsedMs, kept }) => ({ reason, code, signal, elapsedMs, capturedBytes: kept });
function requireExit(run, code = 0) {
  assert.equal(run.reason, 'exit', JSON.stringify(metrics(run)));
  assert.equal(run.code, code, run.stderr.toString());
}
function padded(bytes, size) {
  assert.ok(bytes.length < size);
  return Buffer.concat([bytes, Buffer.alloc(size - bytes.length, 32)]);
}
function tree(leaves) {
  if (leaves === 1) return 'true';
  const half = Math.floor(leaves / 2);
  return '(' + tree(half) + '&&' + tree(leaves - half) + ')';
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
  const write = (name, bytes) => { writeFileSync(join(temporary, name), bytes); return name; };
  const source = readFileSync(join(fixture, 'source-no-call.yaml'));
  const dcm = readFileSync(join(fixture, 'dcm.json'));
  const model = JSON.parse(dcm);
  const program = (steps, returned = { literal: 'NO_OP' }, dcmBytes = dcm) => canonicalBytes({
    language: 'keir/v0', domain: { ...model.domain, fingerprint: digest(dcmBytes) }, name: 'resource-probe',
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
  const titleModel = write('title.dcm', readFileSync(join(fixture, 'dcm-title.json')));
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
  const measured = await child(jdk('java'), [...java(compiler, host), 'compiler', temporary, compilerCases]);
  requireExit(measured);
  output.compiler = { process: metrics(measured), ...JSON.parse(measured.stdout) };

  const cli = await child(jdk('java'), ['-Xmx256m', '-jar', compiler, join(temporary, 'source-max.source'),
    join(temporary, dcmName), '--profile=keir-effect/4']);
  requireExit(cli);
  const read = name => readFileSync(join(temporary, name));
  assert.deepEqual(cli.stdout, read('source-max.json'));
  assert.deepEqual(cli.stdout, readFileSync(join(fixture, 'artifact-no-call.json')));
  for (const [id, file] of [['title', 'title'], ['concat', 'title-max'], ['boolean', 'title-composed'], ['main', ''], ['confirmed', 'after-write-read']])
    assert.deepEqual(read(id + '.json'), readFileSync(join(fixture, 'artifact' + (file ? '-' + file : '') + '.json')), id);
  output.compiler.cli = metrics(cli);

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
    admissible: ['source-max', 'expression-text-max', 'nodes-max', 'depth-max', 'string-max', 'aggregate-max'],
  }));
  const nodeArgs = ['--expose-gc', '--max-old-space-size=256', join(here, 'runtime-node.mjs'), temporary, manifest];
  for (const [engine, executable, args] of [
    ['java', jdk('java'), [...java(host), 'runtime', temporary, manifest]],
    ['node', process.execPath, nodeArgs],
  ]) {
    const run = await child(executable, args);
    requireExit(run);
    output.runtimes[engine] = { process: metrics(run), ...JSON.parse(run.stdout) };
  }
  assert.deepEqual(output.runtimes.java.facts, output.runtimes.node.facts);

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
} finally {
  assert.equal(dirname(realpathSync(temporary)), resolvedBuild);
  rmSync(temporary, { recursive: true });
  assert.ok(!existsSync(temporary));
}
console.log(JSON.stringify({ ...output, elapsedMs: performance.now() - start,
  cleanup: { directChildrenClosed: true, temporaryDirectoryRemoved: true } }, null, 2));
