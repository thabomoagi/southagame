<script lang="ts">
    import type { Snippet } from 'svelte';

    let {
        type = 'button',
        variant = 'primary',
        loading = false,
        disabled = false,
        onclick,
        children
    }: {
        type?: 'button' | 'submit';
        variant?: 'primary' | 'secondary' | 'ghost';
        loading?: boolean;
        disabled?: boolean;
        onclick?: () => void;
        children: Snippet;
    } = $props();

    const variants: Record<string, string> = {
        primary:
            'bg-springbok text-white hover:bg-springbok-bright dark:bg-springbok-bright dark:hover:bg-springbok shadow-lg shadow-springbok/25',
        secondary: 'bg-sa-yellow text-slate-900 hover:bg-amber-400 shadow-lg shadow-sa-yellow/25',
        ghost: 'bg-transparent text-springbok dark:text-sa-yellow border-2 border-springbok dark:border-sa-yellow'
    };
</script>

<button
    {type}
    {onclick}
    disabled={disabled || loading}
    class="flex w-full items-center justify-center gap-3 rounded-2xl px-6 py-4 text-lg font-black tracking-wide transition-transform active:scale-[0.98] disabled:opacity-50 disabled:active:scale-100 {variants[variant]}"
>
    {#if loading}
        <span class="h-5 w-5 animate-spin rounded-full border-[3px] border-current/30 border-t-current"></span>
    {/if}
    {@render children()}
</button>