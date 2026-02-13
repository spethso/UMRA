package de.umra

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest
@AutoConfigureGraphQlTester
class GreetingGraphQlTest {

    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @Test
    fun `hello query returns message`() {
        graphQlTester
            .document("query { hello }")
            .execute()
            .path("hello")
            .entity(String::class.java)
            .satisfies { value ->
                assertTrue(value.contains("Hello"))
            }
    }

    @Test
    fun `addText mutation stores text and texts query returns it`() {
        graphQlTester
            .document("mutation { addText(text: \"alpha\") }")
            .execute()
            .path("addText")
            .entity(String::class.java)
            .isEqualTo("alpha")

        graphQlTester
            .document("query { texts }")
            .execute()
            .path("texts")
            .entityList(String::class.java)
            .satisfies { values ->
                assertTrue(values.contains("alpha"))
            }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun h2Properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1" }
            registry.add("spring.datasource.username") { "sa" }
            registry.add("spring.datasource.password") { "" }
            registry.add("spring.datasource.driver-class-name") { "org.h2.Driver" }
        }
    }
}
