<script lang="ts">
    import { goto } from '$app/navigation';
</script>

<svelte:head>
    <title>30 Seconds — How Southa Are You?</title>
    <meta
        name="description"
        content="Play the classic South African 30 Seconds guessing game with your friends."
    />
    <meta property="og:title" content="30 Seconds — How Southa Are You?" />
    <meta
        property="og:description"
        content="Give clues, guess the words and see how many you can get in 30 seconds."
    />
</svelte:head>

<div class="flex min-h-dvh flex-col bg-white px-6 py-12 dark:bg-dark-bg">
    <main class="flex flex-1 flex-col items-center justify-center text-center">
        <div class="w-full max-w-sm">
            <h1
                class="text-5xl font-black tracking-tight text-slate-900 dark:text-white"
            >
                30 Seconds
            </h1>

            <p
                class="mt-2 text-sm font-black uppercase tracking-widest text-slate-500 dark:text-slate-400"
            >
                Group / Party
            </p>

            <div
                class="mt-10 space-y-5 text-lg font-semibold leading-relaxed text-slate-700 dark:text-slate-200"
            >
                <p>It's the classic South African guessing game.</p>

                <p>
                    One player guesses while their team gives clues to help them
                    guess the <strong class="font-black">5 words</strong> on the card.
                </p>

                <p>
                    The team may describe, explain or give hints about the words, but
                    they <strong class="font-black">cannot</strong> say the words themselves.
                </p>

                <p>
                    You have <strong class="font-black">30 seconds</strong> to guess as
                    many as possible before the timer reaches zero.
                </p>
            </div>
        </div>
    </main>

    <footer class="pt-8">
        <button
            type="button"
            onclick={() => goto('/thirty-seconds/play')}
            class="w-full rounded-3xl bg-springbok px-6 py-5 text-2xl font-black tracking-wider text-white shadow-xl shadow-springbok/30 transition-transform active:scale-[0.98] dark:bg-springbok-bright"
        >
            START GAME
        </button>
    </footer>
</div>