<script lang="ts">
    import { api } from '$lib/api';
    import { requireAuth } from '$lib/auth-guard';
    import Avatar from '$lib/components/Avatar.svelte';
    import { onMount } from 'svelte';

    type Period = 'DAILY' | 'MONTHLY';

    type LeaderboardEntry = {
        rank: number;
        userId: string;
        username: string;
        profilePictureUrl: string | null;
        score: number;
    };

    let loading = $state(true);
    let error = $state('');
    let period = $state<Period>('DAILY');
    let entries = $state<LeaderboardEntry[]>([]);

    const periods: Period[] = ['DAILY', 'MONTHLY'];

    onMount(() => {
        if (!requireAuth()) return;
        loadLeaderboard();
    });

    async function loadLeaderboard() {
        loading = true;
        error = '';

        try {
            const data = await api.getLeaderboard(period, 20);
            entries = data?.entries ?? [];
        } catch (err) {
            error = err instanceof Error ? err.message : 'Failed to load leaderboard';
        } finally {
            loading = false;
        }
    }

    function selectPeriod(newPeriod: Period) {
        if (newPeriod === period && !loading) return;
        period = newPeriod;
        loadLeaderboard();
    }

    function rankLabel(rank: number): string {
        if (rank === 1) return '1st place';
        if (rank === 2) return '2nd place';
        if (rank === 3) return '3rd place';
        return `${rank}th place`;
    }

    function getRankClass(rank: number) {
        if (rank === 1) return 'bg-sa-yellow/20 text-yellow-800 dark:text-sa-yellow';
        if (rank === 2) return 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300';
        if (rank === 3) return 'bg-amber-800/15 text-amber-800 dark:bg-amber-800/30 dark:text-amber-400';
        return 'text-slate-600 dark:text-slate-400';
    }
</script>

<svelte:head>
    <title>Leaderboard - How Southa Are You?</title>
    <meta name="description" content="See how you rank on the How Southa leaderboard." />
</svelte:head>

<div class="min-h-dvh bg-white text-slate-900 dark:bg-dark-bg dark:text-white">
    <main class="mx-auto flex w-full max-w-lg flex-col px-6 py-8 pb-28">
        <header class="mb-8 text-center">
            <h1 class="text-4xl font-black tracking-tight">Leaderboard</h1>
            <p class="mt-2 text-sm font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">
                Top Southa scores
            </p>
        </header>

        <div
            class="mx-auto mb-8 flex w-full rounded-2xl border border-slate-200 bg-slate-50 p-1 dark:border-slate-700 dark:bg-dark-surface"
            role="group"
            aria-label="Leaderboard period"
        >
            {#each periods as p}
                <button
                    type="button"
                    onclick={() => selectPeriod(p)}
                    aria-pressed={period === p}
                    class="flex-1 rounded-xl px-2 py-3 text-xs font-black uppercase tracking-wide transition-all active:scale-[0.98] {period === p
                        ? 'bg-springbok text-white shadow-lg shadow-springbok/20 dark:bg-springbok-bright dark:text-dark-bg'
                        : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}"
                >
                    {p}
                </button>
            {/each}
        </div>

        {#if loading}
            <div class="flex flex-col items-center justify-center gap-4 py-16">
                <div class="h-10 w-10 animate-spin rounded-full border-4 border-springbok/25 border-t-springbok dark:border-t-springbok-bright"></div>
                <p class="text-sm font-bold text-slate-500 dark:text-slate-400">
                    Loading leaderboard...
                </p>
            </div>
        {:else if error}
            <div class="flex flex-col items-center justify-center gap-5 py-16 text-center">
                <p class="text-lg font-bold text-red-500">{error}</p>
                <button
                    type="button"
                    onclick={loadLeaderboard}
                    class="rounded-2xl bg-sa-yellow px-6 py-4 text-sm font-black uppercase tracking-wide text-slate-900 shadow-lg shadow-sa-yellow/20 active:scale-[0.98]"
                >
                    Retry
                </button>
            </div>
        {:else if entries.length === 0}
            <div class="flex flex-col items-center justify-center py-16 text-center">
                <h2 class="text-xl font-black">No scores yet</h2>
                <p class="mt-2 text-sm font-semibold text-slate-500 dark:text-slate-400">
                    Be the first to claim the top spot.
                </p>

                <a
                    href="/home"
                    class="mt-6 rounded-2xl bg-springbok px-6 py-4 text-sm font-black uppercase tracking-wide text-white shadow-lg shadow-springbok/20 active:scale-[0.98] dark:bg-springbok-bright dark:text-dark-bg"
                >
                    Play Now
                </a>
            </div>
        {:else}
            <ol class="space-y-3">
                {#each entries as entry (entry.userId)}
                    <li
                        class="flex items-center gap-3 rounded-2xl border border-slate-200/80 bg-slate-50 px-4 py-3.5 shadow-sm dark:border-slate-700 dark:bg-dark-surface"
                    >
                        <span
                            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-xs font-black {getRankClass(
                                entry.rank
                            )}"
                            aria-label={rankLabel(entry.rank)}
                        >
                            #{entry.rank}
                        </span>

                        <Avatar
                            src={entry.profilePictureUrl}
                            name={entry.username}
                            size="sm"
                        />

                        <p class="min-w-0 flex-1 truncate text-base font-black">
                            {entry.username}
                        </p>

                        <span class="shrink-0 text-right text-base font-black">
                            {entry.score}
                            <span class="text-xs font-bold text-slate-500 dark:text-slate-400">
                                pts
                            </span>
                        </span>
                    </li>
                {/each}
            </ol>
        {/if}
    </main>
</div>