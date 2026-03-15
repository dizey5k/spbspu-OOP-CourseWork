import type { CatBreed } from "./catFact";
import type { JsonPlaceholderUser } from "./jsonPlaceHolder";

export interface ApiSourceInfo {
  name: string;
  displayName: string;
}

export interface FetchRequest {
  sources: string[];
  format: "json" | "csv";
  filename?: string;
  append: boolean;
}

export interface AggregatedRecord {
  id: string;
  source: string;
  timestamp: string;
  data: JsonPlaceholderUser | CatBreed;
}
export type FetchResponse = string;
