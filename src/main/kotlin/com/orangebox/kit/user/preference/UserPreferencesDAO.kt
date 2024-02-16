package com.orangebox.kit.user.preference

import com.orangebox.kit.core.dao.AbstractDAO
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UserPreferencesDAO : AbstractDAO<UserPreferences>(UserPreferences::class.java) {
    override fun getId(bean: UserPreferences): Any? {
        return bean.id
    }
}