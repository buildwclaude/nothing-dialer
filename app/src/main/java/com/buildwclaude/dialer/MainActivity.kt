package com.buildwclaude.dialer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buildwclaude.dialer.core.DefaultDialerRole
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.PhoneTheme
import com.buildwclaude.dialer.core.ui.theme.palette
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dialerRole: DefaultDialerRole

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            recreate()
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneTheme {
                var isDefault by remember { mutableStateOf(dialerRole.isDefault) }
                LaunchedEffect(Unit) {
                    requestPermissions()
                    isDefault = dialerRole.isDefault
                }
                HomePlaceholder(
                    isDefault = isDefault,
                    onRequestRole = { dialerRole.requestIntent()?.let(roleLauncher::launch) },
                )
            }
        }
    }

    private fun requestPermissions() {
        val wanted = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        val missing = wanted.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }
}

@Composable
private fun HomePlaceholder(isDefault: Boolean, onRequestRole: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Phone", style = DesignType.screenTitle, color = palette.TextPrimary)
        Spacer(Modifier.height(12.dp))
        Text(
            if (isDefault) "This is your default phone app. ✓\nDialpad, recents and calls come next."
            else "To place and receive calls here, set this as your default phone app.",
            style = DesignType.body,
            color = palette.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (!isDefault) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Set as default phone app",
                style = DesignType.itemTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.Accent)
                    .clickable(onClick = onRequestRole)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
    }
}
