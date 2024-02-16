package com.orangebox.kit.user.terms

import com.orangebox.kit.user.util.UserBaseRestService
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Path("/terms")
class TermsRestService : UserBaseRestService() {

    @Inject
    private lateinit var termsService: TermsService

    @GET
    @Path("/load/{language}")
    @Produces(MediaType.TEXT_HTML + ";charset=utf-8")
    fun load(@PathParam("language") language: String): String? {
        val terms = termsService.retrieveByLanguage(language)
        return terms!!.terms
    }
}