package de.umra.risk.service

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.ProstateCancerRiskRequest

interface RiskAnalyzer {
    fun metadata(): AnalyzerInfo

    fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult
}
