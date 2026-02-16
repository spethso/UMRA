<script setup>
import { computed, ref } from 'vue'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { GraphiQL } from '@caipira/vue-graphiql'

const currentPath = window.location.pathname.replace(/\/+$/, '') || '/'
const showGraphiql = currentPath === '/graphiql'
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
  mutation AnalyzeProstateCancerRisk($input: ProstateCancerRiskInput!) {
    analyzeProstateCancerRisk(input: $input) {
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
const analysisResult = ref(null)

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

  const response = await analyzeMutate({ input })
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

  <main v-else class="home">
    <header class="page-header">
      <h1>Unified Prostate Risk Analyzer</h1>
      <p class="subtitle">Enter clinical factors and optional biomarkers to analyze risk.</p>
    </header>

    <section class="card">
      <p v-if="analyzersLoading">Loading analyzers...</p>
      <p v-else-if="analyzersError" class="error-line">Error loading analyzers: {{ analyzersError.message }}</p>
      <ul v-else class="analyzer-list">
        <li v-for="analyzer in analyzers" :key="analyzer.analyzerId" class="analyzer-pill">
          {{ analyzer.displayName }} ({{ analyzer.analyzerId }})
        </li>
      </ul>
    </section>

    <section class="card">
      <h2>Core factors</h2>
      <div class="form-grid">
        <label>
          Race
          <select v-model="form.race" title="Patient race category used by PCPTRC.">
            <option value="AFRICAN_AMERICAN">African American</option>
            <option value="CAUCASIAN">Caucasian</option>
            <option value="HISPANIC">Hispanic</option>
            <option value="OTHER">Other</option>
          </select>
        </label>

        <label>
          Age
          <input v-model.number="form.age" type="number" min="55" max="90" title="Patient age in years (55 to 90)." />
        </label>

        <label>
          PSA (ng/ml)
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
          DRE
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

    <section class="card">
      <h2>Optional data</h2>
      <div class="toggle-grid">
        <label class="checkbox-row">
          <input v-model="form.detailedFamilyHistoryEnabled" type="checkbox" title="Enable detailed family history inputs for caucasian profile path." />
          Enable detailed family history
        </label>
        <label class="checkbox-row">
          <input v-model="form.pctFreePsaAvailable" type="checkbox" title="Enable percent free PSA field (5 to 75)." />
          Percent free PSA available
        </label>
        <label class="checkbox-row">
          <input v-model="form.pca3Available" type="checkbox" title="Enable PCA3 biomarker field (0.3 to 332.5)." />
          PCA3 available
        </label>
        <label class="checkbox-row">
          <input v-model="form.t2ergAvailable" type="checkbox" title="Enable T2:ERG biomarker field (requires PCA3)." />
          T2:ERG available
        </label>
      </div>

      <div class="form-grid biomarkers-grid">
        <label v-if="form.pctFreePsaAvailable">
          Percent free PSA
          <input v-model.number="form.pctFreePsa" type="number" min="5" max="75" step="0.1" title="Percent free PSA value from lab result." />
        </label>

        <label v-if="form.pca3Available">
          PCA3
          <input v-model.number="form.pca3" type="number" min="0.3" max="332.5" step="0.1" title="PCA3 score from urine biomarker test." />
        </label>

        <label v-if="form.t2ergAvailable">
          T2:ERG
          <input v-model.number="form.t2erg" type="number" min="0" max="6031.6" step="0.1" title="T2:ERG score, only relevant when PCA3 is available." />
        </label>
      </div>

      <div v-if="form.detailedFamilyHistoryEnabled" class="form-grid detailed-grid">
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
      <button :disabled="analyzeLoading" @click="submitForAnalysis" title="Submit all current inputs for risk analysis.">
        {{ analyzeLoading ? 'Analyzing...' : 'Analyze risk' }}
      </button>
      <p v-if="analyzeError" class="error-line">Error analyzing risk: {{ analyzeError.message }}</p>
    </section>

    <section v-if="analysisResult" class="card result-card">
      <h2>Aggregated result</h2>
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

      <h3>Analyzer details</h3>
      <ul class="detail-list">
        <li v-for="entry in analysisResult.analyzers" :key="entry.analyzerId">
          {{ entry.displayName }} — {{ entry.success ? 'Success' : 'Failed' }}
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
  margin-top: 1rem;
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
  gap: 0.9rem;
}

.toggle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.6rem;
  margin-bottom: 0.9rem;
}

.biomarkers-grid {
  margin-bottom: 0.9rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-weight: 600;
  color: #bfdbfe;
}

.checkbox-row {
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.55rem;
  border-radius: 8px;
  background: #0f172a;
  border: 1px solid #1e3a8a;
}

.checkbox-row input {
  width: auto;
  margin: 0;
}

input {
  width: 100%;
  padding: 0.5rem;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #0b1220;
  color: #e2e8f0;
}

select {
  width: 100%;
  padding: 0.5rem;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #0b1220;
  color: #e2e8f0;
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

.analyzer-pill {
  list-style: none;
  border: 1px solid #2563eb;
  background: #0f172a;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  color: #bfdbfe;
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

:global(body) {
  background: radial-gradient(circle at top, #1e293b, #020617 70%);
}
</style>
