package com.opus.readerparser.core.util

/**
 * Pure‑Kotlin title matcher for lightweight in‑memory search.
 *
 * Matching is case‑insensitive (lowercased) with leading/trailing whitespace
 * stripped before comparison.  Supports:
 *
 * 1. **Exact substring match** — the query (after normalisation) appears
 *    anywhere in the title (edit distance 0).
 * 2. **Fuzzy substring match** — the query, with at most one character
 *    insertion, deletion, or substitution (edit distance ≤ 1), appears as
 *    a substring of the title.
 *
 * Example: "Solo" matches "Solo Leveling" (substring), "Solo Levelin"
 * matches "Solo Leveling" (edit distance 1 — one deletion).
 *
 * @see matches
 * @see editDistance
 */
object TitleMatcher {

    private val whitespace = Regex("\\s+")

    /**
     * Returns `true` when [query] matches [title] using substring or
     * fuzzy substring matching (edit distance ≤ 1) after normalising
     * both strings.
     */
    fun matches(query: String, title: String): Boolean {
        val q = normalise(query)
        val t = normalise(title)
        if (q.isEmpty()) return true // empty query matches everything

        // 1. Exact substring match
        if (t.contains(q)) return true

        // 2. Sliding-window fuzzy match: for every window of length
        //    qLen-1, qLen, or qLen+1 within t, check edit distance ≤ 1.
        val qLen = q.length
        val tLen = t.length

        for (winLen in (qLen - 1)..(qLen + 1)) {
            if (winLen < 1 || winLen > tLen) continue
            val maxStart = tLen - winLen
            for (start in 0..maxStart) {
                val window = t.substring(start, start + winLen)
                if (editDistance(q, window) <= 1) return true
            }
        }
        return false
    }

    /**
     * Returns the edit distance (Levenshtein) between two already‑normalised
     * strings, capped at 2 so that any distance ≥ 2 is reported as 2 and the
     * algorithm short‑circuits early.
     */
    fun editDistance(a: String, b: String): Int {
        // Quick equality check
        if (a == b) return 0

        val lenA = a.length
        val lenB = b.length

        // Length difference > 1 → distance at least 2
        if (kotlin.math.abs(lenA - lenB) > 1) return 2

        // Ensure a is the shorter (or equal) string
        val (shorter, longer) = if (lenA <= lenB) a to b else b to a
        val sLen = shorter.length
        val lLen = longer.length

        if (sLen == lLen) {
            // Same length: count differing characters
            var diffs = 0
            for (i in 0 until sLen) {
                if (shorter[i] != longer[i]) {
                    diffs++
                    if (diffs > 1) return 2
                }
            }
            return diffs // 0 or 1 at this point
        }

        // Lengths differ by exactly 1 (already checked diff ≤ 1 above).
        // One insertion / deletion separates them.
        var sIdx = 0
        var lIdx = 0
        var diffs = 0
        while (sIdx < sLen && lIdx < lLen) {
            if (shorter[sIdx] != longer[lIdx]) {
                diffs++
                if (diffs > 1) return 2
                lIdx++ // skip one char in the longer string (insertion into shorter)
            } else {
                sIdx++
                lIdx++
            }
        }
        // Any remaining characters in the longer string after the loop count as
        // an insertion/deletion, but we already know the length difference is 1,
        // so at most one char should remain.
        if (lIdx < lLen || sIdx < sLen) diffs++
        return if (diffs > 1) 2 else diffs
    }

    private fun normalise(s: String): String =
        s.trim().lowercase().replace(whitespace, " ")
}
