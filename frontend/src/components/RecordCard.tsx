import React from "react";
import RecordContent from "./RecordContent";
import "../styles/RecordCard.css";
import type { AggregatedRecord } from "@interfaces/";

interface RecordCardProps {
  record: AggregatedRecord;
}

const RecordCard: React.FC<RecordCardProps> = ({ record }) => {
  const formattedDate = new Date(record.timestamp).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });

  return (
    <div className="record-card">
      <div className="record-header">
        <div className="source-badge">{record.source}</div>
        <div className="timestamp">{formattedDate}</div>
      </div>
      <div className="record-content">
        <RecordContent source={record.source} data={record.data} />
      </div>
    </div>
  );
};

export default RecordCard;
