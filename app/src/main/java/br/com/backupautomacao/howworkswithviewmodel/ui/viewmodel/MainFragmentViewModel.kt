package br.com.backupautomacao.howworkswithviewmodel.ui.viewmodel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.backupautomacao.howworkswithviewmodel.Constants
import br.com.backupautomacao.howworkswithviewmodel.Constants.Companion.CONNECTING_MESSAGE
import br.com.backupautomacao.howworkswithviewmodel.bluetooth.ConnectedThread
import br.com.backupautomacao.howworkswithviewmodel.bluetooth.ConnectingThread
import br.com.backupautomacao.howworkswithviewmodel.network.MgvApi
import br.com.backupautomacao.howworkswithviewmodel.network.MgvProduct
import br.com.backupautomacao.howworkswithviewmodel.utils.Formatter
import br.com.backupautomacao.howworkswithviewmodel.utils.Label
import br.com.backupautomacao.howworkswithviewmodel.utils.Preferences
import br.com.backupautomacao.howworkswithviewmodel.utils.Preferences.Companion.PRINTER_IP
import br.com.backupautomacao.tabletinteligente.Printer
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class MainFragmentViewModel(
  private val preferences: Preferences,
  private val bluetoothAdapter: BluetoothAdapter
) : ViewModel() {
  private val TAG = "MainFragmentViewModel"

  private val _mgvProduct: MutableLiveData<MgvProduct?> = MutableLiveData()
  val mgvProduct: LiveData<MgvProduct?> = _mgvProduct

  private val _statusMessage: MutableLiveData<String> = MutableLiveData()
  val statusMessage: LiveData<String> = _statusMessage

  private val _connectedThreads: MutableLiveData<ArrayList<ConnectedThread>> =
    MutableLiveData(ArrayList())
  val connectedThreads: LiveData<ArrayList<ConnectedThread>> = _connectedThreads

  private val _currentDeviceConnected: MutableLiveData<ConnectedThread> = MutableLiveData()

  private val mHandler: Handler by lazy {
    object : Handler(Looper.getMainLooper()) {

      override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (msg.what == Constants.MESSAGE_READ) {
          val message = msg.obj as ByteArray
          val updateWeightProduct = _mgvProduct.value
          updateWeightProduct?.weight = Formatter.formatFromUranoUseCb2Protocol(message)
          _mgvProduct.value = updateWeightProduct
        }

        if (msg.what == Constants.CONNECTING_STATUS) {
          if (msg.arg1 == 1) {
            _statusMessage.value = CONNECTING_MESSAGE
          }
        }

        if (msg.what == Constants.CONNECTED_STATUS) {
          val thread = msg.obj as ConnectedThread
          Log.i(TAG, "handleMessage: $thread")
          _connectedThreads.value?.add(thread)
          if (_connectedThreads.value?.size == 1) {
            setActiveThread(thread)
          }
          _statusMessage.value = "Balança ${connectedThreads.value?.size} conectada"
        }

        if (msg.what == Constants.ERROR_MESSAGE) {
          val errorMessage = msg.obj as String
          _statusMessage.value = errorMessage
        }
      }
    }
  }

  fun fetchItem(codItem: String) {
    viewModelScope.launch {
      try {
        val response = MgvApi.retrofitService.queryItem("bkp@2018", "1", codItem)
        response.ConsultaItemResult?.let { error ->
          _mgvProduct.value = MgvProduct(Descritivo1aLinha = error.Msg, Codigo = error.Codigo)
        }
        response.item?.let { product ->
          _mgvProduct.value = product
        }
      } catch (e: Exception) {
        _mgvProduct.value = MgvProduct(Descritivo1aLinha = "Erro ao fazer requisição!")
        Log.e(TAG, "fetchItem: ", e)
      }
    }
  }

  fun makeWeighing() {
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
  }

  fun makePrint() {

    val printerIp: String? = "172.016.241.219" //preferences.getConfig(PRINTER_IP)
    if (printerIp == null || printerIp == "") {
      _statusMessage.value = "IP do equipamento não configurado"
      return
    }

    val product = _mgvProduct.value
    val printer = Printer(printerIp)

    if (product == null) {
      _statusMessage.value = "Busque o produto antes de fazer a impressão"
      return
    }

    if (product.weight == null || product.weight == 0.0) {
      _statusMessage.value = "Realize a pesagem"
      return
    }

    val label = Label()

    try {
      val newLabel = label.gerarEtiquetaSimples(mgvProduct.value!!)
      printer.print(newLabel)
      clearFields()
    } catch (e: Exception) {
      _statusMessage.value = "Não foi possível realizar impressão"
      Log.e(TAG, "makePrint: ${e.message}", e)
    }
  }

  @Throws(SecurityException::class)
  fun getAvailableDevices(): LiveData<Set<BluetoothDevice>> {
    return MutableLiveData(bluetoothAdapter.bondedDevices)
  }

  fun setActiveThread(currentThread: ConnectedThread) {
    _currentDeviceConnected.value = currentThread
  }

  private fun clearFields() {
    _mgvProduct.value = MgvProduct()
  }

  fun connectToDevices(devices: Set<BluetoothDevice>) {
    devices.forEach {
      val executor = Executors.newSingleThreadExecutor()
      val newThread = ConnectingThread(bluetoothAdapter, it.address, mHandler)
      executor.execute(newThread)
      executor.shutdown()
    }
  }
}