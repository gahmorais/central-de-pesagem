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
import androidx.work.WorkInfo
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMassProductionBinding
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MassProductionViewModel
import br.com.backupautomacao.centraldepesagem.utils.Formatter
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MassProductionFragment : Fragment() {
  private val binding: FragmentMassProductionBinding by lazy {
    FragmentMassProductionBinding.inflate(layoutInflater)
  }
  private val viewModel: MassProductionViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    configureListeners()
    monitoringPrint()
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
    viewModel.statusPrint.onEach { workInfos ->
      Timber.i("monitoringPrint: $workInfos")
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
            Timber.i("monitoringPrint: Rodando tarefa")
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

    binding.etWeight.setOnEditorActionListener { textView, actionId, _ ->
      Timber.i("configureListeners: $actionId")
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        val weight = textView.text.toString().toDouble()
        viewModel.updateWeight(weight)
        closeVirtualKeyboard(textView)
        return@setOnEditorActionListener true
      }
      false
    }

    binding.printButton.setOnClickListener {
      print()
    }

    viewModel.mgvProduct.onEach { product ->
      binding.apply {
        product?.let {
          tvProductCode.text = it.code
          tvProductDescription.text = it.description
          tvProductPrice.text =
            if (it.price != 0.0) Formatter.formatPrice(it.price) else ""
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
      etProductCode.setText("")
      etLabelQuantity.setText("")
      etWeight.setText("")
    }
  }

  private fun fetchItem() {
    val code = binding.etProductCode.text.toString()
    viewModel.fetchItem(code)
  }

  private fun print() {
    if (binding.etProductCode.text.isNullOrEmpty()) {
      Toast.makeText(requireActivity(), "Insira o código do produto", Toast.LENGTH_SHORT)
        .show()
      return
    }
    if (binding.etWeight.text.isNullOrEmpty()) {
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

    clearFields()
  }

}