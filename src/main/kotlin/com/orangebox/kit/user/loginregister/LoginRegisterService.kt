package com.orangebox.kit.user.loginregister

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.bson.Document

@ApplicationScoped
class LoginRegisterService {

    @Inject
    private lateinit var loginRegisterDAO: LoginRegisterDAO

    fun save(loginRegister: LoginRegister){
        loginRegisterDAO.insert(loginRegister)
    }

    fun loginRegisterDashboard(userId: String, type: String, page: Int): Map<String, *>? {
        return when(type){
            "diario" -> {
                loginRegisterDAO.loginRegisterDaily(userId, page)
            }

            "semanal" -> {
                loginRegisterDAO.loginRegisterWeekly(userId, page)
            }

            "mensal" -> {
                loginRegisterDAO.loginRegisterMonthly(userId, page)
            }

            else -> {
                null
            }
        }
    }
    fun test(){ }
}