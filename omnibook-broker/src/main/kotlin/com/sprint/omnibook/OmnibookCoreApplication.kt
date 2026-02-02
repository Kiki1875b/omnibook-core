package com.sprint.omnibook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OmnibookCoreApplication

fun main(args: Array<String>) {
    runApplication<OmnibookCoreApplication>(*args)
}
