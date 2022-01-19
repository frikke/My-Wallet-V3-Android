package com.blockchain.enviroment

enum class Environment(
    val id: String
) {
    PRODUCTION("env_prod"),
    STAGING("env_staging");

    companion object {
        fun fromString(text: String?): Environment? {
            if (text != null) {
                for (environment in values()) {
                    if (text.equals(environment.id, ignoreCase = true)) {
                        return environment
                    }
                }
            }
            return null
        }
    }
}
