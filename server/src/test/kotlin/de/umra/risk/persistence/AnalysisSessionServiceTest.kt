package de.umra.risk.persistence

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest
class AnalysisSessionServiceTest {

    @Autowired
    private lateinit var analysisSessionService: AnalysisSessionService

    private val standardInput = ProstateCancerRiskInput(
        race = Race.CAUCASIAN,
        age = 65,
        psa = 4.2,
        familyHistory = FamilyHistoryOption.NO,
        dre = DreOption.NORMAL,
        priorBiopsy = PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY,
    )

    private val sampleResponse = RiskAnalysisResponse(
        analyzers = listOf(
            AnalyzerRiskResult(
                analyzerId = "PCPTRC",
                displayName = "PCPTRC 2.0",
                sourceUrl = "https://riskcalc.org",
                forwardedOnline = false,
                success = true,
                warning = null,
                risk = RiskResult(
                    noCancerRisk = 70,
                    lowGradeRisk = 20,
                    highGradeRisk = 10,
                    cancerRisk = 30,
                    grouped = false,
                ),
            ),
        ),
        aggregate = AggregateRiskResult(
            noCancerRisk = 70,
            lowGradeRisk = 20,
            highGradeRisk = 10,
            cancerRisk = 30,
            basedOnAnalyzers = 1,
        ),
    )

    @Test
    fun `save persists and returns a SavedAnalysisSession with valid sessionId`() {
        val saved = analysisSessionService.save(standardInput, listOf("PCPTRC"), sampleResponse, false)
        assertNotNull(saved.sessionId)
        assertDoesNotThrow { UUID.fromString(saved.sessionId) }
        assertNotNull(saved.createdAt)
    }

    @Test
    fun `findById returns null for non-existent id`() {
        val result = analysisSessionService.findById(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun `findById returns the saved session`() {
        val saved = analysisSessionService.save(standardInput, listOf("PCPTRC"), sampleResponse, false)
        val found = analysisSessionService.findById(UUID.fromString(saved.sessionId))
        assertNotNull(found)
        assertEquals(saved.sessionId, found!!.sessionId)
    }

    @Test
    fun `deleteById returns false for non-existent id`() {
        val result = analysisSessionService.deleteById(UUID.randomUUID())
        assertFalse(result)
    }

    @Test
    fun `deleteById returns true and actually deletes`() {
        val saved = analysisSessionService.save(standardInput, listOf("PCPTRC"), sampleResponse, false)
        val id = UUID.fromString(saved.sessionId)
        assertTrue(analysisSessionService.deleteById(id))
        assertNull(analysisSessionService.findById(id))
    }

    @Test
    fun `save correctly serializes and findById correctly deserializes the input and result`() {
        val saved = analysisSessionService.save(standardInput, listOf("PCPTRC"), sampleResponse, true)
        val loaded = analysisSessionService.findById(UUID.fromString(saved.sessionId))
        assertNotNull(loaded)
        assertEquals(standardInput.race, loaded!!.input.race)
        assertEquals(standardInput.age, loaded.input.age)
        assertEquals(standardInput.psa, loaded.input.psa)
        assertEquals(standardInput.familyHistory, loaded.input.familyHistory)
        assertEquals(standardInput.dre, loaded.input.dre)
        assertEquals(standardInput.priorBiopsy, loaded.input.priorBiopsy)
        assertEquals(listOf("PCPTRC"), loaded.selectedAnalyzerIds)
        assertTrue(loaded.autoMode)
        assertEquals(1, loaded.result.analyzers.size)
        assertEquals("PCPTRC", loaded.result.analyzers[0].analyzerId)
        assertEquals(70, loaded.result.aggregate.noCancerRisk)
        assertEquals(1, loaded.result.aggregate.basedOnAnalyzers)
    }
}
