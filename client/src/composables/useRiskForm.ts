import type { AnalyzerFlags, RiskForm } from '../types/risk'

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

export function buildMutationInput({
  form,
  flags,
}: {
  form: RiskForm
  flags: AnalyzerFlags
}): Record<string, string | number | boolean | null> {
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