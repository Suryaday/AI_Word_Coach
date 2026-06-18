package com.wordcoach.app.speech

import java.util.Locale
import kotlin.math.max

/**
 * Turns the speech recognizer's guesses into a friendly 1-5 star rating.
 *
 * It combines two ideas:
 *  1. How closely the spelling matches (edit distance).
 *  2. How closely the *sound* matches (a Soundex phonetic code).
 *
 * The best score across all recognizer hypotheses is used, so the learner
 * is rewarded if any of the device's guesses is a good match.
 */
object PronunciationScorer {

    data class Result(
        val stars: Int,          // 1..5
        val similarity: Double,  // 0.0..1.0
        val heardWord: String,   // best matching hypothesis (cleaned)
        val message: String      // encouraging feedback
    )

    /**
     * @param target      the word the learner is trying to say
     * @param hypotheses  the list of guesses returned by the recognizer
     */
    fun score(target: String, hypotheses: List<String>): Result {
        val cleanTarget = normalize(target)
        if (cleanTarget.isEmpty() || hypotheses.isEmpty()) {
            return Result(1, 0.0, "", "I couldn't hear that. Try once more.")
        }

        var best = 0.0
        var bestHeard = ""

        for (raw in hypotheses) {
            // A hypothesis may contain several words; check each token plus
            // the whole phrase, and keep the closest.
            val candidates = (raw.split(Regex("\\s+")) + raw)
            for (candidate in candidates) {
                val clean = normalize(candidate)
                if (clean.isEmpty()) continue
                val sim = similarity(cleanTarget, clean)
                if (sim > best) {
                    best = sim
                    bestHeard = clean
                }
            }
        }

        val stars = when {
            best >= 0.92 -> 5
            best >= 0.78 -> 4
            best >= 0.60 -> 3
            best >= 0.40 -> 2
            else -> 1
        }
        return Result(stars, best, bestHeard, messageFor(stars))
    }

    /** Combined spelling + sound similarity, range 0.0 .. 1.0. */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val distance = levenshtein(a, b)
        val charSim = 1.0 - distance.toDouble() / max(a.length, b.length)
        val phoneticBonus = if (soundex(a) == soundex(b)) 0.18 else 0.0
        return (charSim + phoneticBonus).coerceIn(0.0, 1.0)
    }

    private fun normalize(text: String): String =
        text.lowercase(Locale.US).filter { it.isLetter() }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,       // deletion
                    curr[j - 1] + 1,   // insertion
                    prev[j - 1] + cost // substitution
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }

    /** Classic Soundex phonetic code (e.g. "Robert" -> "R163"). */
    private fun soundex(input: String): String {
        if (input.isEmpty()) return ""
        val s = input.uppercase(Locale.US)
        val first = s[0]
        val sb = StringBuilder().append(first)
        var lastCode = codeFor(first)
        for (i in 1 until s.length) {
            val code = codeFor(s[i])
            if (code != '0' && code != lastCode) {
                sb.append(code)
                if (sb.length == 4) break
            }
            // Vowels reset the "previous code" so repeats across vowels count.
            if (s[i] != 'H' && s[i] != 'W') lastCode = code
        }
        while (sb.length < 4) sb.append('0')
        return sb.toString()
    }

    private fun codeFor(c: Char): Char = when (c) {
        'B', 'F', 'P', 'V' -> '1'
        'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
        'D', 'T' -> '3'
        'L' -> '4'
        'M', 'N' -> '5'
        'R' -> '6'
        else -> '0'
    }

    private fun messageFor(stars: Int): String = when (stars) {
        5 -> "Perfect! That sounded just right."
        4 -> "Great job! Very close to perfect."
        3 -> "Good effort. Listen again and try once more."
        2 -> "Getting there. Tap the speaker and repeat slowly."
        else -> "Keep trying. Tap the speaker to hear it again."
    }
}
