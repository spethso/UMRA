/**
 * Helpers for constructing and defaulting the prostate-cancer risk input
 * form used throughout the UMRA client.
 *
 * @module composables/useRiskForm
 */

import type { AnalyzerFlags, RiskForm } from '../types/risk'

/**
 * Create a fresh {@link RiskForm} populated with sensible clinical defaults.
 *
 * @returns A new `RiskForm` object — safe to mutate without side-effects.
 */
export function createDefaultRiskForm(): RiskForm {
  return {
    race: 'CAUCASIAN',
    age: 65,
    psa: 4.2,
    familyHistory: 'NO',
    dre: 'NORMAL',
    priorBiopsy: 'NEVER_HAD_PRIOR_BIOPSY',
    detailedFamilyHistoryEnabled: false,
    fdrPcLess60: 'NO',
    fdrPc60: 'NO',
    fdrBc: 'NO',
    sdr: 'NO',
    pctFreePsaAvailable: false,
    pctFreePsa: null,
    pca3Available: false,
    pca3: null,
    t2ergAvailable: false,
    t2erg: null,
    snpsEnabled: false,
    prostateVolumeCc: 40,
    mriPiradsScore: 3,
    dreVolumeClassCc: 40,
    gleasonScoreLegacy: 6,
    biopsyCancerLengthMm: 10,
    biopsyBenignLengthMm: 40,
    ukPostcode: '',
    smokingStatus: 'NON_SMOKER',
    diabetesType: 'NONE',
    manicSchizophrenia: false,
    heightCm: null,
    weightKg: null,
    qcancerYears: 10,
  }
}

/**
 * Build the GraphQL mutation input for **manual** (analyzer-selection) mode.
 *
 * Only the fields relevant to the currently selected analyzers are included,
 * controlled by the boolean {@link AnalyzerFlags}.
 *
 * @param params       - Object containing the form data and active flags.
 * @param params.form  - Current risk-input form values.
 * @param params.flags - Flags indicating which analyzer-specific sections are active.
 * @returns A flat key/value record ready to be passed as the GraphQL `input` variable.
 * @throws {Error} If `form.age` or `form.psa` is not greater than zero.
 */
export function buildMutationInput({ form, flags }: Readonly<{ form: Readonly<RiskForm>; flags: Readonly<AnalyzerFlags> }>): Record<string, string | number | boolean | null> {
  if (Number(form.age) <= 0) {
    throw new Error('Age must be greater than zero.')
  }
  if (Number(form.psa) <= 0) {
    throw new Error('PSA must be greater than zero.')
  }

  const input: Record<string, string | number | boolean | null> = {
    race: form.race,
    age: Number(form.age),
    psa: Number(form.psa),
    familyHistory: form.familyHistory,
    dre: form.dre,
    priorBiopsy: form.priorBiopsy,
    detailedFamilyHistoryEnabled: form.detailedFamilyHistoryEnabled,
    pctFreePsaAvailable: form.pctFreePsaAvailable,
    pca3Available: form.pca3Available,
    t2ergAvailable: form.t2ergAvailable,
    snpsEnabled: form.snpsEnabled,
  }

  if (form.detailedFamilyHistoryEnabled) {
    input.fdrPcLess60 = form.fdrPcLess60
    input.fdrPc60 = form.fdrPc60
    input.fdrBc = form.fdrBc
    input.sdr = form.sdr
  }

  if (form.pctFreePsaAvailable && form.pctFreePsa !== null) {
    input.pctFreePsa = Number(form.pctFreePsa)
  }

  if (form.pca3Available && form.pca3 !== null) {
    input.pca3 = Number(form.pca3)
  }

  if (form.t2ergAvailable && form.t2erg !== null) {
    input.t2erg = Number(form.t2erg)
  }

  if (flags.showProstateVolumeCc && form.prostateVolumeCc !== null) {
    input.prostateVolumeCc = Number(form.prostateVolumeCc)
  }

  if (flags.isUclaPcrcMriSelected && form.mriPiradsScore !== null) {
    input.mriPiradsScore = Number(form.mriPiradsScore)
  }

  if (flags.isSwopRc6Selected && form.dreVolumeClassCc !== null) {
    input.dreVolumeClassCc = Number(form.dreVolumeClassCc)
  }

  if (flags.isSwopRc5Selected && form.gleasonScoreLegacy !== null) {
    input.gleasonScoreLegacy = Number(form.gleasonScoreLegacy)
  }

  if (flags.isSwopRc5Selected && form.biopsyCancerLengthMm !== null) {
    input.biopsyCancerLengthMm = Number(form.biopsyCancerLengthMm)
  }

  if (flags.isSwopRc5Selected && form.biopsyBenignLengthMm !== null) {
    input.biopsyBenignLengthMm = Number(form.biopsyBenignLengthMm)
  }

  if (flags.isQcancerSelected) {
    input.ukPostcode = form.ukPostcode.trim() || null
    input.smokingStatus = form.smokingStatus
    input.diabetesType = form.diabetesType
    input.manicSchizophrenia = form.manicSchizophrenia
    input.qcancerYears = Number(form.qcancerYears)

    if (form.heightCm !== null) {
      input.heightCm = Number(form.heightCm)
    }
    if (form.weightKg !== null) {
      input.weightKg = Number(form.weightKg)
    }
  }

  return input
}

/**
 * Build the GraphQL mutation input for **guided / auto** mode.
 *
 * Sends all non-null fields so the server can determine which analyzers
 * apply based on the available data.
 *
 * @param form - Current risk-input form values (read-only).
 * @returns A flat key/value record ready to be passed as the GraphQL `input` variable.
 * @throws {Error} If `form.age` or `form.psa` is not greater than zero.
 */
export function buildGuidedMutationInput(form: Readonly<RiskForm>): Record<string, string | number | boolean | null> {
  if (Number(form.age) <= 0) {
    throw new Error('Age must be greater than zero.')
  }
  if (Number(form.psa) <= 0) {
    throw new Error('PSA must be greater than zero.')
  }

  const input: Record<string, string | number | boolean | null> = {
    race: form.race,
    age: Number(form.age),
    psa: Number(form.psa),
    familyHistory: form.familyHistory,
    dre: form.dre,
    priorBiopsy: form.priorBiopsy,
    detailedFamilyHistoryEnabled: form.detailedFamilyHistoryEnabled,
    pctFreePsaAvailable: form.pctFreePsaAvailable,
    pca3Available: form.pca3Available,
    t2ergAvailable: form.t2ergAvailable,
    snpsEnabled: form.snpsEnabled,
  }

  if (form.detailedFamilyHistoryEnabled) {
    input.fdrPcLess60 = form.fdrPcLess60
    input.fdrPc60 = form.fdrPc60
    input.fdrBc = form.fdrBc
    input.sdr = form.sdr
  }

  if (form.pctFreePsaAvailable && form.pctFreePsa !== null) {
    input.pctFreePsa = Number(form.pctFreePsa)
  }

  if (form.pca3Available && form.pca3 !== null) {
    input.pca3 = Number(form.pca3)
  }

  if (form.t2ergAvailable && form.t2erg !== null) {
    input.t2erg = Number(form.t2erg)
  }

  if (form.prostateVolumeCc !== null) {
    input.prostateVolumeCc = Number(form.prostateVolumeCc)
  }

  if (form.mriPiradsScore !== null) {
    input.mriPiradsScore = Number(form.mriPiradsScore)
  }

  if (form.dreVolumeClassCc !== null) {
    input.dreVolumeClassCc = Number(form.dreVolumeClassCc)
  }

  if (form.gleasonScoreLegacy !== null) {
    input.gleasonScoreLegacy = Number(form.gleasonScoreLegacy)
  }

  if (form.biopsyCancerLengthMm !== null) {
    input.biopsyCancerLengthMm = Number(form.biopsyCancerLengthMm)
  }

  if (form.biopsyBenignLengthMm !== null) {
    input.biopsyBenignLengthMm = Number(form.biopsyBenignLengthMm)
  }

  input.ukPostcode = form.ukPostcode.trim() || null
  input.smokingStatus = form.smokingStatus
  input.diabetesType = form.diabetesType
  input.manicSchizophrenia = form.manicSchizophrenia
  input.qcancerYears = Number(form.qcancerYears)

  if (form.heightCm !== null) {
    input.heightCm = Number(form.heightCm)
  }
  if (form.weightKg !== null) {
    input.weightKg = Number(form.weightKg)
  }

  return input
}