import { describe, it, expect } from 'vitest'
import {
  createDefaultRiskForm,
  buildMutationInput,
  buildGuidedMutationInput,
} from '../useRiskForm'
import type { AnalyzerFlags, RiskForm } from '../../types/risk'

/** Returns a minimal all-false flags object. */
function noFlags(): AnalyzerFlags {
  return {
    showProstateVolumeCc: false,
    isUclaPcrcMriSelected: false,
    isSwopRc6Selected: false,
    isSwopRc5Selected: false,
    isQcancerSelected: false,
  }
}

describe('createDefaultRiskForm', () => {
  it('returns object with expected defaults', () => {
    const form = createDefaultRiskForm()
    expect(form.age).toBe(65)
    expect(form.psa).toBe(4.2)
    expect(form.race).toBe('CAUCASIAN')
    expect(form.familyHistory).toBe('NO')
    expect(form.dre).toBe('NORMAL')
    expect(form.priorBiopsy).toBe('NEVER_HAD_PRIOR_BIOPSY')
    expect(form.detailedFamilyHistoryEnabled).toBe(false)
    expect(form.pctFreePsaAvailable).toBe(false)
    expect(form.pctFreePsa).toBeNull()
    expect(form.prostateVolumeCc).toBe(40)
  })

  it('returns a new object each call (not same reference)', () => {
    const a = createDefaultRiskForm()
    const b = createDefaultRiskForm()
    expect(a).not.toBe(b)
    expect(a).toEqual(b)
  })
})

describe('buildMutationInput', () => {
  it('returns required core fields for a basic form', () => {
    const form = createDefaultRiskForm()
    const result = buildMutationInput({ form, flags: noFlags() })
    expect(result.race).toBe('CAUCASIAN')
    expect(result.age).toBe(65)
    expect(result.psa).toBe(4.2)
    expect(result.familyHistory).toBe('NO')
    expect(result.dre).toBe('NORMAL')
    expect(result.priorBiopsy).toBe('NEVER_HAD_PRIOR_BIOPSY')
    expect(result.detailedFamilyHistoryEnabled).toBe(false)
    expect(result.pctFreePsaAvailable).toBe(false)
  })

  it('includes fdr fields when detailedFamilyHistoryEnabled is true', () => {
    const form = createDefaultRiskForm()
    form.detailedFamilyHistoryEnabled = true
    form.fdrPcLess60 = 'YES'
    const result = buildMutationInput({ form, flags: noFlags() })
    expect(result.fdrPcLess60).toBe('YES')
    expect(result.fdrPc60).toBe('NO')
    expect(result.fdrBc).toBe('NO')
    expect(result.sdr).toBe('NO')
  })

  it('includes pctFreePsa when pctFreePsaAvailable is true', () => {
    const form = createDefaultRiskForm()
    form.pctFreePsaAvailable = true
    form.pctFreePsa = 15.5
    const result = buildMutationInput({ form, flags: noFlags() })
    expect(result.pctFreePsa).toBe(15.5)
  })

  it('includes prostateVolumeCc only when showProstateVolumeCc flag is true', () => {
    const form = createDefaultRiskForm()
    const flagsOff = noFlags()
    expect(buildMutationInput({ form, flags: flagsOff })).not.toHaveProperty('prostateVolumeCc')

    const flagsOn: AnalyzerFlags = { ...noFlags(), showProstateVolumeCc: true }
    const result = buildMutationInput({ form, flags: flagsOn })
    expect(result.prostateVolumeCc).toBe(40)
  })

  it('throws Error when age <= 0', () => {
    const form = createDefaultRiskForm()
    form.age = 0
    expect(() => buildMutationInput({ form, flags: noFlags() })).toThrow('Age must be greater than zero.')
  })

  it('throws Error when psa <= 0', () => {
    const form = createDefaultRiskForm()
    form.psa = -1
    expect(() => buildMutationInput({ form, flags: noFlags() })).toThrow('PSA must be greater than zero.')
  })
})

describe('buildGuidedMutationInput', () => {
  it('always includes all numeric optional fields when non-null', () => {
    const form = createDefaultRiskForm()
    form.prostateVolumeCc = 50
    form.mriPiradsScore = 4
    form.dreVolumeClassCc = 30
    const result = buildGuidedMutationInput(form)
    expect(result.prostateVolumeCc).toBe(50)
    expect(result.mriPiradsScore).toBe(4)
    expect(result.dreVolumeClassCc).toBe(30)
    expect(result.smokingStatus).toBe('NON_SMOKER')
    expect(result.diabetesType).toBe('NONE')
    expect(result.qcancerYears).toBe(10)
  })

  it('throws Error when age <= 0', () => {
    const form = createDefaultRiskForm()
    form.age = -5
    expect(() => buildGuidedMutationInput(form)).toThrow('Age must be greater than zero.')
  })

  it('throws Error when psa <= 0', () => {
    const form = createDefaultRiskForm()
    form.psa = 0
    expect(() => buildGuidedMutationInput(form)).toThrow('PSA must be greater than zero.')
  })
})
