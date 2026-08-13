import { assertCompatible, defaultCompatibility } from './compat.js';
import request, { requestText } from './request.js';

/**
 * Creates a frozen read-only client for operations-hub plan-http.
 *
 * Endpoints: `GET /health`, `GET /api/v1/plan`, `GET /api/v1/timeline`.
 * Version handshake against `/api/v1/meta` is opt-in via `strictVersion`.
 */
export default async function planHttpClient(baseUrl, options = {}) {
  const fetchFn = options.fetch ?? globalThis.fetch;
  const compatibility = options.compatibility ?? defaultCompatibility();
  const strictVersion = options.strictVersion ?? false;
  if (strictVersion) {
    const meta = await request(baseUrl, fetchFn, '/api/v1/meta');
    assertCompatible(meta, compatibility);
  }
  return Object.freeze({
    health() {
      return requestText(baseUrl, fetchFn, '/health');
    },
    plan() {
      return request(baseUrl, fetchFn, '/api/v1/plan');
    },
    timeline() {
      return request(baseUrl, fetchFn, '/api/v1/timeline');
    },
  });
}
