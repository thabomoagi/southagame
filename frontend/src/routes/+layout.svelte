<script lang="ts">
    import { browser } from '$app/environment';
    import { auth } from '$lib/stores/auth.svelte';
    import { theme } from '$lib/stores/theme.svelte';
    import BottomNav from '$lib/components/BottomNav.svelte';
    import '../app.css';

    let { children } = $props();

    theme.init();
    auth.init();

    $effect(() => {
        if (!browser) return;
        document.documentElement.classList.toggle('dark', theme.dark);
        localStorage.setItem('darkMode', theme.dark ? '1' : '0');
    });
</script>

{@render children()}

<BottomNav />