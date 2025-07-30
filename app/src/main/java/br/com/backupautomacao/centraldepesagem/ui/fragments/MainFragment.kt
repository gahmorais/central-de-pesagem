package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.bluetooth.BluetoothManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMainBinding
import br.com.backupautomacao.centraldepesagem.permission.Permission
import br.com.backupautomacao.centraldepesagem.permission.PermissionManager
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainViewModel
import br.com.backupautomacao.centraldepesagem.ui.factory.MainViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import timber.log.Timber

class MainFragment : Fragment() {

  private val binding: FragmentMainBinding by lazy {
    FragmentMainBinding.inflate(layoutInflater)
  }

  private val mainViewModel: MainViewModel by activityViewModels {
    MainViewModelFactory(
      context = get(),
      epson = get(),
      honeywellPrinter = get(),
      configRepository = get(),
      operationRepository = get(),
      workManager = get(),
      bluetoothHandler = get(),
      resourceProvider = get(),
      label = get(),
      mgvService = get()
    )
  }

  private val permissionManager = PermissionManager.from(this)

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding.lifecycleOwner = this
    binding.viewModel = mainViewModel

    binding.spinnerLoading.btnCancel.setOnClickListener {
      mainViewModel.cancelPrint()
      stopLoading()
    }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bluetoothPermission()
    handleMessages()
    handleButtons()
    configureSpinner()
    configureObservers()
    configureQuantityInput()
    observePrintStatus()
  }

  private fun configureQuantityInput() {
    binding.inputQuantity.setOnEditorActionListener { view, actionId, _ ->
    if (actionId == EditorInfo.IME_ACTION_DONE) {
        val quantity = view.text.toString()
        mainViewModel.updateProductQuantity(quantity)
        hideKeyboard(view)
        true
      } else {
        false
      }
    }
  }

  private fun hideKeyboard(view: View) {
    val imm = requireContext().getSystemService(InputMethodManager::class.java)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }

  private fun observePrintStatus() {
    mainViewModel.statusPrint.onEach { workInfos ->
      if (workInfos.isNotEmpty()) {
        when (workInfos[0].state) {
          WorkInfo.State.SUCCEEDED -> {
            stopLoading()
            mainViewModel.showMessage(getString(R.string.printing_done))
          }
          WorkInfo.State.FAILED -> {
            stopLoading()
            mainViewModel.showMessage(getString(R.string.printing_error))
          }
          WorkInfo.State.CANCELLED -> {
            stopLoading()
            mainViewModel.showMessage(getString(R.string.printing_cancelled))
          }
          WorkInfo.State.RUNNING -> {
            Timber.i("monitoringPrint: Rodando tarefa")
          }
          WorkInfo.State.ENQUEUED -> {
            startLoading(getString(R.string.printing))
          }
          else -> stopLoading()
        }
      }
    }.launchIn(lifecycleScope)
  }

  private fun startLoading(message: String) {
    binding.spinnerLoading.tvMessage.text = message
    binding.spinnerLoading.root.visibility = View.VISIBLE
    binding.mainScreen.visibility = View.GONE
  }

  private fun stopLoading() {
    binding.spinnerLoading.root.visibility = View.GONE
    binding.mainScreen.visibility = View.VISIBLE
  }

  private fun bluetoothPermission() {
    val blutoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Permission.BluetoothAndroidS
    } else {
      Permission.Bluetooth
    }

    permissionManager
      .request(blutoothPermission)
      .rationale(getString(R.string.message_to_access_bluetooth))
      .checkPermission { granted ->
        if (granted) {
          val bluetoothManager =
            requireContext().getSystemService(BluetoothManager::class.java) as BluetoothManager
          val btAdapter = bluetoothManager.adapter
          lifecycleScope.launch {
            try {
              mainViewModel.connectToDevices(btAdapter)
            } catch (e: Exception) {
              e.printStackTrace()
              showMessage("Não foi possível conectar ao dispositivo")
            }
          }
        } else {
          error(getString(R.string.error_to_access_bluetooth))
        }
      }
  }

  private fun configureObservers() {
    mainViewModel.devices.onEach { devices ->
      devices.mapIndexed { i, _ -> "Balança ${i + 1}" }
      ArrayAdapter(
        requireActivity(),
        android.R.layout.simple_spinner_item,
        devices
      ).also { adapter ->
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerScale.adapter = adapter
      }
    }.launchIn(lifecycleScope)
  }

  private fun configureSpinner() {
    mainViewModel.scale.onEach { value ->
      value?.let { binding.spinnerScale.setSelection(value) }
    }

    val itemListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
        val devices = mainViewModel.devices.value
        val devicesName = devices.mapIndexed { index, _ -> "Balança ${index + 1}" }
        ArrayAdapter(
          requireActivity(),
          android.R.layout.simple_spinner_item,
          devicesName
        ).also { adapter ->
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
          binding.spinnerScale.adapter = adapter
        }
        mainViewModel.setActiveDevice(devices[i])
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {}
    }

    binding.spinnerScale.onItemSelectedListener = itemListener
  }

  private fun success(message: String) {
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
      .setTextColor(Color.parseColor("#09AF00"))
      .show()
  }

  private fun error(message: String) {
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
      .setTextColor(Color.parseColor("#B00020"))
      .show()
  }

  private fun getProduct() {
    val code = binding.inputProductCode.text.toString()
    mainViewModel.fetchItem(code)
    binding.inputProductCode.setText("")
  }

  private fun print() {
    permissionManager
      .request(Permission.Storage)
      .rationale(getString(R.string.message_to_access_storage))
      .checkPermission { granted ->
        Timber.d("Granted:$granted")
        if (!granted) {
          lifecycleScope.launch { mainViewModel.print() }
        } else {
          error(getString(R.string.fail_to_access_storage))
        }
      }
  }

  private fun listening() {
    permissionManager
      .request(Permission.Record)
      .rationale(getString(R.string.message_to_access_microphone))
      .checkPermission { granted ->
        if (granted) {
          mainViewModel.listening()
        } else {
          error(getString(R.string.fail_to_access_microphone))
        }
      }
  }

  private fun handleButtons() {
    binding.apply {
      buttonPrint.setOnClickListener {
        print()
      }

      inputProductCode.setOnKeyListener { _, keycode, event ->
        if (event.action == KeyEvent.ACTION_DOWN) {
          if (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER) {
            getProduct()
            return@setOnKeyListener true
          }
        }
        false
      }

      buttonWeighing.setOnClickListener {
        val connectedDevice = mainViewModel.activeDevice.value
        Log.d("MainFragment", "Botão PESAR clicado")
        connectedDevice?.let { mainViewModel.weighining(it) }
      }

      buttonRecord.setOnClickListener {
        viewModel?.toggleShowQuantity()
      }
    }
  }

  private fun handleMessages() {
    mainViewModel.statusMessage.onEach { message ->
      Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }.launchIn(lifecycleScope)
  }

  private fun showMessage(text: String) {
    mainViewModel.emitMessage(text)
  }
}