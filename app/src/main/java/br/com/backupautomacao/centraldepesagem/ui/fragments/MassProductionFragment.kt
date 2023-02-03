package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMassProductionBinding
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MassProductionViewModel
import br.com.backupautomacao.centraldepesagem.utils.Formatter

class MassProductionFragment : Fragment() {
  private var _binding: FragmentMassProductionBinding? = null
  private val binding get() = _binding!!

  private val viewModel: MassProductionViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentMassProductionBinding.inflate(inflater)
    binding.lifecycleOwner = this

    configureListeners()

    return binding.root
  }

  fun closeVirtualKeyboard(view: View) {
    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }


  private fun configureListeners() {
    binding.etProductCode.setOnEditorActionListener { textView, actionId, keyEvent ->
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        fetchItem()
        closeVirtualKeyboard(textView)
        return@setOnEditorActionListener true
      }
      false
    }

    binding.etWeight.setOnEditorActionListener { textView, actionId, keyEvent ->
      Log.i("MassProductionFragment", "configureListeners: $actionId")
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

    viewModel.mgvProduct.observe(viewLifecycleOwner) { product ->
      binding.apply {
        product?.let {
          tvProductCode.text = it.code
          tvProductDescription.text = it.description
          tvProductPrice.text = Formatter.formatPrice(it.price)
        } ?: kotlin.run {
          tvProductCode.text = ""
          tvProductDescription.text = ""
          tvProductPrice.text = ""
        }

      }
    }
  }

  private fun fetchItem() {
    val code = binding.etProductCode.text.toString()
    viewModel.fetchItem(code)
  }

  private fun print() {
    val quantity = binding.etLabelQuantity.text.toString()
    viewModel.print(quantity)
  }

}