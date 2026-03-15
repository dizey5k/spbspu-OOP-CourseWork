import React from "react";
import "../styles/SourcesList.css";
import type { ApiSourceInfo } from "@interfaces/";

interface SourcesListProps {
  sources: ApiSourceInfo[];
  selected: string[];
  onChange: (selected: string[]) => void;
}

const SourcesList: React.FC<SourcesListProps> = ({
  sources,
  selected,
  onChange,
}) => {
  const toggleSource = (name: string) => {
    const newSelected = selected.includes(name)
      ? selected.filter((s) => s !== name)
      : [...selected, name];
    onChange(newSelected);
  };

  return (
    <div className="sources-list">
      <h3>Выберите источники:</h3>
      <div className="chips">
        {sources.map((source) => (
          <button
            key={source.name}
            className={`chip ${selected.includes(source.name) ? "selected" : ""}`}
            onClick={() => toggleSource(source.name)}
            type="button"
          >
            {source.displayName}
          </button>
        ))}
      </div>
    </div>
  );
};

export default SourcesList;
