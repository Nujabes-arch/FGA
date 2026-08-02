package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CardPriorityPerWave
import io.github.fate_grand_automata.scripts.models.CardScore
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.ServantPriorityPerWave
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class FaceCardPriority @Inject constructor(
    private val cardPriority: CardPriorityPerWave,
    private val servantPriority: ServantPriorityPerWave?
) {

    private fun applyCardPriority(
        cards: List<ParsedCard>,
        stage: Int
    ): List<ParsedCard> {
        val groupedByScore = cards.groupBy { CardScore(it.type, it.affinity) }

        return cardPriority
            .atWave(stage)
            .mapNotNull { groupedByScore[it] }
            .flatten()
    }

    private fun applyServantPriority(
        cards: List<ParsedCard>,
        priority: ServantPriorityPerWave,
        stage: Int
    ): List<ParsedCard> {
        val groupedByServant = cards.groupBy { it.servant }

        return priority
            .atWave(stage)
            .mapNotNull { groupedByServant[it] }
            .map { servantCards ->
                applyCardPriority(
                    // Stunned cards at the end
                    cards = servantCards.filter { it.type != CardTypeEnum.Unknown },
                    stage = stage
                )
            }
            .flatten()
            .let { picked ->
                // In case less than 3 cards are picked
                val notPicked = cards - picked

                picked + notPicked
            }
    }

    fun sort(
        cards: List<ParsedCard>,
        stage: Int
    ): List<ParsedCard> =
        servantPriority
            ?.let { applyServantPriority(cards, it, stage) }
            ?: applyCardPriority(cards, stage)

    fun applyCriticalChance(cards: List<ParsedCard>): List<ParsedCard> {
        val reordered = cards.toMutableList()

        listOf(CardTypeEnum.Buster, CardTypeEnum.Arts, CardTypeEnum.Quick).forEach { type ->
            val indices = cards.indices.filter { index ->
                val card = cards[index]
                card.type == type && !card.isStunned
            }
            val group = indices.map(cards::get)

            if (group.isEmpty() || group.any { it.criticalChance == null }) return@forEach

            group
                .sortedByDescending { it.criticalChance }
                .forEachIndexed { index, card -> reordered[indices[index]] = card }
        }

        return reordered
    }
}
