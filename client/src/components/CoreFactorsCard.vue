<script setup lang="ts">
import type { RiskForm } from '../types/risk'

defineProps<{
  form: RiskForm
}>()
</script>

<template>
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
</template>
