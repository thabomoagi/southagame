<script lang="ts">
    import { resolveImageUrl } from '$lib/image-url';

    let {
        src = null,
        name = '',
        alt = '',
        size = 'md',
        uploading = false
    }: {
        src?: string | null;
        name?: string;
        alt?: string;
        size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
        uploading?: boolean;
    } = $props();

    let failed = $state(false);

    const resolved = $derived(src ? resolveImageUrl(src) : null);
    const initial = $derived(name ? name.charAt(0).toUpperCase() : '?');

    $effect(() => {
        void resolved;
        failed = false;
    });

    const sizeClasses: Record<string, string> = {
        xs: 'h-8 w-8 text-xs',
        sm: 'h-10 w-10 text-sm',
        md: 'h-14 w-14 text-xl',
        lg: 'h-20 w-20 text-3xl',
        xl: 'h-28 w-28 text-5xl'
    };

    const fallbackClasses = $derived(
        `${sizeClasses[size]} flex shrink-0 items-center justify-center rounded-full bg-springbok font-black text-white dark:bg-springbok-bright`
    );
</script>

<div class="relative inline-flex shrink-0">
    {#if resolved && !failed}
        <img
            src={resolved}
            alt={alt || name}
            loading={size === 'xl' ? 'eager' : 'lazy'}
            decoding="async"
            class="rounded-full object-cover {sizeClasses[size]}"
            onerror={() => (failed = true)}
        />
    {:else}
        <div class={fallbackClasses} aria-hidden="true">
            {initial}
        </div>
    {/if}

    {#if uploading}
        <div class="absolute inset-0 flex items-center justify-center rounded-full bg-black/40 backdrop-xs">
            <div class="h-6 w-6 animate-spin rounded-full border-2 border-white/30 border-t-white"></div>
        </div>
    {/if}
</div>