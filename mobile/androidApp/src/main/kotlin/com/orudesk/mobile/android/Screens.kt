package com.orudesk.mobile.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.orudesk.mobile.api.Profile
import com.orudesk.mobile.auth.AuthState

/**
 * The app's screens (Day 74). Stateless: they get the state and report what the user did ("state down, events
 * up"), so a preview or a test can draw any state without an API.
 */
@Composable
fun OrudeskApp(
    state: AuthState,
    busy: Boolean,
    onLogin: (String, String) -> Unit,
    onCode: (String) -> Unit,
    onStartOver: () -> Unit,
    onLogout: () -> Unit,
) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(24.dp), contentAlignment = Alignment.Center) {
        when (state) {
            AuthState.Restoring -> CircularProgressIndicator()
            is AuthState.SignedOut -> LoginForm(state.message, busy, onLogin)
            is AuthState.NeedsCode -> CodeForm(state.message, busy, onCode, onStartOver)
            is AuthState.SignedIn -> Home(state.profile, onLogout)
        }
    }
}

@Composable
private fun LoginForm(message: String?, busy: Boolean, onLogin: (String, String) -> Unit) {
    // rememberSaveable: what was typed survives rotation; the password too, but only in this screen's saved state
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val submit = { if (email.isNotBlank() && password.isNotEmpty()) onLogin(email, password) }

    Form(title = "Log in to Orudesk", message = message) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = submit, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Log in") }
    }
}

@Composable
private fun CodeForm(message: String?, busy: Boolean, onCode: (String) -> Unit, onStartOver: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val submit = { if (code.isNotBlank()) onCode(code) }

    Form(title = "Enter your code", message = message) {
        Text("Open your authenticator app and enter the six-digit code. Lost your phone? Enter a recovery code.")
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = submit, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
        TextButton(onClick = onStartOver) { Text("Start again") }
    }
}

@Composable
private fun Home(profile: Profile, onLogout: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Signed in", style = MaterialTheme.typography.headlineMedium)
        Text(profile.email)
        Button(onClick = onLogout) { Text("Log out") }
    }
}

@Composable
private fun Form(title: String, message: String?, content: @Composable () -> Unit) {
    Column(Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (message != null) {
            // A live region: TalkBack reads the error out when it appears
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun CodeStepPreview() {
    MaterialTheme {
        OrudeskApp(AuthState.NeedsCode("That code isn't right, or was already used. Try the newest one."), false, { _, _ -> }, {}, {}, {})
    }
}
