import type { CatBreed } from "./catFact";
import type { JsonPlaceholderUser } from "./jsonPlaceHolder";

export interface BibleChapter {
  verses: string[];
}

export interface BibleBook {
  chapters: BibleChapter[];
}

export type BibleData = Record<string, BibleBook>;

export type RecordData = CatBreed | JsonPlaceholderUser | BibleData;
