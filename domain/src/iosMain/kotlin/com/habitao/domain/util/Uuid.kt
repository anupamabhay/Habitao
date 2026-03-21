package com.habitao.domain.util

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString().lowercase()
