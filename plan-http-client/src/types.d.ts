export interface PlanIntervalOut {
  start: string;
  end: string;
}

export type PlanOut = Record<string, PlanIntervalOut>;

export type TimelineOut = string[];

export interface PlanHttpCompatibility {
  api: readonly string[];
  serverTags: readonly string[];
}

export interface PlanHttpClientOptions {
  fetch?: typeof fetch;
  compatibility?: PlanHttpCompatibility;
  strictVersion?: boolean;
}

export interface PlanHttpClient {
  health(): Promise<string>;
  plan(): Promise<PlanOut>;
  timeline(): Promise<TimelineOut>;
}

declare function planHttpClient(baseUrl: string, options?: PlanHttpClientOptions): Promise<PlanHttpClient>;

export default planHttpClient;
