import { useState, useEffect } from "react";
import { getSources } from "./api/client";
import FetchForm from "./components/FetchForm";
import "./App.css";
import type { ApiSourceInfo } from "./types";
import DataViewer from "./components/DataViewer";

function App() {
  const [sources, setSources] = useState<ApiSourceInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"fetch" | "view">("fetch");

  useEffect(() => {
    loadSources();
  }, []);

  const loadSources = async () => {
    setLoading(true);
    try {
      const response = await getSources();
      setSources(response.data);
      setError(null);
    } catch (err) {
      setError("Не удалось загрузить список источников");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app">
      <h1>Coursework Data Aggregator</h1>
      {error && <div className="error">{error}</div>}

      <nav>
        <button
          onClick={() => setActiveTab("fetch")}
          disabled={activeTab === "fetch"}
        >
          Сбор данных
        </button>
        <button
          onClick={() => setActiveTab("view")}
          disabled={activeTab === "view"}
        >
          Просмотр данных
        </button>
      </nav>

      <main>
        {activeTab === "fetch" ? (
          <FetchForm sources={sources} />
        ) : (
          <DataViewer sources={sources} />
        )}
      </main>
    </div>
  );
}

export default App;
