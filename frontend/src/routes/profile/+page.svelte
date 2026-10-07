<script lang="ts">
    import { goto } from '$app/navigation';
    import { api } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import { requireAuth } from '$lib/auth-guard';
    import Avatar from '$lib/components/Avatar.svelte';
    import { onMount } from 'svelte';

    let loading = $state(true);
    let error = $state('');
    let stats = $state<any>(null);
    let user = $state<any>(null);
    let location = $state('');

    onMount(async () => {
        if (!requireAuth()) return;

        user = auth.user;

        // Location is purely cosmetic. If the service fails,
        // the rest of the profile continues working normally.
        fetch('https://ipapi.co/json/')
            .then((response) => (response.ok ? response.json() : null))
            .then((data) => {
                if (data) {
                    location = `${data.city || ''}, ${data.country_name || ''}`.replace(/^, /, '');
                }
            })
            .catch(() => {
                location = '';
            });

        try {
            stats = await api.getStats();
            loading = false;
        } catch (err) {
            error = err instanceof Error ? err.message : 'Failed to load stats';
            loading = false;
        }
    });

    const standing = $derived([
        {
            label: 'Today',
            score: stats?.dailyScore ?? 0,
            rank: stats?.dailyRank
        },
        {
            label: 'This Month',
            score: stats?.monthlyScore ?? 0,
            rank: stats?.monthlyRank
        }
    ]);
</script>

<svelte:head>
    <title>Profile - How Southa Are You?</title>
    <meta
        name="description"
        content="View your How Southa Are You stats, scores and rankings."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    {#if loading}
        <div class="flex flex-1 flex-col items-center justify-center gap-4">
            <div class="h-12 w-12 animate-spin rounded-full border-4 border-springbok/30 border-t-springbok"></div>
            <p class="text-lg font-bold text-slate-600 dark:text-slate-300">
                Loading stats...
            </p>
        </div>

    {:else if error}
        <div class="flex flex-1 flex-col items-center justify-center gap-6 text-center">
            <p class="text-xl font-bold text-red-500 dark:text-red-400">
                {error}
            </p>

            <button
                type="button"
                onclick={() => goto('/home')}
                class="rounded-2xl bg-springbok px-8 py-4 text-lg font-black text-white transition-transform active:scale-[0.98]"
            >
                GO HOME
            </button>
        </div>

    {:else}
        <div class="mx-auto flex w-full max-w-lg flex-1 flex-col">

            <!-- Profile Header -->
            <div class="flex flex-col items-center text-center">
                <Avatar
                    src={user?.profilePictureUrl}
                    name={user?.username}
                    size="xl"
                />

                <h1 class="mt-4 text-3xl font-black tracking-tight text-slate-900 dark:text-white">
                    {user?.username}
                </h1>

                {#if location}
                    <p class="mt-1 text-sm font-semibold text-slate-500 dark:text-slate-400">
                        {location}
                    </p>
                {/if}

                <a
                    href="/settings"
                    class="mt-4 text-sm font-bold text-springbok underline-offset-4 hover:underline dark:text-sa-yellow"
                >
                    Edit Profile
                </a>
            </div>

            <!-- Game Statistics -->
            <div class="mt-10 flex flex-1 flex-col gap-5">

                <!-- Multiple Choice -->
                <div class="rounded-3xl bg-sa-yellow p-6 shadow-lg shadow-sa-yellow/20">
                    <p class="text-xl font-black text-slate-900">
                        Multiple Choice
                    </p>

                    <div class="mt-4 grid grid-cols-2 gap-4">
                        <div>
                            <p class="text-xs font-black uppercase tracking-widest text-slate-800/60">
                                Games Played
                            </p>

                            <p class="mt-1 text-4xl font-black text-slate-900 tabular-nums">
                                {stats?.totalMcqGames ?? 0}
                            </p>
                        </div>

                        <div>
                            <p class="text-xs font-black uppercase tracking-widest text-slate-800/60">
                                Best Score
                            </p>

                            <p class="mt-1 text-4xl font-black text-slate-900 tabular-nums">
                                {stats?.mcqBestScore ?? 0}
                            </p>
                        </div>
                    </div>
                </div>

                <!-- 30 Seconds -->
                <div class="rounded-3xl bg-springbok p-6 shadow-lg shadow-springbok/20 dark:bg-springbok-bright">
                    <p class="text-xl font-black text-white dark:text-dark-bg">
                        30 Seconds
                    </p>

                    <div class="mt-4">
                        <p class="text-xs font-black uppercase tracking-widest text-white/60 dark:text-dark-bg/60">
                            Games Played
                        </p>

                        <p class="mt-1 text-4xl font-black text-white dark:text-dark-bg tabular-nums">
                            {stats?.totalThirtySecondsGames ?? 0}
                        </p>
                    </div>
                </div>

                <!-- Total Activity -->
                <div class="rounded-3xl border-2 border-slate-200 bg-slate-50 p-6 dark:border-slate-700 dark:bg-dark-surface">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-xs font-black uppercase tracking-widest text-slate-500 dark:text-slate-400">
                                Total Activity
                            </p>

                            <p class="mt-1 text-lg font-black text-slate-900 dark:text-white">
                                Games Played
                            </p>
                        </div>

                        <p class="text-4xl font-black text-springbok dark:text-springbok-bright tabular-nums">
                            {stats?.totalGamesPlayed ?? 0}
                        </p>
                    </div>
                </div>

                <!-- Rankings -->
                <div class="rounded-3xl border-2 border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-dark-surface">

                    <div class="mb-5">
                        <h2 class="text-xl font-black text-slate-900 dark:text-white">
                            Your Rankings
                        </h2>

                        <p class="mt-1 text-xs font-semibold uppercase tracking-widest text-slate-500 dark:text-slate-400">
                            Multiple Choice
                        </p>
                    </div>

                    <div class="space-y-4">
                        {#each standing as row}
                            <div class="flex items-center justify-between">
                                <span class="text-sm font-bold text-slate-600 dark:text-slate-400">
                                    {row.label}
                                </span>

                                <div class="flex items-center gap-4">
                                    <span class="text-sm font-black text-slate-900 dark:text-white tabular-nums">
                                        {row.score} pts
                                    </span>

                                    <span class="min-w-12 text-right text-sm font-black text-springbok dark:text-springbok-bright">
                                        #{row.rank ?? '-'}
                                    </span>
                                </div>
                            </div>
                        {/each}
                    </div>
                </div>
            </div>

            <!-- Bottom spacing for BottomNav -->
            <div class="h-24"></div>
        </div>
    {/if}
</div>