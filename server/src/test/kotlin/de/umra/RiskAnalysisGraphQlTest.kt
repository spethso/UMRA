package de.umra

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
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
          .get()
          .also { analyzers ->
            assertTrue(analyzers.isNotEmpty())
          }
    }

    @Test
    fun `risk mutation returns session with calculated values`() {
        graphQlTester
            .document(
                """
                mutation Analyze(${'$'}input: ProstateCancerRiskInput!, ${'$'}analyzerIds: [String!]) {
                  analyzeProstateCancerRisk(input: ${'$'}input, analyzerIds: ${'$'}analyzerIds) {
                    sessionId
                    result {
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
                .variable("analyzerIds", listOf("PCPTRC"))
            .execute()
            .path("analyzeProstateCancerRisk.sessionId")
            .entity(String::class.java)
            .satisfies { sessionId ->
                assertNotNull(sessionId)
                assertTrue(sessionId.isNotEmpty())
            }
    }

    @Test
    fun `risk mutation returns aggregate and session can be loaded`() {
        val sessionIdHolder = mutableListOf<String>()

        graphQlTester
            .document(
                """
                mutation Analyze(${'$'}input: ProstateCancerRiskInput!, ${'$'}analyzerIds: [String!]) {
                  analyzeProstateCancerRisk(input: ${'$'}input, analyzerIds: ${'$'}analyzerIds) {
                    sessionId
                    result {
                      aggregate {
                        noCancerRisk
                        basedOnAnalyzers
                      }
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
            .variable("analyzerIds", listOf("PCPTRC"))
            .execute()
            .path("analyzeProstateCancerRisk.sessionId")
            .entity(String::class.java)
            .satisfies { sessionId ->
                sessionIdHolder.add(sessionId)
            }

        assertTrue(sessionIdHolder.isNotEmpty())

        graphQlTester
            .document(
                """
                query Session(${'$'}sessionId: String!) {
                  session(sessionId: ${'$'}sessionId) {
                    sessionId
                    result {
                      aggregate {
                        noCancerRisk
                        basedOnAnalyzers
                      }
                    }
                    createdAt
                  }
                }
                """.trimIndent(),
            )
            .variable("sessionId", sessionIdHolder[0])
            .execute()
            .path("session.sessionId")
            .entity(String::class.java)
            .isEqualTo(sessionIdHolder[0])
    }
}
