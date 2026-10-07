<script lang="ts">
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { api } from '$lib/api';
    import Avatar from '$lib/components/Avatar.svelte';
    import { auth } from '$lib/stores/auth.svelte';

    const path = $derived($page.url.pathname);

    const hideOnPaths = [
        '/login',
        '/register',
        '/forgot-password',
        '/reset-password',
        '/thirty-seconds',
        '/multiple-choice'
    ];

    const isHidden = $derived(
        !auth.user ||
        hideOnPaths.includes(path) ||
        path.includes('/play') ||
        path.includes('/game') ||
        path.includes('/result')
    );

    function isActive(href: string) {
        if (href === '/home') return path === '/home' || path === '/';
        return path === href || path.startsWith(href + '/');
    }

    function logout() {
        if (!confirm('Are you sure you want to log out?')) return;

        const refreshToken =
            typeof localStorage !== 'undefined'
                ? localStorage.getItem('refreshToken')
                : null;

        if (refreshToken) {
            api.logout(refreshToken).catch(() => {});
        }

        auth.logout();
        goto('/login');
    }
</script>

{#if !isHidden}
    <nav
        class="fixed right-0 bottom-0 left-0 z-40 border-t border-slate-200/80 bg-white/95 px-3 pt-2 pb-[calc(0.5rem+env(safe-area-inset-bottom))] backdrop-blur dark:border-dark-surface dark:bg-dark-bg/95"
        aria-label="Bottom navigation"
    >
        <div class="relative mx-auto flex max-w-md items-center justify-around gap-1">
            <!-- Play -->
            <a
                href="/home"
                aria-current={isActive('/home') ? 'page' : undefined}
                class="flex flex-col items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-black transition-colors {isActive('/home')
                    ? 'text-springbok dark:text-springbok-bright'
                    : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}"
            >
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 0 1 0 1.972l-11.54 6.347a1.125 1.125 0 0 1-1.667-.986V5.653Z" />
                </svg>
                Play
            </a>

            <!-- Leaderboard -->
            <a
                href="/leaderboard"
                aria-current={isActive('/leaderboard') ? 'page' : undefined}
                class="flex flex-col items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-black transition-colors {isActive('/leaderboard')
                    ? 'text-springbok dark:text-springbok-bright'
                    : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}"
            >
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 18.75h-9m9 0a3 3 0 0 1 3 3h-15a3 3 0 0 1 3-3m9 0v-3.375c0-.621-.503-1.125-1.125-1.125h-.871M7.5 18.75v-3.375c0-.621.504-1.125 1.125-1.125h.872m5.007 0H9.497m5.007 0a7.454 7.454 0 0 1-.982-3.17M9.497 14.25a7.454 7.454 0 0 0 .982-3.17m-5.007 3.17h.872m5.007 0V6.75a2.25 2.25 0 0 0-2.25-2.25h-1.5a2.25 2.25 0 0 0-2.25 2.25v7.5m5.007 0h3.876" />
                </svg>
                Ranking
            </a>

            <!-- Profile (center, elevated) -->
            <a
                href="/profile"
                aria-current={isActive('/profile') ? 'page' : undefined}
                class="relative -top-4 flex flex-col items-center gap-1 rounded-full border p-2.5 shadow-lg transition-transform hover:scale-105 {isActive('/profile')
                    ? 'border-springbok bg-springbok text-white dark:border-springbok-bright dark:bg-springbok-bright dark:text-dark-bg'
                    : 'border-slate-200 bg-white text-slate-700 dark:border-dark-surface dark:bg-dark-surface dark:text-white'}"
            >
                <div class="flex h-6 w-6 items-center justify-center overflow-hidden rounded-full">
                    <Avatar
                        src={auth.user?.profilePictureUrl}
                        name={auth.user?.username}
                        size="xs"
                    />
                </div>
                <span class="text-[10px] font-black uppercase tracking-wider leading-none">
                    Profile
                </span>
            </a>

            <!-- Settings -->
            <a
                href="/settings"
                aria-current={isActive('/settings') ? 'page' : undefined}
                class="flex flex-col items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-black transition-colors {isActive('/settings')
                    ? 'text-springbok dark:text-springbok-bright'
                    : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}"
            >
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456c.54-.203 1.15.008 1.452.506l1.297 2.247c.279.484.248 1.07-.082 1.516l-.88 1.168c-.251.334-.325.776-.195 1.18.037.114.078.228.123.339.192.473.66.798 1.185.798h1.492c.624 0 1.13.506 1.13 1.13v2.28c0 .624-.506 1.13-1.13 1.13h-1.492c-.525 0-.993.325-1.185.798-.045.111-.086.228-.123.34-.13.404-.056.846.195 1.18l.88 1.168c.33.446.912.709 1.452.506l-1.217-.456c-.355-.133-.75-.072-1.075.124a6.47 6.47 0 0 1-.22.128c-.331.183-.581.495-.645.87l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456c-.54.203-1.15-.008-1.452-.506l-1.297-2.247c-.28-.484-.248-1.07.082-1.516l.88-1.168c.251-.334.325-.776.195-1.18a4.195 4.195 0 0 0-.123-.339c-.192-.473-.66-.798-1.185-.798H3.87c-.624 0-1.13-.506-1.13-1.13v-2.28c0-.624.506-1.13 1.13-1.13h1.492c.525 0 .993-.325 1.185-.798.045-.111.086-.225.123-.34.13-.404.056-.846-.195-1.18l-.88-1.168c-.33-.446-.361-1.032-.082-1.516l1.297-2.247c.302-.498.912-.709 1.452-.506l1.217.456c.355.133.75.072 1.075-.124.073-.044.146-.087.22-.127.332-.184.582-.496.645-.87l.213-1.28Z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                </svg>
                Settings
            </a>

            <!-- Logout -->
            <button
                type="button"
                onclick={logout}
                aria-label="Log out"
                class="flex flex-col items-center gap-1 rounded-xl px-3 py-1.5 text-xs font-black text-slate-500 transition-colors hover:text-red-600 dark:text-slate-400 dark:hover:text-red-400"
            >
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-7.5A2.25 2.25 0 0 0 3.75 5.25v13.5A2.25 2.25 0 0 0 6 21h7.5a2.25 2.25 0 0 0 2.25-2.25V15m3.75-3H9m0 0 3-3m-3 3 3 3" />
                </svg>
                Logout
            </button>
        </div>
    </nav>
{/if}