import type { NewsResponse, CollectionStatus } from '../types/news';

const API_BASE_URL = (process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://192.168.1.111:8080').replace(/\/$/, '');

async function getJson<T>(path: string): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12_000);
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      signal: controller.signal,
    });
    if (!response.ok) throw new Error(`API request failed (${response.status})`);
    return (await response.json()) as T;
  } finally {
    clearTimeout(timeout);
  }
}
export async function fetchNews() {
  const response = await fetch(`${API_BASE_URL}/api/v1/news`); // Or your specific endpoint path
  if (!response.ok) {
    throw new Error(`Server returned status ${response.status}`);
  }
  return response.json();
}
// export const fetchNews = () => getJson<NewsResponse>('/api/v1/news?limit=20');
export const fetchCollectionStatus = () => getJson<CollectionStatus>('/api/v1/admin/collection/status');
