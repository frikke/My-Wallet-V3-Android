package com.blockchain.network.interceptor

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cacheable(
    val maxAge: Int // Seconds
) {
    companion object {
        const val MAX_AGE_THREE_DAYS = 3 * 24 * 60 * 60
        const val MAX_AGE_1_DAY = 24 * 60 * 60
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DoNotLogResponseBody

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CustomAuthentication

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthenticationNotRequired
