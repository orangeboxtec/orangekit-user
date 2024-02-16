package com.orangebox.kit.user.util

import com.orangebox.kit.user.UserService
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.ext.Provider

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