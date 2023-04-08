package com.orangebox.kit.user.preference

import com.orangebox.kit.user.util.UserBaseRestService
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/userPreference")
class PreferenceRestService : UserBaseRestService() {

    @Inject
    private lateinit var userPreferenceService: UserPreferenceService

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/save")
    fun savePreference(preference: Preference) {
        userPreferenceService.save(preference)
    }

    @GET
    @Path("/load/{idUser}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun load(@PathParam("idUser") idUser: String) {
        userPreferenceService.load(idUser)
    }
}