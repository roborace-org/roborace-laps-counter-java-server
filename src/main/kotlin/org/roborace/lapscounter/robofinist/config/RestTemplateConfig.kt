package org.roborace.lapscounter.robofinist.config

import mu.KLogging
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder) = builder.build().apply {
        interceptors.add(LoggingRequestInterceptor())
    }

}


class LoggingRequestInterceptor : ClientHttpRequestInterceptor {
    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        traceRequest(request, body)
        execution.execute(request, body).also {
            return traceResponse(it)
        }
    }

    @Throws(IOException::class)
    private fun traceRequest(request: HttpRequest, body: ByteArray) {
        logger.debug("Request     : {} {}", request.method, request.uri)
        logger.debug("Headers     : {}", request.headers)
        logger.debug("Request body: {}", String(body, UTF_8))
    }

    @Throws(IOException::class)
    private fun traceResponse(response: ClientHttpResponse): BufferingClientHttpResponseWrapper {
        val responseBody: ByteArray = response.body.readAllBytes()
        logger.debug("Response Status: {}", response.statusCode)
        logger.debug("Headers        : {}", response.headers)
        logger.debug("Response body  : {}", String(responseBody, UTF_8))
        return BufferingClientHttpResponseWrapper(response, responseBody)
    }

    companion object : KLogging()
}

private class BufferingClientHttpResponseWrapper(
    response: ClientHttpResponse,
    private val body: ByteArray,
) : ClientHttpResponse by response {
    override fun getBody(): InputStream = ByteArrayInputStream(body)
}
