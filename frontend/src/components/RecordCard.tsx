import React from "react";
import DataNode from "./DataNode";
import "../styles/RecordCard.css";
import type {
  AggregatedRecord,
  CatBreed,
  JsonPlaceholderUser,
} from "@interfaces/";

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

  console.log(record);

  const renderContent = () => {
    switch (record.source) {
      case "jsonplaceholder-users":
        return renderJsonPlaceholder(record.data as JsonPlaceholderUser);
      case "catfacts-breeds":
        return renderCatBreed(record.data as CatBreed);
      default:
        return <DataNode data={record.data} />;
    }
  };

  const renderJsonPlaceholder = (user: JsonPlaceholderUser) => (
    <div className="user-details">
      <div className="detail-row">
        <span className="label">Имя:</span> {user.name}
      </div>
      <div className="detail-row">
        <span className="label">Username:</span> {user.username}
      </div>
      <div className="detail-row">
        <span className="label">Email:</span> {user.email}
      </div>
      <div className="detail-row">
        <span className="label">Телефон:</span> {user.phone}
      </div>
      <div className="detail-row">
        <span className="label">Сайт:</span> {user.website}
      </div>
      <div className="detail-row">
        <span className="label">Адрес:</span>
      </div>
      <div className="nested">
        <div>
          {user.address.street}, {user.address.suite}
        </div>
        <div>
          {user.address.city}, {user.address.zipcode}
        </div>
        <div className="geo">
          📍 {user.address.geo.lat}, {user.address.geo.lng}
        </div>
      </div>
      <div className="detail-row">
        <span className="label">Компания:</span> {user.company.name}
      </div>
      <div className="nested">
        <div>{user.company.catchPhrase}</div>
        <div>{user.company.bs}</div>
      </div>
    </div>
  );

  const renderCatBreed = (breed: CatBreed) => (
    <div className="breed-details">
      <div className="detail-row">
        <span className="label">Порода:</span> {breed.breed}
      </div>
      <div className="detail-row">
        <span className="label">Страна:</span> {breed.country}
      </div>
      <div className="detail-row">
        <span className="label">Происхождение:</span> {breed.origin}
      </div>
      <div className="detail-row">
        <span className="label">Тип шерсти:</span> {breed.coat}
      </div>
      <div className="detail-row">
        <span className="label">Окрас:</span> {breed.pattern}
      </div>
    </div>
  );

  return (
    <div className="record-card">
      <div className="record-header">
        <div className="source-badge">{record.source}</div>
        <div className="timestamp">{formattedDate}</div>
      </div>
      <div className="record-content">{renderContent()}</div>
    </div>
  );
};

export default RecordCard;
