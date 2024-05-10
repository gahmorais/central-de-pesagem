package br.com.backupautomacao.pesagem_modular.data.repository

import br.com.backupautomacao.pesagem_modular.data.dao.LabelDao
import br.com.backupautomacao.pesagem_modular.data.model.Label

class LabelRepository(private val labelDao: LabelDao) {
  suspend fun getLabelByName(name: String): Label = labelDao.getLabelByName(name)
  suspend fun getLabelContentByName(name: String): String = labelDao.getLabelByName(name).content

}