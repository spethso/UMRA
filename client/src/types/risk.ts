/**
 * Summary metadata for a single risk analyzer as returned by the
 * `analyzers` GraphQL query.
 */
export interface AnalyzerSummary {
  /** Unique machine-readable identifier (e.g. `'PCPTRC'`). */
  readonly analyzerId: string
  /** Human-friendly display name. */
  readonly displayName: string
  /** URL pointing to the analyzer's scientific source or documentation. */
  readonly sourceUrl: string
}

/**
 * Breakdown of predicted risk percentages returned by an individual
 * analyzer.
 */
export interface RiskBreakdown {
  /** Probability (%) of no cancer. */
  readonly noCancerRisk: number
  /** Probability (%) of low-grade cancer, or `null` if not applicable. */
  readonly lowGradeRisk: number | null
  /** Probability (%) of high-grade cancer, or `null` if not applicable. */
  readonly highGradeRisk: number | null
  /** Overall probability (%) of cancer, or `null` if not applicable. */
  readonly cancerRisk: number | null
  /** Whether the risk values represent grouped (combined) categories. */
  readonly grouped?: boolean
}

/**
 * Full result produced by a single analyzer, including metadata and the
 * optional risk breakdown.
 */
export interface AnalyzerResult {
  readonly analyzerId: string
  readonly displayName: string
  readonly sourceUrl: string
  /** `true` when the calculation was forwarded to an online service. */
  readonly forwardedOnline: boolean
  /** `true` when the analyzer completed without error. */
  readonly success: boolean
  /** Optional warning message from the analyzer. */
  readonly warning: string | null
  /** Risk breakdown, present only when the analyzer succeeded. */
  readonly risk: RiskBreakdown | null
}

/**
 * Aggregate risk values computed across all successful analyzers.
 */
export interface AggregateRisk {
  readonly noCancerRisk: number
  readonly lowGradeRisk: number | null
  readonly highGradeRisk: number | null
  readonly cancerRisk: number | null
  /** Number of analyzers that contributed to this aggregate. */
  readonly basedOnAnalyzers: number
}

/**
 * Combined analysis result containing per-analyzer results and the
 * aggregate summary.
 */
export interface AnalysisResult {
  readonly analyzers: AnalyzerResult[]
  readonly aggregate: AggregateRisk
}

/**
 * Pre-rendered HTML strings for the three legal / informational footer pages.
 */
export interface FooterHtml {
  readonly imprint: string
  readonly privacy: string
  readonly contact: string
}

/**
 * Shape of the reactive risk-input form used by the client.
 *
 * All fields correspond 1 : 1 to the `ProstateCancerRiskInput` GraphQL type.
 */
export interface RiskForm {
  race: string
  age: number
  psa: number
  familyHistory: string
  dre: string
  priorBiopsy: string
  detailedFamilyHistoryEnabled: boolean
  fdrPcLess60: string
  fdrPc60: string
  fdrBc: string
  sdr: string
  pctFreePsaAvailable: boolean
  pctFreePsa: number | null
  pca3Available: boolean
  pca3: number | null
  t2ergAvailable: boolean
  t2erg: number | null
  snpsEnabled: boolean
  prostateVolumeCc: number
  mriPiradsScore: number
  dreVolumeClassCc: number
  gleasonScoreLegacy: number
  biopsyCancerLengthMm: number
  biopsyBenignLengthMm: number
  smokingStatus: string
  diabetesType: string
  manicSchizophrenia: boolean
  heightCm: number | null
  weightKg: number | null
  qcancerYears: number
}

/**
 * Boolean flags indicating which analyzer-specific UI sections are
 * currently active. Used by {@link buildMutationInput} to decide which
 * optional fields to include.
 */
export interface AnalyzerFlags {
  readonly showProstateVolumeCc: boolean
  readonly isUclaPcrcMriSelected: boolean
  readonly isSwopRc6Selected: boolean
  readonly isSwopRc5Selected: boolean
  readonly isQcancerSelected: boolean
}

/**
 * Union of `RiskForm` keys that represent togglable optional-data sections.
 */
export type OptionalToggleKey =
  | 'detailedFamilyHistoryEnabled'
  | 'pctFreePsaAvailable'
  | 'pca3Available'
  | 't2ergAvailable'

/**
 * A single row in the risk-horizon aggregate table shown in the results
 * card.
 */
export interface HorizonAggregateRow {
  /** Horizon group label (e.g. `'Short-term / biopsy decision'`). */
  readonly group: string
  /** Number of analyzers that contributed to this group. */
  readonly analyzers: number
  /** Average cancer-risk percentage across the contributing analyzers. */
  readonly avgCancerRisk: number
}