package io.github.fate_grand_automata.scripts.models

import io.github.lib_automata.Region
import kotlin.math.abs

class CriticalChancePriorityPerWave private constructor(
    private val priorities: List<Boolean>
) : List<Boolean> by priorities {
    fun atWave(wave: Int) = priorities[wave.coerceIn(priorities.indices)]

    override fun toString() = priorities.joinToString(",") { if (it) "T" else "F" }

    companion object {
        private const val waveCount = 3

        val default get() = from(emptyList())

        fun from(priorities: List<Boolean>) = CriticalChancePriorityPerWave(
            if (priorities.size == waveCount) priorities else List(waveCount) { false }
        )

        fun of(serialized: String): CriticalChancePriorityPerWave {
            val values = serialized.split(",")
            if (values.size != waveCount || values.any { it != "T" && it != "F" }) {
                return default
            }

            return from(values.map { it == "T" })
        }
    }
}

data class CriticalChanceMatch(
    val digit: Int,
    val region: Region,
    val score: Double
) {
    init {
        require(digit in 0..9) { "digit must be between 0 and 9" }
    }
}

/**
 * Classifies the fixed FGO critical-chance number format without OCR.
 *
 * A blank digit band is represented by an empty match list and means 0. Any
 * low-confidence or malformed non-blank result is rejected as unknown.
 */
object CriticalChanceClassifier {
    const val searchThreshold = 0.55
    const val minimumScore = 0.80

    private const val maximumYDelta = 6
    private const val minimumGap = 2
    private const val maximumGap = 18

    fun classify(
        matches: List<CriticalChanceMatch>,
        regionValid: Boolean = true
    ): Int? {
        if (!regionValid) return null
        if (matches.isEmpty()) return 0

        val candidates = matches
            .asSequence()
            .filter { it.score >= minimumScore }
            .sortedByDescending { it.score }
            .fold(mutableListOf<CriticalChanceMatch>()) { selected, candidate ->
                if (selected.none { overlaps(it, candidate) }) {
                    selected += candidate
                }
                selected
            }
            .sortedBy { it.region.x }

        if (candidates.isEmpty()) return null

        val sequences = mutableListOf<List<CriticalChanceMatch>>()

        for (first in candidates.indices) {
            for (second in first + 1 until candidates.size) {
                val pair = listOf(candidates[first], candidates[second])
                if (isTwoDigitSequence(pair)) {
                    sequences += pair
                }

                for (third in second + 1 until candidates.size) {
                    val triple = listOf(candidates[first], candidates[second], candidates[third])
                    if (isThreeDigitSequence(triple)) {
                        sequences += triple
                    }
                }
            }
        }

        return sequences
            .maxWithOrNull(compareBy<List<CriticalChanceMatch>> { it.size }
                .thenBy { it.sumOf(CriticalChanceMatch::score) })
            ?.let { sequence ->
                if (sequence.size == 3) 100 else sequence.first().digit * 10
            }
    }

    private fun overlaps(
        first: CriticalChanceMatch,
        second: CriticalChanceMatch
    ): Boolean {
        return abs(first.region.x - second.region.x) <
            maxOf(first.region.width, second.region.width) * 0.6 &&
            abs(first.region.y - second.region.y) <= maximumYDelta
    }

    private fun isTwoDigitSequence(sequence: List<CriticalChanceMatch>): Boolean {
        return sequence.size == 2 &&
            sequence.first().digit in 1..9 &&
            sequence.last().digit == 0 &&
            isAligned(sequence) &&
            hasValidGaps(sequence)
    }

    private fun isThreeDigitSequence(sequence: List<CriticalChanceMatch>): Boolean {
        return sequence.size == 3 &&
            sequence.map { it.digit } == listOf(1, 0, 0) &&
            isAligned(sequence) &&
            hasValidGaps(sequence)
    }

    private fun isAligned(sequence: List<CriticalChanceMatch>) =
        sequence.maxOf { it.region.y } - sequence.minOf { it.region.y } <= maximumYDelta

    private fun hasValidGaps(sequence: List<CriticalChanceMatch>): Boolean =
        sequence.zipWithNext().all { (first, second) ->
            val gap = second.region.x - first.region.right
            gap in minimumGap..maximumGap
        }
}
