package br.com.backupautomacao.pesagem_modular.utils

fun String.properties(copies: Int): String {
  return replace("var_copies", copies.toString())
}

fun String.removeNonAlphaCharacters(): String {
  val regex = "[^a-zA-Z0-9.]".toRegex()
  return replace(regex, "")
}