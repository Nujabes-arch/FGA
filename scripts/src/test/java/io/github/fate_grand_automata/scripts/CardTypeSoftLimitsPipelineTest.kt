package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.containsExactly
import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.enums.CardAffinityEnum
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CardPriorityPerWave
import io.github.fate_grand_automata.scripts.models.CardTypeSoftLimit
import io.github.fate_grand_automata.scripts.models.CardTypeSoftLimits
import io.github.fate_grand_automata.scripts.modules.ApplyBraveChains
import io.github.fate_grand_automata.scripts.modules.FaceCardPriority
import io.github.fate_grand_automata.scripts.models.NPUsage
import kotlin.test.Test

class CardTypeSoftLimitsPipelineTest {
    @Test
    fun appliesAfterCardPriorityBeforeBraveChain() {
        val cards = listOf(
            FaceCardPriorityTest.scathach1WB.copy(affinity = CardAffinityEnum.Normal),
            FaceCardPriorityTest.kama2Q.copy(type = CardTypeEnum.Buster),
            FaceCardPriorityTest.nero3RA.copy(affinity = CardAffinityEnum.Normal),
            FaceCardPriorityTest.nero4RA.copy(affinity = CardAffinityEnum.Normal),
            FaceCardPriorityTest.scathach5WQ.copy(affinity = CardAffinityEnum.Normal)
        )

        val sorted = FaceCardPriority(
            CardPriorityPerWave.of("B, A, Q, WB, WA, WQ, RB, RA, RQ"),
            null
        ).sort(cards, 0)
        val limited = CardTypeSoftLimits(buster = CardTypeSoftLimit.One).apply(sorted)
        val picked = ApplyBraveChains().pick(limited, BraveChainEnum.None)

        assertThat(picked).containsExactly(
            cards[0], cards[2], cards[3], cards[4], cards[1]
        )
    }

    @Test
    fun appliesCriticalChanceBeforeSoftLimit() {
        val busterFirst = FaceCardPriorityTest.scathach1WB.copy(
            affinity = CardAffinityEnum.Normal,
            criticalChance = 20
        )
        val busterSecond = FaceCardPriorityTest.kama2Q.copy(
            type = CardTypeEnum.Buster,
            affinity = CardAffinityEnum.Normal,
            criticalChance = 100
        )
        val arts = FaceCardPriorityTest.nero3RA.copy(affinity = CardAffinityEnum.Normal)
        val cards = listOf(busterFirst, busterSecond, arts)
        val priority = FaceCardPriority(
            CardPriorityPerWave.of("B, A, Q, WB, WA, WQ, RB, RA, RQ"),
            null
        )

        val sorted = priority.sort(cards, 0)
        val critical = priority.applyCriticalChance(sorted)
        val limited = CardTypeSoftLimits(buster = CardTypeSoftLimit.One).apply(critical)
        val picked = ApplyBraveChains().pick(
            cards = limited,
            npUsage = NPUsage.none,
            braveChains = BraveChainEnum.None,
            rearrange = false
        )

        assertThat(picked).containsExactly(busterSecond, arts, busterFirst)
    }
}
