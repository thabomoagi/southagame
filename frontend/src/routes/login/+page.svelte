<script lang="ts">
    import { goto } from '$app/navigation';
    import { auth } from '$lib/stores/auth.svelte';
    import Button from '$lib/components/Button.svelte';
    import Field from '$lib/components/Field.svelte';
    import Logo from '$lib/components/Logo.svelte';

    let identifier = $state('');
    let password = $state('');
    let loading = $state(false);
    let error = $state('');

    async function submit(e: SubmitEvent) {
        e.preventDefault();
        error = '';
        loading = true;

        try {
            await auth.login(identifier.trim(), password);
            goto('/home');
        } catch (err) {
            error = err instanceof Error ? err.message : 'Sign in failed. Try again.';
        } finally {
            loading = false;
        }
    }
</script>

<svelte:head>
    <title>Sign In - How Southa Are You?</title>
    <meta
        name="description"
        content="Sign in to How Southa Are You? and start playing."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col justify-center px-6 py-12">
    <div class="mx-auto w-full max-w-sm">
        <div class="mb-10 flex flex-col items-center">
            <Logo />

            <h1 class="mt-4 text-center text-3xl font-black tracking-tight text-slate-900 dark:text-white">
                How Southa Are You?
            </h1>
        </div>

        <form onsubmit={submit} class="space-y-4">
            <Field
                id="identifier"
                label="Username or Email"
                bind:value={identifier}
                placeholder="Username or Email"
                autocomplete="username"
                autocapitalize="none"
                autocorrect="off"
                required
            />

            <Field
                id="password"
                label="Password"
                type="password"
                bind:value={password}
                placeholder="Password"
                autocomplete="current-password"
                required
            />

            {#if error}
                <p class="text-center text-sm font-bold text-red-600 dark:text-red-400">
                    {error}
                </p>
            {/if}

            <Button
                type="submit"
                {loading}
                disabled={!identifier.trim() || !password}
            >
                Sign In
            </Button>
        </form>

        <div class="mt-6 text-center">
            <a
                href="/forgot-password"
                class="text-sm font-bold text-slate-500 underline-offset-4 hover:underline dark:text-slate-400"
            >
                Forgot your password?
            </a>
        </div>

        <p class="mt-4 text-center text-sm font-semibold text-slate-500 dark:text-slate-400">
            Don't have an account?
            <a
                href="/register"
                class="font-black text-springbok underline-offset-4 hover:underline dark:text-sa-yellow"
            >
                Register
            </a>
        </p>
    </div>
</div>