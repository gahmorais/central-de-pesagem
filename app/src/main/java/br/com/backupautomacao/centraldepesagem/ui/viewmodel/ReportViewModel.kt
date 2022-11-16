package br.com.backupautomacao.centraldepesagem.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import br.com.backupautomacao.centraldepesagem.db.repository.OperationRepository

class ReportViewModel(operationRepository: OperationRepository) : ViewModel() {
  val allOperations: LiveData<List<Operation>> = operationRepository.allOperations.asLiveData()
}