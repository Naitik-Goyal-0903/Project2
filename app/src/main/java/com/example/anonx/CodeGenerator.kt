package com.example.anonx

import java.util.UUID

object CodeGenerator {
    fun generateCode(): String {
        return UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
}
