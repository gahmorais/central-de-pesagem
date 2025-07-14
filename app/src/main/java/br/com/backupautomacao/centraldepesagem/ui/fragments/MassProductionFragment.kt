package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMassProductionBinding
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.InputMode
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MassProductionViewModel
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import br.com.backupautomacao.centraldepesagem.utils.UnitOfMeasure
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MassProductionFragment : Fragment() {
  private var _binding: FragmentMassProductionBinding? = null
  private val binding get() = _binding!!

  private val viewModel: MassProductionViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentMassProductionBinding.inflate(inflater, container, false)
    configureListeners()
    monitoringPrint()
    observeViewModel()
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun closeVirtualKeyboard(view: View) {
    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
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

  private fun monitoringPrint() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.statusPrint.collect { workInfos ->
        if (workInfos.isNotEmpty()) {
          when (workInfos[0].state) {
            WorkInfo.State.SUCCEEDED -> {
              stopLoading()
              Toast.makeText(requireContext(), getString(R.string.printing_done), Toast.LENGTH_SHORT).show()
            }
            WorkInfo.State.FAILED -> {
              stopLoading()
              Toast.makeText(requireContext(), getString(R.string.printing_error), Toast.LENGTH_SHORT).show()
            }
            WorkInfo.State.CANCELLED -> {
              stopLoading()
              Toast.makeText(requireContext(), getString(R.string.printing_cancelled), Toast.LENGTH_SHORT).show()
            }
            WorkInfo.State.ENQUEUED -> {
              startLoading(getString(R.string.printing))
            }
            else -> stopLoading()
          }
        }
      }
    }
  }

  private fun configureListeners() {
    binding.etProductCode.setOnEditorActionListener { textView, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        fetchItem()
        closeVirtualKeyboard(textView)
        true
      } else false
    }

    binding.etWeight.setOnEditorActionListener { textView, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        val weight = textView.text.toString().toDoubleOrNull()
        if (weight != null) {
          viewModel.updateWeight(weight)
          closeVirtualKeyboard(textView)
          true
        } else {
          Toast.makeText(requireContext(), "Peso inválido", Toast.LENGTH_SHORT).show()
          false
        }
      } else false
    }

    binding.printButton.setOnClickListener {
      print()
    }
    binding.btnQuantityMode.setOnClickListener {
      viewModel.setMode(InputMode.QUANTITY)
    }

    binding.btnWeightMode.setOnClickListener {
      viewModel.setMode(InputMode.WEIGHT)
    }
  }

  private fun observeViewModel() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.mgvProduct.collect { product ->
        binding.tvProductCode.text = product?.code.orEmpty()
        binding.tvProductDescription.text = product?.description.orEmpty()
        binding.tvProductPrice.text =
          product?.price?.takeIf { it != 0.0 }?.let { Formatter.formatPrice(it) }.orEmpty()
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.inputMode.collect { mode ->
        when (mode) {
          InputMode.QUANTITY -> {
            binding.etLabelQuantity.visibility = View.VISIBLE
            binding.etWeight.visibility = View.GONE
            binding.etLabelQuantity.requestFocus()
          }
          InputMode.WEIGHT -> {
            binding.etWeight.visibility = View.VISIBLE
            binding.etLabelQuantity.visibility = View.GONE
            binding.etWeight.requestFocus()
          }
        }
      }
    }
  }

  private fun clearFields() {
    binding.etProductCode.setText("")
    binding.etLabelQuantity.setText("")
    binding.etWeight.setText("")
  }

  private fun fetchItem() {
    val code = binding.etProductCode.text.toString()
    viewModel.fetchItem(code)
  }

  private fun print() {
    if (binding.etProductCode.text.isNullOrEmpty()) {
      Toast.makeText(requireActivity(), "Insira o código do produto", Toast.LENGTH_SHORT).show()
      return
    }

    val mode = viewModel.inputMode.value

    when (mode) {
      InputMode.WEIGHT -> {
        val weightText = binding.etWeight.text.toString()
        if (weightText.isEmpty()) {
          Toast.makeText(requireContext(), "Insira o peso do produto", Toast.LENGTH_SHORT).show()
          return
        }
        val weight = weightText.toDoubleOrNull()
        if (weight == null || weight <= 0) {
          Toast.makeText(requireContext(), "Peso inválido", Toast.LENGTH_SHORT).show()
          return
        }
        viewModel.updateWeight(weight)
        viewModel.print("1")
      }

      InputMode.QUANTITY -> {
        val qtyText = binding.etLabelQuantity.text.toString()
        if (qtyText.isEmpty()) {
          Toast.makeText(requireContext(), "Insira a quantidade de etiquetas", Toast.LENGTH_SHORT).show()
          return
        }
        viewModel.print(qtyText)
      }
    }

    clearFields()
  }
}