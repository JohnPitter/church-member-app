package com.churchmanagement.mobile.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MONTHS_PT = listOf(
    "jan", "fev", "mar", "abr", "mai", "jun",
    "jul", "ago", "set", "out", "nov", "dez"
)

private val MONTHS_FULL_PT = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

/** Nome do mês por extenso (1-12). */
fun monthNamePt(month: Int): String = MONTHS_FULL_PT.getOrElse(month - 1) { "" }

/** Data de hoje no fuso do dispositivo. */
fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

/** Ex.: 09/05/2026 */
fun Instant.toShortDate(): String {
    val dt = toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.dayOfMonth.toString().padStart(2, '0')
    val month = dt.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${dt.year}"
}

/** Ex.: 09 mai 2026 */
fun Instant.toLongDate(): String {
    val dt = toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.dayOfMonth.toString().padStart(2, '0')
    val month = MONTHS_PT[dt.monthNumber - 1]
    return "$day $month ${dt.year}"
}
