package com.kheyr.sms.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kheyr.sms.KheyrApplication
import com.kheyr.sms.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.kheyr.sms.api.ApiConfig
import com.kheyr.sms.api.KheyrApiService
import com.kheyr.sms.api.RemoteDevice
import com.kheyr.sms.desktop.DesktopRelayExecutor
import com.kheyr.sms.desktop.DesktopRelayResolver
import com.kheyr.sms.desktop.PairingQrCodec
import com.kheyr.sms.desktop.PairingQrResult
import com.kheyr.sms.desktop.PairingQrValidator
import com.kheyr.sms.realtime.KheyrRealtimeClient
import com.kheyr.sms.sync.SyncEncryptionKeyStore
import com.kheyr.sms.sync.crypto.EncryptedSmsBody
import com.kheyr.sms.sync.crypto.SmsBodyEncryptor
import com.kheyr.sms.conversation.ConversationSearchMatcher
import com.kheyr.sms.contacts.ContactRepository
import com.kheyr.sms.contacts.DeviceContact
import com.kheyr.sms.conversation.SearchableMessage
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SmsRefreshEvents
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.domain.ThreadSorter
import com.kheyr.sms.onboarding.DefaultSmsRoleChecker
import com.kheyr.sms.onboarding.OnboardingGateState
import com.kheyr.sms.onboarding.OnboardingCopy
import com.kheyr.sms.preferences.AppPreferences
import com.kheyr.sms.settings.HelpFeedbackModel
import com.kheyr.sms.settings.NotificationSettings
import com.kheyr.sms.settings.SettingsCategory
import com.kheyr.sms.settings.SettingsCategoryOrder
import com.kheyr.sms.settings.ThemePreference
import com.kheyr.sms.settings.UnknownSenderNotificationMode
import com.kheyr.sms.settings.NotificationContentMode
import com.kheyr.sms.telephony.ComposerSimResolver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kheyr.sms.util.JalaliDateFormatter
import java.time.Instant
import kotlin.math.roundToInt

enum class AppScreen { Onboarding, Main, NewMessage, Conversation, SettingsDetail, DesktopSync, Help }

private sealed interface InboxPane {
    data object List : InboxPane
    data class Chat(val threadId: Long) : InboxPane
}

// Captures exactly which messages a deferred delete should remove. Deleting by the snapshot's ids
// (instead of the whole thread) means messages that arrive while the undo snackbar is visible survive.
private data class PendingThreadDelete(val threadId: Long, val messageIds: List<Long>)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KheyrAppShell(openThreadId: Long? = null, onThreadConsumed: () -> Unit = {}) {
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
    // All thread-list reloads run through this single job; starting a new one cancels the previous
    // so concurrent loads can't land out of order or clobber an optimistic update.
    val reloadJob = remember { mutableStateOf<Job?>(null) }

    // Real-time backend channel: receives desktop-SMS relay requests and runs them through the
    // decrypt -> persist -> send -> report-status pipeline. Null until a backend URL is configured.
    val realtimeClient = remember {
        if (!ApiConfig.isConfigured) {
            null
        } else {
            KheyrRealtimeClient(
                baseUrl = ApiConfig.baseUrl,
                accessTokenProvider = { preferences.authTokens().first },
                onDesktopSmsRequest = { payload ->
                    val encryptor = SmsBodyEncryptor(SyncEncryptionKeyStore(context).getOrCreateKey())
                    val resolver = DesktopRelayResolver(
                        // Relayed body/number arrive encrypted with the shared sync key; fall back to
                        // treating the value as plaintext if it isn't in encrypted wire form.
                        decrypt = { value -> EncryptedSmsBody.parse(value)?.let(encryptor::decryptBody) ?: value },
                        resolveSubscriptionId = { simId -> simId?.toIntOrNull() ?: preferences.defaultSubscriptionId },
                    )
                    val executor = DesktopRelayExecutor(resolver, sender, repository, api)
                    scope.launch { executor.handle(payload) }
                },
            )
        }
    }
    // Re-evaluated each recomposition; flips true after sign-in (and false after logout), so the
    // effect (re)connects/disconnects the realtime channel at the right moments rather than only at launch.
    val realtimeSignedIn = preferences.authTokens().first != null
    DisposableEffect(realtimeSignedIn) {
        if (realtimeSignedIn) realtimeClient?.start()
        onDispose { realtimeClient?.stop() }
    }

    var screen by remember { mutableStateOf(if (preferences.onboardingComplete) AppScreen.Main else AppScreen.Onboarding) }
    var selectedTab by remember { mutableStateOf(MainTab.Chats) }
    var chatFolder by remember { mutableStateOf(ChatFolder.All) }
    var settingsCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var threadsLoading by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var contactsLoading by remember { mutableStateOf(false) }
    var contactsSearchQuery by remember { mutableStateOf("") }
    var newMessageQuery by remember { mutableStateOf("") }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    // Retains the open thread while the back/close slide is running. navigateBack() clears
    // selectedThread synchronously, so the outgoing Chat pane is rendered from this snapshot
    // (resolved by pane.threadId) instead of going blank mid-animation.
    var lastOpenedThread by remember { mutableStateOf<SmsThread?>(null) }
    var messages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var composerState by remember { mutableStateOf(SmsComposerState()) }
    var searchQuery by remember { mutableStateOf("") }
    var threadListFilter by remember { mutableStateOf(ThreadListFilter.All) }
    var conversationSearchQuery by remember { mutableStateOf("") }
    var conversationSearchActive by remember { mutableStateOf(false) }
    var showThreadMenu by remember { mutableStateOf<SmsThread?>(null) }
    var chatsOverflowExpanded by remember { mutableStateOf(false) }
    var threadSelectionOverflowExpanded by remember { mutableStateOf(false) }
    var isDefaultSms by remember { mutableStateOf(DefaultSmsRoleChecker.isDefaultSmsApp(context)) }
    var smsPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var contactsPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var sims by remember { mutableStateOf<List<SimCard>>(emptyList()) }
    var resumeNonce by remember { mutableIntStateOf(0) }
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
                    resumeNonce++
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
    val snackbarHostState = remember { SnackbarHostState() }
    var inboxNavForward by remember { mutableStateOf(true) }
    var pendingDelete by remember { mutableStateOf<PendingThreadDelete?>(null) }
    var threadSelection by remember { mutableStateOf(ThreadSelectionState()) }
    val threadListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val contactsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val darkTheme = isKheyrDarkTheme(themePreference)

    fun resolvedComposerSim(thread: SmsThread? = selectedThread): Int? =
        ComposerSimResolver.resolve(sims, thread?.simSlot, preferences.defaultSubscriptionId)

    fun composerStateForThread(thread: SmsThread): SmsComposerState = SmsComposerState(
        selectedSubscriptionId = resolvedComposerSim(thread),
        requiresSimSelection = sims.size > 1,
    )

    fun openConversation(thread: SmsThread) {
        threadSelection = threadSelection.clear()
        inboxNavForward = true
        selectedThread = thread
        lastOpenedThread = thread
        conversationSearchActive = false
        conversationSearchQuery = ""
        composerState = composerStateForThread(thread)
        scope.launch {
            // Load the messages before switching screens so the conversation slides in already
            // populated, instead of sliding in empty and popping the bubbles in a frame later.
            messages = repository.loadLocalMessages(thread.id)
            screen = AppScreen.Conversation
            repository.markThreadRead(thread.id)
        }
    }

    fun openConversationForRecipient(recipient: NewMessageRecipient) {
        val existing = threads.firstOrNull { contactRepository.matchesAddress(it.address, recipient.address) }
        val threadId = existing?.id ?: Telephony.Threads.getOrCreateThreadId(context, setOf(recipient.address))
        openConversation(
            existing?.copy(displayName = recipient.displayName.ifBlank { existing.displayName }, contactPhotoUri = recipient.photoUri ?: existing.contactPhotoUri)
                ?: SmsThread(
                    id = threadId,
                    address = recipient.address,
                    displayName = recipient.displayName.ifBlank { recipient.address },
                    lastMessage = "",
                    lastMessageAt = Instant.now(),
                    contactPhotoUri = recipient.photoUri,
                ),
        )
    }

    fun openConversationForContact(contact: DeviceContact) {
        openConversationForRecipient(NewMessageViewModel().recipientFor(contact))
    }

    fun navigateBack() {
        when {
            screen == AppScreen.Conversation -> {
                inboxNavForward = false
                // Flip state synchronously so the slide-out starts on this frame; the exiting Chat
                // pane keeps rendering from lastOpenedThread. mark-read is a DB write, so push it to
                // the background instead of letting it stall the back animation.
                val closing = selectedThread
                selectedThread = null
                screen = AppScreen.Main
                conversationSearchActive = false
                conversationSearchQuery = ""
                closing?.id?.let { id -> scope.launch { repository.markThreadRead(id) } }
            }
            screen == AppScreen.NewMessage -> {
                screen = AppScreen.Main
                selectedTab = MainTab.Chats
                newMessageQuery = ""
            }
            screen == AppScreen.SettingsDetail -> {
                screen = AppScreen.Main
                selectedTab = MainTab.Settings
            }
            screen in listOf(AppScreen.DesktopSync, AppScreen.Help) -> {
                screen = AppScreen.Main
                selectedTab = MainTab.Chats
            }
        }
    }

    val handleSystemBack = threadSelection.isSelectionMode || (screen != AppScreen.Onboarding && (
        screen == AppScreen.Conversation ||
            screen == AppScreen.NewMessage ||
            screen == AppScreen.SettingsDetail ||
            screen in listOf(AppScreen.DesktopSync, AppScreen.Help)
        ))

    BackHandler(enabled = handleSystemBack) {
        if (threadSelection.isSelectionMode) threadSelection = threadSelection.clear() else navigateBack()
    }

    fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun gateState() = OnboardingGateState(isDefaultSms, smsPermissionGranted, contactsPermissionGranted, hasPermission(Manifest.permission.POST_NOTIFICATIONS) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

    suspend fun loadThreadsForFolder(): List<SmsThread> {
        val loaded = when (chatFolder) {
            ChatFolder.All -> repository.loadLocalThreads().let(ThreadSorter::inboxThreads)
            ChatFolder.Spam -> repository.loadSpamThreads()
            ChatFolder.Archived -> repository.loadArchivedThreads()
            ChatFolder.Pinned -> repository.loadPinnedThreads()
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

    // Serializes thread reloads: cancels any in-flight reload before launching the next, so the
    // last requested reload is the one that writes `threads`.
    fun launchThreadReload(block: suspend () -> Unit) {
        reloadJob.value?.cancel()
        reloadJob.value = scope.launch { block() }
    }

    fun refreshThreadsLocal() {
        if (!smsPermissionGranted) return
        launchThreadReload {
            threads = loadThreadsForFolder()
        }
    }

    suspend fun commitPendingDelete() {
        val pending = pendingDelete ?: return
        pendingDelete = null
        // Delete only the messages captured at delete time, leaving any that arrived during the undo window.
        repository.deleteMessagesByIds(pending.messageIds)
    }

    fun deleteThreadWithUndo(thread: SmsThread) {
        scope.launch {
            commitPendingDelete()
            val folder = chatFolder.toThreadFolder()
            val snapshot = repository.loadLocalMessageEntities(thread.id)
            val snapshotIds = snapshot.map { it.id }
            pendingDelete = PendingThreadDelete(thread.id, snapshotIds)
            // Delete the snapshot rows now so the optimistic removal matches persisted state. Only
            // these ids are removed, so messages that arrive during the undo window survive; Undo
            // re-inserts the snapshot as the sole copies instead of duplicating still-present rows.
            repository.deleteMessagesByIds(snapshotIds)
            threads = ThreadListOptimisticUpdate.applyAction(threads, thread, ThreadBulkAction.Delete)
            threads = ThreadListOptimisticUpdate.filterForFolder(threads, folder)
            if (selectedThread?.id == thread.id) {
                navigateBack()
            }
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.thread_deleted),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                // Only clear the pending slot if it still belongs to THIS thread; a later overlapping
                // delete may have taken the slot, and clearing it would drop that thread's deletion.
                if (pendingDelete?.threadId == thread.id) pendingDelete = null
                repository.restoreThreadMessages(snapshot)
                refreshThreadsLocal()
            } else if (pendingDelete?.threadId == thread.id) {
                commitPendingDelete()
            }
        }
    }


    fun selectedThreadsForAction(): List<SmsThread> = threads.filter { it.id in threadSelection.selectedThreadIds }

    fun runThreadSelectionAction(action: ThreadBulkAction) {
        val request = threadSelection.request(action)
        if (!request.isRunnable) return
        val selectedThreads = selectedThreadsForAction()
        val folder = chatFolder.toThreadFolder()
        // Bulk Archive/Mark spam/Mute apply one consistent target state across the whole selection
        // (mirroring bulk pin) instead of flipping each thread from its own state, which would do the
        // opposite for some rows in a mixed selection. Only turn the state off when every selected
        // thread already has it on.
        val target: Boolean? = when (action) {
            ThreadBulkAction.Archive -> !selectedThreads.all { it.isArchived }
            ThreadBulkAction.MarkSpam -> !selectedThreads.all { it.isSpam }
            ThreadBulkAction.Mute -> !selectedThreads.all { it.isMuted }
            else -> null
        }
        threads = selectedThreads.fold(threads) { current, thread ->
            ThreadListOptimisticUpdate.applyAction(current, thread, action, target)
        }
        threads = ThreadListOptimisticUpdate.filterForFolder(threads, folder)
        threadSelection = threadSelection.clear()
        scope.launch {
            selectedThreads.forEach { thread ->
                when (action) {
                    ThreadBulkAction.MarkRead -> repository.markThreadRead(thread.id)
                    ThreadBulkAction.Archive -> repository.updateArchived(thread.id, target ?: !thread.isArchived)
                    ThreadBulkAction.MarkSpam -> repository.updateSpam(thread.id, target ?: !thread.isSpam)
                    ThreadBulkAction.Mute -> repository.updateMuted(thread.id, target ?: !thread.isMuted)
                    ThreadBulkAction.Delete -> repository.deleteThreadMessages(thread.id)
                }
            }
            refreshThreadsLocal()
        }
    }

    fun runThreadSelectionPin(pinned: Boolean) {
        val selectedThreads = selectedThreadsForAction()
        if (selectedThreads.isEmpty()) return
        val folder = chatFolder.toThreadFolder()
        threads = selectedThreads.fold(threads) { current, thread ->
            ThreadListOptimisticUpdate.applyPin(current, thread, pinned)
        }
        threads = ThreadListOptimisticUpdate.filterForFolder(threads, folder)
        threadSelection = threadSelection.clear()
        scope.launch {
            selectedThreads.forEach { thread -> repository.updatePinned(thread.id, pinned) }
            refreshThreadsLocal()
        }
    }

    fun syncThreadsInBackground() {
        if (!smsPermissionGranted) return
        launchThreadReload {
            repository.syncTelephonyMessages()
            threads = loadThreadsForFolder()
        }
    }

    fun refreshThreads() {
        if (!smsPermissionGranted) return
        launchThreadReload {
            threadsLoading = threads.isEmpty()
            try {
                threads = loadThreadsForFolder()
            } finally {
                threadsLoading = false
            }
            repository.syncTelephonyMessages()
            threads = loadThreadsForFolder()
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
        composerState = composerState.copy(
            requiresSimSelection = sims.size > 1,
            selectedSubscriptionId = resolvedComposerSim() ?: sims.firstOrNull()?.subscriptionId,
        )
        refreshThreads()
        refreshContacts()
    }
    fun openAppPermissionSettings() {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(settingsIntent)
            statusMessage = "If the default SMS prompt is unavailable, enable SMS permissions and default app access for Kheyr in Android settings."
        } catch (_: ActivityNotFoundException) {
            statusMessage = "Open Android settings manually and enable SMS permissions/default app access for Kheyr."
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultSms = DefaultSmsRoleChecker.isDefaultSmsApp(context)
        if (isDefaultSms) {
            permissionLauncher.launch(requiredPermissions())
        } else {
            openAppPermissionSettings()
        }
    }

    LaunchedEffect(chatFolder, screen, selectedTab) {
        if (screen == AppScreen.Main && selectedTab == MainTab.Chats) {
            // One sequenced reload (quick local load, then sync, then reload) instead of two
            // concurrent launches that raced to assign `threads`.
            launchThreadReload {
                threads = loadThreadsForFolder()
                repository.syncTelephonyMessages()
                threads = loadThreadsForFolder()
            }
        }
    }

    LaunchedEffect(resumeNonce) {
        if (resumeNonce > 0 && screen == AppScreen.Main && selectedTab == MainTab.Chats) {
            refreshThreadsLocal()
        }
    }

    LaunchedEffect(Unit) {
        SmsRefreshEvents.events.collect { event ->
            when (event) {
                SmsRefreshEvents.RefreshKind.Threads -> {
                    if (screen == AppScreen.Main && selectedTab == MainTab.Chats) {
                        refreshThreadsLocal()
                    }
                }
                is SmsRefreshEvents.RefreshKind.ThreadMessages -> {
                    if (screen == AppScreen.Main && selectedTab == MainTab.Chats) {
                        refreshThreadsLocal()
                    }
                    if (selectedThread?.id == event.threadId) {
                        messages = repository.loadLocalMessages(event.threadId)
                        selectedThread = threads.filterOrFind(event.threadId) ?: selectedThread
                    }
                }
            }
        }
    }

    LaunchedEffect(screen, selectedTab, contactsPermissionGranted) {
        if (screen == AppScreen.Main && selectedTab == MainTab.Contacts) refreshContacts()
    }

    LaunchedEffect(Unit) {
        if (smsPermissionGranted) {
            sims = simRepository.activeSims()
            composerState = composerState.copy(
                requiresSimSelection = sims.size > 1,
                selectedSubscriptionId = resolvedComposerSim() ?: sims.firstOrNull()?.subscriptionId,
            )
        }
    }

    LaunchedEffect(sims, selectedThread?.id) {
        if (selectedThread != null) {
            val resolved = resolvedComposerSim(selectedThread)
            if (resolved != null) {
                composerState = composerState.copy(
                    selectedSubscriptionId = resolved,
                    requiresSimSelection = sims.size > 1,
                )
            }
        }
    }

    // Opening a notification deep-links straight into its conversation.
    LaunchedEffect(openThreadId) {
        val id = openThreadId ?: return@LaunchedEffect
        if (!smsPermissionGranted || !preferences.onboardingComplete) {
            onThreadConsumed()
            return@LaunchedEffect
        }
        val target = repository.loadLocalThreads().firstOrNull { it.id == id }
            ?: repository.loadSpamThreads().firstOrNull { it.id == id }
            ?: repository.loadArchivedThreads().firstOrNull { it.id == id }
        if (target != null) {
            openConversation(contactRepository.enrichThreads(listOf(target)).firstOrNull() ?: target)
        }
        onThreadConsumed()
    }

    val showShellTopBar = screen != AppScreen.Onboarding && screen != AppScreen.Conversation && screen != AppScreen.NewMessage

    KheyrTheme(themePreference = themePreference) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
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
                                val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    context.getSystemService(RoleManager::class.java)
                                } else {
                                    null
                                }
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                                        .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                } else {
                                    null
                                }
                                if (intent == null) {
                                    openAppPermissionSettings()
                                } else {
                                    try {
                                        roleLauncher.launch(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        openAppPermissionSettings()
                                    }
                                }
                            },
                            onRequestPermissions = { permissionLauncher.launch(requiredPermissions()) },
                            onSkipSync = { preferences.syncOptInSkipped = true; onboardingStep = 4 },
                            onEnableSync = { syncEnabled = true; preferences.saveSyncSettings(preferences.syncSettings().copy(enabled = true)); KheyrWorkerScheduler.scheduleAll(context, true); onboardingStep = 4 },
                            onVerifyOtp = {
                                scope.launch(Dispatchers.IO) {
                                    // Persist tokens + device_id so every authenticated feature (sync, pairing,
                                    // relay, logout) can authenticate. The verify response already registers an
                                    // Android device server-side, so no separate registerDevice call is needed.
                                    val tokens = api.verifyOtp(otpPhone, otpCode)
                                    if (tokens != null) {
                                        preferences.saveAuthTokens(tokens.accessToken, tokens.refreshToken)
                                        preferences.userPhone = otpPhone
                                        tokens.deviceId?.let { id ->
                                            preferences.saveSyncSettings(preferences.syncSettings().copy(deviceId = id))
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (tokens != null || !ApiConfig.isConfigured) onboardingStep = 3
                                        else statusMessage = "OTP verification failed. Check code or configure API base URL."
                                    }
                                }
                            },
                            onRequestOtp = {
                                scope.launch(Dispatchers.IO) {
                                    val sent = api.requestOtp(otpPhone)
                                    withContext(Dispatchers.Main) {
                                        statusMessage = if (sent || !ApiConfig.isConfigured) "OTP sent (or configure ${ApiConfig.BASE_URL_PLACEHOLDER})" else "Failed to request OTP"
                                    }
                                }
                            },
                            onFinish = { preferences.onboardingComplete = true; screen = AppScreen.Main; selectedTab = MainTab.Chats; refreshThreads() },
                        )
                        AppScreen.Main, AppScreen.NewMessage, AppScreen.Conversation -> {
                            if (screen == AppScreen.NewMessage) {
                                NewMessageScreen(
                                    contacts = contacts,
                                    loading = contactsLoading,
                                    hasPermission = contactsPermissionGranted,
                                    query = newMessageQuery,
                                    onQueryChange = { newMessageQuery = it },
                                    onRequestPermission = { permissionLauncher.launch(requiredPermissions()) },
                                    onRecipientSelected = { openConversationForRecipient(it) },
                                    onNavigateBack = { navigateBack() },
                                )
                            } else if (screen == AppScreen.Main && selectedTab != MainTab.Chats) {
                                when (selectedTab) {
                                    MainTab.Contacts -> ContactsScreen(
                                        contacts = contacts,
                                        loading = contactsLoading,
                                        hasPermission = contactsPermissionGranted,
                                        searchQuery = contactsSearchQuery,
                                        onSearchChange = { contactsSearchQuery = it },
                                        onRequestPermission = { permissionLauncher.launch(requiredPermissions()) },
                                        onContactClick = { openConversationForContact(it) },
                                        listState = contactsListState,
                                    )
                                    MainTab.Settings -> SettingsListScreen(
                                        onCategoryClick = {
                                            settingsCategory = it
                                            screen = AppScreen.SettingsDetail
                                        },
                                    )
                                    MainTab.Profile -> ProfileScreen(
                                        themePreference = themePreference,
                                        onThemeChange = { themePreference = it; preferences.themePreference = it },
                                        onHelpClick = { screen = AppScreen.Help },
                                        phoneNumber = preferences.userPhone,
                                        signedIn = preferences.authTokens().first != null,
                                        onLogout = {
                                            scope.launch(Dispatchers.IO) {
                                                api.logout()
                                                clearLocalSession(preferences)
                                                withContext(Dispatchers.Main) {
                                                    realtimeClient?.stop()
                                                    statusMessage = "Signed out"
                                                    onboardingStep = 0
                                                    screen = AppScreen.Onboarding
                                                }
                                            }
                                        },
                                        onDeleteAccount = {
                                            scope.launch(Dispatchers.IO) {
                                                api.deleteAccount()
                                                clearLocalSession(preferences)
                                                withContext(Dispatchers.Main) {
                                                    realtimeClient?.stop()
                                                    statusMessage = "Account deleted"
                                                    onboardingStep = 0
                                                    screen = AppScreen.Onboarding
                                                }
                                            }
                                        },
                                    )
                                    MainTab.Chats -> Unit
                                }
                            }
                            val showChatPane = screen == AppScreen.Conversation || (screen == AppScreen.Main && selectedTab == MainTab.Chats)
                            if (showChatPane) {
                            val inboxPane: InboxPane = if (screen == AppScreen.Conversation && selectedThread != null) {
                                InboxPane.Chat(selectedThread!!.id)
                            } else {
                                InboxPane.List
                            }
                            AnimatedContent(
                                targetState = inboxPane,
                                transitionSpec = {
                                    // The Chat pane slides opaquely over/off; the threads List pane also
                                    // fades as it parallaxes (it/3) so it doesn't linger half-visible
                                    // behind the chat. Disable the size clip so neither pane is cropped
                                    // mid-slide (e.g. when the IME/composer changes the chat's height).
                                    val transform = if (inboxNavForward) {
                                        slideInHorizontally(animationSpec = tween(300)) { it } togetherWith
                                            (slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } +
                                                fadeOut(tween(300)))
                                    } else {
                                        (slideInHorizontally(animationSpec = tween(300)) { -it / 3 } +
                                            fadeIn(tween(300))) togetherWith
                                            slideOutHorizontally(animationSpec = tween(300)) { it }
                                    }
                                    transform.using(SizeTransform(clip = false))
                                },
                                label = "inbox",
                            ) { pane ->
                                when (pane) {
                                    InboxPane.List -> {
                                        // Filter once per (threads, filter, query) change instead of allocating a
                                        // new filtered list + SearchableThread per row on every recomposition
                                        // (e.g. selection toggles or snackbar show/hide).
                                        val visibleThreads = remember(threads, threadListFilter, searchQuery) {
                                            threads.filter { thread ->
                                                threadListFilter.matches(thread) &&
                                                    threadSearchMatcher.matches(
                                                        SearchableThread(thread.displayName, thread.address, thread.lastMessage),
                                                        searchQuery,
                                                    )
                                            }
                                        }
                                        ThreadFolderScreen(
                                        threads = visibleThreads,
                                        folder = chatFolder.toThreadFolder(),
                                        showFilters = chatFolder == ChatFolder.All,
                                        threadListFilter = threadListFilter,
                                        onThreadListFilterChange = { threadListFilter = it },
                                        searchQuery = searchQuery,
                                        onSearchChange = { searchQuery = it },
                                        sims = sims,
                                        mapper = threadRowMapper,
                                        loading = threadsLoading,
                                        selectedThreadIds = threadSelection.selectedThreadIds,
                                        onThreadClick = { thread ->
                                            if (threadSelection.isSelectionMode) threadSelection = threadSelection.toggle(thread.id) else openConversation(thread)
                                        },
                                        onThreadLongPress = { thread -> threadSelection = threadSelection.select(thread.id) },
                                        listState = threadListState,
                                        emptyText = when {
                                            threadsLoading -> "Loading conversations..."
                                            !smsPermissionGranted -> "Grant SMS access to load conversations"
                                            chatFolder == ChatFolder.Spam -> "No spam messages"
                                            chatFolder == ChatFolder.Archived -> "No archived conversations"
                                            chatFolder == ChatFolder.Pinned -> "No pinned conversations"
                                            else -> "No conversations yet"
                                        },
                                    )
                                    }
                                    is InboxPane.Chat -> (selectedThread ?: lastOpenedThread)?.takeIf { it.id == pane.threadId }?.let { thread ->
                                        // Map messages only when the thread/messages/sims change (not on every
                                        // composer keystroke); merge the live composer state with a cheap copy.
                                        val screenModel = remember(thread, messages, sims) {
                                            screenMapper.map(thread, messages, sims, SmsComposerState())
                                        }.copy(composer = composerState)
                                        ConversationScreenContent(
                                            screen = screenModel,
                                            sims = sims,
                                            darkTheme = darkTheme,
                                            searchActive = conversationSearchActive,
                                            searchQuery = conversationSearchQuery,
                                            onSearchQueryChange = { conversationSearchQuery = it },
                                            matchingIds = conversationSearchMatcher.matchingIds(messages.map { SearchableMessage(it.id, it.body, it.address) }, conversationSearchQuery),
                                            onBodyChange = { composerState = composerReducer.reduce(composerState, SmsComposerEvent.BodyChanged(it)) },
                                            onSimSelected = { composerState = composerReducer.reduce(composerState, SmsComposerEvent.SubscriptionSelected(it)) },
                                            onSend = {
                                                // Honor the SIM the user picked in the composer; only fall
                                                // back to the resolver when nothing has been selected.
                                                val subscriptionId = composerState.selectedSubscriptionId
                                                    ?: resolvedComposerSim(thread)
                                                if (subscriptionId == null) {
                                                    composerState = composerState.copy(error = ComposerError.MissingSimSelection)
                                                    return@ConversationScreenContent
                                                }
                                                val readyState = if (composerState.selectedSubscriptionId != subscriptionId) {
                                                    composerState.copy(selectedSubscriptionId = subscriptionId)
                                                } else {
                                                    composerState
                                                }
                                                val state = composerReducer.reduce(readyState, SmsComposerEvent.SendRequested)
                                                composerState = state
                                                if (state.error != null) return@ConversationScreenContent
                                                val text = state.body.trim()
                                                scope.launch {
                                                    val telephonyId = repository.persistOutgoing(thread.address, text, subscriptionId)
                                                    repository.markSending(telephonyId)
                                                    repository.syncTelephonyMessagesByIds(listOf(telephonyId))
                                                    sender.send(SmsSendRequest(thread.address, text, subscriptionId, telephonyId))
                                                    composerState = composerReducer.reduce(state, SmsComposerEvent.SendCompleted)
                                                    messages = repository.loadLocalMessages(thread.id)
                                                    refreshThreadsLocal()
                                                }
                                            },
                                            onRetry = { messageId ->
                                                val message = messages.firstOrNull { it.id == messageId } ?: return@ConversationScreenContent
                                                val telephonyId = message.telephonyId ?: return@ConversationScreenContent
                                                scope.launch {
                                                    repository.markSending(telephonyId)
                                                    sender.send(SmsSendRequest(message.address, message.body, message.simSlot, telephonyId))
                                                    repository.syncTelephonyMessagesByIds(listOf(telephonyId))
                                                    messages = repository.loadLocalMessages(thread.id)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            }
                        }
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
                        AppScreen.DesktopSync -> DesktopSyncScreen(api = api, onStatus = { statusMessage = it })
                        AppScreen.Help -> HelpScreen()
                    }
                }

                // The inbox top bar slides/fades in sync with the inbox<->conversation content
                // slide (tween 300) instead of popping the instant `screen` flips.
                AnimatedVisibility(
                    visible = showShellTopBar,
                    enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 3 },
                    exit = fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -it / 3 },
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    GlassTopBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                    ) {
                        ShellTopAppBar(
                            screen = screen,
                            selectedTab = selectedTab,
                            chatFolder = chatFolder,
                            settingsCategory = settingsCategory,
                            selectedCount = if (screen == AppScreen.Main && selectedTab == MainTab.Chats) threadSelection.selectedThreadIds.size else 0,
                            selectionPinLabel = if (selectedThreadsForAction().all { it.isPinned }) "Unpin" else "Pin",
                            chatsOverflowExpanded = chatsOverflowExpanded,
                            selectionOverflowExpanded = threadSelectionOverflowExpanded,
                            onChatsOverflowDismiss = { chatsOverflowExpanded = false },
                            onChatsOverflowExpand = { chatsOverflowExpanded = true },
                            onSelectionOverflowDismiss = { threadSelectionOverflowExpanded = false },
                            onSelectionOverflowExpand = { threadSelectionOverflowExpanded = true },
                            onChatFolderSelected = { folder ->
                                chatFolder = folder
                                threadSelection = threadSelection.clear()
                                refreshThreadsLocal()
                            },
                            onOverflowAction = { action ->
                                when (action) {
                                    ChatsOverflowAction.DesktopSync -> screen = AppScreen.DesktopSync
                                    ChatsOverflowAction.HelpFeedback -> screen = AppScreen.Help
                                    ChatsOverflowAction.Compose -> {
                                        newMessageQuery = ""
                                        screen = AppScreen.NewMessage
                                        refreshContacts()
                                    }
                                }
                            },
                            onNavigateBack = { navigateBack() },
                            onSettingsBack = { screen = AppScreen.Main },
                            onSelectionClose = { threadSelection = threadSelection.clear() },
                            onSelectionAction = { threadSelectionOverflowExpanded = false; runThreadSelectionAction(it) },
                            onSelectionPin = { pinned -> threadSelectionOverflowExpanded = false; runThreadSelectionPin(pinned) },
                        )
                    }
                }

                // The conversation top bar slides in from the right with its content (and out with
                // it on back), using lastOpenedThread so it stays rendered through the exit slide.
                AnimatedVisibility(
                    visible = screen == AppScreen.Conversation,
                    enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it },
                    exit = fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it },
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    (selectedThread ?: lastOpenedThread)?.let { thread ->
                        val headerSubtitle = remember(thread, messages.size, sims) {
                            screenMapper.header(thread, messages.size, sims).subtitle
                        }
                        ConversationTopBar(
                            thread = thread,
                            headerSubtitle = headerSubtitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding(),
                            onNavigateBack = { navigateBack() },
                            onSearchToggle = { conversationSearchActive = !conversationSearchActive },
                            onCall = {
                                val uri = Uri.parse("tel:${thread.address}")
                                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                            },
                        )
                    }
                }

                // The bottom nav slides down/fades out when leaving the inbox (e.g. opening a thread)
                // instead of vanishing instantly while the content slides.
                AnimatedVisibility(
                    visible = screen == AppScreen.Main,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                    exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    KheyrBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            settingsCategory = null
                            // Selection UI is only shown on the Chats tab; clear it when leaving so the
                            // back handler doesn't swallow Back to dismiss an invisible selection.
                            threadSelection = threadSelection.clear()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
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

        showThreadMenu?.let { thread ->
            val folder = chatFolder.toThreadFolder()
            ThreadActionDialog(
                thread = thread,
                onDismiss = { showThreadMenu = null },
                onAction = { action ->
                    showThreadMenu = null
                    if (action == ThreadBulkAction.Delete) {
                        deleteThreadWithUndo(thread)
                    } else {
                        threads = ThreadListOptimisticUpdate.applyAction(threads, thread, action)
                        threads = ThreadListOptimisticUpdate.filterForFolder(threads, folder)
                        scope.launch {
                            when (action) {
                                ThreadBulkAction.MarkRead -> repository.markThreadRead(thread.id)
                                ThreadBulkAction.Archive -> repository.updateArchived(thread.id, !thread.isArchived)
                                ThreadBulkAction.MarkSpam -> repository.updateSpam(thread.id, !thread.isSpam)
                                ThreadBulkAction.Mute -> repository.updateMuted(thread.id, !thread.isMuted)
                                ThreadBulkAction.Delete -> Unit
                            }
                        }
                    }
                },
                onPin = {
                    showThreadMenu = null
                    val pinned = !thread.isPinned
                    val updated = ThreadListOptimisticUpdate.applyPin(threads, thread, pinned)
                    threads = if (folder == ThreadFolder.Inbox) {
                        ThreadSorter.inboxThreads(updated)
                    } else {
                        updated.sortedWith(
                            compareByDescending<SmsThread> { it.isPinned }
                                .thenByDescending { it.pinnedAt }
                                .thenByDescending { it.lastMessageAt },
                        )
                    }
                    threads = ThreadListOptimisticUpdate.filterForFolder(threads, folder)
                    scope.launch { repository.updatePinned(thread.id, pinned) }
                },
            )
        }
    }
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
    val steps = OnboardingCopy.steps
    val stepInfo = steps.getOrElse(step) { steps.last() }
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(
                progress = { (step + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stepInfo.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${step + 1} / ${steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KheyrMark(size = 96.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stepInfo.headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stepInfo.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stepInfo.detail,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            when (step) {
                1 -> OnboardingStatusRow(
                    complete = gate.isDefaultSmsApp,
                    completeLabel = "Default SMS role granted",
                    pendingLabel = "Kheyr is not the default SMS app yet",
                )
                2 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OnboardingPermissionRow("SMS access", gate.smsPermissionGranted)
                    OnboardingPermissionRow("Contacts", gate.contactsPermissionGranted)
                    OnboardingPermissionRow("Notifications", gate.notificationPermissionGranted)
                }
                3 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = otpPhone,
                        onValueChange = onOtpPhoneChange,
                        label = { Text("Phone (+E.164)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = onOtpCodeChange,
                        label = { Text("Verification code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(
                        "Backend URL: ${ApiConfig.baseUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        OnboardingBottomBar(
            step = step,
            gate = gate,
            otpCode = otpCode,
            onStepChange = onStepChange,
            onRequestDefault = onRequestDefault,
            onRequestPermissions = onRequestPermissions,
            onSkipSync = onSkipSync,
            onEnableSync = onEnableSync,
            onVerifyOtp = onVerifyOtp,
            onRequestOtp = onRequestOtp,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun OnboardingStatusRow(complete: Boolean, completeLabel: String, pendingLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (complete) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (complete) completeLabel else pendingLabel,
            color = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OnboardingPermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(
            if (granted) "Granted" else "Needed",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun OnboardingBottomBar(
    step: Int,
    gate: OnboardingGateState,
    otpCode: String,
    onStepChange: (Int) -> Unit,
    onRequestDefault: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSkipSync: () -> Unit,
    onEnableSync: () -> Unit,
    onVerifyOtp: () -> Unit,
    onRequestOtp: () -> Unit,
    onFinish: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (step) {
                0 -> Button(onClick = { onStepChange(1) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
                1 -> {
                    if (!gate.isDefaultSmsApp) {
                        OutlinedButton(onClick = onRequestDefault, modifier = Modifier.fillMaxWidth()) {
                            Text("Make default SMS app")
                        }
                    }
                    Button(
                        onClick = { onStepChange(2) },
                        enabled = gate.isDefaultSmsApp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue")
                    }
                }
                2 -> {
                    if (gate.missingRequirements.isNotEmpty()) {
                        OutlinedButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant permissions")
                        }
                    }
                    Button(
                        onClick = { onStepChange(3) },
                        enabled = gate.canUseFullSmsFeatures,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue")
                    }
                }
                3 -> {
                    OutlinedButton(onClick = onRequestOtp, modifier = Modifier.fillMaxWidth()) {
                        Text("Send OTP")
                    }
                    if (otpCode.isNotBlank()) {
                        OutlinedButton(onClick = onVerifyOtp, modifier = Modifier.fillMaxWidth()) {
                            Text("Verify OTP")
                        }
                    }
                    Button(onClick = onEnableSync, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable sync")
                    }
                    TextButton(onClick = onSkipSync, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip for now")
                    }
                }
                else -> Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Open inbox")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ThreadFolderScreen(
    threads: List<SmsThread>,
    folder: ThreadFolder,
    showFilters: Boolean,
    threadListFilter: ThreadListFilter,
    onThreadListFilterChange: (ThreadListFilter) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sims: List<SimCard>,
    mapper: ThreadRowPresentationMapper,
    loading: Boolean,
    selectedThreadIds: Set<Long>,
    onThreadClick: (SmsThread) -> Unit,
    onThreadLongPress: (SmsThread) -> Unit,
    listState: LazyListState,
    emptyText: String,
) {
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(topInset))
        KheyrSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = "Search threads",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (showFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThreadListFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = threadListFilter == filter,
                        onClick = { onThreadListFilterChange(filter) },
                        label = { Text(threadListFilterLabel(filter)) },
                    )
                }
            }
        }
        if (loading && threads.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (threads.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(emptyText) }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                items(threads, key = { it.id }) { thread ->
                    // Memoize the pure presentation/date derivations so they don't recompute on every
                    // row recomposition (e.g. toggling multi-select, which changes selectedThreadIds).
                    val row = remember(thread, folder, sims) { mapper.map(thread, folder, sims) }
                    val timeLabel = remember(thread.lastMessageAt) { formatMessageTime(thread.lastMessageAt) }
                    TelegramStyleThreadRow(
                        title = row.title,
                        preview = row.preview,
                        timeLabel = timeLabel,
                        photoUri = thread.contactPhotoUri,
                        unreadBadge = row.unreadBadge,
                        showPinned = row.showPinned,
                        showMuted = row.showMuted,
                        simBadge = row.simBadge,
                        showSpamBadge = row.showSpamBadge,
                        onClick = { onThreadClick(thread) },
                        onLongClick = { onThreadLongPress(thread) },
                        selected = thread.id in selectedThreadIds,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationScreenContent(
    screen: ConversationScreenModel,
    sims: List<SimCard>,
    darkTheme: Boolean,
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
    val topInset = KheyrChromeInsets.shellTop()

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
            KheyrSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search in conversation",
                modifier = Modifier
                    .padding(top = topInset, start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .focusRequester(searchFocusRequester),
                focusRequester = searchFocusRequester,
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = if (searchActive) 8.dp else topInset,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleMessages, key = { it.id }) { row ->
                ConversationBubbleRow(
                    row = row,
                    darkTheme = darkTheme,
                    highlight = if (row.id in matchingIds) highlightQuery else null,
                    onRetry = { onRetry(row.id) },
                )
            }
        }
        TelegramStyleComposer(
            body = screen.composer.body,
            sims = sims,
            selectedSubscriptionId = screen.composer.selectedSubscriptionId,
            sending = screen.composer.sending,
            error = screen.composer.error?.name,
            onBodyChange = onBodyChange,
            onSimSelected = onSimSelected,
            onSend = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationTopBar(
    thread: SmsThread,
    headerSubtitle: String?,
    onNavigateBack: () -> Unit,
    onSearchToggle: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassTopBar(modifier = modifier) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContactAvatar(
                        displayName = thread.displayName.ifBlank { thread.address },
                        photoUri = thread.contactPhotoUri,
                        size = 32.dp,
                    )
                    Column {
                        Text(thread.displayName.ifBlank { thread.address })
                        headerSubtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            actions = {
                IconButton(onClick = onSearchToggle) { Icon(Icons.Default.Search, "Search") }
                IconButton(onClick = onCall) { Icon(Icons.Default.Call, "Call") }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}

@Composable
private fun SettingsListScreen(onCategoryClick: (SettingsCategory) -> Unit) {
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    val grouped = SettingsCategoryOrder.ordered.groupBy { category ->
        when (category) {
            SettingsCategory.Notifications, SettingsCategory.UnknownSenders -> "Notifications"
            SettingsCategory.SpamProtection -> "Privacy"
            SettingsCategory.DualSim -> "Messaging"
            SettingsCategory.Sync, SettingsCategory.DesktopDevices -> "Cloud"
            SettingsCategory.PrivacySecurity -> "Security"
            SettingsCategory.Appearance, SettingsCategory.About -> "General"
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(top = topInset, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        grouped.forEach { (sectionTitle, categories) ->
            item(key = sectionTitle) {
                SettingsSection(title = sectionTitle) {
                    categories.forEach { category ->
                        SettingsRow(
                            title = category.name.replace(Regex("([a-z])([A-Z])"), "$1 $2"),
                            subtitle = settingsCategoryDescription(category),
                            onClick = { onCategoryClick(category) },
                        )
                    }
                }
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
    val topInset = KheyrChromeInsets.shellTop()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(top = topInset, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                // steps is the count of points BETWEEN the ends, so 29 yields 31 integer ticks
                // (0..30 at spacing 1.0); roundToInt avoids the truncation bias of toInt().
                Slider(value = spamAutoDeleteDays.toFloat(), onValueChange = { onSpamAutoDeleteChange(it.roundToInt()) }, valueRange = 0f..30f, steps = 29)
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
            SettingsCategory.DesktopDevices -> Text("Manage paired desktop devices from the Chats overflow menu.")
            SettingsCategory.PrivacySecurity -> {
                Button(onClick = onDeleteCloudData, modifier = Modifier.fillMaxWidth()) { Text("Delete cloud data") }
                OutlinedButton(onClick = onExportCloudData, modifier = Modifier.fillMaxWidth()) { Text("Export cloud data") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Direct messages"); Switch(checked = directMessagesEnabled, onCheckedChange = onDirectMessagesChange) }
            }
            SettingsCategory.Appearance -> ThemePreference.entries.forEach { pref ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(pref.name); RadioButton(selected = themePreference == pref, onClick = { onThemeChange(pref) }) }
            }
            SettingsCategory.About -> {
                KheyrBrandHeader(subtitle = "Version 0.1.0")
                Text("Modern SMS with spam filtering, sync, and desktop relay.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    context.safeStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                }) { Text("Privacy policy") }
            }
        }
    }
}

@Composable
private fun DesktopSyncScreen(api: KheyrApiService, onStatus: (String) -> Unit) {
    val topInset = KheyrChromeInsets.shellTop()
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<RemoteDevice>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    fun refresh() {
        if (!ApiConfig.isConfigured) return
        loading = true
        scope.launch(Dispatchers.IO) {
            val result = api.listDevices().filter { it.isDesktop }
            withContext(Dispatchers.Main) { devices = result; loading = false }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw == null) {
            onStatus("Pairing cancelled")
            return@rememberLauncherForActivityResult
        }
        when (val parsed = PairingQrValidator.validate(PairingQrCodec.parse(raw), System.currentTimeMillis() / 1000)) {
            is PairingQrResult.Invalid -> onStatus(parsed.reason)
            is PairingQrResult.Valid -> scope.launch(Dispatchers.IO) {
                val ok = api.approvePairing(parsed.sessionId, "Desktop") != null
                withContext(Dispatchers.Main) {
                    onStatus(if (ok) "Desktop paired" else "Pairing failed — check connection")
                    if (ok) refresh()
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(top = topInset, start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pair a desktop", style = MaterialTheme.typography.titleMedium)
        Text(
            "Scan the QR code shown on the Kheyr desktop app to let it read and send SMS through this phone.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                if (!ApiConfig.isConfigured) {
                    onStatus("Configure the backend URL first")
                    return@Button
                }
                scanLauncher.launch(ScanOptions().setOrientationLocked(false).setBeepEnabled(false).setPrompt("Scan the desktop pairing QR"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Scan pairing QR") }

        Text("Paired desktops", style = MaterialTheme.typography.titleSmall)
        when {
            !ApiConfig.isConfigured -> Text("Configure the backend URL to manage paired devices.", style = MaterialTheme.typography.bodySmall)
            loading && devices.isEmpty() -> Text("Loading…", style = MaterialTheme.typography.bodySmall)
            devices.isEmpty() -> Text("No desktops paired yet.", style = MaterialTheme.typography.bodySmall)
            else -> devices.forEach { device ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(device.name.ifBlank { "Desktop" }, style = MaterialTheme.typography.bodyLarge)
                        Text(device.platform, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val ok = api.revokeDevice(device.id)
                            withContext(Dispatchers.Main) {
                                onStatus(if (ok) "Device revoked" else "Revoke failed")
                                if (ok) refresh()
                            }
                        }
                    }) { Text("Revoke") }
                }
            }
        }
    }
}

@Composable
private fun HelpScreen() {
    val topInset = KheyrChromeInsets.shellTop()
    val context = LocalContext.current
    val help = HelpFeedbackModel(helpUrl = "https://kheyr.app/help", supportEmail = "support@kheyr.app")
    Column(Modifier.padding(top = topInset, start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Help & Feedback", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = { context.safeStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(help.helpUrl))) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open help center") }
        OutlinedButton(
            onClick = { context.safeStartActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${help.supportEmail}"))) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Email support") }
        Text("Help center: ${help.helpUrl}", style = MaterialTheme.typography.labelSmall)
        Text("Email: ${help.supportEmail}", style = MaterialTheme.typography.labelSmall)
    }
}

private const val PRIVACY_POLICY_URL = "https://kheyr.app/privacy"

/** Clears all signed-in state locally (tokens, phone, sync device) and routes back to onboarding. */
private fun clearLocalSession(preferences: AppPreferences) {
    preferences.clearAuthTokens()
    preferences.userPhone = null
    preferences.saveSyncSettings(preferences.syncSettings().copy(enabled = false, deviceId = null))
    preferences.onboardingComplete = false
}

/** Launches an intent, ignoring the case where no app can handle it (avoids ActivityNotFoundException crashes). */
private fun android.content.Context.safeStartActivity(intent: Intent) {
    runCatching { startActivity(intent) }
}


@Composable
private fun NewMessageScreen(
    contacts: List<DeviceContact>,
    loading: Boolean,
    hasPermission: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onRecipientSelected: (NewMessageRecipient) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val topInset = KheyrChromeInsets.shellTop()
    val model = remember(contacts) { NewMessageViewModel(contacts) }
    val state = remember(model, query) { model.stateFor(query) }

    Column(Modifier.fillMaxSize().padding(top = topInset)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("New message", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        KheyrSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Name or phone number",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (!hasPermission) {
            Text(
                "Grant contacts access to search your address book, or type a phone number to continue.",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRequestPermission, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Grant contacts access") }
        }
        state.manualAddress?.let { address ->
            ListItem(
                headlineContent = { Text("Send to $address") },
                supportingContent = { Text("Use typed phone number") },
                leadingContent = { Icon(Icons.Default.Send, contentDescription = null) },
                modifier = Modifier.clickable { model.manualRecipient(query)?.let(onRecipientSelected) },
            )
        }
        if (loading && contacts.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.matches.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (query.isBlank()) "Type a name or phone number" else "No matching contacts")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = KheyrChromeInsets.bottomNav())) {
                items(state.matches, key = { "${it.id}:${it.phoneNumber}" }) { contact ->
                    TelegramStyleContactRow(
                        displayName = contact.displayName,
                        phoneNumber = contact.phoneNumber,
                        photoUri = contact.photoUri,
                        onClick = { onRecipientSelected(model.recipientFor(contact)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsScreen(
    contacts: List<DeviceContact>,
    loading: Boolean,
    hasPermission: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onContactClick: (DeviceContact) -> Unit,
    listState: LazyListState,
) {
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    val filteredContacts = remember(contacts, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.displayName.contains(query, ignoreCase = true) ||
                    contact.phoneNumber.contains(query, ignoreCase = true)
            }
        }
    }

    when {
        !hasPermission -> {
            Column(
                Modifier.fillMaxSize().padding(top = topInset, start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Contacts permission is required to show your address book.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestPermission) { Text("Grant contacts access") }
            }
        }
        loading && contacts.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(top = topInset), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        contacts.isEmpty() -> {
            Box(Modifier.fillMaxSize().padding(top = topInset), contentAlignment = Alignment.Center) {
                Text("No contacts with phone numbers found.")
            }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset))
                KheyrSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search contacts",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (filteredContacts.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No contacts match your search")
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = bottomInset),
                    ) {
                        items(filteredContacts, key = { "${it.id}:${it.phoneNumber}" }) { contact ->
                            TelegramStyleContactRow(
                                displayName = contact.displayName,
                                phoneNumber = contact.phoneNumber,
                                photoUri = contact.photoUri,
                                onClick = { onContactClick(contact) },
                            )
                        }
                    }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                ThreadActionMenuItem(
                    icon = Icons.Default.Star,
                    label = if (thread.isPinned) "Unpin" else "Pin",
                    onClick = onPin,
                )
                ThreadActionMenuItem(
                    icon = Icons.Default.Check,
                    label = "Mark read",
                    onClick = { onAction(ThreadBulkAction.MarkRead) },
                )
                ThreadActionMenuItem(
                    icon = Icons.Default.List,
                    label = if (thread.isArchived) "Unarchive" else "Archive",
                    onClick = { onAction(ThreadBulkAction.Archive) },
                )
                ThreadActionMenuItem(
                    icon = Icons.Default.Warning,
                    label = if (thread.isSpam) "Not spam" else "Mark spam",
                    onClick = { onAction(ThreadBulkAction.MarkSpam) },
                )
                ThreadActionMenuItem(
                    icon = Icons.Default.Notifications,
                    label = if (thread.isMuted) "Unmute" else "Mute",
                    onClick = { onAction(ThreadBulkAction.Mute) },
                )
                ThreadActionMenuItem(
                    icon = Icons.Default.Delete,
                    label = "Delete messages",
                    onClick = { onAction(ThreadBulkAction.Delete) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ThreadActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label)
        }
    }
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS); add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.READ_CONTACTS); add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun formatMessageTime(instant: Instant): String = JalaliDateFormatter.format(instant)

private fun threadListFilterLabel(filter: ThreadListFilter): String = when (filter) {
    ThreadListFilter.All -> "All"
    ThreadListFilter.Unread -> "Unread"
    ThreadListFilter.Contacts -> "Contacts"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopAppBar(
    screen: AppScreen,
    selectedTab: MainTab,
    chatFolder: ChatFolder,
    settingsCategory: SettingsCategory?,
    selectedCount: Int,
    selectionPinLabel: String,
    chatsOverflowExpanded: Boolean,
    selectionOverflowExpanded: Boolean,
    onChatsOverflowDismiss: () -> Unit,
    onChatsOverflowExpand: () -> Unit,
    onSelectionOverflowDismiss: () -> Unit,
    onSelectionOverflowExpand: () -> Unit,
    onChatFolderSelected: (ChatFolder) -> Unit,
    onOverflowAction: (ChatsOverflowAction) -> Unit,
    onNavigateBack: () -> Unit,
    onSettingsBack: () -> Unit,
    onSelectionClose: () -> Unit,
    onSelectionAction: (ThreadBulkAction) -> Unit,
    onSelectionPin: (Boolean) -> Unit,
) {
    TopAppBar(
        title = {
            if (selectedCount > 0) {
                Text("$selectedCount selected")
            } else Text(
                when (screen) {
                    AppScreen.SettingsDetail -> settingsCategory?.name?.replace(Regex("([a-z])([A-Z])"), "$1 $2") ?: "Settings"
                    AppScreen.DesktopSync -> "Desktop Sync"
                    AppScreen.Help -> "Help & Feedback"
                    AppScreen.Main -> when (selectedTab) {
                        MainTab.Chats -> chatFolder.title
                        MainTab.Contacts -> "Contacts"
                        MainTab.Settings -> "Settings"
                        MainTab.Profile -> "Profile"
                    }
                    else -> "Kheyr"
                },
            )
        },
        navigationIcon = {
            when (screen) {
                AppScreen.SettingsDetail -> IconButton(onClick = onSettingsBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                AppScreen.DesktopSync, AppScreen.Help -> IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                else -> if (selectedCount > 0) IconButton(onClick = onSelectionClose) { Icon(Icons.Default.Close, "Clear selection") }
            }
        },
        actions = {
            if (selectedCount > 0) {
                IconButton(onClick = { onSelectionAction(ThreadBulkAction.MarkSpam) }) { Icon(Icons.Default.Warning, "Mark spam") }
                IconButton(onClick = { onSelectionAction(ThreadBulkAction.Archive) }) { Icon(Icons.Default.List, "Archive") }
                IconButton(onClick = { onSelectionAction(ThreadBulkAction.Delete) }) { Icon(Icons.Default.Delete, "Delete") }
                Box {
                    IconButton(onClick = onSelectionOverflowExpand) { Icon(Icons.Default.MoreVert, "More selection actions") }
                    DropdownMenu(expanded = selectionOverflowExpanded, onDismissRequest = onSelectionOverflowDismiss) {
                        DropdownMenuItem(text = { Text(selectionPinLabel) }, onClick = { onSelectionPin(selectionPinLabel == "Pin") })
                        DropdownMenuItem(text = { Text("Mark read") }, onClick = { onSelectionAction(ThreadBulkAction.MarkRead) })
                        DropdownMenuItem(text = { Text("Mute") }, onClick = { onSelectionAction(ThreadBulkAction.Mute) })
                    }
                }
            } else if (screen == AppScreen.Main && selectedTab == MainTab.Chats) {
                Box {
                    IconButton(onClick = onChatsOverflowExpand) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = chatsOverflowExpanded,
                        onDismissRequest = onChatsOverflowDismiss,
                    ) {
                        MainNavigationModel.chatFolders().forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.title) },
                                onClick = {
                                    onChatsOverflowDismiss()
                                    onChatFolderSelected(folder)
                                },
                                leadingIcon = {
                                    if (chatFolder == folder) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                            )
                        }
                        HorizontalDivider()
                        MainNavigationModel.overflowActions().forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.title) },
                                onClick = {
                                    onChatsOverflowDismiss()
                                    onOverflowAction(action)
                                },
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
    )
}
