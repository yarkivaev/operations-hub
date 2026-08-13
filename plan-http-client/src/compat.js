import defaultCompat from '../compat.json' with { type: 'json' };

/**
 * Thrown when server version is outside client compatibility matrix.
 */
export function planHttpVersionError(server, allowed) {
  const error = new Error(`plan-http server version ${server} is not in compatibility matrix ${allowed.join(',')}`);
  error.name = 'PlanHttpVersionError';
  error.serverVersion = server;
  error.allowedVersions = allowed;
  return error;
}

export function defaultCompatibility() {
  return Object.freeze({
    api: Object.freeze([...defaultCompat.api]),
    serverTags: Object.freeze([...defaultCompat.serverTags]),
  });
}

export function assertCompatible(meta, compatibility) {
  if (!compatibility.api.includes(meta.api)) {
    throw planHttpVersionError(meta.planHttp, compatibility.serverTags);
  }
  if (!compatibility.serverTags.includes(meta.planHttp)) {
    throw planHttpVersionError(meta.planHttp, compatibility.serverTags);
  }
}
