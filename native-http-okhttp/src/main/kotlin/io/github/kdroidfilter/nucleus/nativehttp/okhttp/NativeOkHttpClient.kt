package io.github.kdroidfilter.nucleus.nativehttp.okhttp

import io.github.kdroidfilter.nucleus.nativessl.NativeTrustManager
import okhttp3.OkHttpClient

object NativeOkHttpClient {
    fun create(): OkHttpClient =
        OkHttpClient
            .Builder()
            .withNativeSsl()
            .build()

    fun OkHttpClient.Builder.withNativeSsl(): OkHttpClient.Builder =
        sslSocketFactory(NativeTrustManager.sslSocketFactory, NativeTrustManager.trustManager)
}
