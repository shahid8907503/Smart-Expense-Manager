package com.smart.expense

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.data.repository.ExpenseRepository
import com.smart.expense.security.BiometricAuthManager
import com.smart.expense.ui.navigation.AppNavGraph
import com.smart.expense.ui.theme.ForestGreenContainer
import com.smart.expense.ui.theme.ForestGreenPrimary
import com.smart.expense.ui.theme.SmartExpenseTheme
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var expenseRepository: ExpenseRepository
    private var lastBackgroundTimeMs: Long = 0
    private var triggerRelock: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authRepository = AuthRepository(this)
        expenseRepository = ExpenseRepository(this)

        setContent {
            SmartExpenseTheme {
                val user by authRepository.currentUser.collectAsState()
                var isAppUnlocked by remember { mutableStateOf(!user.isBiometricEnabled || !user.isLoggedIn) }

                triggerRelock = {
                    isAppUnlocked = false
                }

                // Anti-Screenshot & Screen Recording Privacy Shield (FLAG_SECURE)
                LaunchedEffect(user.isAntiScreenshotEnabled) {
                    if (user.isAntiScreenshotEnabled) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                LaunchedEffect(user.isBiometricEnabled, user.isLoggedIn) {
                    if (user.isBiometricEnabled && user.isLoggedIn && !isAppUnlocked) {
                        requestBiometricUnlock {
                            isAppUnlocked = true
                        }
                    } else if (!user.isBiometricEnabled || !user.isLoggedIn) {
                        isAppUnlocked = true
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (user.isLoggedIn && user.isBiometricEnabled && !isAppUnlocked) {
                        // Bank-Grade App Lock Screen with Brute Force Protection
                        AppLockScreen(
                            userName = user.name,
                            onUnlockClick = {
                                requestBiometricUnlock {
                                    isAppUnlocked = true
                                }
                            },
                            onPinUnlockSuccess = {
                                isAppUnlocked = true
                            }
                        )
                    } else {
                        AppNavGraph(
                            authRepository = authRepository,
                            expenseRepository = expenseRepository
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastBackgroundTimeMs = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        val user = authRepository.currentUser.value
        // Bank-Grade Auto-lock: If app is minimized for more than 15 seconds, re-lock
        if (user.isLoggedIn && user.isBiometricEnabled && lastBackgroundTimeMs > 0) {
            val elapsed = System.currentTimeMillis() - lastBackgroundTimeMs
            if (elapsed > 15_000) {
                triggerRelock?.invoke()
            }
        }
    }

    private fun requestBiometricUnlock(onSuccess: () -> Unit) {
        BiometricAuthManager.promptBiometric(
            activity = this,
            title = "Smart Expense Protected",
            subtitle = "Verify fingerprint or screen PIN to access financial records",
            onSuccess = {
                onSuccess()
            },
            onError = { err ->
                Toast.makeText(this, "Unlock cancelled: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AppLockScreen(
    userName: String,
    onUnlockClick: () -> Unit,
    onPinUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val savedPin = remember {
        context.getSharedPreferences("smart_expense_auth_prefs", Context.MODE_PRIVATE).getString("app_lock_pin", null)
    }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    // Brute-force countdown timer
    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            lockoutSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(ForestGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Smart Expense Protected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (userName.isNotEmpty()) "Welcome back, $userName\nHardware-backed security active" else "Bank-grade security active.\nPlease authenticate to proceed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!savedPin.isNullOrEmpty()) {
                if (lockoutSeconds > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Too many failed attempts!\nLocked out for $lockoutSeconds seconds.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { input ->
                            if (input.length <= 4) {
                                enteredPin = input
                                pinError = false
                                if (input.length == 4) {
                                    if (input == savedPin) {
                                        onPinUnlockSuccess()
                                    } else {
                                        pinError = true
                                        failedAttempts++
                                        if (failedAttempts >= 3) {
                                            lockoutSeconds = 30
                                            failedAttempts = 0
                                        }
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        label = { Text("Enter 4-Digit Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Incorrect PIN (${3 - failedAttempts} attempts remaining)",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onUnlockClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Unlock with Biometrics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
