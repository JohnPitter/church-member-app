package com.churchmanagement.mobile.util

/**
 * Monta o link do WhatsApp (wa.me) a partir de um telefone brasileiro.
 * Retorna null se o número for inválido/ausente. Prefixa o código do país (55) quando necessário.
 */
fun whatsappUrl(phone: String?): String? {
    val digits = phone?.filter { it.isDigit() } ?: return null
    if (digits.length < 10) return null
    val withCountry = if (digits.startsWith("55") && digits.length >= 12) digits else "55$digits"
    return "https://wa.me/$withCountry"
}
