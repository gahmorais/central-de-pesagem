package br.com.backupautomacao.centraldepesagem.ui.zpl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LabelViewModel : ViewModel() {

  private val _labels = MutableLiveData<List<ZplLabel>>()
  val labels: LiveData<List<ZplLabel>> = _labels

  fun loadLabelsFromAssets(labelList: List<ZplLabel>) {
    _labels.value = labelList
  }
}



/*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LabelViewModel: ViewModel() {

  private val _labels = MutableLiveData<List<ZplLabel>>().apply {
    value = listOf(
      ZplLabel(
        name = "Etiqueta Produto 01",
        zplCode = "^XA^FO50,50^A0N,50,50^FDProduto 01^FS^XZ"
      ),
      ZplLabel(
        name = "Etiqueta Produto 02",
        zplCode = "^XA^FO50,50^A0N,40,40^FDProduto 02^FS^XZ"
      )
    )
  }

  val labels: LiveData<List<ZplLabel>> = _labels
}

 */