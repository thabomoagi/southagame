<script lang="ts">
    import { goto } from '$app/navigation';
    import { api } from '$lib/api';
    import Button from '$lib/components/Button.svelte';
    import Field from '$lib/components/Field.svelte';
    import Logo from '$lib/components/Logo.svelte';
    import { isStrongPassword, PASSWORD_HINT } from '$lib/validation';

    let token = $state('');
    let newPassword = $state('');
    let confirmPassword = $state('');
    let loading = $state(false);
    let error = $state('');
    let success = $state(false);

    async function submit(event: SubmitEvent) {
        event.preventDefault();

        error = '';
        success = false;

        if (!isStrongPassword(newPassword)) {
            error = PASSWORD_HINT;
            return;
        }

        if (newPassword !== confirmPassword) {
            error = 'Passwords do not match.';
            return;
        }

        loading = true;

        try {
            await api.resetPassword(token.trim(), newPassword);

            success = true;

            token = '';
            newPassword = '';
            confirmPassword = '';

            setTimeout(() => {
                goto('/login');
            }, 2000);
        } catch (err) {
            error =
                err instanceof Error
                    ? err.message
                    : 'Failed to reset password. Please try again.';
        } finally {
            loading = false;
        }
    }
</script>

<svelte:head>
    <title>Create New Password — How Southa Are You?</title>
    <meta
        name="description"
        content="Create a new password for your How Southa Are You account."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    <div class="flex flex-1 flex-col items-center justify-center">
        <div class="w-full max-w-sm">
            <div class="flex flex-col items-center text-center">
                <Logo />

                <h1 class="mt-6 text-4xl font-black tracking-tight text-slate-900 dark:text-white">
                    New Password
                </h1>

                <p class="mt-3 text-base font-semibold text-slate-600 dark:text-slate-300">
                    Enter the reset code from your email and choose a new password.
                </p>
            </div>

            {#if success}
                <div
                    class="mt-8 rounded-2xl bg-springbok/10 px-5 py-4 text-center text-sm font-bold text-springbok dark:bg-springbok/20 dark:text-springbok-bright"
                >
                    Password updated successfully. Redirecting to sign in...
                </div>
            {/if}

            <form onsubmit={submit} class="mt-8 space-y-5">
                <Field
                    id="token"
                    label="Reset Code"
                    bind:value={token}
                    placeholder="Reset code from email"
                    autocomplete="one-time-code"
                    required
                />

                <Field
                    id="newPassword"
                    label="New Password"
                    type="password"
                    bind:value={newPassword}
                    placeholder="New password"
                    autocomplete="new-password"
                    minlength={8}
                    required
                    hint={PASSWORD_HINT}
                    {error}
                />

                <Field
                    id="confirmPassword"
                    label="Confirm New Password"
                    type="password"
                    bind:value={confirmPassword}
                    placeholder="Confirm new password"
                    autocomplete="new-password"
                    minlength={8}
                    required
                />

                <Button
                    type="submit"
                    {loading}
                    disabled={!token.trim() || !newPassword || !confirmPassword}
                >
                    RESET PASSWORD
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
                    href="/forgot-password"
                    class="text-slate-600 underline underline-offset-4 transition-colors hover:text-slate-900 dark:text-slate-400 dark:hover:text-white"
                >
                    Need a new reset code?
                </a>
            </div>
        </div>
    </div>
</div>