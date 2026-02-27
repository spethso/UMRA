package de.umra.risk.swop

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.RiskResult
import de.umra.risk.service.RiskAnalyzer
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.roundToInt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Risk analyzer implementing SWOP Risk Calculator 5 – distinguishes
 * indolent from aggressive prostate cancer using biopsy and volume data.
 */
@Component
class SwopRc5RiskAnalyzer : RiskAnalyzer {
    private val logger = LoggerFactory.getLogger(SwopRc5RiskAnalyzer::class.java)

    /**
     * Returns SWOP RC5 metadata.
     *
     * @return [AnalyzerInfo] for SWOP Risk Calculator 5
     */
    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "SWOP_RC5",
        displayName = "SWOP Risk Calculator 5 (Indolent vs aggressive)",
        sourceUrl = "https://www.prostatecancer-riskcalculator.com/2011/en/w6.html",
    )

    /**
     * Computes indolent-vs-aggressive cancer probability.
     *
     * @param request pre-validated patient data including biopsy measurements
     * @return [AnalyzerRiskResult] with indolent risk percentage
     * @throws IllegalArgumentException if biopsy or volume parameters are outside valid ranges
     */
    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult {
        logger.debug("SWOP RC5 analysis started")
        val gleason = request.gleasonScoreLegacy ?: 6
        val cancerLength = request.biopsyCancerLengthMm ?: 10.0
        val benignLength = request.biopsyBenignLengthMm ?: 40.0
        val prostateVolume = request.prostateVolumeCc ?: 40.0
        val psa = request.psa

        require(gleason in 4..6) { "SWOP RC5 requires legacy Gleason score 4..6." }
        require(cancerLength in 1.0..65.0) { "SWOP RC5 requires biopsy cancer length between 1 and 65 mm." }
        require(benignLength in 10.0..110.0) { "SWOP RC5 requires biopsy benign length between 10 and 110 mm." }
        require(prostateVolume in 10.0..90.0) { "SWOP RC5 requires prostate volume between 10 and 90 cc." }
        require(psa in 1.0..80.0) { "SWOP RC5 requires PSA between 1 and 80 ng/ml." }

        val (gl22, gl23) = when (gleason) {
            6 -> Pair(0.0, 0.0)
            5 -> Pair(0.0, 1.0)
            else -> Pair(1.0, 0.0)
        }

        val linear = -4.196 + 0.25 * (
            -5 * (ln(psa) - 3) +
                0.1 * (prostateVolume - 20) +
                4 * gl22 +
                1 * gl23 -
                3 * (ln(cancerLength) - 3) +
                0.1 * (benignLength - 40)
            )

        val probability = round(1000.0 / (1 + exp(-linear))) / 1000.0
        val indolentRiskPercent = (probability * 100.0).roundToInt().coerceIn(0, 100)

        return AnalyzerRiskResult(
            analyzerId = metadata().analyzerId,
            displayName = metadata().displayName,
            sourceUrl = metadata().sourceUrl,
            forwardedOnline = false,
            success = true,
            warning = null,
            risk = RiskResult(
                noCancerRisk = 100 - indolentRiskPercent,
                lowGradeRisk = indolentRiskPercent,
                highGradeRisk = null,
                cancerRisk = indolentRiskPercent,
                grouped = true,
            ),
        )
    }
}
