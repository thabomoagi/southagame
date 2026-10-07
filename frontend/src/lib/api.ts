import { PUBLIC_API_BASE_URL } from '$env/static/public';

const BASE = PUBLIC_API_BASE_URL;
const COLD_START_TIMEOUT = 60000;

let refreshPromise: Promise<boolean> | null = null;

function getAccessToken(): string | null {
	if (typeof window === 'undefined') return null;
	return localStorage.getItem('accessToken');
}

function getRefreshToken(): string | null {
	if (typeof window === 'undefined') return null;
	return localStorage.getItem('refreshToken');
}

function clearSession() {
	if (typeof window === 'undefined') return;

	localStorage.removeItem('accessToken');
	localStorage.removeItem('refreshToken');
	localStorage.removeItem('user');
}

export function hasUsableSession(): boolean {
	return Boolean(isAccessTokenUnexpired(getAccessToken()) || getRefreshToken());
}

function responseMessage(payload: unknown, fallback: string): string {
	if (!payload || typeof payload !== 'object') return fallback;

	const data = payload as Record<string, unknown>;
	return String(data.message ?? data.detail ?? data.title ?? fallback);
}

async function readResponse<T>(res: Response, fallback: string): Promise<T> {
	const text = await res.text();
	let payload: any = null;

	if (text) {
		try {
			payload = JSON.parse(text);
		} catch {
			payload = null;
		}
	}

	if (!res.ok) {
		throw new Error(responseMessage(payload, `${fallback} (${res.status})`));
	}

	if (!payload) {
		return undefined as T;
	}

	if (payload.success === false) {
		throw new Error(responseMessage(payload, fallback));
	}

	return (payload.data ?? payload) as T;
}

function base64UrlDecode(input: string): string {
	let base64 = input.replace(/-/g, '+').replace(/_/g, '/');
	const pad = base64.length % 4;

	if (pad) {
		base64 += '='.repeat(4 - pad);
	}

	return decodeURIComponent(
		atob(base64)
			.split('')
			.map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
			.join('')
	);
}

function isAccessTokenUnexpired(token: string | null): boolean {
	if (!token) return false;

	try {
		const parts = token.split('.');
		if (parts.length < 2) return false;

		const payload = JSON.parse(base64UrlDecode(parts[1]));
		const exp = Number(payload?.exp);

		if (!Number.isFinite(exp) || exp <= 0) {
			return false;
		}

		const expiresMs = exp > 1e12 ? exp : exp * 1000;

		return expiresMs > Date.now() + 5000;
	} catch {
		return false;
	}
}

export async function resolveSessionOnEntry(): Promise<boolean> {
	if (typeof window === 'undefined') return false;

	const access = getAccessToken();
	const refresh = getRefreshToken();

	if (!access && !refresh) {
		return false;
	}

	if (isAccessTokenUnexpired(access)) {
		try {
			const res = await fetch(`${BASE}/users/me`, {
				headers: {
					Authorization: `Bearer ${access}`
				}
			});

			if (res.ok) {
				return true;
			}
		} catch {
			// Fall through to refresh.
		}
	}

	const refreshed = await refreshAccessToken();

	if (!refreshed) {
		clearSession();
	}

	return refreshed;
}

async function refreshAccessToken(): Promise<boolean> {
	const refreshToken = getRefreshToken();

	if (!refreshToken) {
		clearSession();
		return false;
	}

	if (!refreshPromise) {
		refreshPromise = (async () => {
			try {
				const res = await fetch(`${BASE}/auth/refresh`, {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json'
					},
					body: JSON.stringify({ refreshToken })
				});

				const json = await readResponse<any>(res, 'Session refresh failed');

				if (json?.success && json.data?.accessToken) {
					localStorage.setItem('accessToken', json.data.accessToken);

					if (json.data.refreshToken) {
						localStorage.setItem('refreshToken', json.data.refreshToken);
					}

					if (json.data.username || json.data.userId) {
						localStorage.setItem(
							'user',
							JSON.stringify({
								id: json.data.userId,
								userId: json.data.userId,
								username: json.data.username,
								email: json.data.email,
								profilePictureUrl: json.data.profilePictureUrl
							})
						);
					}

					return true;
				}

				clearSession();
				return false;
			} catch {
				clearSession();
				return false;
			} finally {
				refreshPromise = null;
			}
		})();
	}

	return refreshPromise;
}

async function request<T>(
	endpoint: string,
	options: RequestInit = {},
	authBehavior: 'session' | 'none' = 'session',
	retried = false
): Promise<T> {
	const token = authBehavior === 'session' ? getAccessToken() : null;

	const controller = new AbortController();
	const timeoutId = setTimeout(() => controller.abort(), COLD_START_TIMEOUT);

	try {
		const res = await fetch(`${BASE}${endpoint}`, {
			...options,
			signal: controller.signal,
			headers: {
				'Content-Type': 'application/json',
						Accept: 'application/json',
				...(token && { Authorization: `Bearer ${token}` }),
				...options.headers
			}
		});

		if (
			authBehavior === 'session' &&
			(res.status === 401 || res.status === 403) &&
			!retried
		) {
			clearTimeout(timeoutId);

			const refreshed = await refreshAccessToken();

			if (refreshed) {
				return request<T>(endpoint, options, authBehavior, true);
			}

			if (
				typeof window !== 'undefined' &&
				window.location.pathname !== '/login'
			) {
				window.location.href = '/login';
			}

			throw new Error('Session expired. Please sign in again.');
		}

		clearTimeout(timeoutId);

		return await readResponse<T>(res, 'Request failed');
	} catch (error) {
		clearTimeout(timeoutId);

		if (error instanceof DOMException && error.name === 'AbortError') {
			throw new Error(
				'Server is waking up. Please wait a moment and try again.'
			);
		}

		if (error instanceof TypeError) {
			throw new Error('Unable to connect. Check your network and try again.');
		}

		throw error;
	}
}

async function uploadRequest<T>(
	endpoint: string,
	formData: FormData,
	retried = false
): Promise<T> {
	const token = getAccessToken();

	const controller = new AbortController();
	const timeoutId = setTimeout(() => controller.abort(), COLD_START_TIMEOUT);

	try {
		const res = await fetch(`${BASE}${endpoint}`, {
			method: 'POST',
			body: formData,
			signal: controller.signal,
			headers: {
				...(token && { Authorization: `Bearer ${token}` })
			}
		});

		if ((res.status === 401 || res.status === 403) && !retried) {
			clearTimeout(timeoutId);

			const refreshed = await refreshAccessToken();

			if (refreshed) {
				return uploadRequest<T>(endpoint, formData, true);
			}

			if (
				typeof window !== 'undefined' &&
				window.location.pathname !== '/login'
			) {
				window.location.href = '/login';
			}

			throw new Error('Session expired. Please sign in again.');
		}

		clearTimeout(timeoutId);

		return await readResponse<T>(res, 'Upload failed');
	} catch (error) {
		clearTimeout(timeoutId);

		if (error instanceof DOMException && error.name === 'AbortError') {
			throw new Error(
				'Server is waking up. Please wait a moment and try again.'
			);
		}

		if (error instanceof TypeError) {
			throw new Error('Unable to connect. Check your network and try again.');
		}

		throw error;
	}
}

export const api = {
	login: (identifier: string, password: string) =>
		request('/auth/login', {
			method: 'POST',
			body: JSON.stringify({ identifier, password })
		}),

	register: (username: string, email: string, password: string) =>
		request('/auth/register', {
			method: 'POST',
			body: JSON.stringify({ username, email, password })
		}),

	forgotPassword: (identifier: string) =>
		request('/auth/forgot-password', {
			method: 'POST',
			body: JSON.stringify({ identifier })
		}),

	resetPassword: (token: string, newPassword: string) =>
		request('/auth/reset-password', {
			method: 'POST',
			body: JSON.stringify({ token, newPassword })
		}),

	changePassword: (currentPassword: string, newPassword: string) =>
		request('/users/me/password', {
			method: 'PATCH',
			body: JSON.stringify({
				currentPassword,
				newPassword
			})
		}),

	updateUser: (data: {
		username?: string;
		email?: string;
		profilePictureUrl?: string;
	}) =>
		request('/users/me', {
			method: 'PATCH',
			body: JSON.stringify(data)
		}),

	uploadAvatar: (file: File) => {
		const formData = new FormData();
		formData.append('file', file);

		return uploadRequest<string>('/users/avatar', formData);
	},

	deleteUser: () =>
		request('/users/me', {
			method: 'DELETE'
		}),

	getStats: () =>
		request('/users/me/stats'),

	startThirtySeconds: (
		playerNames: string[],
		roundsPerPlayer: number
	) =>
		request('/thirty-seconds/games/start', {
			method: 'POST',
			body: JSON.stringify({
				mode: 'SOLO',
				playerNames,
				roundsPerPlayer
			})
		}, 'none'),

	scoreRound: (
		gameId: string,
		roundId: string,
		score: number
	) =>
		request(`/thirty-seconds/games/${gameId}/rounds/score`, {
			method: 'POST',
			body: JSON.stringify({
				roundId,
				score
			})
		}),

	completeGame: (gameId: string) =>
		request(`/thirty-seconds/games/${gameId}/complete`, {
			method: 'POST'
		}),

	startQna: (difficulty: string) =>
		request('/qna/attempts/start', {
			method: 'POST',
			body: JSON.stringify({
				difficulty
			})
		}),

	submitQna: (
		attemptId: string,
		answers: Array<{
			questionId: string;
			selectedOptionId: string;
			timeTakenMs: number;
		}>
	) =>
		request(`/qna/attempts/${attemptId}/submit`, {
			method: 'POST',
			body: JSON.stringify({
				answers
			})
		}),

	getLeaderboard: (
		period: string = 'ALL',
		limit: number = 20
	) =>
		request<{
			period: string;
			entries: Array<{
				rank: number;
				userId: string;
				username: string;
				profilePictureUrl: string | null;
				score: number;
			}>;
		}>(
			`/leaderboard/qna?period=${encodeURIComponent(period)}&limit=${limit}`
		),

	logout: (refreshToken: string) =>
		request('/auth/logout', {
			method: 'POST',
			body: JSON.stringify({
				refreshToken
			})
		})
};