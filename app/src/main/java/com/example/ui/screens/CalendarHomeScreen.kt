package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddEditAppointmentDialog
import com.example.ui.components.AddEditNoteDialog
import com.example.ui.components.AgendaCalendarView
import com.example.ui.components.AppointmentDetailDialog
import com.example.ui.components.DayScheduleView
import com.example.ui.components.FilterChipRow
import com.example.ui.components.ManageCategoriesDialog
import com.example.ui.components.MonthCalendarView
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.NoteDetailDialog
import com.example.ui.components.PartnerHeaderBar
import com.example.ui.components.PartnerPairingDialog
import com.example.ui.components.WeekCalendarView
import com.example.ui.viewmodel.CalendarUiState
import com.example.ui.viewmodel.CalendarViewMode
import com.example.ui.viewmodel.CalendarViewModel
import com.example.ui.viewmodel.MainNavigationTab
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHomeScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingLinkRequest by viewModel.pendingLinkRequest.collectAsStateWithLifecycle()
    val isDriveSyncing by viewModel.isDriveSyncing.collectAsStateWithLifecycle()
    val driveStatusMessage by viewModel.driveStatusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lang = uiState.coupleProfile.appLanguage
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uiState.newCreatedAlert) {
        uiState.newCreatedAlert?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearAlert()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_home_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.currentMainTab == MainNavigationTab.CALENDAR) Icons.Filled.CalendarMonth else Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "TwoGether",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (uiState.currentMainTab == MainNavigationTab.NOTEBOOK) {
                                Text(
                                    text = com.example.ui.util.AppStrings.get(lang, "notebook_title"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (uiState.currentMainTab == MainNavigationTab.CALENDAR) {
                        IconButton(
                            onClick = { viewModel.setSearchActive(!uiState.isSearchActive) },
                            modifier = Modifier.testTag("btn_toggle_search")
                        ) {
                            Icon(
                                imageVector = if (uiState.isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search Appointments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.openPairingDialog() },
                        modifier = Modifier.testTag("btn_top_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu & Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_main_nav")
            ) {
                NavigationBarItem(
                    selected = uiState.currentMainTab == MainNavigationTab.CALENDAR,
                    onClick = { viewModel.switchMainTab(MainNavigationTab.CALENDAR) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = com.example.ui.util.AppStrings.get(lang, "nav_calendar")
                        )
                    },
                    label = {
                        Text(
                            text = com.example.ui.util.AppStrings.get(lang, "nav_calendar"),
                            fontWeight = if (uiState.currentMainTab == MainNavigationTab.CALENDAR) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_calendar")
                )

                NavigationBarItem(
                    selected = uiState.currentMainTab == MainNavigationTab.NOTEBOOK,
                    onClick = { viewModel.switchMainTab(MainNavigationTab.NOTEBOOK) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = com.example.ui.util.AppStrings.get(lang, "nav_notebook")
                        )
                    },
                    label = {
                        Text(
                            text = com.example.ui.util.AppStrings.get(lang, "nav_notebook"),
                            fontWeight = if (uiState.currentMainTab == MainNavigationTab.NOTEBOOK) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_notebook")
                )
            }
        },
        floatingActionButton = {
            if (uiState.currentMainTab == MainNavigationTab.NOTEBOOK) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddNoteDialog() },
                    icon = { Icon(imageVector = Icons.Filled.EditNote, contentDescription = null) },
                    text = {
                        Text(
                            text = com.example.ui.util.AppStrings.get(lang, "add_note"),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_add_note")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.currentMainTab == MainNavigationTab.CALENDAR) {
                // Calendar View Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Search Bar (if activated)
                    AnimatedVisibility(
                        visible = uiState.isSearchActive,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(com.example.ui.util.AppStrings.get(lang, "search_placeholder")) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = com.example.ui.util.AppStrings.get(lang, "search_clear")
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("input_search_appointments"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Couple Sync & Status Header Bar
                    PartnerHeaderBar(
                        profile = uiState.coupleProfile,
                        syncState = uiState.syncState,
                        onSyncClick = { viewModel.syncWithPartner() },
                        onPairingClick = { viewModel.openPairingDialog() }
                    )

                    // View Mode Selector (Month, Week, Agenda)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        TabRow(
                            selectedTabIndex = uiState.viewMode.ordinal,
                            containerColor = Color.Transparent,
                            indicator = {},
                            divider = {}
                        ) {
                            CalendarViewMode.entries.forEach { mode ->
                                val isSelected = uiState.viewMode == mode
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.setViewMode(mode) },
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(MaterialTheme.colorScheme.surface)
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (mode) {
                                                    CalendarViewMode.MONTH -> Icons.Filled.CalendarMonth
                                                    CalendarViewMode.WEEK -> Icons.Filled.ViewWeek
                                                    CalendarViewMode.AGENDA -> Icons.Filled.ViewAgenda
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val tabKey = when (mode) {
                                                CalendarViewMode.MONTH -> "tab_month"
                                                CalendarViewMode.WEEK -> "tab_week"
                                                CalendarViewMode.AGENDA -> "tab_agenda"
                                            }
                                            Text(
                                                text = com.example.ui.util.AppStrings.get(uiState.coupleProfile.appLanguage, tabKey),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Filter Chips (All, Together, Mine, Partner)
                    FilterChipRow(
                        currentFilter = uiState.ownerFilter,
                        profile = uiState.coupleProfile,
                        onFilterSelected = { viewModel.setOwnerFilter(it) }
                    )

                    // Main Calendar Content
                    when (uiState.viewMode) {
                        CalendarViewMode.MONTH -> {
                            MonthCalendarView(
                                displayMonthMillis = uiState.displayMonthMillis,
                                selectedDateMillis = uiState.selectedDateMillis,
                                appointments = uiState.filteredAppointments,
                                profile = uiState.coupleProfile,
                                viewMode = uiState.viewMode,
                                onDateSelected = { viewModel.selectDate(it) },
                                onPrevMonth = { viewModel.prevMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onTodayClick = { viewModel.goToToday() },
                                onViewModeChanged = { viewModel.setViewMode(it) },
                                onMonthClick = { viewModel.openMonthYearPicker() }
                            )

                            // Overview of Selected Day below Month View
                            DayScheduleView(
                                selectedDateMillis = uiState.selectedDateMillis,
                                appointments = uiState.selectedDayAppointments,
                                profile = uiState.coupleProfile,
                                onAddClick = { viewModel.openAddDialog(uiState.selectedDateMillis) },
                                onAppointmentClick = { viewModel.openDetailDialog(it) }
                            )
                        }

                        CalendarViewMode.WEEK -> {
                            WeekCalendarView(
                                selectedDateMillis = uiState.selectedDateMillis,
                                appointments = uiState.filteredAppointments,
                                profile = uiState.coupleProfile,
                                onDateSelected = { viewModel.selectDate(it) },
                                onPrevWeek = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDateMillis }
                                    cal.add(Calendar.WEEK_OF_YEAR, -1)
                                    viewModel.selectDate(cal.timeInMillis)
                                },
                                onNextWeek = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDateMillis }
                                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                                    viewModel.selectDate(cal.timeInMillis)
                                },
                                onTodayClick = { viewModel.goToToday() },
                                onMonthClick = { viewModel.openMonthYearPicker() }
                            )

                            DayScheduleView(
                                selectedDateMillis = uiState.selectedDateMillis,
                                appointments = uiState.selectedDayAppointments,
                                profile = uiState.coupleProfile,
                                onAddClick = { viewModel.openAddDialog(uiState.selectedDateMillis) },
                                onAppointmentClick = { viewModel.openDetailDialog(it) }
                            )
                        }

                        CalendarViewMode.AGENDA -> {
                            AgendaCalendarView(
                                appointments = uiState.filteredAppointments,
                                profile = uiState.coupleProfile,
                                onAppointmentClick = { viewModel.openDetailDialog(it) },
                                onAddClick = { viewModel.openAddDialog(uiState.selectedDateMillis) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(90.dp)) // Padding for FAB
                }
            } else {
                // Separate Notebook Tab
                NotebookScreen(
                    profile = uiState.coupleProfile,
                    notes = uiState.notes,
                    categories = uiState.noteCategories,
                    onOpenManageCategories = { viewModel.openManageCategoriesDialog(initialTab = 1) },
                    searchQuery = uiState.noteSearchQuery,
                    onSearchQueryChange = { viewModel.setNoteSearchQuery(it) },
                    selectedCategory = uiState.selectedNoteCategory,
                    onSelectCategory = { viewModel.setNoteCategoryFilter(it) },
                    selectedOwnerType = uiState.selectedNoteOwnerType,
                    onSelectOwnerType = { viewModel.setNoteOwnerTypeFilter(it) },
                    onNoteClick = { viewModel.openNoteDetailDialog(it) },
                    onTogglePin = { id, pinned -> viewModel.togglePinNote(id, pinned) },
                    onToggleChecklistItem = { noteId, idx, isChecked ->
                        viewModel.toggleChecklistItem(noteId, idx, isChecked)
                    },
                    onShareNote = { viewModel.shareNote(it) },
                    onDeleteNote = { viewModel.deleteNote(it) },
                    onSyncNotes = { viewModel.syncNotes() },
                    onAddNewNote = { viewModel.openAddNoteDialog() }
                )
            }
        }
    }

    // Dialogs
    if (uiState.isMonthYearPickerOpen) {
        MonthYearPickerDialog(
            initialMillis = uiState.displayMonthMillis,
            appLanguage = uiState.coupleProfile.appLanguage,
            onDismissRequest = { viewModel.closeMonthYearPicker() },
            onMonthYearSelected = { year, month ->
                viewModel.setMonthAndYear(year, month)
            }
        )
    }

    if (uiState.isAddDialogOpen) {
        AddEditAppointmentDialog(
            initialDateMillis = uiState.selectedDateMillis,
            editingAppointment = uiState.editingAppointment,
            profile = uiState.coupleProfile,
            categories = uiState.appointmentCategories,
            onManageCategories = { viewModel.openManageCategoriesDialog(initialTab = 0) },
            onDismiss = { viewModel.closeAddDialog() },
            onSave = { viewModel.saveAppointment(it) },
            onDelete = {
                viewModel.deleteAppointment(it)
                viewModel.closeAddDialog()
            }
        )
    }

    // Add / Edit Note Dialog
    if (uiState.isAddEditNoteDialogOpen) {
        AddEditNoteDialog(
            profile = uiState.coupleProfile,
            editingNote = uiState.editingNote,
            categories = uiState.noteCategories,
            onDismiss = { viewModel.closeNoteDialog() },
            onSave = { viewModel.saveNote(it) },
            onDelete = { viewModel.deleteNote(it) },
            onShare = { viewModel.shareNote(it) }
        )
    }

    // Manage Categories Dialog
    if (uiState.isManageCategoriesDialogOpen) {
        ManageCategoriesDialog(
            initialTab = uiState.manageCategoriesInitialTab,
            appointmentCategories = uiState.appointmentCategories,
            noteCategories = uiState.noteCategories,
            appLanguage = uiState.coupleProfile.appLanguage,
            onAddAppointmentCategory = { viewModel.addAppointmentCategory(it) },
            onUpdateAppointmentCategory = { viewModel.updateAppointmentCategory(it) },
            onDeleteAppointmentCategory = { viewModel.deleteAppointmentCategory(it) },
            onAddNoteCategory = { viewModel.addNoteCategory(it) },
            onUpdateNoteCategory = { viewModel.updateNoteCategory(it) },
            onDeleteNoteCategory = { viewModel.deleteNoteCategory(it) },
            onDismiss = { viewModel.closeManageCategoriesDialog() }
        )
    }

    if (uiState.isDetailDialogOpen && uiState.selectedDetailAppointment != null) {
        AppointmentDetailDialog(
            appointment = uiState.selectedDetailAppointment!!,
            profile = uiState.coupleProfile,
            onDismiss = { viewModel.closeDetailDialog() },
            onEdit = {
                val appt = uiState.selectedDetailAppointment!!
                viewModel.closeDetailDialog()
                viewModel.openEditDialog(appt)
            },
            onDelete = {
                viewModel.deleteAppointment(uiState.selectedDetailAppointment!!.id)
            }
        )
    }

    if (uiState.isNoteDetailDialogOpen && uiState.selectedDetailNote != null) {
        val noteToView = uiState.selectedDetailNote!!
        NoteDetailDialog(
            note = noteToView,
            profile = uiState.coupleProfile,
            onDismiss = { viewModel.closeNoteDetailDialog() },
            onEdit = {
                viewModel.closeNoteDetailDialog()
                viewModel.openEditNoteDialog(noteToView)
            },
            onDelete = {
                viewModel.deleteNote(noteToView.id)
                viewModel.closeNoteDetailDialog()
            },
            onTogglePin = { noteId, isPinned ->
                viewModel.togglePinNote(noteId, isPinned)
            },
            onShare = { note ->
                viewModel.shareNote(note)
            }
        )
    }

    if (uiState.isPairingDialogOpen) {
        PartnerPairingDialog(
            profile = uiState.coupleProfile,
            onDismiss = { viewModel.closePairingDialog() },
            onJoinCode = { code, partnerName -> viewModel.joinCoupleCode(code, partnerName) },
            onUpdateProfile = { myName, partnerName, myColor, partnerColor, togetherColor ->
                viewModel.updateProfile(myName, partnerName, myColor, partnerColor, togetherColor)
            },
            onUpdateAppSettings = { appTheme, widgetTheme, language ->
                viewModel.updateAppSettings(appTheme, widgetTheme, language)
            },
            onLinkGoogleAccount = { email, name ->
                viewModel.linkGoogleAccount(email, name)
            },
            onDisconnectGoogleAccount = {
                viewModel.disconnectGoogleAccount()
            },
            onRestoreGoogleCloud = {
                viewModel.restoreFromGoogleCloud()
            },
            isDriveSyncing = isDriveSyncing,
            driveStatusMessage = driveStatusMessage,
            onBackupToGoogleDrive = { viewModel.backupToGoogleDrive() },
            onRestoreFromGoogleDrive = { viewModel.restoreFromGoogleDrive() },
            onExportToGoogleDriveSaf = { uri -> viewModel.exportToGoogleDriveSaf(uri) },
            onImportFromGoogleDriveSaf = { uri -> viewModel.importFromGoogleDriveSaf(uri) },
            onClearDriveStatus = { viewModel.clearDriveStatusMessage() },
            onRegenerateCode = { viewModel.regenerateCoupleCode() },
            onUnlinkPartner = { keepOwnEvents ->
                viewModel.unlinkPartner(keepOwnEvents)
            },
            onSimulatePartnerPlan = { viewModel.simulatePartnerActivity() },
            onDisconnect = { viewModel.disconnectPairing() },
            onUpdateAutoSyncInterval = { interval -> viewModel.updateAutoSyncInterval(interval) },
            pendingLinkRequest = pendingLinkRequest,
            onAcceptPartnerLink = { req -> viewModel.acceptPartnerLink(req) },
            onDismissPartnerLink = { req -> viewModel.dismissPartnerLink(req) },
            onCheckPendingLinkRequest = { viewModel.checkForPendingLinkRequest() }
        )
    }

    // Global Incoming Partner Link Dialog (prompts immediately when partner enters our code)
    pendingLinkRequest?.let { req ->
        val isDe = lang == "DE"
        val reqName = if (req.partnerName.isNotBlank() && req.partnerName != "Partner")
            req.partnerName
        else if (isDe) "Dein Partner" else "Your partner"

        AlertDialog(
            onDismissRequest = { viewModel.dismissPartnerLink(req) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isDe) "Partner möchte verbinden! 💕" else "Partner wants to link up! 💕",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (isDe)
                        "$reqName hat deinen Paar-Code ${req.coupleCode} eingegeben!\n\nMöchtest du eure Kalender und Notizen jetzt automatisch miteinander verbinden?"
                    else
                        "$reqName entered your Couple Code ${req.coupleCode}!\n\nWould you like to link up and sync calendars and notes automatically?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptPartnerLink(req) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_accept_partner_link")
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDe) "Jetzt verbinden 💕" else "Link Up Now 💕", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPartnerLink(req) },
                    modifier = Modifier.testTag("btn_dismiss_partner_link")
                ) {
                    Text(if (isDe) "Später" else "Not Now")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

