package br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.services.PrinterUsb
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import net.gotev.speech.Speech

class MainViewModelFactory(
    private val printerUsb: PrinterUsb,
    private val configRepository: ConfigRepository,
    private val operationRepository: OperationRepository,
    private val workManager: WorkManager,
    private val bluetoothAdapter: BluetoothAdapter,
    private val resourceProvider: ResourceProvider,
    private val label: Label
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainFragmentViewModel(
            printerUsb, configRepository, operationRepository, workManager,
            bluetoothAdapter, resourceProvider, label
        ) as T
    }
}