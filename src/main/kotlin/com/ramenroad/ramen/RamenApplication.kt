package com.ramenroad.ramen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RamenApplication

fun main(args: Array<String>) {
    runApplication<RamenApplication>(*args)
}
