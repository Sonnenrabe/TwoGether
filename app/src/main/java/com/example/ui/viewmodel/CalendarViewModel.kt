package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CouplePreferences
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.CoupleProfile
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import com.example.data.model.PartnerLinkRequest
import com.example.data.model.SyncState
import com.example.data.remote.GoogleDriveBackupPayload
import com.example.data.remote.GoogleDriveFileMetadata
import com.example.data.remote.GoogleDriveSyncService
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

enum class MainNavigationTab {
    CALENDAR,
    NOTEBOOK
}

enum class CalendarViewMode(val label: String) {
    MONTH("Month"),
    WEEK("Week"),
    AGENDA("Agenda")
}

enum class OwnerFilter(val label: String, val badge: String) {
    ALL("All", "✨"),
    TOGETHER("Together", "💜"),
    ME("My Plans", "💙"),
    PARTNER("Partner", "💖")
}

data class CalendarUiState(
    val currentMainTab: MainNavigationTab = MainNavigationTab.CALENDAR,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val displayMonthMillis: Long = System.currentTimeMillis(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val ownerFilter: OwnerFilter = OwnerFilter.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val syncMessage: String = "",
    val allAppointments: List<Appointment> = emptyList(),
    val filteredAppointments: List<Appointment> = emptyList(),
    val selectedDayAppointments: List<Appointment> = emptyList(),
    val coupleProfile: CoupleProfile = CoupleProfile(),
    val isAddDialogOpen: Boolean = false,
    val editingAppointment: Appointment? = null,
    val isPairingDialogOpen: Boolean = false,
    val isDetailDialogOpen: Boolean = false,
    val selectedDetailAppointment: Appointment? = null,
    val isMonthYearPickerOpen: Boolean = false,
    val newCreatedAlert: String? = null,
    // Notebook fields
    val notes: List<Note> = emptyList(),
    val noteSearchQuery: String = "",
    val selectedNoteCategory: NoteCategory? = null,
    val selectedNoteOwnerType: OwnerType? = null,
    val isAddEditNoteDialogOpen: Boolean = false,
    val editingNote: Note? = null,
    val noteCategories: List<NoteCategory> = NoteCategory.DEFAULT_CATEGORIES,
    val appointmentCategories: List<AppointmentCategory> = AppointmentCategory.DEFAULT_CATEGORIES,
    val isManageCategoriesDialogOpen: Boolean = false,
    val manageCategoriesInitialTab: Int = 0,
    val isNoteDetailDialogOpen: Boolean = false,
    val selectedDetailNote: Note? = null
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val couplePreferences = CouplePreferences(application)
    private val repository = AppointmentRepository(
        context = application,
        appointmentDao = db.appointmentDao(),
        couplePreferences = couplePreferences
    )
    private val noteRepository = NoteRepository(
        context = application,
        noteDao = db.noteDao(),
        couplePreferences = couplePreferences
    )
    private val reminderScheduler = com.example.util.AppointmentReminderScheduler(application)

    private val _currentMainTab = MutableStateFlow(MainNavigationTab.CALENDAR)
    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    private val _displayMonthMillis = MutableStateFlow(System.currentTimeMillis())
    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    private val _ownerFilter = MutableStateFlow(OwnerFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val _syncMessage = MutableStateFlow("")
    private val _isAddDialogOpen = MutableStateFlow(false)
    private val _editingAppointment = MutableStateFlow<Appointment?>(null)
    private val _isPairingDialogOpen = MutableStateFlow(false)
    private val _isDetailDialogOpen = MutableStateFlow(false)
    private val _selectedDetailAppointment = MutableStateFlow<Appointment?>(null)
    private val _isMonthYearPickerOpen = MutableStateFlow(false)
    private val _newCreatedAlert = MutableStateFlow<String?>(null)

    // Notebook StateFlows
    private val _noteSearchQuery = MutableStateFlow("")
    private val _selectedNoteCategory = MutableStateFlow<NoteCategory?>(null)
    private val _selectedNoteOwnerType = MutableStateFlow<OwnerType?>(null)
    private val _isAddEditNoteDialogOpen = MutableStateFlow(false)
    private val _editingNote = MutableStateFlow<Note?>(null)
    private val _isManageCategoriesDialogOpen = MutableStateFlow(false)
    private val _manageCategoriesInitialTab = MutableStateFlow(0)
    private val _isNoteDetailDialogOpen = MutableStateFlow(false)
    private val _selectedDetailNote = MutableStateFlow<Note?>(null)

    // Partner link request StateFlow
    private val _pendingLinkRequest = MutableStateFlow<PartnerLinkRequest?>(null)
    val pendingLinkRequest: StateFlow<PartnerLinkRequest?> = _pendingLinkRequest.asStateFlow()
    private val _dismissedPartnerDeviceIds = mutableSetOf<String>()

    private var autoSyncJob: Job? = null

    val coupleProfile: StateFlow<CoupleProfile> = couplePreferences.coupleProfile

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.allActiveAppointments,
        coupleProfile,
        _selectedDateMillis,
        _displayMonthMillis,
        _viewMode,
        _ownerFilter,
        _searchQuery,
        _isSearchActive,
        _syncState,
        _syncMessage,
        _isAddDialogOpen,
        _editingAppointment,
        _isPairingDialogOpen,
        _isDetailDialogOpen,
        _selectedDetailAppointment,
        _isMonthYearPickerOpen,
        _newCreatedAlert,
        _currentMainTab,
        noteRepository.allActiveNotes,
        _noteSearchQuery,
        _selectedNoteCategory,
        _selectedNoteOwnerType,
        _isAddEditNoteDialogOpen,
        _editingNote,
        noteRepository.noteCategories,
        _isManageCategoriesDialogOpen,
        repository.appointmentCategories,
        _manageCategoriesInitialTab,
        _isNoteDetailDialogOpen,
        _selectedDetailNote
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allAppts = args[0] as List<Appointment>
        val profile = args[1] as CoupleProfile
        val selectedDate = args[2] as Long
        val displayMonth = args[3] as Long
        val viewMode = args[4] as CalendarViewMode
        val ownerFilter = args[5] as OwnerFilter
        val searchQuery = args[6] as String
        val isSearchActive = args[7] as Boolean
        val syncState = args[8] as SyncState
        val syncMsg = args[9] as String
        val isAddOpen = args[10] as Boolean
        val editingAppt = args[11] as Appointment?
        val isPairingOpen = args[12] as Boolean
        val isDetailOpen = args[13] as Boolean
        val selectedDetail = args[14] as Appointment?
        val isMonthYearOpen = args[15] as Boolean
        val newAlert = args[16] as String?
        val mainTab = args[17] as MainNavigationTab
        val allNotes = args[18] as List<Note>
        val nbSearch = args[19] as String
        val nbCategory = args[20] as NoteCategory?
        val nbOwner = args[21] as OwnerType?
        val isNbAddOpen = args[22] as Boolean
        val editingN = args[23] as Note?
        val categories = args[24] as List<NoteCategory>
        val isManageCategoriesOpen = args[25] as Boolean
        val apptCategories = args[26] as List<AppointmentCategory>
        val manageInitialTab = args[27] as Int
        val isNbDetailOpen = args[28] as Boolean
        val selectedNbDetail = args[29] as Note?

        // Calculate selected day bounds
        val selCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val dayStart = (selCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = (selCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val dayAppointments = allAppts.filter { appt ->
            appt.startEpochMillis < dayEnd && appt.endEpochMillis >= dayStart
        }.sortedBy { it.startEpochMillis }

        // Filter all appointments based on owner and search
        val filtered = allAppts.filter { appt ->
            val matchesFilter = when (ownerFilter) {
                OwnerFilter.ALL -> true
                OwnerFilter.TOGETHER -> appt.ownerType == OwnerType.TOGETHER
                OwnerFilter.ME -> appt.ownerType == OwnerType.ME
                OwnerFilter.PARTNER -> appt.ownerType == OwnerType.PARTNER
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                appt.title.contains(searchQuery, ignoreCase = true) ||
                        appt.description.contains(searchQuery, ignoreCase = true) ||
                        appt.location.contains(searchQuery, ignoreCase = true) ||
                        appt.category.displayName.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }

        CalendarUiState(
            currentMainTab = mainTab,
            selectedDateMillis = selectedDate,
            displayMonthMillis = displayMonth,
            viewMode = viewMode,
            ownerFilter = ownerFilter,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            syncState = syncState,
            syncMessage = syncMsg,
            allAppointments = allAppts,
            filteredAppointments = filtered,
            selectedDayAppointments = dayAppointments,
            coupleProfile = profile,
            isAddDialogOpen = isAddOpen,
            editingAppointment = editingAppt,
            isPairingDialogOpen = isPairingOpen,
            isDetailDialogOpen = isDetailOpen,
            selectedDetailAppointment = selectedDetail,
            isMonthYearPickerOpen = isMonthYearOpen,
            newCreatedAlert = newAlert,
            notes = allNotes,
            noteSearchQuery = nbSearch,
            selectedNoteCategory = nbCategory,
            selectedNoteOwnerType = nbOwner,
            isAddEditNoteDialogOpen = isNbAddOpen,
            editingNote = editingN,
            noteCategories = categories,
            appointmentCategories = apptCategories,
            isManageCategoriesDialogOpen = isManageCategoriesOpen,
            manageCategoriesInitialTab = manageInitialTab,
            isNoteDetailDialogOpen = isNbDetailOpen,
            selectedDetailNote = selectedNbDetail
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    init {
        viewModelScope.launch {
            // Clean slate: remove placeholder appointments and notes per user request
            val profile = couplePreferences.coupleProfile.value
            repository.purgePlaceholderAppointments()
            noteRepository.purgePlaceholderNotes()

            // Auto-sync on startup if paired
            if (profile.isPaired && profile.coupleCode.isNotBlank()) {
                syncWithPartner()
                syncNotes()
            }

            // Reschedule all future appointment reminders
            try {
                val allActive = repository.getAllActiveAppointments()
                reminderScheduler.rescheduleAllReminders(allActive)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Reactively start or update auto-sync loop whenever interval setting changes
        viewModelScope.launch {
            couplePreferences.coupleProfile
                .map { it.autoSyncIntervalMinutes }
                .distinctUntilChanged()
                .collect { interval ->
                    startAutoSyncLoop(interval)
                }
        }

        // Periodically check if partner entered our couple code while unpaired
        viewModelScope.launch {
            while (isActive) {
                val profile = couplePreferences.coupleProfile.value
                if (!profile.isPaired && profile.coupleCode.isNotBlank() && _pendingLinkRequest.value == null) {
                    try {
                        val req = repository.checkPendingJoinRequest(profile.coupleCode, couplePreferences.getDeviceId())
                        if (req != null && !_dismissedPartnerDeviceIds.contains(req.partnerDeviceId)) {
                            _pendingLinkRequest.value = req
                        }
                    } catch (e: Exception) {
                        // Ignore periodic network errors
                    }
                }
                delay(4000L)
            }
        }
    }

    private fun startAutoSyncLoop(intervalMinutes: Int) {
        autoSyncJob?.cancel()
        if (intervalMinutes <= 0) return

        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMinutes * 60 * 1000L)
                val currentProfile = couplePreferences.coupleProfile.value
                if (currentProfile.isPaired && currentProfile.coupleCode.isNotBlank()) {
                    syncWithPartner()
                    syncNotes()
                }
            }
        }
    }

    fun updateAutoSyncInterval(intervalMinutes: Int) {
        couplePreferences.updateProfile(autoSyncIntervalMinutes = intervalMinutes)
    }

    fun acceptPartnerLink(request: PartnerLinkRequest) {
        viewModelScope.launch {
            repository.confirmJoinAccepted(request.coupleCode, request.partnerDeviceId)
            val pName = if (request.partnerName.isNotBlank() && request.partnerName != "Partner") {
                request.partnerName
            } else {
                couplePreferences.coupleProfile.value.partnerName
            }
            couplePreferences.updateProfile(
                isPaired = true,
                partnerName = pName
            )
            _pendingLinkRequest.value = null
            syncWithPartner()
            syncNotes()
            val isDe = couplePreferences.coupleProfile.value.appLanguage == "DE"
            _newCreatedAlert.value = if (isDe) "Erfolgreich mit $pName verbunden! 💕" else "Successfully linked with $pName! 💕"
        }
    }

    fun dismissPartnerLink(request: PartnerLinkRequest) {
        _dismissedPartnerDeviceIds.add(request.partnerDeviceId)
        _pendingLinkRequest.value = null
    }

    fun switchMainTab(tab: MainNavigationTab) {
        _currentMainTab.value = tab
    }

    fun openAddNoteDialog() {
        _selectedDetailNote.value = null
        _isNoteDetailDialogOpen.value = false
        _editingNote.value = null
        _isAddEditNoteDialogOpen.value = true
    }

    fun openNoteDetailDialog(note: Note) {
        _selectedDetailNote.value = note
        _isNoteDetailDialogOpen.value = true
    }

    fun closeNoteDetailDialog() {
        _isNoteDetailDialogOpen.value = false
        _selectedDetailNote.value = null
    }

    fun openEditNoteDialog(note: Note) {
        _isNoteDetailDialogOpen.value = false
        _selectedDetailNote.value = null
        _editingNote.value = note
        _isAddEditNoteDialogOpen.value = true
    }

    fun closeNoteDialog() {
        _isAddEditNoteDialogOpen.value = false
        _editingNote.value = null
    }

    fun saveNote(note: Note) {
        // Immediately dismiss the window as requested to eliminate latency and duplicate taps
        _isAddEditNoteDialogOpen.value = false
        _editingNote.value = null
        _newCreatedAlert.value = "Note saved 💕"

        viewModelScope.launch {
            noteRepository.saveNote(note)
        }
    }

    fun deleteNote(id: String) {
        _isNoteDetailDialogOpen.value = false
        _selectedDetailNote.value = null
        if (_editingNote.value?.id == id) {
            closeNoteDialog()
        }

        viewModelScope.launch {
            noteRepository.deleteNote(id)
        }
    }

    fun togglePinNote(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            noteRepository.togglePin(id, isPinned)
        }
    }

    fun toggleChecklistItem(noteId: String, itemIndex: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val note = noteRepository.getNoteById(noteId) ?: return@launch
            if (itemIndex in note.checklistItems.indices) {
                val updatedItems = note.checklistItems.toMutableList().apply {
                    this[itemIndex] = this[itemIndex].copy(isChecked = isChecked)
                }
                noteRepository.saveNote(note.copy(checklistItems = updatedItems), autoSync = true)
            }
        }
    }

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun setNoteCategoryFilter(category: NoteCategory?) {
        _selectedNoteCategory.value = category
    }

    fun setNoteOwnerTypeFilter(ownerType: OwnerType?) {
        _selectedNoteOwnerType.value = ownerType
    }

    fun shareNote(note: Note) {
        noteRepository.shareNote(note)
    }

    fun simulatePartnerNote() {
        viewModelScope.launch {
            val profile = couplePreferences.coupleProfile.value
            noteRepository.simulatePartnerNote(profile.partnerName, profile.partnerColorHex)
            _newCreatedAlert.value = "💌 ${profile.partnerName} added a surprise note!"
        }
    }

    fun syncNotes() {
        viewModelScope.launch {
            val profile = couplePreferences.coupleProfile.value
            if (profile.coupleCode.isNotBlank()) {
                val res = noteRepository.syncNotesWithPartner(profile.coupleCode)
                if (res.isSuccess) {
                    val count = res.getOrDefault(0)
                    if (count > 0) {
                        _newCreatedAlert.value = "Synced $count notes with ${profile.partnerName} 💕"
                    }
                }
            }
        }
    }

    fun selectDate(millis: Long) {
        _selectedDateMillis.value = millis
        _displayMonthMillis.value = millis
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply { timeInMillis = _displayMonthMillis.value }
        cal.add(Calendar.MONTH, 1)
        _displayMonthMillis.value = cal.timeInMillis
    }

    fun prevMonth() {
        val cal = Calendar.getInstance().apply { timeInMillis = _displayMonthMillis.value }
        cal.add(Calendar.MONTH, -1)
        _displayMonthMillis.value = cal.timeInMillis
    }

    fun openMonthYearPicker() {
        _isMonthYearPickerOpen.value = true
    }

    fun closeMonthYearPicker() {
        _isMonthYearPickerOpen.value = false
    }

    fun setMonthAndYear(year: Int, month: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _displayMonthMillis.value
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        _displayMonthMillis.value = cal.timeInMillis

        // Also update selected date to same year & month
        val selCal = Calendar.getInstance().apply {
            timeInMillis = _selectedDateMillis.value
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            // Clamp day if necessary (e.g. Feb 31 -> Feb 28)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = get(Calendar.DAY_OF_MONTH)
            if (currentDay > maxDay) {
                set(Calendar.DAY_OF_MONTH, maxDay)
            }
        }
        _selectedDateMillis.value = selCal.timeInMillis
        _isMonthYearPickerOpen.value = false
    }

    fun goToToday() {
        val now = System.currentTimeMillis()
        _selectedDateMillis.value = now
        _displayMonthMillis.value = now
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun setOwnerFilter(filter: OwnerFilter) {
        _ownerFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun openAddDialog(presetDateMillis: Long? = null) {
        if (presetDateMillis != null) {
            _selectedDateMillis.value = presetDateMillis
        }
        _editingAppointment.value = null
        _isAddDialogOpen.value = true
    }

    fun openEditDialog(appointment: Appointment) {
        _editingAppointment.value = appointment
        _isAddDialogOpen.value = true
    }

    fun closeAddDialog() {
        _isAddDialogOpen.value = false
        _editingAppointment.value = null
    }

    fun openDetailDialog(appointment: Appointment) {
        _selectedDetailAppointment.value = appointment
        _isDetailDialogOpen.value = true
    }

    fun openAppointmentById(id: String) {
        viewModelScope.launch {
            val appt = repository.getAppointmentById(id)
            if (appt != null) {
                _selectedDateMillis.value = appt.startEpochMillis
                _displayMonthMillis.value = appt.startEpochMillis
                _selectedDetailAppointment.value = appt
                _isDetailDialogOpen.value = true
            }
        }
    }

    fun closeDetailDialog() {
        _isDetailDialogOpen.value = false
        _selectedDetailAppointment.value = null
    }

    fun openPairingDialog() {
        _isPairingDialogOpen.value = true
    }

    fun closePairingDialog() {
        _isPairingDialogOpen.value = false
    }

    fun saveAppointment(appointment: Appointment) {
        // Immediately dismiss the window as requested to eliminate latency and duplicate taps
        _isAddDialogOpen.value = false
        _editingAppointment.value = null
        _newCreatedAlert.value = "Saved \"${appointment.title}\""

        viewModelScope.launch {
            val coupleCode = couplePreferences.coupleProfile.value.coupleCode
            val toSave = appointment.copy(coupleId = coupleCode)
            repository.saveAppointment(toSave)
            // Schedule high-priority alarm notification for appointment
            reminderScheduler.scheduleReminder(toSave)
        }
    }

    fun deleteAppointment(id: String) {
        // Immediately dismiss detail dialog
        _isDetailDialogOpen.value = false
        _selectedDetailAppointment.value = null
        _newCreatedAlert.value = "Appointment removed"

        viewModelScope.launch {
            repository.deleteAppointment(id)
            reminderScheduler.cancelReminder(id)
        }
    }

    fun clearAlert() {
        _newCreatedAlert.value = null
    }

    fun syncWithPartner() {
        viewModelScope.launch {
            val code = couplePreferences.coupleProfile.value.coupleCode
            if (code.isBlank()) return@launch

            _syncState.value = SyncState.SYNCING
            _syncMessage.value = "Syncing with partner..."

            val apptResult = repository.syncWithPartner(code)
            val noteResult = noteRepository.syncNotesWithPartner(code)

            if (apptResult.isSuccess || noteResult.isSuccess) {
                val count = apptResult.getOrDefault(0) + noteResult.getOrDefault(0)
                _syncState.value = SyncState.SUCCESS
                _syncMessage.value = if (count > 0) "Synced! ($count new updates)" else "Up to date with partner 💕"
                try {
                    val allActive = repository.getAllActiveAppointments()
                    reminderScheduler.rescheduleAllReminders(allActive)
                } catch (e: Exception) {
                    // Non-blocking
                }
            } else {
                _syncState.value = SyncState.OFFLINE
                _syncMessage.value = "Offline (Local calendar saved)"
            }
        }
    }

    fun updateProfile(
        myName: String,
        partnerName: String,
        myColorHex: String,
        partnerColorHex: String,
        togetherColorHex: String
    ) {
        couplePreferences.updateProfile(
            myName = myName,
            partnerName = partnerName,
            myColorHex = myColorHex,
            partnerColorHex = partnerColorHex,
            togetherColorHex = togetherColorHex
        )
    }

    fun updateAppSettings(
        appThemeMode: String,
        widgetThemeMode: String,
        appLanguage: String
    ) {
        couplePreferences.updateProfile(
            appThemeMode = appThemeMode,
            widgetThemeMode = widgetThemeMode,
            appLanguage = appLanguage
        )
        viewModelScope.launch {
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun joinCoupleCode(code: String, partnerName: String) {
        viewModelScope.launch {
            val cleanCode = code.uppercase().trim()
            val currentProfile = couplePreferences.coupleProfile.value
            val myName = if (currentProfile.myName.isNotBlank() && currentProfile.myName != "You") {
                currentProfile.myName
            } else if (!currentProfile.googleAccountName.isNullOrBlank()) {
                currentProfile.googleAccountName
            } else {
                "Partner"
            }
            val myDeviceId = couplePreferences.getDeviceId()

            couplePreferences.joinCoupleCode(cleanCode, partnerName)
            // Announce join request so original owner gets instant popup
            repository.announceJoinRequest(cleanCode, myName, myDeviceId)
            syncWithPartner()
            syncNotes()
        }
    }

    fun disconnectPairing() {
        couplePreferences.disconnectPairing()
    }

    fun regenerateCoupleCode() {
        viewModelScope.launch {
            val newCode = repository.generateNewVerifiedCoupleCode()
            _newCreatedAlert.value = "Neuer Paar-Code: $newCode"
        }
    }

    fun unlinkPartner(keepOwnEvents: Boolean) {
        viewModelScope.launch {
            repository.unlinkPartner(keepOwnEvents)
            noteRepository.deletePartnerNotes()
            _newCreatedAlert.value = "Partner unlinked successfully. Ready for a new couple code!"
        }
    }

    fun linkGoogleAccount(email: String, displayName: String, photoUrl: String? = null) {
        couplePreferences.setGoogleAccount(email, displayName, photoUrl)
        viewModelScope.launch {
            // Check if there is an existing backup to restore
            val restoredCount = repository.restoreFromGoogleAccount(email).getOrDefault(0)
            if (restoredCount > 0) {
                _newCreatedAlert.value = "Welcome back! Restored $restoredCount appointments from your Google Account."
            } else {
                syncWithPartner()
            }
        }
    }

    fun disconnectGoogleAccount() {
        couplePreferences.disconnectGoogleAccount()
    }

    private val googleDriveService = GoogleDriveSyncService(getApplication())
    private val _isDriveSyncing = MutableStateFlow(false)
    val isDriveSyncing: StateFlow<Boolean> = _isDriveSyncing.asStateFlow()

    private val _driveStatusMessage = MutableStateFlow<String?>(null)
    val driveStatusMessage: StateFlow<String?> = _driveStatusMessage.asStateFlow()

    fun clearDriveStatusMessage() {
        _driveStatusMessage.value = null
    }

    fun backupToGoogleDrive(activity: Activity? = null, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            try {
                val profile = couplePreferences.coupleProfile.value
                val email = profile.googleAccountEmail ?: ""
                val appointments = repository.getAllAppointmentsForBackup()
                val notes = noteRepository.getAllNotesForBackup()
                val appCats = couplePreferences.appointmentCategories.value
                val noteCats = couplePreferences.noteCategories.value
                val payload = googleDriveService.buildPayload(profile, appointments, notes, appCats, noteCats)

                val token = googleDriveService.getDriveAuthToken(email, activity)
                if (token != null) {
                    val result = googleDriveService.uploadToGoogleDrive(token, payload)
                    if (result.isSuccess) {
                        couplePreferences.setLastDriveSyncMillis(System.currentTimeMillis())
                        val msg = "TwoGether_Calendar_Backup.json in Google Drive gesichert (${appointments.size} Termine, ${notes.size} Notizen)"
                        _driveStatusMessage.value = msg
                        _newCreatedAlert.value = msg
                        onResult(true, msg)
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Drive Upload fehlgeschlagen"
                        _driveStatusMessage.value = err
                        onResult(false, err)
                    }
                } else {
                    val msg = "Direktes Google Drive Token benötigt Konto-Freigabe oder SAF-Export"
                    _driveStatusMessage.value = msg
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                val err = e.message ?: "Unbekannter Fehler"
                _driveStatusMessage.value = err
                onResult(false, err)
            } finally {
                _isDriveSyncing.value = false
            }
        }
    }

    fun restoreFromGoogleDrive(activity: Activity? = null, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            try {
                val profile = couplePreferences.coupleProfile.value
                val email = profile.googleAccountEmail ?: ""
                val token = googleDriveService.getDriveAuthToken(email, activity)
                if (token != null) {
                    val result = googleDriveService.downloadFromGoogleDrive(token)
                    if (result.isSuccess) {
                        val payload = result.getOrNull()!!
                        applyRestoredDrivePayload(payload)
                        val msg = "${payload.appointments.size} Termine & ${payload.notes.size} Notizen aus Google Drive geladen!"
                        _driveStatusMessage.value = msg
                        _newCreatedAlert.value = msg
                        onResult(true, msg)
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Wiederherstellung fehlgeschlagen"
                        _driveStatusMessage.value = err
                        onResult(false, err)
                    }
                } else {
                    onResult(false, "Kein Google Drive Token verfügbar")
                }
            } catch (e: Exception) {
                val err = e.message ?: "Wiederherstellungsfehler"
                _driveStatusMessage.value = err
                onResult(false, err)
            } finally {
                _isDriveSyncing.value = false
            }
        }
    }

    fun exportToGoogleDriveSaf(uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            try {
                val profile = couplePreferences.coupleProfile.value
                val appointments = repository.getAllAppointmentsForBackup()
                val notes = noteRepository.getAllNotesForBackup()
                val appCats = couplePreferences.appointmentCategories.value
                val noteCats = couplePreferences.noteCategories.value
                val payload = googleDriveService.buildPayload(profile, appointments, notes, appCats, noteCats)
                val res = googleDriveService.exportToSafUri(uri, payload)
                if (res.isSuccess) {
                    couplePreferences.setLastDriveSyncMillis(System.currentTimeMillis())
                    val msg = "Backup-Datei erfolgreich in Google Drive / Speicher abgelegt!"
                    _driveStatusMessage.value = msg
                    _newCreatedAlert.value = msg
                    onResult(true, msg)
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Export fehlgeschlagen"
                    onResult(false, err)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Export-Fehler")
            } finally {
                _isDriveSyncing.value = false
            }
        }
    }

    fun importFromGoogleDriveSaf(uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isDriveSyncing.value = true
            try {
                val res = googleDriveService.importFromSafUri(uri)
                if (res.isSuccess) {
                    val payload = res.getOrNull()!!
                    applyRestoredDrivePayload(payload)
                    val msg = "${payload.appointments.size} Termine & ${payload.notes.size} Notizen importiert!"
                    _driveStatusMessage.value = msg
                    _newCreatedAlert.value = msg
                    onResult(true, msg)
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Import fehlgeschlagen"
                    onResult(false, err)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Import-Fehler")
            } finally {
                _isDriveSyncing.value = false
            }
        }
    }

    private suspend fun applyRestoredDrivePayload(payload: GoogleDriveBackupPayload) {
        val appCats = payload.appointmentCategories.map { it.toDomain() }
        if (appCats.isNotEmpty()) {
            couplePreferences.mergeRemoteAppointmentCategories(appCats)
        }
        val noteCats = payload.noteCategories.map { it.toDomain() }
        if (noteCats.isNotEmpty()) {
            couplePreferences.mergeRemoteNoteCategories(noteCats)
        }
        val appointments = payload.appointments.map { it.toDomain() }
        repository.restoreAppointmentsFromList(appointments)

        val notes = payload.notes.map { it.toDomain() }
        noteRepository.restoreNotesFromList(notes)

        couplePreferences.setLastDriveSyncMillis(System.currentTimeMillis())
        syncWithPartner()
    }

    fun restoreFromGoogleCloud() {
        val email = couplePreferences.coupleProfile.value.googleAccountEmail
        if (email.isNullOrBlank()) return
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            val count = repository.restoreFromGoogleAccount(email).getOrDefault(0)
            _syncState.value = SyncState.SUCCESS
            _newCreatedAlert.value = "Restored $count appointments from Google Cloud."
        }
    }

    fun simulatePartnerActivity() {
        viewModelScope.launch {
            val profile = couplePreferences.coupleProfile.value
            val simulated = repository.simulatePartnerAction(profile.partnerName, profile.coupleCode)
            _newCreatedAlert.value = "${profile.partnerName} added \"${simulated.title}\"!"
            syncWithPartner()
        }
    }

    fun openManageCategoriesDialog(initialTab: Int = 0) {
        _manageCategoriesInitialTab.value = initialTab.coerceIn(0, 1)
        _isManageCategoriesDialogOpen.value = true
    }

    fun closeManageCategoriesDialog() {
        _isManageCategoriesDialogOpen.value = false
    }

    fun addAppointmentCategory(category: AppointmentCategory) {
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }

    fun updateAppointmentCategory(category: AppointmentCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteAppointmentCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun addNoteCategory(category: NoteCategory) {
        viewModelScope.launch {
            noteRepository.addCategory(category)
        }
    }

    fun updateNoteCategory(category: NoteCategory) {
        viewModelScope.launch {
            noteRepository.updateCategory(category)
        }
    }

    fun deleteNoteCategory(categoryId: String) {
        viewModelScope.launch {
            if (_selectedNoteCategory.value?.id == categoryId) {
                _selectedNoteCategory.value = null
            }
            noteRepository.deleteCategory(categoryId)
        }
    }
}
