package br.com.backupautomacao.pesagem_modular.db.repository

import br.com.backupautomacao.pesagem_modular.db.dao.LabelDao
import br.com.backupautomacao.pesagem_modular.db.model.Label

class LabelRepository(private val labelDao: LabelDao) {
    suspend fun getLabelByName(name: String): Label = labelDao.getLabelByName(name)
    suspend fun getLabelContentByName(name: String): String = labelDao.getLabelByName(name).content

}