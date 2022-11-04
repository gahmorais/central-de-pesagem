package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository

class ReportViewModel(private val operationRepository: OperationRepository) : ViewModel() {
  val allOperations: LiveData<List<Operation>> = operationRepository.allOperations.asLiveData()
}