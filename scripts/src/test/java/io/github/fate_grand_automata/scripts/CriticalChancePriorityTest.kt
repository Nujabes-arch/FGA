package io.github.fate_grand_automata.scripts

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.fate_grand_automata.scripts.models.CriticalChancePriorityPerWave
import kotlin.test.Test

class CriticalChancePriorityTest {
    @Test
    fun roundTripsThreeWaveValues() {
        val priority = CriticalChancePriorityPerWave.from(listOf(true, false, true))

        assertThat(CriticalChancePriorityPerWave.of(priority.toString()).toString())
            .isEqualTo("T,F,T")
        assertThat(priority.atWave(0)).isEqualTo(true)
        assertThat(priority.atWave(2)).isEqualTo(true)
    }

    @Test
    fun malformedOrAbnormalValuesDisableEveryWave() {
        listOf("", "T,F", "T,F,T,F", "T,X,F").forEach { serialized ->
            assertThat(CriticalChancePriorityPerWave.of(serialized).toList())
                .containsExactly(false, false, false)
        }

        assertThat(CriticalChancePriorityPerWave.from(listOf(true)).toList())
            .containsExactly(true, false, false)
        assertThat(CriticalChancePriorityPerWave.from(listOf(true, false)).toList())
            .containsExactly(true, false, false)
        assertThat(CriticalChancePriorityPerWave.from(listOf(true, false, true)).toList())
            .containsExactly(true, false, true)
    }
}
