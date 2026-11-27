package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Presented before opening Android Accessibility settings, with a separate decline path. */
@Composable
fun AccessibilityDisclosure(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Allow app and browser inspection?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Strict Blocker uses Android Accessibility to detect the foreground app for the schedules you choose.")
                Text("When Browser Guard is on, it also reads visible browser address and search fields to detect restricted websites or explicit searches and return you to Home. It does not read passwords or other editable fields.")
                Text("Inspected addresses and searches stay in memory on this device. They are not saved or sent to a server. Only generic block events and counts are saved locally.")
                Text("You can decline, switch Browser Guard off, or revoke Accessibility access in Android Settings. Recording and your companion work without this access.")
            }
        },
        confirmButton = { TextButton(onClick = onAgree) { Text("Agree and continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } })
}
