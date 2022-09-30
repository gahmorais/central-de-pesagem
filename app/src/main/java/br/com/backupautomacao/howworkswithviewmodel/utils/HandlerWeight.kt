package br.com.backupautomacao.tabletinteligente.utils

import java.nio.charset.StandardCharsets
import java.util.*

fun handlerWeightUranoScale(s: StringBuilder): Double {
  var weightString = String(s)
  weightString = weightString
    .substringAfter(":")
    .substringBefore("g")
    .trim()
  return weightString.toDouble()
}

fun handlerUranoProtocolUsecb2(data: ByteArray): Double {
  val s = String(data, StandardCharsets.UTF_8)
  val noAlphaNum = "[^a-zA-Z0-9.]".toRegex()

  return s
    .replace(noAlphaNum, "")
    .substringAfter("PESOL")
    .substringBefore("k")
    .toDouble()
}

fun handlerWeightUranoPOPS(s: String): Double {
  return s.substringAfter("N0")
    .substringBefore("kg")
    .replace(",", ".")
    .trim()
    .toDouble()
}

fun handlerWeightToledoScale(b: ByteArray): Double {
  val weightInGrams = String(b, StandardCharsets.UTF_8).replace("\\p{C}", "").toDouble()
  return weightInGrams / 1000
}

fun formatWeight(value: Double?): String {
  return String.format(Locale.ENGLISH, "%.3f Kg", value).replace(".", ",")
}

fun formatPrice(value: Double): String {
  return String.format(
    Locale.ENGLISH,
    "R$ %.2f",
    value
  ).replace(".", ",")
}