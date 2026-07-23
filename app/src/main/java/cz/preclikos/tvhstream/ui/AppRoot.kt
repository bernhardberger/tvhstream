package cz.preclikos.tvhstream.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.preclikos.tvhstream.core.ApplianceLaunchRequests
import cz.preclikos.tvhstream.stores.LastPlayedChannelStore
import cz.preclikos.tvhstream.ui.components.ContentContainer
import cz.preclikos.tvhstream.ui.components.InfoBanner
import cz.preclikos.tvhstream.ui.components.SideRail
import cz.preclikos.tvhstream.ui.player.VideoPlayerScreen
import cz.preclikos.tvhstream.ui.screens.ChannelsScreen
import cz.preclikos.tvhstream.ui.screens.EpgGridScreen
import cz.preclikos.tvhstream.ui.screens.SettingsScreen
import cz.preclikos.tvhstream.viewmodels.AppConnectionViewModel
import cz.preclikos.tvhstream.viewmodels.ChannelsViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

object Routes {
    const val CHANNELS = "channels"
    const val EPG = "epg"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    fun player(channelId: Int, serviceId: Int, channelName: String) =
        "player/$channelId/$serviceId/${android.net.Uri.encode(channelName)}"
}

@Composable
fun AppRoot(applianceLaunchRequests: ApplianceLaunchRequests) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val activity = context as? Activity

    val appVm: AppConnectionViewModel = koinViewModel()
    val status by appVm.status.collectAsState()
    val channelsVm: ChannelsViewModel = koinViewModel()
    val lastPlayedChannelStore: LastPlayedChannelStore = koinInject()
    val applianceLaunchRequest by applianceLaunchRequests.pending.collectAsState()

    val backStackEntry by nav.currentBackStackEntryAsState()

    val contentFocus = remember { FocusRequester() }
    val currentRoute = backStackEntry?.destination?.route
    val topRoute = currentRoute?.substringBefore("/")
    val showRail = topRoute != Routes.PLAYER

    val isPlayer = currentRoute?.startsWith(Routes.PLAYER) == true

    LaunchedEffect(applianceLaunchRequest) {
        if (applianceLaunchRequest == null) return@LaunchedEffect

        val persistedId = lastPlayedChannelStore.channelId.first()
        val channels = channelsVm.channels.filter { it.isNotEmpty() }.first()
        val target = applianceLaunchRequests.resolve(channels.map { it.id }, persistedId)
            ?: return@LaunchedEffect
        val channel = channels.firstOrNull { it.id == target.channelId }
            ?: return@LaunchedEffect

        if (applianceLaunchRequests.consume(target.request)) {
            nav.navigate(Routes.player(channel.id, channel.id, channel.name))
        }
    }

    BackHandler {
        when (currentRoute) {
            Routes.CHANNELS, Routes.EPG -> {
                activity?.finishAffinity()
                kotlin.system.exitProcess(0)
            }

            Routes.SETTINGS -> {
                nav.navigate(Routes.CHANNELS) { launchSingleTop = true }
            }

            else -> nav.popBackStack()
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(Modifier.fillMaxSize()) {
            if (showRail) {
                SideRail(
                    currentRoute = topRoute,
                    onNavigate = { route ->
                        val current = nav.currentBackStackEntry?.destination?.route
                        if (current == route) {
                            if (!isPlayer) contentFocus.requestFocus()
                        } else {
                            nav.navigate(route) {
                                popUpTo(Routes.CHANNELS) { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                NavHost(
                    navController = nav,
                    startDestination = Routes.CHANNELS,
                ) {

                    composable(Routes.CHANNELS) {
                        ContentContainer(contentFocus) {
                            ChannelsScreen(
                                onPlay = { channelId, serviceId, name ->
                                    nav.navigate(Routes.player(channelId, serviceId, name))
                                }
                            )
                        }
                    }

                    composable(Routes.EPG) {
                        ContentContainer(contentFocus) {
                            EpgGridScreen(
                                onPlay = { channelId, serviceId, name ->
                                    nav.navigate(Routes.player(channelId, serviceId, name))
                                }
                            )
                        }
                    }

                    composable(Routes.SETTINGS) {
                        ContentContainer(contentFocus) {
                            SettingsScreen()
                        }
                    }

                    composable(
                        route = "${Routes.PLAYER}/{channelId}/{serviceId}/{channelName}",
                        arguments = listOf(
                            navArgument("channelId") { type = NavType.IntType },
                            navArgument("serviceId") { type = NavType.IntType },
                            navArgument("channelName") { type = NavType.StringType },
                        )
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getInt("channelId") ?: 0
                        val serviceId = backStackEntry.arguments?.getInt("serviceId") ?: 0
                        val channelName = backStackEntry.arguments?.getString("channelName") ?: ""

                        VideoPlayerScreen(
                            channelId = channelId,
                            channelName = channelName,
                            serviceId = serviceId,
                            onClose = { nav.popBackStack() }
                        )
                    }
                }

                InfoBanner(message = status, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
