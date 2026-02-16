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
    <h1>Unified Prostate Risk Analyzer</h1>

    <section>
      <p v-if="analyzersLoading">Loading analyzers...</p>
      <p v-else-if="analyzersError">Error loading analyzers: {{ analyzersError.message }}</p>
      <ul v-else>
        <li v-for="analyzer in analyzers" :key="analyzer.analyzerId">
          {{ analyzer.displayName }} ({{ analyzer.analyzerId }})
        </li>
      </ul>
    </section>

    <section class="form-grid">
      <label>
        Race
        <select v-model="form.race">
          <option value="AFRICAN_AMERICAN">African American</option>
          <option value="CAUCASIAN">Caucasian</option>
          <option value="HISPANIC">Hispanic</option>
          <option value="OTHER">Other</option>
        </select>
      </label>

      <label>
        Age
        <input v-model.number="form.age" type="number" min="55" max="90" />
      </label>

      <label>
        PSA (ng/ml)
        <input v-model.number="form.psa" type="number" min="0.1" max="50" step="0.1" />
      </label>

      <label>
        Family history
        <select v-model="form.familyHistory">
          <option value="YES">Yes</option>
          <option value="NO">No</option>
          <option value="DO_NOT_KNOW">Do not know</option>
        </select>
      </label>

      <label>
        DRE
        <select v-model="form.dre">
          <option value="ABNORMAL">Abnormal</option>
          <option value="NORMAL">Normal</option>
          <option value="NOT_PERFORMED_OR_NOT_SURE">Not performed or not sure</option>
        </select>
      </label>

      <label>
        Prior biopsy
        <select v-model="form.priorBiopsy">
          <option value="NEVER_HAD_PRIOR_BIOPSY">Never had a prior biopsy</option>
          <option value="PRIOR_NEGATIVE_BIOPSY">Prior negative biopsy</option>
          <option value="NOT_SURE">Not sure</option>
        </select>
      </label>

      <label class="checkbox-row">
        <input v-model="form.detailedFamilyHistoryEnabled" type="checkbox" />
        Enable detailed family history
      </label>

      <template v-if="form.detailedFamilyHistoryEnabled">
        <label>
          FDR prostate cancer < 60
          <select v-model="form.fdrPcLess60">
            <option value="NO">No</option>
            <option value="YES_ONE">Yes, one</option>
            <option value="YES_TWO_OR_MORE">Yes, two or more</option>
          </select>
        </label>
        <label>
          FDR prostate cancer >= 60
          <select v-model="form.fdrPc60">
            <option value="NO">No</option>
            <option value="YES_ONE">Yes, one</option>
            <option value="YES_TWO_OR_MORE">Yes, two or more</option>
          </select>
        </label>
        <label>
          FDR breast cancer
          <select v-model="form.fdrBc">
            <option value="NO">No</option>
            <option value="YES_AT_LEAST_ONE">Yes, at least one</option>
          </select>
        </label>
        <label>
          SDR prostate cancer
          <select v-model="form.sdr">
            <option value="NO">No</option>
            <option value="YES_AT_LEAST_ONE">Yes, at least one</option>
          </select>
        </label>
      </template>

      <label class="checkbox-row">
        <input v-model="form.pctFreePsaAvailable" type="checkbox" />
        Percent free PSA available
      </label>
      <label v-if="form.pctFreePsaAvailable">
        Percent free PSA
        <input v-model.number="form.pctFreePsa" type="number" min="5" max="75" step="0.1" />
      </label>

      <label class="checkbox-row">
        <input v-model="form.pca3Available" type="checkbox" />
        PCA3 available
      </label>
      <label v-if="form.pca3Available">
        PCA3
        <input v-model.number="form.pca3" type="number" min="0.3" max="332.5" step="0.1" />
      </label>

      <label class="checkbox-row">
        <input v-model="form.t2ergAvailable" type="checkbox" />
        T2:ERG available
      </label>
      <label v-if="form.t2ergAvailable">
        T2:ERG
        <input v-model.number="form.t2erg" type="number" min="0" max="6031.6" step="0.1" />
      </label>
    </section>

    <section>
      <button :disabled="analyzeLoading" @click="submitForAnalysis">
        {{ analyzeLoading ? 'Analyzing...' : 'Analyze risk' }}
      </button>
      <p v-if="analyzeError">Error analyzing risk: {{ analyzeError.message }}</p>
    </section>

    <section v-if="analysisResult">
      <h2>Aggregated result</h2>
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

      <h3>Analyzer details</h3>
      <ul>
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
  max-width: 700px;
  margin: 3rem auto;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  line-height: 1.5;
}

.graphiql-shell {
  width: 100%;
  max-width: none;
  margin: 0;
  height: 100vh;
}

h1 {
  margin-bottom: 1rem;
}

section {
  margin-top: 1.25rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0.75rem;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.checkbox-row {
  flex-direction: row;
  align-items: center;
}

input {
  width: 100%;
  padding: 0.5rem;
}

select {
  width: 100%;
  padding: 0.5rem;
}

button {
  padding: 0.5rem 0.75rem;
  margin-top: 0.5rem;
  cursor: pointer;
}

ul {
  margin-top: 0.75rem;
  padding-left: 1.2rem;
}
</style>
