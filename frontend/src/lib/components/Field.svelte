<script lang="ts">
    import type { HTMLInputAttributes } from 'svelte/elements';

    let {
        id,
        label,
        type = 'text',
        value = $bindable(''),
        placeholder = '',
        autocomplete,
        autocapitalize = 'none',
        autocorrect = 'off',
        spellcheck = false,
        required = false,
        minlength,
        maxlength,
        error = '',
        hint = ''
    }: {
        id: string;
        label: string;
        type?: 'text' | 'email' | 'password' | 'url';
        value?: string;
        placeholder?: string;
        autocomplete?: HTMLInputAttributes['autocomplete'];
        autocapitalize?: HTMLInputAttributes['autocapitalize'];
        autocorrect?: HTMLInputAttributes['autocorrect'];
        spellcheck?: boolean;
        required?: boolean;
        minlength?: number;
        maxlength?: number;
        error?: string;
        hint?: string;
    } = $props();

    let showPassword = $state(false);

    const isPassword = $derived(type === 'password');
    const inputType = $derived(isPassword && showPassword ? 'text' : type);
    const hasError = $derived(Boolean(error));

    const inputClasses = $derived(
        [
            'w-full rounded-2xl border-2 bg-white px-5 text-lg font-semibold transition-colors',
            'placeholder:text-slate-400 focus:border-springbok focus:outline-none',
            'dark:bg-dark-surface dark:placeholder:text-slate-500',
            isPassword ? 'py-4 pr-14' : 'py-4',
            hasError
                ? 'border-red-400 focus:border-red-500 dark:border-red-500/60'
                : 'border-slate-200 dark:border-slate-700'
        ].join(' ')
    );
</script>

<label class="block">
    <span class="mb-2 block text-sm font-bold text-slate-700 dark:text-slate-300">
        {label}{required ? ' *' : ''}
    </span>

    <div class="relative">
        <input
            {id}
            type={inputType}
            bind:value
            {placeholder}
            {autocomplete}
            {autocapitalize}
            {autocorrect}
            {spellcheck}
            {required}
            {minlength}
            {maxlength}
            name={id}
            class={inputClasses}
            aria-invalid={hasError || undefined}
            aria-describedby={hasError ? `${id}-error` : hint ? `${id}-hint` : undefined}
        />

        {#if isPassword}
            <button
                type="button"
                onclick={() => (showPassword = !showPassword)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                class="absolute top-1/2 right-3 -translate-y-1/2 rounded-xl p-2 text-slate-400 transition-colors hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-springbok/30 dark:hover:text-slate-300"
            >
                {#if showPassword}
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M13.875 18.825A10.05 10.05 0 0 1 12 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 0 1 4.07-4.069m1.588-2.716A9.97 9.97 0 0 1 12 5c4.478 0 8.573 3.007 9.963 7a9.986 9.986 0 0 1-4.07 4.069m-1.588 2.716a3.5 3.5 0 0 1-4.886-4.886m4.886 4.886L21 21m-8.758-8.758 8.758 8.758M3 3l8.758 8.758M3 3l18 18"
                        />
                    </svg>
                {:else}
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z"
                        />
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"
                        />
                    </svg>
                {/if}
            </button>
        {/if}
    </div>

    {#if hint}
        <span id="{id}-hint" class="mt-1.5 block text-xs font-semibold text-slate-500 dark:text-slate-400">
            {hint}
        </span>
    {/if}

    {#if hasError}
        <span id="{id}-error" class="mt-1.5 block text-sm font-bold text-red-600 dark:text-red-400">
            {error}
        </span>
    {/if}
</label>