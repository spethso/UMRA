package de.umra.risk.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.RiskAnalysisResponse
import de.umra.risk.model.SavedAnalysisSession
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AnalysisSessionService(
    private val repository: AnalysisSessionRepository,
    private val objectMapper: ObjectMapper,
) {
    fun save(
        input: ProstateCancerRiskInput,
        analyzerIds: List<String>?,
        response: RiskAnalysisResponse,
    ): SavedAnalysisSession {
        val entity = AnalysisSessionEntity(
            id = UUID.randomUUID(),
            inputJson = objectMapper.writeValueAsString(input),
            selectedAnalyzerIds = objectMapper.writeValueAsString(analyzerIds ?: emptyList<String>()),
            resultJson = objectMapper.writeValueAsString(response),
            createdAt = Instant.now(),
        )
        val saved = repository.save(entity)
        return toModel(saved, input, analyzerIds, response)
    }

    fun findById(id: UUID): SavedAnalysisSession? {
        val entity = repository.findById(id).orElse(null) ?: return null
        val input = objectMapper.readValue(entity.inputJson, ProstateCancerRiskInput::class.java)
        val analyzerIds = objectMapper.readValue(
            entity.selectedAnalyzerIds,
            objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java),
        ) as List<String>
        val response = objectMapper.readValue(entity.resultJson, RiskAnalysisResponse::class.java)
        return toModel(entity, input, analyzerIds, response)
    }

    private fun toModel(
        entity: AnalysisSessionEntity,
        input: ProstateCancerRiskInput,
        analyzerIds: List<String>?,
        response: RiskAnalysisResponse,
    ): SavedAnalysisSession = SavedAnalysisSession(
        sessionId = entity.id.toString(),
        input = input,
        selectedAnalyzerIds = analyzerIds ?: emptyList(),
        result = response,
        createdAt = entity.createdAt.toString(),
    )
}
