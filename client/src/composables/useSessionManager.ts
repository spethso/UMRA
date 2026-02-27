/**
 * Composable that encapsulates all session-related state and operations,
 * including loading a previously stored session, deleting session data,
 * and copying the session ID to the clipboard.
 *
 * @module composables/useSessionManager
 */

import { ref, type Ref } from 'vue'
import { useMutation } from '@vue/apollo-composable'
import { apolloClient } from '../apolloClient'
import { DELETE_SESSION_MUTATION, SESSION_QUERY } from '../graphql/documents'
import type { AnalysisResult, RiskForm } from '../types/risk'
import { logDebug, logInfo, logWarn, logError } from '../utils/logger'

/**
 * Dependencies that the session manager writes to when a stored session is loaded.
 */
export interface UseSessionManagerDeps {
  /** The current analysis result. */
  readonly analysisResult: Ref<AnalysisResult | null>
  /** The risk-input form state. */
  readonly form: Ref<RiskForm>
  /** The current analysis mode. */
  readonly analysisMode: Ref<'manual' | 'guided'>
  /** The user's storage-consent flag. */
  readonly storeConsent: Ref<boolean>
  /** The list of selected analyzer IDs. */
  readonly selectedAnalyzerIds: Ref<string[]>
}

/** Shape returned by {@link useSessionManager}. */
export interface UseSessionManagerReturn {
  /** Server-assigned session ID (set after a stored analysis or a loaded session). */
  readonly sessionId: Ref<string | null>
  /** Two-way bound input field for entering a session ID. */
  readonly sessionIdInput: Ref<string>
  /** Error message shown after a failed session load. */
  readonly sessionLoadError: Ref<string | null>
  /** `true` while a session-load request is in flight. */
  readonly sessionLoading: Ref<boolean>
  /** `true` while a session-delete request is in flight. */
  readonly deleteLoading: Ref<boolean>
  /** Result feedback after a delete attempt. */
  readonly deleteResult: Ref<{ success: boolean; message: string } | null>
  /** Briefly `true` after the session ID has been copied to the clipboard. */
  readonly copied: Ref<boolean>
  /** Load a stored session by the ID currently in {@link sessionIdInput}. */
  readonly loadSession: () => Promise<void>
  /** Delete the session identified by {@link sessionIdInput}. */
  readonly deleteSessionData: () => Promise<void>
  /** Copy the current {@link sessionId} to the system clipboard. */
  readonly copySessionId: () => Promise<void>
}

const CTX = 'SessionManager'

/**
 * Creates session-management state and actions.
 *
 * @param deps - Application state that is updated when a session is
 *               loaded (form data, analysis results, mode, etc.).
 * @returns State and async action functions for session management.
 */
export function useSessionManager(deps: Readonly<UseSessionManagerDeps>): UseSessionManagerReturn {
  const { mutate: deleteMutate } = useMutation<
    { deleteSession: boolean },
    { sessionId: string }
  >(DELETE_SESSION_MUTATION)

  /* ── own reactive state ─────────────────────────────────────── */
  const sessionId = ref<string | null>(null)
  const sessionIdInput = ref('')
  const sessionLoadError = ref<string | null>(null)
  const sessionLoading = ref(false)
  const deleteLoading = ref(false)
  const deleteResult = ref<{ success: boolean; message: string } | null>(null)
  const copied = ref(false)
  let copiedTimeout: ReturnType<typeof setTimeout> | null = null

  /* ── actions ────────────────────────────────────────────────── */

  /**
   * Load a previously stored session by its ID, restoring all form
   * inputs, analysis results, and analyzer selections.
   *
   * Validates that the session ID input is not empty before issuing
   * the network request.
   */
  const loadSession = async (): Promise<void> => {
    const id = sessionIdInput.value.trim()
    if (!id) {
      logWarn(CTX, 'loadSession called with empty session ID')
      sessionLoadError.value = 'Session ID must not be empty.'
      return
    }

    logDebug(CTX, 'Loading session', { sessionId: id })
    sessionLoadError.value = null
    sessionLoading.value = true

    try {
      const { data } = await apolloClient.query({
        query: SESSION_QUERY,
        variables: { sessionId: id },
        fetchPolicy: 'network-only',
      })
      const session = data?.session
      if (!session) {
        logWarn(CTX, 'Session not found', { sessionId: id })
        sessionLoadError.value = 'Session not found.'
        return
      }

      logInfo(CTX, 'Session loaded successfully', { sessionId: id })
      sessionId.value = session.sessionId
      deps.analysisResult.value = session.result
      deps.analysisMode.value = session.autoMode ? 'guided' : 'manual'
      deps.storeConsent.value = true

      const loadedInput = session.input
      deps.form.value = {
        race: loadedInput.race ?? 'CAUCASIAN',
        age: loadedInput.age ?? 65,
        psa: loadedInput.psa ?? 4.2,
        familyHistory: loadedInput.familyHistory ?? 'NO',
        dre: loadedInput.dre ?? 'NORMAL',
        priorBiopsy: loadedInput.priorBiopsy ?? 'NEVER_HAD_PRIOR_BIOPSY',
        detailedFamilyHistoryEnabled: loadedInput.detailedFamilyHistoryEnabled ?? false,
        fdrPcLess60: loadedInput.fdrPcLess60 ?? 'NO',
        fdrPc60: loadedInput.fdrPc60 ?? 'NO',
        fdrBc: loadedInput.fdrBc ?? 'NO',
        sdr: loadedInput.sdr ?? 'NO',
        pctFreePsaAvailable: loadedInput.pctFreePsaAvailable ?? false,
        pctFreePsa: loadedInput.pctFreePsa ?? null,
        pca3Available: loadedInput.pca3Available ?? false,
        pca3: loadedInput.pca3 ?? null,
        t2ergAvailable: loadedInput.t2ergAvailable ?? false,
        t2erg: loadedInput.t2erg ?? null,
        snpsEnabled: loadedInput.snpsEnabled ?? false,
        prostateVolumeCc: loadedInput.prostateVolumeCc ?? 40,
        mriPiradsScore: loadedInput.mriPiradsScore ?? 3,
        dreVolumeClassCc: loadedInput.dreVolumeClassCc ?? 40,
        gleasonScoreLegacy: loadedInput.gleasonScoreLegacy ?? 6,
        biopsyCancerLengthMm: loadedInput.biopsyCancerLengthMm ?? 10,
        biopsyBenignLengthMm: loadedInput.biopsyBenignLengthMm ?? 40,
        ukPostcode: loadedInput.ukPostcode ?? '',
        smokingStatus: loadedInput.smokingStatus ?? 'NON_SMOKER',
        diabetesType: loadedInput.diabetesType ?? 'NONE',
        manicSchizophrenia: loadedInput.manicSchizophrenia ?? false,
        heightCm: loadedInput.heightCm ?? null,
        weightKg: loadedInput.weightKg ?? null,
        qcancerYears: loadedInput.qcancerYears ?? 10,
      }

      if (session.selectedAnalyzerIds?.length) {
        deps.selectedAnalyzerIds.value = [...session.selectedAnalyzerIds]
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Failed to load session.'
      logError(CTX, 'Failed to load session', { sessionId: id, error: message })
      sessionLoadError.value = message
    } finally {
      sessionLoading.value = false
    }
  }

  /**
   * Delete the session identified by the current session-ID input.
   *
   * Validates that the session ID input is not empty before issuing
   * the network request.
   */
  const deleteSessionData = async (): Promise<void> => {
    const id = sessionIdInput.value.trim()
    if (!id) {
      logWarn(CTX, 'deleteSessionData called with empty session ID')
      deleteResult.value = { success: false, message: 'Session ID must not be empty.' }
      return
    }

    logDebug(CTX, 'Deleting session', { sessionId: id })
    deleteLoading.value = true
    deleteResult.value = null

    try {
      const response = await deleteMutate({ sessionId: id })
      const deleted = response?.data?.deleteSession
      if (deleted) {
        logInfo(CTX, 'Session deleted successfully', { sessionId: id })
        deleteResult.value = { success: true, message: 'Session data deleted successfully.' }
        if (sessionId.value === id) {
          sessionId.value = null
        }
      } else {
        logWarn(CTX, 'Session not found for deletion', { sessionId: id })
        deleteResult.value = { success: false, message: 'Session not found.' }
      }
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Failed to delete session.'
      logError(CTX, 'Failed to delete session', { sessionId: id, error: message })
      deleteResult.value = { success: false, message }
    } finally {
      deleteLoading.value = false
    }
  }

  /**
   * Copy the current session ID to the system clipboard and briefly
   * set the {@link copied} flag so the UI can show feedback.
   */
  const copySessionId = async (): Promise<void> => {
    if (sessionId.value) {
      await navigator.clipboard.writeText(sessionId.value)
      logDebug(CTX, 'Session ID copied to clipboard')
      copied.value = true
      if (copiedTimeout) clearTimeout(copiedTimeout)
      copiedTimeout = setTimeout(() => {
        copied.value = false
      }, 2000)
    }
  }

  return {
    sessionId,
    sessionIdInput,
    sessionLoadError,
    sessionLoading,
    deleteLoading,
    deleteResult,
    copied,
    loadSession,
    deleteSessionData,
    copySessionId,
  }
}
