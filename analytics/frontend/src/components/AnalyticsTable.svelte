<script lang="ts" generics="T extends { id: number; prompt: string }">
  let {
    title,
    description,
    items,
    valueKey,
    valueLabel,
    formatValue = (value: unknown) => String(value),
  }: {
    title: string;
    description: string;
    items: T[];
    valueKey: keyof T;
    valueLabel: string;
    formatValue?: (value: unknown) => string;
  } = $props();
</script>

<section
  class="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/70"
>
  <div class="border-b border-zinc-800 px-5 py-4">
    <h2 class="text-sm font-bold text-white">{title}</h2>
    <p class="mt-1 text-xs text-zinc-500">{description}</p>
  </div>

  {#if items.length === 0}
    <div class="px-5 py-10 text-center text-sm text-zinc-600">
      No analytics data available.
    </div>
  {:else}
    <div class="divide-y divide-zinc-800">
      {#each items as item, index}
        <div class="flex items-start gap-4 px-5 py-4">
          <div
            class="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-zinc-800 text-[11px] font-bold text-zinc-500"
          >
            {index + 1}
          </div>

          <div class="min-w-0 flex-1">
            <p class="text-sm leading-5 text-zinc-300">
              {item.prompt}
            </p>

            <p class="mt-1 font-mono text-[10px] text-zinc-600">
              Q-{item.id}
            </p>
          </div>

          <div class="shrink-0 text-right">
            <p class="text-sm font-bold text-emerald-400">
              {formatValue(item[valueKey])}
            </p>

            <p
              class="mt-0.5 text-[10px] uppercase tracking-wider text-zinc-600"
            >
              {valueLabel}
            </p>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</section>
