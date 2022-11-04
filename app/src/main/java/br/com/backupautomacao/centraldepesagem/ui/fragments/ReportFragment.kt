package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentReportBinding
import br.com.backupautomacao.centraldepesagem.ui.OperationListAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.ReportViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory.ReportViewModelFactory

class ReportFragment : Fragment() {

  private var _binding: FragmentReportBinding? = null
  private val binding get() = _binding!!

  private val reportViewModel by activityViewModels<ReportViewModel> {
    val repository = (requireActivity().application as AppApplication).operationRepository
    ReportViewModelFactory(repository)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
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
    reportViewModel.allOperations.observe(viewLifecycleOwner) { operations ->
      adapter.submitList(operations)
    }

    binding.reportRecyclerview.setOnLongClickListener {
      it.isClickable
    }

  }

}