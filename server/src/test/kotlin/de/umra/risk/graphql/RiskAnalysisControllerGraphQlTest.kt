package de.umra.risk.graphql

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import java.util.UUID

@SpringBootTest
@AutoConfigureGraphQlTester
class RiskAnalysisControllerGraphQlTest {

    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    private val standardInputMap = mapOf(
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
    )

    private val analyzeMutation = """
        mutation Analyze(${'$'}input: ProstateCancerRiskInput!, ${'$'}analyzerIds: [String!], ${'$'}storeResult: Boolean) {
          analyzeProstateCancerRisk(input: ${'$'}input, analyzerIds: ${'$'}analyzerIds, storeResult: ${'$'}storeResult) {
            sessionId
            selectedAnalyzerIds
            autoMode
            stored
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
    """.trimIndent()

    @Test
    fun `deleteSession with invalid UUID format returns error`() {
        graphQlTester
            .document(
                """
                mutation Delete(${'$'}sessionId: String!) {
                  deleteSession(sessionId: ${'$'}sessionId)
                }
                """.trimIndent(),
            )
            .variable("sessionId", "not-a-valid-uuid")
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty(), "Should have at least one error")
            }
    }

    @Test
    fun `deleteSession with non-existent UUID returns false`() {
        graphQlTester
            .document(
                """
                mutation Delete(${'$'}sessionId: String!) {
                  deleteSession(sessionId: ${'$'}sessionId)
                }
                """.trimIndent(),
            )
            .variable("sessionId", UUID.randomUUID().toString())
            .execute()
            .path("deleteSession")
            .entity(Boolean::class.java)
            .isEqualTo(false)
    }

    @Test
    fun `session query with non-existent ID returns null`() {
        graphQlTester
            .document(
                """
                query Session(${'$'}sessionId: String!) {
                  session(sessionId: ${'$'}sessionId) {
                    sessionId
                  }
                }
                """.trimIndent(),
            )
            .variable("sessionId", UUID.randomUUID().toString())
            .execute()
            .path("session")
            .valueIsNull()
    }

    @Test
    fun `full flow - analyze with store, load session, delete session, verify deleted`() {
        // Step 1: Analyze and store
        val sessionIdHolder = mutableListOf<String>()
        graphQlTester
            .document(analyzeMutation)
            .variable("input", standardInputMap)
            .variable("analyzerIds", listOf("PCPTRC"))
            .variable("storeResult", true)
            .execute()
            .path("analyzeProstateCancerRisk.sessionId")
            .entity(String::class.java)
            .satisfies { sessionId ->
                assertNotNull(sessionId)
                sessionIdHolder.add(sessionId)
            }

        assertTrue(sessionIdHolder.isNotEmpty())
        val sessionId = sessionIdHolder[0]

        // Step 2: Load the stored session
        graphQlTester
            .document(
                """
                query Session(${'$'}sessionId: String!) {
                  session(sessionId: ${'$'}sessionId) {
                    sessionId
                    autoMode
                    selectedAnalyzerIds
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
            .variable("sessionId", sessionId)
            .execute()
            .path("session.sessionId")
            .entity(String::class.java)
            .isEqualTo(sessionId)

        // Step 3: Delete the session
        graphQlTester
            .document(
                """
                mutation Delete(${'$'}sessionId: String!) {
                  deleteSession(sessionId: ${'$'}sessionId)
                }
                """.trimIndent(),
            )
            .variable("sessionId", sessionId)
            .execute()
            .path("deleteSession")
            .entity(Boolean::class.java)
            .isEqualTo(true)

        // Step 4: Verify it's deleted
        graphQlTester
            .document(
                """
                query Session(${'$'}sessionId: String!) {
                  session(sessionId: ${'$'}sessionId) {
                    sessionId
                  }
                }
                """.trimIndent(),
            )
            .variable("sessionId", sessionId)
            .execute()
            .path("session")
            .valueIsNull()
    }

    @Test
    fun `analyze without store returns null sessionId and stored false`() {
        graphQlTester
            .document(analyzeMutation)
            .variable("input", standardInputMap)
            .variable("analyzerIds", listOf("PCPTRC"))
            .variable("storeResult", false)
            .execute()
            .path("analyzeProstateCancerRisk")
            .entity(Any::class.java)
            .satisfies { response ->
                @Suppress("UNCHECKED_CAST")
                val map = response as Map<String, Any?>
                assertNull(map["sessionId"], "sessionId should be null when not stored")
                assertEquals(false, map["stored"], "stored should be false")
            }
    }

    @Test
    fun `recommendedAnalyzers query returns results`() {
        graphQlTester
            .document(
                """
                query Recommended(${'$'}input: ProstateCancerRiskInput!) {
                  recommendedAnalyzers(input: ${'$'}input) {
                    analyzerId
                    displayName
                    sourceUrl
                  }
                }
                """.trimIndent(),
            )
            .variable("input", standardInputMap)
            .execute()
            .path("recommendedAnalyzers")
            .entityList(Any::class.java)
            .get()
            .also { analyzers ->
                assertTrue(analyzers.isNotEmpty(), "Should recommend at least one analyzer")
            }
    }
}
