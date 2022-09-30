package br.com.backupautomacao.centraldepesagem.ui.viewmodel

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
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CONNECTED_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.CONNECTING_STATUS
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_GET_PRODUCT
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_MAKE_WEIGHING
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_MESSAGE
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_REQUEST
import br.com.backupautomacao.centraldepesagem.Constants.Companion.ERROR_RESPONSE_MGV
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_CONNECTING
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_ERROR_IP_NOT_SETTED
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_PRINTING_ERROR
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_READ
import br.com.backupautomacao.centraldepesagem.Constants.Companion.MESSAGE_SCALE_CONNECTED
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectedThread
import br.com.backupautomacao.centraldepesagem.bluetooth.ConnectingThread
import br.com.backupautomacao.centraldepesagem.network.MgvApi
import br.com.backupautomacao.centraldepesagem.network.MgvProduct
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import br.com.backupautomacao.centraldepesagem.utils.Label
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository.Companion.PRINTER_IP
import br.com.backupautomacao.tabletinteligente.Printer
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class MainFragmentViewModel(
  private val configRepository: ConfigRepository,
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
        if (msg.what == MESSAGE_READ) {
          val message = msg.obj as ByteArray
          val updateWeightProduct = _mgvProduct.value
          updateWeightProduct?.weight = Formatter.formatFromUranoUseCb2Protocol(message)
          _mgvProduct.value = updateWeightProduct
        }

        if (msg.what == CONNECTING_STATUS) {
          if (msg.arg1 == 1) {
            _statusMessage.value = MESSAGE_CONNECTING
          }
        }

        if (msg.what == CONNECTED_STATUS) {
          val thread = msg.obj as ConnectedThread
          _connectedThreads.value?.add(thread)
          if (_connectedThreads.value?.size == 1) {
            setActiveThread(thread)
          }
          _statusMessage.value = MESSAGE_SCALE_CONNECTED(connectedThreads.value?.size)
        }

        if (msg.what == ERROR_MESSAGE) {
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
          _statusMessage.value = ERROR_RESPONSE_MGV(error.Msg, error.Codigo)
        }
        response.item?.let { product ->
          _mgvProduct.value = product
        }
      } catch (e: Exception) {
        _statusMessage.value = ERROR_REQUEST
        Log.e(TAG, "fetchItem: ", e)
      }
    }
  }

  fun makeWeighing() {
    if (checkProductStatus()) {
      _statusMessage.value = ERROR_GET_PRODUCT("pesagem")
      return
    }
    val command = byteArrayOf(0x05)
    _currentDeviceConnected.value?.write(command)
  }

  private fun checkProductStatus(): Boolean {
    if (_mgvProduct.value == null) {
      return true
    }
    return _mgvProduct.value!!.Codigo.isEmpty()
  }

  fun makePrint() {

    val printerIp: String? = configRepository.getConfig(PRINTER_IP)
    if (printerIp == "" || printerIp == null) {
      _statusMessage.value = MESSAGE_ERROR_IP_NOT_SETTED
      return
    }
    val printer = Printer(printerIp)

    if (checkProductStatus()) {
      _statusMessage.value = ERROR_GET_PRODUCT("impressão")
      return
    }

    val product = _mgvProduct.value!!
    if (product.weight == null || product.weight == 0.0) {
      _statusMessage.value = ERROR_MAKE_WEIGHING
      return
    }

    val label = Label()

    try {
      val newLabel = label.createSimpleLabel(mgvProduct.value!!)
      printer.print(newLabel)
      clearFields()
    } catch (e: Exception) {
      _statusMessage.value = MESSAGE_PRINTING_ERROR
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