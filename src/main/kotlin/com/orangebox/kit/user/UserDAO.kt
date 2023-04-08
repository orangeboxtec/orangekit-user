package com.orangebox.kit.user

import com.orangebox.kit.core.dao.AbstractDAO
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserDAO : AbstractDAO<User>(User::class.java) {
    override fun getId(bean: User): Any? {
        return bean.id
    }

    fun retrieveByIdFacebook(idFacebook: String): User? {
        return retrieve(
            createBuilder()
                .appendParamQuery("idFacebook", idFacebook)
                .build()
        )
    }

    fun retrieveByIdGoogle(idGoogle: String): User? {
        return retrieve(
            createBuilder()
                .appendParamQuery("idGoogle", idGoogle)
                .build()
        )
    }

    fun retrieveByIdApple(idApple: String): User? {
        return retrieve(
            createBuilder()
                .appendParamQuery("idApple", idApple)
                .build()
        )
    }

    fun listByFieldInfo(field: String, value: String): List<User>? {
        return search(
            createBuilder()
                .appendParamQuery("info.$field", value)
                .build()
        )
    }
}