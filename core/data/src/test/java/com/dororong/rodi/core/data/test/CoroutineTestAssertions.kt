package com.dororong.rodi.core.data.test

suspend inline fun <reified T : Throwable> assertThrowsSuspend(
    crossinline block: suspend () -> Unit,
): T {
    try {
        block()
    } catch (exception: Throwable) {
        if (exception is T) return exception
        throw exception
    }
    throw AssertionError("Expected ${T::class.simpleName} to be thrown")
}
