package cz.preclikos.tvhstream.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cz.preclikos.tvhstream.core.BackAction
import cz.preclikos.tvhstream.core.nestedBackAction
import cz.preclikos.tvhstream.ui.components.SettingsSubRail
import cz.preclikos.tvhstream.ui.TvScreenPadding
import androidx.compose.ui.unit.dp
import cz.preclikos.tvhstream.ui.screens.settings.SettingsAppliance
import cz.preclikos.tvhstream.ui.screens.settings.SettingsConnection
import cz.preclikos.tvhstream.ui.screens.settings.SettingsLanguage
import cz.preclikos.tvhstream.ui.screens.settings.SettingsOptions
import cz.preclikos.tvhstream.ui.screens.settings.SettingsPlayer

object SettingsRoutes {
    const val GENERAL = "settings/general"
    const val PLAYER = "settings/player"
    const val OPTIONS = "settings/options"
    const val CONNECTION = "settings/connection"
    const val APPLIANCE = "settings/appliance"
    const val ABOUT = "settings/about"
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val nav = rememberNavController()

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    BackHandler {
        when (nestedBackAction(hasPreviousEntry = nav.previousBackStackEntry != null)) {
            BackAction.POP_NAVIGATION -> nav.popBackStack()
            BackAction.RETURN_TO_PARENT -> onBack()
            BackAction.RETURN_TO_PLAYER -> Unit
            BackAction.FINISH_ACTIVITY -> Unit
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        colors = SurfaceDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(TvScreenPadding)
        ) {
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

            Spacer(Modifier.width(32.dp))

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

                    composable(SettingsRoutes.OPTIONS) {
                        SettingsOptions()
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
