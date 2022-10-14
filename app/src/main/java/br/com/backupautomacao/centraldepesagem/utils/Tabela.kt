package br.com.backupautomacao.centraldepesagem.utils

import androidx.compose.ui.text.toLowerCase

fun findItem(item: String): Int {
  return when (Util.removeAccents(item).lowercase()) {
    "alcatra" -> 352
    "bacon manta rio sul" -> 1
    "carne seca friboi" -> 29
    "carne seca coxao duro" -> 154
    "coxao mole bovino" -> 1345
    "apresuntado nobreza" -> 7
    "bacon aurora" -> 9
    "calabresa deli" -> 30
    "pe suino" -> 32
    "queijo coalho cristina" -> 38
    "jilo" -> 81
    "uva niagra" -> 82
    "maca argentina" -> 89
    "manga espada" -> 96
    "limao siciliano" -> 103
    "bolo seco" -> 821
    "mini baguete" -> 896
    else -> 0
  }
}