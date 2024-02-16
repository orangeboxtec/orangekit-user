package com.orangebox.kit.user.preference

import com.orangebox.kit.admin.userb.UserBService
import com.orangebox.kit.core.exception.BusinessException
import com.orangebox.kit.user.UserService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

@ApplicationScoped
class UserPreferenceService {


    @Inject
    private lateinit var userPreferencesDAO: UserPreferencesDAO

    @Inject
    private lateinit var userService: UserService

    @Inject
    private lateinit var userBService: UserBService


    fun save(preference: Preference) {

        if(preference.key == null || preference.value == null){
            throw BusinessException("key and value fields required")
        }

        var userPreferences = load(preference.idUser!!)

        if (userPreferences == null) {
            userPreferences = UserPreferences()
            userPreferences.creationDate = Date()
            userPreferences.idUser = preference.idUser
            userPreferencesDAO.insert(userPreferences)
        }

        if(userPreferences.preferences == null){
            userPreferences.preferences = ArrayList()
        }

        val preferenceDB = userPreferences.preferences?.find { p -> p.key == preference.key }
        if(preferenceDB != null){
            preferenceDB.value = preference.value
        } else {
            userPreferences.preferences?.add(preference)
        }

        userPreferencesDAO.update(userPreferences)
    }


    fun load(idUser: String): UserPreferences?{
        return userPreferencesDAO.retrieve(userPreferencesDAO.createBuilder()
            .appendParamQuery("idUser", idUser)
            .build())
    }
}