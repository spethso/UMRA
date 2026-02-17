export interface AnalyzerSummary {
  analyzerId: string
  displayName: string
  sourceUrl: string
}

export interface RiskBreakdown {
  noCancerRisk: number
  lowGradeRisk: number | null
  highGradeRisk: number | null
  cancerRisk: number | null
  grouped?: boolean
}

export interface AnalyzerResult {
  analyzerId: string
  displayName: string
  sourceUrl: string
  forwardedOnline: boolean
  success: boolean
  warning: string | null
  risk: RiskBreakdown | null
}

export interface AggregateRisk {
  noCancerRisk: number
  lowGradeRisk: number | null
  highGradeRisk: number | null
  cancerRisk: number | null
  basedOnAnalyzers: number
}

export interface AnalysisResult {
  analyzers: AnalyzerResult[]
  aggregate: AggregateRisk
}

export interface FooterHtml {
  imprint: string
  privacy: string
  contact: string
}

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
  ukPostcode: string
  smokingStatus: string
  diabetesType: string
  manicSchizophrenia: boolean
  heightCm: number | null
  weightKg: number | null
  qcancerYears: number
}

export interface AnalyzerFlags {
  showProstateVolumeCc: boolean
  isUclaPcrcMriSelected: boolean
  isSwopRc6Selected: boolean
  isSwopRc5Selected: boolean
  isQcancerSelected: boolean
}

export type OptionalToggleKey =
  | 'detailedFamilyHistoryEnabled'
  | 'pctFreePsaAvailable'
  | 'pca3Available'
  | 't2ergAvailable'

export interface HorizonAggregateRow {
  group: string
  analyzers: number
  avgCancerRisk: number
}