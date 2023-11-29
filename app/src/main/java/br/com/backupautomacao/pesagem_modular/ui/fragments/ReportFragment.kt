package br.com.backupautomacao.pesagem_modular.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.pesagem_modular.databinding.FragmentReportBinding
import br.com.backupautomacao.pesagem_modular.ui.OperationListAdapter
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReportFragment : Fragment() {

  private var _binding: FragmentReportBinding? = null
  private val binding get() = _binding!!
  private val reportViewModel: ReportViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentReportBinding.inflate(inflater)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configureRecyclerview()
  }

  private fun configureRecyclerview() {
    val adapter = OperationListAdapter()
    binding.reportRecyclerview.adapter = adapter
    lifecycleScope.launch {
      reportViewModel.allOperations.collect { operations ->

        adapter.submitList(operations)
      }
    }
//    reportViewModel.allOperations.observe(viewLifecycleOwner) {  ->
//    }

    binding.reportRecyclerview.setOnLongClickListener {
      it.isClickable
    }
  }
}