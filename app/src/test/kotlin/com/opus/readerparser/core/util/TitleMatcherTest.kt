package com.opus.readerparser.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TitleMatcherTest {

    // -----------------------------------------------------------------
    // editDistance
    // -----------------------------------------------------------------

    @Test
    fun `editDistance is 0 for identical strings`() {
        assertThat(TitleMatcher.editDistance("hello", "hello")).isEqualTo(0)
    }

    @Test
    fun `editDistance is 1 for single substitution`() {
        assertThat(TitleMatcher.editDistance("hello", "hallo")).isEqualTo(1)
    }

    @Test
    fun `editDistance is 1 for single insertion`() {
        assertThat(TitleMatcher.editDistance("hello", "helloo")).isEqualTo(1)
    }

    @Test
    fun `editDistance is 1 for single deletion`() {
        assertThat(TitleMatcher.editDistance("hello", "helo")).isEqualTo(1)
    }

    @Test
    fun `editDistance is 2 for two substitutions`() {
        assertThat(TitleMatcher.editDistance("hello", "hxllo")).isEqualTo(1)
        assertThat(TitleMatcher.editDistance("hello", "hxllx")).isEqualTo(2)
    }

    @Test
    fun `editDistance is 2 when length differs by 2`() {
        assertThat(TitleMatcher.editDistance("hi", "hello")).isEqualTo(2)
    }

    @Test
    fun `editDistance is 2 for empty vs length-2 string`() {
        assertThat(TitleMatcher.editDistance("", "ab")).isEqualTo(2)
    }

    @Test
    fun `editDistance is 1 for empty vs single char`() {
        assertThat(TitleMatcher.editDistance("", "a")).isEqualTo(1)
    }

    @Test
    fun `editDistance is 0 for empty vs empty`() {
        assertThat(TitleMatcher.editDistance("", "")).isEqualTo(0)
    }

    // -----------------------------------------------------------------
    // matches (which normalises internally)
    // -----------------------------------------------------------------

    @Test
    fun `matches true for exact case-insensitive match`() {
        assertThat(TitleMatcher.matches("Solo Leveling", "solo leveling")).isTrue()
    }

    @Test
    fun `matches true for single typo`() {
        assertThat(TitleMatcher.matches("Solo Leveling", "Solo Levelin")).isTrue()
    }

    @Test
    fun `matches false when two chars differ`() {
        assertThat(TitleMatcher.matches("Solo Leveling", "Solo Levelex")).isFalse()
    }

    @Test
    fun `matches true with extra char`() {
        assertThat(TitleMatcher.matches("Solo Leveling", "Solo Levelingg")).isTrue()
    }

    @Test
    fun `matches true with missing char`() {
        assertThat(TitleMatcher.matches("Solo Leveling", "Solo Levelng")).isTrue()
    }

    @Test
    fun `matches true for completely different short strings with distance 1`() {
        assertThat(TitleMatcher.matches("a", "b")).isTrue()
    }

    @Test
    fun `matches true when single-char query matches a single-char window via substitution`() {
        // "a" has edit distance 1 from "b" (substitution), and "b" is a window of "bc"
        assertThat(TitleMatcher.matches("a", "bc")).isTrue()
    }

    @Test
    fun `matches false when query with distance 2 from any window`() {
        assertThat(TitleMatcher.matches("ab", "xyz")).isFalse()
    }

    @Test
    fun `matches ignores leading and trailing whitespace`() {
        assertThat(TitleMatcher.matches("  Solo Leveling  ", "solo leveling")).isTrue()
    }

    @Test
    fun `matches empty query returns true for any title`() {
        assertThat(TitleMatcher.matches("", "Anything")).isTrue()
    }

    @Test
    fun `matches blank query returns true for any title`() {
        assertThat(TitleMatcher.matches("  ", "Something")).isTrue()
    }

    @Test
    fun `matches one insert in longer string`() {
        // "abcd" vs "abXd" — insert 'X' → distance 1
        assertThat(TitleMatcher.matches("abcd", "abXcd")).isTrue()
    }

    @Test
    fun `matches across different character cases`() {
        assertThat(TitleMatcher.matches("the wandering inn", "The Wandering Inn")).isTrue()
    }

    @Test
    fun `matches single character substitution across case`() {
        assertThat(TitleMatcher.matches("the wandering inn", "the wandaring inn")).isTrue()
    }

    @Test
    fun `matches false for two character substitutions`() {
        assertThat(TitleMatcher.matches("abc", "xbc")).isTrue()
        assertThat(TitleMatcher.matches("abc", "xyc")).isFalse()
    }
}
