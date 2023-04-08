package com.orangebox.kit.user.preference

import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId
import java.util.*
import kotlin.collections.ArrayList

@OKEntity("userPreferences")
class UserPreferences {

    @OKId
    var id: String? = null
    var idUser: String? = null
    var creationDate: Date? = null
    var preferences: ArrayList<Preference>? = null

    constructor(id: String?) {
        this.id = id
    }

    constructor()
}