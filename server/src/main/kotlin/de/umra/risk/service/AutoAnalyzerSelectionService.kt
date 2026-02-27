package de.umra.risk.service

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.ProstateCancerRiskInput
import org.springframework.stereotype.Service

/**
 * Determines which analyzers are applicable based on the data provided in the input.
 *
 * Rules:
 * - PCPTRC, SWOP_RC2, SWOP_RC5, SWOP_RC6, QCANCER_10YR_PROSTATE_PSA always apply
 *   (they either only need core fields or have safe defaults for optional ones).
 * - UCLA_PCRC_MRI requires both prostateVolumeCc and mriPiradsScore; it is only
 *   included when both values are present.
 */
@Service
class AutoAnalyzerSelectionService(
    private val analyzers: List<RiskAnalyzer>,
) {
    fun recommend(input: ProstateCancerRiskInput): List<AnalyzerInfo> =
        analyzers
            .filter { isApplicable(it.metadata().analyzerId, input) }
            .map { it.metadata() }

    fun recommendIds(input: ProstateCancerRiskInput): List<String> =
        recommend(input).map { it.analyzerId }

    private fun isApplicable(analyzerId: String, input: ProstateCancerRiskInput): Boolean =
        when (analyzerId) {
            "UCLA_PCRC_MRI" -> input.prostateVolumeCc != null && input.mriPiradsScore != null
            else -> true
        }
}
