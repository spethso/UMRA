package de.umra.risk.graphql

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.SavedAnalysisSession
import de.umra.risk.persistence.AnalysisSessionService
import de.umra.risk.service.AutoAnalyzerSelectionService
import de.umra.risk.service.RiskAggregationService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class RiskAnalysisController(
    private val riskAggregationService: RiskAggregationService,
    private val analysisSessionService: AnalysisSessionService,
    private val autoAnalyzerSelectionService: AutoAnalyzerSelectionService,
) {
    @QueryMapping
    fun analyzers(): List<AnalyzerInfo> = riskAggregationService.availableAnalyzers()

    @QueryMapping
    fun recommendedAnalyzers(@Argument input: ProstateCancerRiskInput): List<AnalyzerInfo> =
        autoAnalyzerSelectionService.recommend(input)

    @QueryMapping
    fun session(@Argument sessionId: String): SavedAnalysisSession? =
        analysisSessionService.findById(UUID.fromString(sessionId))

    @MutationMapping
    fun analyzeProstateCancerRisk(
        @Argument input: ProstateCancerRiskInput,
        @Argument analyzerIds: List<String>?,
    ): SavedAnalysisSession {
        val autoMode = analyzerIds == null
        val response = riskAggregationService.analyze(input, analyzerIds)
        val effectiveIds = if (autoMode) {
            response.analyzers.map { it.analyzerId }
        } else {
            analyzerIds
        }
        return analysisSessionService.save(input, effectiveIds, response, autoMode)
    }
}
