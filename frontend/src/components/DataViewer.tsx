import React, { useState } from "react";
import { getData } from "../api/client";
import RecordCard from "./RecordCard";
import "../styles/DataViewer.css";
import type { AggregatedRecord, ApiSourceInfo } from "@interfaces/";

interface DataViewerProps {
  sources: ApiSourceInfo[];
}

const DataViewer: React.FC<DataViewerProps> = ({ sources }) => {
  const [filename, setFilename] = useState("data.json");
  const [format, setFormat] = useState<"json" | "csv">("json");
  const [selectedSource, setSelectedSource] = useState<string>("");
  const [records, setRecords] = useState<AggregatedRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getData(
        filename,
        format,
        selectedSource || undefined,
      );
      setRecords(response.data);
    } catch (err) {
      setError("Не удалось загрузить данные");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="data-viewer">
      <div className="filter-panel">
        <h3>Просмотр сохранённых данных</h3>
        <div className="filter-grid">
          <div className="form-group">
            <label>Имя файла:</label>
            <input
              type="text"
              value={filename}
              onChange={(e) => setFilename(e.target.value)}
              className="glass-input"
            />
          </div>
          <div className="form-group">
            <label>Формат:</label>
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
            <label>Фильтр по источнику:</label>
            <select
              value={selectedSource}
              onChange={(e) => setSelectedSource(e.target.value)}
              className="glass-select"
            >
              <option value="">Все источники</option>
              {sources.map((s) => (
                <option key={s.name} value={s.name}>
                  {s.displayName}
                </option>
              ))}
            </select>
          </div>
        </div>
        <button onClick={loadData} disabled={loading} className="glass-button">
          {loading ? "Загрузка..." : "Загрузить данные"}
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      {records.length > 0 ? (
        <div className="records-list">
          {records.map((record) => (
            <RecordCard key={record.id} record={record} />
          ))}
        </div>
      ) : (
        !loading && <p className="no-records">Нет записей для отображения.</p>
      )}
    </div>
  );
};

export default DataViewer;
