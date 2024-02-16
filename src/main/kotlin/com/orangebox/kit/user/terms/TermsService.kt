package com.orangebox.kit.user.terms

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class TermsService {

    @Inject
    private lateinit var termsDAO: TermsDAO

    @Throws(Exception::class)
    fun retrieveByLanguage(language: String?): Terms? {
        return termsDAO.retrieve(Terms(language))
    }
}