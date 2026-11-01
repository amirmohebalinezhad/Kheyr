package com.kheyr.sms.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kheyr.sms.contacts.DeviceContact

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    contacts: List<DeviceContact>,
    loading: Boolean,
    hasPermission: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onContactClick: (DeviceContact) -> Unit,
    onCompose: () -> Unit,
    listState: LazyListState,
) {
    val topInset = KheyrChromeInsets.shellTop()
    val bottomInset = KheyrChromeInsets.bottomNav()
    val filtered = remember(contacts, searchQuery) {
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
    val sections = remember(filtered) { contactSections(filtered) }

    when {
        !hasPermission -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset),
                contentAlignment = Alignment.Center,
            ) {
                KheyrEmptyState(
                    icon = KheyrIcons.Contacts,
                    title = "Bring your people in",
                    message = "Allow contacts access to see who is already on this phone and start a conversation in one tap.",
                    actionLabel = "Grant contacts access",
                    onAction = onRequestPermission,
                )
            }
        }
        loading && contacts.isEmpty() -> {
            Column(Modifier.padding(top = topInset)) { KheyrSkeletonList() }
        }
        contacts.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset),
                contentAlignment = Alignment.Center,
            ) {
                KheyrEmptyState(
                    icon = KheyrIcons.Contacts,
                    title = "No contacts found",
                    message = "Contacts with a phone number will appear here. You can still text any number directly.",
                    actionLabel = "New message",
                    onAction = onCompose,
                )
            }
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(topInset))
                KheyrSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search contacts",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Text(
                    text = "${filtered.size} ${if (filtered.size == 1) "contact" else "contacts"}",
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (filtered.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        KheyrEmptyState(
                            icon = KheyrIcons.Contacts,
                            title = "No matches",
                            message = "No contacts match “$searchQuery”.",
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = bottomInset),
                    ) {
                        sections.forEach { (letter, sectionContacts) ->
                            stickyHeader(key = "letter:$letter") {
                                LetterHeader(letter)
                            }
                            items(sectionContacts, key = { "${it.id}:${it.phoneNumber}" }) { contact ->
                                TelegramStyleContactRow(
                                    displayName = contact.displayName,
                                    phoneNumber = contact.phoneNumber,
                                    photoUri = contact.photoUri,
                                    onClick = { onContactClick(contact) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = letter,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun contactSections(contacts: List<DeviceContact>): List<Pair<String, List<DeviceContact>>> {
    val grouped = LinkedHashMap<String, MutableList<DeviceContact>>()
    contacts.sortedBy { it.displayName.lowercase() }.forEach { contact ->
        val first = contact.displayName.trim().firstOrNull()?.uppercaseChar()
        val key = if (first != null && first.isLetter()) first.toString() else "#"
        grouped.getOrPut(key) { mutableListOf() }.add(contact)
    }
    return grouped.map { (letter, people) -> letter to people }
}

@Composable
fun NewMessageScreen(
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
    val bottomInset = KheyrChromeInsets.bottomNav()
    val model = remember(contacts) { NewMessageViewModel(contacts) }
    val state = remember(model, query) { model.stateFor(query) }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(topInset))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KheyrIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
            )
            Spacer(Modifier.size(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "New message",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "To a contact or a phone number",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        KheyrSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Name or phone number",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (!hasPermission) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Search your address book by allowing contacts access, or type a number below.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                KheyrPrimaryButton(
                    text = "Allow",
                    onClick = onRequestPermission,
                    modifier = Modifier.width(96.dp),
                )
            }
        }
        state.manualAddress?.let { address ->
            Surface(
                onClick = { model.manualRecipient(query)?.let(onRecipientSelected) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KheyrIconTile(icon = KheyrIcons.Chat, size = 40.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Send a message", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        KheyrIcons.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        when {
            loading && contacts.isEmpty() -> KheyrSkeletonList(modifier = Modifier.padding(top = 8.dp))
            state.matches.isEmpty() && state.manualAddress == null -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                KheyrEmptyState(
                    icon = KheyrIcons.Chat,
                    title = if (query.isBlank()) "Who do you want to text?" else "No matches",
                    message = if (query.isBlank()) {
                        "Type a name or a phone number to begin."
                    } else {
                        "No contact matches “$query”. Keep typing to send to a number instead."
                    },
                )
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 6.dp, bottom = bottomInset),
            ) {
                if (state.matches.isNotEmpty()) {
                    item(key = "contacts-header") {
                        KheyrSectionHeader("Contacts")
                    }
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
}
