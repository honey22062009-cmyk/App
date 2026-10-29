package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.data.preferences.SettingsPreferences
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class CharacterOption(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color
)

private val characterOptions = listOf(
    CharacterOption("nova", "Nova · Fox", Color(0xFFE45B99), Color(0xFFFFB0C9)),
    CharacterOption("milo", "Milo · Bear", Color(0xFFA953C7), Color(0xFFD7A5EA)),
    CharacterOption("aria", "Aria · Cat", Color(0xFFF4866B), Color(0xFFFFC2A8)),
    CharacterOption("kai", "Kai · Panda", Color(0xFF8C5CC7), Color(0xFFC7A5EF)),
    CharacterOption("luna", "Luna · Rabbit", Color(0xFFD08BB1), Color(0xFFF4CADD)),
    CharacterOption("sol", "Sol · Owl", Color(0xFFD36A89), Color(0xFFF1A0B5))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingExperienceScreen(
    settings: SettingsPreferences,
    scope: CoroutineScope,
    reduceMotion: Boolean,
    onFinished: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("prefer_not_to_say") }
    var characterId by rememberSaveable { mutableStateOf("nova") }
    var companionName by rememberSaveable { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scroll = rememberScrollState()
    LaunchedEffect(step) {
        focus.clearFocus(force = true)
        keyboard?.hide()
        scroll.scrollTo(0)
    }
    LaunchedEffect(settings) {
        if (name.isBlank()) name = settings.userNameFlow.first()
        if (ageText.isBlank()) ageText = settings.userAgeFlow.first().takeIf { it > 0 }?.toString().orEmpty()
        if (companionName.isBlank()) companionName = settings.characterNameFlow.first()
    }
    BackHandler(enabled = step > 0 && !saving) { step-- }

    val age = ageText.toIntOrNull()
    val canContinue = when (step) {
        1 -> name.trim().length in 1..32
        2 -> age != null && age in 13..120
        else -> true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        val wide = maxWidth >= 700.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(
                    horizontal = if (wide) 48.dp else 20.dp,
                    vertical = 22.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    IconButton(onClick = { if (!saving) step-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Strict Blocker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Personal setup · stored on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${step + 1}/6",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / 6f },
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 5 } + fadeOut())
                    }
                },
                label = "onboarding_step"
            ) { current ->
                GlassCard(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    variant = GlassVariant.Accent,
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(if (wide) 34.dp else 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        when (current) {
                            0 -> WelcomeStep()
                            1 -> NameStep(name = name, onNameChange = { name = it.take(32) })
                            2 -> AgeStep(ageText = ageText, onAgeChange = { value ->
                                ageText = value.filter(Char::isDigit).take(3)
                            })
                            3 -> GenderStep(gender = gender, onGenderChange = { gender = it })
                            4 -> CharacterStep(
                                selected = characterId,
                                onSelected = { characterId = it },
                                wide = wide,
                                reduceMotion = reduceMotion
                            )
                            else -> CompanionNameStep(
                                companionName = companionName,
                                onNameChange = { companionName = it.take(24) },
                                characterId = characterId
                            )
                        }

                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (current == 5) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            saving = true
                                            try {
                                            settings.saveLocalProfile(
                                                name = name,
                                                age = age ?: 13,
                                                gender = gender,
                                                characterId = characterId,
                                                companionName = null
                                            )
                                            onFinished()
                                            } catch (_: Exception) {
                                                error = "Could not save your setup. Please try again."
                                            } finally { saving = false }
                                        }
                                    },
                                    enabled = !saving,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Skip name")
                                }
                            }

                            Button(
                                onClick = {
                                    if (current < 5) {
                                        step++
                                    } else {
                                        scope.launch {
                                            saving = true
                                            try {
                                            settings.saveLocalProfile(
                                                name = name,
                                                age = age ?: 13,
                                                gender = gender,
                                                characterId = characterId,
                                                companionName = companionName
                                            )
                                            onFinished()
                                            } catch (_: Exception) {
                                                error = "Could not save your setup. Please try again."
                                            } finally { saving = false }
                                        }
                                    }
                                },
                                enabled = canContinue && !saving,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Text(if (saving) "Saving…" else if (current == 0) "Get started" else if (current == 5) "Continue" else "Next")
                                Spacer(Modifier.size(8.dp))
                                Icon(
                                    if (current == 5) Icons.Default.Check else Icons.Default.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.widthIn(max = 860.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "Your setup stays local. No account is required for core features.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Welcome to Strict Blocker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            "Build better routines, track progress, and keep your digital boundaries where you want them.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ) {
            Text(
                "No account gate. No automatic detection of private behavior. You decide what gets logged.",
                modifier = Modifier.padding(18.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("What should we call you?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Used for greetings inside the app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Name") },
            supportingText = { Text("${name.trim().length}/32") }
        )
    }
}

@Composable
private fun AgeStep(ageText: String, onAgeChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How old are you?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Stored locally and not shown publicly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = ageText,
            onValueChange = onAgeChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                if (ageText.isNotBlank() && (ageText.toIntOrNull() !in 13..120)) {
                    Text("Enter an age from 13 to 120.")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenderStep(gender: String, onGenderChange: (String) -> Unit) {
    val options = listOf(
        "male" to "Male",
        "female" to "Female",
        "other" to "Other",
        "prefer_not_to_say" to "Prefer not to say"
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Representation preference", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Optional context only. Character choice is independent.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (id, label) ->
                FilterChip(
                    selected = gender == id,
                    onClick = { onGenderChange(id) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterStep(selected: String, onSelected: (String) -> Unit, wide: Boolean, reduceMotion: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Choose your companion", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Pick a character, then turn, pet or play with them right here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        com.example.ui.components.InteractiveCompanion(
            characterId = selected,
            name = characterOptions.firstOrNull { it.id == selected }?.label?.substringBefore(" ·").orEmpty(),
            reduceMotion = reduceMotion,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            characterOptions.forEach { option ->
                FilterChip(selected = selected == option.id, onClick = { onSelected(option.id) },
                    label = { Text(option.label) }, modifier = Modifier.widthIn(min = if (wide) 220.dp else 110.dp))
            }
        }
    }
}

@Composable
private fun CompanionNameStep(
    companionName: String,
    onNameChange: (String) -> Unit,
    characterId: String
) {
    val defaultName = characterOptions.firstOrNull { it.id == characterId }?.label?.substringBefore(" ·") ?: "Companion"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompanionAvatar(characterId = characterId, modifier = Modifier.size(132.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Want to give your companion a name?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Optional. If you skip, we'll use $defaultName.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = companionName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Companion name") }
            )
        }
    }
}

@Composable
fun CompanionAvatar(characterId: String, modifier: Modifier = Modifier) {
    com.example.ui.components.CompanionScene(characterId, modifier, backgroundColor = MaterialTheme.colorScheme.primaryContainer)
}
