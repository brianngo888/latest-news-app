export type NewsArticle = {
  id: string;
  title: string;
  source: string;
  url: string;
  summary?: string | null;
  category?: string | null;
  publishedAt?: string | null;
  firstSeenAt?: string | null;
  updatedAt?: string | null;
};

export type NewsResponse = {
  items: NewsArticle[];
  responseAt: string;
  count: number;
};

export type CollectionStatus = {
  lastAttemptAt?: string | null;
  lastSuccessAt?: string | null;
  lastError?: string | null;
  lastItemCount: number;
};
