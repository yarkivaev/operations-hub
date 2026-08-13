import planHttpError from './error.js';

/**
 * Performs JSON HTTP GET against plan-http read API.
 */
export default async function request(baseUrl, fetchFn, path, query) {
  const root = baseUrl.replace(/\/$/, '');
  const suffix = query ? `?${query}` : '';
  const url = `${root}${path}${suffix}`;
  const response = await fetchFn(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw planHttpError(response, path);
  }
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  if (text.length === 0) {
    return null;
  }
  return JSON.parse(text);
}

export async function requestText(baseUrl, fetchFn, path) {
  const root = baseUrl.replace(/\/$/, '');
  const url = `${root}${path}`;
  const response = await fetchFn(url, { headers: { Accept: 'text/plain' } });
  if (!response.ok) {
    throw planHttpError(response, path);
  }
  return response.text();
}
