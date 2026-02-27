package de.umra.risk.pcptrc

import de.umra.risk.model.SnpGenotype
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PcptrcRiskModelTest {

    private val model = PcptrcRiskModel()

    @Test
    fun `basic calculation with standard inputs returns valid risk percentages`() {
        val result = model.calculate(
            psa = 4.2,
            pctFreePsa = null,
            pca3 = null,
            t2erg = null,
            age = 65.0,
            race = 0.0,
            priorBiopsy = 0.0,
            dre = 0.0,
            famHistory = 0.0,
            fdrPcLess60 = null,
            fdrPc60 = null,
            fdrBc = null,
            sdr = null,
            snps = emptyList(),
        )
        assertTrue(result.noCancerRisk >= 0, "No-cancer risk should be >= 0")
        assertNotNull(result.lowGradeRisk)
        assertNotNull(result.highGradeRisk)
        val sum = result.noCancerRisk + (result.lowGradeRisk ?: 0) + (result.highGradeRisk ?: 0)
        assertEquals(100, sum, "Risk percentages should sum to 100")
        assertFalse(result.grouped, "Standard calculation should not be grouped")
    }

    @Test
    fun `calculation with pctFreePsa input`() {
        val result = model.calculate(
            psa = 4.2,
            pctFreePsa = 25.0,
            pca3 = null,
            t2erg = null,
            age = 65.0,
            race = 0.0,
            priorBiopsy = 0.0,
            dre = 0.0,
            famHistory = 0.0,
            fdrPcLess60 = null,
            fdrPc60 = null,
            fdrBc = null,
            sdr = null,
            snps = emptyList(),
        )
        assertTrue(result.noCancerRisk >= 0)
        val sum = result.noCancerRisk + (result.lowGradeRisk ?: 0) + (result.highGradeRisk ?: 0)
        assertEquals(100, sum, "Risk percentages should sum to 100 with pctFreePsa")
    }

    @Test
    fun `calculation with pca3 input`() {
        val result = model.calculate(
            psa = 4.2,
            pctFreePsa = null,
            pca3 = 35.0,
            t2erg = null,
            age = 65.0,
            race = 0.0,
            priorBiopsy = 0.0,
            dre = 0.0,
            famHistory = 0.0,
            fdrPcLess60 = null,
            fdrPc60 = null,
            fdrBc = null,
            sdr = null,
            snps = emptyList(),
        )
        assertTrue(result.noCancerRisk >= 0)
        val sum = result.noCancerRisk + (result.lowGradeRisk ?: 0) + (result.highGradeRisk ?: 0)
        assertEquals(100, sum, "Risk percentages should sum to 100 with PCA3")
    }

    @Test
    fun `calculation with detailed family history`() {
        val result = model.calculate(
            psa = 4.2,
            pctFreePsa = null,
            pca3 = null,
            t2erg = null,
            age = 65.0,
            race = 0.0,
            priorBiopsy = 0.0,
            dre = 0.0,
            famHistory = 0.0,
            fdrPcLess60 = 1,
            fdrPc60 = 0,
            fdrBc = 0,
            sdr = 0,
            snps = emptyList(),
        )
        assertTrue(result.noCancerRisk >= 0)
        assertTrue(result.grouped, "Detailed family history calculation should be grouped")
        assertNotNull(result.cancerRisk, "Should have combined cancer risk")
        assertNull(result.lowGradeRisk, "Grouped result should not have low-grade risk")
        assertNull(result.highGradeRisk, "Grouped result should not have high-grade risk")
        assertEquals(100, result.noCancerRisk + (result.cancerRisk ?: 0), "Should sum to 100")
    }

    @Test
    fun `calculation with SNP genotypes`() {
        val snps = listOf(
            SnpGenotype(snpIndex = 1, riskAlleles = 2),
            SnpGenotype(snpIndex = 5, riskAlleles = 0),
            SnpGenotype(snpIndex = 10, riskAlleles = 1),
        )
        val result = model.calculate(
            psa = 4.2,
            pctFreePsa = null,
            pca3 = null,
            t2erg = null,
            age = 65.0,
            race = 0.0,
            priorBiopsy = 0.0,
            dre = 0.0,
            famHistory = 0.0,
            fdrPcLess60 = null,
            fdrPc60 = null,
            fdrBc = null,
            sdr = null,
            snps = snps,
        )
        assertTrue(result.noCancerRisk >= 0)
        assertTrue(result.grouped, "SNP calculation should be grouped")
        assertNotNull(result.cancerRisk)
        assertEquals(100, result.noCancerRisk + (result.cancerRisk ?: 0), "Should sum to 100")
    }
}
