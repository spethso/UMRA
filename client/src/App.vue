<script setup>
import MarkdownIt from 'markdown-it'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { GraphiQL } from '@caipira/vue-graphiql'

const currentPath = ref(window.location.pathname.replace(/\/+$/, '') || '/')
const showGraphiql = computed(() => currentPath.value === '/graphiql')
const legalPathToKey = {
  '/imprint': 'imprint',
  '/privacy': 'privacy',
  '/contact': 'contact',
}
const legalPageKey = computed(() => legalPathToKey[currentPath.value] ?? null)
const showLegalPage = computed(() => legalPageKey.value !== null)
const legalPageTitle = computed(() => {
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
`

const { result: analyzersResult, loading: analyzersLoading, error: analyzersError } = useQuery(ANALYZERS_QUERY)
const { mutate: analyzeMutate, loading: analyzeLoading, error: analyzeError } = useMutation(ANALYZE_MUTATION)

const analyzers = computed(() => analyzersResult.value?.analyzers ?? [])
const allAnalyzerIds = computed(() => analyzers.value.map((analyzer) => analyzer.analyzerId))
const selectedAnalyzerIds = ref([])
const initializedAnalyzerSelection = ref(false)
const analysisResult = ref(null)
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })
const footerHtml = ref({
  imprint: '',
  privacy: '',
  contact: '',
})

const form = ref({
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
})

const toggleOptional = (key) => {
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

const loadFooterMarkdown = async () => {
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

const updateCurrentPath = () => {
  currentPath.value = window.location.pathname.replace(/\/+$/, '') || '/'
}

const navigateTo = (path) => {
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

const isAnalyzerSelected = (analyzerId) => selectedAnalyzerIds.value.includes(analyzerId)

const toggleAnalyzerSelection = (analyzerId) => {
  if (isAnalyzerSelected(analyzerId)) {
    selectedAnalyzerIds.value = selectedAnalyzerIds.value.filter((id) => id !== analyzerId)
    return
  }

  selectedAnalyzerIds.value = [...selectedAnalyzerIds.value, analyzerId]
}

const selectAllAnalyzers = () => {
  selectedAnalyzerIds.value = [...allAnalyzerIds.value]
}

const selectedAnalyzerCount = computed(() => selectedAnalyzerIds.value.length)

const analyzerHorizonMeta = {
  PCPTRC: 'Short-term screening / biopsy decision support',
  SWOP_RC2: 'Short-term biopsy risk signal',
  SWOP_RC5: 'Short-term pathology profile (indolent vs aggressive)',
  SWOP_RC6: 'Mid-term estimate (4-year future risk)',
  UCLA_PCRC_MRI: 'Short-term MRI-guided biopsy decision support',
  QCANCER_10YR_PROSTATE_PSA: 'Long-term estimate (1-15 years, typically 10-year)',
}

const analyzerHorizonGroup = {
  PCPTRC: 'Short-term / biopsy decision',
  SWOP_RC2: 'Short-term / biopsy decision',
  SWOP_RC5: 'Short-term / biopsy decision',
  UCLA_PCRC_MRI: 'Short-term / biopsy decision',
  SWOP_RC6: 'Mid-term estimate',
  QCANCER_10YR_PROSTATE_PSA: 'Long-term estimate',
}

const describeAnalyzerHorizon = (analyzerId) => analyzerHorizonMeta[analyzerId] ?? 'Unspecified horizon'

const horizonAggregateRows = computed(() => {
  const result = analysisResult.value
  if (!result?.analyzers?.length) {
    return []
  }

  const buckets = new Map()
  result.analyzers
    .filter((entry) => entry.success && entry.risk)
    .forEach((entry) => {
      const group = analyzerHorizonGroup[entry.analyzerId] ?? 'Unspecified horizon'
      const cancerRisk = entry.risk.cancerRisk ?? (100 - entry.risk.noCancerRisk)
      const current = buckets.get(group) ?? { sum: 0, count: 0 }
      buckets.set(group, {
        sum: current.sum + cancerRisk,
        count: current.count + 1,
      })
    })

  return Array.from(buckets.entries()).map(([group, data]) => ({
    group,
    analyzers: data.count,
    avgCancerRisk: Math.round(data.sum / data.count),
  }))
})

watch(isPcptrcSelected, (selected) => {
  if (selected) {
    return
  }

  form.value.detailedFamilyHistoryEnabled = false
  form.value.pctFreePsaAvailable = false
  form.value.pca3Available = false
  form.value.t2ergAvailable = false
})

const submitForAnalysis = async () => {
  const input = {
    race: form.value.race,
    age: Number(form.value.age),
    psa: Number(form.value.psa),
    familyHistory: form.value.familyHistory,
    dre: form.value.dre,
    priorBiopsy: form.value.priorBiopsy,
    detailedFamilyHistoryEnabled: form.value.detailedFamilyHistoryEnabled,
    pctFreePsaAvailable: form.value.pctFreePsaAvailable,
    pca3Available: form.value.pca3Available,
    t2ergAvailable: form.value.t2ergAvailable,
    snpsEnabled: form.value.snpsEnabled,
  }

  if (form.value.detailedFamilyHistoryEnabled) {
    input.fdrPcLess60 = form.value.fdrPcLess60
    input.fdrPc60 = form.value.fdrPc60
    input.fdrBc = form.value.fdrBc
    input.sdr = form.value.sdr
  }

  if (form.value.pctFreePsaAvailable && form.value.pctFreePsa !== null) {
    input.pctFreePsa = Number(form.value.pctFreePsa)
  }

  if (form.value.pca3Available && form.value.pca3 !== null) {
    input.pca3 = Number(form.value.pca3)
  }

  if (form.value.t2ergAvailable && form.value.t2erg !== null) {
    input.t2erg = Number(form.value.t2erg)
  }

  if (showProstateVolumeCc.value && form.value.prostateVolumeCc !== null) {
    input.prostateVolumeCc = Number(form.value.prostateVolumeCc)
  }

  if (isUclaPcrcMriSelected.value && form.value.mriPiradsScore !== null) {
    input.mriPiradsScore = Number(form.value.mriPiradsScore)
  }

  if (isSwopRc6Selected.value && form.value.dreVolumeClassCc !== null) {
    input.dreVolumeClassCc = Number(form.value.dreVolumeClassCc)
  }

  if (isSwopRc5Selected.value && form.value.gleasonScoreLegacy !== null) {
    input.gleasonScoreLegacy = Number(form.value.gleasonScoreLegacy)
  }

  if (isSwopRc5Selected.value && form.value.biopsyCancerLengthMm !== null) {
    input.biopsyCancerLengthMm = Number(form.value.biopsyCancerLengthMm)
  }

  if (isSwopRc5Selected.value && form.value.biopsyBenignLengthMm !== null) {
    input.biopsyBenignLengthMm = Number(form.value.biopsyBenignLengthMm)
  }

  if (isQcancerSelected.value) {
    input.ukPostcode = form.value.ukPostcode?.trim() || null
    input.smokingStatus = form.value.smokingStatus
    input.diabetesType = form.value.diabetesType
    input.manicSchizophrenia = form.value.manicSchizophrenia
    input.qcancerYears = Number(form.value.qcancerYears)

    if (form.value.heightCm !== null) {
      input.heightCm = Number(form.value.heightCm)
    }
    if (form.value.weightKg !== null) {
      input.weightKg = Number(form.value.weightKg)
    }
  }

  const response = await analyzeMutate({
    input,
    analyzerIds: [...selectedAnalyzerIds.value],
  })
  analysisResult.value = response?.data?.analyzeProstateCancerRisk ?? null
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
      <div class="footer-content" v-html="footerHtml[legalPageKey]" />
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

    <section class="card">
      <h2>Analyzer selection</h2>
      <p v-if="analyzersLoading">Loading analyzers...</p>
      <p v-else-if="analyzersError" class="error-line">Error loading analyzers: {{ analyzersError.message }}</p>
      <template v-else>
        <p class="selection-line">Selected: {{ selectedAnalyzerCount }} / {{ analyzers.length }}</p>
        <div class="selection-actions">
          <button type="button" class="ghost-button" @click="selectAllAnalyzers">Select all</button>
        </div>
        <ul class="analyzer-list">
          <li v-for="analyzer in analyzers" :key="analyzer.analyzerId" class="analyzer-item">
            <button
              type="button"
              class="analyzer-pill"
              :class="{ selected: isAnalyzerSelected(analyzer.analyzerId) }"
              :aria-pressed="isAnalyzerSelected(analyzer.analyzerId)"
              @click="toggleAnalyzerSelection(analyzer.analyzerId)"
            >
              {{ analyzer.displayName }} ({{ analyzer.analyzerId }})
            </button>
          </li>
        </ul>
      </template>
    </section>

    <section class="card">
      <h2>Core factors</h2>
      <div class="form-grid">
        <label>
          Race / Ethnicity
          <select v-model="form.race" title="Patient race/ethnicity category used by selected analyzers.">
            <option value="AFRICAN_AMERICAN">African American</option>
            <option value="ASIAN">Asian</option>
            <option value="CAUCASIAN">Caucasian</option>
            <option value="HISPANIC_LATINO">Hispanic / Latino</option>
            <option value="MIDDLE_EASTERN_NORTH_AFRICAN">Middle Eastern / North African</option>
            <option value="NATIVE_AMERICAN_OR_ALASKA_NATIVE">Native American / Alaska Native</option>
            <option value="NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER">Native Hawaiian / Pacific Islander</option>
            <option value="OTHER">Other</option>
            <option value="UNKNOWN">Unknown / Prefer not to say</option>
          </select>
        </label>

        <label>
          Age
          <input v-model.number="form.age" type="number" min="25" max="90" title="Patient age in years. Different analyzers have different valid age ranges." />
        </label>

        <label>
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              PSA (ng/ml)
              <span class="tooltip-popover">
                Prostate-specific antigen (PSA) is a blood marker from the prostate. Higher values can indicate inflammation, enlargement, or possible cancer risk.
              </span>
            </span>
          </span>
          <input v-model.number="form.psa" type="number" min="0.1" max="50" step="0.1" title="Prostate-specific antigen value, greater than 0 and up to 50." />
        </label>

        <label>
          Family history
          <select v-model="form.familyHistory" title="Whether first-degree relatives had prostate cancer.">
            <option value="YES">Yes</option>
            <option value="NO">No</option>
            <option value="DO_NOT_KNOW">Do not know</option>
          </select>
        </label>

        <label>
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              DRE
              <span class="tooltip-popover">
                Digital Rectal Examination (DRE) is a physical exam to assess the prostate for suspicious firmness or nodules.
              </span>
            </span>
          </span>
          <select v-model="form.dre" title="Digital rectal exam status.">
            <option value="ABNORMAL">Abnormal</option>
            <option value="NORMAL">Normal</option>
            <option value="NOT_PERFORMED_OR_NOT_SURE">Not performed or not sure</option>
          </select>
        </label>

        <label>
          Prior biopsy
          <select v-model="form.priorBiopsy" title="History of previous prostate biopsy.">
            <option value="NEVER_HAD_PRIOR_BIOPSY">Never had a prior biopsy</option>
            <option value="PRIOR_NEGATIVE_BIOPSY">Prior negative biopsy</option>
            <option value="NOT_SURE">Not sure</option>
          </select>
        </label>
      </div>
    </section>

    <section v-if="showOptionalDataSection" class="card">
      <h2>Optional data</h2>
      <div v-if="isPcptrcSelected" class="optional-card-grid">
        <button
          type="button"
          class="option-card"
          :class="{ selected: form.detailedFamilyHistoryEnabled }"
          :aria-pressed="form.detailedFamilyHistoryEnabled"
          data-tooltip="Enable detailed family history inputs for caucasian profile path."
          @click="toggleOptional('detailedFamilyHistoryEnabled')"
        >
          <span class="option-title">Detailed family history</span>
          <span class="option-state">{{ form.detailedFamilyHistoryEnabled ? 'Enabled' : 'Disabled' }}</span>
        </button>

        <button
          type="button"
          class="option-card"
          :class="{ selected: form.pctFreePsaAvailable }"
          :aria-pressed="form.pctFreePsaAvailable"
          data-tooltip="Enable percent free PSA field (5 to 75)."
          @click="toggleOptional('pctFreePsaAvailable')"
        >
          <span class="option-title">Percent free PSA available</span>
          <span class="option-state">{{ form.pctFreePsaAvailable ? 'Enabled' : 'Disabled' }}</span>
        </button>

        <button
          type="button"
          class="option-card"
          :class="{ selected: form.pca3Available }"
          :aria-pressed="form.pca3Available"
          data-tooltip="Enable PCA3 biomarker field (0.3 to 332.5)."
          @click="toggleOptional('pca3Available')"
        >
          <span class="option-title">PCA3 available</span>
          <span class="option-state">{{ form.pca3Available ? 'Enabled' : 'Disabled' }}</span>
        </button>

        <button
          type="button"
          class="option-card"
          :class="{ selected: form.t2ergAvailable }"
          :aria-pressed="form.t2ergAvailable"
          data-tooltip="Enable T2:ERG biomarker field (requires PCA3)."
          @click="toggleOptional('t2ergAvailable')"
        >
          <span class="option-title">T2:ERG available</span>
          <span class="option-state">{{ form.t2ergAvailable ? 'Enabled' : 'Disabled' }}</span>
        </button>
      </div>

      <div class="form-grid biomarkers-grid">
        <label v-if="isPcptrcSelected && form.pctFreePsaAvailable">
          Percent free PSA
          <input v-model.number="form.pctFreePsa" type="number" min="5" max="75" step="0.1" title="Percent free PSA value from lab result." />
        </label>

        <label v-if="isPcptrcSelected && form.pca3Available">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              PCA3
              <span class="tooltip-popover">
                PCA3 is a urine biomarker associated with prostate cancer probability; larger values suggest higher risk.
              </span>
            </span>
          </span>
          <input v-model.number="form.pca3" type="number" min="0.3" max="332.5" step="0.1" title="PCA3 score from urine biomarker test." />
        </label>

        <label v-if="isPcptrcSelected && form.t2ergAvailable">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              T2:ERG
              <span class="tooltip-popover">
                T2:ERG is a urine biomarker linked to prostate tumor gene fusion activity and is interpreted alongside PCA3.
              </span>
            </span>
          </span>
          <input v-model.number="form.t2erg" type="number" min="0" max="6031.6" step="0.1" title="T2:ERG score, only relevant when PCA3 is available." />
        </label>

        <label v-if="showProstateVolumeCc">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              Prostate volume (cc)
              <span class="tooltip-popover">
                Prostate gland volume in cubic centimeters used by UCLA PCRC-MRI and SWOP RC5.
              </span>
            </span>
          </span>
          <input v-model.number="form.prostateVolumeCc" type="number" min="5" max="300" step="1" title="Used by analyzers requiring prostate volume (UCLA PCRC-MRI, SWOP RC5)." />
        </label>

        <label v-if="isUclaPcrcMriSelected">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              MRI PI-RADS score (UCLA)
              <span class="tooltip-popover">
                PI-RADS category from multiparametric MRI. UCLA PCRC-MRI groups ≤2 as 2, then 3, 4, and 5.
              </span>
            </span>
          </span>
          <select v-model.number="form.mriPiradsScore" title="PI-RADS score used by UCLA PCRC-MRI.">
            <option :value="2">Negative or ≤ 2</option>
            <option :value="3">3</option>
            <option :value="4">4</option>
            <option :value="5">5</option>
          </select>
        </label>

        <label v-if="isSwopRc6Selected">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              DRE volume class (SWOP RC6)
              <span class="tooltip-popover">
                DRE-based prostate size class used by SWOP RC6; values correspond to the calculator&apos;s fixed volume categories.
              </span>
            </span>
          </span>
          <select v-model.number="form.dreVolumeClassCc" title="DRE-based volume class used by SWOP RC6 Future Risk Calculator.">
            <option :value="25">25</option>
            <option :value="40">40</option>
            <option :value="60">60</option>
          </select>
        </label>

        <label v-if="isSwopRc5Selected">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              Legacy Gleason score (SWOP RC5)
              <span class="tooltip-popover">
                Historical Gleason category expected by SWOP RC5 for legacy model compatibility.
              </span>
            </span>
          </span>
          <select v-model.number="form.gleasonScoreLegacy" title="Legacy Gleason categories expected by SWOP RC5.">
            <option :value="4">2+2</option>
            <option :value="5">2+3</option>
            <option :value="6">3+3</option>
          </select>
        </label>

        <label v-if="isSwopRc5Selected">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              Biopsy cancer length (mm, SWOP RC5)
              <span class="tooltip-popover">
                Total malignant tissue length in biopsy cores (millimeters), used by SWOP RC5.
              </span>
            </span>
          </span>
          <input v-model.number="form.biopsyCancerLengthMm" type="number" min="1" max="65" step="0.1" title="Cancer length in biopsy core for SWOP RC5." />
        </label>

        <label v-if="isSwopRc5Selected">
          <span class="label-title tooltip-label">
            <span class="tooltip-anchor" tabindex="0">
              Biopsy benign length (mm, SWOP RC5)
              <span class="tooltip-popover">
                Total benign tissue length in biopsy cores (millimeters), used by SWOP RC5.
              </span>
            </span>
          </span>
          <input v-model.number="form.biopsyBenignLengthMm" type="number" min="10" max="110" step="0.1" title="Benign tissue length in biopsy core for SWOP RC5." />
        </label>

        <label v-if="isQcancerSelected">
          UK postcode (optional, QCancer)
          <input v-model="form.ukPostcode" type="text" maxlength="8" title="UK postcode used by QCancer; leave blank if unknown." />
        </label>

        <label v-if="isQcancerSelected">
          Smoking status (QCancer)
          <select v-model="form.smokingStatus" title="Smoking category used by QCancer.">
            <option value="NON_SMOKER">Non-smoker</option>
            <option value="EX_SMOKER">Ex-smoker</option>
            <option value="LIGHT">Light smoker (&lt;10/day)</option>
            <option value="MODERATE">Moderate smoker (10-19/day)</option>
            <option value="HEAVY">Heavy smoker (20+/day)</option>
          </select>
        </label>

        <label v-if="isQcancerSelected">
          Diabetes type (QCancer)
          <select v-model="form.diabetesType" title="Diabetes category used by QCancer.">
            <option value="NONE">None</option>
            <option value="TYPE_1">Type 1</option>
            <option value="TYPE_2">Type 2</option>
          </select>
        </label>

        <label v-if="isQcancerSelected">
          Manic depression or schizophrenia (QCancer)
          <select v-model="form.manicSchizophrenia" title="Mental health comorbidity input used by QCancer.">
            <option :value="false">No</option>
            <option :value="true">Yes</option>
          </select>
        </label>

        <label v-if="isQcancerSelected">
          Height (cm, optional QCancer)
          <input v-model.number="form.heightCm" type="number" min="140" max="210" step="1" title="Provide with weight or leave both blank." />
        </label>

        <label v-if="isQcancerSelected">
          Weight (kg, optional QCancer)
          <input v-model.number="form.weightKg" type="number" min="40" max="180" step="1" title="Provide with height or leave both blank." />
        </label>

        <label v-if="isQcancerSelected">
          QCancer horizon (years)
          <select v-model.number="form.qcancerYears" title="QCancer can estimate risk over 1 to 15 years.">
            <option v-for="year in 15" :key="`qcancer-year-${year}`" :value="year">{{ year }}</option>
          </select>
        </label>
      </div>

      <div v-if="isPcptrcSelected && form.detailedFamilyHistoryEnabled" class="form-grid detailed-grid">
        <label>
          FDR prostate cancer &lt; 60
          <select v-model="form.fdrPcLess60" title="Count of first-degree relatives diagnosed before age 60.">
            <option value="NO">No</option>
            <option value="YES_ONE">Yes, one</option>
            <option value="YES_TWO_OR_MORE">Yes, two or more</option>
          </select>
        </label>
        <label>
          FDR prostate cancer ≥ 60
          <select v-model="form.fdrPc60" title="Count of first-degree relatives diagnosed at or after age 60.">
            <option value="NO">No</option>
            <option value="YES_ONE">Yes, one</option>
            <option value="YES_TWO_OR_MORE">Yes, two or more</option>
          </select>
        </label>
        <label>
          FDR breast cancer
          <select v-model="form.fdrBc" title="Whether first-degree relatives had breast cancer.">
            <option value="NO">No</option>
            <option value="YES_AT_LEAST_ONE">Yes, at least one</option>
          </select>
        </label>
        <label>
          SDR prostate cancer
          <select v-model="form.sdr" title="Whether second-degree relatives had prostate cancer.">
            <option value="NO">No</option>
            <option value="YES_AT_LEAST_ONE">Yes, at least one</option>
          </select>
        </label>
      </div>
    </section>

    <section class="actions-row">
      <button :disabled="analyzeLoading || selectedAnalyzerCount === 0" @click="submitForAnalysis" title="Submit all current inputs for risk analysis.">
        {{ analyzeLoading ? 'Analyzing...' : 'Analyze risk' }}
      </button>
      <p v-if="selectedAnalyzerCount === 0" class="error-line">Select at least one analyzer.</p>
      <p v-if="analyzeError" class="error-line">Error analyzing risk: {{ analyzeError.message }}</p>
    </section>

    <section v-if="analysisResult" class="card result-card">
      <h2>Aggregated result</h2>
      <p class="selection-line">
        The aggregate below combines analyzers with different horizons. Use the horizon split to compare short-term biopsy decisions vs mid-/long-term estimates.
      </p>
      <div class="result-grid">
        <p>No cancer: {{ analysisResult.aggregate.noCancerRisk }}%</p>
        <p v-if="analysisResult.aggregate.lowGradeRisk !== null">
          Low-grade cancer: {{ analysisResult.aggregate.lowGradeRisk }}%
        </p>
        <p v-if="analysisResult.aggregate.highGradeRisk !== null">
          High-grade cancer: {{ analysisResult.aggregate.highGradeRisk }}%
        </p>
        <p v-if="analysisResult.aggregate.cancerRisk !== null">
          Cancer risk: {{ analysisResult.aggregate.cancerRisk }}%
        </p>
      </div>

      <div v-if="horizonAggregateRows.length" class="result-grid">
        <p v-for="bucket in horizonAggregateRows" :key="bucket.group">
          {{ bucket.group }}: avg cancer risk {{ bucket.avgCancerRisk }}% ({{ bucket.analyzers }} analyzer{{ bucket.analyzers > 1 ? 's' : '' }})
        </p>
      </div>

      <h3>Analyzer details</h3>
      <ul class="detail-list">
        <li v-for="entry in analysisResult.analyzers" :key="entry.analyzerId">
          {{ entry.displayName }} — {{ entry.success ? 'Success' : 'Failed' }}
          <span> | Horizon: {{ describeAnalyzerHorizon(entry.analyzerId) }}</span>
          <span> | Forwarded online: {{ entry.forwardedOnline ? 'Yes' : 'No' }}</span>
          <span v-if="entry.warning"> | {{ entry.warning }}</span>
          <div v-if="entry.risk">
            No cancer: {{ entry.risk.noCancerRisk }}%
            <span v-if="entry.risk.lowGradeRisk !== null"> | Low-grade: {{ entry.risk.lowGradeRisk }}%</span>
            <span v-if="entry.risk.highGradeRisk !== null"> | High-grade: {{ entry.risk.highGradeRisk }}%</span>
            <span v-if="entry.risk.cancerRisk !== null"> | Cancer: {{ entry.risk.cancerRisk }}%</span>
          </div>
        </li>
      </ul>
    </section>

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

<style scoped>
.home {
  max-width: 960px;
  margin: 2rem auto;
  padding: 1rem;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  line-height: 1.5;
  color: #dbeafe;
}

.graphiql-shell {
  width: 100%;
  max-width: none;
  margin: 0;
  height: 100vh;
}

.page-header {
  margin-bottom: 1rem;
}

h1 {
  margin: 0;
  color: #ffffff;
}

.subtitle {
  margin-top: 0.35rem;
  color: #93c5fd;
}

section {
  margin-top: 1.15rem;
}

.card {
  background: #111827;
  border: 1px solid #1d4ed8;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.15);
}

h2,
h3 {
  margin-top: 0;
  color: #bfdbfe;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.1rem;
}

.optional-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.8rem;
  margin-bottom: 1rem;
}

.biomarkers-grid {
  margin-bottom: 1rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-weight: 600;
  color: #bfdbfe;
}

.label-title {
  display: inline-flex;
  align-items: center;
  gap: 0;
}

.tooltip-label {
  width: fit-content;
}

.tooltip-anchor {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: help;
}

.tooltip-popover {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%) translateY(2px);
  min-width: 220px;
  max-width: 320px;
  padding: 0.55rem 0.7rem;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #0b1220;
  color: #dbeafe;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.25);
  font-size: 0.78rem;
  font-weight: 500;
  line-height: 1.35;
  white-space: normal;
  z-index: 20;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: opacity 70ms ease-out, transform 70ms ease-out, visibility 0s linear 70ms;
}

.tooltip-popover::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 100%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: #0b1220 transparent transparent transparent;
}

.tooltip-anchor:hover .tooltip-popover,
.tooltip-anchor:focus-visible .tooltip-popover,
.tooltip-anchor:focus-within .tooltip-popover {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
  transition-delay: 0s;
}

input {
  width: 100%;
  padding: 0.5rem;
  min-height: 42px;
  box-sizing: border-box;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #0b1220;
  color: #e2e8f0;
}

select {
  width: 100%;
  position: relative;
  padding: 0.5rem;
  padding-right: 2rem;
  min-height: 42px;
  box-sizing: border-box;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background-color: #0b1220;
  color: #e2e8f0;
  appearance: none;
  -webkit-appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #93c5fd 50%),
    linear-gradient(135deg, #93c5fd 50%, transparent 50%);
  background-position:
    calc(100% - 14px) calc(50% - 3px),
    calc(100% - 8px) calc(50% - 3px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
}

input:focus,
select:focus {
  outline: 2px solid #3b82f6;
  outline-offset: 1px;
}

button {
  padding: 0.65rem 1rem;
  border-radius: 10px;
  border: 1px solid #60a5fa;
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.actions-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.option-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.2rem;
  width: 100%;
  min-height: 74px;
  padding: 0.7rem 0.8rem;
  border-radius: 10px;
  border: 1px solid #1e3a8a;
  background: #0f172a;
  color: #bfdbfe;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

.option-card:hover,
.option-card:focus-visible {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.25);
}

.option-card.selected {
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.34), rgba(37, 99, 235, 0.26));
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.28);
}

.option-title {
  font-weight: 700;
  color: #dbeafe;
}

.option-state {
  font-size: 0.82rem;
  color: #93c5fd;
}

.option-card::after {
  content: attr(data-tooltip);
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%) translateY(4px);
  width: max-content;
  max-width: 260px;
  padding: 0.45rem 0.6rem;
  border-radius: 8px;
  border: 1px solid #3b82f6;
  background: #0b1220;
  color: #dbeafe;
  font-size: 0.78rem;
  font-weight: 500;
  line-height: 1.25;
  white-space: normal;
  opacity: 0;
  pointer-events: none;
  z-index: 20;
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.option-card:hover::after,
.option-card:focus-visible::after {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

ul {
  margin-top: 0.5rem;
  padding-left: 1.2rem;
}

.analyzer-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.analyzer-item {
  list-style: none;
}

.analyzer-pill {
  border: 1px solid #2563eb;
  background: #0f172a;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  color: #bfdbfe;
  font-size: 0.9rem;
}

.analyzer-pill.selected {
  border-color: #60a5fa;
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.35), rgba(37, 99, 235, 0.2));
}

.selection-line {
  margin: 0 0 0.6rem;
  color: #bfdbfe;
}

.selection-actions {
  margin-bottom: 0.75rem;
}

.ghost-button {
  background: transparent;
  color: #93c5fd;
  border: 1px solid #1d4ed8;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.3rem 1rem;
}

.detail-list {
  color: #dbeafe;
}

.error-line {
  color: #fca5a5;
}

.site-footer {
  margin-top: 1.5rem;
  border-top: 1px solid #1d4ed8;
  padding-top: 0.85rem;
}

.site-footer-inner {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.footer-nav {
  display: flex;
  gap: 0.9rem;
  flex-wrap: wrap;
}

.footer-nav a {
  color: #93c5fd;
  text-decoration: none;
  font-size: 0.92rem;
}

.footer-nav a:hover {
  text-decoration: underline;
}

.footer-note {
  margin: 0;
  color: #93c5fd;
  font-size: 0.85rem;
}

.legal-view {
  min-height: calc(100vh - 4rem);
}

.legal-card {
  max-width: 900px;
}

.footer-content {
  color: #bfdbfe;
  font-size: 0.94rem;
}

.footer-content :deep(p) {
  margin: 0.35rem 0;
}

.footer-content :deep(a) {
  color: #93c5fd;
}

:global(body) {
  background: radial-gradient(circle at top, #1e293b, #020617 70%);
}
</style>
