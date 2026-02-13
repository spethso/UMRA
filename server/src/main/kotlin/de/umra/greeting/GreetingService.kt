package de.umra.greeting

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GreetingService(
    private val greetingRepository: GreetingRepository,
) {
    fun getHelloMessage(): String = "Hello from Kotlin + GraphQL + PostgreSQL"

    @Transactional
    fun addText(text: String): String {
        val saved = greetingRepository.save(GreetingEntity(message = text))
        return saved.message
    }

    @Transactional(readOnly = true)
    fun getAllTexts(): List<String> = greetingRepository.findAllByOrderByIdAsc().map { it.message }
}
