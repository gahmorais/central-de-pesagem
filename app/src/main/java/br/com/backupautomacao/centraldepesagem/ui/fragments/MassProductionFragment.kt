package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

  private fun configureListeners() {
    binding.etProductCode.setOnKeyListener { _, keycode, event ->
      if (event.action == KeyEvent.ACTION_DOWN) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER) {
          println("Botão ok pressionado")
          fetchItem()
          return@setOnKeyListener true
        }
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
    viewModel.print()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }
}