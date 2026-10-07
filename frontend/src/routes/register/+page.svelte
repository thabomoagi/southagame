<script lang="ts">
    import { goto } from '$app/navigation';
    import { auth } from '$lib/stores/auth.svelte';
    import Button from '$lib/components/Button.svelte';
    import Field from '$lib/components/Field.svelte';
    import Logo from '$lib/components/Logo.svelte';
    import { isStrongPassword, PASSWORD_HINT } from '$lib/validation';

    let username = $state('');
    let email = $state('');
    let password = $state('');
    let loading = $state(false);
    let error = $state('');

    async function submit(e: SubmitEvent) {
        e.preventDefault();
        error = '';

        if (!isStrongPassword(password)) {
            error = PASSWORD_HINT;
            return;
        }

        loading = true;

        try {
            await auth.register(username.trim(), email.trim().toLowerCase(), password);
            goto('/home');
        } catch (err) {
            error = err instanceof Error
                ? err.message
                : 'Registration failed. Try again.';
        } finally {
            loading = false;
        }
    }
</script>

<svelte:head>
    <title>Register - How Southa Are You?</title>
    <meta
        name="description"
        content="Create an account to play How Southa Are You? and compete on the leaderboard."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col justify-center px-6 py-12">
    <div class="mx-auto w-full max-w-sm">

        <div class="mb-10 flex flex-col items-center">
            <Logo />

            <h1 class="mt-4 text-center text-3xl font-black tracking-tight text-slate-900 dark:text-white">
                Register
            </h1>
        </div>

        <form onsubmit={submit} class="space-y-4">
            <Field
                id="username"
                label="Username"
                bind:value={username}
                placeholder="Username"
                autocomplete="username"
                autocapitalize="none"
                autocorrect="off"
                required
                minlength={3}
            />

            <Field
                id="email"
                label="Email"
                type="email"
                bind:value={email}
                placeholder="Email"
                autocomplete="email"
                autocapitalize="none"
                required
            />

            <Field
                id="password"
                label="Password"
                type="password"
                bind:value={password}
                placeholder="Password"
                autocomplete="new-password"
                required
                minlength={8}
                hint={PASSWORD_HINT}
                {error}
            />

            <Button
                type="submit"
                {loading}
                disabled={
                    !username.trim() ||
                    !email.trim() ||
                    !isStrongPassword(password)
                }
            >
                Register
            </Button>
        </form>

        <p class="mt-8 text-center text-sm font-semibold text-slate-500 dark:text-slate-400">
            Already have an account?
            <a
                href="/login"
                class="font-black text-springbok underline-offset-4 hover:underline dark:text-sa-yellow"
            >
                Sign In
            </a>
        </p>
    </div>
</div>