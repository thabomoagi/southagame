import { goto } from '$app/navigation';
import { auth } from '$lib/stores/auth.svelte';
import { hasUsableSession } from '$lib/api';

/**
 * Client-side route guard.
 * Initialises the session and redirects unauthenticated users to /login.
 */
export function requireAuth(): boolean {
    if (typeof window === 'undefined') return false;

    auth.init();

    if (auth.user && hasUsableSession()) {
        return true;
    }

    goto('/login');
    return false;
}