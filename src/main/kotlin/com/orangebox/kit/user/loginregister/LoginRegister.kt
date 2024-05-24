package com.orangebox.kit.user.loginregister

import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId
import java.util.*

@OKEntity("loginRegister")
class LoginRegister {

    @OKId
    var id: String? = null

    var userId: String? = null

    var loginDate: Date? = null
}