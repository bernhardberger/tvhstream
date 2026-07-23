package cz.preclikos.tvhstream.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import cz.preclikos.tvhstream.core.ApplianceLaunchRequests

class MainActivity : ComponentActivity() {
    private val applianceLaunchRequests = ApplianceLaunchRequests()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TVHStreamTheme {
                AppRoot(applianceLaunchRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applianceLaunchRequests.request()
    }
}
