package com.buildwclaude.dialer.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.dialer.core.ui.MonoAvatar
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.data.ContactsRepository
import com.buildwclaude.dialer.domain.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repo: ContactsRepository,
) : ViewModel() {
    val contacts = MutableStateFlow<List<Contact>>(emptyList())
    init { refresh() }
    fun refresh() = viewModelScope.launch { contacts.value = repo.contacts() }
}

// One flat list of rows: a letter header, then its contacts, then next letter…
private sealed interface Row {
    data class Header(val letter: Char) : Row
    data class Person(val contact: Contact) : Row
}

@Composable
fun ContactsScreen(
    onPlaceCall: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

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
    // letter -> row index, for the A–Z fast scroller.
    val letterIndex = remember(rows) {
        buildMap {
            rows.forEachIndexed { i, r -> if (r is Row.Header) putIfAbsent(r.letter, i) }
        }
    }
    val presentLetters = remember(letterIndex) { ('A'..'Z').filter { it in letterIndex } + '#'.takeIf { '#' in letterIndex }.let { if (it != null) listOf(it) else emptyList() } }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(palette.Surface)) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Contacts",
                style = DesignType.screenTitle,
                color = palette.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
            )
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
                            is Row.Person -> ContactRow(r.contact) { onPlaceCall(r.contact.number) }
                        }
                    }
                }
            }
        }

        if (presentLetters.isNotEmpty()) {
            AlphabetIndex(
                letters = presentLetters,
                modifier = Modifier.align(Alignment.CenterEnd),
                onLetter = { letter ->
                    letterIndex[letter]?.let { idx -> scope.launch { listState.scrollToItem(idx) } }
                },
            )
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
    ) {
        MonoAvatar(contact.name, contact.photoUri, 40.dp)
        Spacer(Modifier.width(16.dp))
        Text(
            contact.name,
            style = DesignType.itemTitle,
            color = palette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Minimal vertical A–Z index pinned to the right edge; drag to jump. */
@Composable
private fun AlphabetIndex(
    letters: List<Char>,
    modifier: Modifier = Modifier,
    onLetter: (Char) -> Unit,
) {
    var height by remember { mutableStateOf(1) }
    var active by remember { mutableStateOf<Char?>(null) }

    fun pick(y: Float) {
        val idx = ((y / height) * letters.size).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[idx]
        if (letter != active) {
            active = letter
            onLetter(letter)
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .padding(vertical = 8.dp)
            .pointerInput(letters) {
                height = size.height
                detectVerticalDragGestures(
                    onDragStart = { pick(it.y) },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                ) { change, _ -> pick(change.position.y) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEach { ch ->
            Text(
                ch.toString(),
                color = if (ch == active) palette.TextPrimary else palette.Muted,
                fontSize = 11.sp,
                fontWeight = if (ch == active) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
