package com.buildwclaude.dialer.ui.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.dialer.core.ui.ContactActionSheet
import com.buildwclaude.dialer.core.ui.MonoAvatar
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.data.ContactsRepository
import com.buildwclaude.dialer.domain.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repo: ContactsRepository,
) : ViewModel() {
    val contacts = MutableStateFlow<List<Contact>>(emptyList())
    init { refresh() }
    fun refresh() = viewModelScope.launch { contacts.value = repo.contacts() }

    fun contactUri(c: Contact) = repo.contactUri(c)

    fun delete(targets: List<Contact>) = viewModelScope.launch {
        repo.delete(targets)
        contacts.value = repo.contacts()
    }
}

// One flat list of rows: a letter header, then its contacts, then next letter…
private sealed interface Row {
    data class Header(val letter: Char) : Row
    data class Person(val contact: Contact) : Row
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    onPlaceCall: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var sheetFor by remember { mutableStateOf<Contact?>(null) }

    // Back leaves selection mode / closes the sheet instead of exiting the app.
    BackHandler(enabled = selecting || sheetFor != null) {
        if (sheetFor != null) sheetFor = null else { selecting = false; selected = emptySet() }
    }

    val rows = remember(contacts) {
        val list = ArrayList<Row>()
        var last: Char? = null
        for (c in contacts) {
            if (c.sortLetter != last) {
                list += Row.Header(c.sortLetter)
                last = c.sortLetter
            }
            list += Row.Person(c)
        }
        list
    }
    // letter -> row index of its header, for the wheel's list jumps.
    val letterIndex = remember(rows) {
        buildMap {
            rows.forEachIndexed { i, r -> if (r is Row.Header) putIfAbsent(r.letter, i) }
        }
    }
    // Full A–Z (+ '#') so empty letters render dimmed and are skipped, per spec.
    val wheelLetters = remember { ('A'..'Z').toList() + '#' }
    val counts = remember(contacts) { contacts.groupingBy { it.sortLetter }.eachCount() }

    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(palette.Surface)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    if (selecting) "${selected.size} selected" else "Contacts",
                    style = DesignType.screenTitle,
                    color = palette.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (selecting) {
                    Text(
                        "Delete",
                        style = DesignType.itemTitle,
                        color = if (selected.isEmpty()) palette.Muted else palette.Negative,
                        modifier = Modifier
                            .clickable(enabled = selected.isNotEmpty()) {
                                viewModel.delete(contacts.filter { it.id in selected })
                                selecting = false; selected = emptySet()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    Text(
                        "Done",
                        style = DesignType.itemTitle,
                        color = palette.TextSecondary,
                        modifier = Modifier
                            .clickable { selecting = false; selected = emptySet() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                } else if (contacts.isNotEmpty()) {
                    Text(
                        "Select",
                        style = DesignType.itemTitle,
                        color = palette.Accent,
                        modifier = Modifier
                            .clickable { selecting = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            if (contacts.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("No contacts", style = DesignType.body, color = palette.TextSecondary)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(
                        count = rows.size,
                        key = { i -> (rows[i] as? Row.Person)?.contact?.id ?: "h${(rows[i] as Row.Header).letter}" },
                    ) { i ->
                        when (val r = rows[i]) {
                            is Row.Header -> Text(
                                r.letter.toString(),
                                color = palette.Muted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.Surface)
                                    .padding(start = 20.dp, top = 10.dp, bottom = 4.dp),
                            )
                            is Row.Person -> ContactRow(
                                contact = r.contact,
                                selecting = selecting,
                                checked = r.contact.id in selected,
                                onToggle = {
                                    selected = if (r.contact.id in selected) selected - r.contact.id
                                    else selected + r.contact.id
                                },
                                onOpen = { sheetFor = r.contact },
                                onLongPress = {
                                    if (!selecting) { selecting = true; selected = setOf(r.contact.id) }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (contacts.isNotEmpty() && !selecting) {
            EdgeAlphabetWheel(
                letters = wheelLetters,
                counts = counts,
                listState = listState,
                sectionIndex = { letter -> letterIndex[letter] },
            )
        }
    }

    sheetFor?.let { c ->
        ContactActionSheet(
            title = c.name,
            number = c.number,
            photoUri = c.photoUri,
            contactUri = viewModel.contactUri(c),
            onDismiss = { sheetFor = null },
            onCall = { sheetFor = null; onPlaceCall(c.number) },
            onDelete = { viewModel.delete(listOf(c)) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(
    contact: Contact,
    selecting: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selecting) onToggle() else onOpen() },
                onLongClick = onLongPress,
            )
            .padding(start = 20.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (selecting) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = palette.Accent,
                    uncheckedColor = palette.Muted,
                ),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
        } else {
            MonoAvatar(contact.name, contact.photoUri, 40.dp)
            Spacer(Modifier.width(16.dp))
        }
        Text(
            contact.name,
            style = DesignType.itemTitle,
            color = palette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
