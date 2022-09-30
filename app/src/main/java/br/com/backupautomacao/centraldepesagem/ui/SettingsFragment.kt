package br.com.backupautomacao.centraldepesagem.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import br.com.backupautomacao.centraldepesagem.databinding.FragmentSettingsBinding
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.SettingsViewModelFactory
import br.com.backupautomacao.tabletinteligente.utils.EventMask

private const val TAG = "SettingsFragment"
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
  }

  private fun handleMessages() {
    viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}