package io.github.kdroidfilter.nucleus.nativehttp

import io.github.kdroidfilter.nucleus.nativessl.NativeTrustManager
import java.net.http.HttpClient
import javax.net.ssl.SSLParameters

object NativeHttpClient {
    fun create(): HttpClient =
        HttpClient
            .newBuilder()
            .withNativeSsl()
            .build()

    fun HttpClient.Builder.withNativeSsl(): HttpClient.Builder =
        sslContext(NativeTrustManager.sslContext)
            .sslParameters(
                SSLParameters().apply {
                    needClientAuth = false
                },
            )
}
