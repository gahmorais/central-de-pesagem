package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.centraldepesagem.databinding.FragmentReportBinding
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import br.com.backupautomacao.centraldepesagem.ui.OperationListAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.ReportViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory.ReportViewModelFactory
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ReportFragment : Fragment() {

  private var _binding: FragmentReportBinding? = null
  private val binding get() = _binding!!

  private val reportViewModel by activityViewModels<ReportViewModel> {
    val repository: OperationRepository by inject()
    ReportViewModelFactory(repository)
  }

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