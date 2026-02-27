package de.umra.risk.service

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class RiskAggregationServiceTest {

    @Autowired
    private lateinit var riskAggregationService: RiskAggregationService

    private val standardInput = ProstateCancerRiskInput(
        race = Race.CAUCASIAN,
        age = 65,
        psa = 4.2,
        familyHistory = FamilyHistoryOption.NO,
        dre = DreOption.NORMAL,
        priorBiopsy = PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY,
    )

    @Test
    fun `availableAnalyzers returns metadata from all injected analyzers`() {
        val analyzers = riskAggregationService.availableAnalyzers()
        assertTrue(analyzers.isNotEmpty(), "Should have at least one analyzer")
        assertTrue(analyzers.any { it.analyzerId == "PCPTRC" }, "Should contain PCPTRC")
        assertTrue(analyzers.any { it.analyzerId == "SWOP_RC2" }, "Should contain SWOP_RC2")
        analyzers.forEach { info ->
            assertTrue(info.analyzerId.isNotBlank(), "Analyzer id should not be blank")
            assertTrue(info.displayName.isNotBlank(), "Display name should not be blank")
            assertTrue(info.sourceUrl.isNotBlank(), "Source URL should not be blank")
        }
    }

    @Test
    fun `analyze with specific analyzerIds returns results only for those analyzers`() {
        val response = riskAggregationService.analyze(standardInput, listOf("PCPTRC"))
        assertEquals(1, response.analyzers.size, "Should only have one analyzer result")
        assertEquals("PCPTRC", response.analyzers[0].analyzerId)
        assertTrue(response.analyzers[0].success)
        assertNotNull(response.analyzers[0].risk)
    }

    @Test
    fun `analyze with null analyzerIds triggers auto-selection`() {
        val response = riskAggregationService.analyze(standardInput, null)
        // Auto-selection with standard input (no prostate volume / MRI) should exclude UCLA_PCRC_MRI
        assertTrue(response.analyzers.isNotEmpty(), "Auto-selection should return at least one analyzer")
        assertFalse(
            response.analyzers.any { it.analyzerId == "UCLA_PCRC_MRI" },
            "UCLA_PCRC_MRI should be excluded when MRI data is missing",
        )
    }

    @Test
    fun `analyze with empty analyzerIds list returns all analyzers`() {
        val allAnalyzerIds = riskAggregationService.availableAnalyzers().map { it.analyzerId }
        val response = riskAggregationService.analyze(standardInput, emptyList())
        assertEquals(allAnalyzerIds.size, response.analyzers.size, "Empty list should run all analyzers")
    }

    @Test
    fun `analyze when an analyzer throws returns result with success false and error message`() {
        // UCLA_PCRC_MRI requires prostateVolumeCc and mriPiradsScore — omitting them causes failure
        val response = riskAggregationService.analyze(standardInput, listOf("UCLA_PCRC_MRI"))
        assertEquals(1, response.analyzers.size)
        val uclaResult = response.analyzers[0]
        assertFalse(uclaResult.success, "Should not succeed without MRI data")
        assertNotNull(uclaResult.warning, "Should contain an error message")
        assertNull(uclaResult.risk, "Risk should be null on failure")
    }

    @Test
    fun `aggregate logic averages multiple successful results`() {
        val response = riskAggregationService.analyze(standardInput, listOf("PCPTRC", "SWOP_RC2"))
        val agg = response.aggregate
        val successfulCount = response.analyzers.count { it.success }
        assertEquals(successfulCount, agg.basedOnAnalyzers, "basedOnAnalyzers should match successful count")
        assertTrue(agg.noCancerRisk in 0..100, "Aggregated no-cancer risk should be 0..100")
    }

    @Test
    fun `aggregate with no successful results returns zeros`() {
        // Request UCLA_PCRC_MRI without required fields — it will fail
        val response = riskAggregationService.analyze(standardInput, listOf("UCLA_PCRC_MRI"))
        val agg = response.aggregate
        assertEquals(0, agg.basedOnAnalyzers)
        assertEquals(0, agg.noCancerRisk)
        assertNull(agg.lowGradeRisk)
        assertNull(agg.highGradeRisk)
        assertNull(agg.cancerRisk)
    }
}
