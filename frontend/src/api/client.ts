import type {
  AggregatedRecord,
  ApiSourceInfo,
  FetchRequest,
  FetchResponse,
} from "@interfaces/";
import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

export const getSources = () => apiClient.get<ApiSourceInfo[]>("/sources");
export const fetchData = (request: FetchRequest) =>
  apiClient.post<FetchResponse>("/fetch", request);
export const getData = (filename: string, format: string, source?: string) =>
  apiClient.get<AggregatedRecord[]>("/data", {
    params: { filename, format, source },
  });
