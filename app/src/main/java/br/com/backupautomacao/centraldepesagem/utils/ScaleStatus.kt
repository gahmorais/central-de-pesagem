package br.com.backupautomacao.centraldepesagem.utils

enum class ScaleStatus(val value: String) {
  UNSTABLE("IIIII"),
  NO_WEIGHT("NNNNN"),
  OVERWEIGHT("SSSSS"),
}