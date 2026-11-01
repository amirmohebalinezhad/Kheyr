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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kheyr.sms.KheyrApplication
import com.kheyr.sms.R
import com.kheyr.sms.SendToRequest
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
import com.kheyr.sms.data.AppDatabase
import com.kheyr.sms.data.SmsMessage
import com.kheyr.sms.data.SmsRefreshEvents
import com.kheyr.sms.data.SmsRepository
import com.kheyr.sms.data.SmsThread
import com.kheyr.sms.domain.ThreadSorter
import com.kheyr.sms.onboarding.DefaultRoleMonitorState
import com.kheyr.sms.onboarding.DefaultSmsRoleChecker
import com.kheyr.sms.onboarding.OnboardingGateState
import com.kheyr.sms.onboarding.OnboardingCopy
import com.kheyr.sms.preferences.AppPreferences
import androidx.core.app.NotificationManagerCompat
import com.kheyr.sms.receiver.ActiveConversation
import com.kheyr.sms.receiver.IncomingSmsNotifications
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
import com.kheyr.sms.telephony.ThreadIdResolver
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
private data class PendingThreadDelete(
    val threadId: Long,
    // Captured up front: once the Room rows are gone there is no way to look these up again, and they
    // are what the deferred system-SMS-store delete needs.
    val telephonyIds: List<Long>,
)

// The Scaffold's content padding is deliberately ignored: this screen is edge-to-edge and places its
// own chrome (top bar, bottom nav, conversation bar) as overlays inside a Box, insetting each one via
// KheyrChromeInsets / statusBarsPadding / navigationBarsPadding. Consuming the Scaffold padding as well
// would inset the same system bars twice.
@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KheyrAppShell(
    openThreadId: Long? = null,
    onThreadConsumed: () -> Unit = {},
    sendTo: SendToRequest? = null,
    onSendToConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as KheyrApplication
    val preferences = app.preferences
    val repository = remember { SmsRepository(context) }
    val contactRepository = remember { ContactRepository(context) }
    val simRepository = remember { SimRepository(context) }
    val sender = remember { SmsSender(context) }
    // create() wires the refresh-token retry and token persistence, so every screen here survives
    // the 60-minute access-token lifetime instead of silently failing after an hour (B-04).
    val api = remember { KheyrApiService.create(preferences) }
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
    // Losing the default-SMS role or READ_SMS after onboarding used to leave the inbox with bare
    // text and no way back (B-16); these drive a dismissible recovery banner above the thread list.
    var accessBannerDismissed by remember { mutableStateOf(false) }
    var roleMonitor by remember { mutableStateOf(DefaultRoleMonitorState(isDefaultSms, isDefaultSms)) }
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val lifecycle = activity?.lifecycle ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val nowDefaultSms = DefaultSmsRoleChecker.isDefaultSmsApp(context)
                    val monitor = DefaultRoleMonitorState(wasDefaultSmsApp = isDefaultSms, isDefaultSmsApp = nowDefaultSms)
                    roleMonitor = monitor
                    isDefaultSms = nowDefaultSms
                    smsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                    contactsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    // A role that disappeared while we were in the background is new information, and
                    // once everything is granted again the next loss has to be able to warn afresh —
                    // in both cases an earlier dismissal is stale.
                    if (monitor.shouldWarnRoleRemoved || (isDefaultSms && smsPermissionGranted)) accessBannerDismissed = false
                    // The receive pipeline reads this from a broadcast thread; restore it on resume so
                    // the conversation on screen keeps suppressing its own notifications (B-33).
                    ActiveConversation.setOpen(selectedThread?.id)
                    if (smsPermissionGranted) {
                        sims = simRepository.activeSims()
                        resumeNonce++
                    }
                }
                // Backgrounded means nothing is on screen, so every incoming message may notify again.
                Lifecycle.Event.ON_STOP -> ActiveConversation.setOpen(null)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            ActiveConversation.setOpen(null)
        }
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
    val inboxSearchFocusRequester = remember { FocusRequester() }

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
        // Tell the receive pipeline which chat is on screen so it skips notifying for it, and clear
        // any notification already posted for this thread. Suppressing new ones is only half of
        // B-33: without the cancel, a notification raised a moment earlier stays on screen for a
        // conversation the user is now reading, and suppression means nothing will ever replace it.
        ActiveConversation.setOpen(thread.id)
        NotificationManagerCompat.from(context).cancel(IncomingSmsNotifications.notificationId(thread.id))
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
        val target = if (existing != null) {
            existing.copy(
                displayName = recipient.displayName.ifBlank { existing.displayName },
                contactPhotoUri = recipient.photoUri ?: existing.contactPhotoUri,
            )
        } else {
            SmsThread(
                id = ThreadIdResolver.getOrCreateThreadId(context, recipient.address),
                address = recipient.address,
                displayName = recipient.displayName.ifBlank { recipient.address },
                lastMessage = "",
                lastMessageAt = Instant.now(),
                contactPhotoUri = recipient.photoUri,
            )
        }
        openConversation(target)
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
                ActiveConversation.setOpen(null)
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
        // The Room rows already went at delete time so the list updated immediately. The system SMS
        // store is only touched here, once the undo window has closed - that split is what makes Undo
        // able to give the messages back at all (B-14).
        repository.deleteTelephonyMessages(pending.telephonyIds)
    }

    fun deleteThreadWithUndo(thread: SmsThread) {
        scope.launch {
            commitPendingDelete()
            val folder = chatFolder.toThreadFolder()
            val snapshot = repository.loadLocalMessageEntities(thread.id)
            val snapshotIds = snapshot.map { it.id }
            pendingDelete = PendingThreadDelete(thread.id, snapshot.mapNotNull { it.telephonyId })
            // Room only, so the optimistic removal matches persisted state while the system SMS rows
            // stay put until the undo window closes. Only these ids are removed, so messages that
            // arrive during the window survive; Undo re-inserts the snapshot as the sole copies
            // instead of duplicating still-present rows.
            repository.deleteLocalMessagesByIds(snapshotIds)
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

    // Shared by onboarding step 1 and the post-onboarding recovery banner (B-16), so both offer the
    // same route back to the default-SMS role.
    fun requestDefaultSmsRole() {
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
    }

    LaunchedEffect(chatFolder, screen, selectedTab) {
        if (screen == AppScreen.Main && selectedTab == MainTab.Chats) {
            // One sequenced reload (quick local load, then sync, then reload) instead of two
            // concurrent launches that raced to assign `threads`. The telephony sync reads the SMS
            // provider, so it is gated on SMS permission — otherwise a revoked READ_SMS (e.g. after
            // the user changes the default SMS app) would throw SecurityException here and crash on open.
            launchThreadReload {
                threads = loadThreadsForFolder()
                if (smsPermissionGranted) {
                    repository.syncTelephonyMessages()
                    threads = loadThreadsForFolder()
                }
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
                        // Only mark read when the conversation is genuinely ON SCREEN. selectedThread
                        // survives backgrounding - the composition is not destroyed by ON_STOP - so
                        // keying this off it alone marked messages that arrived while the user was in
                        // another app as read, in Room and in the system store, before they had seen
                        // them. ActiveConversation is cleared on ON_STOP, which is exactly the
                        // distinction needed here.
                        if (ActiveConversation.isOpen(event.threadId)) {
                            repository.markThreadRead(event.threadId)
                        }
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

    // "Message" from a contact card, the dialer, a web sms:/smsto: link, or our own copy-menu action
    // lands here; open (or create) that recipient's conversation instead of dumping the user on the
    // inbox, prefilling any shared body (B-12).
    LaunchedEffect(sendTo) {
        val request = sendTo ?: return@LaunchedEffect
        if (!smsPermissionGranted || !preferences.onboardingComplete) {
            onSendToConsumed()
            return@LaunchedEffect
        }
        val address = request.address
        if (address == null) {
            // A bare "sms:" carries no recipient, so let the user pick one rather than do nothing.
            newMessageQuery = ""
            screen = AppScreen.NewMessage
        } else {
            openConversationForRecipient(NewMessageRecipient(displayName = "", address = address))
            request.body?.let { body ->
                composerState = composerReducer.reduce(composerState, SmsComposerEvent.BodyChanged(body))
            }
        }
        onSendToConsumed()
    }

    val showShellTopBar = screen != AppScreen.Onboarding && screen != AppScreen.Conversation && screen != AppScreen.NewMessage

    // Toasts should feel like a glance, not a modal: fade out on their own while keeping the
    // manual dismiss affordance for anything the user needs to re-read.
    LaunchedEffect(statusMessage) {
        if (statusMessage == null) return@LaunchedEffect
        kotlinx.coroutines.delay(5000)
        statusMessage = null
    }

    KheyrTheme(themePreference = themePreference) {
        Scaffold(
            snackbarHost = {
                KheyrSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 92.dp),
                )
            },
            containerColor = Color.Transparent,
        ) {
            KheyrScreenBackground {
                Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        AppScreen.Onboarding -> KheyrOnboardingFlow(
                            step = onboardingStep,
                            gate = gateState(),
                            // Sync uploads to an account, so the toggle stays unavailable until the
                            // phone number is verified (B-03).
                            signedIn = preferences.authTokens().first != null,
                            otpPhone = otpPhone,
                            otpCode = otpCode,
                            appDarkTheme = darkTheme,
                            onStepChange = { onboardingStep = it },
                            onOtpPhoneChange = { otpPhone = it },
                            onOtpCodeChange = { otpCode = it },
                            onRequestDefault = { requestDefaultSmsRole() },
                            onRequestPermissions = { permissionLauncher.launch(requiredPermissions()) },
                            onOpenSettings = { openAppPermissionSettings() },
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
                                        if (tokens != null || !ApiConfig.isConfigured) {
                                            if (onboardingStep == 3) onboardingStep = 4
                                            else onboardingStep = 3
                                        }
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
                                        onCompose = {
                                            newMessageQuery = ""
                                            screen = AppScreen.NewMessage
                                        },
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
                                                clearLocalSession(context, preferences)
                                                withContext(Dispatchers.Main) {
                                                    realtimeClient?.stop()
                                                    statusMessage = "Signed out"
                                                    // Sync is off now, so stop its worker and land back on the
                                                    // inbox instead of replaying onboarding (B-20).
                                                    syncEnabled = false
                                                    KheyrWorkerScheduler.scheduleAll(context, false)
                                                    screen = AppScreen.Main
                                                    selectedTab = MainTab.Chats
                                                }
                                            }
                                        },
                                        onDeleteAccount = {
                                            scope.launch(Dispatchers.IO) {
                                                api.deleteAccount()
                                                clearLocalSession(context, preferences)
                                                withContext(Dispatchers.Main) {
                                                    realtimeClient?.stop()
                                                    statusMessage = "Account deleted"
                                                    // Sync is off now, so stop its worker and land back on the
                                                    // inbox instead of replaying onboarding (B-20).
                                                    syncEnabled = false
                                                    KheyrWorkerScheduler.scheduleAll(context, false)
                                                    screen = AppScreen.Main
                                                    selectedTab = MainTab.Chats
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
                                        banner = {
                                            // Losing the role or SMS permission after onboarding used to be a
                                            // dead end: the list just said "Grant SMS access" with no action (B-16).
                                            if (preferences.onboardingComplete && !accessBannerDismissed && (!isDefaultSms || !smsPermissionGranted)) {
                                                AccessRecoveryBanner(
                                                    isDefaultSms = isDefaultSms,
                                                    smsPermissionGranted = smsPermissionGranted,
                                                    roleWasRemoved = roleMonitor.shouldWarnRoleRemoved,
                                                    onMakeDefault = { requestDefaultSmsRole() },
                                                    onGrantPermission = { permissionLauncher.launch(requiredPermissions()) },
                                                    onDismiss = { accessBannerDismissed = true },
                                                )
                                            }
                                        },
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
                                        onThreadLongPress = { thread ->
                                            // First long-press starts multi-select as before; a second one on the
                                            // same (only) selected row opens its per-thread menu, which is the
                                            // dialog's only trigger and therefore the only route to Block (B-19).
                                            if (threadSelection.selectedThreadIds == setOf(thread.id)) {
                                                showThreadMenu = thread
                                            } else {
                                                threadSelection = threadSelection.select(thread.id)
                                            }
                                        },
                                        listState = threadListState,
                                        searchFocusRequester = inboxSearchFocusRequester,
                                        onCompose = {
                                            newMessageQuery = ""
                                            screen = AppScreen.NewMessage
                                            refreshContacts()
                                        },
                                        showFab = snackbarHostState.currentSnackbarData == null,
                                        emptyTitle = when {
                                            !smsPermissionGranted -> "SMS access needed"
                                            searchQuery.isNotBlank() -> "No results"
                                            chatFolder == ChatFolder.Spam -> "Spam is quiet"
                                            chatFolder == ChatFolder.Archived -> "Nothing archived"
                                            chatFolder == ChatFolder.Pinned -> "No pinned chats"
                                            else -> "A quiet inbox"
                                        },
                                        emptyMessage = when {
                                            !smsPermissionGranted -> "Allow Kheyr to read your messages so your conversations can show up here."
                                            searchQuery.isNotBlank() -> "No messages match “$searchQuery”."
                                            chatFolder == ChatFolder.Spam -> "Messages that look like spam will be filed here, away from your inbox."
                                            chatFolder == ChatFolder.Archived -> "Conversations you archive to clear your inbox will live here."
                                            chatFolder == ChatFolder.Pinned -> "Pin the conversations you never want to lose."
                                            else -> "Messages you send and receive will appear here, beautifully organized."
                                        },
                                        emptyActionLabel = when {
                                            !smsPermissionGranted -> "Grant access"
                                            searchQuery.isNotBlank() -> "Clear search"
                                            chatFolder == ChatFolder.All -> "Start a conversation"
                                            else -> "Back to inbox"
                                        },
                                        onEmptyAction = when {
                                            !smsPermissionGranted -> ({ permissionLauncher.launch(requiredPermissions()) })
                                            searchQuery.isNotBlank() -> ({ searchQuery = "" })
                                            chatFolder == ChatFolder.All -> ({
                                                newMessageQuery = ""
                                                screen = AppScreen.NewMessage
                                                refreshContacts()
                                            })
                                            else -> ({ chatFolder = ChatFolder.All; refreshThreadsLocal() })
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
                                                if (state.error != null) {
                                                    composerState = state
                                                    return@ConversationScreenContent
                                                }
                                                if (!isDefaultSms) {
                                                    // A non-default SMS app can't write to the system SMS store, so the
                                                    // send would throw. Guide the user back to the role instead of crashing.
                                                    // The sending=true state is deliberately NOT published here: nothing
                                                    // would ever clear it and Send would stay disabled forever (B-09).
                                                    statusMessage = "Set Kheyr as your default SMS app to send messages."
                                                    return@ConversationScreenContent
                                                }
                                                composerState = state
                                                val text = state.body.trim()
                                                scope.launch {
                                                    // Hoisted so the catch can resolve a row that was already persisted.
                                                    var persistedTelephonyId: Long? = null
                                                    try {
                                                        val telephonyId = repository.persistOutgoing(thread.address, text, subscriptionId)
                                                        persistedTelephonyId = telephonyId
                                                        repository.markSending(telephonyId)
                                                        repository.syncTelephonyMessagesByIds(listOf(telephonyId))
                                                        sender.send(SmsSendRequest(thread.address, text, subscriptionId, telephonyId))
                                                        composerState = composerReducer.reduce(state, SmsComposerEvent.SendCompleted)
                                                        messages = repository.loadLocalMessages(thread.id)
                                                        refreshThreadsLocal()
                                                    } catch (t: Exception) {
                                                        if (t is kotlinx.coroutines.CancellationException) throw t
                                                        // The row is already stored as "sending", but send() threw before the
                                                        // telephony stack accepted it, so no PendingIntent was ever registered
                                                        // and SmsSendStatusReceiver will never resolve it. Left alone the bubble
                                                        // spins on "Sending" forever with no Retry, because showRetry is driven
                                                        // by MessageStatus.Failed. Mark it failed so the user can retry.
                                                        persistedTelephonyId?.let { id ->
                                                            runCatching { withContext(Dispatchers.IO) { repository.markFailed(id) } }
                                                        }
                                                        // BodyChanged clears `error` but not `sending`, so without this the
                                                        // Send button stays disabled until the chat is reopened (B-09).
                                                        composerState = composerReducer.reduce(composerState, SmsComposerEvent.SendFailed)
                                                        messages = repository.loadLocalMessages(thread.id)
                                                        statusMessage = "Couldn't send the message. Make sure Kheyr is your default SMS app."
                                                    }
                                                }
                                            },
                                            onRetry = { messageId ->
                                                val message = messages.firstOrNull { it.id == messageId } ?: return@ConversationScreenContent
                                                val telephonyId = message.telephonyId ?: return@ConversationScreenContent
                                                if (!isDefaultSms) {
                                                    statusMessage = "Set Kheyr as your default SMS app to send messages."
                                                    return@ConversationScreenContent
                                                }
                                                scope.launch {
                                                    try {
                                                        repository.markSending(telephonyId)
                                                        sender.send(SmsSendRequest(message.address, message.body, message.simSlot, telephonyId))
                                                        repository.syncTelephonyMessagesByIds(listOf(telephonyId))
                                                        messages = repository.loadLocalMessages(thread.id)
                                                    } catch (t: Exception) {
                                                        if (t is kotlinx.coroutines.CancellationException) throw t
                                                        // send() throws on an undialable recipient or a revoked SEND_SMS;
                                                        // uncaught here it would kill the process (B-10). markSending has
                                                        // already moved the row out of Failed, so put it back: otherwise one
                                                        // failed retry turns a retryable message into a permanent "Sending".
                                                        runCatching { withContext(Dispatchers.IO) { repository.markFailed(telephonyId) } }
                                                        composerState = composerReducer.reduce(composerState, SmsComposerEvent.SendFailed)
                                                        messages = repository.loadLocalMessages(thread.id)
                                                        statusMessage = "Couldn't send the message. Make sure Kheyr is your default SMS app."
                                                    }
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
                                // Sync uploads to an account; without one the toggle would start a
                                // 15-minute worker that can never upload anything (B-03).
                                signedIn = preferences.authTokens().first != null,
                                defaultSub = preferences.defaultSubscriptionId,
                                sims = sims,
                                directMessagesEnabled = preferences.directMessagesEnabled,
                                spamAutoDeleteDays = preferences.spamAutoDeleteDays,
                                onNotificationChange = { notificationSettings = it; preferences.saveNotificationSettings(it) },
                                onThemeChange = { themePreference = it; preferences.themePreference = it },
                                onSyncChange = { enabled ->
                                    if (enabled && preferences.authTokens().first == null) {
                                        statusMessage = "Sign in from the Profile tab before enabling sync."
                                    } else {
                                        syncEnabled = enabled
                                        preferences.saveSyncSettings(preferences.syncSettings().copy(enabled = enabled))
                                        // Nothing is queued while sync is off, so arm the one-time
                                        // backfill again on the way down. Without this, re-enabling
                                        // sync leaves everything received during the off period
                                        // permanently absent from the cloud.
                                        if (!enabled) preferences.initialBackfillDone = false
                                        KheyrWorkerScheduler.scheduleAll(context, enabled)
                                    }
                                },
                                onDefaultSimChange = { preferences.defaultSubscriptionId = it },
                                onDirectMessagesChange = { preferences.directMessagesEnabled = it },
                                onSpamAutoDeleteChange = { preferences.spamAutoDeleteDays = it },
                                // Both used to run OkHttp straight from the click handler, i.e. on the
                                // main thread, where every call fails with NetworkOnMainThreadException
                                // and reported the wrong reason (B-11).
                                onDeleteCloudData = {
                                    if (!ApiConfig.isConfigured) {
                                        statusMessage = "Configure API base URL first"
                                    } else {
                                        scope.launch(Dispatchers.IO) {
                                            val deleted = api.deleteCloudData()
                                            withContext(Dispatchers.Main) {
                                                statusMessage = if (deleted) "Cloud data deletion requested" else "Cloud data deletion failed — check your connection"
                                            }
                                        }
                                    }
                                },
                                onExportCloudData = {
                                    if (!ApiConfig.isConfigured) {
                                        statusMessage = "Configure API base URL first"
                                    } else {
                                        scope.launch(Dispatchers.IO) {
                                            val export = api.exportCloudData()
                                            withContext(Dispatchers.Main) {
                                                statusMessage = if (export != null) "Export requested" else "Export failed — check your connection"
                                            }
                                        }
                                    }
                                },
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
                            onSearchFocus = { inboxSearchFocusRequester.requestFocus() },
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
                        val header = remember(thread, messages.size, sims) {
                            screenMapper.header(thread, messages.size, sims)
                        }
                        GlassTopBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding(),
                        ) {
                            ConversationTopBar(
                                thread = thread,
                                headerSubtitle = header.subtitle,
                                // An alphanumeric sender ("VERIFY") has nothing to dial (B-34).
                                callEnabled = header.callEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                onNavigateBack = { navigateBack() },
                                onSearchToggle = { conversationSearchActive = !conversationSearchActive },
                                onCall = {
                                    val uri = Uri.parse("tel:${thread.address}")
                                    context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                                },
                            )
                        }
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

                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 2 },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(200)) { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = when (screen) {
                                AppScreen.Main -> KheyrChromeInsets.bottomNav() + 6.dp
                                AppScreen.Conversation -> 104.dp
                                else -> 24.dp
                            },
                        ),
                ) {
                    statusMessage?.let { msg ->
                        KheyrToast(message = msg, onDismiss = { statusMessage = null })
                    }
                }
            }
            }
        }

        showThreadMenu?.let { thread ->
            val folder = chatFolder.toThreadFolder()
            ThreadActionDialog(
                thread = thread,
                blocked = preferences.isBlockedSender(thread.address),
                onDismiss = { showThreadMenu = null },
                onToggleBlock = {
                    // The notifier already suppresses blocked senders; nothing ever wrote the set (B-19).
                    val blocked = !preferences.isBlockedSender(thread.address)
                    preferences.setBlockedSender(thread.address, blocked)
                    showThreadMenu = null
                    threadSelection = threadSelection.clear()
                    statusMessage = if (blocked) {
                        "Notifications from ${thread.address} are blocked"
                    } else {
                        "Notifications from ${thread.address} are allowed again"
                    }
                },
                onAction = { action ->
                    showThreadMenu = null
                    threadSelection = threadSelection.clear()
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
                    threadSelection = threadSelection.clear()
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

/**
 * Recovery banner for a default-SMS role or SMS permission lost after onboarding (B-16). Dismissible,
 * and re-shown by the resume check when the role disappears again.
 */
@Composable
private fun AccessRecoveryBanner(
    isDefaultSms: Boolean,
    smsPermissionGranted: Boolean,
    roleWasRemoved: Boolean,
    onMakeDefault: () -> Unit,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KheyrIconTile(
                icon = Icons.Default.Warning,
                tint = MaterialTheme.colorScheme.error,
                size = 40.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "SMS access lost",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        roleWasRemoved -> "Kheyr is no longer your default SMS app, so it can't receive or send messages."
                        !isDefaultSms -> "Kheyr isn't your default SMS app, so it can't receive or send messages."
                        else -> "SMS permission is missing, so conversations can't be loaded."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isDefaultSms) {
                        TextButton(onClick = onMakeDefault, contentPadding = PaddingValues(end = 12.dp)) {
                            Text("Make default", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!smsPermissionGranted) {
                        TextButton(onClick = onGrantPermission, contentPadding = PaddingValues(end = 12.dp)) {
                            Text("Grant permission", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Dismiss",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}


/**
 * Clears the signed-in sync state locally (tokens, phone, sync device, backfill flag).
 *
 * `onboardingComplete` deliberately stays true: the sync account is optional and signing out of it
 * changes neither the default-SMS role nor any permission, so replaying onboarding would be a
 * pointless dead end (B-20). Clearing initialBackfillDone makes a later sign-in backfill again.
 */
private fun clearLocalSession(context: android.content.Context, preferences: AppPreferences) {
    preferences.clearAuthTokens()
    preferences.userPhone = null
    preferences.saveSyncSettings(preferences.syncSettings().copy(enabled = false, deviceId = null))
    preferences.initialBackfillDone = false
    preferences.saveSyncCursor(null)
    // The queue holds plaintext bodies queued by the account that is signing out. Leaving them would
    // upload one person's messages under whoever signs in next on this device.
    runCatching { AppDatabase.getInstance(context).syncQueueDao().deleteAll() }
}

/** Launches an intent, ignoring the case where no app can handle it (avoids ActivityNotFoundException crashes). */
internal fun android.content.Context.safeStartActivity(intent: Intent) {
    runCatching { startActivity(intent) }
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS); add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.READ_CONTACTS); add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

internal fun formatMessageTime(instant: Instant): String = JalaliDateFormatter.format(instant)

internal fun threadListFilterLabel(filter: ThreadListFilter): String = when (filter) {
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
    onSearchFocus: () -> Unit,
) {
    val selectionMode = selectedCount > 0
    val title = when {
        selectionMode -> "$selectedCount selected"
        screen == AppScreen.SettingsDetail -> settingsCategory?.let { settingsDisplayTitle(it) } ?: "Settings"
        screen == AppScreen.DesktopSync -> "Desktop sync"
        screen == AppScreen.Help -> "Help & feedback"
        screen == AppScreen.Main -> when (selectedTab) {
            MainTab.Chats -> "Kheyr"
            MainTab.Contacts -> "Contacts"
            MainTab.Settings -> "Settings"
            MainTab.Profile -> "Profile"
        }
        else -> "Kheyr"
    }
    val subtitle = when {
        selectionMode -> "Tap to add more"
        screen == AppScreen.SettingsDetail -> "Settings"
        screen == AppScreen.DesktopSync -> "Link a computer"
        screen == AppScreen.Help -> "We read every message"
        screen == AppScreen.Main -> when (selectedTab) {
            MainTab.Chats -> chatFolder.title
            MainTab.Contacts -> "Your address book"
            MainTab.Settings -> "Private by default"
            MainTab.Profile -> "Your account"
        }
        else -> null
    }

    KheyrTopBar(
        title = title,
        subtitle = subtitle,
        leading = when {
            selectionMode -> {
                {
                    KheyrIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Clear selection",
                        onClick = onSelectionClose,
                    )
                }
            }
            screen == AppScreen.SettingsDetail -> {
                {
                    KheyrIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onSettingsBack,
                    )
                }
            }
            screen == AppScreen.DesktopSync || screen == AppScreen.Help -> {
                {
                    KheyrIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onNavigateBack,
                    )
                }
            }
            screen == AppScreen.Main && selectedTab == MainTab.Chats -> {
                { KheyrMark(size = 34.dp) }
            }
            screen == AppScreen.Main -> {
                {
                    KheyrIconTile(
                        icon = when (selectedTab) {
                            MainTab.Chats -> KheyrIcons.Chat
                            MainTab.Contacts -> KheyrIcons.Contacts
                            MainTab.Settings -> Icons.Default.Settings
                            MainTab.Profile -> KheyrIcons.Profile
                        },
                        size = 38.dp,
                    )
                }
            }
            else -> null
        },
        actions = {
            if (selectionMode) {
                KheyrIconButton(
                    icon = Icons.Default.Check,
                    contentDescription = "Mark read",
                    onClick = { onSelectionAction(ThreadBulkAction.MarkRead) },
                )
                KheyrIconButton(
                    icon = KheyrIcons.Archive,
                    contentDescription = "Archive",
                    onClick = { onSelectionAction(ThreadBulkAction.Archive) },
                )
                KheyrIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    onClick = { onSelectionAction(ThreadBulkAction.Delete) },
                    tint = MaterialTheme.colorScheme.error,
                )
                Box {
                    KheyrIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More selection actions",
                        onClick = onSelectionOverflowExpand,
                    )
                    KheyrDropdownMenu(
                        expanded = selectionOverflowExpanded,
                        onDismissRequest = onSelectionOverflowDismiss,
                    ) {
                        DropdownMenuItem(
                            text = { Text(selectionPinLabel) },
                            leadingIcon = { Icon(KheyrIcons.Pin, contentDescription = null) },
                            onClick = { onSelectionPin(selectionPinLabel == "Pin") },
                        )
                        DropdownMenuItem(
                            text = { Text("Mark read") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            onClick = { onSelectionAction(ThreadBulkAction.MarkRead) },
                        )
                        DropdownMenuItem(
                            text = { Text("Mark spam") },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                            onClick = { onSelectionAction(ThreadBulkAction.MarkSpam) },
                        )
                        DropdownMenuItem(
                            text = { Text("Mute") },
                            leadingIcon = { Icon(KheyrIcons.Mute, contentDescription = null) },
                            onClick = { onSelectionAction(ThreadBulkAction.Mute) },
                        )
                    }
                }
            } else if (screen == AppScreen.Main && selectedTab == MainTab.Chats) {
                KheyrIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search conversations",
                    onClick = onSearchFocus,
                )
                Box {
                    KheyrIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        onClick = onChatsOverflowExpand,
                    )
                    KheyrDropdownMenu(
                        expanded = chatsOverflowExpanded,
                        onDismissRequest = onChatsOverflowDismiss,
                    ) {
                        Text(
                            text = "FOLDERS",
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MainNavigationModel.chatFolders().forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (folder) {
                                            ChatFolder.All -> KheyrIcons.Chat
                                            ChatFolder.Spam -> Icons.Default.Warning
                                            ChatFolder.Archived -> KheyrIcons.Archive
                                            ChatFolder.Pinned -> KheyrIcons.Pin
                                        },
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon = {
                                    if (chatFolder == folder) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                onClick = {
                                    onChatsOverflowDismiss()
                                    onChatFolderSelected(folder)
                                },
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        MainNavigationModel.overflowActions().forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (action) {
                                            ChatsOverflowAction.DesktopSync -> KheyrIcons.Monitor
                                            ChatsOverflowAction.HelpFeedback -> KheyrIcons.Help
                                            ChatsOverflowAction.Compose -> KheyrIcons.Compose
                                        },
                                        contentDescription = null,
                                    )
                                },
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KheyrDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        content = content,
    )
}
