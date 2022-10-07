package br.com.backupautomacao.centraldepesagem.utils

import androidx.compose.ui.text.toLowerCase

fun findItem(item: String): Int {
  return when (Util.removeAccents(item).lowercase()) {
    "alcatra" -> 352
    "bacon" -> 1
    "carne seca friboi" -> 29
    "carne seca coxao duro" -> 154
    "coxao mole bovino" -> 1345
    else -> 0
  }
}