package br.com.backupautomacao.pesagem_modular.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.backupautomacao.pesagem_modular.db.repository.OperationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportViewModel(operationRepository: OperationRepository) : ViewModel() {
    private val _allOperations = MutableStateFlow(operationRepository.allOperations)
    val allOperations = _allOperations.asStateFlow()
}