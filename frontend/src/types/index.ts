export * from "./dto";
export * from "./catFact";
export * from "./jsonPlaceHolder";
export * from "./apiData";

export interface FetchRequest {
  sources: string[];
  format: "json" | "csv";
  filename?: string;
  append: boolean;
  maxParallel?: number;
}

export interface SchedulerStartRequest {
  sources: string[];
  maxParallel: number;
  intervalSeconds: number;
  format: "json" | "csv";
  filename: string;
  append: boolean;
}

export type SchedulerStatus = Record<string, boolean>;
