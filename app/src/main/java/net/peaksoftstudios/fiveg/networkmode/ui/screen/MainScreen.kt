package net.peaksoftstudios.fiveg.networkmode.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import net.peaksoftstudios.fiveg.networkmode.R
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModel
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModelFactory

private data class Tab(
    val icon: ImageVector,
    val labelRes: Int,
    val title: String
)

private val tabs = listOf(
    Tab(Icons.Filled.SimCard, R.string.network, "Network mode"),
    Tab(Icons.Filled.SignalCellularAlt, R.string.signal, "Signal & speed"),
    Tab(Icons.Filled.CellTower, R.string.tower, "Cell towers"),
    Tab(Icons.Filled.DataUsage, R.string.data_usage, "Data usage")
)

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val dataUsageViewModel: DataUsageViewModel = viewModel(factory = DataUsageViewModelFactory(context))

    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutUsContainer(onBackToMain = { showAbout = false })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MainTopBar(
                title = tabs[selectedTab].title,
                onAboutClick = { showAbout = true }
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = selectedTab == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = {
                                Text(
                                    stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
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
