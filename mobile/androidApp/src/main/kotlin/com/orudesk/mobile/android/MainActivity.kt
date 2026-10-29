package com.orudesk.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val auth: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    val state by auth.state.collectAsStateWithLifecycle()
                    val busy by auth.busy.collectAsStateWithLifecycle()
                    OrudeskApp(
                        state = state,
                        busy = busy,
                        onLogin = auth::login,
                        onCode = auth::submitCode,
                        onStartOver = auth::startOver,
                        onLogout = auth::logout,
                    )
                }
            }
        }
    }
}
