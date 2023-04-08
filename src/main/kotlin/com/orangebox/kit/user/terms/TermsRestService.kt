package com.orangebox.kit.user.terms

import com.orangebox.kit.user.util.UserBaseRestService
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


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