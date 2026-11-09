import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, dirname, join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { setupNodeExample } from './setup-node-example.mjs';

const name = '@keir/runtime-node', entry = 'node_modules/' + name;
const declaration = 'file:../../.tools/packages/keir-runtime-node-0.3.0.tgz';
const integrity = bytes => 'sha512-' + createHash('sha512').update(bytes).digest('base64');
const json = path => JSON.parse(readFileSync(path));
const writeJson = (path, value) => writeFileSync(path, JSON.stringify(value, null, 2) + '\n');

function fixture(t, external = true) {
  const root = realpathSync(mkdtempSync(join(tmpdir(), 'keir source setup ')));
  assert.equal(dirname(root), realpathSync(tmpdir()));
  assert.ok(basename(root).startsWith('keir source setup '));
  t.after(() => rmSync(root, { recursive: true, force: true, maxRetries: 3 }));
  mkdirSync(join(root, 'runtime-node'), { recursive: true });
  mkdirSync(join(root, 'examples/node-host'), { recursive: true });
  const manifestPath = join(root, 'examples/node-host/package.json');
  const lockPath = join(root, 'examples/node-host/package-lock.json');
  const manifest = { name: 'source-example', private: true, description: 'Existing user edit', dependencies: { [name]: declaration } };
  const lock = { name: 'source-example', lockfileVersion: 3, requires: true, packages: {
    '': { name: manifest.name, dependencies: { ...manifest.dependencies } },
    [entry]: { version: '0.3.0', resolved: declaration, integrity: integrity('previous archive') },
  } };
  if (external) {
    manifest.dependencies.external = '1.0.0';
    lock.packages[''].dependencies.external = '1.0.0';
    lock.packages['node_modules/external'] = { version: '1.0.0', resolved: 'https://example.invalid/external.tgz', integrity: 'sha512-fixed' };
  }
  writeFileSync(manifestPath, JSON.stringify(manifest, null, 4).replaceAll('\n', '\r\n') + '\r\n');
  writeFileSync(lockPath, JSON.stringify(lock, null, 4).replaceAll('\n', '\r\n') + '\r\n');
  writeJson(join(root, 'runtime-node/package.json'), { name, version: '0.3.0' });
  writeFileSync(join(root, 'runtime-node/README.md'), 'First source archive\n');
  return { root, manifestPath, lockPath, originalManifest: readFileSync(manifestPath), originalLock: readFileSync(lockPath), calls: [] };
}

function restored(f) {
  assert.ok(readFileSync(f.manifestPath).equals(f.originalManifest));
  assert.ok(readFileSync(f.lockPath).equals(f.originalLock));
}

function commands(f, mutate = () => {}) {
  return args => {
    const stage = args[0];
    f.calls.push(stage);
    assert.ok(args.includes('--ignore-scripts'));
    assert.equal(args[args.indexOf('--cache') + 1], '.tools/npm-cache');
    if (stage !== 'pack') assert.ok(args.includes('--no-audit') && args.includes('--no-fund'));
    const archive = join(f.root, '.tools/packages/keir-runtime-node-0.3.0.tgz');
    if (stage === 'pack') {
      const bytes = readFileSync(join(f.root, 'runtime-node/README.md'));
      writeFileSync(archive, bytes);
      const packed = [{ name, version: '0.3.0', filename: basename(archive), integrity: integrity(bytes) }];
      mutate(stage, f);
      return JSON.stringify(packed);
    }
    if (stage === 'install') {
      assert.ok(args.includes('--package-lock-only'));
      const lock = json(f.lockPath);
      lock.packages[entry].integrity = integrity(readFileSync(archive));
      writeJson(f.lockPath, lock);
    }
    mutate(stage, f);
    return '';
  };
}

test('refresh changes only local tar integrity and repeated setup is idempotent', t => {
  const f = fixture(t), run = commands(f);
  const first = setupNodeExample(f.root, run);
  const expected = JSON.parse(f.originalLock);
  expected.packages[entry].integrity = first;
  assert.deepEqual(json(f.lockPath), expected);
  assert.ok(readFileSync(f.manifestPath).equals(f.originalManifest));
  const installedLock = readFileSync(f.lockPath);
  assert.equal(setupNodeExample(f.root, run), first);
  assert.ok(readFileSync(f.lockPath).equals(installedLock));
  assert.deepEqual(f.calls, ['pack', 'install', 'ci', 'pack', 'install', 'ci']);
});

for (const [label, change] of [
  ['external pin', lock => { lock.packages['node_modules/external'].integrity = 'changed'; }],
  ['runtime version', lock => { lock.packages[entry].version = '0.4.0'; }],
  ['runtime resolved path', lock => { lock.packages[entry].resolved = 'file:other.tgz'; }],
  ['host declaration', lock => { lock.packages[''].dependencies[name] = 'file:other.tgz'; }],
  ['runtime dependencies', lock => { lock.packages[entry].dependencies = { extra: '1' }; }],
]) test(`unexpected ${label} drift restores exact originals before ci`, t => {
  const f = fixture(t);
  const run = commands(f, stage => { if (stage === 'install') { const lock = json(f.lockPath); change(lock); writeJson(f.lockPath, lock); } });
  assert.throws(() => setupNodeExample(f.root, run), /beyond the local archive integrity/);
  restored(f); assert.deepEqual(f.calls, ['pack', 'install']);
});

test('unexpected manifest mutation restores CRLF, whitespace and pre-existing edits', t => {
  const f = fixture(t);
  const run = commands(f, stage => { if (stage === 'install') writeJson(f.manifestPath, { ...json(f.manifestPath), description: 'npm drift' }); });
  assert.throws(() => setupNodeExample(f.root, run), /changed package.json/);
  restored(f); assert.deepEqual(f.calls, ['pack', 'install']);
});

for (const failed of ['pack', 'install', 'ci']) test(`failed ${failed} restores both files without further npm stages`, t => {
  const f = fixture(t);
  const run = commands(f, stage => {
    if (stage === failed) {
      writeFileSync(f.manifestPath, '{}'); writeFileSync(f.lockPath, '{}');
      throw new Error('Synthetic npm failure');
    }
  });
  assert.throws(() => setupNodeExample(f.root, run), /Synthetic npm failure/);
  restored(f); assert.equal(f.calls.at(-1), failed);
});

test('a stale or replaced packed file fails integrity validation before refresh', t => {
  const f = fixture(t);
  const run = commands(f, stage => { if (stage === 'pack') writeFileSync(join(f.root, '.tools/packages/keir-runtime-node-0.3.0.tgz'), 'Other bytes'); });
  assert.throws(() => setupNodeExample(f.root, run), /Packed archive differs/);
  restored(f); assert.deepEqual(f.calls, ['pack']);
});

test('ci postcondition failure also restores originals', t => {
  const f = fixture(t);
  const run = commands(f, stage => { if (stage === 'ci') writeFileSync(f.manifestPath, '{}'); });
  assert.throws(() => setupNodeExample(f.root, run), /changed package.json/);
  restored(f);
});

for (const [label, path, change] of [
  ['runtime name', 'runtime-node/package.json', value => { value.name = 'other'; }],
  ['runtime version', 'runtime-node/package.json', value => { value.version = '0.3.0 & invalid'; }],
  ['host archive path', 'examples/node-host/package.json', value => { value.dependencies[name] = 'file:../../../other.tgz'; }],
  ['locked archive path', 'examples/node-host/package-lock.json', value => { value.packages[entry].resolved = 'file:other.tgz'; }],
]) test(`invalid ${label} refuses before npm or writes`, t => {
  const f = fixture(t), target = join(f.root, path), value = json(target);
  change(value); writeJson(target, value);
  const manifest = readFileSync(f.manifestPath), lock = readFileSync(f.lockPath);
  assert.throws(() => setupNodeExample(f.root, () => assert.fail('Unexpected npm invocation')));
  assert.ok(readFileSync(f.manifestPath).equals(manifest)); assert.ok(readFileSync(f.lockPath).equals(lock));
  assert.equal(existsSync(join(f.root, '.tools')), false);
});

test('CLI rejects unexpected arguments without running setup', () => {
  const result = spawnSync(process.execPath, [fileURLToPath(new URL('./setup-node-example.mjs', import.meta.url)), '--unexpected'], { encoding: 'utf8' });
  assert.equal(result.status, 1); assert.match(result.stderr, /takes no arguments/);
});

test('real native npm handles spaces, changed cached archive and idempotent refresh', t => {
  const f = fixture(t, false);
  mkdirSync(join(f.root, 'scripts'));
  const script = join(f.root, 'scripts/setup-node-example.mjs');
  writeFileSync(script, readFileSync(new URL('./setup-node-example.mjs', import.meta.url)));
  const run = () => {
    const result = spawnSync(process.execPath, [script], { cwd: tmpdir(), encoding: 'utf8', env: { ...process.env, npm_config_offline: 'true' }, timeout: 30000 });
    assert.equal(result.status, 0, result.stderr + result.stdout);
    assert.ok(readFileSync(f.manifestPath).equals(f.originalManifest));
    return json(f.lockPath).packages[entry].integrity;
  };
  const first = run();
  writeFileSync(join(f.root, 'runtime-node/README.md'), 'Changed source archive\n');
  const second = run(); assert.notEqual(first, second);
  assert.equal(readFileSync(join(f.root, 'examples/node-host/node_modules/@keir/runtime-node/README.md'), 'utf8'), 'Changed source archive\n');
  const lock = readFileSync(f.lockPath); assert.equal(run(), second); assert.ok(readFileSync(f.lockPath).equals(lock));
});
