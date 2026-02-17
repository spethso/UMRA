import { computed, type ComputedRef, type Ref } from 'vue'
import type { AnalysisResult, HorizonAggregateRow } from '../types/risk'

const analyzerHorizonMeta: Record<string, string> = {
  PCPTRC: 'Short-term screening / biopsy decision support',
  SWOP_RC2: 'Short-term biopsy risk signal',
  SWOP_RC5: 'Short-term pathology profile (indolent vs aggressive)',
  SWOP_RC6: 'Mid-term estimate (4-year future risk)',
  UCLA_PCRC_MRI: 'Short-term MRI-guided biopsy decision support',
  QCANCER_10YR_PROSTATE_PSA: 'Long-term estimate (1-15 years, typically 10-year)',
}

const analyzerHorizonGroup: Record<string, string> = {
  PCPTRC: 'Short-term / biopsy decision',
  SWOP_RC2: 'Short-term / biopsy decision',
  SWOP_RC5: 'Short-term / biopsy decision',
  UCLA_PCRC_MRI: 'Short-term / biopsy decision',
  SWOP_RC6: 'Mid-term estimate',
  QCANCER_10YR_PROSTATE_PSA: 'Long-term estimate',
}

export function useRiskHorizon(analysisResultRef: Ref<AnalysisResult | null>): {
  describeAnalyzerHorizon: (analyzerId: string) => string
  horizonAggregateRows: ComputedRef<HorizonAggregateRow[]>
} {
  const describeAnalyzerHorizon = (analyzerId: string): string =>
    analyzerHorizonMeta[analyzerId] ?? 'Unspecified horizon'

  const horizonAggregateRows = computed<HorizonAggregateRow[]>(() => {
    const result = analysisResultRef.value
    if (!result?.analyzers?.length) {
      return []
    }

    const buckets = new Map<string, { sum: number; count: number }>()
    result.analyzers
      .filter((entry) => entry.success && entry.risk)
      .forEach((entry) => {
        if (!entry.risk) {
          return
        }
        const group = analyzerHorizonGroup[entry.analyzerId] ?? 'Unspecified horizon'
        const cancerRisk = entry.risk.cancerRisk ?? (100 - entry.risk.noCancerRisk)
        const current = buckets.get(group) ?? { sum: 0, count: 0 }
        buckets.set(group, {
          sum: current.sum + cancerRisk,
          count: current.count + 1,
        })
      })

    return Array.from(buckets.entries()).map(([group, data]) => ({
      group,
      analyzers: data.count,
      avgCancerRisk: Math.round(data.sum / data.count),
    }))
  })

  return {
    describeAnalyzerHorizon,
    horizonAggregateRows,
  }
}