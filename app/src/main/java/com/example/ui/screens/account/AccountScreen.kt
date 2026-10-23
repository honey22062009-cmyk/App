package com.example.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.auth.FirebaseAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember(context) { FirebaseAuthProvider.getAuth(context) }
    var user by remember { mutableStateOf<FirebaseUser?>(auth?.currentUser) }
    var create by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(auth) {
        if (auth == null) return@DisposableEffect onDispose { }
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Account", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Sign in and account security", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (user != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp))
                    Text("You're signed in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user?.email.orEmpty())
                    Text("Sign-in identifies your account. It does not by itself grant paid Premium access.")
                    Button(onClick = { auth?.signOut(); message = "Signed out." }) { Text("Sign out") }
                }
            }
        } else {
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Secure account sign-in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Passwords are handled by Firebase Authentication and are not stored by Strict Blocker.")
                    if (auth == null) {
                        Text("Login UI is installed, but authentication is not connected in this build. Configure the Firebase values documented in the repository README and enable Email/Password sign-in in Firebase Console. The app will not pretend a local profile is a real account.", color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !create, onClick = { create = false; message = "" }, label = { Text("Sign in") })
                        FilterChip(selected = create, onClick = { create = true; message = "" }, label = { Text("Create account") })
                    }
                    OutlinedTextField(email, { email = it.trimStart(); message = "" }, label = { Text("Email address") }, singleLine = true, enabled = auth != null && !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it; message = "" }, label = { Text("Password (8+ characters)") }, singleLine = true, enabled = auth != null && !busy, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth())
                    if (create) OutlinedTextField(confirm, { confirm = it; message = "" }, label = { Text("Confirm password") }, singleLine = true, enabled = auth != null && !busy, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = auth != null && !busy,
                        onClick = {
                            when {
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> message = "Enter a valid email address."
                                password.length < 8 -> message = "Use at least 8 characters."
                                create && password != confirm -> message = "The passwords do not match."
                                auth == null -> message = "Authentication is not configured."
                                else -> {
                                    busy = true
                                    val task = if (create) auth.createUserWithEmailAndPassword(email.trim(), password) else auth.signInWithEmailAndPassword(email.trim(), password)
                                    task.addOnCompleteListener { result ->
                                        busy = false
                                        message = if (result.isSuccessful) if (create) "Account created." else "Signed in successfully."
                                            else "Authentication failed: ${result.exception?.localizedMessage ?: "check your credentials and connection"}"
                                        if (result.isSuccessful) password = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text(if (create) "Create account" else "Sign in")
                    }
                    if (!create) TextButton(enabled = auth != null && email.isNotBlank() && !busy, onClick = {
                        auth?.sendPasswordResetEmail(email.trim())?.addOnCompleteListener { result ->
                            message = if (result.isSuccessful) "Reset instructions sent if the account exists." else "Could not send reset email."
                        }
                    }) { Text("Forgot password?") }
                    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Privacy and purchase status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Schedules and blocking history remain local. Account identity and paid-purchase verification are separate services; Premium is not unlocked by signing in.")
                }
            }
        }
    }
}
