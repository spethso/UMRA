<script setup lang="ts">
/**
 * Root application component.
 *
 * Orchestrates navigation, legal pages, session management, the risk-input
 * form, analyzer selection, and analysis submission by delegating to
 * dedicated composables extracted for maintainability.
 */

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { GraphiQL } from '@caipira/vue-graphiql'
import AnalyzerSelectionCard from './components/AnalyzerSelectionCard.vue'
import CoreFactorsCard from './components/CoreFactorsCard.vue'
import OptionalDataCard from './components/OptionalDataCard.vue'
import ResultsCard from './components/ResultsCard.vue'
import { createDefaultRiskForm, buildMutationInput, buildGuidedMutationInput } from './composables/useRiskForm'
import { useRiskHorizon } from './composables/useRiskHorizon'
import { useNavigation } from './composables/useNavigation'
import { useLegalPages } from './composables/useLegalPages'
import { useSessionManager } from './composables/useSessionManager'
import { ANALYZERS_QUERY, ANALYZE_MUTATION } from './graphql/documents'
import type { AnalysisResult, AnalyzerSummary, OptionalToggleKey, RiskForm } from './types/risk'
import './styles/app.css'

/* ── Navigation & Legal Pages ────────────────────────────────── */

const { currentPath, updateCurrentPath, navigateTo, showGraphiql, showLegalPage } = useNavigation()
const { legalPageTitle, legalPageContent, loadFooterMarkdown } = useLegalPages(currentPath)

/* ── GraphiQL endpoint ───────────────────────────────────────── */

const graphiqlEndpoint =
  import.meta.env.VITE_GRAPHQL_ENDPOINT ||
  `${window.location.protocol}//${window.location.hostname}:8080/graphql`

/* ── GraphQL hooks ───────────────────────────────────────────── */

const { result: analyzersResult, loading: analyzersLoading, error: analyzersError } =
  useQuery<{ analyzers: AnalyzerSummary[] }>(ANALYZERS_QUERY)

const { mutate: analyzeMutate, loading: analyzeLoading, error: analyzeError } = useMutation<
  { analyzeProstateCancerRisk: { sessionId: string | null; selectedAnalyzerIds: string[]; stored: boolean; result: AnalysisResult } },
  { input: Record<string, string | number | boolean | null>; analyzerIds: string[] | null; storeResult: boolean }
>(ANALYZE_MUTATION)

/* ── State ───────────────────────────────────────────────────── */

const analyzers = computed<AnalyzerSummary[]>(() => analyzersResult.value?.analyzers ?? [])
const allAnalyzerIds = computed<string[]>(() => analyzers.value.map((a) => a.analyzerId))
const selectedAnalyzerIds = ref<string[]>([])
const initializedAnalyzerSelection = ref(false)
const analysisResult = ref<AnalysisResult | null>(null)
const analysisMode = ref<'manual' | 'guided'>('guided')
const storeConsent = ref(false)
const form = ref<RiskForm>(createDefaultRiskForm())

/* ── Session management ──────────────────────────────────────── */

const {
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
} = useSessionManager({
  analysisResult,
  form,
  analysisMode,
  storeConsent,
  selectedAnalyzerIds,
})

/* ── Form helpers ────────────────────────────────────────────── */

/** Toggle one of the optional-data boolean switches on the form. */
const toggleOptional = (key: OptionalToggleKey): void => {
  form.value[key] = !form.value[key]
}

/* ── Analyzer selection computed ─────────────────────────────── */

const isPcptrcSelected = computed(() => selectedAnalyzerIds.value.includes('PCPTRC'))
const isSwopRc5Selected = computed(() => selectedAnalyzerIds.value.includes('SWOP_RC5'))
const isSwopRc6Selected = computed(() => selectedAnalyzerIds.value.includes('SWOP_RC6'))
const isUclaPcrcMriSelected = computed(() => selectedAnalyzerIds.value.includes('UCLA_PCRC_MRI'))
const isQcancerSelected = computed(() => selectedAnalyzerIds.value.includes('QCANCER_10YR_PROSTATE_PSA'))
const showProstateVolumeCc = computed(() => isSwopRc5Selected.value || isUclaPcrcMriSelected.value)
const isGuidedMode = computed(() => analysisMode.value === 'guided')
const showOptionalDataSection = computed(
  () =>
    isGuidedMode.value ||
    isPcptrcSelected.value ||
    isSwopRc5Selected.value ||
    isSwopRc6Selected.value ||
    isUclaPcrcMriSelected.value ||
    isQcancerSelected.value,
)
const selectedAnalyzerCount = computed(() => selectedAnalyzerIds.value.length)

/* ── Risk horizon helpers ────────────────────────────────────── */

const { describeAnalyzerHorizon, horizonAggregateRows } = useRiskHorizon(analysisResult)

/* ── Lifecycle ───────────────────────────────────────────────── */

onMounted(() => {
  loadFooterMarkdown()
  window.addEventListener('popstate', updateCurrentPath)
})

onBeforeUnmount(() => {
  window.removeEventListener('popstate', updateCurrentPath)
})

/* ── Analyzer selection watchers ─────────────────────────────── */

watch(
  allAnalyzerIds,
  (ids) => {
    if (!ids.length) {
      selectedAnalyzerIds.value = []
      initializedAnalyzerSelection.value = false
      return
    }

    if (!initializedAnalyzerSelection.value) {
      selectedAnalyzerIds.value = [...ids]
      initializedAnalyzerSelection.value = true
      return
    }

    const selectedSet = new Set(selectedAnalyzerIds.value)
    selectedAnalyzerIds.value = ids.filter((id) => selectedSet.has(id))
  },
  { immediate: true },
)

/** Toggle a single analyzer in or out of the selection. */
const toggleAnalyzerSelection = (analyzerId: string): void => {
  if (selectedAnalyzerIds.value.includes(analyzerId)) {
    selectedAnalyzerIds.value = selectedAnalyzerIds.value.filter((id) => id !== analyzerId)
    return
  }
  selectedAnalyzerIds.value = [...selectedAnalyzerIds.value, analyzerId]
}

/** Select every available analyzer. */
const selectAllAnalyzers = (): void => {
  selectedAnalyzerIds.value = [...allAnalyzerIds.value]
}

watch(isPcptrcSelected, (selected: boolean) => {
  if (selected) return
  form.value.detailedFamilyHistoryEnabled = false
  form.value.pctFreePsaAvailable = false
  form.value.pca3Available = false
  form.value.t2ergAvailable = false
})

/* ── Analysis submission ─────────────────────────────────────── */

/**
 * Build the mutation input from the current form and submit it for
 * server-side risk analysis.  Updates `analysisResult` and `sessionId`
 * on success.
 */
const submitForAnalysis = async (): Promise<void> => {
  let input: Record<string, string | number | boolean | null>
  let analyzerIds: string[] | null

  if (isGuidedMode.value) {
    input = buildGuidedMutationInput(form.value)
    analyzerIds = null
  } else {
    input = buildMutationInput({
      form: form.value,
      flags: {
        showProstateVolumeCc: showProstateVolumeCc.value,
        isUclaPcrcMriSelected: isUclaPcrcMriSelected.value,
        isSwopRc6Selected: isSwopRc6Selected.value,
        isSwopRc5Selected: isSwopRc5Selected.value,
        isQcancerSelected: isQcancerSelected.value,
      },
    })
    analyzerIds = [...selectedAnalyzerIds.value]
  }

  const response = await analyzeMutate({ input, analyzerIds, storeResult: storeConsent.value })
  const session = response?.data?.analyzeProstateCancerRisk
  if (session) {
    analysisResult.value = session.result
    sessionId.value = session.stored ? session.sessionId : null
  }
}
</script>

<template>
  <main v-if="showGraphiql" class="graphiql-shell">
    <GraphiQL
      :url="graphiqlEndpoint"
      theme="dark"
      namespace="umra"
    />
  </main>

  <main v-else-if="showLegalPage" class="home legal-view">
    <header class="page-header">
      <h1>{{ legalPageTitle }}</h1>
      <p class="subtitle">Legal information page</p>
    </header>

    <section class="card legal-card">
      <div class="footer-content" v-html="legalPageContent" />
    </section>

    <footer class="site-footer">
      <div class="site-footer-inner">
        <nav class="footer-nav">
          <a href="/" @click.prevent="navigateTo('/')">Back to risk analyzer</a>
        </nav>
      </div>
    </footer>
  </main>

  <main v-else class="home">
    <header class="page-header">
      <h1>Unified Prostate Risk Analyzer</h1>
      <p class="subtitle">Enter clinical factors and optional biomarkers to analyze risk.</p>
    </header>

    <section class="card session-card">
      <h2>Data storage</h2>

      <div class="consent-toggle-row">
        <button
          type="button"
          class="consent-toggle"
          :class="{ active: storeConsent }"
          role="switch"
          :aria-checked="storeConsent"
          @click="storeConsent = !storeConsent"
        >
          <span class="consent-toggle-track">
            <span class="consent-toggle-thumb" />
          </span>
          <span class="consent-toggle-text">{{ storeConsent ? 'Storage enabled' : 'Storage disabled' }}</span>
        </button>
      </div>

      <div v-if="storeConsent" class="consent-notice">
        <p><strong>Consent notice:</strong> By enabling storage you agree that the medical data you enter and the analysis results will be stored on the server and associated with a session ID. You can use this ID to retrieve or delete your data at any time. No personal identification information beyond what you enter is collected.</p>
      </div>

      <div class="session-load-row">
        <input
          v-model="sessionIdInput"
          type="text"
          placeholder="Enter session ID to load or delete"
          class="session-input"
        />
        <button :disabled="sessionLoading || !sessionIdInput.trim()" @click="loadSession">
          {{ sessionLoading ? 'Loading...' : 'Load' }}
        </button>
        <button class="delete-btn" :disabled="deleteLoading || !sessionIdInput.trim()" @click="deleteSessionData">
          {{ deleteLoading ? 'Deleting...' : 'Delete' }}
        </button>
      </div>
      <p v-if="sessionLoadError" class="error-line">{{ sessionLoadError }}</p>
      <p v-if="deleteResult" :class="deleteResult.success ? 'success-line' : 'error-line'">{{ deleteResult.message }}</p>

      <div v-if="sessionId" class="session-id-display">
        <span class="session-label">Session ID:</span>
        <code class="session-id-value">{{ sessionId }}</code>
        <button class="copy-btn" :class="{ 'copy-btn--copied': copied }" @click="copySessionId" title="Copy session ID">{{ copied ? 'Copied!' : 'Copy' }}</button>
      </div>
      <p v-if="sessionId" class="hint-line">Save this ID to re-access your inputs and results later.</p>
    </section>

    <section class="card mode-card">
      <h2>Analysis mode</h2>
      <div class="mode-toggle">
        <button
          type="button"
          class="mode-toggle-btn"
          :class="{ active: analysisMode === 'guided' }"
          @click="analysisMode = 'guided'"
        >
          <span class="mode-toggle-label">Guided analysis</span>
          <span class="mode-toggle-desc">Enter all available data — server selects applicable analyzers</span>
        </button>
        <button
          type="button"
          class="mode-toggle-btn"
          :class="{ active: analysisMode === 'manual' }"
          @click="analysisMode = 'manual'"
        >
          <span class="mode-toggle-label">Manual selection</span>
          <span class="mode-toggle-desc">Choose specific analyzers and provide their required data</span>
        </button>
      </div>
    </section>

    <AnalyzerSelectionCard
      v-if="!isGuidedMode"
      :analyzers-loading="analyzersLoading"
      :analyzers-error-message="analyzersError?.message ?? ''"
      :analyzers="analyzers"
      :selected-analyzer-ids="selectedAnalyzerIds"
      @toggle-analyzer="toggleAnalyzerSelection"
      @select-all="selectAllAnalyzers"
    />

    <CoreFactorsCard :form="form" />

    <OptionalDataCard
      :form="form"
      :is-pcptrc-selected="isPcptrcSelected"
      :is-swop-rc5-selected="isSwopRc5Selected"
      :is-swop-rc6-selected="isSwopRc6Selected"
      :is-ucla-pcrc-mri-selected="isUclaPcrcMriSelected"
      :is-qcancer-selected="isQcancerSelected"
      :show-optional-data-section="showOptionalDataSection"
      :show-prostate-volume-cc="showProstateVolumeCc"
      :guided-mode="isGuidedMode"
      @toggle-optional="toggleOptional"
    />

    <section class="actions-row">
      <button :disabled="analyzeLoading || (!isGuidedMode && selectedAnalyzerCount === 0)" @click="submitForAnalysis" title="Submit all current inputs for risk analysis.">
        {{ analyzeLoading ? 'Analyzing...' : 'Analyze risk' }}
      </button>
      <p v-if="!isGuidedMode && selectedAnalyzerCount === 0" class="error-line">Select at least one analyzer.</p>
      <p v-if="analyzeError" class="error-line">Error analyzing risk: {{ analyzeError.message }}</p>
    </section>

    <ResultsCard
      :analysis-result="analysisResult"
      :session-id="sessionId"
      :horizon-aggregate-rows="horizonAggregateRows"
      :describe-analyzer-horizon="describeAnalyzerHorizon"
    />

    <footer class="site-footer">
      <div class="site-footer-inner">
        <nav class="footer-nav">
          <a href="/imprint" @click.prevent="navigateTo('/imprint')">Imprint</a>
          <a href="/privacy" @click.prevent="navigateTo('/privacy')">Privacy statement</a>
          <a href="/contact" @click.prevent="navigateTo('/contact')">Contact</a>
        </nav>
        <p class="footer-note">UMRA · Munich, Germany</p>
      </div>
    </footer>
  </main>
</template>
