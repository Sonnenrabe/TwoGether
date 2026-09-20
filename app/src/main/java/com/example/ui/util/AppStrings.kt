package com.example.ui.util

import com.example.data.model.AppointmentCategory
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType

object AppStrings {
    enum class SupportedLanguage(val code: String, val displayName: String, val flag: String) {
        SYSTEM("SYSTEM", "System Default", "🌐"),
        EN("EN", "English", "🇬🇧"),
        DE("DE", "Deutsch", "🇩🇪"),
        IT("IT", "Italiano", "🇮🇹"),
        ES("ES", "Español", "🇪🇸"),
        FR("FR", "Français", "🇫🇷");

        companion object {
            fun fromCode(code: String): SupportedLanguage {
                return entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
            }
        }
    }

    private fun resolveLangCode(lang: String): String {
        return when (lang.uppercase()) {
            "EN" -> "EN"
            "DE" -> "DE"
            "IT" -> "IT"
            "ES" -> "ES"
            "FR" -> "FR"
            else -> {
                val sysLang = java.util.Locale.getDefault().language.lowercase()
                when (sysLang) {
                    "de" -> "DE"
                    "it" -> "IT"
                    "es" -> "ES"
                    "fr" -> "FR"
                    else -> "EN"
                }
            }
        }
    }

    fun get(lang: String, key: String, vararg args: Any): String {
        val resolved = resolveLangCode(lang)
        val map = when (resolved) {
            "DE" -> deStrings
            "IT" -> itStrings
            "ES" -> esStrings
            "FR" -> frStrings
            else -> enStrings
        }
        val text = map[key] ?: enStrings[key] ?: key
        return if (args.isNotEmpty()) {
            try { String.format(text, *args) } catch (e: Exception) { text }
        } else {
            text
        }
    }

    fun getCategoryName(category: AppointmentCategory, lang: String): String {
        if (category.isCustom) return category.displayName
        val defaultEnglish = AppointmentCategory.DEFAULT_CATEGORIES.firstOrNull { it.id == category.id }?.displayName
        if (defaultEnglish != null && category.displayName != defaultEnglish) {
            return category.displayName
        }
        val resolved = resolveLangCode(lang)
        return when (resolved) {
            "DE" -> when (category.id) {
                "DATE_NIGHT" -> "Date Night"
                "TOGETHER_PLAN" -> "Gemeinsam"
                "WORK" -> "Arbeit & Beruf"
                "HEALTH" -> "Gesundheit & Arzt"
                "TRAVEL" -> "Reise & Ausflug"
                "HOME_CHORE" -> "Haushalt & Erledigung"
                "CELEBRATION" -> "Feier & Geburtstag"
                "PERSONAL" -> "Persönlich"
                "OTHER" -> "Sonstiges"
                else -> category.displayName
            }
            "IT" -> when (category.id) {
                "DATE_NIGHT" -> "Serata Romantica"
                "TOGETHER_PLAN" -> "Insieme"
                "WORK" -> "Lavoro & Ufficio"
                "HEALTH" -> "Salute & Medico"
                "TRAVEL" -> "Viaggi & Vacanze"
                "HOME_CHORE" -> "Casa & Faccende"
                "CELEBRATION" -> "Feste & Compleanni"
                "PERSONAL" -> "Personale"
                "OTHER" -> "Altro"
                else -> category.displayName
            }
            "ES" -> when (category.id) {
                "DATE_NIGHT" -> "Noche de Cita"
                "TOGETHER_PLAN" -> "Juntos"
                "WORK" -> "Trabajo & Negocios"
                "HEALTH" -> "Salud & Médico"
                "TRAVEL" -> "Viajes & Paseos"
                "HOME_CHORE" -> "Hogar & Tareas"
                "CELEBRATION" -> "Fiestas & Cumpleaños"
                "PERSONAL" -> "Personal"
                "OTHER" -> "Otro"
                else -> category.displayName
            }
            "FR" -> when (category.id) {
                "DATE_NIGHT" -> "Soirée en amoureux"
                "TOGETHER_PLAN" -> "Ensemble"
                "WORK" -> "Travail & Pro"
                "HEALTH" -> "Santé & Médecin"
                "TRAVEL" -> "Voyage & Sortie"
                "HOME_CHORE" -> "Maison & Courses"
                "CELEBRATION" -> "Fête & Anniversaire"
                "PERSONAL" -> "Personnel"
                "OTHER" -> "Autre"
                else -> category.displayName
            }
            else -> when (category.id) {
                "DATE_NIGHT" -> "Date Night"
                "TOGETHER_PLAN" -> "Together"
                "WORK" -> "Work"
                "HEALTH" -> "Health & Doctor"
                "TRAVEL" -> "Travel & Trip"
                "HOME_CHORE" -> "Home & Errands"
                "CELEBRATION" -> "Celebration"
                "PERSONAL" -> "Personal"
                "OTHER" -> "Other"
                else -> category.displayName
            }
        }
    }

    fun getNoteCategoryName(category: NoteCategory, lang: String): String {
        if (category.isCustom) return category.displayName
        val defaultEnglish = NoteCategory.DEFAULT_CATEGORIES.firstOrNull { it.id == category.id }?.displayName
        if (defaultEnglish != null && category.displayName != defaultEnglish) {
            return category.displayName
        }
        val t = { key: String -> get(lang, key) }
        return when (category.id) {
            NoteCategory.GENERAL.id -> t("filter_cat_general")
            NoteCategory.DATE_IDEAS.id -> t("filter_cat_date_ideas")
            NoteCategory.SHOPPING.id -> t("filter_cat_shopping")
            NoteCategory.LOVE_NOTE.id -> t("filter_cat_love_notes")
            NoteCategory.TODO.id -> t("filter_cat_todo")
            NoteCategory.TRAVEL.id -> t("filter_cat_travel")
            else -> category.displayName
        }
    }

    fun getOwnerTypeName(ownerType: OwnerType, lang: String, myName: String = "You", partnerName: String = "Partner"): String {
        val resolved = resolveLangCode(lang)
        return when (resolved) {
            "DE" -> when (ownerType) {
                OwnerType.TOGETHER -> "Gemeinsam"
                OwnerType.ME -> "Mein Termin ($myName)"
                OwnerType.PARTNER -> "Termin von $partnerName"
            }
            "IT" -> when (ownerType) {
                OwnerType.TOGETHER -> "Insieme"
                OwnerType.ME -> "Mio ($myName)"
                OwnerType.PARTNER -> "Di $partnerName"
            }
            "ES" -> when (ownerType) {
                OwnerType.TOGETHER -> "Juntos"
                OwnerType.ME -> "Mío ($myName)"
                OwnerType.PARTNER -> "De $partnerName"
            }
            "FR" -> when (ownerType) {
                OwnerType.TOGETHER -> "Ensemble"
                OwnerType.ME -> "Moi ($myName)"
                OwnerType.PARTNER -> "De $partnerName"
            }
            else -> when (ownerType) {
                OwnerType.TOGETHER -> "Together"
                OwnerType.ME -> "My Appointment ($myName)"
                OwnerType.PARTNER -> "$partnerName's Appointment"
            }
        }
    }

    private val enStrings = mapOf(
        "app_title" to "TwoGether",
        "tab_month" to "Month",
        "tab_week" to "Week",
        "tab_day" to "Day",
        "tab_agenda" to "List",
        "filter_all" to "All",
        "filter_together" to "Together",
        "filter_mine" to "Mine",
        "filter_partner" to "Partner",
        "add_appointment" to "New Appointment",
        "edit_appointment" to "Edit Appointment",
        "settings" to "Settings & Sync",
        "pairing_tab" to "Pairing",
        "cloud_tab" to "Google Cloud",
        "profiles_tab" to "Colors & Names",
        "appearance_tab" to "Design",
        "language_tab" to "Language",
        "app_theme" to "App Theme",
        "widget_theme" to "Widget Theme",
        "app_language" to "App Language",
        "theme_system" to "System Default",
        "theme_light" to "Light Mode ☀️",
        "theme_dark" to "Dark Mode 🌙",
        "theme_system_desc" to "Follow system appearance",
        "theme_light_desc" to "Always bright & clean",
        "theme_dark_desc" to "Easy on the eyes at night",
        "lang_system" to "System Default",
        "lang_en" to "English",
        "lang_de" to "Deutsch",
        "lang_it" to "Italiano",
        "lang_es" to "Español",
        "lang_fr" to "Français",
        "save" to "Save",
        "cancel" to "Cancel",
        "delete" to "Delete",
        "today" to "Today",
        "jump_to_date" to "Jump to Date",
        "go_to_month" to "Go to Month",
        "current_month" to "Current Month",
        "no_plans" to "No appointments for this day",
        "plans_count" to "%d plans",
        "synced" to "Synced",
        "syncing" to "Syncing...",
        "offline" to "Offline Mode",
        "couple_code" to "Your Couple Code",
        "copy_code" to "Copy Code",
        "share_code" to "Share",
        "join_couple" to "Enter Partner's Code",
        "connect_partner" to "Connect to Partner",
        "disconnect" to "Disconnect",
        "unlink_partner" to "Unlink Partner / New Partner",
        "title_event_name" to "Title / Event Name *",
        "title_placeholder" to "e.g., Romantic Dinner, Movie Night",
        "who_is_this_for" to "Who is this for?",
        "category" to "Category",
        "date" to "Date",
        "all_day_event" to "All-day event",
        "starts" to "Starts",
        "ends" to "Ends",
        "location_optional" to "Location (optional)",
        "location_placeholder" to "e.g., Central Park / Online",
        "notes_optional" to "Notes / Details (optional)",
        "notes_placeholder" to "e.g., Dress code, reservation number...",
        "reminder_notification" to "Reminder Notification",
        "mins_before_15" to "15m before",
        "mins_before_30" to "30m before",
        "hour_before_1" to "1 hour before",
        "day_before_1" to "1 day before",
        "save_plan" to "Save Plan",
        "edit_plan" to "Edit Plan",
        "all_day" to "All day",
        "reminder_mins_before" to "Reminder: %d minutes before",
        "notes" to "Notes",
        "schedule_of" to "%s's Schedule",
        "together_schedule" to "Together (%s & %s)",
        "search_placeholder" to "Search appointments, dates, places...",
        "search_clear" to "Clear",
        "tap_to_pair" to "Tap to pair calendar 💕",
        "pairing_code_label" to "Sync Code",
        "no_appointments_day" to "No upcoming appointments",
        "empty_day_prompt" to "Tap + to add dates and plans for you and %s!",
        "together_color" to "Together Events Color",
        "nav_calendar" to "Calendar",
        "nav_notebook" to "Notebook",
        "notebook_title" to "Shared Notebook",
        "add_note" to "New Note",
        "edit_note" to "Edit Note",
        "note_title_label" to "Title *",
        "note_title_placeholder" to "e.g., Weekend plans, Wishlist, Love note...",
        "note_type_text" to "Text Note",
        "note_type_checklist" to "Checklist",
        "note_content_placeholder" to "Write something lovely or useful together...",
        "add_checklist_item" to "Add item...",
        "share_note" to "Share Note",
        "pin_note" to "Pin to top",
        "unpin_note" to "Unpin",
        "delete_note_confirm" to "Delete this note?",
        "empty_notes_title" to "No notes yet",
        "empty_notes_desc" to "Create shared lists, cute love notes, date ideas, or packing lists with %s!",
        "search_notes_placeholder" to "Search notes & lists...",
        "all_notes" to "All",
        "filter_cat_general" to "General",
        "filter_cat_date_ideas" to "Date Ideas",
        "filter_cat_shopping" to "Shopping & Groceries",
        "filter_cat_love_notes" to "Love Notes",
        "filter_cat_todo" to "To-Do Lists",
        "filter_cat_travel" to "Travel & Trip",
        "sync_notes" to "Sync Notes",
        "note_saved" to "Note saved 💕",
        "note_deleted" to "Note deleted",
        "simulate_partner_note" to "Partner Surprise Note 💌",
        "manage_categories" to "Manage Categories",
        "add_category" to "Add Category",
        "edit_category" to "Edit Category",
        "category_name" to "Category Name",
        "category_name_placeholder" to "e.g. Movies, Wishlist, Recipes...",
        "category_emoji" to "Icon / Emoji",
        "category_color" to "Category Color",
        "delete_category_confirm" to "Delete category \"%s\"? Notes will be moved to General.",
        "delete_appointment_category_confirm" to "Delete category \"%s\"? Existing appointments will be moved to Other.",
        "cannot_delete_last_category" to "At least one category is required.",
        "tab_appointments" to "Appointments",
        "tab_notes" to "Notes"
    )

    private val deStrings = mapOf(
        "app_title" to "TwoGether",
        "tab_month" to "Monat",
        "tab_week" to "Woche",
        "tab_day" to "Tag",
        "tab_agenda" to "Liste",
        "filter_all" to "Alle",
        "filter_together" to "Gemeinsam",
        "filter_mine" to "Meine",
        "filter_partner" to "Partner",
        "add_appointment" to "Neuer Termin",
        "edit_appointment" to "Termin bearbeiten",
        "settings" to "Einstellungen & Sync",
        "pairing_tab" to "Kopplung",
        "cloud_tab" to "Google Cloud",
        "profiles_tab" to "Farben & Namen",
        "appearance_tab" to "Design",
        "language_tab" to "Sprache",
        "app_theme" to "App-Erscheinungsbild",
        "widget_theme" to "Widget-Erscheinungsbild",
        "app_language" to "App-Sprache",
        "theme_system" to "Systemstandard",
        "theme_light" to "Heller Modus ☀️",
        "theme_dark" to "Dunkler Modus 🌙",
        "theme_system_desc" to "Folgt der Systemeinstellung",
        "theme_light_desc" to "Immer hell & freundlich",
        "theme_dark_desc" to "Augenschonend bei Nacht",
        "lang_system" to "Systemstandard",
        "lang_en" to "English",
        "lang_de" to "Deutsch",
        "lang_it" to "Italiano",
        "lang_es" to "Español",
        "lang_fr" to "Français",
        "save" to "Speichern",
        "cancel" to "Abbrechen",
        "delete" to "Löschen",
        "today" to "Heute",
        "jump_to_date" to "Zu Datum springen",
        "go_to_month" to "Monat öffnen",
        "current_month" to "Aktueller Monat",
        "no_plans" to "Keine Termine an diesem Tag",
        "plans_count" to "%d Termine",
        "synced" to "Synchronisiert",
        "syncing" to "Synchronisiere...",
        "offline" to "Offline-Modus",
        "couple_code" to "Euer Paar-Code",
        "copy_code" to "Code kopieren",
        "share_code" to "Teilen",
        "join_couple" to "Code des Partners eingeben",
        "connect_partner" to "Mit Partner verbinden",
        "disconnect" to "Trennen",
        "unlink_partner" to "Partner trennen / Neuer Partner",
        "title_event_name" to "Titel / Terminname *",
        "title_placeholder" to "z.B. Romantisches Abendessen, Kino",
        "who_is_this_for" to "Für wen ist dieser Termin?",
        "category" to "Kategorie",
        "date" to "Datum",
        "all_day_event" to "Ganztägiger Termin",
        "starts" to "Beginn",
        "ends" to "Ende",
        "location_optional" to "Ort (optional)",
        "location_placeholder" to "z.B. Restaurant, Kino, Zuhause",
        "notes_optional" to "Notizen / Details (optional)",
        "notes_placeholder" to "z.B. Reservierungsnummer, Kleidung...",
        "reminder_notification" to "Erinnerungsbenachrichtigung",
        "mins_before_15" to "15 Min vorher",
        "mins_before_30" to "30 Min vorher",
        "hour_before_1" to "1 Stunde vorher",
        "day_before_1" to "1 Tag vorher",
        "save_plan" to "Termin speichern",
        "edit_plan" to "Termin bearbeiten",
        "all_day" to "Ganztägig",
        "reminder_mins_before" to "Erinnerung: %d Minuten vorher",
        "notes" to "Notizen",
        "schedule_of" to "Termine von %s",
        "together_schedule" to "Gemeinsam (%s & %s)",
        "search_placeholder" to "Termine, Orte, Notizen durchsuchen...",
        "search_clear" to "Löschen",
        "tap_to_pair" to "Tippen zum Koppeln 💕",
        "pairing_code_label" to "Sync-Code",
        "no_appointments_day" to "Keine anstehenden Termine",
        "empty_day_prompt" to "Tippe auf +, um Termine für dich und %s hinzuzufügen!",
        "together_color" to "Farbe für gemeinsame Termine",
        "nav_calendar" to "Kalender",
        "nav_notebook" to "Notizbuch",
        "notebook_title" to "Gemeinsames Notizbuch",
        "add_note" to "Neue Notiz",
        "edit_note" to "Notiz bearbeiten",
        "note_title_label" to "Titel *",
        "note_title_placeholder" to "z.B. Packliste, Ausflugsziele, Liebesbotschaft...",
        "note_type_text" to "Textnotiz",
        "note_type_checklist" to "Checkliste",
        "note_content_placeholder" to "Schreibt etwas Schönes zusammen...",
        "add_checklist_item" to "Eintrag hinzufügen...",
        "share_note" to "Notiz teilen",
        "pin_note" to "Oben anheften",
        "unpin_note" to "Loslösen",
        "delete_note_confirm" to "Diese Notiz löschen?",
        "empty_notes_title" to "Noch keine Notizen",
        "empty_notes_desc" to "Erstelle gemeinsame Listen, Liebesbriefe, Date-Ideen oder Packlisten mit %s!",
        "search_notes_placeholder" to "Notizen & Listen durchsuchen...",
        "all_notes" to "Alle",
        "filter_cat_general" to "Allgemein",
        "filter_cat_date_ideas" to "Date-Ideen",
        "filter_cat_shopping" to "Einkauf & Besorgungen",
        "filter_cat_love_notes" to "Liebesnotizen",
        "filter_cat_todo" to "To-Do-Listen",
        "filter_cat_travel" to "Reise & Urlaub",
        "sync_notes" to "Notizen synchronisieren",
        "note_saved" to "Notiz gespeichert 💕",
        "note_deleted" to "Notiz gelöscht",
        "simulate_partner_note" to "Partner-Überraschungsnotiz 💌",
        "manage_categories" to "Kategorien verwalten",
        "add_category" to "Kategorie hinzufügen",
        "edit_category" to "Kategorie bearbeiten",
        "category_name" to "Kategoriename",
        "category_name_placeholder" to "z.B. Filme, Wunschliste, Rezepte...",
        "category_emoji" to "Icon / Emoji",
        "category_color" to "Kategoriefarbe",
        "delete_category_confirm" to "Kategorie \"%s\" löschen? Zugehörige Notizen werden auf 'Allgemein' gesetzt.",
        "delete_appointment_category_confirm" to "Kategorie \"%s\" löschen? Bestehende Termine werden auf 'Sonstiges' gesetzt.",
        "cannot_delete_last_category" to "Mindestens eine Kategorie muss erhalten bleiben.",
        "tab_appointments" to "Termine",
        "tab_notes" to "Notizen"
    )

    private val itStrings = mapOf(
        "app_title" to "TwoGether",
        "tab_month" to "Mese",
        "tab_week" to "Settimana",
        "tab_day" to "Giorno",
        "tab_agenda" to "Lista",
        "filter_all" to "Tutti",
        "filter_together" to "Insieme",
        "filter_mine" to "Miei",
        "filter_partner" to "Partner",
        "add_appointment" to "Nuovo Appuntamento",
        "edit_appointment" to "Modifica Appuntamento",
        "settings" to "Impostazioni & Sync",
        "pairing_tab" to "Accoppiamento",
        "cloud_tab" to "Google Cloud",
        "profiles_tab" to "Colori & Nomi",
        "appearance_tab" to "Design",
        "language_tab" to "Lingua",
        "app_theme" to "Tema dell'app",
        "widget_theme" to "Tema del widget",
        "app_language" to "Lingua dell'app",
        "theme_system" to "Predefinito di sistema",
        "theme_light" to "Modalità Chiara ☀️",
        "theme_dark" to "Modalità Scura 🌙",
        "theme_system_desc" to "Segue l'aspetto del sistema",
        "theme_light_desc" to "Sempre chiaro e luminoso",
        "theme_dark_desc" to "Rilassante per gli occhi di notte",
        "lang_system" to "Predefinito",
        "lang_en" to "English",
        "lang_de" to "Deutsch",
        "lang_it" to "Italiano",
        "lang_es" to "Español",
        "lang_fr" to "Français",
        "save" to "Salva",
        "cancel" to "Annulla",
        "delete" to "Elimina",
        "today" to "Oggi",
        "jump_to_date" to "Vai alla data",
        "go_to_month" to "Apri mese",
        "current_month" to "Mese corrente",
        "no_plans" to "Nessun appuntamento per questo giorno",
        "plans_count" to "%d impegni",
        "synced" to "Sincronizzato",
        "syncing" to "Sincronizzazione...",
        "offline" to "Modalità offline",
        "couple_code" to "Il vostro codice di coppia",
        "copy_code" to "Copia codice",
        "share_code" to "Condividi",
        "join_couple" to "Inserisci codice partner",
        "connect_partner" to "Connettiti al partner",
        "disconnect" to "Disconnetti",
        "unlink_partner" to "Scollega Partner / Nuovo Partner",
        "title_event_name" to "Titolo / Nome evento *",
        "title_placeholder" to "es. Cena a lume di candela, Cinema",
        "who_is_this_for" to "Per chi è questo appuntamento?",
        "category" to "Categoria",
        "date" to "Data",
        "all_day_event" to "Evento per l'intera giornata",
        "starts" to "Inizio",
        "ends" to "Fine",
        "location_optional" to "Luogo (facoltativo)",
        "location_placeholder" to "es. Ristorante, Casa, Online",
        "notes_optional" to "Note / Dettagli (facoltativo)",
        "notes_placeholder" to "es. Numero di prenotazione, abbigliamento...",
        "reminder_notification" to "Notifica promemoria",
        "mins_before_15" to "15 min prima",
        "mins_before_30" to "30 min prima",
        "hour_before_1" to "1 ora prima",
        "day_before_1" to "1 giorno prima",
        "save_plan" to "Salva appuntamento",
        "edit_plan" to "Modifica appuntamento",
        "all_day" to "Tutto il giorno",
        "reminder_mins_before" to "Promemoria: %d minuti prima",
        "notes" to "Note",
        "schedule_of" to "Appuntamenti di %s",
        "together_schedule" to "Insieme (%s & %s)",
        "search_placeholder" to "Cerca appuntamenti, luoghi, note...",
        "search_clear" to "Cancella",
        "tap_to_pair" to "Tocca per accoppiare 💕",
        "pairing_code_label" to "Codice Sync",
        "no_appointments_day" to "Nessun appuntamento in programma",
        "empty_day_prompt" to "Tocca + per aggiungere impegni per te e %s!",
        "together_color" to "Colore per eventi insieme",
        "nav_calendar" to "Calendario",
        "nav_notebook" to "Quaderno",
        "notebook_title" to "Quaderno Condiviso",
        "add_note" to "Nuova Nota",
        "edit_note" to "Modifica Nota",
        "note_title_label" to "Titolo *",
        "note_title_placeholder" to "es. Lista valigia, Idee appuntamento, Ti amo...",
        "note_type_text" to "Nota di testo",
        "note_type_checklist" to "Lista di controllo",
        "note_content_placeholder" to "Scrivete qualcosa di bello insieme...",
        "add_checklist_item" to "Aggiungi elemento...",
        "share_note" to "Condividi Nota",
        "pin_note" to "Fissa in alto",
        "unpin_note" to "Rimuovi fissaggio",
        "delete_note_confirm" to "Eliminare questa nota?",
        "empty_notes_title" to "Ancora nessuna nota",
        "empty_notes_desc" to "Crea liste condivise, messaggi dolci o idee di viaggio con %s!",
        "search_notes_placeholder" to "Cerca note e liste...",
        "all_notes" to "Tutte",
        "filter_cat_general" to "Generale",
        "filter_cat_date_ideas" to "Idee Appuntamenti",
        "filter_cat_shopping" to "Spesa & Acquisti",
        "filter_cat_love_notes" to "Note d'Amore",
        "filter_cat_todo" to "Cose da Fare",
        "filter_cat_travel" to "Viaggi & Vacanze",
        "sync_notes" to "Sincronizza Note",
        "note_saved" to "Nota salvata 💕",
        "note_deleted" to "Nota eliminata",
        "simulate_partner_note" to "Nota sorpresa del partner 💌",
        "manage_categories" to "Gestisci Categorie",
        "add_category" to "Aggiungi Categoria",
        "edit_category" to "Modifica Categoria",
        "category_name" to "Nome Categoria",
        "category_name_placeholder" to "es. Film, Ricette, Idee...",
        "category_emoji" to "Icona / Emoji",
        "category_color" to "Colore Categoria",
        "delete_category_confirm" to "Eliminare la categoria \"%s\"? Le note verranno spostate in Generale.",
        "delete_appointment_category_confirm" to "Eliminare la categoria \"%s\"? Gli appuntamenti saranno spostati in 'Altro'.",
        "cannot_delete_last_category" to "È richiesta almeno una categoria.",
        "tab_appointments" to "Appuntamenti",
        "tab_notes" to "Note"
    )

    private val esStrings = mapOf(
        "app_title" to "TwoGether",
        "tab_month" to "Mes",
        "tab_week" to "Semana",
        "tab_day" to "Día",
        "tab_agenda" to "Lista",
        "filter_all" to "Todos",
        "filter_together" to "Juntos",
        "filter_mine" to "Míos",
        "filter_partner" to "Pareja",
        "add_appointment" to "Nueva Cita",
        "edit_appointment" to "Editar Cita",
        "settings" to "Ajustes & Sync",
        "pairing_tab" to "Emparejamiento",
        "cloud_tab" to "Google Cloud",
        "profiles_tab" to "Colores & Nombres",
        "appearance_tab" to "Diseño",
        "language_tab" to "Idioma",
        "app_theme" to "Tema de la app",
        "widget_theme" to "Tema del widget",
        "app_language" to "Idioma de la app",
        "theme_system" to "Predeterminado del sistema",
        "theme_light" to "Modo Claro ☀️",
        "theme_dark" to "Modo Oscuro 🌙",
        "theme_system_desc" to "Sigue la apariencia del sistema",
        "theme_light_desc" to "Siempre claro y agradable",
        "theme_dark_desc" to "Suave para la vista de noche",
        "lang_system" to "Predeterminado",
        "lang_en" to "English",
        "lang_de" to "Deutsch",
        "lang_it" to "Italiano",
        "lang_es" to "Español",
        "lang_fr" to "Français",
        "save" to "Guardar",
        "cancel" to "Cancelar",
        "delete" to "Eliminar",
        "today" to "Hoy",
        "jump_to_date" to "Ir a la fecha",
        "go_to_month" to "Abrir mes",
        "current_month" to "Mes actual",
        "no_plans" to "No hay citas para este día",
        "plans_count" to "%d citas",
        "synced" to "Sincronizado",
        "syncing" to "Sincronizando...",
        "offline" to "Modo sin conexión",
        "couple_code" to "Vuestro código de pareja",
        "copy_code" to "Copiar código",
        "share_code" to "Compartir",
        "join_couple" to "Introducir código de pareja",
        "connect_partner" to "Conectar con pareja",
        "disconnect" to "Desconectar",
        "unlink_partner" to "Desvincular Pareja / Nueva Pareja",
        "title_event_name" to "Título / Nombre de cita *",
        "title_placeholder" to "ej., Cena romántica, Cine",
        "who_is_this_for" to "¿Para quién es esta cita?",
        "category" to "Categoría",
        "date" to "Fecha",
        "all_day_event" to "Evento de todo el día",
        "starts" to "Comienza",
        "ends" to "Termina",
        "location_optional" to "Ubicación (opcional)",
        "location_placeholder" to "ej., Restaurante, Casa, Online",
        "notes_optional" to "Notas / Detalles (opcional)",
        "notes_placeholder" to "ej., Código de vestimenta, reserva...",
        "reminder_notification" to "Notificación de recordatorio",
        "mins_before_15" to "15 min antes",
        "mins_before_30" to "30 min antes",
        "hour_before_1" to "1 hora antes",
        "day_before_1" to "1 día antes",
        "save_plan" to "Guardar cita",
        "edit_plan" to "Editar cita",
        "all_day" to "Todo el día",
        "reminder_mins_before" to "Recordatorio: %d minutos antes",
        "notes" to "Notas",
        "schedule_of" to "Citas de %s",
        "together_schedule" to "Juntos (%s & %s)",
        "search_placeholder" to "Buscar citas, lugares, notas...",
        "search_clear" to "Borrar",
        "tap_to_pair" to "Toca para vincular 💕",
        "pairing_code_label" to "Código Sync",
        "no_appointments_day" to "No hay citas próximas",
        "empty_day_prompt" to "¡Toca + para agregar planes para ti y %s!",
        "together_color" to "Color para eventos juntos",
        "nav_calendar" to "Calendario",
        "nav_notebook" to "Cuaderno",
        "notebook_title" to "Cuaderno Compartido",
        "add_note" to "Nueva Nota",
        "edit_note" to "Editar Nota",
        "note_title_label" to "Título *",
        "note_title_placeholder" to "ej. Lista de equipaje, Ideas para citas...",
        "note_type_text" to "Nota de texto",
        "note_type_checklist" to "Lista de tareas",
        "note_content_placeholder" to "Escriban algo lindo juntos...",
        "add_checklist_item" to "Añadir elemento...",
        "share_note" to "Compartir Nota",
        "pin_note" to "Fijar arriba",
        "unpin_note" to "Desfijar",
        "delete_note_confirm" to "¿Eliminar esta nota?",
        "empty_notes_title" to "Aún no hay notas",
        "empty_notes_desc" to "¡Crea listas compartidas, notas de amor o ideas de viaje con %s!",
        "search_notes_placeholder" to "Buscar notas y listas...",
        "all_notes" to "Todas",
        "filter_cat_general" to "General",
        "filter_cat_date_ideas" to "Ideas de Citas",
        "filter_cat_shopping" to "Compras y Supermercado",
        "filter_cat_love_notes" to "Notas de Amor",
        "filter_cat_todo" to "Listas de Tareas",
        "filter_cat_travel" to "Viajes y Vacaciones",
        "sync_notes" to "Sincronizar Notas",
        "note_saved" to "Nota guardada 💕",
        "note_deleted" to "Nota eliminada",
        "simulate_partner_note" to "Nota sorpresa de pareja 💌",
        "manage_categories" to "Administrar Categorías",
        "add_category" to "Agregar Categoría",
        "edit_category" to "Editar Categoría",
        "category_name" to "Nombre de Categoría",
        "category_name_placeholder" to "ej. Películas, Recetas, Deseos...",
        "category_emoji" to "Ícono / Emoji",
        "category_color" to "Color de Categoría",
        "delete_category_confirm" to "¿Eliminar la categoría \"%s\"? Las notas se moverán a General.",
        "delete_appointment_category_confirm" to "¿Eliminar la categoría \"%s\"? Las citas se moverán a 'Otro'.",
        "cannot_delete_last_category" to "Se requiere al menos una categoría.",
        "tab_appointments" to "Citas",
        "tab_notes" to "Notas"
    )

    private val frStrings = mapOf(
        "app_title" to "TwoGether",
        "tab_month" to "Mois",
        "tab_week" to "Semaine",
        "tab_day" to "Jour",
        "tab_agenda" to "Liste",
        "filter_all" to "Tous",
        "filter_together" to "Ensemble",
        "filter_mine" to "Moi",
        "filter_partner" to "Partenaire",
        "add_appointment" to "Nouveau rendez-vous",
        "edit_appointment" to "Modifier rendez-vous",
        "settings" to "Paramètres & Sync",
        "pairing_tab" to "Connexion",
        "cloud_tab" to "Google Cloud",
        "profiles_tab" to "Couleurs & Noms",
        "appearance_tab" to "Design",
        "language_tab" to "Langue",
        "app_theme" to "Thème de l'application",
        "widget_theme" to "Thème du widget",
        "app_language" to "Langue de l'application",
        "theme_system" to "Par défaut du système",
        "theme_light" to "Mode Clair ☀️",
        "theme_dark" to "Mode Sombre 🌙",
        "theme_system_desc" to "Suit les paramètres du système",
        "theme_light_desc" to "Toujours lumineux et clair",
        "theme_dark_desc" to "Agréable pour les yeux la nuit",
        "lang_system" to "Par défaut",
        "lang_en" to "English",
        "lang_de" to "Deutsch",
        "lang_it" to "Italiano",
        "lang_es" to "Español",
        "lang_fr" to "Français",
        "save" to "Enregistrer",
        "cancel" to "Annuler",
        "delete" to "Supprimer",
        "today" to "Aujourd'hui",
        "jump_to_date" to "Aller à la date",
        "go_to_month" to "Ouvrir le mois",
        "current_month" to "Mois actuel",
        "no_plans" to "Aucun rendez-vous ce jour-là",
        "plans_count" to "%d rendez-vous",
        "synced" to "Synchronisé",
        "syncing" to "Synchronisation...",
        "offline" to "Mode hors-ligne",
        "couple_code" to "Votre code de couple",
        "copy_code" to "Copier le code",
        "share_code" to "Partager",
        "join_couple" to "Entrer le code partenaire",
        "connect_partner" to "Se connecter au partenaire",
        "disconnect" to "Déconnecter",
        "unlink_partner" to "Dissocier le partenaire / Nouveau partenaire",
        "title_event_name" to "Titre / Nom du rendez-vous *",
        "title_placeholder" to "ex. Dîner aux chandelles, Cinéma",
        "who_is_this_for" to "Pour qui est ce rendez-vous ?",
        "category" to "Catégorie",
        "date" to "Date",
        "all_day_event" to "Événement toute la journée",
        "starts" to "Début",
        "ends" to "Fin",
        "location_optional" to "Lieu (facultatif)",
        "location_placeholder" to "ex. Restaurant, Maison, En ligne",
        "notes_optional" to "Notes / Détails (facultatif)",
        "notes_placeholder" to "ex. Code vestimentaire, réservation...",
        "reminder_notification" to "Notification de rappel",
        "mins_before_15" to "15 min avant",
        "mins_before_30" to "30 min avant",
        "hour_before_1" to "1 heure avant",
        "day_before_1" to "1 jour avant",
        "save_plan" to "Enregistrer",
        "edit_plan" to "Modifier",
        "all_day" to "Toute la journée",
        "reminder_mins_before" to "Rappel : %d minutes avant",
        "notes" to "Notes",
        "schedule_of" to "Rendez-vous de %s",
        "together_schedule" to "Ensemble (%s & %s)",
        "search_placeholder" to "Rechercher rendez-vous, lieux, notes...",
        "search_clear" to "Effacer",
        "tap_to_pair" to "Appuyez pour coupler 💕",
        "pairing_code_label" to "Code Sync",
        "no_appointments_day" to "Aucun rendez-vous à venir",
        "empty_day_prompt" to "Appuyez sur + pour ajouter des rendez-vous avec %s !",
        "together_color" to "Couleur des rendez-vous ensemble",
        "nav_calendar" to "Calendrier",
        "nav_notebook" to "Carnet",
        "notebook_title" to "Carnet Partagé",
        "add_note" to "Nouvelle Note",
        "edit_note" to "Modifier la Note",
        "note_title_label" to "Titre *",
        "note_title_placeholder" to "ex. Liste de valise, Idées de sortie, Je t'aime...",
        "note_type_text" to "Note de texte",
        "note_type_checklist" to "Liste à cocher",
        "note_content_placeholder" to "Écrivez quelque chose ensemble...",
        "add_checklist_item" to "Ajouter un élément...",
        "share_note" to "Partager la Note",
        "pin_note" to "Épingler en haut",
        "unpin_note" to "Détacher",
        "delete_note_confirm" to "Supprimer cette note ?",
        "empty_notes_title" to "Aucune note pour l'instant",
        "empty_notes_desc" to "Créez des listes partagées, des mots doux ou des idées avec %s !",
        "search_notes_placeholder" to "Rechercher des notes et listes...",
        "all_notes" to "Toutes",
        "filter_cat_general" to "Général",
        "filter_cat_date_ideas" to "Idées de Sortie",
        "filter_cat_shopping" to "Courses & Achats",
        "filter_cat_love_notes" to "Mots Doux",
        "filter_cat_todo" to "Tâches à faire",
        "filter_cat_travel" to "Voyages & Vacances",
        "sync_notes" to "Synchroniser les Notes",
        "note_saved" to "Note enregistrée 💕",
        "note_deleted" to "Note supprimée",
        "simulate_partner_note" to "Mot doux surprise du partenaire 💌",
        "manage_categories" to "Gérer les Catégories",
        "add_category" to "Ajouter une Catégorie",
        "edit_category" to "Modifier la Catégorie",
        "category_name" to "Nom de la Catégorie",
        "category_name_placeholder" to "ex. Films, Recettes, Idées...",
        "category_emoji" to "Icône / Emoji",
        "category_color" to "Couleur de la Catégorie",
        "delete_category_confirm" to "Supprimer la catégorie « %s » ? Les notes seront déplacées dans Général.",
        "delete_appointment_category_confirm" to "Supprimer la catégorie « %s » ? Les rendez-vous seront déplacés dans « Autre ».",
        "cannot_delete_last_category" to "Au moins une catégorie est requise.",
        "tab_appointments" to "Rendez-vous",
        "tab_notes" to "Notes"
    )
}
