package de.umra.risk.model

/** Self-reported race / ethnicity of the patient. */
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

/** Whether the patient has a family history of prostate cancer. */
enum class FamilyHistoryOption {
    YES,
    NO,
    DO_NOT_KNOW,
}

/** Digital rectal examination result. */
enum class DreOption {
    ABNORMAL,
    NORMAL,
    NOT_PERFORMED_OR_NOT_SURE,
}

/** Whether the patient has had a previous prostate biopsy. */
enum class PriorBiopsyOption {
    NEVER_HAD_PRIOR_BIOPSY,
    PRIOR_NEGATIVE_BIOPSY,
    NOT_SURE,
}

/** Count of first-degree relatives with prostate cancer. */
enum class RelativeCountOption {
    NO,
    YES_ONE,
    YES_TWO_OR_MORE,
}

/** Whether at least one relative is affected. */
enum class BinaryRelativeOption {
    NO,
    YES_AT_LEAST_ONE,
}

/** Smoking status category for QCancer. */
enum class SmokingStatusOption {
    NON_SMOKER,
    EX_SMOKER,
    LIGHT,
    MODERATE,
    HEAVY,
}

/** Diabetes type for QCancer. */
enum class DiabetesTypeOption {
    NONE,
    TYPE_1,
    TYPE_2,
}

/** SNP genotype as supplied by the client (GraphQL input). */
data class SnpGenotypeInput(
    val snpIndex: Int,
    val riskAlleles: Int,
)

/**
 * Raw patient input as received via the GraphQL schema.
 *
 * Optional fields default to `null` or safe sentinel values so that
 * analyzers not requiring them can proceed without additional checks.
 */
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

/**
 * Static metadata describing a registered risk analyzer.
 *
 * @property analyzerId  unique identifier used in GraphQL queries
 * @property displayName human-readable display name
 * @property sourceUrl   URL to the original calculator / publication
 */
data class AnalyzerInfo(
    val analyzerId: String,
    val displayName: String,
    val sourceUrl: String,
)

/**
 * Computed risk percentages for a single analysis.
 *
 * @property noCancerRisk  probability (0–100) of no cancer
 * @property lowGradeRisk  probability of low-grade cancer, or `null` when grouped
 * @property highGradeRisk probability of high-grade cancer, or `null` when grouped
 * @property cancerRisk    combined cancer probability, or `null` if not computed
 * @property grouped       whether low/high grades are aggregated into [cancerRisk]
 */
data class RiskResult(
    val noCancerRisk: Int,
    val lowGradeRisk: Int? = null,
    val highGradeRisk: Int? = null,
    val cancerRisk: Int? = null,
    val grouped: Boolean,
)

/**
 * Result produced by a single analyzer, including success/failure metadata.
 *
 * @property analyzerId      analyzer that produced this result
 * @property displayName     human-readable analyzer name
 * @property sourceUrl       original source URL
 * @property forwardedOnline whether the request was forwarded to an online service
 * @property success         whether the computation succeeded
 * @property warning         optional warning or error message
 * @property risk            computed risk values, `null` on failure
 */
data class AnalyzerRiskResult(
    val analyzerId: String,
    val displayName: String,
    val sourceUrl: String,
    val forwardedOnline: Boolean,
    val success: Boolean,
    val warning: String? = null,
    val risk: RiskResult? = null,
)

/**
 * Aggregate risk computed across multiple analyzers.
 *
 * @property noCancerRisk    averaged no-cancer probability
 * @property lowGradeRisk    averaged low-grade probability, or `null`
 * @property highGradeRisk   averaged high-grade probability, or `null`
 * @property cancerRisk      averaged cancer probability, or `null`
 * @property basedOnAnalyzers number of analyzers that contributed
 */
data class AggregateRiskResult(
    val noCancerRisk: Int,
    val lowGradeRisk: Int? = null,
    val highGradeRisk: Int? = null,
    val cancerRisk: Int? = null,
    val basedOnAnalyzers: Int,
)

/**
 * Envelope containing per-analyzer results and the overall aggregate.
 *
 * @property analyzers individual analyzer results
 * @property aggregate averaged result across successful analyzers
 */
data class RiskAnalysisResponse(
    val analyzers: List<AnalyzerRiskResult>,
    val aggregate: AggregateRiskResult,
)

/**
 * Response returned by the `analyzeProstateCancerRisk` mutation.
 *
 * @property sessionId          persisted session id, or `null` if not stored
 * @property selectedAnalyzerIds ids of the analyzers that were used
 * @property autoMode           whether auto-selection was active
 * @property stored             whether the result was persisted
 * @property result             the full analysis response
 */
data class AnalysisSessionResponse(
    val sessionId: String?,
    val selectedAnalyzerIds: List<String>,
    val autoMode: Boolean,
    val stored: Boolean,
    val result: RiskAnalysisResponse,
)

/**
 * A previously stored analysis session, including input, result, and audit fields.
 *
 * @property sessionId          unique session identifier
 * @property input              the patient data that was analyzed
 * @property selectedAnalyzerIds analyzer ids that were used
 * @property autoMode           whether analyzers were auto-selected
 * @property result             the stored analysis response
 * @property createdAt          ISO-8601 creation timestamp
 */
data class SavedAnalysisSession(
    val sessionId: String,
    val input: ProstateCancerRiskInput,
    val selectedAnalyzerIds: List<String>,
    val autoMode: Boolean,
    val result: RiskAnalysisResponse,
    val createdAt: String,
)

/** Internal SNP genotype representation passed to risk models. */
data class SnpGenotype(
    val snpIndex: Int,
    val riskAlleles: Int,
)

/**
 * Pre-validated, internal request object constructed from [ProstateCancerRiskInput]
 * and passed to individual [de.umra.risk.service.RiskAnalyzer] implementations.
 */
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
