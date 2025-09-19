package br.com.backupautomacao.centraldepesagem.utils

enum class ScaleStatus(val value: String) {
  UNSTABLE("IIIII"),
  NEGATIVE_WEIGHT("NNNNN"),
  OVERWEIGHT("SSSSS"),
}