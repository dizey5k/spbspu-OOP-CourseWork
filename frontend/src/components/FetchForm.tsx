import React, { useState } from "react";
import { fetchData } from "../api/client";
import SourcesList from "./SourcesList";
import "../styles/FetchForm.css";
import type { ApiSourceInfo, FetchRequest } from "@interfaces/";

interface FetchFormProps {
  sources: ApiSourceInfo[];
}

const FetchForm: React.FC<FetchFormProps> = ({ sources }) => {
  const [selectedSources, setSelectedSources] = useState<string[]>([]);
  const [format, setFormat] = useState<"json" | "csv">("json");
  const [filename, setFilename] = useState("");
  const [append, setAppend] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedSources.length === 0) {
      setError("Выберите хотя бы один источник");
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);

    const request: FetchRequest = {
      sources: selectedSources,
      format,
      filename: filename.trim() || undefined,
      append,
    };

    try {
      const response = await fetchData(request);
      setResult(response.data);
    } catch (err) {
      setError("Ошибка при выполнении запроса");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="fetch-form">
      <div className="form-card">
        <SourcesList
          sources={sources}
          selected={selectedSources}
          onChange={setSelectedSources}
        />

        <div className="form-group">
          <label>Формат файла:</label>
          <select
            value={format}
            onChange={(e) => setFormat(e.target.value as "json" | "csv")}
            className="glass-select"
          >
            <option value="json">JSON</option>
            <option value="csv">CSV</option>
          </select>
        </div>

        <div className="form-group">
          <label>Имя файла (оставьте пустым для data.{format})</label>
          <input
            type="text"
            value={filename}
            onChange={(e) => setFilename(e.target.value)}
            className="glass-input"
          />
        </div>

        <div className="form-group checkbox">
          <label>
            <input
              type="checkbox"
              checked={append}
              onChange={(e) => setAppend(e.target.checked)}
            />
            Дозапись (append)
          </label>
        </div>

        <button type="submit" disabled={loading} className="glass-button">
          {loading ? "Загрузка..." : "Выполнить сбор"}
        </button>

        {error && <div className="error-message">{error}</div>}
        {result && <div className="success-message">{result}</div>}
      </div>
    </form>
  );
};

export default FetchForm;
