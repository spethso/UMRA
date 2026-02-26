<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMutation, useQuery, useLazyQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { GraphiQL } from '@caipira/vue-graphiql'
import AnalyzerSelectionCard from './components/AnalyzerSelectionCard.vue'
import CoreFactorsCard from './components/CoreFactorsCard.vue'
import OptionalDataCard from './components/OptionalDataCard.vue'
import ResultsCard from './components/ResultsCard.vue'
import { createDefaultRiskForm, buildMutationInput } from './composables/useRiskForm'
import { useRiskHorizon } from './composables/useRiskHorizon'
import type { AnalysisResult, AnalyzerSummary, FooterHtml, OptionalToggleKey, RiskForm } from './types/risk'
import './styles/app.css'

type LegalPageKey = 'imprint' | 'privacy' | 'contact'

const currentPath = ref(window.location.pathname.replace(/\/+$/, '') || '/')
const showGraphiql = computed(() => currentPath.value === '/graphiql')
const legalPathToKey: Record<string, LegalPageKey> = {
  '/imprint': 'imprint',
  '/privacy': 'privacy',
  '/contact': 'contact',
}
const legalPageKey = computed<LegalPageKey | null>(() => legalPathToKey[currentPath.value] ?? null)
const showLegalPage = computed(() => legalPageKey.value !== null)
const legalPageTitle = computed<string>(() => {
  switch (legalPageKey.value) {
    case 'imprint':
      return 'Imprint'
    case 'privacy':
      return 'Privacy statement'
    case 'contact':
      return 'Contact details'
    default:
      return ''
  }
})
const legalPageContent = computed(() => (legalPageKey.value ? footerHtml.value[legalPageKey.value] : ''))
const graphiqlEndpoint =
  import.meta.env.VITE_GRAPHQL_ENDPOINT ||
  `${window.location.protocol}//${window.location.hostname}:8080/graphql`

const ANALYZERS_QUERY = gql`
  query Analyzers {
    analyzers {
      analyzerId
      displayName
      sourceUrl
    }
  }
`

const ANALYZE_MUTATION = gql`
  mutation AnalyzeProstateCancerRisk($input: ProstateCancerRiskInput!, $analyzerIds: [String!]) {
    analyzeProstateCancerRisk(input: $input, analyzerIds: $analyzerIds) {
      sessionId
      selectedAnalyzerIds
      result {
        analyzers {
          analyzerId
          displayName
          sourceUrl
          forwardedOnline
          success
          warning
          risk {
            noCancerRisk
            lowGradeRisk
            highGradeRisk
            cancerRisk
            grouped
          }
        }
        aggregate {
          noCancerRisk
          lowGradeRisk
          highGradeRisk
          cancerRisk
          basedOnAnalyzers
        }
      }
    }
  }
`

const SESSION_QUERY = gql`
  query Session($sessionId: String!) {
    session(sessionId: $sessionId) {
      sessionId
      selectedAnalyzerIds
      input {
        race
        age
        psa
        familyHistory
        dre
        priorBiopsy
        detailedFamilyHistoryEnabled
        fdrPcLess60
        fdrPc60
        fdrBc
        sdr
        pctFreePsaAvailable
        pctFreePsa
        pca3Available
        pca3
        t2ergAvailable
        t2erg
        snpsEnabled
        prostateVolumeCc
        mriPiradsScore
        dreVolumeClassCc
        gleasonScoreLegacy
        biopsyCancerLengthMm
        biopsyBenignLengthMm
        ukPostcode
        smokingStatus
        diabetesType
        manicSchizophrenia
        heightCm
        weightKg
        qcancerYears
      }
      result {
        analyzers {
          analyzerId
          displayName
          sourceUrl
          forwardedOnline
          success
          warning
          risk {
            noCancerRisk
            lowGradeRisk
            highGradeRisk
            cancerRisk
            grouped
          }
        }
        aggregate {
          noCancerRisk
          lowGradeRisk
          highGradeRisk
          cancerRisk
          basedOnAnalyzers
        }
      }
      createdAt
    }
  }
`

const { result: analyzersResult, loading: analyzersLoading, error: analyzersError } =
  useQuery<{ analyzers: AnalyzerSummary[] }>(ANALYZERS_QUERY)
const { mutate: analyzeMutate, loading: analyzeLoading, error: analyzeError } = useMutation<
  { analyzeProstateCancerRisk: { sessionId: string; selectedAnalyzerIds: string[]; result: AnalysisResult } },
  { input: Record<string, string | number | boolean | null>; analyzerIds: string[] }
>(ANALYZE_MUTATION)

const analyzers = computed<AnalyzerSummary[]>(() => analyzersResult.value?.analyzers ?? [])
const allAnalyzerIds = computed<string[]>(() => analyzers.value.map((analyzer) => analyzer.analyzerId))
const selectedAnalyzerIds = ref<string[]>([])
const initializedAnalyzerSelection = ref(false)
const analysisResult = ref<AnalysisResult | null>(null)
const sessionId = ref<string | null>(null)
const sessionIdInput = ref('')
const sessionLoadError = ref<string | null>(null)
const sessionLoading = ref(false)
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })
const footerHtml = ref<FooterHtml>({
  imprint: '',
  privacy: '',
  contact: '',
})

const form = ref<RiskForm>(createDefaultRiskForm())

const toggleOptional = (key: OptionalToggleKey): void => {
  form.value[key] = !form.value[key]
}

const isPcptrcSelected = computed(() => selectedAnalyzerIds.value.includes('PCPTRC'))
const isSwopRc5Selected = computed(() => selectedAnalyzerIds.value.includes('SWOP_RC5'))
const isSwopRc6Selected = computed(() => selectedAnalyzerIds.value.includes('SWOP_RC6'))
const isUclaPcrcMriSelected = computed(() => selectedAnalyzerIds.value.includes('UCLA_PCRC_MRI'))
const isQcancerSelected = computed(() => selectedAnalyzerIds.value.includes('QCANCER_10YR_PROSTATE_PSA'))
const showProstateVolumeCc = computed(() => isSwopRc5Selected.value || isUclaPcrcMriSelected.value)
const showOptionalDataSection = computed(
  () =>
    isPcptrcSelected.value ||
    isSwopRc5Selected.value ||
    isSwopRc6Selected.value ||
    isUclaPcrcMriSelected.value ||
    isQcancerSelected.value,
)

const markdownConfigBasePath = `${import.meta.env.BASE_URL}config/`

const loadFooterMarkdown = async (): Promise<void> => {
  const [imprint, privacy, contact] = await Promise.all([
    fetch(`${markdownConfigBasePath}imprint.md`),
    fetch(`${markdownConfigBasePath}privacy.md`),
    fetch(`${markdownConfigBasePath}contact.md`),
  ])

  const [imprintText, privacyText, contactText] = await Promise.all([
    imprint.ok ? imprint.text() : Promise.resolve('Content currently unavailable.'),
    privacy.ok ? privacy.text() : Promise.resolve('Content currently unavailable.'),
    contact.ok ? contact.text() : Promise.resolve('Content currently unavailable.'),
  ])

  footerHtml.value = {
    imprint: markdown.render(imprintText),
    privacy: markdown.render(privacyText),
    contact: markdown.render(contactText),
  }
}

onMounted(() => {
  loadFooterMarkdown()
  window.addEventListener('popstate', updateCurrentPath)
})

onBeforeUnmount(() => {
  window.removeEventListener('popstate', updateCurrentPath)
})

const updateCurrentPath = (): void => {
  currentPath.value = window.location.pathname.replace(/\/+$/, '') || '/'
}

const navigateTo = (path: string): void => {
  if (window.location.pathname !== path) {
    window.history.pushState({}, '', path)
    currentPath.value = path
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

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

const toggleAnalyzerSelection = (analyzerId: string): void => {
  if (selectedAnalyzerIds.value.includes(analyzerId)) {
    selectedAnalyzerIds.value = selectedAnalyzerIds.value.filter((id) => id !== analyzerId)
    return
  }

  selectedAnalyzerIds.value = [...selectedAnalyzerIds.value, analyzerId]
}

const selectAllAnalyzers = (): void => {
  selectedAnalyzerIds.value = [...allAnalyzerIds.value]
}

const selectedAnalyzerCount = computed(() => selectedAnalyzerIds.value.length)
const { describeAnalyzerHorizon, horizonAggregateRows } = useRiskHorizon(analysisResult)

watch(isPcptrcSelected, (selected: boolean) => {
  if (selected) {
    return
  }

  form.value.detailedFamilyHistoryEnabled = false
  form.value.pctFreePsaAvailable = false
  form.value.pca3Available = false
  form.value.t2ergAvailable = false
})

const submitForAnalysis = async (): Promise<void> => {
  const input = buildMutationInput({
    form: form.value,
    flags: {
      showProstateVolumeCc: showProstateVolumeCc.value,
      isUclaPcrcMriSelected: isUclaPcrcMriSelected.value,
      isSwopRc6Selected: isSwopRc6Selected.value,
      isSwopRc5Selected: isSwopRc5Selected.value,
      isQcancerSelected: isQcancerSelected.value,
    },
  })

  const response = await analyzeMutate({
    input,
    analyzerIds: [...selectedAnalyzerIds.value],
  })
  const session = response?.data?.analyzeProstateCancerRisk
  if (session) {
    analysisResult.value = session.result
    sessionId.value = session.sessionId
  }
}

const loadSession = async (): Promise<void> => {
  const id = sessionIdInput.value.trim()
  if (!id) return
  sessionLoadError.value = null
  sessionLoading.value = true
  try {
    const { default: { apolloClient } } = await import('./apolloClient')
    const { data } = await apolloClient.query({
      query: SESSION_QUERY,
      variables: { sessionId: id },
      fetchPolicy: 'network-only',
    })
    const session = data?.session
    if (!session) {
      sessionLoadError.value = 'Session not found.'
      return
    }
    sessionId.value = session.sessionId
    analysisResult.value = session.result

    const loadedInput = session.input
    form.value = {
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
      selectedAnalyzerIds.value = [...session.selectedAnalyzerIds]
    }
  } catch (e: any) {
    sessionLoadError.value = e.message ?? 'Failed to load session.'
  } finally {
    sessionLoading.value = false
  }
}

const copied = ref(false)
let copiedTimeout: ReturnType<typeof setTimeout> | null = null

const copySessionId = async (): Promise<void> => {
  if (sessionId.value) {
    await navigator.clipboard.writeText(sessionId.value)
    copied.value = true
    if (copiedTimeout) clearTimeout(copiedTimeout)
    copiedTimeout = setTimeout(() => { copied.value = false }, 2000)
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
      <h2>Session</h2>
      <div class="session-load-row">
        <input
          v-model="sessionIdInput"
          type="text"
          placeholder="Enter session ID to load previous analysis"
          class="session-input"
        />
        <button :disabled="sessionLoading || !sessionIdInput.trim()" @click="loadSession">
          {{ sessionLoading ? 'Loading...' : 'Load' }}
        </button>
      </div>
      <p v-if="sessionLoadError" class="error-line">{{ sessionLoadError }}</p>
      <div v-if="sessionId" class="session-id-display">
        <span class="session-label">Session ID:</span>
        <code class="session-id-value">{{ sessionId }}</code>
        <button class="copy-btn" :class="{ 'copy-btn--copied': copied }" @click="copySessionId" title="Copy session ID">{{ copied ? 'Copied!' : 'Copy' }}</button>
      </div>
      <p v-if="sessionId" class="hint-line">Save this ID to re-access your inputs and results later.</p>
    </section>

    <AnalyzerSelectionCard
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
      @toggle-optional="toggleOptional"
    />

    <section class="actions-row">
      <button :disabled="analyzeLoading || selectedAnalyzerCount === 0" @click="submitForAnalysis" title="Submit all current inputs for risk analysis.">
        {{ analyzeLoading ? 'Analyzing...' : 'Analyze risk' }}
      </button>
      <p v-if="selectedAnalyzerCount === 0" class="error-line">Select at least one analyzer.</p>
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
