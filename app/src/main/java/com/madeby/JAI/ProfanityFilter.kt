package com.madeby.JAI

import java.util.Locale
import java.util.regex.Pattern

object ProfanityFilter {

    private val VULGAR_HINDI_WORDS = listOf(
        "आंड़","आंड","आँड","बहनचोद","बेहेनचोद","भेनचोद","बकचोद","बकचोदी","बेवड़ा","बेवड़े",
        "बेवकूफ","भड़ुआ","भड़वा","भोसड़ा","भोसड़ीके","भोसड़ीकी","भोसड़ीवाला","भोसड़ीवाले",
        "भोसरचोदल","भोसदचोद","भोसड़ाचोदल","भोसड़ाचोद","बब्बे","बूबे","बुर","चरसी","चूचे",
        "चूची","चुची","चोद","चुदने","चुदवा","चुदवाने","चूत","चूतिया","चुटिया","चूतिये",
        "चुत्तड़","चूत्तड़","दलाल","दलले","फट्टू","गधा","गधे","गधालंड","गांड","गांडू",
        "गंडफट","गंडिया","गंडिये","गू","गोटे","हग","हग्गू","हगने","हरामी","हरामजादा",
        "हरामज़ादा","हरामजादे","हरामज़ादे","हरामखोर","झाट","झाटू","कुत्ता","कुत्ते","कुतिया",
        "कुत्ती","लेंडी","लोड़े","लौड़े","लौड़ा","लोड़ा","लौडा","लिंग","लोडा","लोडे","लंड",
        "लौंडा","लौंडे","लौंडी","लौंडिया","लुल्ली","मार","मारो","मारूंगा","मादरचोद","मादरचूत",
        "मादरचुत","मम्मे","मूत","मुत","मूतने","मुतने","मूठ","मुठ","नुननी","नुननु","पाजी",
        "पेसाब","पेशाब","पिल्ला","पिल्ले","पिसाब","पोरकिस्तान","रांड","रंडी","सुअर","सूअर",
        "टट्टे","टट्टी","उल्लू"
    )

    private val VULGAR_HINGLISH_WORDS = hashSetOf(
        "aad","aand","bahenchod","behenchod","bhenchod","bhenchodd","bc","bakchod","bakchodd",
        "bakchodi","bevda","bewda","bevdey","bewday","bevakoof","bevkoof","bevkuf","bewakoof",
        "bewkoof","bewkuf","bhadua","bhaduaa","bhadva","bhadvaa","bhadwa","bhadwaa","bhosada",
        "bhosda","bhosdaa","bhosdike","bhonsdike","bsdk","bhosdiki","bhosdiwala","bhosdiwale",
        "bhosadchodal","bhosadchod","babbe","babbey","bube","bubey","bur","burr","buurr","buur",
        "charsi","chooche","choochi","chuchi","chhod","chod","chodd","chudne","chudney","chudwa",
        "chudwaa","chudwane","chudwaane","choot","chut","chute","chutia","chutiya","chutiye",
        "chuttad","chutad","dalaal","dalal","dalle","dalley","fattu","gadha","gadhe","gadhalund",
        "gaand","gand","gandu","gaandu","gandfat","gandfut","gandiya","gandiye","goo","gu","gote","gotey",
        "gotte","hag","haggu","hagne","hagney","harami","haramjada","haraamjaada","haramzyada",
        "haraamzyaada","haraamjaade","haraamzaade","haraamkhor","haramkhor","jhat","jhaat","jhaatu",
        "jhatu","kutta","kutte","kuttey","kutia","kutiya","kuttiya","kutti","landi","landy",
        "laude","laudey","laura","lora","lauda","ling","loda","lode","lund","launda","lounde",
        "laundey","laundi","loundi","laundiya","loundiya","lulli","maar","maro","marunga","madarchod",
        "madarchodd","madarchood","madarchoot","madarchut","mc","mamme","mammey","moot","mut",
        "mootne","mutne","mooth","muth","nunni","nunnu","paaji","paji","pesaab","pesab","peshaab",
        "peshab","pilla","pillay","pille","pilley","pisaab","pisab","pkmkb","porkistan","raand",
        "rand","randi","randy","suar","tatte","tatti","tatty","ullu","tmkc","mkb","bkl","randwa",
        "saala","kamina","kaminey"
    )

    private val VULGAR_ENGLISH_WORDS = hashSetOf(
        "fuck", "fucking", "fucked", "fucker", "fuckers", "shit", "bitch", "bitches",
        "asshole", "bastard", "cunt", "dick", "pussy", "whore", "slut", "nigger", "nigga",
        "faggot", "cock", "penis", "vagina", "boobs", "boob", "tits", "tit", "dildo",
        "porn", "porno", "pornography", "sex", "sexy", "nude", "nudes", "hitler", "nazi",
        "terrorist", "suicide", "murder", "rape", "rapist"
    )

    private val VULGAR_ENGLISH_REGEX = Pattern.compile(
        "\\b(f+[u*@_.-]*c+k+|s+h+[i*@_.-]*t+|b+[i*@_.-]*t+c+h+|a+s+s+h+o+l+e+|d+[i*@_.-]*c+k+|p+u+s+s+y+|c+u+n+t+|w+h+o+r+e+|s+l+u+t+|n+[i*@_.-]*g+g+[a*e*r*]*|f+a+g+g*o*t*|r+e+t+a+r+d+|b+a+s+t+a+r+d+|p+o+r+n+|b+o+o+b+s+|t+i+t+s+|d+i+l+d+o+)\\b",
        Pattern.CASE_INSENSITIVE
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

    /**
     * General profanity scanner returns true if rawText contains vulgar / inappropriate words
     */
    fun hasProfanity(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        val trimmed = rawText.trim()

        // 1. Direct Devanagari check
        for (w in VULGAR_HINDI_WORDS) {
            if (trimmed.contains(w)) return true
        }

        // 2. English Regex Pattern Check
        if (VULGAR_ENGLISH_REGEX.matcher(trimmed).find()) return true

        // 3. Normalized Text & Leet Translation
        val normalized = normalizeText(trimmed)
        if (VULGAR_ENGLISH_REGEX.matcher(normalized).find()) return true

        // 4. Token & Compact checks
        val tokens = normalized.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        for (token in tokens) {
            if (VULGAR_HINGLISH_WORDS.contains(token) || VULGAR_ENGLISH_WORDS.contains(token)) {
                return true
            }
        }

        // 5. Acronym / Substring checks
        val compact = normalized.filter { it.isLetterOrDigit() }
        for (word in VULGAR_HINGLISH_WORDS) {
            if (word.length >= 4 && compact.contains(word)) return true
        }
        for (word in VULGAR_ENGLISH_WORDS) {
            if (word.length >= 4 && compact.contains(word)) return true
        }

        return false
    }

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
        if (hasProfanity(trimmed)) {
            return CheckResult(
                isClean = false,
                sanitizedText = trimmed,
                reason = "Display name contains inappropriate or prohibited words 🛡️"
            )
        }
        return CheckResult(isClean = true, sanitizedText = trimmed)
    }

    fun checkSubjectName(rawText: String?): CheckResult {
        if (rawText.isNullOrBlank()) {
            return CheckResult(isClean = false, sanitizedText = "", reason = "Subject name cannot be empty.")
        }
        val trimmed = rawText.trim()
        if (trimmed.length > 25) {
            return CheckResult(isClean = false, sanitizedText = trimmed.take(25), reason = "Subject name cannot exceed 25 characters.")
        }
        if (hasProfanity(trimmed)) {
            return CheckResult(
                isClean = false,
                sanitizedText = trimmed,
                reason = "Subject name contains inappropriate or prohibited words 🛡️"
            )
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
            } else if (ch.isLetterOrDigit() || ch == ' ') {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
