import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useRiskHorizon } from '../useRiskHorizon'
import type { AnalysisResult } from '../../types/risk'

describe('useRiskHorizon', () => {
  describe('describeAnalyzerHorizon', () => {
    it("returns expected text for 'PCPTRC'", () => {
      const resultRef = ref<AnalysisResult | null>(null)
      const { describeAnalyzerHorizon } = useRiskHorizon(resultRef)
      expect(describeAnalyzerHorizon('PCPTRC')).toBe(
        'Short-term screening / biopsy decision support',
      )
    })

    it("returns 'Unspecified horizon' for unknown analyzer", () => {
      const resultRef = ref<AnalysisResult | null>(null)
      const { describeAnalyzerHorizon } = useRiskHorizon(resultRef)
      expect(describeAnalyzerHorizon('UNKNOWN')).toBe('Unspecified horizon')
    })
  })

  describe('horizonAggregateRows', () => {
    it('is empty when result is null', () => {
      const resultRef = ref<AnalysisResult | null>(null)
      const { horizonAggregateRows } = useRiskHorizon(resultRef)
      expect(horizonAggregateRows.value).toEqual([])
    })

    it('groups analyzers correctly by horizon bucket', () => {
      const resultRef = ref<AnalysisResult | null>({
        analyzers: [
          {
            analyzerId: 'PCPTRC',
            displayName: 'PCPTRC',
            sourceUrl: '',
            forwardedOnline: false,
            success: true,
            warning: null,
            risk: { noCancerRisk: 70, lowGradeRisk: 15, highGradeRisk: 15, cancerRisk: 30 },
          },
          {
            analyzerId: 'SWOP_RC2',
            displayName: 'SWOP RC2',
            sourceUrl: '',
            forwardedOnline: false,
            success: true,
            warning: null,
            risk: { noCancerRisk: 80, lowGradeRisk: 10, highGradeRisk: 10, cancerRisk: 20 },
          },
          {
            analyzerId: 'SWOP_RC6',
            displayName: 'SWOP RC6',
            sourceUrl: '',
            forwardedOnline: false,
            success: true,
            warning: null,
            risk: { noCancerRisk: 60, lowGradeRisk: 20, highGradeRisk: 20, cancerRisk: 40 },
          },
        ],
        aggregate: { noCancerRisk: 70, lowGradeRisk: 15, highGradeRisk: 15, cancerRisk: 30, basedOnAnalyzers: 3 },
      })
      const { horizonAggregateRows } = useRiskHorizon(resultRef)
      const rows = horizonAggregateRows.value

      // PCPTRC + SWOP_RC2 → "Short-term / biopsy decision"
      const shortTerm = rows.find((r) => r.group === 'Short-term / biopsy decision')
      expect(shortTerm).toBeDefined()
      expect(shortTerm!.analyzers).toBe(2)

      // SWOP_RC6 → "Mid-term estimate"
      const midTerm = rows.find((r) => r.group === 'Mid-term estimate')
      expect(midTerm).toBeDefined()
      expect(midTerm!.analyzers).toBe(1)
    })

    it('computes correct average cancer risk', () => {
      const resultRef = ref<AnalysisResult | null>({
        analyzers: [
          {
            analyzerId: 'PCPTRC',
            displayName: 'PCPTRC',
            sourceUrl: '',
            forwardedOnline: false,
            success: true,
            warning: null,
            risk: { noCancerRisk: 70, lowGradeRisk: 15, highGradeRisk: 15, cancerRisk: 30 },
          },
          {
            analyzerId: 'SWOP_RC2',
            displayName: 'SWOP RC2',
            sourceUrl: '',
            forwardedOnline: false,
            success: true,
            warning: null,
            risk: { noCancerRisk: 80, lowGradeRisk: 10, highGradeRisk: 10, cancerRisk: 20 },
          },
        ],
        aggregate: { noCancerRisk: 75, lowGradeRisk: 12.5, highGradeRisk: 12.5, cancerRisk: 25, basedOnAnalyzers: 2 },
      })
      const { horizonAggregateRows } = useRiskHorizon(resultRef)
      const rows = horizonAggregateRows.value
      const shortTerm = rows.find((r) => r.group === 'Short-term / biopsy decision')!
      // (30 + 20) / 2 = 25, Math.round(25) = 25
      expect(shortTerm.avgCancerRisk).toBe(25)
    })
  })
})
