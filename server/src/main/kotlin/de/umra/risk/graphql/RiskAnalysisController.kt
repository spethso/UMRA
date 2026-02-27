package de.umra.risk.graphql

import de.umra.risk.model.AnalysisSessionResponse
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
        @Argument storeResult: Boolean?,
    ): AnalysisSessionResponse {
        val autoMode = analyzerIds == null
        val response = riskAggregationService.analyze(input, analyzerIds)
        val effectiveIds = if (autoMode) {
            response.analyzers.map { it.analyzerId }
        } else {
            analyzerIds ?: emptyList()
        }
        val store = storeResult ?: false
        val sessionId = if (store) {
            val saved = analysisSessionService.save(input, effectiveIds, response, autoMode)
            saved.sessionId
        } else {
            null
        }
        return AnalysisSessionResponse(
            sessionId = sessionId,
            selectedAnalyzerIds = effectiveIds,
            autoMode = autoMode,
            stored = store,
            result = response,
        )
    }

    @MutationMapping
    fun deleteSession(@Argument sessionId: String): Boolean =
        analysisSessionService.deleteById(UUID.fromString(sessionId))
}
