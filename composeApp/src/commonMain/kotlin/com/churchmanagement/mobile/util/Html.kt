package com.churchmanagement.mobile.util

private val TAG_REGEX = Regex("<[^>]*>")
private val WHITESPACE_REGEX = Regex("[ \\t\\x0B\\f]+")
private val MANY_NEWLINES = Regex("\\n{3,}")

/**
 * Remove marcação HTML (rich-text do blog) para exibição em texto simples.
 * Converte <br>/<p> em quebras de linha e decodifica entidades comuns.
 */
fun String.stripHtml(): String {
    if (isBlank()) return this
    return this
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)</div>"), "\n")
        .replace(Regex("(?i)</li>"), "\n")
        .replace(TAG_REGEX, "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(WHITESPACE_REGEX, " ")
        .replace(MANY_NEWLINES, "\n\n")
        .trim()
}
