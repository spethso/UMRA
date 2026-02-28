package de.umra.risk.swop

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SwopRc2RiskAnalyzerTest {

    private val analyzer = SwopRc2RiskAnalyzer()

    private val standardRequest = ProstateCancerRiskRequest(
        race = Race.CAUCASIAN,
        age = 65,
        psa = 4.2,
        familyHistory = FamilyHistoryOption.NO,
        dre = DreOption.NORMAL,
        priorBiopsy = PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY,
        detailedFamilyHistoryEnabled = false,
        fdrPcLess60 = null,
        fdrPc60 = null,
        fdrBc = null,
        sdr = null,
        pctFreePsaAvailable = false,
        pctFreePsa = null,
        pca3Available = false,
        pca3 = null,
        t2ergAvailable = false,
        t2erg = null,
        snpsEnabled = false,
        snpGenotypes = emptyList(),
        prostateVolumeCc = null,
        mriPiradsScore = null,
        dreVolumeClassCc = null,
        gleasonScoreLegacy = null,
        biopsyCancerLengthMm = null,
        biopsyBenignLengthMm = null,
        smokingStatus = SmokingStatusOption.NON_SMOKER,
        diabetesType = DiabetesTypeOption.NONE,
        manicSchizophrenia = false,
        heightCm = null,
        weightKg = null,
        qcancerYears = 10,
    )

    @Test
    fun `valid PSA returns valid risk`() {
        val result = analyzer.analyze(standardRequest)
        assertEquals("SWOP_RC2", result.analyzerId)
        assertTrue(result.success)
        assertNotNull(result.risk)
        val risk = result.risk!!
        assertTrue(risk.noCancerRisk in 0..100)
        assertNotNull(risk.cancerRisk)
        assertEquals(100, risk.noCancerRisk + (risk.cancerRisk ?: 0), "No-cancer + cancer should sum to 100")
    }

    @Test
    fun `PSA below 0_4 throws`() {
        val request = standardRequest.copy(psa = 0.3)
        val ex = assertThrows<IllegalArgumentException> {
            analyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("PSA"), "Error should mention PSA")
    }
}
