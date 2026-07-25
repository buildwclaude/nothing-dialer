package com.buildwclaude.dialer.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.dialer.R
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
class FavoritesViewModel @Inject constructor(
    private val repo: ContactsRepository,
) : ViewModel() {
    val favorites = MutableStateFlow<List<Contact>>(emptyList())
    init { refresh() }
    fun refresh() = viewModelScope.launch { favorites.value = repo.favorites() }
}

@Composable
fun FavoritesScreen(
    onPlaceCall: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(palette.Surface)) {
        Text(
            "Favorites",
            style = DesignType.screenTitle,
            color = palette.TextPrimary,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
        )
        if (favorites.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No favorites yet", style = DesignType.body, color = palette.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Star a contact in your phone to see it here.",
                    style = DesignType.body,
                    color = palette.Muted,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(favorites, key = { it.id }) { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaceCall(c.number) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        MonoAvatar(c.name, c.photoUri, 44.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            c.name,
                            style = DesignType.itemTitle,
                            color = palette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painterResource(R.drawable.ic_phone_call),
                            contentDescription = "Call",
                            tint = palette.TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    HorizontalDivider(
                        color = palette.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 80.dp),
                    )
                }
            }
        }
    }
}
