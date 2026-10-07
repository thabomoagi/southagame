<script lang="ts">
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import Logo from '$lib/components/Logo.svelte';

    let status = $derived($page.status);

    const isNotFound = $derived(status === 404);
</script>

<svelte:head>
    <title>
        {isNotFound ? 'Page Not Found' : 'Something Went Wrong'} — How Southa Are You?
    </title>
    <meta name="robots" content="noindex" />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    <div class="flex flex-1 flex-col items-center justify-center text-center">
        <Logo />

        <h1 class="mt-8 text-6xl font-black tracking-tight text-slate-900 dark:text-white">
            {isNotFound ? '404' : 'Error'}
        </h1>

        <p class="mt-4 text-xl font-black text-slate-900 dark:text-white">
            {isNotFound ? 'Page not found' : 'Something went wrong'}
        </p>

        <p class="mt-2 max-w-sm text-base font-semibold text-slate-600 dark:text-slate-300">
            {isNotFound
                ? 'The page you are looking for does not exist.'
                : 'Something went wrong while loading this page. Please try again later.'}
        </p>
    </div>

    <button
        onclick={() => goto('/')}
        class="w-full rounded-3xl bg-springbok px-6 py-5 text-2xl font-black tracking-wider text-white shadow-xl shadow-springbok/30 transition-transform active:scale-[0.98] dark:bg-springbok-bright"
    >
        GO HOME
    </button>
</div>