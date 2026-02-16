package de.umra.risk.pcptrc

import de.umra.risk.model.RiskResult
import de.umra.risk.model.SnpGenotype
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

private data class ModelInput(
    val psa: Double,
    val pctFreePsa: Double?,
    val pca3: Double?,
    val t2erg: Double?,
    val age: Double,
    val race: Double,
    val priorBiopsy: Double?,
    val dre: Double?,
    val famHistory: Double?,
    val fdrPcLess60: Int?,
    val fdrPc60: Int?,
    val fdrBc: Int?,
    val sdr: Int?,
    val snps: List<SnpGenotype>,
)

class PcptrcRiskModel {
    fun calculate(
        psa: Double,
        pctFreePsa: Double?,
        pca3: Double?,
        t2erg: Double?,
        age: Double,
        race: Double,
        priorBiopsy: Double?,
        dre: Double?,
        famHistory: Double?,
        fdrPcLess60: Int?,
        fdrPc60: Int?,
        fdrBc: Int?,
        sdr: Int?,
        snps: List<SnpGenotype>,
    ): RiskResult {
        val input = ModelInput(
            psa = psa,
            pctFreePsa = pctFreePsa,
            pca3 = pca3,
            t2erg = t2erg,
            age = age,
            race = race,
            priorBiopsy = priorBiopsy,
            dre = dre,
            famHistory = if (fdrPcLess60 != null && fdrPc60 != null && fdrBc != null) 0.0 else famHistory,
            fdrPcLess60 = fdrPcLess60,
            fdrPc60 = fdrPc60,
            fdrBc = fdrBc,
            sdr = sdr,
            snps = snps,
        )

        return calculateRisk(input)
    }

    private fun calculateRisk(input: ModelInput): RiskResult {
        val data = mutableListOf(1.0, log2(input.psa), input.age, input.race)

        val hasPrior = input.priorBiopsy != null
        if (hasPrior) {
            input.priorBiopsy?.let { data.add(it) }
        }

        val hasDre = input.dre != null
        if (hasDre) {
            input.dre?.let { data.add(it) }
        }

        val hasFam = input.famHistory != null
        if (hasFam) {
            input.famHistory?.let { data.add(it) }
        }

        val (noLow, noHigh) = coefficients(hasPrior, hasDre, hasFam)

        var riskNo = 1.0 / (1.0 + exp(dot(noLow, data)) + exp(dot(noHigh, data)))
        var riskLow = exp(dot(noLow, data)) / (1.0 + exp(dot(noLow, data)) + exp(dot(noHigh, data)))
        var riskHigh = 1.0 - riskNo - riskLow

        if (input.pctFreePsa != null) {
            val coefHigh = doubleArrayOf(4.8059009, -0.3483031)
            val coefLow = doubleArrayOf(4.9730353, -0.3856204)
            val coefNo = doubleArrayOf(4.9602101, -0.2195069)

            val probHigh = dnorm(log2(input.pctFreePsa), dot(coefHigh, listOf(1.0, log2(input.psa))), 0.6452902)
            val probLow = dnorm(log2(input.pctFreePsa), dot(coefLow, listOf(1.0, log2(input.psa))), 0.6010974)
            val probNo = dnorm(log2(input.pctFreePsa), dot(coefNo, listOf(1.0, log2(input.psa))), 0.5154306)

            val updateHigh = riskHigh * probHigh / (riskNo * probNo + riskLow * probLow)
            val updateLow = riskLow * probLow / (riskNo * probNo + riskHigh * probHigh)
            val updateNo = riskNo * probNo / (riskLow * probLow + riskHigh * probHigh)

            riskLow = updateLow / (updateLow + 1.0)
            riskNo = updateNo / (updateNo + 1.0)
            riskHigh = 1.0 - riskLow - riskNo
        }

        if (input.pca3 != null) {
            val coefHigh = doubleArrayOf(0.346 + 1.222, 0.058, 0.733)
            val coefLow = doubleArrayOf(0.346 + 0.908, 0.058, 0.733)
            val coefNo = doubleArrayOf(0.346, 0.058, 0.733)
            var probHigh = dnorm(log2(input.pca3), dot(coefHigh, listOf(1.0, input.age, input.race)), 1.51)
            var probLow = dnorm(log2(input.pca3), dot(coefLow, listOf(1.0, input.age, input.race)), 1.51)
            var probNo = dnorm(log2(input.pca3), dot(coefNo, listOf(1.0, input.age, input.race)), 1.51)

            if (input.t2erg != null) {
                val logisticHigh = doubleArrayOf(0.2 + 0.46, 0.18, -0.507)
                val logisticLow = doubleArrayOf(0.2 + 0.60, 0.18, -0.507)
                val logisticNo = doubleArrayOf(0.2, 0.18, -0.507)

                val nonZero = if (input.t2erg != 0.0) 1 else 0
                val linearHigh = dot(logisticHigh, listOf(1.0, log2(input.pca3), input.race))
                val linearLow = dot(logisticLow, listOf(1.0, log2(input.pca3), input.race))
                val linearNo = dot(logisticNo, listOf(1.0, log2(input.pca3), input.race))

                val binomHigh = exp(linearHigh) / (1.0 + exp(linearHigh))
                val binomLow = exp(linearLow) / (1.0 + exp(linearLow))
                val binomNo = exp(linearNo) / (1.0 + exp(linearNo))

                probHigh *= dbinom(nonZero, binomHigh)
                probLow *= dbinom(nonZero, binomLow)
                probNo *= dbinom(nonZero, binomNo)

                if (nonZero == 1) {
                    val sd = 2.87
                    val t2CoefHigh = doubleArrayOf(0.911 + 2.037, 0.398)
                    val t2CoefLow = doubleArrayOf(0.911 + 0.971, 0.398)
                    val t2CoefNo = doubleArrayOf(0.911, 0.511)

                    probHigh *= dnorm(log2(input.t2erg), dot(t2CoefHigh, listOf(1.0, log2(input.pca3))), sd)
                    probLow *= dnorm(log2(input.t2erg), dot(t2CoefLow, listOf(1.0, log2(input.pca3))), sd)
                    probNo *= dnorm(log2(input.t2erg), dot(t2CoefNo, listOf(1.0, log2(input.pca3))), sd)
                }
            }

            val updateHigh = riskHigh * probHigh / (riskNo * probNo + riskLow * probLow)
            val updateLow = riskLow * probLow / (riskNo * probNo + riskHigh * probHigh)
            val updateNo = riskNo * probNo / (riskLow * probLow + riskHigh * probHigh)

            riskLow = updateLow / (updateLow + 1.0)
            riskNo = updateNo / (updateNo + 1.0)
            riskHigh = 1.0 - riskLow - riskNo
        }

        var flag = false
        var riskCancer = riskLow + riskHigh

        if (input.fdrPcLess60 != null && input.fdrPc60 != null && input.fdrBc != null) {
            flag = true
            val lr = detailedFamilyLikelihoodRatio(input.fdrPcLess60, input.fdrPc60, input.fdrBc, input.sdr ?: 0)
            val priorOdds = riskCancer / riskNo
            riskCancer = (lr * priorOdds) / (lr * priorOdds + 1.0)
            riskNo = 1.0 / (1.0 + lr * priorOdds)
        }

        if (input.snps.isNotEmpty()) {
            if (!flag) {
                riskCancer = riskLow + riskHigh
            }
            flag = true

            val lr0 = doubleArrayOf(
                0.949, 0.959, 1.0, 0.977, 0.960, 0.874, 0.868, 0.937, 0.939, 0.881,
                0.922, 0.806, 0.955, 0.948, 0.899, 0.818, 0.917, 0.861, 1.0, 0.927,
                0.848, 0.824, 0.814, 0.863, 0.890, 0.880, 0.800, 1.0, 0.851, 0.865,
            )
            val lr1 = doubleArrayOf(
                1.073, 1.062, 0.779, 1.066, 1.0, 0.945, 1.0, 1.0, 1.0, 1.0,
                1.0, 1.0, 1.575, 1.129, 0.971, 1.0, 1.307, 1.0, 1.0, 1.118,
                1.0, 0.929, 1.0, 1.0, 1.0, 0.934, 0.890, 1.0, 1.0, 1.044,
            )
            val lr2 = doubleArrayOf(
                1.179, 1.164, 1.031, 1.407, 1.0, 1.093, 1.101, 1.0, 1.100, 1.123,
                1.105, 1.069, 2.552, 1.355, 1.067, 1.188, 1.887, 1.291, 1.0, 1.291,
                1.173, 1.044, 1.194, 1.226, 1.109, 1.055, 1.044, 1.0, 1.161, 1.262,
            )

            var lr = 1.0
            input.snps.forEach { snp ->
                val idx = snp.snpIndex - 1
                lr *= when (snp.riskAlleles) {
                    0 -> lr0[idx]
                    1 -> lr1[idx]
                    else -> lr2[idx]
                }
            }

            val priorOdds = riskCancer / riskNo
            riskCancer = (lr * priorOdds) / (lr * priorOdds + 1.0)
            riskNo = 1.0 / (1.0 + lr * priorOdds)
        }

        return if (!flag) {
            val noRounded = (riskNo * 100.0).roundToInt()
            val lowRounded = (riskLow * 100.0).roundToInt()
            val highRounded = 100 - noRounded - lowRounded
            RiskResult(
                noCancerRisk = noRounded,
                lowGradeRisk = lowRounded,
                highGradeRisk = highRounded,
                cancerRisk = lowRounded + highRounded,
                grouped = false,
            )
        } else {
            var noRounded = (riskNo * 100.0).roundToInt()
            var cancerRounded = (riskCancer * 100.0).roundToInt()

            if (cancerRounded > 75) {
                cancerRounded = 75
                noRounded = 25
            }
            noRounded = 100 - cancerRounded

            RiskResult(
                noCancerRisk = noRounded,
                lowGradeRisk = null,
                highGradeRisk = null,
                cancerRisk = cancerRounded,
                grouped = true,
            )
        }
    }

    private fun coefficients(hasPrior: Boolean, hasDre: Boolean, hasFam: Boolean): Pair<DoubleArray, DoubleArray> {
        return when {
            hasPrior && hasDre && hasFam -> Pair(
                doubleArrayOf(-3.00215469, 0.25613390, 0.01643637, 0.12172599, -0.45533257, -0.03864628, 0.27197219),
                doubleArrayOf(-7.05304534, 0.70489441, 0.04753804, 1.04174529, -0.21409933, 0.40068434, 0.22467348),
            )

            hasPrior && hasDre && !hasFam -> Pair(
                doubleArrayOf(-2.89648245, 0.25904098, 0.01559192, 0.11996693, -0.45444000, -0.03729244),
                doubleArrayOf(-6.96119633, 0.70674359, 0.04676393, 1.03937720, -0.21100921, 0.40319606),
            )

            hasPrior && !hasDre && hasFam -> Pair(
                doubleArrayOf(-3.01529063, 0.25578861, 0.01654912, 0.12327661, -0.45825158, 0.27183869),
                doubleArrayOf(-6.94522156, 0.70637260, 0.04697087, 1.02065099, -0.18320006, 0.23044734),
            )

            hasPrior && !hasDre && !hasFam -> Pair(
                doubleArrayOf(-2.90917471, 0.25872451, 0.01570165, 0.12141077, -0.45729181),
                doubleArrayOf(-6.85264083, 0.70797314, 0.04621214, 1.01887797, -0.17972927),
            )

            !hasPrior && hasDre && hasFam -> Pair(
                doubleArrayOf(-2.90933651, 0.23803667, 0.01447269, 0.11443251, -0.06592322, 0.27128248),
                doubleArrayOf(-6.99449483, 0.69530025, 0.04637911, 1.03847001, 0.38651649, 0.22287791),
            )

            !hasPrior && hasDre && !hasFam -> Pair(
                doubleArrayOf(-2.80429793, 0.24127801, 0.01363705, 0.11165777, -0.06487585),
                doubleArrayOf(-6.90681925, 0.69720305, 0.04566552, 1.03622425, 0.38925765),
            )

            !hasPrior && !hasDre && hasFam -> Pair(
                doubleArrayOf(-2.92983751, 0.23714373, 0.01463232, 0.11765430, 0.27095959),
                doubleArrayOf(-6.90439295, 0.69799874, 0.04608130, 1.01787561, 0.22913998),
            )

            else -> Pair(
                doubleArrayOf(-2.81814489, 0.24044370, 0.01370219, 0.12000825),
                doubleArrayOf(-6.84249970, 0.70043815, 0.04574460, 1.01699029),
            )
        }
    }

    private fun detailedFamilyLikelihoodRatio(fdrLess60: Int, fdr60: Int, fdrBc: Int, sdr: Int): Double {
        if (fdrLess60 == 0) {
            if (fdr60 == 0) {
                var lr = if (fdrBc == 0) 0.92 else 0.90
                if (sdr >= 1) {
                    lr *= 1.09
                }
                return lr
            }
            if (fdr60 == 1) {
                return if (fdrBc == 0) 1.63 else 1.87
            }
            if (fdr60 >= 2) {
                return if (fdrBc == 0) 3.46 else 3.72
            }
        }

        if (fdrLess60 == 1) {
            if (fdr60 == 0) {
                return if (fdrBc == 0) 2.47 else 2.30
            }
            if (fdr60 == 1) {
                return if (fdrBc == 0) 3.65 else 6.59
            }
            if (fdr60 >= 2) {
                return 6.28
            }
        }

        if (fdrLess60 >= 2) {
            if (fdr60 == 0) {
                return if (fdrBc == 0) 5.68 else 7.71
            }
            if (fdr60 == 1) {
                return if (fdrBc == 0) 9.92 else 11.57
            }
            if (fdr60 >= 2) {
                return if (fdrBc == 0) 21.21 else 15.91
            }
        }

        return 1.0
    }

    private fun dot(coefficients: DoubleArray, values: List<Double>): Double =
        coefficients.zip(values).sumOf { (coefficient, value) -> coefficient * value }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

    private fun dnorm(value: Double, mean: Double, sd: Double): Double {
        val exponent = -0.5 * ((value - mean) / sd) * ((value - mean) / sd)
        return (1.0 / (sd * sqrt(2.0 * PI))) * exp(exponent)
    }

    private fun dbinom(nonZero: Int, probability: Double): Double {
        return if (nonZero == 1) probability else (1.0 - probability)
    }
}
