package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.backupautomacao.centraldepesagem.databinding.FragmentReportBinding
import br.com.backupautomacao.centraldepesagem.ui.OperationListAdapter
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReportFragment : Fragment() {

  private val binding: FragmentReportBinding by lazy {
    FragmentReportBinding.inflate(layoutInflater)
  }
  private val reportViewModel: ReportViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = binding.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    configureRecyclerview()
  }

  private fun configureRecyclerview() {
    val adapter = OperationListAdapter()
    binding.reportRecyclerview.adapter = adapter
    reportViewModel.allOperations.onEach { operations ->
      adapter.submitList(operations.last())
    }

    binding.reportRecyclerview.setOnLongClickListener {
      it.isClickable
    }
  }
}