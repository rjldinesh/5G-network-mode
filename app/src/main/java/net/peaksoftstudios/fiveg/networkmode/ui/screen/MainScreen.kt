package net.peaksoftstudios.fiveg.networkmode.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.peaksoftstudios.fiveg.networkmode.R
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModel
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val dataUsageViewModel: DataUsageViewModel = viewModel(factory = DataUsageViewModelFactory(
        context
    )
    )

    val titles = listOf("Switch to 5G-4G", "Signal & Speed Test", "Cell Tower Details", "Data Usage by Apps")

    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutUsContainer(onBackToMain = { showAbout = false })
    } else {
        Scaffold(
            topBar = {
                MainTopBar(
                    title = titles[selectedTab],
                    onAboutClick = { showAbout = true }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    val items = listOf(
                        Triple(Icons.Filled.Home,  R.string.network, 0),
                        Triple(Icons.Filled.SignalCellularAlt, R.string.signal, 1),
                        Triple(Icons.Filled.BarChart, R.string.tower, 2),
                        Triple(Icons.Filled.DataUsage, R.string.data_usage, 3)
                    )

                    items.forEach { (icon, labelRes, index) ->
                        val isSelected = selectedTab == index
                        val tintColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        val textStyle = MaterialTheme.typography.bodySmall.copy(color = tintColor)

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = stringResource(labelRes),
                                    tint = tintColor
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    style = textStyle
                                )
                            },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        )
        { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> NetworkSwitcherScreen()
                    1 -> SignalStrengthScreen()
                    2 -> CellTowerScreen()
                    3 -> DataUsageScreen(dataUsageViewModel)
                }
            }
        }
    }
}
