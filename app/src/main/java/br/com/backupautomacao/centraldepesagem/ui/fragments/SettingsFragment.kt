package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.factory.SettingsViewModelFactory
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask
import br.com.backupautomacao.centraldepesagem.utils.LabelManager
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.IOException

class SettingsFragment : Fragment() {

  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!

  private lateinit var viewModel: SettingsViewModel

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater, container, false)

    val context = requireContext()
    val configRepository = ConfigRepository(context)
    val resourceProvider = ResourceProvider(context)

    val factory = SettingsViewModelFactory(configRepository, resourceProvider)
    viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    LabelManager.ensureLabelsDirectory(requireContext())

    setupLabelTypeSpinners()
    observeConfigFields()
    handleMessages()
    handleTextInputs()
    handleButtons()
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
    }

    binding.buttonSaveServerAddress.setOnClickListener {
      val serverAddress = binding.textInputServerAddress.text.toString()
      viewModel.saveServerAddress(serverAddress)
    }

    binding.buttonSaveMgvConfig.setOnClickListener {
      saveMgvConfig()
    }
  }

  private fun saveMgvConfig() {
    val password = binding.textInputMgvPassword.text.toString()
    val store = binding.textInputMgvStoreNumber.text.toString()
    viewModel.saveMgvConfig(password, store)
    binding.textInputMgvPassword.setText("")
  }

  private fun saveNetworkConfig() {
    val ip = binding.textInputPrinterIp.text.toString().trim()
    val port = binding.textInputPrinterPort.text.toString().trim()
    viewModel.saveNetworkConfig(ip, port)
  }

  private fun saveUsbConfig() {
    val selected = binding.spinnerUsbDevices.selectedItem?.toString() ?: return
    val parts = selected.split(";")
    if (parts.size == 2) {
      val model = parts[0]
      val device = parts[1]
      viewModel.savePrinterUsbName(model, device)
    } else {
      Toast.makeText(requireContext(), "Formato inválido de impressora USB", Toast.LENGTH_SHORT).show()
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
    val devices = usbManager.deviceList.values.map { "${it.productName};${it.deviceName}" }

    if (devices.isNotEmpty()) {
      binding.spinnerUsbDevices.adapter = getUsbPrinterListAdapter(devices)
      binding.statusImpressora.text = "Impressora USB detectada!"
      handleSpinnerUsbList()
    } else {
      binding.statusImpressora.text = "Nenhuma impressora USB detectada."
      Toast.makeText(requireContext(), "Nenhuma impressora USB encontrada.", Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleSpinnerUsbList() {
    binding.spinnerUsbDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val printerName = adapter?.getItemAtPosition(position)?.toString()
        Timber.i("Impressora selecionada: $printerName")
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
  }

  private fun setupLabelTypeSpinners() {
    val labelFiles = LabelManager.listExternalLabelFiles(requireContext())
    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labelFiles)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

    binding.spinnerLabelKg.adapter = adapter
    binding.spinnerLabelQuant.adapter = adapter

    binding.spinnerLabelKg.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        viewModel.setLabelTemplateKg(labelFiles[pos])
      }

      override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    binding.spinnerLabelQuant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        viewModel.setLabelTemplateQuant(labelFiles[pos])
      }

      override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    viewModel.labelTemplateKg.value.let { selected ->
      val index = labelFiles.indexOf(selected)
      if (index >= 0) binding.spinnerLabelKg.setSelection(index)
    }

    viewModel.labelTemplateQuant.value.let { selected ->
      val index = labelFiles.indexOf(selected)
      if (index >= 0) binding.spinnerLabelQuant.setSelection(index)
    }
  }

  private fun observeConfigFields() {
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.serverAddress.collect { binding.textInputServerAddress.setText(it) } }
        launch { viewModel.printerIp.collect { binding.textInputPrinterIp.setText(it) } }
        launch { viewModel.printerPort.collect { binding.textInputPrinterPort.setText(it) } }
        launch { viewModel.storeNumber.collect { binding.textInputMgvStoreNumber.setText(it) } }
        launch { viewModel.printerTypeLiveData.collect { binding.radioGroupPrinterType.check(getPrinterTypeId(it)) } }
        launch { viewModel.automaticPrintingEnable.collect { binding.checkboxAutomaticPrint.isChecked = it } }
      }
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

  private fun getSelectedRadioButton(id: Int): PrinterType {
    return when (id) {
      R.id.radioButtonNetwork -> PrinterType.NETWORK
      R.id.radioButtonUSB -> PrinterType.USB
      else -> throw IllegalArgumentException("Opção de impressora inválida")
    }
  }

  private fun getPrinterTypeId(type: String): Int {
    return when (type.uppercase()) {
      "USB" -> R.id.radioButtonUSB
      else -> R.id.radioButtonNetwork
    }
  }

  private fun getUsbPrinterListAdapter(devices: List<String>): ArrayAdapter<String> {
    return ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, devices).apply {
      setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}








/*
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.factory.SettingsViewModelFactory
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask
import br.com.backupautomacao.centraldepesagem.utils.LabelManager
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import br.com.backupautomacao.centraldepesagem.utils.ResourceProvider
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.IOException

class SettingsFragment : Fragment() {

  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private lateinit var viewModel: SettingsViewModel

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View{
    _binding = FragmentSettingsBinding.inflate(inflater, container, false)

    val context = requireContext()
    val configRepository = ConfigRepository(context)
    val resourceProvider = ResourceProvider(context)

    val factory = SettingsViewModelFactory(configRepository, resourceProvider)
    viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    LabelManager.ensureLabelsDirectory(requireContext())
    val labelFiles = LabelManager.listExternalLabelFiles(requireContext())
    Log.d("LabelManager", "Arquivos encontrados: $labelFiles")

    setupLabelTypeSpinners()
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

//    private fun handleMessages() {
//      viewLifecycleOwner.lifecycleScope.launch {
//        viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
//          viewModel.statusMessage.collect { message ->
//            if (message.isNotEmpty()) {
//              Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//              viewModel.clearStatusMessage()
//            }
//          }
//        }
//      }
//    }


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
        adapterView: AdapterView<*>?, view: View?, position: Int, id: Long
      ) {
        val printerName = adapterView?.getItemAtPosition(position)?.toString()
        Timber.i("Impressora selecionada: $printerName")
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
  }

  private fun setupLabelTypeSpinners() {
    val labelFiles = LabelManager.listExternalLabelFiles(requireContext())

    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labelFiles)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

    binding.spinnerLabelKg.adapter = adapter
    binding.spinnerLabelQuant.adapter = adapter

    val savedKg = viewModel.getLabelTemplateKg()
    val savedQuant = viewModel.getLabelTemplateQuant()

    savedKg?.let { kg ->
      val index = labelFiles.indexOf(kg)
      if (index >= 0) binding.spinnerLabelKg.setSelection(index)
    }

    savedQuant?.let { quant ->
      val index = labelFiles.indexOf(quant)
      if (index >= 0) binding.spinnerLabelQuant.setSelection(index)
    }

    binding.spinnerLabelKg.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        viewModel.setLabelTemplateKg(labelFiles[position])
      }

      override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    binding.spinnerLabelQuant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        viewModel.setLabelTemplateQuant(labelFiles[position])
      }

      override fun onNothingSelected(parent: AdapterView<*>) {}
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


//  override fun onResume() {
//    super.onResume()
//    carregarConfiguracoes()
//  }
//
//  override fun onPause() {
//    super.onPause()
//   salvarConfiguracoes()
//  }

  private fun observeConfigFields() {
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.serverAddress.collect { binding.textInputServerAddress.setText(it) }
        }
        launch {
          viewModel.printerIp.collect { binding.textInputPrinterIp.setText(it) }
        }
        launch {
          viewModel.printerPort.collect { binding.textInputPrinterPort.setText(it) }
        }
        launch {
          viewModel.storeNumber.collect { binding.textInputMgvStoreNumber.setText(it.toString()) }
        }
        launch {
          viewModel.printerType.collect { type ->
            binding.radioGroupPrinterType.check(getPrinterTypeId(type))
          }
        }
        launch {
          binding.checkboxAutomaticPrint.isChecked = viewModel.automaticPrintingEnable
        }
      }
    }
  }


  override fun onResume() {
    super.onResume()
    loadSavedConfigs()
  }

  private fun loadSavedConfigs() {
    val configRepository = ConfigRepository(requireContext())

//    val automaticPrinting = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)
    binding.checkboxAutomaticPrint.isChecked = viewModel.automaticPrintingEnable
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val store = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER)
    val printerType = configRepository.getTextConfig(ConfigRepository.PRINTER_TYPE) ?: "Network"

    binding.apply {
//      checkboxAutomaticPrint.isChecked = automaticPrinting
      textInputServerAddress.setText(serverAddress)
      textInputPrinterIp.setText(printerAddress)
      textInputPrinterPort.setText(portAddress.toString())
      textInputMgvStoreNumber.setText(store.toString())
      radioGroupPrinterType.check(getPrinterTypeId(printerType))
    }
  }
}

 */



/*
  private fun carregarConfiguracoes() {
    binding.textInputPrinterIp.setText(configRepository.getPrinterIp() ?: "")
    binding.textInputPrinterPort.setText(configRepository.getPrinterPort().toString())

    binding.textInputMgvPassword.setText(configRepository.getTextConfig(ConfigRepository.PASSWORD_MGV) ?: "")
    binding.textInputMgvStoreNumber.setText(configRepository.getIntConfig(ConfigRepository.STORE_NUMBER).toString())
    binding.textInputServerAddress.setText(configRepository.getTextConfig(ConfigRepository.SERVER_ADDRESS) ?: "")

    binding.checkboxAutomaticPrint.isChecked = configRepository.getBooleanConfig(ConfigRepository.AUTOMATIC_PRINT)

    when (configRepository.getPrinterType()) {
      PrinterType.USB -> binding.radioButtonUSB.isChecked = true
      PrinterType.NETWORK -> binding.radioButtonNetwork.isChecked = true
    }

    val templateKg = configRepository.getTextConfig("LABEL_TEMPLATE_KG")
    val templateQuant = configRepository.getTextConfig("LABEL_TEMPLATE_QUANT")
    selecionarItemNoSpinner(binding.spinnerLabelKg, templateKg)
    selecionarItemNoSpinner(binding.spinnerLabelQuant, templateQuant)
  }


  private fun salvarConfiguracoes() {
    configRepository.setTextConfig(ConfigRepository.PRINTER_IP, binding.textInputPrinterIp.text.toString())
    configRepository.setIntConfig(ConfigRepository.PRINTER_PORT, binding.textInputPrinterPort.text.toString().toIntOrNull() ?: 9100)

    configRepository.setTextConfig(ConfigRepository.PASSWORD_MGV, binding.textInputMgvPassword.text.toString())
    configRepository.setIntConfig(ConfigRepository.STORE_NUMBER, binding.textInputMgvStoreNumber.text.toString().toIntOrNull() ?: 1)
    configRepository.setTextConfig(ConfigRepository.SERVER_ADDRESS, binding.textInputServerAddress.text.toString())

    configRepository.setBooleanConfig(ConfigRepository.AUTOMATIC_PRINT, binding.checkboxAutomaticPrint.isChecked)

    val tipo = when {
      binding.radioButtonUSB.isChecked -> PrinterType.USB
      else -> PrinterType.NETWORK
    }
    configRepository.setPrinterType(tipo.toString())

    val templateKg = binding.spinnerLabelKg.selectedItem?.toString() ?: ""
    val templateQuant = binding.spinnerLabelQuant.selectedItem?.toString() ?: ""
    configRepository.setTextConfig("LABEL_TEMPLATE_KG", templateKg)
    configRepository.setTextConfig("LABEL_TEMPLATE_QUANT", templateQuant)
  }

 */
