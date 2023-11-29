package br.com.backupautomacao.pesagem_modular.utils

fun String.properties(copies: Int): String {
  return replace("var_copies", copies.toString())
}