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
 * Risk analyzer implementing SWOP Risk Calculator 2 – a PSA-only logistic model
 * for overall prostate cancer probability.
 */
@Component
class SwopRc2RiskAnalyzer : RiskAnalyzer {
    private val logger = LoggerFactory.getLogger(SwopRc2RiskAnalyzer::class.java)

    /**
     * Returns SWOP RC2 metadata.
     *
     * @return [AnalyzerInfo] for SWOP Risk Calculator 2
     */
    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "SWOP_RC2",
        displayName = "SWOP Risk Calculator 2 (Using PSA Result)",
        sourceUrl = "https://www.prostatecancer-riskcalculator.com/2011/en/w2.html?v=2",
    )

    /**
     * Computes overall prostate cancer risk from PSA alone.
     *
     * @param request pre-validated patient data
     * @return [AnalyzerRiskResult] with the cancer probability
     * @throws IllegalArgumentException if PSA is outside the valid range (0.4–50)
     */
    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult {
        logger.debug("SWOP RC2 analysis started")
        require(request.psa in 0.4..50.0) { "SWOP RC2 requires PSA between 0.4 and 50 ng/ml." }

        val riskPercent = calculateRiskPercent(request.psa)

        return AnalyzerRiskResult(
            analyzerId = metadata().analyzerId,
            displayName = metadata().displayName,
            sourceUrl = metadata().sourceUrl,
            forwardedOnline = false,
            success = true,
            warning = null,
            risk = RiskResult(
                noCancerRisk = 100 - riskPercent,
                lowGradeRisk = null,
                highGradeRisk = null,
                cancerRisk = riskPercent,
                grouped = false,
            ),
        )
    }

    private fun calculateRiskPercent(psa: Double): Int {
        val ln2 = ln(2.0)
        val probability = round(1000.0 / (1 + exp(-1 * (-1.275405 + 0.728802 * (ln(psa) / ln2 - 2.0394821))))) / 1000.0
        return (probability * 100.0).roundToInt()
    }
}
