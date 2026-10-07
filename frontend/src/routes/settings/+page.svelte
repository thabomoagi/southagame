<script lang="ts">
    import { goto } from '$app/navigation';
    import { auth } from '$lib/stores/auth.svelte';
    import { api } from '$lib/api';
    import { requireAuth } from '$lib/auth-guard';
    import { isStrongPassword, PASSWORD_HINT } from '$lib/validation';
    import Avatar from '$lib/components/Avatar.svelte';
    import Field from '$lib/components/Field.svelte';
    import { onMount } from 'svelte';

    let currentPassword = $state('');
    let newPassword = $state('');
    let confirmPassword = $state('');
    let loading = $state(false);
    let error = $state('');
    let success = $state(false);

    let newUsername = $state('');
    let usernameLoading = $state(false);
    let usernameError = $state('');
    let usernameSuccess = $state(false);

    let newEmail = $state('');
    let emailLoading = $state(false);
    let emailError = $state('');
    let emailSuccess = $state(false);

    let uploading = $state(false);
    let picError = $state('');
    let picSuccess = $state(false);

    let deleteConfirm = $state(false);
    let deleting = $state(false);
    let deleteError = $state('');

    let fileInput: HTMLInputElement | undefined;

    onMount(() => {
        if (!requireAuth()) return;

        newUsername = auth.user?.username || '';
        newEmail = auth.user?.email || '';
    });

    const ALLOWED_TYPES = [
        'image/jpeg',
        'image/png',
        'image/webp',
        'image/gif',
        'image/bmp'
    ];

    const MAX_SIZE = 10 * 1024 * 1024;
    const MAX_AVATAR_DIMENSION = 512;
    const MAX_AVATAR_BYTES = 90 * 1024;

    function isSupportedImage(file: File): string {
        if (!ALLOWED_TYPES.includes(file.type)) {
            return 'Please choose a JPG, PNG, WebP, GIF or BMP image.';
        }

        if (file.size > MAX_SIZE) {
            return 'Image size must not exceed 10 MB.';
        }

        return '';
    }

    async function uploadAvatar(file: File) {
        picError = '';
        picSuccess = false;

        const problem = isSupportedImage(file);

        if (problem) {
            picError = problem;
            return;
        }

        uploading = true;

        try {
            const url = await api.uploadAvatar(file);

            auth.updateUserState({
                profilePictureUrl: url
            });

            picSuccess = true;
            
            setTimeout(() => {
                picSuccess = false;
            }, 5000);
        } catch (err) {
            picError =
                err instanceof Error
                    ? err.message
                    : 'Failed to upload your profile picture.';
        } finally {
            uploading = false;
        }
    }

    function onFilePicked(event: Event) {
        const input = event.currentTarget as HTMLInputElement;
        const file = input.files?.[0];

        input.value = '';

        if (file) {
            uploadAvatar(file);
        }
    }

    async function changeUsername() {
        usernameError = '';
        usernameSuccess = false;

        const value = newUsername.trim();

        if (!value || value.length < 3) {
            usernameError = 'Username must be at least 3 characters';
            return;
        }

        usernameLoading = true;

        try {
            await api.updateUser({
                username: value
            });

            auth.updateUserState({
                username: value
            });

            usernameSuccess = true;
            
            setTimeout(() => {
                usernameSuccess = false;
            }, 5000);
        } catch (err) {
            usernameError =
                err instanceof Error
                    ? err.message
                    : 'Failed to update username';
        } finally {
            usernameLoading = false;
        }
    }

    async function changeEmail() {
        emailError = '';
        emailSuccess = false;

        const value = newEmail.trim();

        if (!value || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            emailError = 'Please enter a valid email address';
            return;
        }

        emailLoading = true;

        try {
            await api.updateUser({
                email: value
            });

            auth.updateUserState({
                email: value
            });

            emailSuccess = true;
            
            setTimeout(() => {
                emailSuccess = false;
            }, 5000);
        } catch (err) {
            emailError =
                err instanceof Error
                    ? err.message
                    : 'Failed to update email';
        } finally {
            emailLoading = false;
        }
    }

    async function changePassword() {
        error = '';
        success = false;

        if (!isStrongPassword(newPassword)) {
            error = PASSWORD_HINT;
            return;
        }

        if (newPassword !== confirmPassword) {
            error = 'Passwords do not match';
            return;
        }

        loading = true;

        try {
            await api.changePassword(currentPassword, newPassword);

            success = true;

            currentPassword = '';
            newPassword = '';
            confirmPassword = '';
            
            setTimeout(() => {
                success = false;
            }, 5000);
        } catch (err) {
            error =
                err instanceof Error
                    ? err.message
                    : 'Failed to change password';
        } finally {
            loading = false;
        }
    }

    async function deleteAccount() {
        deleteError = '';
        deleting = true;

        try {
            await api.deleteUser();

            auth.logout();
            goto('/login');
        } catch (err) {
            deleteError =
                err instanceof Error
                    ? err.message
                    : 'Failed to delete account';

            deleting = false;
        }
    }

    function logout() {
        const refreshToken = localStorage.getItem('refreshToken');

        if (refreshToken) {
            api.logout(refreshToken).catch(() => {});
        }

        auth.logout();
        goto('/login');
    }
</script>

<svelte:head>
    <title>Settings — How Southa Are You?</title>
    <meta
        name="description"
        content="Manage your How Southa account, profile picture and security details."
    />
    <meta property="og:title" content="Settings — How Southa Are You?" />
</svelte:head>

<div class="dark:bg-dark-bg flex min-h-dvh flex-col bg-white">
    <div class="mx-auto w-full max-w-lg flex-1 px-4 py-8 pb-24 sm:px-6">

        <!-- Page Header -->
        <div
            class="mb-8 border-b border-slate-200 pb-5 text-center dark:border-slate-800 sm:text-left"
        >
            <h1
                class="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white sm:text-4xl"
            >
                Settings
            </h1>

            <p
                class="mt-1.5 text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400"
            >
                Manage your player profile and security details.
            </p>
        </div>

        <div class="space-y-6">

            <!-- Player Profile -->
            <section
                class="rounded-3xl border border-slate-200/80 bg-slate-50/50 p-6 shadow-xs dark:border-slate-800/80 dark:bg-slate-900/40"
            >
                <h2
                    class="text-xs font-extrabold uppercase tracking-widest text-slate-400 dark:text-slate-500"
                >
                    Player Profile
                </h2>

                <!-- Profile Picture -->
                <div class="mt-4 flex items-center gap-5">
                    <Avatar
                        src={auth.user?.profilePictureUrl}
                        name={auth.user?.username}
                        size="lg"
                        {uploading}
                    />

                    <div class="min-w-0 flex-1">
                        {#if uploading}
                            <p
                                class="flex items-center gap-2 text-xs font-bold text-slate-500 dark:text-slate-400"
                            >
                                <span
                                    class="h-4 w-4 animate-spin rounded-full border-2 border-springbok/30 border-t-springbok"
                                ></span>

                                Uploading avatar...
                            </p>
                        {:else if picSuccess}
                            <p
                                class="text-xs font-bold text-springbok dark:text-springbok-bright"
                            >
                                Profile picture updated successfully.
                            </p>
                        {:else}
                            <p
                                class="text-xs font-medium text-slate-500 dark:text-slate-400"
                            >
                                JPG, PNG, WebP, GIF or BMP up to 10 MB.
                            </p>
                        {/if}

                        <button
                            type="button"
                            onclick={() => fileInput?.click()}
                            disabled={uploading}
                            class="mt-3 inline-flex items-center gap-2 rounded-xl bg-springbok px-4 py-2 text-xs font-bold text-white shadow-sm transition-all hover:opacity-90 focus:outline-none focus-visible:ring-4 focus-visible:ring-springbok/40 active:scale-98 disabled:opacity-50 dark:bg-springbok-bright"
                        >
                            Change Picture
                        </button>
                    </div>
                </div>

                <input
                    bind:this={fileInput}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif,image/bmp"
                    class="sr-only"
                    onchange={onFilePicked}
                />

                {#if picError}
                    <p
                        class="mt-3 text-xs font-bold text-rose-600 dark:text-rose-400"
                    >
                        {picError}
                    </p>
                {/if}

                <!-- Username -->
                <div
                    class="mt-6 border-t border-slate-200/80 pt-6 dark:border-slate-800/80"
                >
                    {#if usernameSuccess}
                        <div
                            class="mb-4 rounded-xl bg-springbok/10 px-3.5 py-2.5 text-xs font-bold text-springbok dark:bg-springbok/15 dark:text-springbok-bright"
                        >
                            Username updated successfully.
                        </div>
                    {/if}

                    <form
                        onsubmit={(event) => {
                            event.preventDefault();
                            changeUsername();
                        }}
                        class="space-y-4"
                    >
                        <Field
                            id="username"
                            label="Username"
                            bind:value={newUsername}
                            placeholder="Username"
                            autocomplete="username"
                            required
                            minlength={3}
                            error={usernameError}
                        />

                        <button
                            type="submit"
                            disabled={
                                usernameLoading ||
                                !newUsername.trim() ||
                                newUsername.trim() === auth.user?.username
                            }
                            class="w-full rounded-xl bg-springbok px-4 py-2.5 text-center text-xs font-bold text-white shadow-sm transition-all hover:opacity-90 focus:outline-none focus-visible:ring-4 focus-visible:ring-springbok/40 active:scale-98 disabled:opacity-50 dark:bg-springbok-bright"
                        >
                            {usernameLoading ? 'Saving...' : 'Save Username'}
                        </button>
                    </form>
                </div>
            </section>

            <!-- Email & Security -->
            <section
                class="rounded-3xl border border-slate-200/80 bg-slate-50/50 p-6 shadow-xs dark:border-slate-800/80 dark:bg-slate-900/40"
            >
                <h2
                    class="text-xs font-extrabold uppercase tracking-widest text-slate-400 dark:text-slate-500"
                >
                    Email & Security
                </h2>

                <!-- Email -->
                {#if emailSuccess}
                    <div
                        class="mt-4 rounded-xl bg-springbok/10 px-3.5 py-2.5 text-xs font-bold text-springbok dark:bg-springbok/15 dark:text-springbok-bright"
                    >
                        Email updated successfully.
                    </div>
                {/if}

                <form
                    onsubmit={(event) => {
                        event.preventDefault();
                        changeEmail();
                    }}
                    class="mt-4 space-y-4"
                >
                    <Field
                        id="email"
                        label="Email Address"
                        type="email"
                        bind:value={newEmail}
                        placeholder="Email address"
                        autocomplete="email"
                        required
                        error={emailError}
                    />

                    <button
                        type="submit"
                        disabled={
                            emailLoading ||
                            !newEmail.trim() ||
                            newEmail.trim() === auth.user?.email
                        }
                        class="w-full rounded-xl bg-springbok px-4 py-2.5 text-center text-xs font-bold text-white shadow-sm transition-all hover:opacity-90 focus:outline-none focus-visible:ring-4 focus-visible:ring-springbok/40 active:scale-98 disabled:opacity-50 dark:bg-springbok-bright"
                    >
                        {emailLoading ? 'Saving...' : 'Save Email'}
                    </button>
                </form>

                <!-- Password -->
                <div
                    class="mt-6 border-t border-slate-200/80 pt-6 dark:border-slate-800/80"
                >
                    <h3
                        class="mb-4 text-xs font-extrabold uppercase tracking-widest text-slate-400 dark:text-slate-500"
                    >
                        Change Password
                    </h3>

                    {#if success}
                        <div
                            class="mb-4 rounded-xl bg-springbok/10 px-3.5 py-2.5 text-xs font-bold text-springbok dark:bg-springbok/15 dark:text-springbok-bright"
                        >
                            Password updated successfully.
                        </div>
                    {/if}

                    <form
                        onsubmit={(event) => {
                            event.preventDefault();
                            changePassword();
                        }}
                        class="space-y-4"
                    >
                        <Field
                            id="currentPassword"
                            label="Current Password"
                            type="password"
                            bind:value={currentPassword}
                            autocomplete="current-password"
                            required
                        />

                        <Field
                            id="newPassword"
                            label="New Password"
                            type="password"
                            bind:value={newPassword}
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
                            autocomplete="new-password"
                            minlength={8}
                            required
                        />

                        <button
                            type="submit"
                            disabled={
                                loading ||
                                !currentPassword ||
                                !newPassword ||
                                !confirmPassword
                            }
                            class="w-full rounded-xl bg-springbok px-4 py-2.5 text-center text-xs font-bold text-white shadow-sm transition-all hover:opacity-90 focus:outline-none focus-visible:ring-4 focus-visible:ring-springbok/40 active:scale-98 disabled:opacity-50 dark:bg-springbok-bright"
                        >
                            {loading ? 'Updating...' : 'Update Password'}
                        </button>
                    </form>
                </div>
            </section>

            <!-- Session -->
            <section
                class="rounded-3xl border border-slate-200/80 bg-slate-50/50 p-6 shadow-xs dark:border-slate-800/80 dark:bg-slate-900/40"
            >
                <h2
                    class="text-xs font-extrabold uppercase tracking-widest text-slate-400 dark:text-slate-500"
                >
                    Session
                </h2>

                <div class="mt-4">
                    <button
                        onclick={logout}
                        class="w-full rounded-xl border border-slate-200 bg-white px-5 py-3 text-center text-xs font-bold text-slate-700 shadow-xs transition-all hover:bg-slate-100 focus:outline-none focus-visible:ring-4 focus-visible:ring-slate-300/30 active:scale-98 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-750"
                    >
                        Log Out
                    </button>
                </div>
            </section>

            <!-- Danger Zone -->
            <section
                class="rounded-3xl border border-rose-200/80 bg-rose-50/30 p-6 shadow-xs dark:border-rose-900/50 dark:bg-rose-950/10"
            >
                <h2
                    class="text-xs font-extrabold uppercase tracking-widest text-rose-500 dark:text-rose-400"
                >
                    Danger Zone
                </h2>

                <div class="mt-4">
                    {#if !deleteConfirm}
                        <button
                            onclick={() => (deleteConfirm = true)}
                            class="w-full rounded-xl border border-rose-200 bg-rose-50/50 px-5 py-3 text-center text-xs font-bold text-rose-600 shadow-xs transition-all hover:bg-rose-100 focus:outline-none focus-visible:ring-4 focus-visible:ring-rose-500/20 active:scale-98 dark:border-rose-900/50 dark:bg-rose-950/20 dark:text-rose-400 dark:hover:bg-rose-950/40"
                        >
                            Delete Account
                        </button>
                    {:else}
                        <div
                            class="rounded-2xl border border-rose-300 bg-rose-50 p-4 dark:border-rose-800 dark:bg-rose-950/30"
                        >
                            <p
                                class="text-center text-xs font-bold text-rose-700 dark:text-rose-300"
                            >
                                This permanently deletes your account and all
                                game scores. This action cannot be undone.
                            </p>

                            {#if deleteError}
                                <p
                                    class="mt-2 text-center text-xs font-bold text-rose-600 dark:text-rose-400"
                                >
                                    {deleteError}
                                </p>
                            {/if}

                            <div class="mt-3 flex flex-col gap-2">
                                <button
                                    onclick={deleteAccount}
                                    disabled={deleting}
                                    class="w-full rounded-xl bg-rose-600 px-4 py-2.5 text-center text-xs font-bold text-white shadow-sm transition-all focus:outline-none focus-visible:ring-4 focus-visible:ring-rose-500/40 active:scale-98 disabled:opacity-50"
                                >
                                    {deleting
                                        ? 'Deleting...'
                                        : 'Yes, Delete Permanently'}
                                </button>

                                <button
                                    onclick={() => {
                                        deleteConfirm = false;
                                        deleteError = '';
                                    }}
                                    disabled={deleting}
                                    class="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-center text-xs font-bold text-slate-600 shadow-sm transition-all focus:outline-none focus-visible:ring-4 focus-visible:ring-slate-300/40 active:scale-98 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    {/if}
                </div>
            </section>
        </div>
    </div>
</div>