import assert from 'node:assert/strict';
import { readFileSync, writeFileSync } from 'node:fs';
import { delimiter, join } from 'node:path';
import { canonicalBytes, digest } from '../../runtime-node/src/canonical.js';

function nested(guard, leaf, fallback, levels, padding = false) {
  let text = leaf;
  for (let index = levels - 1; index >= 0; index--) {
    let condition = guard;
    if (padding && index >= 1 && index <= 3) condition = '(' + guard + ' && ' + guard + ')';
    if (padding === 'message' && index === 3) condition = '(' + guard + ' ? true : false)';
    text = '(' + condition + ' ? ' + text + ' : ' + fallback + ')';
  }
  assert.ok(Buffer.byteLength(text) <= 512);
  return { cel: text };
}

export async function runGuardedResultQualification(context) {
  const { here, fixture, compiler, host, jdk, temporary, limits, child, requireExit, metrics, output } = context;
  const start = performance.now(), totalMs = 3 * limits.childMs;
  const remaining = () => {
    const left = totalMs - (performance.now() - start);
    assert.ok(left > 0, 'Guarded-result qualification exhausted its total budget');
    return Math.min(limits.childMs, left);
  };
  const write = (name, bytes) => { writeFileSync(join(temporary, name), bytes); return name; };
  const base = readFileSync(join(fixture, 'dcm-guarded-result.json'));
  assert.equal(digest(base), readFileSync(join(fixture, 'dcm-guarded-result.sha256'), 'ascii').trim());
  const model = JSON.parse(base);
  model.types.CreatedRecord.fields.padding = 'StringLabels';
  const modelBytes = canonicalBytes(model);
  const value = { reference: 'r'.repeat(256), flag: false, note: 'n'.repeat(256), enabled: false,
    state: 'CREATED', at: '2026-02-01T00:00:00.000Z', labels: Array(16).fill('l'.repeat(256)), padding: Array(16).fill('') };
  let left = 8192 - canonicalBytes(value).length;
  assert.ok(left >= 0 && left <= 16 * 256);
  for (let index = 0; index < value.padding.length; index++) {
    const count = Math.min(256, left); value.padding[index] = 'p'.repeat(count); left -= count;
  }
  assert.equal(left, 0); assert.equal(canonicalBytes(value).length, 8192);
  const source = canonicalBytes({ language: 'keir/2', domain: { ...model.domain, fingerprint: digest(modelBytes) },
    entrypoint: 'RecordArrived', name: 'guarded-result-retention-boundary', steps: [
      { id: 'g', let: { cel: 'input.createNew' } }, { id: 'h', let: { cel: 'input.enabled' } },
      ...Array.from({ length: 4 }, (_, index) => ({ id: 'a' + index,
        call: index === 3 ? 'records.create' : 'records.lookup', when: index === 0 || index === 3 ? 'g' : 'h',
        args: index === 3 ? { label: { cel: 'input.label' } } : { reference: { cel: 'input.existingReference' } } })),
      { id: 't0', let: nested('g', 'a0.flag', 'input.enabled', 14, true) },
      { id: 't1', let: nested('h', 'a1.flag', 'input.enabled', 14, true) },
    ], return: { record: {
      at: nested('g', 'a3.at', 'input.at', 14, true),
      enabled: nested('h', 'a2.flag', 'input.enabled', 14, true),
      flag: nested('h', 'a1.flag', 'input.enabled', 14, true),
      message: nested('g', '(has(a3.note) ? a3.note : "")', '"unchanged"', 13, 'message'),
      reference: nested('g', 'a3.reference', 'input.existingReference', 14, true),
      state: nested('g', 'a0.state', 'input.state', 14, true),
    } } });
  const expected = [];
  for (const [g, h] of [[false, false], [true, true], [true, false], [false, true]]) {
    const trace = [], add = (event, fields = {}) => trace.push({ sequence: trace.length, event, ...fields });
    add('INVOCATION_START');
    for (const step of ['g', 'h']) { add('STEP_START', { step }); add('VALUE_BOUND', { step }); }
    const calls = [];
    for (let index = 0; index < 4; index++) {
      const step = 'a' + index, writeCall = index === 3, capability = writeCall ? 'records.create' : 'records.lookup';
      const taken = index === 0 || index === 3 ? g : h;
      add('STEP_START', { step }); add('GUARD_EVALUATED', { step, taken });
      if (!taken) { add('STEP_SKIPPED', { step }); continue; }
      calls.push(capability);
      if (writeCall) add('EFFECT_INTENT', { step, capability });
      add('CALL_DISPATCHED', { step, capability });
      if (writeCall) add('EFFECT_RECORDED', { step, capability, state: 'CONFIRMED' });
      add('CALL_SUCCEEDED', { step, capability });
    }
    for (const step of ['t0', 't1']) { add('STEP_START', { step }); add('VALUE_BOUND', { step }); }
    add('RETURNED'); add('TERMINATED');
    expected.push({ input: { createNew: g, enabled: h, existingReference: 'existing', label: 'label', state: 'EXISTING',
      at: '2026-01-01T00:00:00.000Z' }, calls, outcome: { classification: 'COMPLETED',
      result: { at: g ? value.at : '2026-01-01T00:00:00.000Z', enabled: false, flag: false,
        message: g ? value.note : 'unchanged', reference: g ? '__REFERENCE__' : 'existing', state: g ? 'CREATED' : 'EXISTING' },
      effects: g ? [{ step: 'a3', capability: 'records.create', state: 'CONFIRMED' }] : [], trace } });
  }
  const manifest = { model: write('guarded.dcm', modelBytes), source: write('guarded.source', source), artifact: 'guarded.artifact',
    value, expected, limits: { warmups: 16, blocks: 3, perBlock: 64, invocationMs: 10_000 } };
  const manifestName = write('guarded-manifest.json', canonicalBytes(manifest));
  output.limits = { ...manifest.limits, childMs: limits.childMs, totalMs, heapMiB: 256, captureBytes: limits.captureBytes };
  output.pins = { model: digest(modelBytes), source: digest(source), value: digest(canonicalBytes(value)),
    expected: digest(canonicalBytes(expected)) };
  const classpath = [temporary, compiler, host].join(delimiter);
  const setup = await child(jdk('javac'), ['-cp', classpath, '-d', temporary, join(here, 'GuardedResultProbe.java')], { timeout: remaining() });
  requireExit(setup); output.probeCompilation = metrics(setup);
  const java = mode => ['-Xmx256m', '-cp', classpath, 'dev.keir.runtime.GuardedResultProbe', mode, temporary, manifestName];
  const produced = await child(jdk('java'), java('compile'), { timeout: remaining() });
  output.producer = { process: metrics(produced), ...(produced.stdout.length ? { report: JSON.parse(produced.stdout) } : {}) };
  requireExit(produced);
  assert.equal(output.producer.report.aggregateNodes, 512); assert.equal(output.producer.report.depth, 16);
  assert.equal(output.producer.report.maxVariables, 9); assert.equal(output.producer.report.calls, 4);
  assert.equal(output.producer.report.steps, 8); assert.equal(output.producer.report.maxExpressionNodes, 64);
  output.pins.artifact = digest(readFileSync(join(temporary, manifest.artifact)));
  output.runtimes = {};
  const node = ['--expose-gc', '--max-old-space-size=256', join(here, 'guarded-result-node.mjs'), temporary, manifestName];
  for (const engine of ['java', 'node']) {
    const run = await child(engine === 'java' ? jdk('java') : process.execPath, engine === 'java' ? java('runtime') : node,
      { timeout: remaining() });
    const report = run.stdout.length ? JSON.parse(run.stdout) : undefined;
    output.runtimes[engine] = { process: metrics(run), report }; requireExit(run);
    assert.equal(report.status, 'passed'); assert.equal(report.completedWarmups, manifest.limits.warmups);
    assert.equal(report.completedInvocations, manifest.limits.blocks * manifest.limits.perBlock);
    assert.equal(report.maxRetainedResults, 4); assert.equal(report.resultBytes, 8192);
    assert.equal(report.artifactDigest, output.pins.artifact);
  }
  assert.equal(output.runtimes.java.report.dispatchCount, output.runtimes.node.report.dispatchCount);
  assert.equal(output.runtimes.java.report.outcomesDigest, output.runtimes.node.report.outcomesDigest);
  const overflow = structuredClone(manifest), boundary = structuredClone(manifest.expected[1]);
  overflow.value.padding[overflow.value.padding.findIndex(text => text.length < 256)] += 'x';
  assert.equal(canonicalBytes(overflow.value).length, 8193);
  const success = boundary.outcome.trace.findIndex(row => row.event === 'CALL_SUCCEEDED');
  boundary.calls = ['records.lookup'];
  boundary.outcome = { classification: 'LIMIT_EXCEEDED', error: { code: 'BUDGET_EXCEEDED', path: '/steps/2' }, effects: [],
    trace: [...boundary.outcome.trace.slice(0, success), { sequence: success, event: 'TERMINATED', code: 'BUDGET_EXCEEDED' }] };
  overflow.expected = [boundary]; overflow.limits = { ...manifest.limits, warmups: 0, blocks: 1, perBlock: 1 };
  write('guarded-overflow.json', canonicalBytes(overflow)); output.boundaryChecks = [];
  for (const engine of ['java', 'node']) {
    const args = engine === 'java' ? [...java('runtime').slice(0, -1), 'guarded-overflow.json']
      : [...node.slice(0, -1), 'guarded-overflow.json'];
    const run = await child(engine === 'java' ? jdk('java') : process.execPath, args, { timeout: remaining() });
    const report = run.stdout.length ? JSON.parse(run.stdout) : undefined;
    output.boundaryChecks.push({ engine, process: metrics(run), report }); requireExit(run);
    assert.equal(report.status, 'passed'); assert.equal(report.completedInvocations, 1);
    assert.equal(report.dispatchCount, 1); assert.equal(report.maxRetainedResults, 0);
  }
  assert.equal(output.boundaryChecks[0].report.outcomesDigest, output.boundaryChecks[1].report.outcomesDigest);
  const negative = structuredClone(manifest);
  negative.expected[1].outcome.result.message = 'WRONG';
  write('guarded-negative.json', canonicalBytes(negative));
  output.failureChecks = [];
  for (const engine of ['java', 'node']) {
    const args = engine === 'java' ? [...java('runtime').slice(0, -1), 'guarded-negative.json']
      : [...node.slice(0, -1), 'guarded-negative.json'];
    const run = await child(engine === 'java' ? jdk('java') : process.execPath, args, { timeout: remaining() });
    requireExit(run, 1); const report = JSON.parse(run.stdout);
    assert.equal(report.status, 'failed'); assert.match(report.error, /Unexpected complete outcome/);
    output.failureChecks.push({ engine, process: metrics(run), report });
  }
  output.status = 'passed'; output.elapsedMs = performance.now() - start;
}
