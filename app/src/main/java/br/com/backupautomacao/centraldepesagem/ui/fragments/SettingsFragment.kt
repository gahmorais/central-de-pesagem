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
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.db.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.utils.EventMask
import br.com.backupautomacao.centraldepesagem.utils.PrinterType
import kotlinx.coroutines.flow.onEach
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
        viewModel.statusMessage.onEach { message ->
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
            val deviceNames = deviceList.mapNotNull { it.productName }
            val adapter = getUsbPrinterListAdapter(deviceNames)

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
        val selectedPrinterType = when (binding.radioGroupPrinterType.checkedRadioButtonId) {
            R.id.radioButtonUSB -> "USB"
            R.id.radioButtonNetwork -> "Network"
            else -> null
        }

        if (selectedPrinterType == "Network") {
            val ipAddress = binding.textInputPrinterIp.text.toString()
            val port = binding.textInputPrinterPort.text.toString()

            viewModel.savePrinterAddress(ipAddress, port)
        }

        selectedPrinterType?.let {
            viewModel.savePrinterConfig(it)
            Toast.makeText(
                requireContext(),
                "Configuração de impressora salva!",
                Toast.LENGTH_SHORT
            ).show()
        } ?: Toast.makeText(
            requireContext(),
            "Selecione um tipo de impressora.",
            Toast.LENGTH_SHORT
        ).show()
    }


    /*
    private fun savePrinterConfig() {
        val selectedPrinterType = when (binding.radioGroupPrinterType.checkedRadioButtonId) {
            R.id.radioButtonUSB -> "USB"
            R.id.radioButtonNetwork -> "Network"
            else -> null
        }
        selectedPrinterType?.let {
            viewModel.savePrinterConfig(it)
            Toast.makeText(
                requireContext(),
                "Configuração de impressora salva!",
                Toast.LENGTH_SHORT
            ).show()
        } ?: Toast.makeText(
            requireContext(),
            "Selecione um tipo de impressora.",
            Toast.LENGTH_SHORT
        ).show()
    }

     */

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
                    binding.linearLayoutUsb.visibility = View.GONE
                    viewModel.savePrinterConfig("Network")
                }

                R.id.radioButtonUSB -> {
                    binding.linearLayoutNetworkPrinter.visibility = View.GONE
                    binding.linearLayoutUsb.visibility = View.VISIBLE
                    viewModel.savePrinterConfig("USB")
                    detectUsbPrinter()
                }
            }
        }
    }

    private fun detectUsbPrinter() {
        val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return

        val deviceList = usbManager.deviceList.values
        if (deviceList.isNotEmpty()) {
            val deviceNames = deviceList.mapNotNull { it.productName }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                deviceNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            with(binding) {
                spinnerUsbDevices.adapter = adapter
                statusImpressora.text = "Impressora USB detectada!"

                spinnerUsbDevices.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            adapterView: AdapterView<*>?,
                            p1: View?,
                            i: Int,
                            p3: Long
                        ) {
                            adapterView?.getItemAtPosition(i)?.toString()
                                ?.takeIf { it.isNotEmpty() }?.let { printerName ->
                                    viewModel?.savePrinterName(printerName)
                                    Log.i(
                                        "SettingsFragment",
                                        "Impressora selecionada: $printerName"
                                    )
                                }
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                    }
            }
        } else {
            with(binding) {
                statusImpressora.text = "Nenhuma impressora USB detectada."
            }
            Toast.makeText(
                requireContext(),
                "Nenhuma impressora USB encontrada.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*
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

     */

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