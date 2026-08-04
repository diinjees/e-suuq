package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.AppViewModel
import com.example.ui.LocalLocalization
import com.example.ui.BiometricHelper

@Composable
fun AppLockScreen(viewModel: AppViewModel) {
    val localization = LocalLocalization.current
    val context = LocalContext.current
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    var showSetupSecurityDialog by remember { mutableStateOf(false) }

    // Detect available security types
    val isSecure = remember(context) { BiometricHelper.hasDeviceSecurity(context) }
    val isBiometricReady = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    // Automatically trigger BiometricPrompt on screen launch if enabled
    LaunchedEffect(activeUser) {
        if (activeUser?.isBiometricEnabled == true) {
            if (isSecure) {
                BiometricHelper.showBiometricAndCredentialPrompt(
                    context = context,
                    title = "Unlock E-Suuq",
                    subtitle = "Authenticate using your phone security to unlock",
                    onSuccess = {
                        viewModel.navigateTo(AppScreen.MAIN)
                    },
                    onError = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                showSetupSecurityDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Circular icon depending on available security type
                val icon = when {
                    !isSecure -> Icons.Default.LockOpen
                    isBiometricReady -> Icons.Default.Fingerprint
                    else -> Icons.Default.VpnKey
                }
                
                val iconColor = when {
                    !isSecure -> MaterialTheme.colorScheme.error
                    isBiometricReady -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                val containerColor = iconColor.copy(alpha = 0.12f)

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Security Status Icon",
                        tint = iconColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Security Type Header & Instructions
                val securityTitle = when {
                    !isSecure -> "No Device Security Setup"
                    isBiometricReady -> "Biometric Authentication"
                    else -> "Device Screen Lock"
                }

                val securitySubtitle = when {
                    !isSecure -> "Your phone does not have any screen lock or biometric security enabled. Please set it up in your device settings to unlock E-Suuq."
                    isBiometricReady -> "Authenticate using your phone's fingerprint, face recognition, or standard credential to unlock."
                    else -> "Authenticate using your phone's PIN, Pattern, or Password to unlock."
                }

                Text(
                    text = securityTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = securitySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Unlock / Setup Action Button
                if (isSecure) {
                    Button(
                        onClick = {
                            BiometricHelper.showBiometricAndCredentialPrompt(
                                context = context,
                                title = "Unlock E-Suuq",
                                subtitle = "Authenticate using your phone security to unlock",
                                onSuccess = {
                                    viewModel.navigateTo(AppScreen.MAIN)
                                },
                                onError = { err ->
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val buttonText = if (isBiometricReady) "Unlock with Biometrics" else "Unlock with PIN / Pattern"
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            BiometricHelper.openSecuritySettings(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = "Set Up Phone Security",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSetupSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSetupSecurityDialog = false },
            title = {
                Text(
                    text = "Device Security Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Your phone does not have any screen lock or biometric security enabled (such as PIN, Pattern, Password, Fingerprint, or Face). Please set it up in your device settings to unlock E-Suuq.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSetupSecurityDialog = false
                        BiometricHelper.openSecuritySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSetupSecurityDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}
