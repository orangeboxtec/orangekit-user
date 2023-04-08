package com.orangebox.kit.user.terms

import com.orangebox.kit.core.annotation.OKEntity
import com.orangebox.kit.core.annotation.OKId

@OKEntity(name = "terms")
class Terms {
	@OKId
    var language: String? = null
	var terms: String? = null
    constructor()
    constructor(language: String?) {
        this.language = language
    }
}