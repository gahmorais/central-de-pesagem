package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModelFactory
import br.com.backupautomacao.centraldepesagem.utils.EventMask

class SettingsFragment : Fragment() {
  private var _binding: FragmentSettingsBinding? = null
  private val binding get() = _binding!!
  private val viewModel: SettingsViewModel by lazy {
    val configRepository = ConfigRepository(requireActivity())
    val factory = SettingsViewModelFactory(configRepository)
    ViewModelProvider(this, factory)[SettingsViewModel::class.java]
  }

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
    binding.buttonSaveIp.setOnClickListener {
      val printerIp: String = binding.textInputPrinterIp.text.toString()
      viewModel.savePrinterAddress(printerIp)
    }

    binding.buttonSaveServerAddress.setOnClickListener {
      val serverAddress: String = binding.textInputServerAddress.text.toString()
      viewModel.saveServerAddress(serverAddress)
    }
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
    binding.textInputServerAddress.setText(serverAddress)
    binding.textInputPrinterIp.setText(printerAddress)
  }
}