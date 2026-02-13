package de.umra

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UmraServerApplication

fun main(args: Array<String>) {
    runApplication<UmraServerApplication>(*args)
}
