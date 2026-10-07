import { PUBLIC_API_BASE_URL } from '$env/static/public';

/**
 * The API base URL from the environment, e.g.
 * `https://how-sa-are-you-api.onrender.com/api/v1`.
 *
 * Backend-served profile pictures use the same origin under the `/uploads`
 * prefix, so derive the API host origin from the configured base URL.
 */
function apiOrigin(): string {
	const base = PUBLIC_API_BASE_URL;

	const pathIdx = base.indexOf('/api/');
	if (pathIdx > 0) {
		return base.slice(0, pathIdx);
	}

	try {
		return new URL(base).origin;
	} catch {
		return base;
	}
}

/**
 * Resolves a possibly-relative profile-picture URL into an absolute URL.
 *
 * Absolute http(s) URLs pass through unchanged.
 * Empty input returns null.
 */
export function resolveImageUrl(url?: string | null): string | null {
	if (!url) return null;

	if (/^https?:\/\//i.test(url)) {
		return url;
	}

	if (url.startsWith('//')) {
		return `https:${url}`;
	}

	if (url.startsWith('/')) {
		return `${apiOrigin()}${url}`;
	}

	return `${apiOrigin()}/${url}`;
}