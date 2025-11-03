package net.peaksoftstudios.fiveg.networkmode.ui.screen


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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageUiState
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(viewModel: DataUsageViewModel) {

    val state by viewModel.uiState

    Scaffold(
        topBar = {
            null
        }/*,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.loadData() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }
        }*/
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state) {
                is DataUsageUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        color = Color.Gray
                    )
                }

                is DataUsageUiState.Error -> {
                    Text(
                        "Error loading data: ${(state as DataUsageUiState.Error).message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
