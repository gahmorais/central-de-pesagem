package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SettingsFragment : Fragment() {
  private val binding: FragmentSettingsBinding by lazy {
    FragmentSettingsBinding.inflate(layoutInflater)
  }
  private val viewModel: SettingsViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = binding.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    handleMessages()
    handleButtons()
    handleTextInputs()
    handleCheckbox()
    handleRadioGroup()
  }

  private fun handleTextInputs() {
    val eventMask = EventMask(binding.textInputPrinterIp, EventMask.MASCARA_IP)
    binding.textInputPrinterIp.addTextChangedListener(eventMask)
  }

  private fun handleButtons() {
    binding.buttonSavePrinterConfig.setOnClickListener {
      val idRadioButton = binding.radioGroupPrinterType.checkedRadioButtonId
      val selectedPrinter = getSelectedRadioButton(idRadioButton)

      when (selectedPrinter) {
        PrinterType.USB -> saveUsbConfig()
        PrinterType.NETWORK -> saveNetworkConfig()
      }

      viewModel.savePrinterConfig(selectedPrinter)

      Toast.makeText(
        requireContext(),
        "Configuração de impressora salva!",
        Toast.LENGTH_SHORT
      ).show()
    }

    binding.buttonSaveServerAddress.setOnClickListener {
      val serverAddress: String = binding.textInputServerAddress.text.toString()
      viewModel.saveServerAddress(serverAddress)
    }

    binding.buttonSaveMgvConfig.setOnClickListener {
      saveMgvConfig()
    }
  }

  private fun saveMgvConfig() {
    val password = binding.textInputMgvPassword.text.toString()
    val store = binding.textInputMgvStoreNumber.text.toString()

    viewModel.saveMgvConfig(password = password, store = store)
    binding.textInputMgvPassword.setText("")
  }

  private fun saveNetworkConfig() {
    val ipAddress = binding.textInputPrinterIp.text.toString().trim()
    val port = binding.textInputPrinterPort.text.toString().trim()

    if (ipAddress.isEmpty()) {
      Toast.makeText(requireContext(), "O endereço IP não pode estar vazio", Toast.LENGTH_SHORT).show()
      return
    }

    if (port.isEmpty()) {
      Toast.makeText(requireContext(), "O número da porta não pode estar vazio", Toast.LENGTH_SHORT).show()
      return
    }

    viewModel.saveNetworkConfig(ipAddress, port)
  }

  private fun saveUsbConfig() {
    val spinnerUsb = binding.spinnerUsbDevices
    val printerName = spinnerUsb.selectedItem.toString()
    val parts = printerName.split(";")
    if (parts.size == 2) {
      val model = parts[0]
      val deviceName = parts[1]
      Timber.i("USB: Model=$model, Device=$deviceName")
      viewModel.savePrinterUsbName(model, deviceName)
    } else {
      Toast.makeText(requireContext(), "Formato inválido de impressora USB", Toast.LENGTH_SHORT).show()
    }
  }


  private fun handleMessages() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.statusMessage.collect { message ->
        if (message.isNotEmpty()) {
          Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
          viewModel.clearStatusMessage()
        }
      }
    }
  }

  private fun handleCheckbox() {
    binding.checkboxAutomaticPrint.setOnCheckedChangeListener { _, isChecked ->
      viewModel.enableAutomaticPrint(isChecked)
    }
  }

  private fun handleRadioGroup() {
    binding.radioGroupPrinterType.setOnCheckedChangeListener { _, checkedId ->
      when (checkedId) {
        R.id.radioButtonNetwork -> {
          binding.containerNetworkOptions.visibility = View.VISIBLE
          binding.containerUsbOptions.visibility = View.GONE
        }
        R.id.radioButtonUSB -> {
          binding.containerNetworkOptions.visibility = View.GONE
          binding.containerUsbOptions.visibility = View.VISIBLE
          detectUsbPrinter()
        }
      }
    }
  }

  private fun detectUsbPrinter() {
    val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
    val deviceList = usbManager.deviceList.values
    if (deviceList.isNotEmpty()) {
      val devices = deviceList.map { "${it.productName};${it.deviceName}" }
      val adapter = getUsbPrinterListAdapter(devices)

      with(binding) {
        spinnerUsbDevices.adapter = adapter
        statusImpressora.text = "Impressora USB detectada!"
        handleSpinnerUsbList()
      }
    } else {
      binding.statusImpressora.text = "Nenhuma impressora USB detectada."
      Toast.makeText(requireContext(), "Nenhuma impressora USB encontrada.", Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleSpinnerUsbList() {
    binding.spinnerUsbDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(
        adapterView: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        val printerName = adapterView?.getItemAtPosition(position)?.toString()
        Timber.i("Impressora selecionada: $printerName")
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
  }


  private fun getUsbPrinterListAdapter(devices: List<String>): ArrayAdapter<String> {
    return ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_item,
      devices
    ).apply {
      setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
  }

  private fun getSelectedRadioButton(radioButtonId: Int): PrinterType {
    return when (radioButtonId) {
      R.id.radioButtonNetwork -> PrinterType.NETWORK
      R.id.radioButtonUSB -> PrinterType.USB
      else -> throw IllegalArgumentException("Opção de impressora inválida")
    }
  }

  private fun getPrinterTypeId(printerType: String): Int {
    return when (printerType) {
      "USB" -> R.id.radioButtonUSB
      else -> R.id.radioButtonNetwork
    }
  }

  override fun onResume() {
    super.onResume()
    loadSavedConfigs()
  }

  private fun loadSavedConfigs() {
    val configRepository = ConfigRepository(requireContext())

    val automaticPrinting = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val store = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER)
    val printerType = configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE) ?: "Network"

    binding.apply {
      checkboxAutomaticPrint.isChecked = automaticPrinting
      textInputServerAddress.setText(serverAddress)
      textInputPrinterIp.setText(printerAddress)
      textInputPrinterPort.setText(portAddress.toString())
      textInputMgvStoreNumber.setText(store.toString())
      radioGroupPrinterType.check(getPrinterTypeId(printerType))
    }
  }
}



//FUNCIONA PORÉM NÃO PEGA A IMPRESSÃO AUTOMATICA
/*
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SettingsFragment : Fragment() {
  private val binding: FragmentSettingsBinding by lazy {
    FragmentSettingsBinding.inflate(layoutInflater)
  }
  private val viewModel: SettingsViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = binding.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    handleMessages()
    handleButtons()
    handleTextInputs()
    handleCheckbox()
    handleRadioGroup()
  }

  private fun handleTextInputs() {
    val eventMask = EventMask(binding.textInputPrinterIp, EventMask.MASCARA_IP)
    binding.textInputPrinterIp.addTextChangedListener(eventMask)
  }

  private fun getSelectedRadioButton(radioButton: Int): PrinterType {
    return when (radioButton) {
      R.id.radioButtonNetwork -> PrinterType.NETWORK
      R.id.radioButtonUSB -> PrinterType.USB
      else -> throw Exception("Opção inválida")
    }
  }

  private fun getPrinterTypeId(printerType: String): Int {
    return when (printerType) {
      "USB" -> R.id.radioButtonUSB
      else -> R.id.radioButtonNetwork
//      else -> throw Exception("Opção inválida")
    }
  }

  private fun handleButtons() {
    binding.buttonSavePrinterConfig.setOnClickListener {
      val idRadioButton = binding.radioGroupPrinterType.checkedRadioButtonId
      val selectedPrinter = getSelectedRadioButton(idRadioButton)

      when (selectedPrinter) {
        PrinterType.USB -> {
          saveUsbConfig()
        }

        PrinterType.NETWORK -> {
          saveNetworkConfig()
        }
      }
      viewModel.savePrinterConfig(selectedPrinter)
      Toast.makeText(
        requireContext(),
        "Configuração de impressora salva!",
        Toast.LENGTH_SHORT
      ).show()
    }

    binding.buttonSaveServerAddress.setOnClickListener {
      val serverAddress: String = binding.textInputServerAddress.text.toString()
      viewModel.saveServerAddress(serverAddress)
    }

    binding.buttonSaveMgvConfig.setOnClickListener {
      saveMgvConfig()
    }
  }

  private fun saveMgvConfig() {
    val password = binding.textInputMgvPassword.text.toString()
    val store = binding.textInputMgvStoreNumber.text.toString()

    viewModel.saveMgvConfig(password = password, store = store)
    binding.textInputMgvPassword.setText("")
  }

  private fun saveNetworkConfig() {
    val ipAddress = binding.textInputPrinterIp.text.toString().trim()
    val port = binding.textInputPrinterPort.text.toString().trim()

    if (ipAddress.isEmpty()) {
      Toast.makeText(
        requireContext(),
        "O endereço IP não pode estar vazio",
        Toast.LENGTH_SHORT
      ).show()
      return
    }

    if (port.isEmpty()) {
      Toast.makeText(
        requireContext(),
        "O número da porta não pode estar vazio",
        Toast.LENGTH_SHORT
      ).show()
      return
    }

    viewModel.saveNetworkConfig(ipAddress, port)

  }

  private fun saveUsbConfig() {
    val spinnerUsb = binding.spinnerUsbDevices
    val printerName = spinnerUsb.selectedItem.toString()
    Timber.i("USB: $printerName")
    viewModel.savePrinterUsbName(printerName)
  }

  private fun handleMessages() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.statusMessage.collect { message ->
        if (message.isNotEmpty()) {
          Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
          viewModel.clearStatusMessage()
        }
      }
    }
  }

      private fun handleCheckbox() {
    binding.checkboxAutomaticPrint.setOnCheckedChangeListener { _, isChecked ->
      viewModel.enableAutomaticPrint(isChecked)
    }
  }

  private fun handleRadioGroup() {
    binding.radioGroupPrinterType.setOnCheckedChangeListener { _, checkedId ->
      when (checkedId) {
        R.id.radioButtonNetwork -> {
          binding.containerNetworkOptions.visibility = View.VISIBLE
          binding.containerUsbOptions.visibility = View.GONE
        }

        R.id.radioButtonUSB -> {
          binding.containerNetworkOptions.visibility = View.GONE
          binding.containerUsbOptions.visibility = View.VISIBLE
          detectUsbPrinter()
        }
      }
    }
  }

  private fun getUsbPrinterListAdapter(devices: List<String>): ArrayAdapter<String> {
    return ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_item,
      devices
    ).apply {
      setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
  }

  private fun handleSpinnerUsbList() {
    binding.spinnerUsbDevices.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
          adapterView: AdapterView<*>?,
          p1: View?,
          position: Int,
          p3: Long
        ) {
          val printerName = adapterView
            ?.getItemAtPosition(position)
            ?.toString()
            ?.takeIf { it.isNotEmpty() }

          if (!printerName.isNullOrEmpty()) {
            Timber.i("Impressora selecionada: $printerName")
          }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {}
      }
  }

  private fun detectUsbPrinter() {
    val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
    val deviceList = usbManager.deviceList.values
    if (deviceList.isNotEmpty()) {
      val devices = deviceList.map {
        "${it.productName};${it.deviceName}"
      }

      val adapter = getUsbPrinterListAdapter(devices)

      with(binding) {
        spinnerUsbDevices.adapter = adapter
        statusImpressora.text = "Impressora USB detectada!"
        handleSpinnerUsbList()
      }
    } else {
      binding.statusImpressora.text = "Nenhuma impressora USB detectada."
      Toast.makeText(
        requireContext(),
        "Nenhuma impressora USB encontrada.",
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  override fun onResume() {
    super.onResume()
    binding.checkboxAutomaticPrint.isChecked = viewModel.automaticPrintingEnable
    val configRepository = ConfigRepository(requireActivity())
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val store = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER)
    val printerType = configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE) ?: "Network"
    binding.apply {
      val selectedPrinter = getPrinterTypeId(printerType)
      radioGroupPrinterType.check(selectedPrinter)
      textInputServerAddress.setText(serverAddress)
      textInputPrinterPort.setText(portAddress.toString())
      textInputPrinterIp.setText(printerAddress)
      textInputMgvStoreNumber.setText(store.toString())
    }
  }
}
*/