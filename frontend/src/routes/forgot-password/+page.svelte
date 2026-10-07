<script lang="ts">
    import { goto } from '$app/navigation';  // ADD THIS
    import { api } from '$lib/api';
    import Button from '$lib/components/Button.svelte';
    import Field from '$lib/components/Field.svelte';
    import Logo from '$lib/components/Logo.svelte';
    import { onMount } from 'svelte';

    let email = $state('');
    let loading = $state(false);
    let error = $state('');
    let success = $state(false);

    onMount(() => {
        email = '';
    });

    async function submit(event: SubmitEvent) {
        event.preventDefault();

        error = '';
        success = false;
        loading = true;

        try {
            await api.forgotPassword(email.trim());

            // CHANGE THIS: Redirect to reset page instead of showing success
            goto('/reset-password');
        } catch (err) {
            const message =
                err instanceof Error
                    ? err.message
                    : 'Failed to send reset code. Please try again.';

            if (
                message.toLowerCase().includes('too many') ||
                message.toLowerCase().includes('rate limit')
            ) {
                error = 'Too many reset requests. Please wait a while before trying again.';
            } else {
                error = message;
            }
        } finally {
            loading = false;
        }
    }
</script>

<svelte:head>
    <title>Reset Password — How Southa Are You?</title>
    <meta
        name="description"
        content="Reset your How Southa Are You password."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    <div class="flex flex-1 flex-col items-center justify-center">
        <div class="w-full max-w-sm">
            <div class="flex flex-col items-center text-center">
                <Logo />

                <h1 class="mt-6 text-4xl font-black tracking-tight text-slate-900 dark:text-white">
                    Reset Password
                </h1>

                <p class="mt-3 text-base font-semibold text-slate-600 dark:text-slate-300">
                    Enter your email or username to receive a reset code.
                </p>
            </div>

            {#if success}
                <div
                    class="mt-8 rounded-2xl bg-springbok/10 px-5 py-4 text-center text-sm font-bold text-springbok dark:bg-springbok/20 dark:text-springbok-bright"
                >
                    Check your email for a password reset code.
                </div>
            {/if}

            <form onsubmit={submit} class="mt-8 space-y-5">
                <Field
                    id="email"
                    label="Email or username"
                    bind:value={email}
                    placeholder="Email or username"
                    autocomplete="username"
                    autocapitalize="none"
                    autocorrect="off"
                    required
                    {error}
                />

                <Button
                    type="submit"
                    {loading}
                    disabled={!email.trim()}
                >
                    SEND RESET CODE
                </Button>
            </form>

            <div class="mt-8 flex flex-col gap-3 text-center text-sm font-semibold">
                <a
                    href="/login"
                    class="text-slate-600 underline underline-offset-4 transition-colors hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                >
                    Remember your password? Sign In
                </a>

                <a
                    href="/register"
                    class="text-slate-600 underline underline-offset-4 transition-colors hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                >
                    Need an account? Register
                </a>
            </div>
        </div>
    </div>
</div>