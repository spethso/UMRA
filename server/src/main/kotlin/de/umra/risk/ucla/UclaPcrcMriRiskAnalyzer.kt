package de.umra.risk.ucla

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.DreOption
import de.umra.risk.model.PriorBiopsyOption
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.Race
import de.umra.risk.model.RiskResult
import de.umra.risk.service.RiskAnalyzer
import kotlin.math.exp
import kotlin.math.roundToInt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Risk analyzer implementing the UCLA Prostate Cancer Risk Calculator
 * for MRI-guided biopsy (PCRC-MRI).
 *
 * Uses a published logistic-regression model to estimate high-grade
 * prostate cancer probability from MRI, PSA, and clinical variables.
 */
@Component
class UclaPcrcMriRiskAnalyzer : RiskAnalyzer {
    private val logger = LoggerFactory.getLogger(UclaPcrcMriRiskAnalyzer::class.java)

    /**
     * Returns UCLA PCRC-MRI metadata.
     *
     * @return [AnalyzerInfo] for UCLA PCRC-MRI
     */
    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "UCLA_PCRC_MRI",
        displayName = "UCLA PCRC-MRI (MRI-guided biopsy)",
        sourceUrl = "https://www.uclahealth.org/departments/urology/iuo/research/prostate-cancer/risk-calculator-mri-guided-biopsy-pcrc-mri",
    )

    /**
     * Computes the probability of clinically significant (high-grade)
     * prostate cancer using the PCRC-MRI model.
     *
     * @param request pre-validated patient data; must include prostate volume and PI-RADS score
     * @return [AnalyzerRiskResult] with high-grade cancer probability
     * @throws IllegalArgumentException if required MRI fields are missing or out of range
     */
    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult {
        logger.debug("UCLA PCRC-MRI analysis started")
        val age = request.age.toDouble()
        val psa = request.psa
        val dre = request.dre.toUclaValue()
        val prevBiopsy = request.priorBiopsy.toUclaValue()
        val prostateVolume = request.prostateVolumeCc
            ?: throw IllegalArgumentException("UCLA PCRC-MRI requires prostate volume (cc).")
        val piradsScore = request.mriPiradsScore
            ?: throw IllegalArgumentException("UCLA PCRC-MRI requires MRI PI-RADS score.")

        require(age in 18.0..100.0) { "UCLA PCRC-MRI requires age between 18 and 100." }
        require(psa in 0.1..150.0) { "UCLA PCRC-MRI requires PSA between 0.1 and 150 ng/ml." }
        require(prostateVolume in 5.0..300.0) { "UCLA PCRC-MRI requires prostate volume between 5 and 300 cc." }
        require(piradsScore in 2..5) { "UCLA PCRC-MRI requires PI-RADS score between 2 and 5." }

        val raceWeight = request.race.toUclaRaceWeight()
        val piradsWeight = piradsScore.toUclaPiradsWeight()
        val psaDensityBinary = if ((psa / prostateVolume) >= 0.15) 1.0 else 0.0

        val equation = MRI_INTERCEPT +
            (MRI_AGE * age) +
            raceWeight +
            (MRI_PSA * psa) +
            (MRI_DRE * dre) +
            (MRI_PREV_BIOPSY * prevBiopsy) +
            (MRI_PROSTATE_VOLUME * prostateVolume) +
            (MRI_PSA_DENSITY * psaDensityBinary) +
            piradsWeight

        val odds = exp(equation)
        val probabilityPercent = ((odds / (1.0 + odds)) * 100.0).roundToInt().coerceIn(0, 100)

        return AnalyzerRiskResult(
            analyzerId = metadata().analyzerId,
            displayName = metadata().displayName,
            sourceUrl = metadata().sourceUrl,
            forwardedOnline = false,
            success = true,
            warning = null,
            risk = RiskResult(
                noCancerRisk = 100 - probabilityPercent,
                lowGradeRisk = null,
                highGradeRisk = probabilityPercent,
                cancerRisk = probabilityPercent,
                grouped = true,
            ),
        )
    }

    private fun PriorBiopsyOption.toUclaValue(): Double = when (this) {
        PriorBiopsyOption.PRIOR_NEGATIVE_BIOPSY -> 1.0
        PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY, PriorBiopsyOption.NOT_SURE -> 0.0
    }

    private fun DreOption.toUclaValue(): Double = when (this) {
        DreOption.ABNORMAL -> 1.0
        DreOption.NORMAL, DreOption.NOT_PERFORMED_OR_NOT_SURE -> 0.0
    }

    private fun Race.toUclaRaceWeight(): Double = when (this) {
        Race.AFRICAN_AMERICAN -> 0.2378
        Race.ASIAN -> -0.7719
        Race.CAUCASIAN -> 0.0
        Race.OTHER,
        Race.HISPANIC_LATINO,
        Race.MIDDLE_EASTERN_NORTH_AFRICAN,
        Race.NATIVE_AMERICAN_OR_ALASKA_NATIVE,
        Race.NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER -> 0.1884
        Race.UNKNOWN -> -0.0856
    }

    private fun Int.toUclaPiradsWeight(): Double = when (this) {
        2 -> 0.0
        3 -> 0.5102
        4 -> 1.3751
        else -> 2.7627
    }

    companion object {
        private const val MRI_INTERCEPT = -4.62990
        private const val MRI_AGE = 0.05470
        private const val MRI_PSA = 0.01920
        private const val MRI_DRE = 0.86460
        private const val MRI_PREV_BIOPSY = -0.61630
        private const val MRI_PROSTATE_VOLUME = -0.01790
        private const val MRI_PSA_DENSITY = 0.88020
    }
}
