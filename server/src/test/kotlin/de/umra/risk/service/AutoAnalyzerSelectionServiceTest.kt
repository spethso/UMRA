package de.umra.risk.service

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AutoAnalyzerSelectionServiceTest {

    @Autowired
    private lateinit var autoAnalyzerSelectionService: AutoAnalyzerSelectionService

    private val fullInput = ProstateCancerRiskInput(
        race = Race.CAUCASIAN,
        age = 65,
        psa = 4.2,
        familyHistory = FamilyHistoryOption.NO,
        dre = DreOption.NORMAL,
        priorBiopsy = PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY,
        prostateVolumeCc = 35.0,
        mriPiradsScore = 3,
    )

    @Test
    fun `all analyzers recommended when all data is present`() {
        val ids = autoAnalyzerSelectionService.recommendIds(fullInput)
        assertTrue(ids.contains("PCPTRC"), "Should include PCPTRC")
        assertTrue(ids.contains("SWOP_RC2"), "Should include SWOP_RC2")
        assertTrue(ids.contains("UCLA_PCRC_MRI"), "Should include UCLA_PCRC_MRI when MRI data is present")
    }

    @Test
    fun `UCLA_PCRC_MRI excluded when prostateVolumeCc is null`() {
        val input = fullInput.copy(prostateVolumeCc = null)
        val ids = autoAnalyzerSelectionService.recommendIds(input)
        assertFalse(ids.contains("UCLA_PCRC_MRI"), "UCLA_PCRC_MRI should be excluded without prostate volume")
        assertTrue(ids.contains("PCPTRC"), "Core analyzers should still be present")
    }

    @Test
    fun `UCLA_PCRC_MRI excluded when mriPiradsScore is null`() {
        val input = fullInput.copy(mriPiradsScore = null)
        val ids = autoAnalyzerSelectionService.recommendIds(input)
        assertFalse(ids.contains("UCLA_PCRC_MRI"), "UCLA_PCRC_MRI should be excluded without PI-RADS score")
        assertTrue(ids.contains("PCPTRC"), "Core analyzers should still be present")
    }
}
