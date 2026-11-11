package com.smart.expense.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smart.expense.data.local.AppDatabase
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.ui.components.VoiceWaveform
import com.smart.expense.ui.theme.*
import com.smart.expense.voice.SpeechRecognizerHelper
import com.smart.expense.voice.TextToSpeechHelper
import com.smart.expense.voice.VoiceQueryEngine
import com.smart.expense.voice.VoiceQueryLocalResult
import kotlinx.coroutines.launch

@Composable
fun VoiceAssistantQueryScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = authRepository.currentUser.collectAsState().value

    val db = remember { AppDatabase.getDatabase(context) }
    val queryEngine = remember { VoiceQueryEngine(db.transactionDao(), db.budgetDao()) }
    var ttsHelper: TextToSpeechHelper? by remember { mutableStateOf(null) }

    var isListening by remember { mutableStateOf(false) }
    var rmsdB by remember { mutableFloatStateOf(0f) }
    var userQuestion by remember { mutableStateOf("") }
    var queryResult by remember { mutableStateOf<VoiceQueryLocalResult?>(null) }
    var speechHelper: SpeechRecognizerHelper? by remember { mutableStateOf(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) speechHelper?.startListening(user.preferredLanguage)
    }

    LaunchedEffect(Unit) {
        ttsHelper = TextToSpeechHelper(context)
        speechHelper = SpeechRecognizerHelper(
            context = context,
            onRmsDbChanged = { rmsdB = it },
            onPartialResult = { userQuestion = it },
            onFinalResult = { question ->
                userQuestion = question
                scope.launch {
                    val result = queryEngine.executeQuery(question, user.userId, user.preferredLanguage)
                    queryResult = result
                    ttsHelper?.speak(result.spokenAnswer, user.preferredLanguage)
                }
            },
            onError = { isListening = false },
            onListeningStateChanged = { isListening = it }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper?.destroy()
            ttsHelper?.shutdown()
        }
    }

    fun processCustomQuestion(q: String) {
        userQuestion = q
        scope.launch {
            val result = queryEngine.executeQuery(q, user.userId, user.preferredLanguage)
            queryResult = result
            ttsHelper?.speak(result.spokenAnswer, user.preferredLanguage)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Voice Financial Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ask questions about your spending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Main Microphone Interaction
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(if (isListening) EmeraldAccent.copy(alpha = 0.2f) else ForestGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isListening) ForestGreenPrimary else EmeraldAccent)
                        .clickable {
                            if (isListening) {
                                speechHelper?.stopListening()
                            } else {
                                if (hasPermission) {
                                    speechHelper?.startListening(user.preferredLanguage)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isListening) "Listening to your question…" else "Tap to Ask a Question",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            VoiceWaveform(
                isListening = isListening,
                rmsdB = rmsdB,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display Recognized User Question
            if (userQuestion.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "\"$userQuestion\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Real DB Calculated Answer Card
            AnimatedVisibility(visible = queryResult != null) {
                queryResult?.let { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ForestGreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = res.headline,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { ttsHelper?.speak(res.spokenAnswer, user.preferredLanguage) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Replay audio",
                                        tint = ForestGreenPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = res.amountFormatted,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                                color = ForestGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = res.detailSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ForestGreenContainer)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = res.spokenAnswer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ForestGreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Suggested Voice Questions Chips
            Text(
                text = "SUGGESTED QUESTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            val suggestions = listOf(
                "How much did I spend on food this month?",
                "What is my total spending today?",
                "Which category has the highest expenses?",
                "What is my remaining food budget?",
                "How much income did I receive this month?",
                "Compare my spending this month with last month."
            )

            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { processCustomQuestion(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

