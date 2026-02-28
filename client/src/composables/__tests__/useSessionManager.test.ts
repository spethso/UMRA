import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * useSessionManager requires Apollo's useMutation (which needs a component
 * setup context and an Apollo provider). We test only the simpler behaviours
 * by mocking the Apollo dependency.
 */

// Mock @vue/apollo-composable so we don't need a real provider
vi.mock('@vue/apollo-composable', () => ({
  useMutation: () => ({ mutate: vi.fn() }),
}))

// Mock apolloClient so the SESSION_QUERY import doesn't fail at runtime
vi.mock('../../apolloClient', () => ({
  apolloClient: { query: vi.fn() },
}))

import { ref } from 'vue'
import { useSessionManager } from '../useSessionManager'
import type { UseSessionManagerDeps } from '../useSessionManager'

function makeDeps(): UseSessionManagerDeps {
  return {
    analysisResult: ref(null),
    form: ref({
      race: 'CAUCASIAN', age: 65, psa: 4.2, familyHistory: 'NO', dre: 'NORMAL',
      priorBiopsy: 'NEVER_HAD_PRIOR_BIOPSY', detailedFamilyHistoryEnabled: false,
      fdrPcLess60: 'NO', fdrPc60: 'NO', fdrBc: 'NO', sdr: 'NO',
      pctFreePsaAvailable: false, pctFreePsa: null, pca3Available: false, pca3: null,
      t2ergAvailable: false, t2erg: null, snpsEnabled: false, prostateVolumeCc: 40,
      mriPiradsScore: 3, dreVolumeClassCc: 40, gleasonScoreLegacy: 6,
      biopsyCancerLengthMm: 10, biopsyBenignLengthMm: 40,
      smokingStatus: 'NON_SMOKER', diabetesType: 'NONE', manicSchizophrenia: false,
      heightCm: null, weightKg: null, qcancerYears: 10,
    }),
    analysisMode: ref<'manual' | 'guided'>('manual'),
    storeConsent: ref(false),
    selectedAnalyzerIds: ref<string[]>([]),
  }
}

describe('useSessionManager', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('sessionId starts as null', () => {
    const { sessionId } = useSessionManager(makeDeps())
    expect(sessionId.value).toBeNull()
  })

  it('deleteSessionData sets error when input is empty', async () => {
    const sm = useSessionManager(makeDeps())
    sm.sessionIdInput.value = ''
    await sm.deleteSessionData()
    expect(sm.deleteResult.value).toEqual({
      success: false,
      message: 'Session ID must not be empty.',
    })
  })

  it('copySessionId calls navigator.clipboard.writeText', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })

    const sm = useSessionManager(makeDeps())
    // Set a session id so the copy branch executes
    sm.sessionId.value = 'test-session-123'
    await sm.copySessionId()
    expect(writeText).toHaveBeenCalledWith('test-session-123')
  })

  it('sessionLoading starts as false', () => {
    const { sessionLoading } = useSessionManager(makeDeps())
    expect(sessionLoading.value).toBe(false)
  })

  it('loadSession sets error when input is empty', async () => {
    const sm = useSessionManager(makeDeps())
    sm.sessionIdInput.value = '   '
    await sm.loadSession()
    expect(sm.sessionLoadError.value).toBe('Session ID must not be empty.')
  })
})
