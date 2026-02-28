package de.umra.risk.ucla

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UclaPcrcMriRiskAnalyzerTest {

    private val analyzer = UclaPcrcMriRiskAnalyzer()

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
        prostateVolumeCc = 35.0,
        mriPiradsScore = 3,
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
    fun `valid input returns valid structure`() {
        val result = analyzer.analyze(standardRequest)
        assertEquals("UCLA_PCRC_MRI", result.analyzerId)
        assertTrue(result.success)
        assertNotNull(result.risk)
        val risk = result.risk!!
        assertTrue(risk.noCancerRisk in 0..100)
        assertTrue(risk.grouped)
        assertNotNull(risk.highGradeRisk)
        assertNotNull(risk.cancerRisk)
    }

    @Test
    fun `missing prostate volume throws`() {
        val request = standardRequest.copy(prostateVolumeCc = null)
        val ex = assertThrows<IllegalArgumentException> {
            analyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("prostate volume", ignoreCase = true))
    }

    @Test
    fun `missing PIRADS score throws`() {
        val request = standardRequest.copy(mriPiradsScore = null)
        val ex = assertThrows<IllegalArgumentException> {
            analyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("PI-RADS", ignoreCase = true))
    }

    @Test
    fun `PI-RADS 1 is out of range`() {
        val request = standardRequest.copy(mriPiradsScore = 1)
        val ex = assertThrows<IllegalArgumentException> {
            analyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("PI-RADS", ignoreCase = true))
    }
}
