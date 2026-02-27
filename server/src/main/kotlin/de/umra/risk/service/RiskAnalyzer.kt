package de.umra.risk.service

import de.umra.risk.model.AnalyzerInfo
import de.umra.risk.model.AnalyzerRiskResult
import de.umra.risk.model.ProstateCancerRiskRequest

/**
 * Contract for a single prostate-cancer risk analyzer.
 *
 * Each implementation encapsulates a specific risk model or external
 * calculator and is discovered automatically at application startup.
 */
interface RiskAnalyzer {

    /**
     * Returns static metadata describing this analyzer (id, display name, source URL).
     *
     * @return [AnalyzerInfo] for this analyzer
     */
    fun metadata(): AnalyzerInfo

    /**
     * Computes a risk result for the given patient request.
     *
     * @param request pre-validated patient data
     * @return [AnalyzerRiskResult] containing computed risk values or failure info
     * @throws IllegalArgumentException if required preconditions for this analyzer are not met
     */
    fun analyze(request: ProstateCancerRiskRequest): AnalyzerRiskResult
}
