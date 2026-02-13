package de.umra.greeting

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class GreetingQueryController(
    private val greetingService: GreetingService,
) {
    @QueryMapping
    fun hello(): String = greetingService.getHelloMessage()

    @QueryMapping
    fun texts(): List<String> = greetingService.getAllTexts()

    @MutationMapping
    fun addText(@Argument text: String): String = greetingService.addText(text)
}
