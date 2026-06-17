package com.kheyr.sms.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kheyr.sms.KheyrApplication
import com.kheyr.sms.api.ApiConfig
import com.kheyr.sms.api.KheyrApiService
import com.kheyr.sms.conversation.ConversationSearchMatcher
import com.kheyr.sms.contacts.ContactRepository
import com.kheyr.sms.contacts.DeviceContact
import com.kheyr.sms.conversation.SearchableMessage
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.domain.ThreadSorter
import com.kheyr.sms.onboarding.DefaultSmsRoleChecker
import com.kheyr.sms.onboarding.OnboardingGateState
import com.kheyr.sms.onboarding.WelcomeScreenCopy
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.settings.HelpFeedbackModel
import com.kheyr.sms.settings.NotificationSettings
import com.kheyr.sms.settings.SettingsCategory
import com.kheyr.sms.settings.SettingsCategoryOrder
import com.kheyr.sms.settings.ThemePreference
import com.kheyr.sms.settings.ThemePreferenceResolver
import com.kheyr.sms.settings.UnknownSenderNotificationMode
import com.kheyr.sms.settings.NotificationContentMode
import com.kheyr.sms.telephony.SimCard
import com.kheyr.sms.telephony.SimRepository
import com.kheyr.sms.telephony.SmsSendRequest
import com.kheyr.sms.telephony.SmsSender
import com.kheyr.sms.thread.ThreadBulkAction
import com.kheyr.sms.thread.ThreadSearchMatcher
import com.kheyr.sms.thread.SearchableThread
import com.kheyr.sms.worker.KheyrWorkerScheduler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import com.kheyr.sms.util.JalaliDateFormatter
import java.time.Instant

enum class AppScreen { Onboarding, Threads, Conversation, Settings, SettingsDetail, DesktopSync, Help, Contacts }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KheyrAppShell() {
    val context = LocalContext.current
    val app = context.applicationContext as KheyrApplication
    val preferences = app.preferences
    val repository = remember { SmsRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val simRepository = remember { SimRepository(context) }
    val sender = remember { SmsSender(context) }
    val api = remember { KheyrApiService(tokenProvider = { preferences.authTokens().first }) }
    val screenMapper = remember { ConversationScreenMapper() }
    val composerReducer = remember { SmsComposerStateReducer() }
    val threadRowMapper = remember { ThreadRowPresentationMapper() }
    val threadSearchMatcher = remember { ThreadSearchMatcher() }
    val conversationSearchMatcher = remember { ConversationSearchMatcher() }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(if (preferences.onboardingComplete) AppScreen.Threads else AppScreen.Onboarding) }
    var drawerItem by remember { mutableStateOf(DrawerItem.AllMessages) }
    var settingsCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var threadsLoading by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var contactsLoading by remember { mutableStateOf(false) }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var composerState by remember { mutableStateOf(SmsComposerState()) }
    var searchQuery by remember { mutableStateOf("") }
    var conversationSearchQuery by remember { mutableStateOf("") }
    var conversationSearchActive by remember { mutableStateOf(false) }
    var showThreadMenu by remember { mutableStateOf<SmsThread?>(null) }
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerState.open() else drawerState.close()
    }
    var isDefaultSms by remember { mutableStateOf(DefaultSmsRoleChecker.isDefaultSmsApp(context)) }
    var smsPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var contactsPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var sims by remember { mutableStateOf<List<SimCard>>(emptyList()) }
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val lifecycle = activity?.lifecycle ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultSms = DefaultSmsRoleChecker.isDefaultSmsApp(context)
                smsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                contactsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                if (smsPermissionGranted) {
                    sims = simRepository.activeSims()
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var notificationSettings by remember { mutableStateOf(preferences.notificationSettings()) }
    var themePreference by remember { mutableStateOf(preferences.themePreference) }
    var syncEnabled by remember { mutableStateOf(preferences.syncSettings().enabled) }
    var onboardingStep by remember { mutableIntStateOf(0) }
    var otpPhone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val systemDark = isSystemInDarkTheme()
    val darkTheme = ThemePreferenceResolver.isDark(themePreference, systemDark)
    val colorScheme = if (darkTheme) darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF0F8B8D)) else lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF0F8B8D))

    fun openConversation(thread: SmsThread) {
        selectedThread = thread
        messages = emptyList()
        conversationSearchActive = false
        conversationSearchQuery = ""
        composerState = SmsComposerState(
            selectedSubscriptionId = thread.simSlot ?: preferences.defaultSubscriptionId ?: sims.firstOrNull()?.subscriptionId,
            requiresSimSelection = sims.size > 1,
        )
        screen = AppScreen.Conversation
        scope.launch {
            repository.markThreadRead(thread.id)
            messages = repository.loadLocalMessages(thread.id)
        }
    }

    fun openConversationForContact(contact: DeviceContact) {
        val existing = threads.firstOrNull { contactRepository.matchesAddress(it.address, contact.phoneNumber) }
        val threadId = existing?.id ?: Telephony.Threads.getOrCreateThreadId(context, setOf(contact.phoneNumber))
        openConversation(
            existing?.copy(displayName = contact.displayName)
                ?: SmsThread(
                    id = threadId,
                    address = contact.phoneNumber,
                    displayName = contact.displayName,
                    lastMessage = "",
                    lastMessageAt = Instant.now(),
                ),
        )
    }

    fun navigateBack() {
        when {
            screen == AppScreen.Conversation -> {
                scope.launch {
                    selectedThread?.id?.let { repository.markThreadRead(it) }
                    selectedThread = null
                    screen = AppScreen.Threads
                    conversationSearchActive = false
                    conversationSearchQuery = ""
                }
            }
            screen == AppScreen.SettingsDetail -> screen = AppScreen.Settings
            screen in listOf(AppScreen.Settings, AppScreen.DesktopSync, AppScreen.Help, AppScreen.Contacts) -> {
                screen = AppScreen.Threads
                drawerItem = DrawerItem.AllMessages
            }
            drawerOpen -> {
                drawerOpen = false
                scope.launch { drawerState.close() }
            }
        }
    }

    val handleSystemBack = screen != AppScreen.Onboarding && (
        screen == AppScreen.Conversation ||
            screen == AppScreen.SettingsDetail ||
            screen in listOf(AppScreen.Settings, AppScreen.DesktopSync, AppScreen.Help, AppScreen.Contacts) ||
            drawerOpen
        )

    BackHandler(enabled = handleSystemBack) { navigateBack() }

    fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun gateState() = OnboardingGateState(isDefaultSms, smsPermissionGranted, contactsPermissionGranted, hasPermission(Manifest.permission.POST_NOTIFICATIONS) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

    suspend fun loadThreadsForDrawer(): List<SmsThread> {
        val loaded = when (drawerItem) {
            DrawerItem.AllMessages -> repository.loadLocalThreads().let(ThreadSorter::inboxThreads)
            DrawerItem.Spam -> repository.loadSpamThreads()
            DrawerItem.Archived -> repository.loadArchivedThreads()
            DrawerItem.Pinned -> repository.loadPinnedThreads()
            else -> repository.loadLocalThreads()
        }
        return contactRepository.enrichThreads(loaded)
    }

    fun refreshContacts() {
        if (!contactsPermissionGranted) {
            contacts = emptyList()
            return
        }
        scope.launch {
            contactsLoading = true
            try {
                contacts = contactRepository.loadContacts()
            } finally {
                contactsLoading = false
            }
        }
    }

    fun refreshThreadsLocal() {
        if (!smsPermissionGranted) return
        scope.launch {
            threads = loadThreadsForDrawer()
        }
    }

    fun syncThreadsInBackground() {
        if (!smsPermissionGranted) return
        scope.launch {
            repository.syncTelephonyMessages()
            threads = loadThreadsForDrawer()
        }
    }

    fun refreshThreads() {
        if (!smsPermissionGranted) return
        scope.launch {
            threadsLoading = threads.isEmpty()
            try {
                threads = loadThreadsForDrawer()
            } finally {
                threadsLoading = false
            }
            repository.syncTelephonyMessages()
            threads = loadThreadsForDrawer()
            selectedThread?.let { current ->
                val loadedMessages = repository.loadLocalMessages(current.id)
                if (selectedThread?.id == current.id) {
                    messages = loadedMessages
                    selectedThread = threads.firstOrNull { it.id == current.id } ?: current
                }
            }
        }
    }
    fun List<SmsThread>.filterOrFind(id: Long) = firstOrNull { it.id == id }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        smsPermissionGranted = results[Manifest.permission.READ_SMS] == true || hasPermission(Manifest.permission.READ_SMS)
        contactsPermissionGranted = results[Manifest.permission.READ_CONTACTS] == true || hasPermission(Manifest.permission.READ_CONTACTS)
        contactRepository.invalidateCache()
        sims = simRepository.activeSims()
        composerState = composerState.copy(requiresSimSelection = sims.size > 1, selectedSubscriptionId = preferences.defaultSubscriptionId ?: sims.firstOrNull()?.subscriptionId)
        refreshThreads()
        refreshContacts()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultSms = DefaultSmsRoleChecker.isDefaultSmsApp(context)
        permissionLauncher.launch(requiredPermissions())
    }

    LaunchedEffect(drawerItem, screen) {
        if (screen == AppScreen.Threads) {
            refreshThreadsLocal()
            syncThreadsInBackground()
        }
    }

    LaunchedEffect(screen, contactsPermissionGranted) {
        if (screen == AppScreen.Contacts) refreshContacts()
    }

    LaunchedEffect(Unit) {
        if (smsPermissionGranted) {
            sims = simRepository.activeSims()
            composerState = composerState.copy(
                requiresSimSelection = sims.size > 1,
                selectedSubscriptionId = preferences.defaultSubscriptionId ?: sims.firstOrNull()?.subscriptionId,
            )
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = KheyrTypography.typography) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = screen == AppScreen.Threads && selectedThread == null,
            drawerContent = {
                ModalDrawerSheet {
                    Text("Kheyr", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    MainDrawerModel.defaultItems().forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = drawerItem == item,
                            onClick = {
                                drawerOpen = false
                                scope.launch { drawerState.close() }
                                drawerItem = item
                                selectedThread = null
                                screen = when (item) {
                                    DrawerItem.Settings -> AppScreen.Settings
                                    DrawerItem.DesktopSync -> AppScreen.DesktopSync
                                    DrawerItem.HelpFeedback -> AppScreen.Help
                                    DrawerItem.Contacts -> AppScreen.Contacts
                                    else -> AppScreen.Threads
                                }
                            },
                            icon = { Icon(drawerIcon(item), contentDescription = item.title) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when {
                                    screen == AppScreen.Conversation -> selectedThread?.displayName?.ifBlank { selectedThread?.address.orEmpty() }.orEmpty()
                                    screen == AppScreen.Settings -> "Settings"
                                    screen == AppScreen.DesktopSync -> "Desktop Sync"
                                    screen == AppScreen.Help -> "Help & Feedback"
                                    screen == AppScreen.Contacts -> "Contacts"
                                    else -> drawerItem.title
                                },
                            )
                        },
                        navigationIcon = {
                            when {
                                screen == AppScreen.Conversation -> IconButton(onClick = { navigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                                screen == AppScreen.SettingsDetail -> IconButton(onClick = { screen = AppScreen.Settings }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                                screen in listOf(AppScreen.Settings, AppScreen.DesktopSync, AppScreen.Help, AppScreen.Contacts) -> IconButton(onClick = { navigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                                screen == AppScreen.Threads && selectedThread == null -> IconButton(onClick = { drawerOpen = true }) { Icon(Icons.Default.Menu, "Menu") }
                            }
                        },
                        actions = {
                            if (screen == AppScreen.Conversation && selectedThread != null) {
                                IconButton(onClick = { conversationSearchActive = !conversationSearchActive }) { Icon(Icons.Default.Search, "Search") }
                                IconButton(onClick = {
                                    val uri = Uri.parse("tel:${selectedThread!!.address}")
                                    context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                                }) { Icon(Icons.Default.Call, "Call") }
                            }
                        },
                    )
                },
                floatingActionButton = {
                    if (screen == AppScreen.Threads && selectedThread == null && drawerItem == DrawerItem.AllMessages) {
                        FloatingActionButton(onClick = { statusMessage = "New conversation: enter a number from Contacts or reply in an existing thread." }) { Icon(Icons.Default.Edit, "Compose") }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (screen) {
                        AppScreen.Onboarding -> OnboardingFlow(
                            step = onboardingStep,
                            gate = gateState(),
                            otpPhone = otpPhone,
                            otpCode = otpCode,
                            onStepChange = { onboardingStep = it },
                            onOtpPhoneChange = { otpPhone = it },
                            onOtpCodeChange = { otpCode = it },
                            onRequestDefault = {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
                                } else Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                roleLauncher.launch(intent)
                            },
                            onRequestPermissions = { permissionLauncher.launch(requiredPermissions()) },
                            onSkipSync = { preferences.syncOptInSkipped = true; onboardingStep = 4 },
                            onEnableSync = { syncEnabled = true; preferences.saveSyncSettings(preferences.syncSettings().copy(enabled = true)); KheyrWorkerScheduler.scheduleAll(context, true); onboardingStep = 4 },
                            onVerifyOtp = {
                                if (api.verifyOtp(otpPhone, otpCode) != null || !ApiConfig.isConfigured) onboardingStep = 3 else statusMessage = "OTP verification failed. Check code or configure API base URL."
                            },
                            onRequestOtp = {
                                if (api.requestOtp(otpPhone) || !ApiConfig.isConfigured) statusMessage = "OTP sent (or configure ${ApiConfig.BASE_URL_PLACEHOLDER})" else statusMessage = "Failed to request OTP"
                            },
                            onFinish = { preferences.onboardingComplete = true; screen = AppScreen.Threads; refreshThreads() },
                        )
                        AppScreen.Threads -> {
                            if (selectedThread == null) {
                                ThreadFolderScreen(
                                    threads = threads.filter { thread ->
                                        val searchable = SearchableThread(thread.displayName, thread.address, thread.lastMessage)
                                        threadSearchMatcher.matches(searchable, searchQuery)
                                    },
                                    folder = drawerItem.toFolder(),
                                    searchQuery = searchQuery,
                                    onSearchChange = { searchQuery = it },
                                    sims = sims,
                                    mapper = threadRowMapper,
                                    loading = threadsLoading,
                                    onThreadClick = { openConversation(it) },
                                    onThreadLongPress = { showThreadMenu = it },
                                    emptyText = when {
                                        threadsLoading -> "Loading conversations..."
                                        !smsPermissionGranted -> "Grant SMS access to load conversations"
                                        drawerItem == DrawerItem.Spam -> "No spam messages"
                                        drawerItem == DrawerItem.Archived -> "No archived conversations"
                                        drawerItem == DrawerItem.Pinned -> "No pinned conversations"
                                        else -> "No conversations yet"
                                    },
                                )
                            }
                        }
                        AppScreen.Conversation -> selectedThread?.let { thread ->
                            val screenModel = screenMapper.map(thread, messages, sims, composerState)
                            ConversationScreenContent(
                                screen = screenModel,
                                sims = sims,
                                searchActive = conversationSearchActive,
                                searchQuery = conversationSearchQuery,
                                onSearchQueryChange = { conversationSearchQuery = it },
                                matchingIds = conversationSearchMatcher.matchingIds(messages.map { SearchableMessage(it.id, it.body, it.address) }, conversationSearchQuery),
                                onBodyChange = { composerState = composerReducer.reduce(composerState, SmsComposerEvent.BodyChanged(it)) },
                                onSimSelected = { composerState = composerReducer.reduce(composerState, SmsComposerEvent.SubscriptionSelected(it)) },
                                onSend = {
                                    val state = composerReducer.reduce(composerState, SmsComposerEvent.SendRequested)
                                    composerState = state
                                    if (state.error != null) return@ConversationScreenContent
                                    val text = state.body.trim()
                                    scope.launch {
                                        val messageId = repository.persistOutgoing(thread.address, text, state.selectedSubscriptionId)
                                        repository.markSending(messageId)
                                        sender.send(SmsSendRequest(thread.address, text, state.selectedSubscriptionId, messageId))
                                        composerState = composerReducer.reduce(state, SmsComposerEvent.SendCompleted)
                                        messages = repository.loadLocalMessages(thread.id)
                                        refreshThreadsLocal()
                                    }
                                },
                                onRetry = { messageId ->
                                    val message = messages.firstOrNull { it.id == messageId } ?: return@ConversationScreenContent
                                    scope.launch {
                                        repository.markSending(message.id)
                                        sender.send(SmsSendRequest(message.address, message.body, message.simSlot, message.id))
                                        messages = repository.loadLocalMessages(thread.id)
                                    }
                                },
                            )
                        }
                        AppScreen.Settings -> SettingsListScreen(onCategoryClick = { settingsCategory = it; screen = AppScreen.SettingsDetail })
                        AppScreen.SettingsDetail -> settingsCategory?.let { category ->
                            SettingsDetailScreen(
                                category = category,
                                notificationSettings = notificationSettings,
                                themePreference = themePreference,
                                syncEnabled = syncEnabled,
                                defaultSub = preferences.defaultSubscriptionId,
                                sims = sims,
                                directMessagesEnabled = preferences.directMessagesEnabled,
                                spamAutoDeleteDays = preferences.spamAutoDeleteDays,
                                onNotificationChange = { notificationSettings = it; preferences.saveNotificationSettings(it) },
                                onThemeChange = { themePreference = it; preferences.themePreference = it },
                                onSyncChange = { syncEnabled = it; preferences.saveSyncSettings(preferences.syncSettings().copy(enabled = it)); KheyrWorkerScheduler.scheduleAll(context, it) },
                                onDefaultSimChange = { preferences.defaultSubscriptionId = it },
                                onDirectMessagesChange = { preferences.directMessagesEnabled = it },
                                onSpamAutoDeleteChange = { preferences.spamAutoDeleteDays = it },
                                onDeleteCloudData = { if (api.deleteCloudData()) statusMessage = "Cloud data deletion requested" else statusMessage = "Configure API base URL first" },
                                onExportCloudData = { if (api.exportCloudData() != null) statusMessage = "Export requested" else statusMessage = "Configure API base URL first" },
                            )
                        }
                        AppScreen.DesktopSync -> DesktopSyncScreen(apiBaseUrl = ApiConfig.baseUrl, onRevoke = { statusMessage = "Revoke device via Settings when backend is configured." })
                        AppScreen.Help -> HelpScreen()
                        AppScreen.Contacts -> ContactsScreen(
                            contacts = contacts,
                            loading = contactsLoading,
                            hasPermission = contactsPermissionGranted,
                            onRequestPermission = { permissionLauncher.launch(requiredPermissions()) },
                            onContactClick = { openConversationForContact(it) },
                        )
                    }
                    statusMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            tonalElevation = 4.dp,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(msg, modifier = Modifier.weight(1f))
                                TextButton(onClick = { statusMessage = null }) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
        }

        showThreadMenu?.let { thread ->
            val folder = drawerItem.toFolder()
            ThreadActionDialog(
                thread = thread,
                onDismiss = { showThreadMenu = null },
                onAction = { action ->
                    showThreadMenu = null
                    threads = ThreadListOptimisticUpdate.applyAction(threads, thread, action)
                    threads = ThreadListOptimisticUpdate.filterForFolder(threads, folder)
                    scope.launch {
                        when (action) {
                            ThreadBulkAction.MarkRead -> repository.markThreadRead(thread.id)
                            ThreadBulkAction.Archive -> repository.updateArchived(thread.id, !thread.isArchived)
                            ThreadBulkAction.MarkSpam -> repository.updateSpam(thread.id, !thread.isSpam)
                            ThreadBulkAction.Mute -> repository.updateMuted(thread.id, !thread.isMuted)
                            ThreadBulkAction.Delete -> repository.deleteThreadMessages(thread.id)
                        }
                    }
                },
                onPin = {
                    showThreadMenu = null
                    val pinned = !thread.isPinned
                    threads = ThreadListOptimisticUpdate.applyPin(threads, thread, pinned)
                    scope.launch { repository.updatePinned(thread.id, pinned) }
                },
            )
        }
    }
}

private fun DrawerItem.toFolder(): ThreadFolder = when (this) {
    DrawerItem.Spam -> ThreadFolder.Spam
    DrawerItem.Archived -> ThreadFolder.Archived
    DrawerItem.Pinned -> ThreadFolder.Pinned
    else -> ThreadFolder.Inbox
}

@Composable
private fun drawerIcon(item: DrawerItem) = when (item) {
    DrawerItem.AllMessages -> Icons.Default.Email
    DrawerItem.Spam -> Icons.Default.Warning
    DrawerItem.Archived -> Icons.Default.List
    DrawerItem.Pinned -> Icons.Default.Star
    DrawerItem.Contacts -> Icons.Default.Person
    DrawerItem.DesktopSync -> Icons.Default.Phone
    DrawerItem.Settings -> Icons.Default.Settings
    DrawerItem.HelpFeedback -> Icons.Default.Info
}

@Composable
private fun OnboardingFlow(
    step: Int,
    gate: OnboardingGateState,
    otpPhone: String,
    otpCode: String,
    onStepChange: (Int) -> Unit,
    onOtpPhoneChange: (String) -> Unit,
    onOtpCodeChange: (String) -> Unit,
    onRequestDefault: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSkipSync: () -> Unit,
    onEnableSync: () -> Unit,
    onVerifyOtp: () -> Unit,
    onRequestOtp: () -> Unit,
    onFinish: () -> Unit,
) {
    val steps = listOf("Welcome", "Default SMS", "Permissions", "Sync account", "Import & rules")
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LinearProgressIndicator(progress = { (step + 1f) / steps.size }, modifier = Modifier.fillMaxWidth())
        Text(steps.getOrElse(step) { "Done" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        when (step) {
            0 -> {
                Text(WelcomeScreenCopy.title, fontWeight = FontWeight.Bold)
                Text(WelcomeScreenCopy.subtitle)
                Text(WelcomeScreenCopy.defaultSmsRequirement)
                Button(onClick = { onStepChange(1) }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
            1 -> {
                Text("Kheyr must be your default SMS app.")
                if (!gate.isDefaultSmsApp) Button(onClick = onRequestDefault, modifier = Modifier.fillMaxWidth()) { Text("Make default SMS app") }
                else Text("Default SMS role granted.", color = MaterialTheme.colorScheme.primary)
                Button(onClick = { onStepChange(2) }, enabled = gate.isDefaultSmsApp, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
            2 -> {
                Text("Grant SMS, contacts, and notification permissions.")
                if (gate.missingRequirements.isNotEmpty()) {
                    Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) { Text("Grant permissions") }
                } else {
                    Text("All permissions granted.", color = MaterialTheme.colorScheme.primary)
                }
                val ready = gate.canUseFullSmsFeatures
                Button(onClick = { onStepChange(3) }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
            3 -> {
                Text("Optional: create an account to enable encrypted sync and desktop access.")
                Text("Backend URL: ${ApiConfig.baseUrl}", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(value = otpPhone, onValueChange = onOtpPhoneChange, label = { Text("Phone (+E.164)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRequestOtp, modifier = Modifier.weight(1f)) { Text("Send OTP") }
                    OutlinedTextField(value = otpCode, onValueChange = onOtpCodeChange, label = { Text("Code") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = onEnableSync, modifier = Modifier.fillMaxWidth()) { Text("Enable sync") }
                TextButton(onClick = onSkipSync, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
                if (otpCode.isNotBlank()) TextButton(onClick = onVerifyOtp) { Text("Verify OTP") }
            }
            else -> {
                Text("Importing existing SMS and downloading spam rules happens automatically.")
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Open inbox") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ThreadFolderScreen(
    threads: List<SmsThread>,
    folder: ThreadFolder,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sims: List<SimCard>,
    mapper: ThreadRowPresentationMapper,
    loading: Boolean,
    onThreadClick: (SmsThread) -> Unit,
    onThreadLongPress: (SmsThread) -> Unit,
    emptyText: String,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(value = searchQuery, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search threads") }, singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) })
        if (loading && threads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (threads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyText) }
        } else {
            LazyColumn {
                items(threads, key = { it.id }) { thread ->
                val row = mapper.map(thread, folder, sims)
                ListItem(
                    headlineContent = { Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(row.preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMessageTime(thread.lastMessageAt), style = MaterialTheme.typography.labelSmall)
                            row.unreadBadge?.let { Badge { Text(it) } }
                        }
                    },
                    leadingContent = {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(row.title.take(1).uppercase()) }
                        }
                    },
                    overlineContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (row.showPinned) Text("📌", style = MaterialTheme.typography.labelSmall)
                            if (row.showMuted) Text("🔇", style = MaterialTheme.typography.labelSmall)
                            row.simBadge?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                            if (row.showSpamBadge) Text("Spam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onThreadClick(thread) }, onLongClick = { onThreadLongPress(thread) }),
                )
                HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ConversationScreenContent(
    screen: ConversationScreenModel,
    sims: List<SimCard>,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchingIds: List<Long>,
    onBodyChange: (String) -> Unit,
    onSimSelected: (Int) -> Unit,
    onSend: () -> Unit,
    onRetry: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val visibleMessages = if (searchActive && searchQuery.isNotBlank()) screen.messages.filter { it.id in matchingIds } else screen.messages
    val highlightQuery = if (searchActive && searchQuery.isNotBlank()) searchQuery else null

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    LaunchedEffect(screen.messages.lastOrNull()?.id, searchActive) {
        if (!searchActive && screen.messages.isNotEmpty()) {
            listState.scrollToItem(screen.messages.lastIndex)
        }
    }

    LaunchedEffect(searchActive, searchQuery, matchingIds) {
        if (searchActive && searchQuery.isNotBlank() && matchingIds.isNotEmpty()) {
            val index = visibleMessages.indexOfFirst { it.id == matchingIds.first() }
            if (index >= 0) listState.scrollToItem(index)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (searchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .focusRequester(searchFocusRequester),
                placeholder = { Text("Search in conversation") },
                singleLine = true,
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(screen.header.title, style = MaterialTheme.typography.titleMedium); screen.header.subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) } }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visibleMessages, key = { it.id }) { row ->
                ConversationBubbleRow(
                    row = row,
                    highlight = if (row.id in matchingIds) highlightQuery else null,
                    onRetry = { onRetry(row.id) },
                )
            }
        }
        MessageComposerPanel(body = screen.composer.body, sims = sims, selectedSubscriptionId = screen.composer.selectedSubscriptionId, sending = screen.composer.sending, error = screen.composer.error?.name, onBodyChange = onBodyChange, onSimSelected = onSimSelected, onSend = onSend)
    }
}

@Composable
private fun ConversationBubbleRow(row: ConversationMessageRow, highlight: String?, onRetry: () -> Unit) {
    val alignment = if (row.layout.alignment == BubbleAlignment.End) Arrangement.End else Arrangement.Start
    Row(Modifier.fillMaxWidth(), horizontalArrangement = alignment) {
        if (row.layout.showBubble) {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = if (row.layout.alignment == BubbleAlignment.End) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                MessageBubbleContent(row, highlight, onRetry, false)
            }
        } else MessageBubbleContent(row, highlight, onRetry, true)
    }
}

@Composable
private fun MessageBubbleContent(row: ConversationMessageRow, highlight: String?, onRetry: () -> Unit, emojiStyle: Boolean) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.padding(12.dp).widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (emojiStyle) {
            Text(row.body, fontSize = 28.sp, textAlign = TextAlign.Center)
        } else {
            HighlightedMessageText(text = row.body, highlight = highlight)
        }
        Text(row.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        row.copyableCode?.let { code ->
            TextButton(
                onClick = { clipboard.setText(AnnotatedString(code)) },
                contentPadding = PaddingValues(0.dp),
            ) { Text("کپی کد") }
        }
        if (row.showRetry) TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("Retry") }
    }
}

@Composable
private fun MessageComposerPanel(body: String, sims: List<SimCard>, selectedSubscriptionId: Int?, sending: Boolean, error: String?, onBodyChange: (String) -> Unit, onSimSelected: (Int) -> Unit, onSend: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sims.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { sims.forEach { sim -> FilterChip(selected = selectedSubscriptionId == sim.subscriptionId, onClick = { onSimSelected(sim.subscriptionId) }, label = { Text(sim.displayName) }) } }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = body, onValueChange = onBodyChange, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, minLines = 1, maxLines = 5)
                Button(onClick = onSend, enabled = body.isNotBlank() && !sending) { Text(if (sending) "..." else "Send") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun SettingsListScreen(onCategoryClick: (SettingsCategory) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SettingsCategoryOrder.ordered) { category ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { onCategoryClick(category) }) {
                ListItem(headlineContent = { Text(category.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")) }, supportingContent = { Text(settingsCategoryDescription(category)) })
            }
        }
    }
}

private fun settingsCategoryDescription(category: SettingsCategory): String = when (category) {
    SettingsCategory.Notifications -> "Ringtone, vibration, and content privacy"
    SettingsCategory.UnknownSenders -> "Alerts from numbers not in contacts"
    SettingsCategory.SpamProtection -> "Global spam rules and thresholds"
    SettingsCategory.DualSim -> "Default SIM for outgoing SMS"
    SettingsCategory.Sync -> "Encrypted cloud sync and status"
    SettingsCategory.DesktopDevices -> "Paired desktop clients"
    SettingsCategory.PrivacySecurity -> "Cloud delete, export, and encryption"
    SettingsCategory.Appearance -> "Light, dark, or system theme"
    SettingsCategory.About -> "Version and legal links"
}

@Composable
private fun SettingsDetailScreen(
    category: SettingsCategory,
    notificationSettings: NotificationSettings,
    themePreference: ThemePreference,
    syncEnabled: Boolean,
    defaultSub: Int?,
    sims: List<SimCard>,
    directMessagesEnabled: Boolean,
    spamAutoDeleteDays: Int,
    onNotificationChange: (NotificationSettings) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onSyncChange: (Boolean) -> Unit,
    onDefaultSimChange: (Int?) -> Unit,
    onDirectMessagesChange: (Boolean) -> Unit,
    onSpamAutoDeleteChange: (Int) -> Unit,
    onDeleteCloudData: () -> Unit,
    onExportCloudData: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (category) {
            SettingsCategory.Notifications -> {
                NotificationContentMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                        RadioButton(selected = notificationSettings.contentMode == mode, onClick = { onNotificationChange(notificationSettings.copy(contentMode = mode)) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vibrate"); Switch(checked = notificationSettings.vibrate, onCheckedChange = { onNotificationChange(notificationSettings.copy(vibrate = it)) })
                }
            }
            SettingsCategory.UnknownSenders -> UnknownSenderNotificationMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mode.name); RadioButton(selected = notificationSettings.unknownSenderMode == mode, onClick = { onNotificationChange(notificationSettings.copy(unknownSenderMode = mode)) })
                }
            }
            SettingsCategory.SpamProtection -> {
                Text("Spam rules are downloaded from the server. Cached rules are used offline.")
                Text("Auto-delete spam after (days, 0=never): $spamAutoDeleteDays")
                Slider(value = spamAutoDeleteDays.toFloat(), onValueChange = { onSpamAutoDeleteChange(it.toInt()) }, valueRange = 0f..30f, steps = 30)
            }
            SettingsCategory.DualSim -> sims.forEach { sim ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sim.displayName); RadioButton(selected = defaultSub == sim.subscriptionId, onClick = { onDefaultSimChange(sim.subscriptionId) })
                }
            }
            SettingsCategory.Sync -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Enable sync"); Switch(checked = syncEnabled, onCheckedChange = onSyncChange) }
                Text("API: ${ApiConfig.baseUrl}", style = MaterialTheme.typography.labelSmall)
            }
            SettingsCategory.DesktopDevices -> Text("Manage paired desktop devices from Desktop Sync in the drawer.")
            SettingsCategory.PrivacySecurity -> {
                Button(onClick = onDeleteCloudData, modifier = Modifier.fillMaxWidth()) { Text("Delete cloud data") }
                OutlinedButton(onClick = onExportCloudData, modifier = Modifier.fillMaxWidth()) { Text("Export cloud data") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Direct messages"); Switch(checked = directMessagesEnabled, onCheckedChange = onDirectMessagesChange) }
            }
            SettingsCategory.Appearance -> ThemePreference.entries.forEach { pref ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(pref.name); RadioButton(selected = themePreference == pref, onClick = { onThemeChange(pref) }) }
            }
            SettingsCategory.About -> {
                Text("Kheyr SMS v0.1.0")
                Text("Modern SMS with spam filtering, sync, and desktop relay.")
                TextButton(onClick = {}) { Text("Privacy policy") }
            }
        }
    }
}

@Composable
private fun DesktopSyncScreen(apiBaseUrl: String, onRevoke: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pair your desktop app by scanning a QR code shown on the desktop client.", style = MaterialTheme.typography.bodyLarge)
        Text("Backend URL: $apiBaseUrl", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("Replace YOUR-BASE-URL in app/build.gradle with your server address.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onRevoke, modifier = Modifier.fillMaxWidth()) { Text("Revoke paired device") }
    }
}

@Composable
private fun HelpScreen() {
    val help = HelpFeedbackModel(helpUrl = "https://kheyr.app/help", supportEmail = "support@kheyr.app")
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Help & Feedback"); Text("Email: ${help.supportEmail}"); Text("Help center: ${help.helpUrl}")
    }
}

@Composable
private fun ContactsScreen(
    contacts: List<DeviceContact>,
    loading: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onContactClick: (DeviceContact) -> Unit,
) {
    when {
        !hasPermission -> {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Contacts permission is required to show your address book.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestPermission) { Text("Grant contacts access") }
            }
        }
        loading && contacts.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        contacts.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No contacts with phone numbers found.")
            }
        }
        else -> {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(contacts, key = { "${it.id}:${it.phoneNumber}" }) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(contact.phoneNumber) },
                        leadingContent = {
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(contact.displayName.take(1).uppercase()) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactClick(contact) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ThreadActionDialog(thread: SmsThread, onDismiss: () -> Unit, onAction: (ThreadBulkAction) -> Unit, onPin: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(thread.displayName.ifBlank { thread.address }) },
        text = {
            Column {
                TextButton(onClick = onPin, modifier = Modifier.fillMaxWidth()) { Text(if (thread.isPinned) "Unpin" else "Pin") }
                TextButton(onClick = { onAction(ThreadBulkAction.MarkRead) }, modifier = Modifier.fillMaxWidth()) { Text("Mark read") }
                TextButton(onClick = { onAction(ThreadBulkAction.Archive) }, modifier = Modifier.fillMaxWidth()) { Text(if (thread.isArchived) "Unarchive" else "Archive") }
                TextButton(onClick = { onAction(ThreadBulkAction.MarkSpam) }, modifier = Modifier.fillMaxWidth()) { Text(if (thread.isSpam) "Not spam" else "Mark spam") }
                TextButton(onClick = { onAction(ThreadBulkAction.Mute) }, modifier = Modifier.fillMaxWidth()) { Text(if (thread.isMuted) "Unmute" else "Mute") }
                TextButton(onClick = { onAction(ThreadBulkAction.Delete) }, modifier = Modifier.fillMaxWidth()) { Text("Delete messages") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS); add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.READ_CONTACTS); add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun formatMessageTime(instant: Instant): String = JalaliDateFormatter.format(instant)
