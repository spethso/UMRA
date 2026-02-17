<script setup lang="ts">
import { computed } from 'vue'
import type { AnalyzerSummary } from '../types/risk'

const props = defineProps<{
  analyzersLoading: boolean
  analyzersErrorMessage: string
  analyzers: AnalyzerSummary[]
  selectedAnalyzerIds: string[]
}>()

const emit = defineEmits<{
  (e: 'toggle-analyzer', analyzerId: string): void
  (e: 'select-all'): void
}>()

const selectedAnalyzerCount = computed(() => props.selectedAnalyzerIds.length)
const isAnalyzerSelected = (analyzerId: string): boolean => props.selectedAnalyzerIds.includes(analyzerId)
</script>

<template>
  <section class="card">
    <h2>Analyzer selection</h2>
    <p v-if="analyzersLoading">Loading analyzers...</p>
    <p v-else-if="analyzersErrorMessage" class="error-line">Error loading analyzers: {{ analyzersErrorMessage }}</p>
    <template v-else>
      <p class="selection-line">Selected: {{ selectedAnalyzerCount }} / {{ analyzers.length }}</p>
      <div class="selection-actions">
        <button type="button" class="ghost-button" @click="emit('select-all')">Select all</button>
      </div>
      <ul class="analyzer-list">
        <li v-for="analyzer in analyzers" :key="analyzer.analyzerId" class="analyzer-item">
          <button
            type="button"
            class="analyzer-pill"
            :class="{ selected: isAnalyzerSelected(analyzer.analyzerId) }"
            :aria-pressed="isAnalyzerSelected(analyzer.analyzerId)"
            @click="emit('toggle-analyzer', analyzer.analyzerId)"
          >
            {{ analyzer.displayName }} ({{ analyzer.analyzerId }})
          </button>
        </li>
      </ul>
    </template>
  </section>
</template>
