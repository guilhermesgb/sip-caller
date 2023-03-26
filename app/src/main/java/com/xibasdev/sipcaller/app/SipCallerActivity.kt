package com.xibasdev.sipcaller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xibasdev.sipcaller.app.view.MainScreen
import com.xibasdev.sipcaller.app.view.call.CallScreen
import com.xibasdev.sipcaller.sip.calling.CallId
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SipCallerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = ROUTE_MAIN_SCREEN,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(
                    route = ROUTE_MAIN_SCREEN
                ) {
                    MainScreen(
                        onNavigateToCallScreen = { callId ->

                            navController.navigate("$ROUTE_CALL_SCREEN/${callId.value}")
                        }
                    )
                }

                composable(
                    route = "$ROUTE_CALL_SCREEN/{callId}",
                    arguments = listOf(navArgument("callId") { type = NavType.StringType })
                ) { navBackStackEntry ->

                    val rawCallId = navBackStackEntry.arguments?.getString("callId")
                        ?: throw IllegalStateException("Routed to call screen with null callId!")

                    CallScreen(
                        lifecycle = lifecycle,
                        callId = CallId(rawCallId)
                    )
                }
            }
        }
    }

    companion object {
        private const val ROUTE_MAIN_SCREEN = "main"
        private const val ROUTE_CALL_SCREEN = "call"
    }
}
