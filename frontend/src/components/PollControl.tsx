import React, { useState, useEffect } from "react";
import type {
  ApiSourceInfo,
  FetchRequest,
  SchedulerStartRequest,
  SchedulerStatus,
} from "@interfaces/";
import {
  fetchData,
  startScheduler,
  stopScheduler,
  getSchedulerStatus,
} from "../api/client";
import SourcesList from "./SourcesList";

interface PollControlProps {
  sources: ApiSourceInfo[];
}

const PollControl: React.FC<PollControlProps> = ({ sources }) => {
  const [selectedSources, setSelectedSources] = useState<string[]>([]);
  const [format, setFormat] = useState<"json" | "csv">("json");
  const [filename, setFilename] = useState("data");
  const [append, setAppend] = useState(false);
  const [maxParallel, setMaxParallel] = useState(5);
  const [intervalSeconds, setIntervalSeconds] = useState(60);
  const [mode, setMode] = useState<"once" | "periodic">("once");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<SchedulerStatus>({});

  // Удаляем расширение из имени файла
  const getBaseName = (name: string) => name.replace(/\.[^/.]+$/, "");

  // Функция обновления статуса
  const refreshStatus = async () => {
    try {
      const res = await getSchedulerStatus();
      setStatus(res.data);
    } catch (err) {
      console.error("Failed to fetch scheduler status", err);
    }
  };

  // Загружаем статус при монтировании и каждые 5 секунд
  useEffect(() => {
    refreshStatus();
    const interval = setInterval(refreshStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedSources.length === 0) {
      setError("Выберите хотя бы один источник");
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);

    const baseName = getBaseName(filename.trim()) || "data";

    try {
      if (mode === "once") {
        const request: FetchRequest = {
          sources: selectedSources,
          format,
          filename: baseName,
          append,
          maxParallel,
        };
        const response = await fetchData(request);
        setResult(response.data);
      } else {
        const request: SchedulerStartRequest = {
          sources: selectedSources,
          maxParallel,
          intervalSeconds,
          format,
          filename: baseName,
          append,
        };
        const response = await startScheduler(request);
        setResult(response.data);
        // Ждём 2 секунды, чтобы планировщик создал задачи, затем обновляем статус
        setTimeout(refreshStatus, 2000);
      }
    } catch (err) {
      setError("Ошибка при выполнении запроса");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleStopJob = async () => {
    setLoading(true);
    try {
      const baseName = getBaseName(filename.trim()) || "data";
      await stopScheduler(baseName);
      await refreshStatus(); // обновляем статус после остановки
      setResult(`Задача для ${baseName} остановлена`);
    } catch (err) {
      setError("Ошибка при остановке задачи");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Активна ли задача для текущего файла?
  const isActive = Object.keys(status).some((key) =>
    key.startsWith(`${getBaseName(filename)}:`),
  );

  return (
    <div className="poll-control">
      <form onSubmit={handleSubmit} className="form-card">
        <SourcesList
          sources={sources}
          selected={selectedSources}
          onChange={setSelectedSources}
        />

        <div className="form-group">
          <label>Режим:</label>
          <div className="mode-switch">
            <button
              type="button"
              onClick={() => setMode("once")}
              className={mode === "once" ? "active" : ""}
            >
              Однократный
            </button>
            <button
              type="button"
              onClick={() => setMode("periodic")}
              className={mode === "periodic" ? "active" : ""}
            >
              Периодический
            </button>
          </div>
        </div>

        <div className="form-group">
          <label>
            Максимальное количество параллельных задач (maxParallel):
          </label>
          <input
            type="number"
            value={maxParallel}
            onChange={(e) => setMaxParallel(Number(e.target.value))}
            min={1}
            max={20}
          />
        </div>

        {mode === "periodic" && (
          <div className="form-group">
            <label>Интервал опроса (секунд):</label>
            <input
              type="number"
              value={intervalSeconds}
              onChange={(e) => setIntervalSeconds(Number(e.target.value))}
              min={1}
            />
          </div>
        )}

        <div className="form-group">
          <label>Формат файла:</label>
          <select
            value={format}
            onChange={(e) => setFormat(e.target.value as "json" | "csv")}
          >
            <option value="json">JSON</option>
            <option value="csv">CSV</option>
          </select>
        </div>

        <div className="form-group">
          <label>Имя файла (без расширения):</label>
          <input
            type="text"
            value={filename}
            onChange={(e) => setFilename(e.target.value)}
            placeholder="data"
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
          {loading
            ? "Загрузка..."
            : mode === "once"
              ? "Выполнить сбор"
              : "Запустить периодический сбор"}
        </button>

        {mode === "periodic" && isActive && (
          <button
            type="button"
            onClick={handleStopJob}
            disabled={loading}
            className="stop-button"
          >
            {loading ? "Остановка..." : "Остановить сбор"}
          </button>
        )}

        {error && <div className="error-message">{error}</div>}
        {result && <div className="success-message">{result}</div>}
      </form>

      {/* Панель статуса отображается всегда, если есть задачи */}
      {Object.keys(status).length > 0 && (
        <div className="status-panel">
          <h4>Статус активных задач</h4>
          <ul>
            {Object.entries(status).map(([task, active]) => (
              <li key={task}>
                {task}: {active ? "🟢 активна" : "🔴 остановлена"}
              </li>
            ))}
          </ul>
          <button onClick={refreshStatus} className="refresh-button">
            Обновить статус
          </button>
        </div>
      )}
    </div>
  );
};

export default PollControl;
