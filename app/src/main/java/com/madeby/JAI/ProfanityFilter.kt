package com.madeby.JAI

import java.util.Locale
import java.util.regex.Pattern

object ProfanityFilter {

    private val BLOCKED_WORDS = hashSetOf(
        // English blocklist
        "fuck", "shit", "bitch", "asshole", "bastard", "cunt", "dick", "pussy", "whore", "slut",
        "nigger", "nigga", "faggot", "cock", "penis", "vagina", "boobs", "tits", "porn", "sex",
        "nude", "nudes", "hitler", "nazi", "terrorist", "suicide", "kill", "murder", "rape",
        
        // Hindi / Hinglish blocklist
        "chutiya", "chutya", "bhenchod", "bhosdike", "bhosadike", "madarchod", "gand", "gaand",
        "gandu", "gaandu", "lauda", "lavda", "loda", "lund", "harami", "randi", "saala", "kamina",
        "bhosda", "chut", "jhaat", "jhat", "tatte", "tatton", "randwa", "kutta", "kaminey",
        "mc", "bc", "bsdk", "tmkc", "mkb", "bkl", "c-h-u-t-i-y-a", "l-u-n-d"
    )

    private val LEET_REPLACEMENTS = mapOf(
        '0' to 'o',
        '1' to 'i',
        '3' to 'e',
        '4' to 'a',
        '@' to 'a',
        '$' to 's',
        '5' to 's',
        '7' to 't',
        '+' to 't',
        '!' to 'i'
    )

    data class CheckResult(
        val isClean: Boolean,
        val sanitizedText: String,
        val reason: String? = null
    )

    fun checkName(rawText: String?): CheckResult {
        if (rawText.isNullOrBlank()) {
            return CheckResult(isClean = false, sanitizedText = "", reason = "Display name cannot be empty.")
        }

        val trimmed = rawText.trim()
        if (trimmed.length < 2) {
            return CheckResult(isClean = false, sanitizedText = trimmed, reason = "Display name must be at least 2 characters.")
        }
        if (trimmed.length > 30) {
            return CheckResult(isClean = false, sanitizedText = trimmed.take(30), reason = "Display name cannot exceed 30 characters.")
        }

        // Check against normalized alphanumeric string
        val normalized = normalizeText(trimmed)
        for (blocked in BLOCKED_WORDS) {
            if (containsWord(normalized, blocked)) {
                return CheckResult(
                    isClean = false,
                    sanitizedText = trimmed,
                    reason = "Display name contains inappropriate or prohibited words."
                )
            }
        }

        return CheckResult(isClean = true, sanitizedText = trimmed)
    }

    private fun normalizeText(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val sb = StringBuilder()
        for (ch in lower) {
            val rep = LEET_REPLACEMENTS[ch]
            if (rep != null) {
                sb.append(rep)
            } else if (ch.isLetterOrDigit()) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun containsWord(normalized: String, blocked: String): Boolean {
        if (normalized.contains(blocked)) return true
        // Also check if words match as standalone tokens
        val regex = Pattern.compile("\\b" + Pattern.quote(blocked) + "\\b", Pattern.CASE_INSENSITIVE)
        return regex.matcher(normalized).find()
    }
}
