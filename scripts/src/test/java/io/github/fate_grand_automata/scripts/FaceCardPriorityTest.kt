package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.containsExactly
import io.github.fate_grand_automata.scripts.enums.CardAffinityEnum
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CardPriorityPerWave
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.FieldSlot
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.ServantPriorityPerWave
import io.github.fate_grand_automata.scripts.models.TeamSlot
import io.github.fate_grand_automata.scripts.modules.FaceCardPriority
import kotlin.test.Test

class FaceCardPriorityTest {
    companion object {
        val scathach1WB = ParsedCard(
            card = CommandCard.Face.A,
            servant = TeamSlot.B,
            fieldSlot = FieldSlot.B,
            type = CardTypeEnum.Buster,
            affinity = CardAffinityEnum.Weak
        )
        val kama2Q = ParsedCard(
            card = CommandCard.Face.B,
            servant = TeamSlot.A,
            fieldSlot = FieldSlot.A,
            type = CardTypeEnum.Quick
        )
        val nero3RA = ParsedCard(
            card = CommandCard.Face.C,
            servant = TeamSlot.C,
            fieldSlot = FieldSlot.C,
            type = CardTypeEnum.Arts,
            affinity = CardAffinityEnum.Resist
        )
        val nero4RA = ParsedCard(
            card = CommandCard.Face.D,
            servant = TeamSlot.C,
            fieldSlot = FieldSlot.C,
            type = CardTypeEnum.Arts,
            affinity = CardAffinityEnum.Resist
        )
        val scathach5WQ = ParsedCard(
            card = CommandCard.Face.E,
            servant = TeamSlot.B,
            fieldSlot = FieldSlot.B,
            type = CardTypeEnum.Quick,
            affinity = CardAffinityEnum.Weak
        )

        /**
         * [Kama, Scathach, Nero]
         * [Scathach WB] [Kama Q] [Nero RA] [Nero RA] [Scathach WQ]
         */
        val lineup1 = listOf(scathach1WB, kama2Q, nero3RA, nero4RA, scathach5WQ)

        val lineup2 = listOf(scathach1WB, scathach5WQ, kama2Q, nero3RA, nero4RA)
    }

    @Test
    fun defaultPriority() {
        val priority = FaceCardPriority(CardPriorityPerWave.default, null)

        val sorted = priority.sort(lineup1, 0).map { it.card }

        assertThat(sorted).containsExactly(CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.B, CommandCard.Face.C, CommandCard.Face.D)
    }

    @Test
    fun defaultServantPriority() {
        val priority = FaceCardPriority(CardPriorityPerWave.default, ServantPriorityPerWave.default)

        val sorted = priority.sort(lineup1, 0).map { it.card }

        assertThat(sorted).containsExactly(CommandCard.Face.B, CommandCard.Face.A, CommandCard.Face.E, CommandCard.Face.C, CommandCard.Face.D)
    }

    @Test
    fun criticalChanceReordersOnlyKnownTypeSlots() {
        val cards = listOf(
            scathach1WB.copy(criticalChance = 20),
            kama2Q.copy(criticalChance = 80),
            nero3RA.copy(criticalChance = 30),
            nero4RA.copy(criticalChance = 90),
            scathach5WQ.copy(type = CardTypeEnum.Buster, criticalChance = 100)
        )

        val result = FaceCardPriority(CardPriorityPerWave.default, null)
            .applyCriticalChance(cards)

        assertThat(result).containsExactly(cards[4], cards[1], cards[3], cards[2], cards[0])
    }

    @Test
    fun criticalChanceNullKeepsWholeTypeGroupStable() {
        val cards = listOf(
            scathach1WB.copy(criticalChance = 20),
            scathach5WQ.copy(type = CardTypeEnum.Buster),
            kama2Q.copy(criticalChance = 80)
        )

        val result = FaceCardPriority(CardPriorityPerWave.default, null)
            .applyCriticalChance(cards)

        assertThat(result).containsExactly(*cards.toTypedArray())
    }
}
