package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportViewModel(operationRepository: OperationRepository) : ViewModel() {
    private val _allOperations = MutableStateFlow(operationRepository.allOperations)
    val allOperations = _allOperations.asStateFlow()
}