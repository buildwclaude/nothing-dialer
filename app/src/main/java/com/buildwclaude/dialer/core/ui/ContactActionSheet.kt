package com.buildwclaude.dialer.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.buildwclaude.dialer.R
import com.buildwclaude.dialer.core.ui.theme.DesignType
import com.buildwclaude.dialer.core.ui.theme.palette

/**
 * Bottom sheet shown when a contact or recent-call row is tapped: the number
 * plus Call / Message / Info actions, so a tap never dials by accident.
 */
@Composable
fun ContactActionSheet(
    title: String,
    number: String,
    photoUri: String?,
    subtitle: String? = null,
    contactUri: Uri? = null,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(
                color = palette.Surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 16.dp),
                ) {
                    // Identity header.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoAvatar(title, photoUri, 52.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                style = DesignType.screenTitle,
                                color = palette.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                number.ifBlank { "Unknown number" },
                                style = DesignType.body,
                                color = palette.TextSecondary,
                                maxLines = 1,
                            )
                            subtitle?.let {
                                Text(it, style = DesignType.body, color = palette.Muted, maxLines = 1)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = palette.Divider)

                    SheetAction(R.drawable.ic_phone_call, "Call", palette.Accent) {
                        onCall()
                    }
                    SheetAction(R.drawable.ic_message, "Message", palette.Accent) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")),
                            )
                        }
                        onDismiss()
                    }
                    if (contactUri != null) {
                        SheetAction(R.drawable.ic_info, "Contact info", palette.Accent) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, contactUri))
                            }
                            onDismiss()
                        }
                        SheetAction(R.drawable.ic_edit, "Edit contact", palette.Accent) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_EDIT, contactUri))
                            }
                            onDismiss()
                        }
                    }
                    SheetAction(R.drawable.ic_copy, "Copy number", palette.TextSecondary) {
                        context.getSystemService(ClipboardManager::class.java)
                            ?.setPrimaryClip(ClipData.newPlainText("number", number))
                        onDismiss()
                    }
                    onDelete?.let { del ->
                        SheetAction(R.drawable.ic_trash, "Delete", palette.Negative) {
                            del()
                            onDismiss()
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cancel",
                        style = DesignType.itemTitle,
                        color = palette.TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: Int,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = DesignType.itemTitle, color = palette.TextPrimary)
    }
}
