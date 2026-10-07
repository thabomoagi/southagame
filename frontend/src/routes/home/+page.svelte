<script lang="ts">
    import Logo from '$lib/components/Logo.svelte';
    import { auth } from '$lib/stores/auth.svelte';
</script>

<div class="flex min-h-dvh flex-col bg-white dark:bg-dark-bg">
    <main class="flex flex-1 flex-col items-center justify-center px-6">
        <Logo />
        <h1 class="mt-4 text-center text-3xl font-black tracking-tight text-slate-900 dark:text-white">
            How Southa Are You?
        </h1>

        <div class="mt-12 flex w-full max-w-sm flex-col gap-5">
            <a
                href="/thirty-seconds"
                class="rounded-3xl bg-springbok px-6 py-7 text-center text-white shadow-xl shadow-springbok/25 transition-transform active:scale-[0.98] dark:bg-springbok-bright"
            >
                <span class="block text-3xl font-black tracking-wide">30 Seconds</span>
                <span class="mt-1 block text-sm font-bold uppercase tracking-widest text-white/75">
                    Multiplayer
                </span>
            </a>
            <a
                href="/multiple-choice"
                class="rounded-3xl bg-sa-yellow px-6 py-7 text-center text-slate-900 shadow-xl shadow-sa-yellow/25 transition-transform active:scale-[0.98]"
            >
                <span class="block text-3xl font-black tracking-wide">Multiple Choice</span>
                <span class="mt-1 block text-sm font-bold uppercase tracking-widest text-slate-800/70">
                    Solo
                </span>
            </a>
        </div>
    </main>

    <footer class="flex items-center justify-center gap-10 px-6 py-8">
        {#if auth.user}
            <a href="/profile" class="text-sm font-bold text-slate-500 underline-offset-4 hover:underline dark:text-slate-400">Profile</a>
            <a href="/settings" class="text-sm font-bold text-slate-500 underline-offset-4 hover:underline dark:text-slate-400">Settings</a>
        {:else}
            <a href="/login" class="text-sm font-bold text-slate-500 underline-offset-4 hover:underline dark:text-slate-400">Log in</a>
            <a href="/register" class="text-sm font-bold text-slate-500 underline-offset-4 hover:underline dark:text-slate-400">Register</a>
        {/if}
    </footer>
</div>