package com.droidablebee.gphoto

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator

abstract class BaseRestService {

    protected static final String X_API_CLIENT = "X-API-CLIENT"
    protected static final String USER_AGENT = "User-Agent"

    protected HTTPBuilder http = createHTTPBuilder()

    protected HTTPBuilder createHTTPBuilder() {

        HTTPBuilder builder = new HTTPBuilder(getDefaultUri(), ContentType.JSON)

        //add commonly used headers
        builder.headers = getHttpHeaders()

        //ignore SSL errors
        builder.ignoreSSLIssues()

        //override default success handler
        builder.handler.success = { HttpResponseDecorator response, parsedResponseBody ->
            response.responseData = parsedResponseBody
            response
        }

        //override default error handler that throws HttpResponseException
        builder.handler.failure = builder.handler.success

        return builder
    }

    protected Map<String, String> getHttpHeaders() {

        return [(X_API_CLIENT): getApiClient(), (USER_AGENT): "groovy-http-client"]
    }

    protected abstract String getApiClient()

    protected abstract String getDefaultUri()
}