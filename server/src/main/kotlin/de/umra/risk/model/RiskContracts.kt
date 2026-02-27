package de.umra.risk.model

enum class Race {
    AFRICAN_AMERICAN,
    ASIAN,
    CAUCASIAN,
    HISPANIC_LATINO,
    MIDDLE_EASTERN_NORTH_AFRICAN,
    NATIVE_AMERICAN_OR_ALASKA_NATIVE,
    NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER,
    OTHER,
    UNKNOWN,
}

enum class FamilyHistoryOption {
    YES,
    NO,
    DO_NOT_KNOW,
}

enum class DreOption {
    ABNORMAL,
    NORMAL,
    NOT_PERFORMED_OR_NOT_SURE,
}

enum class PriorBiopsyOption {
    NEVER_HAD_PRIOR_BIOPSY,
    PRIOR_NEGATIVE_BIOPSY,
    NOT_SURE,
}

enum class RelativeCountOption {
    NO,
    YES_ONE,
    YES_TWO_OR_MORE,
}

enum class BinaryRelativeOption {
    NO,
    YES_AT_LEAST_ONE,
}

enum class SmokingStatusOption {
    NON_SMOKER,
    EX_SMOKER,
    LIGHT,
    MODERATE,
    HEAVY,
}

enum class DiabetesTypeOption {
    NONE,
    TYPE_1,
    TYPE_2,
}

data class SnpGenotypeInput(
    val snpIndex: Int,
    val riskAlleles: Int,
)

data class ProstateCancerRiskInput(
    val race: Race,
    val age: Int,
    val psa: Double,
    val familyHistory: FamilyHistoryOption,
    val dre: DreOption,
    val priorBiopsy: PriorBiopsyOption,
    val detailedFamilyHistoryEnabled: Boolean? = false,
    val fdrPcLess60: RelativeCountOption? = null,
    val fdrPc60: RelativeCountOption? = null,
    val fdrBc: BinaryRelativeOption? = null,
    val sdr: BinaryRelativeOption? = null,
    val pctFreePsaAvailable: Boolean? = false,
    val pctFreePsa: Double? = null,
    val pca3Available: Boolean? = false,
    val pca3: Double? = null,
    val t2ergAvailable: Boolean? = false,
    val t2erg: Double? = null,
    val snpsEnabled: Boolean? = false,
    val snpGenotypes: List<SnpGenotypeInput>? = emptyList(),
    val prostateVolumeCc: Double? = null,
    val mriPiradsScore: Int? = null,
    val dreVolumeClassCc: Int? = null,
    val gleasonScoreLegacy: Int? = null,
    val biopsyCancerLengthMm: Double? = null,
    val biopsyBenignLengthMm: Double? = null,
    val ukPostcode: String? = null,
    val smokingStatus: SmokingStatusOption? = null,
    val diabetesType: DiabetesTypeOption? = null,
    val manicSchizophrenia: Boolean? = false,
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    val qcancerYears: Int? = null,
)

data class AnalyzerInfo(
    val analyzerId: String,
    val displayName: String,
    val sourceUrl: String,
)

data class RiskResult(
    val noCancerRisk: Int,
    val lowGradeRisk: Int? = null,
    val highGradeRisk: Int? = null,
    val cancerRisk: Int? = null,
    val grouped: Boolean,
)

data class AnalyzerRiskResult(
    val analyzerId: String,
    val displayName: String,
    val sourceUrl: String,
    val forwardedOnline: Boolean,
    val success: Boolean,
    val warning: String? = null,
    val risk: RiskResult? = null,
)

data class AggregateRiskResult(
    val noCancerRisk: Int,
    val lowGradeRisk: Int? = null,
    val highGradeRisk: Int? = null,
    val cancerRisk: Int? = null,
    val basedOnAnalyzers: Int,
)

data class RiskAnalysisResponse(
    val analyzers: List<AnalyzerRiskResult>,
    val aggregate: AggregateRiskResult,
)

data class AnalysisSessionResponse(
    val sessionId: String?,
    val selectedAnalyzerIds: List<String>,
    val autoMode: Boolean,
    val stored: Boolean,
    val result: RiskAnalysisResponse,
)

data class SavedAnalysisSession(
    val sessionId: String,
    val input: ProstateCancerRiskInput,
    val selectedAnalyzerIds: List<String>,
    val autoMode: Boolean,
    val result: RiskAnalysisResponse,
    val createdAt: String,
)

data class SnpGenotype(
    val snpIndex: Int,
    val riskAlleles: Int,
)

data class ProstateCancerRiskRequest(
    val race: Race,
    val age: Int,
    val psa: Double,
    val familyHistory: FamilyHistoryOption,
    val dre: DreOption,
    val priorBiopsy: PriorBiopsyOption,
    val detailedFamilyHistoryEnabled: Boolean,
    val fdrPcLess60: RelativeCountOption?,
    val fdrPc60: RelativeCountOption?,
    val fdrBc: BinaryRelativeOption?,
    val sdr: BinaryRelativeOption?,
    val pctFreePsaAvailable: Boolean,
    val pctFreePsa: Double?,
    val pca3Available: Boolean,
    val pca3: Double?,
    val t2ergAvailable: Boolean,
    val t2erg: Double?,
    val snpsEnabled: Boolean,
    val snpGenotypes: List<SnpGenotype>,
    val prostateVolumeCc: Double?,
    val mriPiradsScore: Int?,
    val dreVolumeClassCc: Int?,
    val gleasonScoreLegacy: Int?,
    val biopsyCancerLengthMm: Double?,
    val biopsyBenignLengthMm: Double?,
    val ukPostcode: String?,
    val smokingStatus: SmokingStatusOption,
    val diabetesType: DiabetesTypeOption,
    val manicSchizophrenia: Boolean,
    val heightCm: Int?,
    val weightKg: Int?,
    val qcancerYears: Int,
)
