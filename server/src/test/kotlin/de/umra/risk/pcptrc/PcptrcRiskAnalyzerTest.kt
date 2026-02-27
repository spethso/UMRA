package de.umra.risk.pcptrc

import de.umra.risk.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PcptrcRiskAnalyzerTest {

    @Autowired
    private lateinit var pcptrcRiskAnalyzer: PcptrcRiskAnalyzer

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
        ukPostcode = null,
        smokingStatus = SmokingStatusOption.NON_SMOKER,
        diabetesType = DiabetesTypeOption.NONE,
        manicSchizophrenia = false,
        heightCm = null,
        weightKg = null,
        qcancerYears = 10,
    )

    @Test
    fun `valid input produces expected structure`() {
        val result = pcptrcRiskAnalyzer.analyze(standardRequest)
        assertEquals("PCPTRC", result.analyzerId)
        assertTrue(result.success)
        assertNotNull(result.risk)
        assertTrue(result.risk!!.noCancerRisk in 0..100)
    }

    @Test
    fun `age below 55 throws exception`() {
        val request = standardRequest.copy(age = 50)
        val ex = assertThrows<IllegalArgumentException> {
            pcptrcRiskAnalyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("Age"), "Error should mention age")
    }

    @Test
    fun `PSA out of range throws exception`() {
        val request = standardRequest.copy(psa = 0.0)
        val ex = assertThrows<IllegalArgumentException> {
            pcptrcRiskAnalyzer.analyze(request)
        }
        assertTrue(ex.message!!.contains("PSA"), "Error should mention PSA")
    }

    @Test
    fun `pctFreePsa and pca3 combined throws exception`() {
        val request = standardRequest.copy(
            pctFreePsaAvailable = true,
            pctFreePsa = 25.0,
            pca3Available = true,
            pca3 = 35.0,
        )
        val ex = assertThrows<IllegalArgumentException> {
            pcptrcRiskAnalyzer.analyze(request)
        }
        assertTrue(
            ex.message!!.contains("free PSA") || ex.message!!.contains("PCA3"),
            "Error should mention the mutual exclusion",
        )
    }
}
