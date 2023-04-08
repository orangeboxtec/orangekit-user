package com.orangebox.kit.user.terms

import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class TermsService {

    @Inject
    private lateinit var termsDAO: TermsDAO

    @Throws(Exception::class)
    fun retrieveByLanguage(language: String?): Terms? {
        return termsDAO.retrieve(Terms(language))
    }
}