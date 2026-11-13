import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { canonicalBytes, digest } from '../src/canonical.js';
import { bindModel, prepareProgram, ENVIRONMENT } from '../src/index.js';

const originalModel = JSON.parse(readFileSync(new URL('../../conformance/keir-2/dcm.json', import.meta.url)));
function decisionModel() {
  const dcm = structuredClone(originalModel);
  dcm.format = 'keir-dcm/3';
  dcm.types.Decision = { kind: 'enum', values: ['ALLOW', 'DENY'] };
  dcm.types.DecisionResult = { kind: 'record', fields: {
    decision: 'Decision', message: { type: 'String', optional: true },
  } };
  Object.assign(dcm.entrypoints.TaskCompleted, {
    trigger: 'REQUEST', placement: 'BEFORE_COMMIT', consumer: { id: 'sample.decision', version: '1', kind: 'DECISION' },
    output: 'DecisionResult',
  });
  return dcm;
}

function bind(dcm) {
  const implementations = Object.fromEntries(Object.entries(dcm.capabilities).map(([id, cap]) => [id, {
    version: cap.version, mode: 'simulation', keyType: dcm.hostKey.type,
    invoke() { assert.fail('Admission dispatched an implementation'); },
  }]));
  return bindModel(canonicalBytes(dcm), implementations);
}

test('decision model admits both enum orders and only zero write grants', () => {
  for (const format of ['keir-dcm/3', 'keir-dcm/4']) for (const values of [['ALLOW', 'DENY'], ['DENY', 'ALLOW']]) {
    for (const effectGrant of [undefined, 0]) {
      const dcm = decisionModel();
      dcm.format = format;
      dcm.types.Decision.values = values;
      if (effectGrant !== undefined) dcm.entrypoints.TaskCompleted.effectGrant = effectGrant;
      assert.equal(bind(dcm).diagnostic, null);
    }
  }
  assert.equal(bind(originalModel).diagnostic, null);
  assert.equal(bind({ ...originalModel, format: 'keir-dcm/3' }).diagnostic, null);
});

test('decision model rejects wrong versions, lifecycles, output types and nonzero grant', () => {
  for (const mutate of [
    dcm => { dcm.format = 'keir-dcm/2'; },
    dcm => { dcm.format = 'keir-dcm/5'; },
    dcm => { dcm.entrypoints.TaskCompleted.trigger = 'EVENT'; },
    dcm => { dcm.entrypoints.TaskCompleted.placement = 'AFTER_COMMIT'; },
    dcm => { dcm.entrypoints.TaskCompleted.consumer.kind = 'EVENT_REPORT'; },
    dcm => { dcm.entrypoints.TaskCompleted.effectGrant = 1; },
    dcm => { delete dcm.types.DecisionResult.fields.decision; },
    dcm => { dcm.types.DecisionResult.fields.decision = { type: 'Decision', optional: true }; },
    dcm => { dcm.types.DecisionResult.fields.decision = 'String'; },
    dcm => { dcm.types.Decision.values = ['ALLOW']; },
    dcm => { dcm.types.Decision.values = ['ALLOW', 'ALLOW']; },
    dcm => { dcm.types.Decision.values = ['ALLOW', 'DENY', 'OTHER']; },
    dcm => { dcm.types.Decision.values = ['ALLOW', 'deny']; },
  ]) {
    for (const format of ['keir-dcm/3', 'keir-dcm/4']) {
      const dcm = decisionModel();
      dcm.format = format;
      mutate(dcm);
      const denied = bind(dcm);
      assert.equal(denied.boundModel, null);
      assert.equal(denied.diagnostic.code, 'ADMISSION_INVALID');
    }
  }
});

test('decision output permits required and optional scalars but rejects a named list field', () => {
  const dcm = decisionModel();
  Object.assign(dcm.types.DecisionResult.fields, {
    ready: 'Boolean', at: 'Timestamp', reference: 'ProjectId',
    optionalReference: { type: 'ProjectId', optional: true },
  });
  assert.equal(bind(dcm).diagnostic, null);
  dcm.types.Labels = { kind: 'list', element: 'String', maxItems: 2 };
  dcm.types.DecisionResult.fields.details = 'Labels';
  const denied = bind(dcm);
  assert.equal(denied.boundModel, null);
  assert.deepEqual({ code: denied.diagnostic.code, path: denied.diagnostic.path },
    { code: 'ADMISSION_INVALID', path: '/binding/dcm' });

  Object.assign(dcm.entrypoints.TaskCompleted, {
    trigger: 'EVENT', placement: 'AFTER_COMMIT',
    consumer: { id: 'sample.report', version: '1', kind: 'EVENT_REPORT' },
  });
  assert.equal(bind(dcm).diagnostic, null);
});

test('forged direct and false-guarded decision writes fail preparation before dispatch', () => {
  const dcm = decisionModel(), binding = bind(dcm);
  const resultType = dcm.capabilities['tasks.create'].result;
  for (const guarded of [false, true]) {
    const artifact = {
      format: 'keir-artifact/3', compiler: 'keir-compiler-java/0.3.0', environment: ENVIRONMENT,
      sourceDigest: 'sha256:' + '0'.repeat(64),
      modelRef: { domainId: dcm.domain.id, domainVersion: dcm.domain.version,
        format: dcm.format, digest: digest(canonicalBytes(dcm)), descriptorRecipe: 'keir-record-descriptors/2' },
      entrypoint: 'TaskCompleted', expressions: {}, steps: [
        { op: 'BIND', id: 'ready', resultType: 'Boolean', value: { literal: false, type: 'Boolean' } },
        { op: 'CALL', id: 'write', capability: 'tasks.create', version: '1', resultType,
          args: { projectId: { literal: 'project1', type: 'ProjectId' }, title: { literal: 'x', type: 'String' } },
          ...(guarded ? { when: 'ready' } : {}) },
      ],
      return: { type: 'DecisionResult', fields: { decision: { literal: 'ALLOW', type: 'Decision' } } },
    };
    const denied = prepareProgram(canonicalBytes(artifact), binding.boundModel);
    assert.equal(denied.prepared, null);
    assert.deepEqual({ code: denied.diagnostic.code, path: denied.diagnostic.path },
      { code: 'ENTRY_GRANT_DENIED', path: '/artifact/steps/1' });
  }
});

test('mixed generation tuples report producer mismatch before bound model mismatch', () => {
  const artifact = JSON.parse(readFileSync(new URL('../../conformance/keir-2/artifact.json', import.meta.url)));
  for (const modelVersion of ['2', '3']) {
    const dcm = { ...originalModel, format: 'keir-dcm/' + modelVersion }, binding = bind(dcm);
    assert.equal(binding.diagnostic, null);
    for (const artifactVersion of ['2', '3']) {
      for (const producerVersion of ['2', '3']) {
        if (modelVersion === artifactVersion && artifactVersion === producerVersion) continue;
        const mixed = { ...artifact, format: 'keir-artifact/' + artifactVersion,
          compiler: 'keir-compiler-java/0.' + producerVersion + '.0' };
        const denied = prepareProgram(canonicalBytes(mixed), binding.boundModel);
        assert.equal(denied.prepared, null);
        assert.deepEqual({ code: denied.diagnostic.code, path: denied.diagnostic.path }, {
          code: 'PIN_MISMATCH',
          path: producerVersion !== artifactVersion ? '/artifact/compiler' : '/artifact/modelRef',
        });
      }
    }
  }
});
