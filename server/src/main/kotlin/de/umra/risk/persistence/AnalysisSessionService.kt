package de.umra.risk.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.RiskAnalysisResponse
import de.umra.risk.model.SavedAnalysisSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Service responsible for persisting and retrieving analysis sessions.
 */
@Service
class AnalysisSessionService(
    private val repository: AnalysisSessionRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(AnalysisSessionService::class.java)

    /**
     * Persists a new analysis session and returns its model representation.
     *
     * @param input       the patient data that was analyzed
     * @param analyzerIds analyzer ids that were used
     * @param response    the risk-analysis response to store
     * @param autoMode    whether analyzers were auto-selected
     * @return [SavedAnalysisSession] with the generated session id
     * @throws IllegalArgumentException if the serialized input JSON is blank
     */
    fun save(
        input: ProstateCancerRiskInput,
        analyzerIds: List<String>?,
        response: RiskAnalysisResponse,
        autoMode: Boolean = false,
    ): SavedAnalysisSession {
        val inputJson = objectMapper.writeValueAsString(input)
        require(inputJson.isNotBlank()) { "Serialized input JSON must not be blank" }

        val entity = AnalysisSessionEntity(
            id = UUID.randomUUID(),
            inputJson = inputJson,
            selectedAnalyzerIds = objectMapper.writeValueAsString(analyzerIds ?: emptyList<String>()),
            autoMode = autoMode,
            resultJson = objectMapper.writeValueAsString(response),
            createdAt = Instant.now(),
        )
        val saved = repository.save(entity)
        logger.info("Saved analysis session id={}", saved.id)
        return toModel(saved, input, analyzerIds, response)
    }

    /**
     * Deletes the session with the given id.
     *
     * @param id session UUID
     * @return `true` if the session existed and was deleted, `false` otherwise
     */
    fun deleteById(id: UUID): Boolean {
        if (!repository.existsById(id)) {
            logger.debug("Session id={} not found for deletion", id)
            return false
        }
        repository.deleteById(id)
        logger.info("Deleted analysis session id={}", id)
        return true
    }

    /**
     * Finds a stored session by its id and deserializes the JSON payload.
     *
     * @param id session UUID
     * @return [SavedAnalysisSession], or `null` if no session with the given id exists
     */
    fun findById(id: UUID): SavedAnalysisSession? {
        val entity = repository.findById(id).orElse(null) ?: return null
        logger.debug("Found session id={}", id)
        val input = objectMapper.readValue(entity.inputJson, ProstateCancerRiskInput::class.java)
        val analyzerIds = objectMapper.readValue(
            entity.selectedAnalyzerIds,
            objectMapper.typeFactory.constructCollectionType(List::class.java, String::class.java),
        ) as List<String>
        val response = objectMapper.readValue(entity.resultJson, RiskAnalysisResponse::class.java)
        return toModel(entity, input, analyzerIds, response)
    }

    /**
     * Maps a [AnalysisSessionEntity] together with its deserialized components
     * to a [SavedAnalysisSession] model object.
     */
    private fun toModel(
        entity: AnalysisSessionEntity,
        input: ProstateCancerRiskInput,
        analyzerIds: List<String>?,
        response: RiskAnalysisResponse,
    ): SavedAnalysisSession = SavedAnalysisSession(
        sessionId = entity.id.toString(),
        input = input,
        selectedAnalyzerIds = analyzerIds ?: emptyList(),
        autoMode = entity.autoMode,
        result = response,
        createdAt = entity.createdAt.toString(),
    )
}
