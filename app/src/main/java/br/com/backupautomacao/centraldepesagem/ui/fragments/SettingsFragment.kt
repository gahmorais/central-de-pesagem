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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask


class SettingsFragment : Fragment() {
  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private val viewModel: SettingsViewModel by viewModels()
  private val printerName = ""
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel

    return binding.root
  }

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
    binding.buttonSavePrinterAddress.setOnClickListener {
      savePrinterConfig()
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

  private fun savePrinterConfig() {
    val printerIp: String = binding.textInputPrinterIp.text.toString()
    val printerPort: String = binding.textInputPrinterPort.text.toString()
    viewModel.savePrinterAddress(printerIp, printerPort)
    viewModel.savePrinterConfig("USB")
    viewModel.savePrinterName(printerName)
  }

  private fun handleMessages() {
    viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
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
          binding.linearLayoutNetworkPrinter.visibility = View.VISIBLE
          binding.linearLayoutUSBPrinter.visibility = View.GONE
          viewModel.savePrinterConfig("Network")
        }
        R.id.radioButtonUSB -> {
          binding.linearLayoutNetworkPrinter.visibility = View.GONE
          binding.linearLayoutUSBPrinter.visibility = View.VISIBLE

          detectUsbPrinter()
        }
      }
    }
  }

  private fun detectUsbPrinter() {
    val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
    val deviceList = usbManager.deviceList.values

    if (deviceList.isNotEmpty()) {
      val deviceNames = deviceList.map { it.productName }
      val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, deviceNames)
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      binding.spinnerUsbDevices.adapter = adapter

      binding.statusImpressora.text = "Impressora USB detectada!"
      val itemListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
          val printerName = adapterView?.getItemAtPosition(i).toString()
          if (printerName.isNotEmpty())
          viewModel.savePrinterName(printerName)
          Log.i("SettingsFragment", "onItemSelected: $printerName")
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
      }
      binding.spinnerUsbDevices.onItemSelectedListener = itemListener
    } else {
      binding.statusImpressora.text = "Nenhuma impressora USB detectada."
      Toast.makeText(requireContext(), "Nenhuma impressora USB encontrada.", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onResume() {
    super.onResume()
    binding.checkboxAutomaticPrint.isChecked = viewModel.automaticPrintingEnable
    val configRepository = ConfigRepository(requireActivity())
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val store = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER)
    binding.apply {
      textInputServerAddress.setText(serverAddress)
      textInputPrinterPort.setText(portAddress.toString())
      textInputPrinterIp.setText(printerAddress)
      textInputMgvStoreNumber.setText(store.toString())
    }
  }
}






//Código sem spinner
/*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask

class SettingsFragment : Fragment() {
  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private val viewModel: SettingsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel

    return binding.root
  }

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
    binding.buttonSavePrinterAddress.setOnClickListener {
      savePrinterConfig()
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

  private fun savePrinterConfig() {
    val printerIp: String = binding.textInputPrinterIp.text.toString()
    val printerPort: String = binding.textInputPrinterPort.text.toString()
    viewModel.savePrinterAddress(printerIp, printerPort)
  }

  private fun handleMessages() {
    viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleCheckbox() {
    binding.checkboxAutomaticPrint.setOnCheckedChangeListener { _, isChecked ->
      viewModel.enableAutomaticPrint(isChecked)
    }
  }

  private fun handleRadioGroup() {
    binding.radioGroupPrinterType.setOnCheckedChangeListener { group, checkedId ->
      when (checkedId) {
        R.id.radioButtonNetwork -> {
          binding.linearLayoutNetworkPrinter.visibility = View.VISIBLE
          binding.linearLayoutUSBPrinter.visibility = View.GONE
          viewModel.savePrinterType(false)
        }
        R.id.radioButtonUSB -> {
          binding.linearLayoutNetworkPrinter.visibility = View.GONE
          binding.linearLayoutUSBPrinter.visibility = View.VISIBLE
          viewModel.savePrinterType(true)
          detectUsbPrinter()
        }
      }
    }
  }

  private fun detectUsbPrinter() {
    binding.statusImpressora.text = "Impressora USB detectada!"
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onResume() {
    super.onResume()
    binding.checkboxAutomaticPrint.isChecked = viewModel.automaticPrintingEnable
    val configRepository = ConfigRepository(requireActivity())
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val store = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER)
    binding.apply {
      textInputServerAddress.setText(serverAddress)
      textInputPrinterPort.setText(portAddress.toString())
      textInputPrinterIp.setText(printerAddress)
      textInputMgvStoreNumber.setText(store.toString())
    }
  }
}

 */





//CÓDIGO SEM MGV
/*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask

class SettingsFragment : Fragment() {
  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private val viewModel: SettingsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    handleMessages()
    handleButtons()
    handleTextInputs()
    handleRadioGroup()
  }

  private fun handleTextInputs() {
    val eventMask = EventMask(binding.textInputPrinterIp, EventMask.MASCARA_IP)
    binding.textInputPrinterIp.addTextChangedListener(eventMask)
  }

  private fun handleButtons() {
    binding.buttonSavePrinterAddress.setOnClickListener {
      savePrinterConfig()
    }
  }

  private fun savePrinterConfig() {
    if (binding.radioButtonUSB.isChecked) {
      Toast.makeText(requireContext(), "USB Printer Selected", Toast.LENGTH_SHORT).show()
    } else {
      val printerIp = binding.textInputPrinterIp.text.toString()
      val printerPort = binding.textInputPrinterPort.text.toString()
      viewModel.savePrinterAddress(printerIp, printerPort)
    }
  }

  private fun handleMessages() {
    viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleRadioGroup() {
    binding.radioGroupPrinterType.setOnCheckedChangeListener { _, checkedId ->
      when (checkedId) {
        R.id.radioButtonNetwork -> {
          binding.linearLayoutNetworkPrinter.visibility = View.VISIBLE
          binding.linearLayoutUSBPrinter.visibility = View.GONE
        }
        R.id.radioButtonUSB -> {
          binding.linearLayoutNetworkPrinter.visibility = View.GONE
          binding.linearLayoutUSBPrinter.visibility = View.VISIBLE
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onResume() {
    super.onResume()
    val configRepository = ConfigRepository(requireActivity())
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val isUsbSelected = configRepository.getConfig(ConfigRepository.USB_PRINTER_NAME)?.isNotEmpty() ?: false

    binding.apply {
      textInputPrinterIp.setText(printerAddress)
      textInputPrinterPort.setText(portAddress.toString())
      radioButtonUSB.isChecked = isUsbSelected
      radioButtonNetwork.isChecked = !isUsbSelected
      linearLayoutUSBPrinter.visibility = if (isUsbSelected) View.VISIBLE else View.GONE
      linearLayoutNetworkPrinter.visibility = if (isUsbSelected) View.GONE else View.VISIBLE
    }
  }
}
*/


//Código Original
/*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask

class SettingsFragment : Fragment() {
  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private val viewModel: SettingsViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSettingsBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    handleMessages()
    handleButtons()
    handleTextInputs()
    handleCheckbox()
  }

  private fun handleTextInputs() {
    val eventMask = EventMask(binding.textInputPrinterIp, EventMask.MASCARA_IP)
    binding.textInputPrinterIp.addTextChangedListener(eventMask)
  }

  private fun handleButtons() {
    binding.buttonSavePrinterAddress.setOnClickListener {
      savePrinterConfig()
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

  private fun savePrinterConfig() {
    val printerIp: String = binding.textInputPrinterIp.text.toString()
    val printerPort: String = binding.textInputPrinterPort.text.toString()
    viewModel.savePrinterAddress(printerIp, printerPort)
  }

  private fun handleMessages() {
    viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleCheckbox() {
    binding.checkboxAutomaticPrint.setOnCheckedChangeListener { _, isChecked ->
      viewModel.enableAutomaticPrint(isChecked)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onResume() {
    super.onResume()
    binding.checkboxAutomaticPrint.isChecked = viewModel.automaticPrintingEnable
    val configRepository = ConfigRepository(requireActivity())
    val serverAddress = configRepository.getConfig(ConfigRepository.SERVER_ADDRESS)
    val printerAddress = configRepository.getConfig(ConfigRepository.PRINTER_IP)
    val portAddress = configRepository.getIntConfig(ConfigRepository.PRINTER_PORT)
    val store = configRepository.getIntConfig(ConfigRepository.STORE_NUMBER)
    binding.apply {
      textInputServerAddress.setText(serverAddress)
      textInputPrinterPort.setText(portAddress.toString())
      textInputPrinterIp.setText(printerAddress)
      textInputMgvStoreNumber.setText(store.toString())
    }
  }
}

 */