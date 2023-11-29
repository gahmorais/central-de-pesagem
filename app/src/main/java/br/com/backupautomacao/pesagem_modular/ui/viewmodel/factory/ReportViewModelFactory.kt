package br.com.backupautomacao.pesagem_modular.ui.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.pesagem_modular.db.repository.OperationRepository
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.ReportViewModel

class ReportViewModelFactory(private val operationRepository: OperationRepository) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return ReportViewModel(operationRepository) as T
  }
}