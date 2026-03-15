import React from "react";
import "../styles/DataNode.css";

interface DataNodeProps {
  data: unknown;
  level?: number;
}

const DataNode: React.FC<DataNodeProps> = ({ data, level = 0 }) => {
  const indent = level * 20;

  if (data === null) {
    return (
      <div style={{ marginLeft: indent }} className="null-value">
        null
      </div>
    );
  }

  if (typeof data === "undefined") {
    return (
      <div style={{ marginLeft: indent }} className="undefined-value">
        undefined
      </div>
    );
  }

  if (typeof data === "string") {
    return (
      <div style={{ marginLeft: indent }} className="string-value">
        "{data}"
      </div>
    );
  }

  if (typeof data === "number" || typeof data === "boolean") {
    return (
      <div style={{ marginLeft: indent }} className="primitive-value">
        {String(data)}
      </div>
    );
  }

  if (Array.isArray(data)) {
    return (
      <div style={{ marginLeft: indent }} className="array-node">
        <span className="bracket">[</span>
        <div className="array-items">
          {data.map((item, index) => (
            <div key={index} className="array-item">
              <DataNode data={item} level={level + 1} />
              {index < data.length - 1 && <span className="comma">,</span>}
            </div>
          ))}
        </div>
        <span className="bracket">]</span>
      </div>
    );
  }

  if (typeof data === "object") {
    const obj = data as Record<string, unknown>;
    const entries = Object.entries(obj);
    if (entries.length === 0) {
      return (
        <div style={{ marginLeft: indent }} className="empty-object">
          &#123;&#125;
        </div>
      );
    }
    return (
      <div className="object-node" style={{ marginLeft: indent }}>
        <span className="brace">&#123;</span>
        <div className="object-properties">
          {entries.map(([key, value], index) => (
            <div key={key} className="property">
              <span className="key">{key}:</span>
              <DataNode data={value} level={level + 1} />
              {index < entries.length - 1 && <span className="comma">,</span>}
            </div>
          ))}
        </div>
        <span className="brace">&#125;</span>
      </div>
    );
  }

  return <div style={{ marginLeft: indent }}>Unknown type</div>;
};

export default DataNode;
