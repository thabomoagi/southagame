<script lang="ts">
    import { goto } from '$app/navigation';
    import { api } from '$lib/api';
    import { onMount } from 'svelte';

    let loading = $state(true);
    let error = $state('');
    let words = $state.raw<string[]>([]);
    let timeLeft = $state(30);
    let countdown = $state(3);
    let gameReady = $state(false);
    let gameStarted = $state(false);
    let gameFinished = $state(false);

    let timerId: ReturnType<typeof setInterval> | undefined;

    onMount(async () => {
        try {
            const playerName = 'Player 1';
            const res: any = await api.startThirtySeconds([playerName], 1);

            const data = res?.data ?? res;
            const rounds = data?.rounds;

            if (!Array.isArray(rounds) || rounds.length === 0) {
                throw new Error('The server did not return a game round.');
            }

            const round = rounds[0];
            const prompt = round?.prompt;

            if (Array.isArray(prompt)) {
                words = prompt
                    .map((word: unknown) => String(word).trim())
                    .filter((word: string) => word.length > 0);
            } else if (typeof prompt === 'string') {
                words = prompt
                    .split(',')
                    .map((word: string) => word.trim())
                    .filter((word: string) => word.length > 0);
            }

            if (words.length === 0) {
                throw new Error('The server did not return any words for this round.');
            }

            loading = false;
            gameReady = true;
        } catch (err) {
            error = err instanceof Error ? err.message : 'Failed to start game';
            loading = false;
        }
    });

    // Robust countdown timer that never blocks
    $effect(() => {
        if (!gameReady || gameStarted || gameFinished) return;

        countdown = 3; // start at 3

        const intervalId = setInterval(() => {
            if (countdown <= 1) {
                countdown = 0;
                gameStarted = true;
                clearInterval(intervalId);
            } else {
                countdown -= 1;
            }
        }, 1000);

        return () => clearInterval(intervalId);
    });

    $effect(() => {
        if (!gameStarted || gameFinished) return;

        const endTime = Date.now() + 30_000;

        timerId = setInterval(() => {
            const remaining = Math.max(
                0,
                Math.ceil((endTime - Date.now()) / 1000)
            );

            timeLeft = remaining;

            if (remaining === 0) {
                finishGame();
            }
        }, 100);

        return () => {
            if (timerId) clearInterval(timerId);
        };
    });

    function finishGame() {
        if (gameFinished) return;

        gameFinished = true;

        if (timerId) clearInterval(timerId);

        const params = new URLSearchParams({
            words: words.join(','),
            score: '0',
            total: String(words.length)
        });

        goto(`/thirty-seconds/results?${params.toString()}`);
    }
</script>

<svelte:head>
    <title>30 Seconds — How Southa Are You?</title>
    <meta
        name="description"
        content="30 seconds. Five words on a card. Describe them all!"
    />
    <meta property="og:title" content="30 Seconds — How Southa Are You?" />
</svelte:head>

{#if loading}
    <div
        class="flex min-h-dvh flex-col items-center justify-center gap-4 bg-white dark:bg-dark-bg"
    >
        <div
            class="h-12 w-12 animate-spin rounded-full border-4 border-sa-yellow/30 border-t-sa-yellow"
        ></div>

        <p class="text-lg font-bold text-slate-600 dark:text-slate-300">
            Waking up the server...
        </p>
    </div>

{:else if error}
    <div
        class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-white px-6 dark:bg-dark-bg"
    >
        <p class="max-w-sm text-center text-xl font-bold text-red-500">
            {error}
        </p>

        <div class="flex w-full max-w-sm flex-col gap-3">
            <button
                type="button"
                onclick={() => location.reload()}
                class="w-full rounded-3xl bg-sa-yellow px-6 py-5 text-xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
            >
                RETRY
            </button>

            <button
                type="button"
                onclick={() => goto('/home')}
                class="w-full rounded-3xl border-2 border-slate-200 bg-white px-6 py-5 text-xl font-black tracking-wider text-slate-700 transition-transform active:scale-[0.98] dark:border-slate-700 dark:bg-dark-surface dark:text-white"
            >
                HOME
            </button>
        </div>
    </div>

{:else}
    <div class="flex min-h-dvh flex-col bg-white dark:bg-dark-bg">
        {#if !gameStarted}
            <main class="flex min-h-dvh flex-col items-center justify-center px-6 text-center">
                <p class="text-9xl font-black tabular-nums text-sa-yellow animate-pulse">
                    {countdown || 'GO!'}
                </p>
            </main>
        {:else}
            <header class="flex flex-col items-center gap-1 px-6 pt-8">
                <div
                    class="text-7xl font-black tabular-nums {timeLeft > 10
                        ? 'text-springbok dark:text-springbok-bright'
                        : timeLeft > 5
                          ? 'text-sa-yellow'
                          : 'text-red-500'}"
                    aria-label={`${timeLeft} seconds remaining`}
                >
                    {timeLeft}
                </div>
            </header>

            <main class="flex flex-1 flex-col px-6 py-6 max-w-lg mx-auto w-full justify-center">
                <div class="flex flex-col gap-3">
                    {#each words as word, index}
                        <div
                            class="w-full rounded-2xl border-2 border-springbok/30 bg-springbok/10 px-6 py-4 text-center text-2xl font-black tracking-wide text-springbok-bright dark:border-springbok-bright/40 dark:bg-springbok/20 dark:text-white"
                        >
                            <span class="mr-2 text-sm text-springbok/70 dark:text-white/70">{index + 1}.</span>
                            {word}
                        </div>
                    {/each}
                </div>

                <div class="mt-8">
                    <button
                        type="button"
                        onclick={finishGame}
                        class="w-full rounded-3xl bg-sa-yellow px-6 py-5 text-xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
                    >
                        DONE
                    </button>
                </div>
            </main>
        {/if}
    </div>
{/if}