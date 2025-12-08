package net.peaksoftstudios.fiveg.networkmode.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageUiState
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModel
import net.peaksoftstudios.fiveg.networkmode.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(viewModel: DataUsageViewModel) {

    val state by viewModel.uiState

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ Automatically reload when coming back from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionUtils.hasUsagePermission(context)) {
                    viewModel.loadData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Scaffold(
        topBar = {
            null
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state) {
                is DataUsageUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading data...", color = Color.Gray)
                    }
                }

                is DataUsageUiState.Success -> {
                    val data = (state as DataUsageUiState.Success).data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(data) { app ->
                            AppUsageRow(app)
                        }
                    }
                }

                is DataUsageUiState.Empty -> {
                    Text(
                        "No recent network data found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = Color.Gray
                    )
                }

//                is DataUsageUiState.Error -> {
//                    Text(
//                        "Error loading data: ${(state as DataUsageUiState.Error).message}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }


                is DataUsageUiState.Error -> {

                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()){
                        Text(
                            "Error: ${(state as DataUsageUiState.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if ((state as DataUsageUiState.Error).message.contains("Usage Access")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { PermissionUtils.openUsageAccessSettings(context) }) {
                                Text("Grant Usage Access")
                            }
                        }
                    }

                }
            }
        }
    }
}
