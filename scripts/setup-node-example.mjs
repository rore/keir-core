import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { lstatSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

function npm(root, args) {
  const windows = process.platform === 'win32';
  const result = spawnSync(windows ? process.env.ComSpec ?? 'cmd.exe' : 'npm',
    windows ? ['/d', '/s', '/c', 'npm.cmd', ...args] : args,
    { cwd: root, encoding: 'utf8', windowsHide: true, maxBuffer: 1_048_576 });
  if (result.stderr) process.stderr.write(result.stderr);
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`npm ${args[0]} failed (${result.status ?? result.signal})`);
  if (args[0] !== 'pack' && result.stdout) process.stdout.write(result.stdout);
  return result.stdout;
}

export function setupNodeExample(root, runNpm = args => npm(root, args)) {
  const manifestPath = join(root, 'examples/node-host/package.json');
  const lockPath = join(root, 'examples/node-host/package-lock.json');
  const runtimePath = join(root, 'runtime-node/package.json');
  for (const path of [manifestPath, lockPath, runtimePath]) assert.ok(lstatSync(path).isFile(), 'Expected regular package files');
  const originalManifest = readFileSync(manifestPath), originalLock = readFileSync(lockPath);
  const manifest = JSON.parse(originalManifest), lock = JSON.parse(originalLock);
  const runtime = JSON.parse(readFileSync(runtimePath));
  const name = '@keir/runtime-node', entry = 'node_modules/' + name;
  assert.equal(runtime.name, name, 'Unexpected source runtime package');
  assert.match(runtime.version, /^\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?(?:\+[A-Za-z0-9.-]+)?$/, 'Invalid runtime version');
  const filename = `keir-runtime-node-${runtime.version}.tgz`;
  const declaration = 'file:../../.tools/packages/' + filename;
  assert.equal(manifest.dependencies?.[name], declaration, 'Host must select the matching local source archive');
  assert.equal(lock.packages?.['']?.dependencies?.[name], declaration, 'Lock must match the host declaration');
  assert.equal(lock.packages?.[entry]?.resolved, declaration, 'Unexpected locked runtime path');
  assert.equal(lock.packages[entry].version, runtime.version, 'Unexpected locked runtime version');
  assert.deepEqual(lock.packages[entry].dependencies ?? {}, runtime.dependencies ?? {}, 'Locked runtime dependencies differ');
  const cache = ['--cache', process.env.NPM_CONFIG_CACHE ?? '.tools/npm-cache', '--ignore-scripts'];
  const install = ['--prefix', 'examples/node-host', ...cache, '--no-audit', '--no-fund'];
  try {
    mkdirSync(join(root, '.tools/packages'), { recursive: true });
    const packed = JSON.parse(runNpm(['pack', './runtime-node', '--pack-destination', '.tools/packages', ...cache, '--json']));
    assert.equal(packed.length, 1, 'Expected one packed runtime');
    assert.equal(packed[0].name, name); assert.equal(packed[0].version, runtime.version);
    assert.equal(packed[0].filename, filename, 'Unexpected packed archive path');
    const integrity = 'sha512-' + createHash('sha512').update(readFileSync(join(root, '.tools/packages', filename))).digest('base64');
    assert.equal(packed[0].integrity, integrity, 'Packed archive differs from npm output');
    runNpm(['install', '--package-lock-only', ...install, './.tools/packages/' + filename]);
    const expected = structuredClone(lock);
    expected.packages[entry].integrity = integrity;
    const verify = () => {
      assert.ok(readFileSync(manifestPath).equals(originalManifest), 'npm changed package.json');
      assert.deepEqual(JSON.parse(readFileSync(lockPath)), expected, 'npm changed fields beyond the local archive integrity');
    };
    verify();
    runNpm(['ci', ...install]);
    verify();
    return integrity;
  } catch (error) {
    const failures = [error];
    for (const [path, bytes] of [[manifestPath, originalManifest], [lockPath, originalLock]]) {
      try { writeFileSync(path, bytes); } catch (restoreError) { failures.push(restoreError); }
    }
    if (failures.length > 1) throw new AggregateError(failures, 'Setup failed; an original package file could not be restored');
    throw error;
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  try {
    assert.equal(process.argv.length, 2, 'Source example setup takes no arguments');
    setupNodeExample(fileURLToPath(new URL('../', import.meta.url)));
    console.log('Source Node example ready. Run node examples/node-host/follow-up/run.mjs');
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
