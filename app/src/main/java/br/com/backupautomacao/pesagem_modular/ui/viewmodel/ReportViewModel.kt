package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.backupautomacao.pesagem_modular.data.model.Operation
import br.com.backupautomacao.pesagem_modular.data.repository.OperationRepository
import kotlinx.coroutines.flow.Flow

class ReportViewModel(operationRepository: OperationRepository) : ViewModel() {
  val allOperations: Flow<List<Operation>> = operationRepository.allOperations
}