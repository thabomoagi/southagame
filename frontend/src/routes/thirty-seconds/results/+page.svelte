<script lang="ts">
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';

    let wordsParam = $derived(
        $page.url.searchParams.get('words') || ''
    );

    let words = $derived(
        wordsParam
            ? wordsParam
                  .split(',')
                  .map((word) => word.trim())
                  .filter(Boolean)
            : []
    );
</script>

<svelte:head>
    <title>30 Seconds — How Southa Are You?</title>
    <meta
        name="description"
        content="Time's up! See the words from your 30 Seconds game."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    <main class="flex flex-1 flex-col items-center justify-center text-center">
        <h1 class="text-5xl font-black tracking-tight text-slate-900 dark:text-white">
            Time's Up!
        </h1>

        <div class="mt-10 w-full max-w-sm space-y-3">
            {#each words as word}
                <div class="flex items-center justify-between gap-4 rounded-2xl bg-springbok/10 px-6 py-4 dark:bg-springbok/20">
                    <span class="text-2xl font-black uppercase tracking-wide text-springbok dark:text-springbok-bright">
                        {word}
                    </span>
                    <a
                        href={`https://www.google.com/search?q=${encodeURIComponent(word + ' south africa')}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        class="rounded-full bg-sa-yellow px-3 py-1.5 text-xs font-black text-slate-900 transition-transform hover:scale-105 active:scale-[0.95]"
                    >
                        Learn More
                    </a>
                </div>
            {/each}
        </div>

        {#if words.length === 0}
            <p class="text-xl font-bold text-slate-500 dark:text-slate-400">
                No words were loaded.
            </p>
        {/if}
    </main>

    <footer class="flex flex-col gap-3 pt-8">
        <button
            type="button"
            onclick={() => goto('/thirty-seconds/play')}
            class="w-full rounded-3xl bg-springbok px-6 py-5 text-2xl font-black tracking-wider text-white shadow-xl shadow-springbok/30 transition-transform active:scale-[0.98] dark:bg-springbok-bright"
        >
            PLAY AGAIN
        </button>

        <button
            type="button"
            onclick={() => goto('/home')}
            class="w-full rounded-3xl border-2 border-slate-200 px-6 py-5 text-xl font-black tracking-wider text-slate-600 transition-transform active:scale-[0.98] dark:border-slate-700 dark:text-slate-300"
        >
            HOME
        </button>
    </footer>
</div>