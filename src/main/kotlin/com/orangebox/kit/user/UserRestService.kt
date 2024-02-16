package com.orangebox.kit.user

import com.orangebox.kit.admin.userb.UserBService
import com.orangebox.kit.admin.util.SecuredAdmin
import com.orangebox.kit.authkey.UserAuthKey
import com.orangebox.kit.core.bucket.BucketService
import com.orangebox.kit.core.dto.ResponseList
import com.orangebox.kit.core.exception.BusinessException
import com.orangebox.kit.core.file.FileUpload
import com.orangebox.kit.core.file.GalleryItem
import com.orangebox.kit.user.util.SecuredUser
import com.orangebox.kit.user.util.UserBaseRestService
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.util.*

@Path("/user")
class UserRestService : UserBaseRestService() {

    @Inject
    protected lateinit var userService: UserService

    @Inject
    private lateinit var userBService: UserBService

    @Inject
    private lateinit var bucketService: BucketService

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/loginFB")
    fun loginFB(usMon: User): User? {
        return userService.loginFB(usMon)
    }

    @SecuredAdmin
    @POST
    @Consumes("application/json")
    @Produces("application/json;charset=utf-8")
    @Path("/searchAdmin")
    fun searchAdmin(userSearch: UserSearch): ResponseList<UserCard> {
        return userService.searchAdmin(userSearch)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/loginGoogle")
    fun loginGoogle(usMon: User): User? {
        return userService.loginGoogle(usMon)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/loginApple")
    fun loginApple(usMon: User): User? {
        return userService.loginApple(usMon)
    }

    @GET
    @Path("/load/{idUser}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun load(@PathParam("idUser") idUser: String): User? {
        return userService.retrieve(idUser)
    }


    @GET
    @Path("/checkEmailExists/{email}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun checkEmailExists(@PathParam("email") email: String?): Boolean {
        return userService!!.checkEmailExists(email!!)
    }

    @SecuredUser
    @GET
    @Path("/loggedUser/{token}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun loggedUser(@PathParam("token") token: String): User? {
        return userService.retrieveByToken(token)
    }

    @SecuredUser
    @GET
    @Path("/loggedUserCard")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun loggedUserCard(): UserCard {
        val user = userTokenSession
        return if (user != null) {
            userService.generateCard(user)
        } else {
            throw WebApplicationException(Response.Status.NOT_FOUND)
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/login")
    fun login(usMon: User): User? {
        return userService.login(usMon)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/autoLogin")
    fun autoLogin(usMon: User): User? {
        return userService.autoLogin(usMon)
    }

    @SecuredUser
    @PUT
    @Path("/logout/{idUser}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun logout(@PathParam("idUser") idUser: String) {
        userService.logout(idUser)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/newUser")
    fun newUser(usMon: User): User {
        userService.createNewUser(usMon)
        return usMon
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveAnonymous")
    fun saveAnonymous(user: User): User {
        return userService.saveAnonymous(user)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveByPhone")
    fun saveByPhone(usMon: User): User {
        return userService.saveByPhone(usMon)
    }

    @SecuredUser
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePhoneUser")
    fun updatePhoneUser(usMon: User): User? {
        return userService.updatePhoneUser(usMon)
    }

    @SecuredUser
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/update")
    fun update(usMon: User) {
        userService.updateFromClient(usMon)
    }

    //@SecuredUser
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePassword")
    fun updatePassword(usMon: User): String? {
        return userService.updatePassword(usMon)
    }

    @SecuredUser
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updateStartInfo")
    fun updateStartInfo(userStartInfo: UserStartInfo) {
        userService.updateStartInfo(userStartInfo)
    }


    @SecuredUser
    @POST
    @Path("/saveUserImage")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    fun saveUserImage(fileUpload: FileUpload) {
        val user = userService.retrieve(fileUpload.idObject!!)
            ?: throw BusinessException("User with id  '" + fileUpload.idObject + "' not found to attach photo")

        val idPhoto: String
        if (fileUpload.idSubObject == null) {
            idPhoto = UUID.randomUUID().toString()
            val gi = GalleryItem()
            gi.id = idPhoto
            if (user.gallery == null) {
                user.gallery = ArrayList()
            }
            user.idAvatar = idPhoto
            user.gallery!!.add(gi)
        } else {
            idPhoto = fileUpload.idSubObject!!
        }
        user.urlImage = bucketService.saveImage(fileUpload, "", "user/" + user.id + "/" + idPhoto)
        userService.update(user)
    }


    @SecuredUser
    @GET
    @Path("/searchByName/{nameUser}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun searchByName(@PathParam("nameUser") name: String?): List<UserCard>? {
        return userService!!.searchByName(name!!)
    }

    @SecuredAdmin
    @GET
    @Path("/listAll")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun listAll(): List<UserCard>? {
        return userService.listAll()
    }

    @SecuredAdmin
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveAdmin")
    fun saveAdmin(user: User): User {
        userService.save(user)
        confirmUserEmail(user.id)
        return user
    }

    @SecuredUser
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/cancelUser")
    fun cancelUser(user: User) {
        userService.cancelUser(user.id!!)
    }

    @SecuredUser
    @PUT
    @Path("/confirmUserSMS/{idUser}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun confirmUserSMS(@PathParam("idUser") idUser: String) {
        userService.confirmUserSMS(idUser)
    }

    @SecuredUser
    @PUT
    @Path("/confirmUserEmail/{idUser}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Throws(
        Exception::class
    )
    fun confirmUserEmail(@PathParam("idUser") idUser: String?) {
        userService!!.confirmUserEmail(idUser!!)
    }

    @PUT
    @Path("/forgotPassword/{email}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun forgotPassword(@PathParam("email") email: String) {
        userService.forgotPassword(email)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/validateKey")
    fun validateKey(userAuthKey: UserAuthKey): Boolean {
        return userService.validateKey(userAuthKey)
    }

    @POST
    @SecuredAdmin
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/changeStatus")
    fun changeStatus(user: User) {
        userService.changeStatus(user.id!!)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/deleteUserImage")
    fun removeGallery(userSearch: UserSearch) {
        userService.removeGallery(userSearch)
    }

    @POST
    @SecuredAdmin
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/saveByAdmin")
    fun saveByAdmin(user: User): User {
        userService.saveByAdmin(user)
        return user
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/search")
    fun search(userSearch: UserSearch): List<UserCard>? {
        return userService.search(userSearch)
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/userSearchById/{id}")
    fun userSearchById(@PathParam("id") id: String): User? {
        return userService.userSearchById(id)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/setUserImage")
    fun setUserImage(search: UserSearch) {
        userService.setUserImage(search)
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/getUserById/{id}")
    fun getUserById(@PathParam("id") id: String?): User {
        return userService.getUserById(id!!)
    }


    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/setStatus/{id}/{status}")
    fun setStatus(@PathParam("id") id: String, @PathParam("status") status: String) {
        userService!!.setStatus(id, status)
    }

    @PUT
    @Path("/resetPassword/{email}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    fun resetPassword(@PathParam("email") email: String) {
        userService.forgotPasswordVerifySocialMedia(email)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/validateKeyAng")
    fun validateKeyAngular(userAuthKey: UserAuthKey): User? {
        return userService.validateKeyAngular(userAuthKey)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/updatePasswordForgot")
    fun updatePasswordForgot(usMon: User) {
        userService.updatePasswordForgot(usMon)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/newUserSendEmail")
    fun newUserSendEmailResetPassword(usMon: User): User {
        userService.newUserSendEmailResetPassword(usMon)
        return usMon
    }

}