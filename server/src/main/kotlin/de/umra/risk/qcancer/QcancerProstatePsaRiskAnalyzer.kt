package de.umra.risk.qcancer

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.DiabetesTypeOption
import de.umra.risk.model.FamilyHistoryOption
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.Race
import de.umra.risk.model.RiskResult
import de.umra.risk.model.SmokingStatusOption
import de.umra.risk.service.RiskAnalyzer
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class QcancerProstatePsaRiskAnalyzer(
    restClientBuilder: RestClient.Builder,
) : RiskAnalyzer {
    private val restClient = restClientBuilder.build()

    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "QCANCER_10YR_PROSTATE_PSA",
        displayName = "QCancer (10yr, prostate with PSA)",
        sourceUrl = "https://qcancer.org/10yr/prostate+psa/",
    )

    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult {
        validate(request)

        val years = request.qcancerYears.coerceIn(1, 15)
        val responseBody = runCatching {
            val payload = buildString {
                appendFormField("age", request.age.toString())
                appendFormField("sex", "1")
                appendFormField("ethnicity", request.race.toQcancerEthnicityCode().toString())
                appendFormField("postcode", (request.ukPostcode ?: "").trim().uppercase())
                appendFormField("smoke_cat", request.smokingStatus.toQcancerSmokeCode().toString())
                appendFormField("psa", request.psa.toString())
                appendFormField("diabetes_cat", request.diabetesType.toQcancerDiabetesCode().toString())
                appendFormField("yearsRiskCalculatedFor", years.toString())
                if (request.familyHistory == FamilyHistoryOption.YES) {
                    appendFormField("fh_prostatecancer", "on")
                }
                if (request.manicSchizophrenia) {
                    appendFormField("b_manicschiz", "on")
                }
                if (request.heightCm != null && request.weightKg != null) {
                    appendFormField("height", request.heightCm.toString())
                    appendFormField("weight", request.weightKg.toString())
                }
                appendFormField("calculate", "Calculate risk")
            }

            restClient.post()
                .uri("https://qcancer.org/10yr/prostate+psa/index.php")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(payload)
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
                warning = "QCancer request failed: ${it.message}",
                risk = null,
            )
        }

        val parsedPercent = responseBody.extractRiskPercent(years)
        if (parsedPercent == null) {
            return AnalyzerRiskResult(
                analyzerId = metadata().analyzerId,
                displayName = metadata().displayName,
                sourceUrl = metadata().sourceUrl,
                forwardedOnline = true,
                success = false,
                warning = "QCancer response did not contain a parsable risk value.",
                risk = null,
            )
        }

        val warning = if (responseBody.contains("estimated data", ignoreCase = true)) {
            "QCancer indicates estimated data was used because some optional information was left blank."
        } else {
            null
        }

        return AnalyzerRiskResult(
            analyzerId = metadata().analyzerId,
            displayName = metadata().displayName,
            sourceUrl = metadata().sourceUrl,
            forwardedOnline = true,
            success = true,
            warning = warning,
            risk = RiskResult(
                noCancerRisk = 100 - parsedPercent,
                lowGradeRisk = null,
                highGradeRisk = null,
                cancerRisk = parsedPercent,
                grouped = false,
            ),
        )
    }

    private fun validate(request: ProstateCancerRiskRequest) {
        require(request.age in 25..84) { "QCancer requires age between 25 and 84 years." }
        require(request.psa in 0.0..50.0) { "QCancer requires PSA between 0.0 and 50.0." }
        require(request.qcancerYears in 1..15) { "QCancer years must be between 1 and 15." }

        val hasHeight = request.heightCm != null
        val hasWeight = request.weightKg != null
        require(!(hasHeight xor hasWeight)) { "QCancer requires both height and weight together, or neither." }

        if (hasHeight && hasWeight) {
            require(request.heightCm in 140..210) { "QCancer height must be between 140 and 210 cm." }
            require(request.weightKg in 40..180) { "QCancer weight must be between 40 and 180 kg." }
        }
    }

    private fun StringBuilder.appendFormField(name: String, value: String) {
        if (isNotEmpty()) {
            append('&')
        }
        append(URLEncoder.encode(name, StandardCharsets.UTF_8))
        append('=')
        append(URLEncoder.encode(value, StandardCharsets.UTF_8))
    }

    private fun String.extractRiskPercent(years: Int): Int? {
        val regex = Regex("""risk of having a diagnosis of prostate cancer within the next $years years is\s*([0-9]+(?:\.[0-9]+)?)%""", RegexOption.IGNORE_CASE)
        val value = regex.find(this)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return value.roundToInt().coerceIn(0, 100)
    }

    private fun Race.toQcancerEthnicityCode(): Int = when (this) {
        Race.CAUCASIAN, Race.UNKNOWN -> 1
        Race.ASIAN -> 5
        Race.AFRICAN_AMERICAN -> 7
        Race.HISPANIC_LATINO,
        Race.MIDDLE_EASTERN_NORTH_AFRICAN,
        Race.NATIVE_AMERICAN_OR_ALASKA_NATIVE,
        Race.NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER,
        Race.OTHER -> 9
    }

    private fun SmokingStatusOption.toQcancerSmokeCode(): Int = when (this) {
        SmokingStatusOption.NON_SMOKER -> 0
        SmokingStatusOption.EX_SMOKER -> 1
        SmokingStatusOption.LIGHT -> 2
        SmokingStatusOption.MODERATE -> 3
        SmokingStatusOption.HEAVY -> 4
    }

    private fun DiabetesTypeOption.toQcancerDiabetesCode(): Int = when (this) {
        DiabetesTypeOption.NONE -> 0
        DiabetesTypeOption.TYPE_1 -> 1
        DiabetesTypeOption.TYPE_2 -> 2
    }
}
