package br.com.backupautomacao.centraldepesagem.db.repository

import br.com.backupautomacao.centraldepesagem.db.dao.LabelDao
import br.com.backupautomacao.centraldepesagem.db.model.Label
import kotlinx.coroutines.coroutineScope

class LabelRepository(private val labelDao: LabelDao) {
    suspend fun getLabelByName(name: String): Label = labelDao.getLabelByName(name)
    suspend fun getLabelContentByName(name: String): String = labelDao.getLabelByName(name).content

}