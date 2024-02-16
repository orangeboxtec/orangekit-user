package com.orangebox.kit.user.util

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider

@Provider
class CORSFilter : ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        responseContext.headers.putSingle("Access-Control-Allow-Origin", "*")
        responseContext.headers.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
        val reqHeader = requestContext.getHeaderString("Access-Control-Request-Headers")
        if (reqHeader != null && reqHeader !== "") {
            responseContext.headers.putSingle("Access-Control-Allow-Headers", reqHeader)
        }
    }
}