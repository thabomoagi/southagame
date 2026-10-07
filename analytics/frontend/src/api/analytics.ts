const API_BASE =
  import.meta.env.VITE_API_BASE_URL ??
  'http://127.0.0.1:8000/api/analytics';

export interface OverviewStats {
  total_users: number;
  total_attempts: number;
  total_questions: number;
  total_categories: number;
}

export interface MostPlayedQuestion {
  id: number;
  prompt: string;
  play_count: number;
}

export interface AccuracyQuestion {
  id: number;
  prompt: string;
  total_attempts: number;
  correct_count: number;
  accuracy_percentage: number;
}

export interface MissedQuestion {
  id: number;
  prompt: string;
  miss_count: number;
  total_attempts: number;
}

export interface ResponseTimeQuestion {
  id: number;
  prompt: string;
  avg_time_ms: number;
  total_attempts: number;
}

async function request<T>(url: string): Promise<T> {
  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(`Analytics request failed (${response.status})`);
  }

  return response.json() as Promise<T>;
}

export function fetchOverview(): Promise<OverviewStats> {
  return request<OverviewStats>(`${API_BASE}/overview`);
}

export function fetchMostPlayed(
  limit = 5,
): Promise<MostPlayedQuestion[]> {
  return request<MostPlayedQuestion[]>(
    `${API_BASE}/questions/most-played?limit=${limit}`,
  );
}

export function fetchHardest(
  limit = 5,
): Promise<AccuracyQuestion[]> {
  return request<AccuracyQuestion[]>(
    `${API_BASE}/questions/hardest?limit=${limit}`,
  );
}

export function fetchEasiest(
  limit = 5,
): Promise<AccuracyQuestion[]> {
  return request<AccuracyQuestion[]>(
    `${API_BASE}/questions/easiest?limit=${limit}`,
  );
}

export function fetchMostMissed(
  limit = 5,
): Promise<MissedQuestion[]> {
  return request<MissedQuestion[]>(
    `${API_BASE}/questions/most-missed?limit=${limit}`,
  );
}

export function fetchFastest(
  limit = 5,
): Promise<ResponseTimeQuestion[]> {
  return request<ResponseTimeQuestion[]>(
    `${API_BASE}/questions/fastest?limit=${limit}`,
  );
}

export function fetchSlowest(
  limit = 5,
): Promise<ResponseTimeQuestion[]> {
  return request<ResponseTimeQuestion[]>(
    `${API_BASE}/questions/slowest?limit=${limit}`,
  );
}