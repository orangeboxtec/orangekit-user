package com.orangebox.kit.user.util

import com.orangebox.kit.user.UserService
import javax.annotation.Priority
import javax.inject.Inject
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.ext.Provider

@SecuredUser
@Provider
@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter : ContainerRequestFilter {

    @Inject
    private lateinit var userService: UserService

    override fun filter(requestContext: ContainerRequestContext) {

        // Get the HTTP Authorization header from the request
        val authorizationHeader: String = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)

        // Check if the HTTP Authorization header is present and formatted correctly 
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw NotAuthorizedException("Authorization header must be provided")
        }

        // Extract the token from the HTTP Authorization header
        val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
        try {
            validateToken(token)
        } catch (e: Exception) {
            throw NotAuthorizedException(e.message)
        }
    }

    private fun validateToken(token: String) {
        val validated: Boolean = userService.checkToken(token)
        if (!validated) {
            throw Exception("Invalid Token")
        }
    }
}