package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import br.com.backupautomacao.bluetooth.BluetoothHandler
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.network.MgvService
import br.com.backupautomacao.centraldepesagem.printers.Epson
import br.com.backupautomacao.centraldepesagem.printers.HoneywellPrinter
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider

class MainViewModelFactory(
    private val context: Context,
    private val epson: Epson,
    private val honeywellPrinter: HoneywellPrinter,
    private val configRepository: ConfigRepository,
    private val operationRepository: OperationRepository,
    private val workManager: WorkManager,
    private val bluetoothHandler: BluetoothHandler,
    private val resourceProvider: ResourceProvider,
    private val label: Label,
    private val mgvService: MgvService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                context,
                epson,
                honeywellPrinter,
                configRepository,
                operationRepository,
                workManager,
                bluetoothHandler,
                resourceProvider,
                label,
                mgvService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
