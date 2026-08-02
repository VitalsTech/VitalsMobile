package com.vitals.mobile.core.session

import java.util.UUID

/**
 * Stable per-install device identifier, sent alongside auth requests.
 * Mirrors the `vitals.deviceFingerprint` (`web-...`) concept from VitalsWeb, prefixed for Android.
 */
object DeviceFingerprint {
    fun generate(): String = "android-${UUID.randomUUID()}"
}
