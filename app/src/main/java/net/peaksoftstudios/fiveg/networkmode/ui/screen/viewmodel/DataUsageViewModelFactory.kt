package net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DataUsageViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataUsageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataUsageViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
