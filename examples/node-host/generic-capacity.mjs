import assert from 'node:assert/strict';
import { closeSync, openSync, readSync } from 'node:fs';

const MODEL_BYTES = 65_536;
const ARTIFACT_BYTES = 1_048_576;

export function readBoundedFixture(file, max) {
  const descriptor = openSync(file, 'r');
  try {
    const buffer = Buffer.alloc(max + 1);
    let length = 0;
    while (length < buffer.length) {
      const count = readSync(descriptor, buffer, length, buffer.length - length, null);
      if (!count) break;
      length += count;
    }
    assert.ok(length <= max, 'Bounded trusted fixture file');
    return Buffer.from(buffer.subarray(0, length));
  } finally {
    closeSync(descriptor);
  }
}

function capacityError() {
  return Object.assign(new Error('Harness capacity exceeded.'), { reason: 'CAPACITY_EXCEEDED' });
}

export function createHarness(api) {
  const models = new Map();
  const prepared = new Map();
  let operation = false;
  let inflight = 0;

  function beginOperation() {
    if (operation) throw capacityError();
    operation = true;
  }

  function endOperation() { operation = false; }

  return Object.freeze({
    bind(bytes, approval, invokers) {
      if (!(bytes instanceof Uint8Array) || bytes.byteLength > MODEL_BYTES || models.size >= 4) throw capacityError();
      beginOperation();
      try {
        const result = api.bindModel(bytes, approval, invokers);
        if (result?.boundModel) models.set(result.boundModel, { prepared: 0, inflight: 0 });
        return Object.freeze({ boundModel: result?.boundModel ?? null, diagnostic: result?.diagnostic ?? null });
      } finally { endOperation(); }
    },
    prepare(bytes, bound, admission) {
      const model = models.get(bound);
      if (!model || !(bytes instanceof Uint8Array) || bytes.byteLength > ARTIFACT_BYTES || prepared.size >= 8) throw capacityError();
      beginOperation();
      try {
        const result = api.prepareProgram(bytes, bound, admission);
        if (result?.prepared) {
          prepared.set(result.prepared, { model, inflight: 0, bytes: bytes.byteLength });
          model.prepared++;
        }
        return Object.freeze({ prepared: result?.prepared ?? null, diagnostic: result?.diagnostic ?? null });
      } finally { endOperation(); }
    },
    async invoke(handle, input, context) {
      const item = prepared.get(handle);
      if (!item || inflight >= 2) throw capacityError();
      inflight++;
      item.inflight++;
      item.model.inflight++;
      try { return await api.invokeProgram(handle, input, context); }
      finally {
        inflight--;
        item.inflight--;
        item.model.inflight--;
      }
    },
    releasePrepared(handle) {
      const item = prepared.get(handle);
      if (!item || item.inflight) return false;
      prepared.delete(handle);
      item.model.prepared--;
      return true;
    },
    releaseModel(handle) {
      const item = models.get(handle);
      if (!item || item.prepared || item.inflight) return false;
      models.delete(handle);
      return true;
    },
    usage() {
      // Reserve each model's full canonical/descriptor ceiling without a second descriptor parser.
      return Object.freeze({ models: models.size, canonicalModelBytesReserved: models.size * MODEL_BYTES, descriptorBytesReserved: models.size * MODEL_BYTES,
        prepared: prepared.size, artifactBytesRetained: [...prepared.values()].reduce((n, item) => n + item.bytes, 0), inflight });
    },
  });
}
