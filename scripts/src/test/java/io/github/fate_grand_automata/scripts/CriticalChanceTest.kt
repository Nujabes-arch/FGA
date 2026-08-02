package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.fate_grand_automata.scripts.models.CriticalChanceClassifier
import io.github.fate_grand_automata.scripts.models.CriticalChanceMatch
import io.github.lib_automata.Region
import kotlin.test.Test

class CriticalChanceTest {
    private val positiveSamples = listOf(
        50, 60, 70, 70, 50,
        50, 100, 50, 100, 100,
        100, 60, 100, 80, 100,
        70, 40, 50, 30, 10,
        90, 40, 40, 10, 20
    )

    @Test
    fun positiveSamplesAreClassifiedExactly() {
        val result = positiveSamples.map(::classify)

        assertThat(result).containsExactly(*positiveSamples.toTypedArray())
    }

    @Test
    fun blankDigitBandIsZero() {
        assertThat(CriticalChanceClassifier.classify(emptyList())).isEqualTo(0)
    }

    @Test
    fun malformedLowConfidenceAndClippedResultsAreUnknown() {
        assertThat(
            CriticalChanceClassifier.classify(
                listOf(match(5), match(7))
            )
        ).isEqualTo(null)
        assertThat(
            CriticalChanceClassifier.classify(
                listOf(match(5, score = 0.79), match(0, x = 26, score = 0.79))
            )
        ).isEqualTo(null)
        assertThat(
            CriticalChanceClassifier.classify(classifiedMatches(50), regionValid = false)
        ).isEqualTo(null)
        assertThat(
            CriticalChanceClassifier.classify(emptyList(), blankDigitBand = false)
        ).isEqualTo(null)
        listOf(
            candidateMatches(5, 0, 8),
            candidateMatches(1, 0, 0, 7),
            candidateMatches(9, 1, 0)
        ).forEach { candidates ->
            assertThat(CriticalChanceClassifier.classify(candidates)).isEqualTo(null)
        }
    }

    private fun classify(value: Int): Int? =
        CriticalChanceClassifier.classify(classifiedMatches(value))

    private fun classifiedMatches(value: Int): List<CriticalChanceMatch> {
        val digits = if (value == 100) listOf(1, 0, 0) else listOf(value / 10, 0)
        return candidateMatches(*digits.toIntArray())
    }

    private fun candidateMatches(vararg digits: Int): List<CriticalChanceMatch> {
        var x = 0

        return digits.map { digit ->
            val width = if (digit == 1) 12 else 20
            val match = match(digit, x = x, width = width)
            x += width + 8
            match
        }
    }

    private fun match(
        digit: Int,
        x: Int = 0,
        y: Int = 10,
        width: Int = 18,
        score: Double = 0.9
    ) = CriticalChanceMatch(
        digit = digit,
        region = Region(x, y, width, 16),
        score = score
    )
}
