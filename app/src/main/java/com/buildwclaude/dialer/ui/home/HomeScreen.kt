package com.buildwclaude.dialer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.palette
import com.buildwclaude.dialer.ui.keypad.KeypadScreen

enum class DialerTab(val label: String, val icon: Int) {
    FAVORITES("Favorites", R.drawable.ic_tab_favorites),
    RECENTS("Recents", R.drawable.ic_tab_recents),
    CONTACTS("Contacts", R.drawable.ic_tab_contacts),
    KEYPAD("Keypad", R.drawable.ic_tab_keypad),
    VOICEMAIL("Voicemail", R.drawable.ic_tab_voicemail),
}

@Composable
fun HomeScreen(
    initialNumber: String,
    onPlaceCall: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(DialerTab.KEYPAD) }

    Scaffold(
        containerColor = palette.Surface,
        bottomBar = { TabBar(current = tab, onSelect = { tab = it }) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            when (tab) {
                DialerTab.KEYPAD -> KeypadScreen(initialNumber = initialNumber, onPlaceCall = onPlaceCall)
                else -> ComingSoon(tab)
            }
        }
    }
}

@Composable
private fun ComingSoon(tab: DialerTab) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painterResource(tab.icon),
            contentDescription = null,
            tint = palette.Muted,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(tab.label, style = DesignType.screenTitle, color = palette.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Coming in the next update.", style = DesignType.body, color = palette.TextSecondary)
    }
}

@Composable
private fun TabBar(current: DialerTab, onSelect: (DialerTab) -> Unit) {
    Column(Modifier.background(palette.Surface)) {
        HorizontalDivider(color = palette.Divider, thickness = 0.5.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DialerTab.entries.forEach { t ->
                val selected = t == current
                val tint = if (selected) palette.Accent else palette.Muted
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(t) }
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        painterResource(t.icon),
                        contentDescription = t.label,
                        tint = tint,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        t.label,
                        color = tint,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
