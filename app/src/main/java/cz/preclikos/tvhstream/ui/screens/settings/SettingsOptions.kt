package cz.preclikos.tvhstream.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cz.preclikos.tvhstream.R
import cz.preclikos.tvhstream.settings.UiSettings
import cz.preclikos.tvhstream.settings.UiSettingsStore
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsOptions(
    settingsStore: UiSettingsStore = koinInject(),
) {
    val settings by settingsStore.settings.collectAsState(initial = UiSettings())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_options),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            modifier = Modifier
                .toggleable(
                    value = settings.showEpgMenu,
                    role = Role.Switch,
                    onValueChange = { show ->
                        scope.launch { settingsStore.setShowEpgMenu(show) }
                    },
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = settings.showEpgMenu,
                onCheckedChange = null,
            )
            Text(
                text = stringResource(R.string.show_epg_menu),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
