<script lang="ts">
    import { goto } from '$app/navigation';
    import { api, hasUsableSession } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import { onMount } from 'svelte';

    let loading = $state(true);
    let checking = $state(true);
    let canPlay = $state(false);
    let submitting = $state(false);
    let error = $state('');
    let attemptId = $state('');
    let questions = $state.raw<any[]>([]);
    let currentIndex = $state(0);
    let timeLeft = $state(30);
    let locked = $state(false);
    let submitted = $state(false);
    let answers = $state<any[]>([]);
    let questionShownAt = $state(Date.now());
    let timerId: ReturnType<typeof setInterval> | undefined;

    async function startGame() {
        const difficulties = ['EASY', 'MEDIUM', 'HARD'];
        const shuffled = [...difficulties].sort(() => Math.random() - 0.5);

        for (const difficulty of shuffled) {
            try {
                const res: any = await api.startQna(difficulty);

                attemptId = res.attemptId ?? res.id ?? '';

                questions = (res.questions ?? []).map((q: any) => ({
                    id: q.id ?? q.questionId,
                    text: q.prompt ?? q.questionText ?? q.text ?? q.question ?? '',
                    options: (q.options ?? []).map((o: any) => ({
                        id: o.id ?? o.optionId,
                        text:
                            typeof o === 'string'
                                ? o
                                : (o.optionText ?? o.text ?? o.option ?? o.value ?? '')
                    }))
                }));

                if (questions.length > 0) {
                    questionShownAt = Date.now();
                    loading = false;
                    return;
                }
            } catch (err) {
                const message = err instanceof Error ? err.message : '';

                if (message.toLowerCase().includes('daily limit') || message.includes('20')) {
                    error = 'You have reached your daily limit of 20 MCQ games. Come back tomorrow!';
                    loading = false;
                    return;
                }

                if (
                    message.toLowerCase().includes('not enough') ||
                    message.toLowerCase().includes('insufficient')
                ) {
                    continue;
                }

                error = message || 'Failed to start game';
                loading = false;
                return;
            }
        }

        error = 'No questions available. Please try again later.';
        loading = false;
    }

    onMount(async () => {
        auth.init();
        canPlay = Boolean(auth.user && hasUsableSession());
        checking = false;

        if (canPlay) {
            await startGame();
        } else {
            loading = false;
        }
    });

    $effect(() => {
        if (!loading && !error && questions.length > 0 && !submitted) {
            const endTime = Date.now() + 30_000;

            timerId = setInterval(() => {
                const remaining = Math.max(
                    0,
                    Math.ceil((endTime - Date.now()) / 1000)
                );

                timeLeft = remaining;

                if (remaining === 0) {
                    if (timerId) clearInterval(timerId);
                    finishGame();
                }
            }, 100);

            return () => {
                if (timerId) clearInterval(timerId);
            };
        }
    });

    function selectOption(optionId: string) {
        if (locked || submitted) return;

        locked = true;

        answers = [
            ...answers,
            {
                questionId: questions[currentIndex].id,
                selectedOptionId: optionId,
                timeTakenMs: Date.now() - questionShownAt
            }
        ];

        if (currentIndex + 1 >= questions.length) {
            finishGame();
        } else {
            currentIndex++;
            questionShownAt = Date.now();
            locked = false;
        }
    }

    async function finishGame() {
        if (submitted) return;

        submitted = true;
        submitting = true;

        if (timerId) clearInterval(timerId);

        try {
            const res: any = await api.submitQna(attemptId, answers);
            const params = new URLSearchParams({
                score: String(res.score ?? 0),
                total: String(questions.length)
            });
            goto(`/multiple-choice/game-over?${params.toString()}`);
        } catch (err) {
            error = err instanceof Error ? err.message : 'Failed to submit answers';
            submitted = false;
            submitting = false;
        }
    }

    function retryStart() {
        loading = true;
        error = '';
        answers = [];
        currentIndex = 0;
        timeLeft = 30;
        locked = false;
        submitted = false;
        questionShownAt = Date.now();

        startGame();
    }
</script>

<svelte:head>
    <title>Playing - Multiple Choice - How Southa Are You?</title>
</svelte:head>

{#if checking || loading || submitting}
    <div class="flex min-h-dvh flex-col items-center justify-center gap-4 bg-white dark:bg-dark-bg">
        <div class="h-12 w-12 animate-spin rounded-full border-4 border-sa-yellow/30 border-t-sa-yellow"></div>

        <p class="text-lg font-bold text-slate-600 dark:text-slate-300">
            {submitting ? 'Calculating score...' : 'Loading questions...'}
        </p>
    </div>

{:else if !canPlay}
    <div class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-white px-6 dark:bg-dark-bg">
        <p class="max-w-sm text-center text-xl font-bold text-slate-700 dark:text-slate-200">
            Sign in to play Multiple Choice.
        </p>

        <button
            type="button"
            onclick={() => goto('/login')}
            class="w-full max-w-sm rounded-3xl bg-sa-yellow px-6 py-5 text-xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
        >
            SIGN IN TO PLAY
        </button>
    </div>

{:else if error}
    <div class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-white px-6 dark:bg-dark-bg">
        <p class="max-w-sm text-center text-xl font-bold text-red-500">
            {error}
        </p>

        <div class="flex w-full max-w-sm flex-col gap-3">
            <button
                onclick={retryStart}
                class="w-full rounded-3xl bg-sa-yellow px-6 py-5 text-xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
            >
                RETRY
            </button>

            <button
                onclick={() => goto('/home')}
                class="w-full rounded-3xl border-2 border-slate-200 bg-white px-6 py-5 text-xl font-black tracking-wider text-slate-700 transition-transform active:scale-[0.98] dark:border-slate-700 dark:bg-dark-surface dark:text-white"
            >
                HOME
            </button>
        </div>
    </div>

{:else if questions.length > 0}
    <div class="flex min-h-dvh flex-col bg-white dark:bg-dark-bg">
        <header class="flex flex-col items-center gap-1 px-6 pt-8">
            <div
                class="text-7xl font-black tabular-nums {timeLeft > 10
                    ? 'text-springbok dark:text-springbok-bright'
                    : timeLeft > 5
                      ? 'text-sa-yellow'
                      : 'text-red-500'}"
            >
                {timeLeft}
            </div>

            <p class="text-sm font-black uppercase tracking-widest text-slate-500 dark:text-slate-400">
                Question {currentIndex + 1} of {questions.length}
            </p>
        </header>

        <main class="flex flex-1 flex-col px-6 py-6">
            <h2 class="text-center text-3xl font-black leading-tight text-slate-900 dark:text-white">
                {questions[currentIndex].text}
            </h2>

            <div class="my-6 h-1 w-full rounded-full bg-sa-yellow"></div>

            <div class="flex flex-col gap-4">
                {#each questions[currentIndex].options as option}
                    <button
                        onclick={() => selectOption(option.id)}
                        disabled={locked}
                        class="w-full rounded-2xl border-2 border-slate-200 bg-white px-6 py-5 text-left text-xl font-bold text-slate-800 transition-all active:bg-sa-yellow disabled:opacity-60 dark:border-slate-700 dark:bg-dark-surface dark:text-white"
                    >
                        {option.text}
                    </button>
                {/each}
            </div>
        </main>
    </div>
{/if}