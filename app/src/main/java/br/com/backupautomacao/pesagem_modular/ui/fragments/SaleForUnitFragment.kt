package br.com.backupautomacao.pesagem_modular.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.backupautomacao.pesagem_modular.AppApplication
import br.com.backupautomacao.pesagem_modular.PrintWorker
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.databinding.FragmentSaleForUnitBinding
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.SaleForUnitViewModel
import br.com.backupautomacao.pesagem_modular.utils.Formatter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SaleForUnitFragment : Fragment() {
  private var _binding: FragmentSaleForUnitBinding? = null
  private val binding get() = _binding!!
  private lateinit var workManager: WorkManager
  private val viewModel: SaleForUnitViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentSaleForUnitBinding.inflate(inflater)
    binding.lifecycleOwner = this
    workManager = (requireActivity().application as AppApplication).workManager
    configureListeners()
    return binding.root
  }

  private fun closeVirtualKeyboard(view: View) {
    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }

  private fun startLoading(message: String) {
    binding.apply {
      spinnerLoading.tvMessage.text = message
      spinnerLoading.root.visibility = View.VISIBLE
      mainScreen.visibility = View.GONE
    }
  }

  private fun stopLoading() {
    binding.apply {
      spinnerLoading.root.visibility = View.GONE
      mainScreen.visibility = View.VISIBLE
    }
  }

  private fun monitoringPrint() {
    workManager
      .getWorkInfosForUniqueWorkLiveData(PrintWorker.PRINT_TASK)
      .observe(viewLifecycleOwner) { workInfos ->
        Log.i("MassProductionFragment", "monitoringPrint: $workInfos")
        if (workInfos.isNotEmpty()) {
          when (workInfos[0].state) {
            WorkInfo.State.SUCCEEDED -> {
              stopLoading()
              Toast.makeText(
                requireContext(),
                getString(R.string.printing_done),
                Toast.LENGTH_SHORT
              ).show()
            }

            WorkInfo.State.FAILED -> {
              stopLoading()
              Toast.makeText(
                requireContext(),
                getString(R.string.printing_error),
                Toast.LENGTH_SHORT
              ).show()
            }

            WorkInfo.State.CANCELLED -> {
              Toast.makeText(
                requireContext(),
                getString(R.string.printing_cancelled),
                Toast.LENGTH_SHORT
              ).show()
            }

            WorkInfo.State.RUNNING -> {
              Log.i("MainFragment", "monitoringPrint: Rodando tarefa")
            }

            WorkInfo.State.ENQUEUED -> {
              startLoading(getString(R.string.printing))
            }

            else -> stopLoading()
          }
        }
      }
  }

  private fun configureListeners() {
    binding.etProductCode.setOnEditorActionListener { textView, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        fetchItem()
        closeVirtualKeyboard(textView)
        return@setOnEditorActionListener true
      }
      false
    }

    binding.etProductQuantity.setOnEditorActionListener { textView, actionId, _ ->
      Log.i("MassProductionFragment", "configureListeners: $actionId")
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        val quantity = textView.text.toString().toInt()
        viewModel.updateQuantity(quantity)
        closeVirtualKeyboard(textView)
        return@setOnEditorActionListener true
      }
      false
    }

    binding.printButton.setOnClickListener {
      lifecycleScope.launch { print() }
    }

    viewModel.mgvProduct.observe(viewLifecycleOwner) { product ->
      binding.apply {
        product?.let {
          tvProductCode.text = it.code
          tvProductDescription.text = it.description
          tvProductPrice.text = if (it.price != 0.0) Formatter.formatPrice(it.price) else ""
        } ?: kotlin.run {
          tvProductCode.text = ""
          tvProductDescription.text = ""
          tvProductPrice.text = ""
        }
      }
    }
  }

  private fun clearFields() {
    binding.apply {
      etLabelQuantity.setText("")
      etProductQuantity.setText("")
    }
  }

  private fun fetchItem() {
    val code = binding.etProductCode.text.toString()
    viewModel.fetchItem(code)
  }

  private suspend fun print() {
    if (binding.etProductCode.text.isNullOrEmpty()) {
      Toast.makeText(requireActivity(), "Insira o código do produto", Toast.LENGTH_SHORT).show()
      return
    }
    if (binding.etProductQuantity.text.isNullOrEmpty()) {
      Toast.makeText(requireContext(), "Insira o peso do produto", Toast.LENGTH_SHORT).show()
      return
    }

    if (binding.etLabelQuantity.text.isNullOrEmpty()) {
      Toast.makeText(requireContext(), "Insira a quantidade de etiquetas", Toast.LENGTH_SHORT)
        .show()
      return
    }

    val quantity = binding.etLabelQuantity.text.toString()
    viewModel.print(quantity)
    monitoringPrint()
    clearFields()
  }

}