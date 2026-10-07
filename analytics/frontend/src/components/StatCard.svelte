<script lang="ts">
  let {
    label,
    value,
    description,
  }: {
    label: string;
    value: number;
    description: string;
  } = $props();
</script>

<div class="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5">
  <p class="text-xs font-semibold uppercase tracking-wider text-zinc-500">
    {label}
  </p>

  <p class="mt-3 text-3xl font-black tracking-tight text-white">
    {(value ?? 0).toLocaleString()}
  </p>

  <p class="mt-1 text-xs text-zinc-600">
    {description}
  </p>
</div>
