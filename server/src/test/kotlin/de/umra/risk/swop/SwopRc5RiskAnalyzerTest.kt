package de.umra.risk.swop

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SwopRc5RiskAnalyzerTest {

    private val analyzer = SwopRc5RiskAnalyzer()

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
        prostateVolumeCc = 40.0,
        mriPiradsScore = null,
        dreVolumeClassCc = null,
        gleasonScoreLegacy = 6,
        biopsyCancerLengthMm = 10.0,
        biopsyBenignLengthMm = 40.0,
        smokingStatus = SmokingStatusOption.NON_SMOKER,
        diabetesType = DiabetesTypeOption.NONE,
        manicSchizophrenia = false,
        heightCm = null,
        weightKg = null,
        qcancerYears = 10,
    )

    @Test
    fun `valid input returns valid risk`() {
        val result = analyzer.analyze(standardRequest)
        assertEquals("SWOP_RC5", result.analyzerId)
        assertTrue(result.success)
        assertNotNull(result.risk)
        val risk = result.risk!!
        assertTrue(risk.noCancerRisk in 0..100)
        assertTrue(risk.grouped)
    }

    @Test
    fun `Gleason 7 is out of range and throws`() {
        val request = standardRequest.copy(gleasonScoreLegacy = 7)
        val ex = assertThrows<IllegalArgumentException> {
            analyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("Gleason"), "Error should mention Gleason")
    }
}
