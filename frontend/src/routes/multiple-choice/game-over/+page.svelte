<script lang="ts">
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';

    let score = $derived(Number($page.url.searchParams.get('score') || '0'));
    let total = $derived(Number($page.url.searchParams.get('total') || '5'));
    let percentage = $derived(total > 0 ? (score / total) * 100 : 0);

    let feedback = $derived(
        percentage >= 100
            ? 'Absolute legend! You know Southa inside out!'
            : percentage >= 80
                ? 'Sharp! Almost got all of them!'
                : percentage >= 60
                    ? 'Not bad at all, boet!'
                    : percentage >= 40
                        ? 'Getting there! Keep practicing.'
                        : 'Better luck next time, boet!'
    );

    function playAgain() {
        goto('/multiple-choice/play');
    }
</script>

<svelte:head>
    <title>Game Over - How Southa Are You?</title>
    <meta
        name="description"
        content="See your Multiple Choice score and play again."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    <div class="flex flex-1 flex-col items-center justify-center text-center">
        <h1 class="text-5xl font-black tracking-tight text-slate-900 dark:text-white">
            Game Over!
        </h1>

        <p class="mt-6 text-4xl font-black text-springbok dark:text-sa-yellow">
                        {score} / {total} Points
        </p>

        <p class="mt-5 max-w-sm text-lg font-semibold text-slate-600 dark:text-slate-300">
                        {feedback}
        </p>
    </div>

    <div class="flex flex-col gap-4">
        <button
            onclick={playAgain}
            class="w-full rounded-3xl bg-sa-yellow px-6 py-5 text-2xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
        >
            PLAY AGAIN
        </button>

        <button
            onclick={() => goto('/home')}
            class="w-full rounded-3xl border-2 border-slate-200 bg-white px-6 py-5 text-2xl font-black tracking-wider text-slate-700 transition-transform active:scale-[0.98] dark:border-slate-700 dark:bg-dark-surface dark:text-white"
        >
            FINISH
        </button>
    </div>
</div>