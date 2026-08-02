package com.vitals.mobile.core.network

import com.vitals.mobile.BuildConfig

/** Base URL of the Vitals API Gateway. Configurable via `local.properties` (`API_BASE_URL`). */
object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL
}
