<script lang="ts">
  import type { AIInsights } from "../api/analytics";

  let {
    insights,
    loading,
    onRefresh,
  }: {
    insights: AIInsights | null;
    loading: boolean;
    onRefresh: () => void;
  } = $props();

  let refreshing = $state(false);

  async function handleRefresh() {
    refreshing = true;
    try {
      await onRefresh();
    } finally {
      refreshing = false;
    }
  }
</script>

<div class="space-y-6">
  <div class="flex flex-col justify-between gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/55 p-6 sm:flex-row sm:items-center">
    <div>
      <div class="flex items-center gap-2">
        <span class="inline-flex h-2 w-2 rounded-full {insights?.commentary_available ? 'bg-emerald-500' : 'bg-amber-500'}"></span>
        <span class="text-[10px] font-bold uppercase tracking-widest text-zinc-400">
          {insights?.commentary_available ? 'LLM Powered Intelligence' : 'Fallback Telemetry Mode'}
        </span>
      </div>
      <h2 class="mt-2 text-lg font-black tracking-tight text-white">Doxa AI Analytics & Game Insights</h2>
      <p class="mt-1 text-xs text-zinc-400">
        Automated analysis of player accuracy, response times, and question difficulty patterns.
      </p>
    </div>

    <button
      type="button"
      onclick={handleRefresh}
      disabled={refreshing || loading}
      class="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-2.5 text-xs font-bold text-zinc-950 transition hover:bg-emerald-400 disabled:opacity-50"
    >
      {#if refreshing}
        <div class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-950 border-t-transparent"></div>
        <span>Refreshing...</span>
      {:else}
        <span>Refresh Insights</span>
      {/if}
    </button>
  </div>

  {#if loading && !insights}
    <div class="flex min-h-[40vh] items-center justify-center">
      <div class="text-center">
        <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-zinc-800 border-t-emerald-500"></div>
        <p class="mt-4 text-sm text-zinc-500">Generating AI game insights...</p>
      </div>
    </div>
  {:else if insights}
    {#if !insights.commentary_available}
      <div class="rounded-2xl border border-amber-900/50 bg-amber-950/20 p-5 text-amber-400">
        <p class="text-xs font-bold uppercase tracking-wider">Notice</p>
        <p class="mt-1 text-xs text-amber-300/80">{insights.summary}</p>
      </div>
    {/if}

    <div class="grid gap-6 lg:grid-cols-2">
      <div class="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-6">
        <h3 class="text-sm font-bold uppercase tracking-wider text-emerald-400">Executive Summary</h3>
        <p class="mt-3 text-sm leading-relaxed text-zinc-300">{insights.summary}</p>

        <h3 class="mt-6 text-sm font-bold uppercase tracking-wider text-emerald-400">Difficulty Analysis</h3>
        <p class="mt-3 text-sm leading-relaxed text-zinc-300">{insights.difficulty_analysis}</p>
      </div>

      <div class="space-y-6">
        <div class="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-6">
          <h3 class="text-sm font-bold uppercase tracking-wider text-emerald-400">Key Findings</h3>
          <ul class="mt-3 space-y-2.5">
            {#each insights.key_findings as finding}
              <li class="flex items-start gap-2.5 text-xs leading-relaxed text-zinc-300">
                <span class="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500"></span>
                <span>{finding}</span>
              </li>
            {/each}
          </ul>
        </div>

        <div class="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-6">
          <h3 class="text-sm font-bold uppercase tracking-wider text-emerald-400">Creator Recommendations</h3>
          <ul class="mt-3 space-y-2.5">
            {#each insights.recommendations as rec}
              <li class="flex items-start gap-2.5 text-xs leading-relaxed text-zinc-300">
                <span class="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500"></span>
                <span>{rec}</span>
              </li>
            {/each}
          </ul>
        </div>
      </div>
    </div>
  {/if}
</div>
