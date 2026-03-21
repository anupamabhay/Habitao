package com.habitao.domain.util

actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()
