package de.umra

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester

@SpringBootTest
@AutoConfigureGraphQlTester
class RiskAnalysisGraphQlTest {

    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @Test
    fun `analyzers query returns available analyzer`() {
        graphQlTester
            .document("query { analyzers { analyzerId displayName sourceUrl } }")
            .execute()
            .path("analyzers")
            .entityList(Any::class.java)
            .satisfies { analyzers ->
                assertTrue(analyzers.isNotEmpty())
            }
    }

    @Test
    fun `risk mutation returns calculated values`() {
        graphQlTester
            .document(
                """
                mutation Analyze(${'$'}input: ProstateCancerRiskInput!) {
                  analyzeProstateCancerRisk(input: ${'$'}input) {
                    analyzers {
                      analyzerId
                      success
                      forwardedOnline
                      risk {
                        noCancerRisk
                        lowGradeRisk
                        highGradeRisk
                        cancerRisk
                        grouped
                      }
                    }
                    aggregate {
                      noCancerRisk
                      lowGradeRisk
                      highGradeRisk
                      cancerRisk
                      basedOnAnalyzers
                    }
                  }
                }
                """.trimIndent(),
            )
            .variable(
                "input",
                mapOf(
                    "race" to "CAUCASIAN",
                    "age" to 65,
                    "psa" to 4.2,
                    "familyHistory" to "NO",
                    "dre" to "NORMAL",
                    "priorBiopsy" to "NEVER_HAD_PRIOR_BIOPSY",
                    "detailedFamilyHistoryEnabled" to false,
                    "pctFreePsaAvailable" to false,
                    "pca3Available" to false,
                    "t2ergAvailable" to false,
                    "snpsEnabled" to false,
                ),
            )
            .execute()
            .path("analyzeProstateCancerRisk.aggregate.noCancerRisk")
            .entity(Int::class.java)
            .satisfies { value ->
                assertTrue(value in 0..100)
            }
    }
}
