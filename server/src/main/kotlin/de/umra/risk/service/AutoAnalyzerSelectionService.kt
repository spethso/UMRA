package de.umra.risk.service

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.ProstateCancerRiskInput
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Determines which analyzers are applicable based on the data provided in the input.
 *
 * Rules:
 * - PCPTRC, SWOP_RC2, SWOP_RC5, SWOP_RC6, QCANCER_10YR_PROSTATE_PSA always apply
 *   (they either only need core fields or have safe defaults for optional ones).
 * - UCLA_PCRC_MRI requires both `prostateVolumeCc` and `mriPiradsScore`; it is only
 *   included when both values are present.
 */
@Service
class AutoAnalyzerSelectionService(
    private val analyzers: List<RiskAnalyzer>,
) {
    private val logger = LoggerFactory.getLogger(AutoAnalyzerSelectionService::class.java)

    /**
     * Returns metadata for the analyzers that are applicable to the given input.
     *
     * @param input patient clinical data used to determine applicability
     * @return list of [AnalyzerInfo] for each recommended analyzer
     */
    fun recommend(input: ProstateCancerRiskInput): List<AnalyzerInfo> {
        val result = analyzers
            .filter { isApplicable(it.metadata().analyzerId, input) }
            .map { it.metadata() }
        logger.debug("Recommended {} analyzer(s) for input", result.size)
        return result
    }

    /**
     * Convenience variant of [recommend] that returns only analyzer ids.
     *
     * @param input patient clinical data
     * @return ordered list of recommended analyzer id strings
     */
    fun recommendIds(input: ProstateCancerRiskInput): List<String> =
        recommend(input).map { it.analyzerId }

    /**
     * Decides whether a given analyzer is applicable for the provided input.
     *
     * @param analyzerId the identifier of the analyzer to check
     * @param input      the patient clinical data
     * @return `true` if the analyzer can meaningfully process the input
     */
    private fun isApplicable(analyzerId: String, input: ProstateCancerRiskInput): Boolean =
        when (analyzerId) {
            "UCLA_PCRC_MRI" -> input.prostateVolumeCc != null && input.mriPiradsScore != null
            else -> true
        }
}
