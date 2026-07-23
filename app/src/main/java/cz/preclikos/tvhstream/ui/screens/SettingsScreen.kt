package cz.preclikos.tvhstream.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cz.preclikos.tvhstream.ui.components.SettingsSubRail
import cz.preclikos.tvhstream.ui.screens.settings.SettingsAppliance
import cz.preclikos.tvhstream.ui.screens.settings.SettingsConnection
import cz.preclikos.tvhstream.ui.screens.settings.SettingsLanguage
import cz.preclikos.tvhstream.ui.screens.settings.SettingsPlayer

object SettingsRoutes {
    const val GENERAL = "settings/general"
    const val PLAYER = "settings/player"
    const val CONNECTION = "settings/connection"
    const val APPLIANCE = "settings/appliance"
    const val ABOUT = "settings/about"
}

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val nav = rememberNavController()

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    BackHandler {
        if (!nav.popBackStack()) onClose()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(Modifier.fillMaxSize()) {
            SettingsSubRail(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    nav.navigate(route) {
                        popUpTo(SettingsRoutes.GENERAL) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NavHost(
                    navController = nav,
                    startDestination = SettingsRoutes.GENERAL,
                ) {
                    composable(SettingsRoutes.GENERAL) {
                        SettingsLanguage()
                    }

                    composable(SettingsRoutes.CONNECTION) {
                        SettingsConnection()
                    }

                    composable(SettingsRoutes.PLAYER) {
                        SettingsPlayer()
                    }

                    composable(SettingsRoutes.APPLIANCE) {
                        SettingsAppliance()
                    }
                }
            }
        }
    }
}
