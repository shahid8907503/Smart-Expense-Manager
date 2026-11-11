package com.smart.expense.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.smart.expense.ui.components.VoiceWaveform
import com.smart.expense.ui.theme.EmeraldAccent
import com.smart.expense.ui.theme.ForestGreenContainer
import com.smart.expense.ui.theme.ForestGreenPrimary
import com.smart.expense.voice.NlpTransactionParser
import com.smart.expense.voice.ParsedVoiceTransaction
import com.smart.expense.voice.SpeechRecognizerHelper

@Composable
fun VoiceListeningDialog(
    preferredLanguage: String,
    onDismiss: () -> Unit,
    onTransactionParsed: (ParsedVoiceTransaction) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var rmsdB by remember { mutableFloatStateOf(0f) }
    var recognizedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Listening… Speak naturally") }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var speechHelper: SpeechRecognizerHelper? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            speechHelper?.startListening(preferredLanguage)
        } else {
            statusText = "Microphone permission is required for voice commands"
        }
    }

    LaunchedEffect(Unit) {
        speechHelper = SpeechRecognizerHelper(
            context = context,
            onRmsDbChanged = { rmsdB = it },
            onPartialResult = { partial ->
                recognizedText = partial
            },
            onFinalResult = { finalResult ->
                recognizedText = finalResult
                isProcessing = true
                statusText = "Processing with NLP…"
                // Parse using multilingual NLP
                val parsed = NlpTransactionParser.parse(finalResult, preferredLanguage)
                onTransactionParsed(parsed)
            },
            onError = { err ->
                isListening = false
                statusText = err
            },
            onListeningStateChanged = { listening ->
                isListening = listening
            }
        )

        if (hasPermission) {
            speechHelper?.startListening(preferredLanguage)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper?.destroy()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val langLabel = when (preferredLanguage) {
                        "te" -> "Telugu"
                        "hi" -> "Hindi"
                        else -> "English"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForestGreenContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Mode: $langLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = {
                            speechHelper?.stopListening()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Microphone Big Button with Pulse/Listening ring
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) EmeraldAccent.copy(alpha = 0.2f)
                            else ForestGreenContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isListening) ForestGreenPrimary else Color.Gray)
                            .clickable {
                                if (isListening) {
                                    speechHelper?.stopListening()
                                } else {
                                    speechHelper?.startListening(preferredLanguage)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Stop,
                            contentDescription = "Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Audio Waveform
                VoiceWaveform(
                    isListening = isListening,
                    rmsdB = rmsdB,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recognized Speech Bubble Preview
                AnimatedVisibility(visible = recognizedText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "\"$recognizedText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (recognizedText.isEmpty()) {
                    Text(
                        text = "Speak clearly into your mic or tap a demo prompt below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Demo Preset Chips for guaranteed HOD presentation
                Text(
                    text = "HOD Demo Presets (Tap to Test):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val demoPrompts = when (preferredLanguage) {
                    "ta" -> listOf(
                        "மதிய உணவிற்கு 250 ரூபாய் செலவு செய்தேன்",
                        "பெட்ரோல் 500 ரூபாய்",
                        "மளிகைப் பொருட்கள் 1200",
                        "சம்பளம் 50000 வந்தது"
                    )
                    "te" -> listOf(
                        "లంచ్ కోసం 250 ఖర్చు చేశాను",
                        "కూరగాయలు 400 రూపాయలు",
                        "జీతం 50000 వచ్చింది"
                    )
                    "hi" -> listOf(
                        "दोपहर के खाने पर 250 रुपये खर्च किए",
                        "किराने का सामान 600 रुपये",
                        "सैलरी 45000 आई"
                    )
                    else -> listOf(
                        "I spent 250 rupees on lunch today",
                        "Petrol 500 rupees",
                        "Groceries shopping 1200",
                        "Salary credited 45000"
                    )
                }

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(demoPrompts) { prompt ->
                        AssistChip(
                            onClick = {
                                recognizedText = prompt
                                isProcessing = true
                                statusText = "Processing with NLP…"
                                val parsed = NlpTransactionParser.parse(prompt, preferredLanguage)
                                onTransactionParsed(parsed)
                            },
                            label = { Text(prompt, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = ForestGreenContainer.copy(alpha = 0.4f),
                                labelColor = ForestGreenPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

