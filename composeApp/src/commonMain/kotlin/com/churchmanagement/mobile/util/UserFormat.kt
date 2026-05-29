package com.churchmanagement.mobile.util

import com.churchmanagement.mobile.domain.AppUser

/** Primeiro nome do usuário (ou prefixo do e-mail), para saudações. */
fun AppUser.firstName(): String {
    val name = displayName?.trim().orEmpty()
    if (name.isNotEmpty()) return name.substringBefore(' ')
    return email?.substringBefore('@') ?: "membro"
}
