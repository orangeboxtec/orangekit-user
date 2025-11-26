package com.orangebox.kit.user.loginregister

import com.orangebox.kit.user.util.SecuredUser
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Path("/loginRegister")
class LoginRegisterRestService {

    @Inject
    private lateinit var loginRegister: LoginRegisterService


//    @SecuredUser
    @GET
    @Path("/loginRegisterDashboard/{userId}/{type}/{page}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun loginRegisterDashboard(@PathParam("userId") userId: String, @PathParam("type") type: String, @PathParam("page") page: Int): Map<String, *>? {
        return loginRegister.loginRegisterDashboard(userId, type, page)
    }

    @GET
    @Path("/loginRegisterDashboard")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun loginRegisterDashboardUsers(@QueryParam("usersId") usersId: List<String>?,
                                    @QueryParam("type") type: String,
                                    @QueryParam("page") page: Int?): Map<String, *>? {
        return loginRegister.loginRegisterDashboardUsers(usersId, type, page)
    }

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun test() {
        loginRegister.test()
    }
}