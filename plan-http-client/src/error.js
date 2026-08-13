/**
 * Thrown when plan-http HTTP request fails.
 */
export default function planHttpError(response, path) {
  const error = new Error(`plan-http request failed status ${response.status} at ${path}`);
  error.name = 'PlanHttpError';
  error.status = response.status;
  error.path = path;
  return error;
}
