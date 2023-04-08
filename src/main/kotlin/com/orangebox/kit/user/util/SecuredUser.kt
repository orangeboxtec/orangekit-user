package com.orangebox.kit.user.util

import javax.ws.rs.NameBinding

//the method that use the @Secured annotation can only be consumed 
//by a admin portal (verified by a token in a filter)
@NameBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class SecuredUser 