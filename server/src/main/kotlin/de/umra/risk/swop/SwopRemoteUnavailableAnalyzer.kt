package de.umra.risk.swop

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.ProstateCancerRiskRequest
import de.umra.risk.service.RiskAnalyzer
import org.springframework.stereotype.Component

@Component
class SwopRc4RiskAnalyzer : RiskAnalyzer {
    override fun metadata(): AnalyzerInfo = AnalyzerInfo(
        analyzerId = "SWOP_RC4",
        displayName = "SWOP Risk Calculator 4 (Urologist follow-up model)",
        sourceUrl = "https://www.prostatecancer-riskcalculator.com/seven-prostate-cancer-risk-calculators",
    )

    override fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult = AnalyzerRiskResult(
        analyzerId = metadata().analyzerId,
        displayName = metadata().displayName,
        sourceUrl = metadata().sourceUrl,
        forwardedOnline = false,
        success = false,
        warning = "SWOP RC4 endpoint is currently access-gated by the provider and does not return callable data from this environment.",
        risk = null,
    )
}
