package net.peaksoftstudios.fiveg.networkmode.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AboutUsContainer(onBackToMain: () -> Unit) {
    var currentScreen by remember { mutableStateOf("about") }
    var webTitle by remember { mutableStateOf("") }
    var webUrl by remember { mutableStateOf("") }

    when (currentScreen) {
        "about" -> AboutUsScreen(
            onBack = onBackToMain,
            onOpenWebLink = { title, url ->
                webTitle = title
                webUrl = url
                currentScreen = "web"
            }
        )

        "web" -> WebViewScreen(
            title = webTitle,
            url = webUrl,
            onBack = { currentScreen = "about" }
        )
    }
}
