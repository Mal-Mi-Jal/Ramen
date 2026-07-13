package com.ramenlog.ramen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class RamenApplication

fun main(args: Array<String>) {
    runApplication<RamenApplication>(*args)
}
