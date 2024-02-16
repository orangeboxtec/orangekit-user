package com.orangebox.kit.user.util

import com.orangebox.kit.user.User
import com.orangebox.kit.user.UserService
import jakarta.inject.Inject
import jakarta.ws.rs.NotAuthorizedException
import org.jboss.resteasy.reactive.RestHeader

open class UserBaseRestService {

    @RestHeader("AUTHORIZATION")
    lateinit var authorizationHeader: String

    @Inject
    private lateinit var userService: UserService

    protected val userTokenSession: User?
        get() {
            // Check if the HTTP Authorization header is present and formatted correctly
            if (!authorizationHeader.startsWith("Bearer ")) {
                throw NotAuthorizedException("Authorization header must be provided")
            }

            // Extract the token from the HTTP Authorization header
            val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
            return userService.retrieveByToken(token)
        }
}