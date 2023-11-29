package br.com.backupautomacao.pesagem_modular.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.com.backupautomacao.pesagem_modular.databinding.FragmentSettingsBinding
import br.com.backupautomacao.pesagem_modular.db.repository.ConfigRepository
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.pesagem_modular.utils.EventMask
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {
  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private val viewModel: SettingsViewModel by viewModel()

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