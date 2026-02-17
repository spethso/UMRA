package de.umra.risk.graphql

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.RiskAnalysisResponse
import de.umra.risk.service.RiskAggregationService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class RiskAnalysisController(
    private val riskAggregationService: RiskAggregationService,
) {
    @QueryMapping
    fun analyzers(): List<AnalyzerInfo> = riskAggregationService.availableAnalyzers()

    @MutationMapping
    fun analyzeProstateCancerRisk(
        @Argument input: ProstateCancerRiskInput,
        @Argument analyzerIds: List<String>?,
    ): RiskAnalysisResponse =
        riskAggregationService.analyze(input, analyzerIds)
}
