package com.orangebox.kit.user

import com.orangebox.kit.core.address.AddressInfo
import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId
import com.orangebox.kit.core.file.GalleryItem

import com.orangebox.kit.core.user.GeneralUser
import java.time.LocalDate
import java.util.*
import javax.json.bind.annotation.JsonbDateFormat
import kotlin.collections.ArrayList

@OKEntity("user")
class User : GeneralUser {
    @OKId
    override var id: String? = null
    var idFacebook: String? = null
    var idGoogle: String? = null
    var idApple: String? = null
    var phone: String? = null
    override var phoneNumber: Long? = null
    override var tokenFirebase: String? = null
    var document: String? = null
    override var phoneCountryCode: Int? = null
    override var name: String? = null
    override var lastName: String? = null
    var idObj: String? = null
    var nameObj: String? = null
    var code: String? = null
    override var email: String? = null
    var cpf: String? = null
    var lastAddress: AddressInfo? = null
    var lastLogin: Date? = null
    var password: String? = null
    var oldPassword: String? = null
    var tempPassword: String? = null
    var salt: String? = null
    @JsonbDateFormat("yyyy-MM-dd")
    var birthDate: LocalDate? = null
    var birthDateStr: String? = null
    var gender: String? = null
    var language: String? = null
    var locale: String? = null
    var creationDate: Date? = null
    var phoneConfirmed: Boolean? = null
    var emailConfirmed: Boolean? = null
    var userConfirmed: Boolean? = null
    var token: String? = null
    var type: String? = null
    var idAvatar: String? = null
    var tokenExpirationDate: Date? = null
    var status: String? = null
    var info: HashMap<String, Any>? = null
    var gallery: ArrayList<GalleryItem>? = null
    override var urlImage: String? = null
    var userTokens: ArrayList<UserToken>? = null

    constructor()
    constructor(id: String?) {
        this.id = id
    }
}