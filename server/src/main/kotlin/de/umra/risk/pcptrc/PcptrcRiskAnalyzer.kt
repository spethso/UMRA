package de.umra.risk.pcptrc

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.BinaryRelativeOption
import de.umra.risk.model.DreOption
import de.umra.risk.model.FamilyHistoryOption
import de.umra.risk.model.PriorBiopsyOption
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.model.Race
import de.umra.risk.model.RelativeCountOption
import de.umra.risk.service.RiskAnalyzer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class PcptrcRiskAnalyzer(
    restClientBuilder: RestClient.Builder,
    @Value("\${umra.analyzers.pcptrc.online-forwarding-enabled:true}")
    private val onlineForwardingEnabled: Boolean,
) : RiskAnalyzer {
    private val riskModel = PcptrcRiskModel()
    private val restClient = restClientBuilder.build()

    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "PCPTRC",
        displayName = "Prostate Cancer Prevention Trial Risk Calculator 2.0",
        sourceUrl = "https://www.riskcalc.org/PCPTRC/",
    )

    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult {
        validate(request)

        val detailedFamily = request.detailedFamilyHistoryEnabled && request.race == Race.CAUCASIAN
        val fdrLess60 = if (detailedFamily) request.fdrPcLess60?.toRelativeCount() else null
        val fdr60 = if (detailedFamily) request.fdrPc60?.toRelativeCount() else null
        val fdrBc = if (detailedFamily) request.fdrBc?.toBinaryRelative() else null
        val sdr = if (detailedFamily) request.sdr?.toBinaryRelative() else null

        val modelResult = riskModel.calculate(
            psa = request.psa,
            pctFreePsa = if (request.pctFreePsaAvailable) request.pctFreePsa else null,
            pca3 = if (request.pca3Available) request.pca3 else null,
            t2erg = if (request.t2ergAvailable) request.t2erg else null,
            age = request.age.toDouble(),
            race = if (request.race == Race.AFRICAN_AMERICAN) 1.0 else 0.0,
            priorBiopsy = request.priorBiopsy.toModelValue(),
            dre = request.dre.toModelValue(),
            famHistory = if (detailedFamily) 0.0 else request.familyHistory.toModelValue(),
            fdrPcLess60 = fdrLess60,
            fdrPc60 = fdr60,
            fdrBc = fdrBc,
            sdr = sdr,
            snps = if (request.snpsEnabled && request.race == Race.CAUCASIAN) request.snpGenotypes else emptyList(),
        )

        val onlineForwarded = if (onlineForwardingEnabled) {
            forwardToOnlineAnalyzer(request)
        } else {
            false
        }

        return AnalyzerRiskResult(
            analyzerId = metadata().analyzerId,
            displayName = metadata().displayName,
            sourceUrl = metadata().sourceUrl,
            forwardedOnline = onlineForwarded,
            success = true,
            warning = when {
                !onlineForwardingEnabled -> "Online forwarding is disabled by configuration."
                !onlineForwarded -> "Online forwarding failed; result calculated from validated PCPTRC model."
                else -> null
            },
            risk = modelResult,
        )
    }

    private fun validate(request: ProstateCancerRiskRequest) {
        require(request.age in 55..90) { "Age must be between 55 and 90 years." }
        require(request.psa > 0.0 && request.psa <= 50.0) { "PSA must be > 0 and <= 50." }

        if (request.pctFreePsaAvailable) {
            requireNotNull(request.pctFreePsa) { "Percent free PSA value is required." }
            require(request.pctFreePsa in 5.0..75.0) { "Percent free PSA must be between 5 and 75." }
        }

        if (request.pca3Available) {
            requireNotNull(request.pca3) { "PCA3 value is required." }
            require(request.pca3 in 0.3..332.5) { "PCA3 must be between 0.3 and 332.5." }
        }

        if (request.t2ergAvailable) {
            require(request.pca3Available) { "T2:ERG requires PCA3 to be available." }
            requireNotNull(request.t2erg) { "T2:ERG value is required." }
            require(request.t2erg in 0.0..6031.6) { "T2:ERG must be between 0.0 and 6031.6." }
        }

        require(!(request.pctFreePsaAvailable && (request.pca3Available || request.t2ergAvailable))) {
            "Percent free PSA cannot be combined with PCA3/T2:ERG in PCPTRC."
        }

        if (request.detailedFamilyHistoryEnabled && request.race == Race.CAUCASIAN) {
            requireNotNull(request.fdrPcLess60) { "Detailed family history is enabled: FDR < 60 is required." }
            requireNotNull(request.fdrPc60) { "Detailed family history is enabled: FDR >= 60 is required." }
            requireNotNull(request.fdrBc) { "Detailed family history is enabled: breast cancer history is required." }
            requireNotNull(request.sdr) { "Detailed family history is enabled: second degree relatives are required." }
        }

        if (request.snpsEnabled && request.race == Race.CAUCASIAN) {
            require(request.snpGenotypes.size <= 5) { "At most 5 SNPs are allowed." }
            request.snpGenotypes.forEach {
                require(it.snpIndex in 1..30) { "SNP index must be between 1 and 30." }
                require(it.riskAlleles in 0..2) { "Risk alleles must be between 0 and 2." }
            }
        }
    }

    private fun forwardToOnlineAnalyzer(request: ProstateCancerRiskRequest): Boolean {
        val uri = UriComponentsBuilder
            .fromUriString(metadata().sourceUrl)
            .queryParam("race", request.race.toUiValue())
            .queryParam("age", request.age)
            .queryParam("psa", request.psa)
            .queryParam("famhist", request.familyHistory.toUiValue())
            .queryParam("dre", request.dre.toUiValue())
            .queryParam("priobiop", request.priorBiopsy.toUiValue())
            .queryParam("fpsa", if (request.pctFreePsaAvailable) request.pctFreePsa else null)
            .queryParam("pca_3", if (request.pca3Available) request.pca3 else null)
            .queryParam("t_2erg", if (request.t2ergAvailable) request.t2erg else null)
            .queryParam("calcRisk", 1)
            .build()
            .encode()
            .toUri()

        return runCatching {
            val response = restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(String::class.java)
            response.statusCode.is2xxSuccessful
        }.getOrDefault(false)
    }

    private fun RelativeCountOption.toRelativeCount(): Int = when (this) {
        RelativeCountOption.NO -> 0
        RelativeCountOption.YES_ONE -> 1
        RelativeCountOption.YES_TWO_OR_MORE -> 2
    }

    private fun BinaryRelativeOption.toBinaryRelative(): Int = when (this) {
        BinaryRelativeOption.NO -> 0
        BinaryRelativeOption.YES_AT_LEAST_ONE -> 1
    }

    private fun FamilyHistoryOption.toModelValue(): Double? = when (this) {
        FamilyHistoryOption.YES -> 1.0
        FamilyHistoryOption.NO -> 0.0
        FamilyHistoryOption.DO_NOT_KNOW -> null
    }

    private fun DreOption.toModelValue(): Double? = when (this) {
        DreOption.ABNORMAL -> 1.0
        DreOption.NORMAL -> 0.0
        DreOption.NOT_PERFORMED_OR_NOT_SURE -> null
    }

    private fun PriorBiopsyOption.toModelValue(): Double? = when (this) {
        PriorBiopsyOption.PRIOR_NEGATIVE_BIOPSY -> 1.0
        PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY -> 0.0
        PriorBiopsyOption.NOT_SURE -> null
    }

    private fun Race.toUiValue(): String = when (this) {
        Race.AFRICAN_AMERICAN -> "African American"
        Race.CAUCASIAN -> "Caucasian"
        Race.HISPANIC -> "Hispanic"
        Race.OTHER -> "Other"
    }

    private fun FamilyHistoryOption.toUiValue(): String = when (this) {
        FamilyHistoryOption.YES -> "Yes"
        FamilyHistoryOption.NO -> "No"
        FamilyHistoryOption.DO_NOT_KNOW -> "Do not know"
    }

    private fun DreOption.toUiValue(): String = when (this) {
        DreOption.ABNORMAL -> "Abnormal"
        DreOption.NORMAL -> "Normal"
        DreOption.NOT_PERFORMED_OR_NOT_SURE -> "Not performed or not sure"
    }

    private fun PriorBiopsyOption.toUiValue(): String = when (this) {
        PriorBiopsyOption.NEVER_HAD_PRIOR_BIOPSY -> "Never had a prior biopsy"
        PriorBiopsyOption.PRIOR_NEGATIVE_BIOPSY -> "Prior negative biopsy"
        PriorBiopsyOption.NOT_SURE -> "Not sure"
    }
}
