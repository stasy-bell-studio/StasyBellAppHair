package com.stasybell.os.data.remote

import com.stasybell.os.domain.StudioRules
import com.stasybell.os.security.SecureTokenStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object RetrofitClientFactory {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun create(
        rawBaseUrl: String,
        allowDebugHttp: Boolean,
        tokenStore: SecureTokenStore
    ): StasyBellApiService {
        val validated = StudioRules.validateServerUrl(rawBaseUrl, allowDebugHttp)
        val baseUrlWithSlash = if (validated.endsWith("/")) validated else "$validated/"

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = tokenStore.getAccessToken()
                val reqBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(reqBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrlWithSlash)
            .client(client)
            .addConverterFactory(KotlinxSerializationConverterFactory(json))
            .build()
            .create(StasyBellApiService::class.java)
    }
}

private class KotlinxSerializationConverterFactory(
    private val json: Json
) : Converter.Factory() {
    private val contentType = "application/json; charset=UTF-8".toMediaType()

    @OptIn(ExperimentalSerializationApi::class)
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == Unit::class.java) {
            return Converter<ResponseBody, Unit> { it.close() }
        }
        val serializer = json.serializersModule.serializer(type)
        return Converter<ResponseBody, Any?> { body ->
            body.use {
                val text = it.string()
                if (text.isBlank()) null else json.decodeFromString(serializer, text)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val serializer = json.serializersModule.serializer(type)
        return Converter<Any, RequestBody> { value ->
            val encoded = json.encodeToString(serializer, value)
            encoded.toRequestBody(contentType)
        }
    }
}
