import React from "react";
import DataNode from "./DataNode";
import type { BibleData, CatBreed, JsonPlaceholderUser } from "@interfaces/";

interface RecordContentProps {
  source: string;
  data: unknown;
}

const isCatFactBreed = (data: unknown): data is CatBreed => {
  return (
    data !== null &&
    typeof data === "object" &&
    "breed" in data &&
    "country" in data
  );
};

const isJsonPlaceholderUser = (data: unknown): data is JsonPlaceholderUser => {
  return (
    data !== null &&
    typeof data === "object" &&
    "id" in data &&
    "name" in data &&
    "email" in data
  );
};

const isBibleData = (data: unknown): data is BibleData => {
  return (
    data !== null &&
    typeof data === "object" &&
    Object.values(data).some(
      (val: unknown) => val && typeof val === "object" && "chapters" in val,
    )
  );
};

const RecordContent: React.FC<RecordContentProps> = ({ source, data }) => {
  if (!data) return <div>Нет данных</div>;

  switch (source) {
    case "cat-facts-breeds":
      if (isCatFactBreed(data)) return <CatBreedsView data={data} />;
      break;
    case "jsonplaceholder-users":
      if (isJsonPlaceholderUser(data))
        return <JsonPlaceholderUserView data={data} />;
      break;
    case "justbible":
      if (isBibleData(data)) return <JustBibleView data={data} />;
      break;
    default:
      return <DataNode data={data} />;
  }
  return <DataNode data={data} />;
};

const CatBreedsView = ({ data }: { data: CatBreed }) => {
  return (
    <div className="cat-breed">
      <div className="field">
        <span className="field-label">Порода:</span> {data.breed}
      </div>
      <div className="field">
        <span className="field-label">Страна:</span> {data.country}
      </div>
      <div className="field">
        <span className="field-label">Происхождение:</span> {data.origin}
      </div>
      <div className="field">
        <span className="field-label">Шерсть:</span> {data.coat}
      </div>
      <div className="field">
        <span className="field-label">Окрас:</span> {data.pattern}
      </div>
    </div>
  );
};

const JsonPlaceholderUserView = ({ data }: { data: JsonPlaceholderUser }) => {
  const address = data.address;
  return (
    <div className="json-user">
      <div className="field">
        <span className="field-label">Имя:</span> {data.name}
      </div>
      <div className="field">
        <span className="field-label">Username:</span> {data.username}
      </div>
      <div className="field">
        <span className="field-label">Email:</span> {data.email}
      </div>
      <div className="field">
        <span className="field-label">Город:</span> {address.city}
      </div>
      <div className="field">
        <span className="field-label">Улица:</span> {address.street}
      </div>
      <div className="field">
        <span className="field-label">Телефон:</span> {data.phone}
      </div>
      <div className="field">
        <span className="field-label">Сайт:</span> {data.website}
      </div>
    </div>
  );
};

const JustBibleView = ({ data }: { data: BibleData }) => {
  const books = Object.entries(data);
  const totalBooks = books.length;
  const totalChapters = books.reduce(
    (sum, [_, book]) => sum + (book.chapters?.length || 0),
    0,
  );
  return (
    <div className="justbible">
      <div className="field">
        <span className="field-label">Перевод:</span> Синодальный (русский)
      </div>
      <div className="field">
        <span className="field-label">Книг:</span> {totalBooks}
      </div>
      <div className="field">
        <span className="field-label">Глав всего:</span> {totalChapters}
      </div>
      <details>
        <summary>Список книг и глав</summary>
        <ul className="books-list">
          {books.map(([bookName, book]) => (
            <li key={bookName}>
              <strong>{bookName}</strong> ({book.chapters?.length || 0} глав)
              {book.chapters && book.chapters.length > 0 && (
                <details>
                  <summary>Показать главы</summary>
                  <ul>
                    {book.chapters.map((chapter, idx) => (
                      <li key={idx}>
                        Глава {idx + 1}: {chapter.verses?.length || 0} стихов
                      </li>
                    ))}
                  </ul>
                </details>
              )}
            </li>
          ))}
        </ul>
      </details>
    </div>
  );
};

export default RecordContent;
