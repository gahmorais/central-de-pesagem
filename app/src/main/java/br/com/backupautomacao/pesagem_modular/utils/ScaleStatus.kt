package br.com.backupautomacao.pesagem_modular.utils

enum class ScaleStatus(val value: String) {
  UNSTABLE("IIIII"),
  NEGATIVE_WEIGHT("NNNNN"),
  OVERWEIGHT("SSSSS"),
}