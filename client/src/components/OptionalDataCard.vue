<script setup lang="ts">
import type { OptionalToggleKey, RiskForm } from '../types/risk'

defineProps<{
  form: RiskForm
  isPcptrcSelected: boolean
  isSwopRc5Selected: boolean
  isSwopRc6Selected: boolean
  isUclaPcrcMriSelected: boolean
  isQcancerSelected: boolean
  showOptionalDataSection: boolean
  showProstateVolumeCc: boolean
  guidedMode: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-optional', key: OptionalToggleKey): void
}>()
</script>

<template>
  <section v-if="showOptionalDataSection || guidedMode" class="card">
    <h2>Additional data</h2>
    <p v-if="guidedMode" class="guided-hint">Provide as much data as available — the server will determine applicable analyzers.</p>

    <!-- Toggleable data groups — each toggle with its fields inline -->
    <div v-if="isPcptrcSelected || guidedMode" class="toggle-groups">

      <!-- Detailed family history -->
      <div class="toggle-group" :class="{ active: form.detailedFamilyHistoryEnabled }">
        <button
          type="button"
          class="toggle-group-header"
          :aria-pressed="form.detailedFamilyHistoryEnabled"
          @click="emit('toggle-optional', 'detailedFamilyHistoryEnabled')"
        >
          <span class="toggle-group-title">Detailed family history</span>
          <span class="toggle-group-state">{{ form.detailedFamilyHistoryEnabled ? 'Enabled' : 'Disabled' }}</span>
        </button>
        <div v-if="form.detailedFamilyHistoryEnabled" class="toggle-group-fields">
          <div class="form-grid">
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
        </div>
      </div>

      <!-- Percent free PSA -->
      <div class="toggle-group" :class="{ active: form.pctFreePsaAvailable }">
        <button
          type="button"
          class="toggle-group-header"
          :aria-pressed="form.pctFreePsaAvailable"
          @click="emit('toggle-optional', 'pctFreePsaAvailable')"
        >
          <span class="toggle-group-title">Percent free PSA</span>
          <span class="toggle-group-state">{{ form.pctFreePsaAvailable ? 'Enabled' : 'Disabled' }}</span>
        </button>
        <div v-if="form.pctFreePsaAvailable" class="toggle-group-fields">
          <label>
            Value (%)
            <input v-model.number="form.pctFreePsa" type="number" min="5" max="75" step="0.1" title="Percent free PSA value from lab result." />
          </label>
        </div>
      </div>

      <!-- PCA3 -->
      <div class="toggle-group" :class="{ active: form.pca3Available }">
        <button
          type="button"
          class="toggle-group-header"
          :aria-pressed="form.pca3Available"
          @click="emit('toggle-optional', 'pca3Available')"
        >
          <span class="toggle-group-title">PCA3</span>
          <span class="toggle-group-state">{{ form.pca3Available ? 'Enabled' : 'Disabled' }}</span>
        </button>
        <div v-if="form.pca3Available" class="toggle-group-fields">
          <label>
            <span class="label-title tooltip-label">
              <span class="tooltip-anchor" tabindex="0">
                Score
                <span class="tooltip-popover">
                  PCA3 is a urine biomarker associated with prostate cancer probability; larger values suggest higher risk.
                </span>
              </span>
            </span>
            <input v-model.number="form.pca3" type="number" min="0.3" max="332.5" step="0.1" title="PCA3 score from urine biomarker test." />
          </label>
        </div>
      </div>

      <!-- T2:ERG -->
      <div class="toggle-group" :class="{ active: form.t2ergAvailable }">
        <button
          type="button"
          class="toggle-group-header"
          :aria-pressed="form.t2ergAvailable"
          @click="emit('toggle-optional', 't2ergAvailable')"
        >
          <span class="toggle-group-title">T2:ERG</span>
          <span class="toggle-group-state">{{ form.t2ergAvailable ? 'Enabled' : 'Disabled' }}</span>
        </button>
        <div v-if="form.t2ergAvailable" class="toggle-group-fields">
          <label>
            <span class="label-title tooltip-label">
              <span class="tooltip-anchor" tabindex="0">
                Score
                <span class="tooltip-popover">
                  T2:ERG is a urine biomarker linked to prostate tumor gene fusion activity and is interpreted alongside PCA3.
                </span>
              </span>
            </span>
            <input v-model.number="form.t2erg" type="number" min="0" max="6031.6" step="0.1" title="T2:ERG score, only relevant when PCA3 is available." />
          </label>
        </div>
      </div>
    </div>

    <!-- Always-visible fields, separated by a subtle divider when toggles are shown -->
    <div
      v-if="showProstateVolumeCc || isUclaPcrcMriSelected || isSwopRc5Selected || isSwopRc6Selected || isQcancerSelected || guidedMode"
      class="form-grid biomarkers-grid"
      :class="{ 'has-top-separator': isPcptrcSelected || guidedMode }"
    >
      <label v-if="showProstateVolumeCc || guidedMode">
        <span class="label-title tooltip-label">
          <span class="tooltip-anchor" tabindex="0">
            Prostate volume (cc)
            <span class="tooltip-popover">
              Prostate gland volume in cubic centimeters, used for MRI-based and biopsy-based risk models.
            </span>
          </span>
        </span>
        <input v-model.number="form.prostateVolumeCc" type="number" min="5" max="300" step="1" title="Used by analyzers requiring prostate volume (UCLA PCRC-MRI, SWOP RC5)." />
      </label>

      <label v-if="isUclaPcrcMriSelected || guidedMode">
        <span class="label-title tooltip-label">
          <span class="tooltip-anchor" tabindex="0">
            {{ guidedMode ? 'MRI PI-RADS score' : 'MRI PI-RADS score (UCLA)' }}
            <span class="tooltip-popover">
              PI-RADS category from multiparametric MRI, grouped as ≤2, 3, 4, and 5.
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

      <label v-if="isSwopRc6Selected || guidedMode">
        <span class="label-title tooltip-label">
          <span class="tooltip-anchor" tabindex="0">
            {{ guidedMode ? 'DRE volume class' : 'DRE volume class (SWOP RC6)' }}
            <span class="tooltip-popover">
              DRE-based prostate size class; values correspond to fixed volume categories.
            </span>
          </span>
        </span>
        <select v-model.number="form.dreVolumeClassCc" title="DRE-based volume class used by SWOP RC6 Future Risk Calculator.">
          <option :value="25">25</option>
          <option :value="40">40</option>
          <option :value="60">60</option>
        </select>
      </label>

      <label v-if="isSwopRc5Selected || guidedMode">
        <span class="label-title tooltip-label">
          <span class="tooltip-anchor" tabindex="0">
            {{ guidedMode ? 'Legacy Gleason score' : 'Legacy Gleason score (SWOP RC5)' }}
            <span class="tooltip-popover">
              Historical Gleason category for legacy model compatibility.
            </span>
          </span>
        </span>
        <select v-model.number="form.gleasonScoreLegacy" title="Legacy Gleason categories expected by SWOP RC5.">
          <option :value="4">2+2</option>
          <option :value="5">2+3</option>
          <option :value="6">3+3</option>
        </select>
      </label>

      <label v-if="isSwopRc5Selected || guidedMode">
        <span class="label-title tooltip-label">
          <span class="tooltip-anchor" tabindex="0">
            {{ guidedMode ? 'Biopsy cancer length (mm)' : 'Biopsy cancer length (mm, SWOP RC5)' }}
            <span class="tooltip-popover">
              Total malignant tissue length in biopsy cores (millimeters).
            </span>
          </span>
        </span>
        <input v-model.number="form.biopsyCancerLengthMm" type="number" min="1" max="65" step="0.1" title="Cancer length in biopsy core for SWOP RC5." />
      </label>

      <label v-if="isSwopRc5Selected || guidedMode">
        <span class="label-title tooltip-label">
          <span class="tooltip-anchor" tabindex="0">
            {{ guidedMode ? 'Biopsy benign length (mm)' : 'Biopsy benign length (mm, SWOP RC5)' }}
            <span class="tooltip-popover">
              Total benign tissue length in biopsy cores (millimeters).
            </span>
          </span>
        </span>
        <input v-model.number="form.biopsyBenignLengthMm" type="number" min="10" max="110" step="0.1" title="Benign tissue length in biopsy core for SWOP RC5." />
      </label>

      <label v-if="isQcancerSelected || guidedMode">
        {{ guidedMode ? 'Smoking status' : 'Smoking status (QCancer)' }}
        <select v-model="form.smokingStatus" title="Smoking category for risk calculation.">
          <option value="NON_SMOKER">Non-smoker</option>
          <option value="EX_SMOKER">Ex-smoker</option>
          <option value="LIGHT">Light smoker (&lt;10/day)</option>
          <option value="MODERATE">Moderate smoker (10-19/day)</option>
          <option value="HEAVY">Heavy smoker (20+/day)</option>
        </select>
      </label>

      <label v-if="isQcancerSelected || guidedMode">
        {{ guidedMode ? 'Diabetes type' : 'Diabetes type (QCancer)' }}
        <select v-model="form.diabetesType" title="Diabetes category for risk calculation.">
          <option value="NONE">None</option>
          <option value="TYPE_1">Type 1</option>
          <option value="TYPE_2">Type 2</option>
        </select>
      </label>

      <label v-if="isQcancerSelected || guidedMode">
        {{ guidedMode ? 'Manic depression or schizophrenia' : 'Manic depression or schizophrenia (QCancer)' }}
        <select v-model="form.manicSchizophrenia" title="Mental health comorbidity input for risk calculation.">
          <option :value="false">No</option>
          <option :value="true">Yes</option>
        </select>
      </label>

      <label v-if="isQcancerSelected || guidedMode">
        {{ guidedMode ? 'Height (cm, optional)' : 'Height (cm, optional QCancer)' }}
        <input v-model.number="form.heightCm" type="number" min="140" max="210" step="1" title="Provide with weight or leave both blank." />
      </label>

      <label v-if="isQcancerSelected || guidedMode">
        {{ guidedMode ? 'Weight (kg, optional)' : 'Weight (kg, optional QCancer)' }}
        <input v-model.number="form.weightKg" type="number" min="40" max="180" step="1" title="Provide with height or leave both blank." />
      </label>

      <label v-if="isQcancerSelected || guidedMode">
        QCancer horizon (years)
        <select v-model.number="form.qcancerYears" title="QCancer can estimate risk over 1 to 15 years.">
          <option v-for="year in 15" :key="`qcancer-year-${year}`" :value="year">{{ year }}</option>
        </select>
      </label>
    </div>
  </section>
</template>
