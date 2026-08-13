import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { describe, it } from 'node:test';
import defaultCompat from '../../compat.json' with { type: 'json' };
import { assertCompatible, planHttpVersionError } from '../../src/compat.js';
import planHttpClient from '../../src/client.js';

const compatibility = Object.freeze({
  api: Object.freeze(['v1']),
  serverTags: Object.freeze(['0.1.0']),
});

describe('assertCompatible', () => {
  it('accepts meta inside compatibility matrix', () => {
    const packageJson = JSON.parse(readFileSync(new URL('../../package.json', import.meta.url), 'utf8'));
    assert.doesNotThrow(() => assertCompatible({ api: 'v1', planHttp: '0.1.0', tag: 'v0.1.0' }, compatibility));
    assert.doesNotThrow(() => assertCompatible(
      { api: 'v1', planHttp: packageJson.version, tag: `v${packageJson.version}` },
      { ...compatibility, serverTags: Object.freeze([...defaultCompat.serverTags]) },
    ));
  });
});

describe('planHttpVersionError', () => {
  it('names PlanHttpVersionError', () => {
    assert.equal(planHttpVersionError('0.3.0', ['0.1.0']).name, 'PlanHttpVersionError');
  });
});

describe('planHttpClient version gate', () => {
  it('rejects server outside compatibility matrix', async () => {
    const fetchFn = async () => ({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ api: 'v1', planHttp: '0.3.0', tag: 'v0.3.0' }),
    });
    await assert.rejects(() => planHttpClient('http://127.0.0.1:8080', { fetch: fetchFn, compatibility, strictVersion: true }), (err) => err.name === 'PlanHttpVersionError');
  });
});
