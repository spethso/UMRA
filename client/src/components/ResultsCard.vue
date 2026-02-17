<script setup lang="ts">
import type { AnalysisResult, HorizonAggregateRow } from '../types/risk'

defineProps<{
  analysisResult: AnalysisResult | null
  horizonAggregateRows: HorizonAggregateRow[]
  describeAnalyzerHorizon: (analyzerId: string) => string
}>()
</script>

<template>
  <section v-if="analysisResult" class="card result-card">
    <h2>Aggregated result</h2>
    <p class="selection-line">
      The aggregate below combines analyzers with different horizons. Use the horizon split to compare short-term biopsy decisions vs mid-/long-term estimates.
    </p>
    <div class="result-grid">
      <p>No cancer: {{ analysisResult.aggregate.noCancerRisk }}%</p>
      <p v-if="analysisResult.aggregate.lowGradeRisk !== null">
        Low-grade cancer: {{ analysisResult.aggregate.lowGradeRisk }}%
      </p>
      <p v-if="analysisResult.aggregate.highGradeRisk !== null">
        High-grade cancer: {{ analysisResult.aggregate.highGradeRisk }}%
      </p>
      <p v-if="analysisResult.aggregate.cancerRisk !== null">
        Cancer risk: {{ analysisResult.aggregate.cancerRisk }}%
      </p>
    </div>

    <div v-if="horizonAggregateRows.length" class="result-grid">
      <p v-for="bucket in horizonAggregateRows" :key="bucket.group">
        {{ bucket.group }}: avg cancer risk {{ bucket.avgCancerRisk }}% ({{ bucket.analyzers }} analyzer{{ bucket.analyzers > 1 ? 's' : '' }})
      </p>
    </div>

    <h3>Analyzer details</h3>
    <ul class="detail-list">
      <li v-for="entry in analysisResult.analyzers" :key="entry.analyzerId">
        {{ entry.displayName }} — {{ entry.success ? 'Success' : 'Failed' }}
        <span> | Horizon: {{ describeAnalyzerHorizon(entry.analyzerId) }}</span>
        <span> | Forwarded online: {{ entry.forwardedOnline ? 'Yes' : 'No' }}</span>
        <span v-if="entry.warning"> | {{ entry.warning }}</span>
        <div v-if="entry.risk">
          No cancer: {{ entry.risk.noCancerRisk }}%
          <span v-if="entry.risk.lowGradeRisk !== null"> | Low-grade: {{ entry.risk.lowGradeRisk }}%</span>
          <span v-if="entry.risk.highGradeRisk !== null"> | High-grade: {{ entry.risk.highGradeRisk }}%</span>
          <span v-if="entry.risk.cancerRisk !== null"> | Cancer: {{ entry.risk.cancerRisk }}%</span>
        </div>
      </li>
    </ul>
  </section>
</template>
