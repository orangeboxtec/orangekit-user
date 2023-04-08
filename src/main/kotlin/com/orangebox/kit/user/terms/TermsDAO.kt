package com.orangebox.kit.user.terms

import com.orangebox.kit.core.dao.AbstractDAO
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class TermsDAO : AbstractDAO<Terms>(Terms::class.java) {
    override fun getId(bean: Terms): Any? {
        return bean.language
    }
}