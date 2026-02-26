package de.umra.risk.graphql

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.SavedAnalysisSession
import de.umra.risk.persistence.AnalysisSessionService
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
) {
    @QueryMapping
    fun analyzers(): List<AnalyzerInfo> = riskAggregationService.availableAnalyzers()

    @QueryMapping
    fun session(@Argument sessionId: String): SavedAnalysisSession? =
        analysisSessionService.findById(UUID.fromString(sessionId))

    @MutationMapping
    fun analyzeProstateCancerRisk(
        @Argument input: ProstateCancerRiskInput,
        @Argument analyzerIds: List<String>?,
    ): SavedAnalysisSession {
        val response = riskAggregationService.analyze(input, analyzerIds)
        return analysisSessionService.save(input, analyzerIds, response)
    }
}
