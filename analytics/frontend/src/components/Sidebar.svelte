<script lang="ts">
  interface NavItem {
    id: string;
    label: string;
  }

  let {
    activeTab,
    onSelect,
  }: {
    activeTab: string;
    onSelect: (tab: string) => void;
  } = $props();

  const navItems: NavItem[] = [
    { id: "overview", label: "Overview" },
    { id: "questions", label: "Questions" },
    { id: "ai-insights", label: "AI Insights" },
  ];
</script>

<aside
  class="hidden w-64 shrink-0 border-r border-zinc-800 bg-zinc-950 lg:block"
>
  <div class="sticky top-0 flex h-screen flex-col">
    <div class="border-b border-zinc-800 px-6 py-6">
      <div class="flex items-center gap-3">
        <div
          class="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500 text-sm font-black text-zinc-950"
        >
          D
        </div>

        <div>
          <p class="text-sm font-black tracking-tight text-white">Doxa</p>
          <p class="text-[10px] uppercase tracking-widest text-zinc-600">
            Analytics
          </p>
        </div>
      </div>
    </div>

    <nav class="flex-1 px-3 py-5">
      <p
        class="px-3 pb-3 text-[10px] font-bold uppercase tracking-widest text-zinc-600"
      >
        Analytics
      </p>

      <div class="space-y-1">
        {#each navItems as item}
          <button
            type="button"
            onclick={() => onSelect(item.id)}
            class="flex w-full items-center rounded-xl px-3 py-2.5 text-left text-sm font-semibold transition {activeTab ===
            item.id
              ? 'bg-emerald-500/10 text-emerald-400'
              : 'text-zinc-500 hover:bg-zinc-900 hover:text-zinc-300'}"
          >
            {item.label}
          </button>
        {/each}
      </div>
    </nav>

    <div class="border-t border-zinc-800 px-6 py-5">
      <p class="text-[10px] uppercase tracking-widest text-zinc-700">
        How Southa Are You
      </p>
      <p class="mt-1 text-xs text-zinc-600">Analytics layer</p>
    </div>
  </div>
</aside>
