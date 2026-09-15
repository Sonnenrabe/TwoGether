package com.example.ui.components

import android.accounts.AccountManager
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CoupleProfile
import com.example.ui.util.AppStrings

@Composable
fun PartnerPairingDialog(
    profile: CoupleProfile,
    onDismiss: () -> Unit,
    onJoinCode: (code: String, partnerName: String) -> Unit,
    onUpdateProfile: (myName: String, partnerName: String, myColor: String, partnerColor: String, togetherColor: String) -> Unit,
    onUpdateAppSettings: (appTheme: String, widgetTheme: String, language: String) -> Unit = { _, _, _ -> },
    onLinkGoogleAccount: (email: String, name: String) -> Unit = { _, _ -> },
    onDisconnectGoogleAccount: () -> Unit = {},
    onRestoreGoogleCloud: () -> Unit = {},
    isDriveSyncing: Boolean = false,
    driveStatusMessage: String? = null,
    onBackupToGoogleDrive: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onExportToGoogleDriveSaf: (Uri) -> Unit = {},
    onImportFromGoogleDriveSaf: (Uri) -> Unit = {},
    onClearDriveStatus: () -> Unit = {},
    onUnlinkPartner: (keepOwnEvents: Boolean) -> Unit = {},
    onSimulatePartnerPlan: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportToGoogleDriveSaf(uri)
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportFromGoogleDriveSaf(uri)
        }
    }

    var myName by remember { mutableStateOf(profile.myName) }
    var partnerName by remember { mutableStateOf(profile.partnerName) }
    var joinCodeInput by remember { mutableStateOf("") }
    var joinPartnerNameInput by remember { mutableStateOf("") }

    var myColorHex by remember { mutableStateOf(profile.myColorHex) }
    var partnerColorHex by remember { mutableStateOf(profile.partnerColorHex) }
    var togetherColorHex by remember { mutableStateOf(profile.togetherColorHex) }

    var selectedAppTheme by remember { mutableStateOf(profile.appThemeMode) }
    var selectedWidgetTheme by remember { mutableStateOf(profile.widgetThemeMode) }
    var selectedLanguage by remember { mutableStateOf(profile.appLanguage) }

    val lang = selectedLanguage
    fun t(key: String, vararg args: Any): String = AppStrings.get(lang, key, *args)
    val isDe = lang.equals("DE", ignoreCase = true)

    // Google Sign-In state
    var googleEmailInput by remember { mutableStateOf(profile.googleAccountEmail ?: "") }
    var googleNameInput by remember { mutableStateOf(profile.googleAccountName ?: profile.myName) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    // Native Android Google Account Picker / Sign-In Launcher
    val googleAccountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val displayName = accountName.substringBefore("@").replaceFirstChar { it.uppercase() }
                onLinkGoogleAccount(accountName, displayName)
                showGoogleLoginDialog = false
                Toast.makeText(
                    context,
                    if (isDe) "Erfolgreich mit Google angemeldet ($accountName)!" else "Successfully signed in with Google ($accountName)!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val launchGoogleSignIn: () -> Unit = {
        try {
            val intent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
            googleAccountPickerLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback to dialog if AccountManager intent cannot be handled directly
            showGoogleLoginDialog = true
        }
    }

    // Unlink Partner Dialog state
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }

    val availableColors = listOf(
        "#3B82F6" to "Sky Blue",
        "#EC4899" to "Coral Pink",
        "#8B5CF6" to "Lavender",
        "#E11D48" to "Rose Red",
        "#10B981" to "Emerald",
        "#F59E0B" to "Amber Gold",
        "#06B6D4" to "Cyan",
        "#6366F1" to "Indigo"
    )

    // Unlink Partner Confirmation Sub-Dialog
    if (showUnlinkConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.HeartBroken,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = t("unlink_partner"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isDe)
                            "Möchtest du die Kopplung mit \"${profile.partnerName}\" trennen? Wenn du die App Freunden weitergibst oder dich neu verbindest, kannst du einen frischen Code erhalten."
                        else
                            "Do you want to unlink from \"${profile.partnerName}\"? If you share this app with friends or connect with someone new, you can generate a fresh couple code.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isDe) "Wähle eine Option:" else "Choose an option:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            onUnlinkPartner(true)
                            showUnlinkConfirmDialog = false
                            Toast.makeText(
                                context,
                                if (isDe) "Partner getrennt! Eigene Termine behalten & neuer Code generiert." else "Partner unlinked! Kept your events & generated new code.",
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_unlink_keep_own")
                    ) {
                        Text(
                            text = if (isDe) "Eigene Termine behalten & neuer Code" else "Keep my events & new code",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onUnlinkPartner(false)
                            showUnlinkConfirmDialog = false
                            Toast.makeText(
                                context,
                                if (isDe) "Komplett zurückgesetzt! Frischer Kalender." else "Reset completely! Fresh calendar ready.",
                                Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_unlink_clear_all")
                    ) {
                        Text(
                            text = if (isDe) "Alles leeren & komplett neu starten" else "Clear all & start fresh",
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showUnlinkConfirmDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(t("cancel"))
                    }
                }
            }
        )
    }

    // Google Sign In Prompt Sub-Dialog
    if (showGoogleLoginDialog) {
        var showManualInput by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showGoogleLoginDialog = false },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isDe) "Google-Konto verbinden" else "Sign in with Google Account",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isDe)
                            "Melde dich mit deinem offiziellen Google-Konto an, um deine gemeinsamen Termine mit Ende-zu-Ende-Verschlüsselung (AES-256-GCM) in der Cloud zu sichern und auf jedem Gerät wiederherzustellen."
                        else
                            "Sign in with your verified Google Account to backup your couple calendar with AES-256-GCM encryption in the cloud and restore on any device.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Android System Google Account Picker / Sign-In Button
                    Surface(
                        onClick = {
                            launchGoogleSignIn()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_dialog_system_google_signin")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isDe) "Google-Konto auswählen / Anmelden" else "Select Google Account / Sign In",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Open Official Google Sign-In Page (Web / Browser)
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com/signin"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Browser could not be opened", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_google_web_signin")
                    ) {
                        Icon(imageVector = Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDe) "Google-Anmeldeseite im Browser öffnen" else "Open Google Sign-in Page in Browser",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Optional manual entry toggle (for dev or emulator without Google Play Services)
                    TextButton(
                        onClick = { showManualInput = !showManualInput },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showManualInput)
                                (if (isDe) "Manuelle Eingabe verbergen" else "Hide manual entry")
                            else
                                (if (isDe) "E-Mail manuell eintragen..." else "Enter email manually..."),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showManualInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = googleEmailInput,
                            onValueChange = { googleEmailInput = it },
                            label = { Text(if (isDe) "Google E-Mail" else "Google Email") },
                            placeholder = { Text("e.g. name@gmail.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_google_email_dialog"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = googleNameInput,
                            onValueChange = { googleNameInput = it },
                            label = { Text(if (isDe) "Dein Name" else "Display Name") },
                            placeholder = { Text("e.g. Marco") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_google_name_dialog"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (googleEmailInput.isNotBlank()) {
                                    val name = if (googleNameInput.isNotBlank()) googleNameInput.trim() else googleEmailInput.substringBefore("@")
                                    onLinkGoogleAccount(googleEmailInput.trim(), name)
                                    showGoogleLoginDialog = false
                                    Toast.makeText(
                                        context,
                                        if (isDe) "Google-Konto verknüpft! Cloud-Backup aktiv." else "Google Account linked! Cloud backup active.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = googleEmailInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("btn_manual_connect_backup")
                        ) {
                            Text(if (isDe) "Manuell verknüpfen" else "Link Manually", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGoogleLoginDialog = false }) {
                    Text(t("cancel"))
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(28.dp))
                .testTag("dialog_couple_pairing"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TwoGether - " + t("settings"),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: 0. Pairing, 1. Google Cloud, 2. Colors & Names, 3. Design, 4. Language
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    edgePadding = 6.dp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(t("pairing_tab"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(t("cloud_tab"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(t("profiles_tab"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text(t("appearance_tab"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text(t("language_tab") + " 🌐", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: PAIRING & UNLINK PARTNER (Protected by Google Sign-In)
                        val isGoogleLoggedIn = profile.isGoogleLinked && !profile.googleAccountEmail.isNullOrBlank()

                        if (!isGoogleLoggedIn) {
                            // Google Login Required Gate
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isDe) "🔒 Google-Login erforderlich" else "🔒 Google Sign-In Required",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isDe)
                                            "Um eure Termine vor Datenverlust und unbefugtem Zugriff zu schützen, müsst ihr euch zuerst mit eurem Google-Konto anmelden."
                                        else
                                            "To protect your calendar data and enable secure synchronization, both partners must sign in with their Google account before pairing.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showGoogleLoginDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_must_login_google")
                                    ) {
                                        Icon(imageVector = Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isDe) "Jetzt mit Google verbinden" else "Sign In with Google Account",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Google Connected Security Badge
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isDe) "Geschützt über Google-Konto" else "Secured via Google Account",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = profile.googleAccountEmail ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Pairing Status Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (profile.isPaired) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (profile.isPaired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (profile.isPaired) Icons.Filled.Favorite else Icons.Filled.PersonAdd,
                                        contentDescription = null,
                                        tint = if (profile.isPaired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (profile.isPaired)
                                                (if (isDe) "💖 Verbunden mit ${profile.partnerName}" else "💖 Linked with ${profile.partnerName}")
                                            else
                                                (if (isDe) "Noch nicht gekoppelt" else "Not Paired Yet"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (profile.isPaired)
                                                (if (isDe) "Eure Termine synchronisieren sich automatisch." else "Appointments sync between both devices.")
                                            else
                                                (if (isDe) "Teile deinen Code oder tritt deinem Partner bei." else "Share code or enter your partner's code to pair."),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = t("couple_code"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Couple Code Display Box
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = profile.coupleCode,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp,
                                            fontSize = 26.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isDe)
                                            "Teile diesen Code mit ${profile.partnerName}, damit ihr eure Kalender synchronisieren könnt!"
                                        else
                                            "Share this code with ${profile.partnerName} so they can sync with your calendar!",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Copy Code Button
                                        FilledTonalButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Couple Code", profile.coupleCode)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, if (isDe) "Code kopiert!" else "Couple code copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("btn_copy_couple_code")
                                        ) {
                                            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(t("copy_code"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Share Code Button
                                        Button(
                                            onClick = {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, "Let's plan together on TwoGether! Join using my couple code: ${profile.coupleCode}")
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "Share TwoGether Code"))
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("btn_share_couple_code")
                                        ) {
                                            Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(t("share_code"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Join Partner's Code Section
                            Text(
                                text = t("join_couple"),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase() },
                                label = { Text(if (isDe) "Code eingeben (z.B. LOVE-421)" else "Enter 6-digit Code (e.g. LOVE-421)") },
                                placeholder = { Text("LOVE-421") },
                                singleLine = true,
                                leadingIcon = { Icon(imageVector = Icons.Filled.Link, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_join_code"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = joinPartnerNameInput,
                                onValueChange = { joinPartnerNameInput = it },
                                label = { Text(if (isDe) "Name deines Partners" else "Partner's Name") },
                                placeholder = { Text("e.g. Alex / Sarah") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_partner_name_join"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotBlank()) {
                                        val pName = if (joinPartnerNameInput.isNotBlank()) joinPartnerNameInput.trim() else profile.partnerName
                                        onJoinCode(joinCodeInput.trim(), pName)
                                        Toast.makeText(context, if (isDe) "Mit Kalender $joinCodeInput verbunden!" else "Connected to couple calendar $joinCodeInput!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    }
                                },
                                enabled = joinCodeInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_confirm_join_code")
                            ) {
                                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t("connect_partner"), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Dedicated UNLINK PARTNER Button
                            OutlinedButton(
                                onClick = { showUnlinkConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_unlink_partner_trigger")
                            ) {
                                Icon(imageVector = Icons.Filled.HeartBroken, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "💔 " + t("unlink_partner"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }


                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Test / Simulation Tool
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDe) "Partner-Sync testen" else "Test Partner Sync",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isDe)
                                        "Simuliere, dass dein Partner einen Termin hinzufügt, um die Synchronisierung zu testen."
                                    else
                                        "Simulate your partner adding an appointment to test synchronization and widget updates right away.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FilledTonalButton(
                                    onClick = {
                                        onSimulatePartnerPlan()
                                        Toast.makeText(context, if (isDe) "Partner-Termin empfangen!" else "Partner appointment synced!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_simulate_partner_plan")
                                ) {
                                    Text(if (isDe) "✨ Test-Termin von ${profile.partnerName} erzeugen" else "✨ Add Test Appointment from ${profile.partnerName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: GOOGLE CLOUD BACKUP & RESTORE
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDe) "Google Play / Cloud Sicherung" else "Google Account Cloud Backup",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isDe)
                                    "Verknüpfe dein Google-Konto, um alle Termine sicher in der Cloud zu speichern. Bei einem neuen Handy oder Neuinstallation kannst du einfach mit Google einloggen und alle Daten wiederherstellen!"
                                else
                                    "Link your Google Account to protect your calendar dates in the cloud. If you switch devices or reinstall the app, simply log in with Google to restore where you left off!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (profile.isGoogleLinked && !profile.googleAccountEmail.isNullOrBlank()) {
                                // Connected Account Card
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (profile.googleAccountName ?: profile.myName).take(1).uppercase(),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 20.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = profile.googleAccountName ?: profile.myName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = profile.googleAccountEmail ?: "",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isDe) "Sicheres Cloud-Backup aktiv" else "Secure Cloud Backup Active",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Restore Button
                                        Button(
                                            onClick = {
                                                onRestoreGoogleCloud()
                                                Toast.makeText(
                                                    context,
                                                    if (isDe) "Termine aus Google Cloud wiederhergestellt!" else "Dates restored from Google Cloud!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("btn_restore_google_cloud")
                                        ) {
                                            Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isDe) "Termine aus Google Cloud abrufen" else "Restore Dates from Google Cloud", fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        TextButton(
                                            onClick = {
                                                onDisconnectGoogleAccount()
                                                Toast.makeText(context, if (isDe) "Google-Konto getrennt" else "Google Account disconnected", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(if (isDe) "Google-Konto trennen" else "Disconnect Google Account", fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                // Not connected card
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isDe) "Kein Google-Konto verbunden" else "No Google Account Linked",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isDe)
                                                "Verbinde dich, um deine gemeinsamen Termine bei Gerätewechsel nie zu verlieren."
                                            else
                                                "Sign in to protect your calendar dates and never lose them when switching phones.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Official Google Sign-In Button
                                        Surface(
                                            onClick = { launchGoogleSignIn() },
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            tonalElevation = 2.dp,
                                            shadowElevation = 1.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_sign_in_google")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                                    contentDescription = "Google Logo",
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (isDe) "Über Google anmelden" else "Sign in with Google",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Alternative sign in & browser login link
                                        TextButton(
                                            onClick = { showGoogleLoginDialog = true },
                                            modifier = Modifier.fillMaxWidth().testTag("btn_google_options")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.OpenInBrowser,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDe) "Anmelde-Optionen & Browser-Login" else "Sign-in options & Browser login",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Google Drive Direct Backup & SAF File Management Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().testTag("card_google_drive_backup")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CloudSync,
                                                contentDescription = "Google Drive",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isDe) "Google Drive Backup" else "Google Drive Backup",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = if (isDe) "twogether_couple_backup.json" else "twogether_couple_backup.json",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isDe)
                                            "Sichere deine gemeinsamen Termine & Kategorien direkt als Datei in Google Drive. Du kannst die Datei in deinem Google Drive sehen, verwalten und auf jedem Gerät wiederherstellen."
                                        else
                                            "Backup your shared appointments & categories directly to Google Drive. You can see the file in your Google Drive, manage it, and restore it anytime.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    )

                                    if (isDriveSyncing) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isDe) "Google Drive Synchronisation läuft..." else "Syncing with Google Drive...",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }

                                    if (!driveStatusMessage.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = driveStatusMessage,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontSize = 11.sp
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = onClearDriveStatus,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Schließen",
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Direct Google Drive App Sync Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onBackupToGoogleDrive,
                                            enabled = !isDriveSyncing,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("btn_drive_backup")
                                        ) {
                                            Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDe) "In Drive sichern" else "Backup to Drive",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = onRestoreFromGoogleDrive,
                                            enabled = !isDriveSyncing,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("btn_drive_restore")
                                        ) {
                                            Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isDe) "Aus Drive laden" else "Restore Drive",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // SAF File System & Google Drive App Pickers
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                createDocLauncher.launch("twogether_couple_backup.json")
                                            },
                                            enabled = !isDriveSyncing,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("btn_saf_export")
                                        ) {
                                            Icon(imageVector = Icons.Filled.SaveAs, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isDe) "Speichern unter…" else "Save file as…",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                openDocLauncher.launch(arrayOf("application/json", "*/*"))
                                            },
                                            enabled = !isDriveSyncing,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).testTag("btn_saf_import")
                                        ) {
                                            Icon(imageVector = Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isDe) "Datei öffnen…" else "Open file…",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // End-to-End Encryption (E2EE) Transparency & Security Card
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().testTag("card_e2ee_security_status")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isDe) "Ende-zu-Ende-Verschlüsselung" else "End-to-End Encryption",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "AES-256-GCM • Zero Knowledge",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = if (isDe)
                                            "Alle Termine, Notizen und Checklisten werden direkt auf deinem Smartphone mit AES-256-GCM verschlüsselt. Wenn jemand die JSON-Datei in der Cloud oder auf einem Speicher findet, sieht er nur unlesbaren Buchstabensalat (Ciphertext). Ohne euren privaten Schlüssel ist der Inhalt unmöglich zu entschlüsseln."
                                        else
                                            "All appointments, notes, and checklist items are encrypted directly on your smartphone with military-grade AES-256-GCM. Even if someone discovers the raw JSON file on cloud storage or a relay, they can only see unreadable ciphertext. Without your private keys, decrypting is mathematically impossible.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Security badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "256-Bit", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(text = "AES-GCM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "10.000x", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(text = "PBKDF2 SHA-256", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "128-Bit", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(text = if (isDe) "Integritätstag" else "Auth Tag", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Copy / Export Encrypted JSON Proof Button
                                    OutlinedButton(
                                        onClick = {
                                            val syncService = com.example.data.remote.PartnerSyncService()
                                            val sampleEmail = profile.googleAccountEmail?.ifBlank { null } ?: "couple@local.device"
                                            val sampleBackup = com.example.data.remote.GoogleCloudBackupEnvelope(
                                                userEmail = sampleEmail,
                                                coupleCode = profile.coupleCode,
                                                myName = profile.myName,
                                                partnerName = profile.partnerName,
                                                isPaired = profile.isPaired,
                                                lastUpdated = System.currentTimeMillis()
                                            )
                                            val encryptedJsonSample = syncService.exportEncryptedBackupJson(sampleBackup)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Encrypted TwoGether JSON", encryptedJsonSample)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(
                                                context,
                                                if (isDe) "Verschlüsseltes JSON kopiert! (Nur Ciphertext enthalten)" else "Encrypted JSON copied! (Only ciphertext contained)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_copy_encrypted_json_proof")
                                    ) {
                                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isDe) "Verschlüsseltes JSON kopieren (Beweis)" else "Copy Encrypted JSON (Proof)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: CUSTOMIZE NAMES & COLORS
                        OutlinedTextField(
                            value = myName,
                            onValueChange = { myName = it },
                            label = { Text(if (isDe) "Dein Name" else "Your Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_my_name"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text(if (isDe) "Name des Partners" else "Partner's Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_partner_name"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 1. My Color
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val curMyColor = try { Color(android.graphics.Color.parseColor(myColorHex)) } catch (e: Exception) { Color(0xFF3B82F6) }
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(curMyColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDe) "Deine Farbe" else "Your Color",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableColors.forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSel = myColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                        .clickable { myColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Partner Color
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val curPartnerColor = try { Color(android.graphics.Color.parseColor(partnerColorHex)) } catch (e: Exception) { Color(0xFFEC4899) }
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(curPartnerColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDe) "${partnerName}s Farbe" else "${partnerName}'s Color",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableColors.forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSel = partnerColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                        .clickable { partnerColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Together Color
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val curTogetherColor = try { Color(android.graphics.Color.parseColor(togetherColorHex)) } catch (e: Exception) { Color(0xFF8B5CF6) }
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(curTogetherColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t("together_color"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableColors.forEach { (hex, _) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSel = togetherColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                        .clickable { togetherColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                onUpdateProfile(myName.trim(), partnerName.trim(), myColorHex, partnerColorHex, togetherColorHex)
                                Toast.makeText(context, if (isDe) "Profile aktualisiert!" else "Profiles updated!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_profiles")
                        ) {
                            Text(t("save"), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                onDisconnect()
                                Toast.makeText(context, if (isDe) "Kopplung getrennt & neuer Code generiert" else "Pairing disconnected & new code generated", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(t("disconnect"), fontSize = 12.sp)
                        }
                    }

                    3 -> {
                        // TAB 3: DESIGN ONLY (App Theme & Widget Theme)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Section 1: App Theme
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = t("app_theme"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            val themeOptions = listOf(
                                "SYSTEM" to (t("theme_system") to "📱"),
                                "LIGHT" to (t("theme_light") to "☀️"),
                                "DARK" to (t("theme_dark") to "🌙")
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                themeOptions.forEach { (mode, pair) ->
                                    val (label, icon) = pair
                                    val isSelected = selectedAppTheme.equals(mode, ignoreCase = true)
                                    Surface(
                                        onClick = { selectedAppTheme = mode },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chip_app_theme_$mode")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = icon, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Section 2: Widget Theme
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = t("widget_theme"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDe) "Wähle unabhängig vom App-Design hell oder dunkel für das Widget auf deinem Startbildschirm." else "Customize the home screen widget independently from the app theme.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val widgetThemeOptions = listOf(
                                "SYSTEM" to (t("theme_system") to "📱"),
                                "LIGHT" to (t("theme_light") to "☀️"),
                                "DARK" to (t("theme_dark") to "🌙")
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                widgetThemeOptions.forEach { (mode, pair) ->
                                    val (label, icon) = pair
                                    val isSelected = selectedWidgetTheme.equals(mode, ignoreCase = true)
                                    Surface(
                                        onClick = { selectedWidgetTheme = mode },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chip_widget_theme_$mode")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = icon, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    onUpdateAppSettings(selectedAppTheme, selectedWidgetTheme, selectedLanguage)
                                    Toast.makeText(
                                        context,
                                        if (isDe) "Design-Einstellungen gespeichert!" else "Design settings saved!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_save_theme_settings")
                            ) {
                                Text(
                                    text = t("save"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    4 -> {
                        // TAB 4: DEDICATED LANGUAGE SELECTION (English, German, Italian, Spanish, French, System)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("app_language"),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isDe)
                                    "Wähle deine bevorzugte Sprache für TwoGether. Die Änderung wird sofort auf alle Ansichten und Widgets angewendet."
                                else
                                    "Choose your preferred language for TwoGether. Changes will be instantly applied across the app and calendar widgets.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val languages = listOf(
                                AppStrings.SupportedLanguage.SYSTEM to t("lang_system"),
                                AppStrings.SupportedLanguage.EN to "English 🇬🇧",
                                AppStrings.SupportedLanguage.DE to "Deutsch 🇩🇪",
                                AppStrings.SupportedLanguage.IT to "Italiano 🇮🇹",
                                AppStrings.SupportedLanguage.ES to "Español 🇪🇸",
                                AppStrings.SupportedLanguage.FR to "Français 🇫🇷"
                            )

                            languages.forEach { (langObj, label) ->
                                val isSelected = selectedLanguage.equals(langObj.code, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedLanguage = langObj.code
                                            onUpdateAppSettings(selectedAppTheme, selectedWidgetTheme, langObj.code)
                                            Toast.makeText(context, "Language set: $label", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("lang_option_${langObj.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = langObj.flag,
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    onUpdateAppSettings(selectedAppTheme, selectedWidgetTheme, selectedLanguage)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_save_language")
                            ) {
                                Text(t("save"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Generous bottom spacer so no content is cut off on any device screen
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
