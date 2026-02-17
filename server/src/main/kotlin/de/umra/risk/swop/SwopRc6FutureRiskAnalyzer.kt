package de.umra.risk.swop

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.DreOption
import de.umra.risk.model.FamilyHistoryOption
import de.umra.risk.model.PriorBiopsyOption
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.RiskResult
import de.umra.risk.service.RiskAnalyzer
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SwopRc6FutureRiskAnalyzer(
    restClientBuilder: RestClient.Builder,
) : RiskAnalyzer {
    private val restClient = restClientBuilder.build()

    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "SWOP_RC6",
        displayName = "SWOP Future Risk Calculator (4-year risk)",
        sourceUrl = "https://www.prostatecancer-riskcalculator.com/2012/index.php",
    )

    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult {
        val volumeClass = request.dreVolumeClassCc?.takeIf { it in setOf(25, 40, 60) } ?: 40

        val responseBody = runCatching {
            restClient.post()
                .uri(metadata().sourceUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(
                    "d11=${request.age}" +
                        "&d12=${request.psa}" +
                        "&d13=${request.dre.toSwopValue()}" +
                        "&d14=${request.familyHistory.toSwopValue()}" +
                        "&d15=$volumeClass" +
                        "&d16=${request.priorBiopsy.toSwopValue()}",
                )
                .retrieve()
                .body(String::class.java)
                ?: ""
        }.getOrElse {
            return AnalyzerRiskResult(
                analyzerId = metadata().analyzerId,
                displayName = metadata().displayName,
                sourceUrl = metadata().sourceUrl,
                forwardedOnline = false,
                success = false,
                warning = "SWOP RC6 request failed: ${it.message}",
                risk = null,
            )
        }

        val noCancer = responseBody.extractPercentById("prob-no-cancer")
        val lowRisk = responseBody.extractPercentById("prob-low-risk")
        val aggressive = responseBody.extractPercentById("prob-aggressive-cancer")

        if (noCancer == null || lowRisk == null || aggressive == null) {
            return AnalyzerRiskResult(
                analyzerId = metadata().analyzerId,
                displayName = metadata().displayName,
                sourceUrl = metadata().sourceUrl,
                forwardedOnline = true,
                success = false,
                warning = "SWOP RC6 response did not contain parsable probability values.",
                risk = null,
            )
        }

        return AnalyzerRiskResult(
            analyzerId = metadata().analyzerId,
            displayName = metadata().displayName,
            sourceUrl = metadata().sourceUrl,
            forwardedOnline = true,
            success = true,
            warning = null,
            risk = RiskResult(
                noCancerRisk = noCancer,
                lowGradeRisk = lowRisk,
                highGradeRisk = aggressive,
                cancerRisk = (lowRisk + aggressive).coerceIn(0, 100),
                grouped = true,
            ),
        )
    }

    private fun String.extractPercentById(id: String): Int? {
        val pattern = Regex("""id=\"$id\"[^>]*>([0-9]+(?:\\.[0-9]+)?)""")
        val value = pattern.find(this)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return value.toInt().coerceIn(0, 100)
    }

    private fun DreOption.toSwopValue(): Int = when (this) {
        DreOption.ABNORMAL -> 1
        DreOption.NORMAL, DreOption.NOT_PERFORMED_OR_NOT_SURE -> 0
    }

    private fun FamilyHistoryOption.toSwopValue(): Int = when (this) {
        FamilyHistoryOption.YES -> 1
        FamilyHistoryOption.NO, FamilyHistoryOption.DO_NOT_KNOW -> 0
    }

    private fun PriorBiopsyOption.toSwopValue(): Int = when (this) {
        PriorBiopsyOption.PRIOR_NEGATIVE_BIOPSY -> 1
        PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY, PriorBiopsyOption.NOT_SURE -> 0
    }
}
