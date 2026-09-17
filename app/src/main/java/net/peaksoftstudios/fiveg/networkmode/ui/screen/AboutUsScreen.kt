package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.peaksoftstudios.fiveg.networkmode.manager.InAppRatingManager
import net.peaksoftstudios.fiveg.networkmode.ui.components.AppCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.AppLogo
import net.peaksoftstudios.fiveg.networkmode.ui.components.IconTile
import net.peaksoftstudios.fiveg.networkmode.ui.components.PrimaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Navy

private const val SUPPORT_EMAIL = "peaksoftstudios@gmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    onBack: () -> Unit,
    onOpenWebLink: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.7"
        } catch (e: Exception) {
            "1.7"
        }
    }

    BackHandler { onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("About", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLogo(
                    size = 96.dp,
                    modifier = Modifier.shadow(elevation = 16.dp, shape = CircleShape, ambientColor = Navy, spotColor = Navy)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "5G Network Mode Switcher",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Version $versionName",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    "Manage network modes, check signal strength and keep an eye on data usage. Built by Peaksoft Studios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            AppCard {
                AboutRow(
                    icon = Icons.Outlined.Mail,
                    tileBackground = MaterialTheme.colorScheme.secondaryContainer,
                    tileTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    overline = "Contact",
                    title = SUPPORT_EMAIL,
                    onClick = { openSupportEmail(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AboutRow(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy policy",
                    onClick = { onOpenWebLink("Privacy policy", "file:///android_asset/privacy_policy_5gmode.html") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AboutRow(
                    icon = Icons.Outlined.Description,
                    title = "Terms & conditions",
                    onClick = { onOpenWebLink("Terms & conditions", "file:///android_asset/terms_and_conditions_5gmode.html") }
                )
            }

            PrimaryActionButton(
                text = "Rate & give feedback",
                icon = Icons.Filled.Star,
                iconTint = Mint,
                height = 52.dp,
                onClick = {
                    val activity = context as? Activity ?: return@PrimaryActionButton
                    scope.launch {
                        InAppRatingManager(activity, context).launchReviewWithFallback()
                    }
                }
            )

            Text(
                "© 2026 Peaksoft Studios",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    overline: String? = null,
    tileBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    tileTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, size = 40.dp, background = tileBackground, tint = tileTint, iconSize = 22.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = if (overline != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$SUPPORT_EMAIL")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Support: 5G Network Mode Switcher")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}
