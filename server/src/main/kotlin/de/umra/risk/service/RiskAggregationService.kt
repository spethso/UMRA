package de.umra.risk.service

import de.umra.risk.model.AggregateRiskResult
import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.RiskAnalysisResponse
import de.umra.risk.model.SnpGenotype
import org.springframework.stereotype.Service

@Service
class RiskAggregationService(
    private val analyzers: List<RiskAnalyzer>,
) {
    fun availableAnalyzers(): List<AnalyzerInfo> = analyzers.map { it.metadata() }

    fun analyze(input: ProstateCancerRiskInput, analyzerIds: List<String>? = null): RiskAnalysisResponse {
        val request = input.toRequest()
        val selectedIds = analyzerIds
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()

        val selectedAnalyzers = if (selectedIds.isEmpty()) {
            analyzers
        } else {
            analyzers.filter { analyzer -> selectedIds.contains(analyzer.metadata().analyzerId) }
        }

        val analyzerResults = selectedAnalyzers.map { analyzer ->
            runCatching { analyzer.analyze(request) }
                .getOrElse { exception ->
                    val metadata = analyzer.metadata()
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
                dreVolumeClassCc = dreVolumeClassCc,
                gleasonScoreLegacy = gleasonScoreLegacy,
                biopsyCancerLengthMm = biopsyCancerLengthMm,
                biopsyBenignLengthMm = biopsyBenignLengthMm,
        )
}
