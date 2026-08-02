package io.github.fate_grand_automata.ui.card_priority

import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fate_grand_automata.R
import io.github.fate_grand_automata.scripts.enums.BraveChainEnum
import io.github.fate_grand_automata.scripts.models.CardTypeSoftLimit
import io.github.fate_grand_automata.scripts.models.TeamSlot
import io.github.fate_grand_automata.ui.FGAListItemColors
import io.github.fate_grand_automata.ui.drag_sort.DragSort
import io.github.fate_grand_automata.ui.drag_sort.DragSortAdapter
import io.github.fate_grand_automata.ui.prefs.listDialog
import io.github.fate_grand_automata.ui.prefs.Preference
import io.github.fate_grand_automata.util.stringRes

@Composable
fun CardPriorityListItem.Render(
    useServantPriority: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardPriorityDragSort(scores)

        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    stringResource(R.string.card_type_soft_limits),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp)
                )
                Text(
                    stringResource(R.string.card_type_soft_limits_description),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                var limits by cardTypeSoftLimits

                Row(modifier = Modifier.fillMaxWidth()) {
                    CardTypeSoftLimitPicker(
                        label = "B",
                        selected = limits.buster,
                        onSelectedChange = { limits = limits.copy(buster = it) },
                        modifier = Modifier.weight(1f)
                    )
                    CardTypeSoftLimitPicker(
                        label = "A",
                        selected = limits.arts,
                        onSelectedChange = { limits = limits.copy(arts = it) },
                        modifier = Modifier.weight(1f)
                    )
                    CardTypeSoftLimitPicker(
                        label = "Q",
                        selected = limits.quick,
                        onSelectedChange = { limits = limits.copy(quick = it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                var criticalChance by criticalChancePriority

                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { criticalChance = !criticalChance },
                    headlineContent = { Text(stringResource(R.string.critical_chance_priority)) },
                    supportingContent = {
                        Text(stringResource(R.string.critical_chance_priority_description))
                    },
                    trailingContent = {
                        Checkbox(
                            checked = criticalChance,
                            onCheckedChange = { criticalChance = it }
                        )
                    },
                    colors = FGAListItemColors()
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                var braveChains by braveChains

                val braveChainDialog = listDialog(
                    selected = braveChains,
                    onSelectedChange = { braveChains = it },
                    entries = BraveChainEnum.entries
                        .associateWith { stringResource(it.stringRes) },
                    title = stringResource(R.string.p_brave_chains)
                )

                ListItem(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { braveChainDialog.show() },
                    headlineContent = { Text(stringResource(R.string.p_brave_chains)) },
                    supportingContent = { Text(stringResource(braveChains.stringRes)) },
                    colors = FGAListItemColors()
                )

                var rearrange by rearrangeCards

                ListItem(
                    modifier = Modifier
                        .weight(1.1f)
                        .clickable { rearrange = !rearrange },
                    headlineContent = { Text(stringResource(R.string.p_rearrange_cards)) },
                    trailingContent = {
                        Checkbox(
                            checked = rearrange,
                            onCheckedChange = { rearrange = it }
                        )
                    },
                    colors = FGAListItemColors()
                )
            }
        }

        if (useServantPriority) {
            ServantPriority(
                priorities = servantPriority
            )
        }
    }
}

@Composable
private fun CardTypeSoftLimitPicker(
    label: String,
    selected: CardTypeSoftLimit,
    onSelectedChange: (CardTypeSoftLimit) -> Unit,
    modifier: Modifier = Modifier
) {
    val dialog = listDialog(
        selected = selected,
        onSelectedChange = onSelectedChange,
        entries = CardTypeSoftLimit.entries.associateWith { stringResource(it.stringRes) },
        title = stringResource(R.string.card_type_soft_limit_picker_title)
    )

    Preference(
        title = label,
        summary = stringResource(selected.stringRes),
        onClick = { dialog.show() },
        modifier = modifier
    )
}

@Composable
private fun ServantPriority(
    priorities: MutableList<TeamSlot>
) {
    Text(
        "Servant Priority".uppercase(),
        modifier = Modifier
            .padding(bottom = 5.dp, top = 16.dp)
    )

    val context = LocalContext.current

    DragSort(
        items = priorities,
        viewConfigGrabber = {
            DragSortAdapter.ItemViewConfig(
                foregroundColor = Color.WHITE,
                backgroundColor = when (it.position) {
                    1 -> R.color.colorArts
                    2 -> R.color.colorQuick
                    3 -> R.color.colorBuster
                    4 -> R.color.colorArtsResist
                    5 -> R.color.colorQuickResist
                    else -> R.color.colorBusterResist
                }.let { res -> context.getColor(res) },
                text = "  ${it.position}  "
            )
        }
    )
}
