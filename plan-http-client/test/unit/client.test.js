import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import planHttpClient from '../../src/client.js';

describe('planHttpClient reads', () => {
  it('loads plan rows from /api/v1/plan', async () => {
    const fetchFn = async (url) => {
      assert.equal(url, 'http://127.0.0.1:8080/api/v1/plan');
      return {
        ok: true,
        status: 200,
        text: async () => JSON.stringify({ '7a/algebra/1': { start: '2026-06-01T08:00:00', end: '2026-06-01T08:45:00' } }),
      };
    };
    const api = await planHttpClient('http://127.0.0.1:8080', { fetch: fetchFn });
    const plan = await api.plan();
    assert.equal(plan['7a/algebra/1'].start, '2026-06-01T08:00:00');
  });

  it('loads timeline ids from /api/v1/timeline', async () => {
    const fetchFn = async (url) => {
      assert.equal(url, 'http://127.0.0.1:8080/api/v1/timeline');
      return {
        ok: true,
        status: 200,
        text: async () => JSON.stringify(['7a/algebra/1', '7a/history/1']),
      };
    };
    const api = await planHttpClient('http://127.0.0.1:8080', { fetch: fetchFn });
    const rows = await api.timeline();
    assert.deepEqual(rows, ['7a/algebra/1', '7a/history/1']);
  });
});
