package de.umra.risk.service

import de.umra.risk.model.AggregateRiskResult
import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.DiabetesTypeOption
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.RiskAnalysisResponse
import de.umra.risk.model.SnpGenotype
import de.umra.risk.model.SmokingStatusOption
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Orchestrates risk analysis by delegating to one or more [RiskAnalyzer]
 * implementations and aggregating their results.
 */
@Service
class RiskAggregationService(
    private val analyzers: List<RiskAnalyzer>,
    private val autoAnalyzerSelectionService: AutoAnalyzerSelectionService,
) {
    private val logger = LoggerFactory.getLogger(RiskAggregationService::class.java)

    /**
     * Returns metadata for all registered analyzers.
     *
     * @return list of [AnalyzerInfo] for every known analyzer
     */
    fun availableAnalyzers(): List<AnalyzerInfo> = analyzers.map { it.metadata() }

    /**
     * Runs the risk analysis using the selected (or automatically recommended)
     * analyzers and returns an aggregated response.
     *
     * @param input       patient clinical data
     * @param analyzerIds explicit analyzer ids, `null` for auto-selection,
     *                    or an empty list to run all analyzers
     * @return [RiskAnalysisResponse] containing per-analyzer and aggregate results
     */
    fun analyze(input: ProstateCancerRiskInput, analyzerIds: List<String>? = null): RiskAnalysisResponse {
        val request = input.toRequest()
        val selectedIds = analyzerIds
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()

        val selectedAnalyzers = when {
            selectedIds == null -> {
                val recommended = autoAnalyzerSelectionService.recommendIds(input).toSet()
                analyzers.filter { recommended.contains(it.metadata().analyzerId) }
            }
            selectedIds.isEmpty() -> analyzers
            else -> {
                val matched = analyzers.filter { analyzer -> selectedIds.contains(analyzer.metadata().analyzerId) }
                if (matched.isEmpty() && selectedIds.isNotEmpty()) {
                    logger.warn(
                        "None of the requested analyzerIds {} matched a known analyzer – running none",
                        selectedIds,
                    )
                }
                matched
            }
        }

        logger.info("Running {} analyzer(s): {}", selectedAnalyzers.size, selectedAnalyzers.map { it.metadata().analyzerId })

        val analyzerResults = selectedAnalyzers.map { analyzer ->
            runCatching { analyzer.analyze(request) }
                .getOrElse { exception ->
                    val metadata = analyzer.metadata()
                    logger.warn("Analyzer {} failed: {}", metadata.analyzerId, exception.message)
                    AnalyzerRiskResult(
                        analyzerId = metadata.analyzerId,
                        displayName = metadata.displayName,
                        sourceUrl = metadata.sourceUrl,
                        forwardedOnline = false,
                        success = false,
                        warning = exception.message ?: "Analyzer failed",
                        risk = null,
                    )
                }
        }

        return RiskAnalysisResponse(
            analyzers = analyzerResults,
            aggregate = aggregate(analyzerResults),
        )
    }

    /**
     * Computes an aggregate risk result by averaging the individual analyzer
     * results that completed successfully.
     *
     * @param results per-analyzer results (may include failures)
     * @return [AggregateRiskResult] averaging all successful risk values
     */
    private fun aggregate(results: List<AnalyzerRiskResult>): AggregateRiskResult {
        val successful = results.mapNotNull { it.risk }
        if (successful.isEmpty()) {
            return AggregateRiskResult(
                noCancerRisk = 0,
                lowGradeRisk = null,
                highGradeRisk = null,
                cancerRisk = null,
                basedOnAnalyzers = 0,
            )
        }

        return AggregateRiskResult(
            noCancerRisk = successful.map { it.noCancerRisk }.average().toInt(),
            lowGradeRisk = successful.mapNotNull { it.lowGradeRisk }.averageOrNull(),
            highGradeRisk = successful.mapNotNull { it.highGradeRisk }.averageOrNull(),
            cancerRisk = successful.mapNotNull { it.cancerRisk }.averageOrNull(),
            basedOnAnalyzers = successful.size,
        )
    }

    private fun List<Int>.averageOrNull(): Int? = if (isEmpty()) null else average().toInt()

    private fun ProstateCancerRiskInput.toRequest(): ProstateCancerRiskRequest =
        ProstateCancerRiskRequest(
            race = race,
            age = age,
            psa = psa,
            familyHistory = familyHistory,
            dre = dre,
            priorBiopsy = priorBiopsy,
            detailedFamilyHistoryEnabled = detailedFamilyHistoryEnabled ?: false,
            fdrPcLess60 = fdrPcLess60,
            fdrPc60 = fdrPc60,
            fdrBc = fdrBc,
            sdr = sdr,
            pctFreePsaAvailable = pctFreePsaAvailable ?: false,
            pctFreePsa = pctFreePsa,
            pca3Available = pca3Available ?: false,
            pca3 = pca3,
            t2ergAvailable = t2ergAvailable ?: false,
            t2erg = t2erg,
            snpsEnabled = snpsEnabled ?: false,
            snpGenotypes = snpGenotypes
                ?.map { SnpGenotype(it.snpIndex, it.riskAlleles) }
                ?: emptyList(),
                prostateVolumeCc = prostateVolumeCc,
                mriPiradsScore = mriPiradsScore,
                dreVolumeClassCc = dreVolumeClassCc,
                gleasonScoreLegacy = gleasonScoreLegacy,
                biopsyCancerLengthMm = biopsyCancerLengthMm,
                biopsyBenignLengthMm = biopsyBenignLengthMm,
                smokingStatus = smokingStatus ?: SmokingStatusOption.NON_SMOKER,
                diabetesType = diabetesType ?: DiabetesTypeOption.NONE,
                manicSchizophrenia = manicSchizophrenia ?: false,
                heightCm = heightCm,
                weightKg = weightKg,
                qcancerYears = qcancerYears ?: 10,
        )
}
