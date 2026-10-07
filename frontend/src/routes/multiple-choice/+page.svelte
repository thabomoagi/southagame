<script lang="ts">
    import { goto } from '$app/navigation';
    import { hasUsableSession } from '$lib/api';
    import { auth } from '$lib/stores/auth.svelte';
    import { onMount } from 'svelte';

    let checking = $state(true);
    let canPlay = $state(false);

    onMount(() => {
        auth.init();
        canPlay = Boolean(auth.user && hasUsableSession());
        checking = false;
    });
</script>

<svelte:head>
    <title>Multiple Choice - How Southa Are You?</title>
    <meta
        name="description"
        content="Test your South African knowledge with Multiple Choice — Solo play."
    />
</svelte:head>

{#if !checking}
    <div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
        <div class="flex flex-1 flex-col items-center justify-center text-center">
            <h1 class="text-5xl font-black tracking-tight text-slate-900 dark:text-white">
                Multiple Choice
            </h1>

            <p class="mt-2 text-sm font-black uppercase tracking-widest text-springbok dark:text-springbok-bright">
                Solo
            </p>

            <div class="mt-10 max-w-sm space-y-5 text-lg font-semibold text-slate-700 dark:text-slate-200">
                <p>You&apos;ll have 30 seconds to answer 5 questions and test how well you know South Africa.</p>
                <p>Answer each question as quickly and accurately as possible.</p>
                <p>You will only get one answer for each question.</p>
            </div>
        </div>

        {#if canPlay}
            <button
                onclick={() => goto('/multiple-choice/play')}
                class="w-full rounded-3xl bg-sa-yellow px-6 py-5 text-2xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
            >
                START GAME
            </button>
        {:else}
            <button
                onclick={() => goto('/login')}
                class="w-full rounded-3xl bg-sa-yellow px-6 py-5 text-2xl font-black tracking-wider text-slate-900 shadow-xl shadow-sa-yellow/30 transition-transform active:scale-[0.98]"
            >
                SIGN IN TO PLAY
            </button>
        {/if}
    </div>
{/if}