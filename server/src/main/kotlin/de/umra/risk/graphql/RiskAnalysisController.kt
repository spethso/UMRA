package de.umra.risk.graphql

import de.umra.risk.model.AnalysisSessionResponse
import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.ProstateCancerRiskInput
import de.umra.risk.model.RiskAnalysisResponse
import de.umra.risk.model.SavedAnalysisSession
import de.umra.risk.persistence.AnalysisSessionService
import de.umra.risk.service.AutoAnalyzerSelectionService
import de.umra.risk.service.RiskAggregationService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.UUID

/**
 * GraphQL controller that exposes risk-analysis queries and mutations.
 *
 * All session-id parameters are validated as well-formed UUIDs before
 * being forwarded to the persistence layer.
 */
@Controller
class RiskAnalysisController(
    private val riskAggregationService: RiskAggregationService,
    private val analysisSessionService: AnalysisSessionService,
    private val autoAnalyzerSelectionService: AutoAnalyzerSelectionService,
) {
    private val logger = LoggerFactory.getLogger(RiskAnalysisController::class.java)

    /**
     * Returns metadata for every registered risk analyzer.
     *
     * @return list of [AnalyzerInfo] describing each available analyzer
     */
    @QueryMapping
    fun analyzers(): List<AnalyzerInfo> {
        logger.debug("Fetching available analyzers")
        return riskAggregationService.availableAnalyzers()
    }

    /**
     * Returns the analyzers recommended for the given patient input.
     *
     * @param input patient data used to determine applicable analyzers
     * @return list of [AnalyzerInfo] for recommended analyzers
     */
    @QueryMapping
    fun recommendedAnalyzers(@Argument input: ProstateCancerRiskInput): List<AnalyzerInfo> {
        logger.debug("Resolving recommended analyzers for input")
        return autoAnalyzerSelectionService.recommend(input)
    }

    /**
     * Retrieves a previously stored analysis session.
     *
     * @param sessionId UUID string identifying the session
     * @return the saved session, or `null` if not found
     * @throws IllegalArgumentException if [sessionId] is not a valid UUID
     */
    @QueryMapping
    fun session(@Argument sessionId: String): SavedAnalysisSession? {
        val uuid = parseUuid(sessionId)
        logger.debug("Looking up session id={}", uuid)
        return analysisSessionService.findById(uuid)
    }

    /**
     * Runs prostate-cancer risk analysis, optionally stores the result, and
     * returns a session response.
     *
     * @param input      patient clinical data
     * @param analyzerIds explicit analyzer ids to use, or `null` for auto-selection
     * @param storeResult whether to persist the result (`false` by default)
     * @return [AnalysisSessionResponse] containing results and session metadata
     */
    @MutationMapping
    fun analyzeProstateCancerRisk(
        @Argument input: ProstateCancerRiskInput,
        @Argument analyzerIds: List<String>?,
        @Argument storeResult: Boolean?,
    ): AnalysisSessionResponse {
        val autoMode = analyzerIds == null
        logger.info("Analyzing prostate cancer risk – autoMode={}, storeResult={}", autoMode, storeResult)

        val response = riskAggregationService.analyze(input, analyzerIds)
        val effectiveIds = resolveEffectiveAnalyzerIds(analyzerIds, response, autoMode)

        val store = storeResult ?: false
        val sessionId = if (store) {
            val saved = analysisSessionService.save(input, effectiveIds, response, autoMode)
            logger.info("Analysis session stored id={}", saved.sessionId)
            saved.sessionId
        } else {
            null
        }

        return buildResponse(sessionId, effectiveIds, autoMode, store, response)
    }

    /**
     * Deletes a previously stored analysis session.
     *
     * @param sessionId UUID string identifying the session to delete
     * @return `true` if the session existed and was deleted, `false` otherwise
     * @throws IllegalArgumentException if [sessionId] is not a valid UUID
     */
    @MutationMapping
    fun deleteSession(@Argument sessionId: String): Boolean {
        val uuid = parseUuid(sessionId)
        logger.info("Deleting session id={}", uuid)
        return analysisSessionService.deleteById(uuid)
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Determines the effective list of analyzer ids used for the analysis.
     *
     * In auto-mode the ids are derived from the analyzers that actually
     * produced results; otherwise the caller-supplied list is used as-is.
     *
     * @param analyzerIds the caller-supplied ids (may be `null` in auto-mode)
     * @param response    the analysis response containing per-analyzer results
     * @param autoMode    whether auto-selection was active
     * @return ordered list of effective analyzer ids
     */
    private fun resolveEffectiveAnalyzerIds(
        analyzerIds: List<String>?,
        response: RiskAnalysisResponse,
        autoMode: Boolean,
    ): List<String> = if (autoMode) {
        response.analyzers.map { it.analyzerId }
    } else {
        analyzerIds ?: emptyList()
    }

    /**
     * Assembles the [AnalysisSessionResponse] returned to the client.
     *
     * @param sessionId    persisted session id, or `null` when not stored
     * @param effectiveIds analyzer ids that were used
     * @param autoMode     whether auto-selection was active
     * @param stored       whether the result was persisted
     * @param response     the analysis response payload
     * @return fully populated [AnalysisSessionResponse]
     */
    private fun buildResponse(
        sessionId: String?,
        effectiveIds: List<String>,
        autoMode: Boolean,
        stored: Boolean,
        response: RiskAnalysisResponse,
    ): AnalysisSessionResponse = AnalysisSessionResponse(
        sessionId = sessionId,
        selectedAnalyzerIds = effectiveIds,
        autoMode = autoMode,
        stored = stored,
        result = response,
    )

    /**
     * Parses [value] as a UUID, wrapping format errors into an
     * [IllegalArgumentException] with a user-friendly message.
     *
     * @param value the string to parse
     * @return the parsed [UUID]
     * @throws IllegalArgumentException if [value] is not a valid UUID
     */
    private fun parseUuid(value: String): UUID = try {
        UUID.fromString(value)
    } catch (ex: IllegalArgumentException) {
        logger.warn("Invalid UUID received: {}", value)
        throw IllegalArgumentException("'$value' is not a valid session id (expected UUID format)", ex)
    }
}
