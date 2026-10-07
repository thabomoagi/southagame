<script lang="ts">
  import { onMount } from "svelte";

  import AnalyticsTable from "./components/AnalyticsTable.svelte";
  import Sidebar from "./components/Sidebar.svelte";
  import StatCard from "./components/StatCard.svelte";

  import {
    fetchEasiest,
    fetchFastest,
    fetchHardest,
    fetchMostMissed,
    fetchMostPlayed,
    fetchOverview,
    fetchSlowest,
    type AccuracyQuestion,
    type MissedQuestion,
    type MostPlayedQuestion,
    type OverviewStats,
    type ResponseTimeQuestion,
  } from "./api/analytics";

  let activeTab = $state("overview");

  let overview = $state<OverviewStats | null>(null);
  let mostPlayed = $state<MostPlayedQuestion[]>([]);
  let hardest = $state<AccuracyQuestion[]>([]);
  let easiest = $state<AccuracyQuestion[]>([]);
  let mostMissed = $state<MissedQuestion[]>([]);
  let fastest = $state<ResponseTimeQuestion[]>([]);
  let slowest = $state<ResponseTimeQuestion[]>([]);

  let loading = $state(true);
  let error = $state("");

  const pageTitles: Record<string, string> = {
    overview: "Overview",
    questions: "Question Analytics",
  };

  function selectTab(tab: string) {
    activeTab = tab;
  }

  function formatPercentage(value: unknown): string {
    return `${Number(value).toFixed(1)}%`;
  }

  function formatMilliseconds(value: unknown): string {
    const milliseconds = Number(value);

    if (milliseconds >= 1000) {
      return `${(milliseconds / 1000).toFixed(1)}s`;
    }

    return `${Math.round(milliseconds)}ms`;
  }

  async function loadAnalytics() {
    loading = true;
    error = "";

    try {
      const [
        overviewResult,
        mostPlayedResult,
        hardestResult,
        easiestResult,
        mostMissedResult,
        fastestResult,
        slowestResult,
      ] = await Promise.all([
        fetchOverview(),
        fetchMostPlayed(),
        fetchHardest(),
        fetchEasiest(),
        fetchMostMissed(),
        fetchFastest(),
        fetchSlowest(),
      ]);

      overview = overviewResult;
      mostPlayed = mostPlayedResult;
      hardest = hardestResult;
      easiest = easiestResult;
      mostMissed = mostMissedResult;
      fastest = fastestResult;
      slowest = slowestResult;
    } catch (err) {
      error =
        err instanceof Error
          ? err.message
          : "Unable to connect to the Doxa API.";
    } finally {
      loading = false;
    }
  }

  onMount(loadAnalytics);
</script>

<svelte:head>
  <title>Doxa — Analytics</title>
  <meta
    name="description"
    content="Doxa analytics dashboard for How Southa Are You"
  />
</svelte:head>

<div class="min-h-screen bg-zinc-950 text-zinc-100">
  <div class="flex min-h-screen">
    <Sidebar {activeTab} onSelect={selectTab} />

    <main class="min-w-0 flex-1">
      <header
        class="border-b border-zinc-800 bg-zinc-950/90 px-5 py-5 backdrop-blur sm:px-8"
      >
        <div class="mx-auto flex max-w-7xl items-center justify-between">
          <div>
            <p
              class="text-[10px] font-bold uppercase tracking-[0.2em] text-emerald-500"
            >
              Doxa Analytics
            </p>

            <h1
              class="mt-1 text-xl font-black tracking-tight text-white sm:text-2xl"
            >
              {pageTitles[activeTab] ?? "Analytics"}
            </h1>
          </div>

          <div
            class="flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-900 px-3 py-1.5"
          >
            <span class="h-2 w-2 rounded-full bg-emerald-500"></span>

            <span
              class="text-[10px] font-semibold uppercase tracking-wider text-zinc-500"
            >
              Live
            </span>
          </div>
        </div>
      </header>

      <div class="mx-auto max-w-7xl px-5 py-6 sm:px-8 sm:py-8">
        {#if loading}
          <div class="flex min-h-[60vh] items-center justify-center">
            <div class="text-center">
              <div
                class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-zinc-800 border-t-emerald-500"
              ></div>

              <p class="mt-4 text-sm text-zinc-500">Loading analytics...</p>
            </div>
          </div>
        {:else if error}
          <div class="rounded-2xl border border-red-900/50 bg-red-950/20 p-6">
            <p class="text-sm font-semibold text-red-400">
              Unable to load analytics
            </p>

            <p class="mt-2 text-xs text-red-500/70">
              {error}
            </p>

            <button
              type="button"
              onclick={loadAnalytics}
              class="mt-4 rounded-lg bg-zinc-800 px-4 py-2 text-xs font-semibold text-zinc-300 transition hover:bg-zinc-700"
            >
              Retry
            </button>
          </div>
        {:else if activeTab === "overview" && overview}
          <section>
            <div class="mb-7">
              <p class="max-w-2xl text-sm leading-6 text-zinc-500">
                A live view of player activity and question performance across
                How Southa Are You.
              </p>
            </div>

            <div class="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-4">
              <StatCard
                label="Players"
                value={overview.total_users}
                description="Registered users"
              />

              <StatCard
                label="Attempts"
                value={overview.total_attempts}
                description="Games played"
              />

              <StatCard
                label="Questions"
                value={overview.total_questions}
                description="Question bank"
              />

              <StatCard
                label="Categories"
                value={overview.total_categories}
                description="Active categories"
              />
            </div>

            <div class="mt-8 grid gap-5 xl:grid-cols-2">
              <AnalyticsTable
                title="Most Played"
                description="Questions appearing most frequently in games."
                items={mostPlayed}
                valueKey="play_count"
                valueLabel="plays"
              />

              <AnalyticsTable
                title="Most Missed"
                description="Questions with the highest number of incorrect answers."
                items={mostMissed}
                valueKey="miss_count"
                valueLabel="misses"
              />
            </div>

            <div class="mt-5 grid gap-5 xl:grid-cols-2">
              <AnalyticsTable
                title="Hardest Questions"
                description="Lowest observed answer accuracy."
                items={hardest}
                valueKey="accuracy_percentage"
                valueLabel="accuracy"
                formatValue={formatPercentage}
              />

              <AnalyticsTable
                title="Easiest Questions"
                description="Highest observed answer accuracy."
                items={easiest}
                valueKey="accuracy_percentage"
                valueLabel="accuracy"
                formatValue={formatPercentage}
              />
            </div>
          </section>
        {:else if activeTab === "questions"}
          <section>
            <div class="mb-7">
              <p class="max-w-2xl text-sm leading-6 text-zinc-500">
                Detailed question-level performance based on real player
                attempts and answer behaviour.
              </p>
            </div>

            <div class="grid gap-5 xl:grid-cols-2">
              <AnalyticsTable
                title="Hardest Questions"
                description="Lowest observed answer accuracy."
                items={hardest}
                valueKey="accuracy_percentage"
                valueLabel="accuracy"
                formatValue={formatPercentage}
              />

              <AnalyticsTable
                title="Easiest Questions"
                description="Highest observed answer accuracy."
                items={easiest}
                valueKey="accuracy_percentage"
                valueLabel="accuracy"
                formatValue={formatPercentage}
              />

              <AnalyticsTable
                title="Fastest Responses"
                description="Questions answered in the shortest average time."
                items={fastest}
                valueKey="avg_time_ms"
                valueLabel="average"
                formatValue={formatMilliseconds}
              />

              <AnalyticsTable
                title="Slowest Responses"
                description="Questions taking the longest average time."
                items={slowest}
                valueKey="avg_time_ms"
                valueLabel="average"
                formatValue={formatMilliseconds}
              />

              <div class="xl:col-span-2">
                <AnalyticsTable
                  title="Most Played Questions"
                  description="Questions appearing most frequently across attempts."
                  items={mostPlayed}
                  valueKey="play_count"
                  valueLabel="plays"
                />
              </div>

              <div class="xl:col-span-2">
                <AnalyticsTable
                  title="Most Missed Questions"
                  description="Questions producing the most incorrect answers."
                  items={mostMissed}
                  valueKey="miss_count"
                  valueLabel="misses"
                />
              </div>
            </div>
          </section>
        {/if}
      </div>
    </main>
  </div>
</div>
