package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CardTypeSoftLimit
import io.github.fate_grand_automata.scripts.models.CardTypeSoftLimits
import io.github.fate_grand_automata.scripts.models.CardTypeSoftLimitsPerWave
import io.github.fate_grand_automata.scripts.models.ParsedCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CardTypeSoftLimitsTest {
    private val buster1 = FaceCardPriorityTest.scathach1WB
    private val buster2 = FaceCardPriorityTest.kama2Q.copy(type = CardTypeEnum.Buster)
    private val arts1 = FaceCardPriorityTest.nero3RA.copy(affinity = FaceCardPriorityTest.scathach1WB.affinity)
    private val arts2 = FaceCardPriorityTest.nero4RA.copy(affinity = FaceCardPriorityTest.scathach1WB.affinity)
    private val quick = FaceCardPriorityTest.scathach5WQ

    private val cards = listOf(buster1, buster2, arts1, arts2, quick)

    private fun assertObjectsPreserved(result: List<ParsedCard>) {
        assertEquals(cards.size, result.size)
        cards.forEach { card ->
            assertEquals(1, result.count { it === card })
        }
    }

    @Test
    fun softLimitMovesOverflowBehindOtherTypes() {
        val result = CardTypeSoftLimits(
            buster = CardTypeSoftLimit.One
        ).apply(cards)

        assertThat(result).containsExactly(buster1, arts1, arts2, quick, buster2)
        assertObjectsPreserved(result)
    }

    @Test
    fun softLimitsCanBeSetPerType() {
        val result = CardTypeSoftLimits(
            buster = CardTypeSoftLimit.One,
            arts = CardTypeSoftLimit.One
        ).apply(cards)

        assertThat(result).containsExactly(buster1, arts1, quick, buster2, arts2)
        assertObjectsPreserved(result)
    }

    @Test
    fun twoLimitKeepsTwoPreferredCards() {
        val result = CardTypeSoftLimits(
            buster = CardTypeSoftLimit.Two,
            arts = CardTypeSoftLimit.One
        ).apply(cards)

        assertThat(result).containsExactly(buster1, buster2, arts1, quick, arts2)
        assertObjectsPreserved(result)
    }

    @Test
    fun overflowFallsBackWhenOnlyOneTypeExists() {
        val onlyBuster = cards.map { it.copy(type = CardTypeEnum.Buster) }

        val result = CardTypeSoftLimits(
            buster = CardTypeSoftLimit.One
        ).apply(onlyBuster)

        assertThat(result).containsExactly(*onlyBuster.toTypedArray())
        assertEquals(onlyBuster.size, result.size)
        onlyBuster.forEachIndexed { index, card -> assertSame(card, result[index]) }
    }

    @Test
    fun unknownCardsAreAlwaysLast() {
        val unknown = quick.copy(type = CardTypeEnum.Unknown)

        val result = CardTypeSoftLimits(
            buster = CardTypeSoftLimit.One
        ).apply(listOf(unknown, buster1, buster2, arts1))

        assertThat(result).containsExactly(buster1, arts1, buster2, unknown)
    }

    @Test
    fun unlimitedReturnsTheOriginalList() {
        val result = CardTypeSoftLimits.default.apply(cards)

        assertSame(cards, result)
    }

    @Test
    fun waveSerializationRoundTripsAndInvalidValuesFallback() {
        val limits = CardTypeSoftLimitsPerWave.from(
            listOf(
                CardTypeSoftLimits(CardTypeSoftLimit.One, CardTypeSoftLimit.Two),
                CardTypeSoftLimits(quick = CardTypeSoftLimit.Two)
            )
        )

        assertThat(CardTypeSoftLimitsPerWave.of(limits.toString()).toString()).isEqualTo(limits.toString())
        assertThat(CardTypeSoftLimitsPerWave.of("ONE,invalid\nmissing\nTWO,TWO,TWO").toList())
            .containsExactly(
                CardTypeSoftLimits(CardTypeSoftLimit.One),
                CardTypeSoftLimits.default,
                CardTypeSoftLimits(CardTypeSoftLimit.Two, CardTypeSoftLimit.Two, CardTypeSoftLimit.Two)
            )
    }
}
