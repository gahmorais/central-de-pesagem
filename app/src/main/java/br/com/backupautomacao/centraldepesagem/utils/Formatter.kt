package br.com.backupautomacao.centraldepesagem.utils

import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.round

class Formatter {
  companion object {
    fun formatPrice(value: Double): String {
      return String.format(
        Locale.ENGLISH,
        "R$ %.2f",
        value
      ).replace(".", ",")
    }

    fun formatWeight(value: Double?): String {
      return String.format(Locale.ENGLISH, "%.3f Kg", value).replace(".", ",")
    }

    fun formatFromUranoUseCb2Protocol(data: ByteArray): Double {
      val s = String(data, StandardCharsets.UTF_8)
      val noAlphaNum = "[^a-zA-Z0-9.]".toRegex()

      return s
        .replace(noAlphaNum, "")
        .substringAfter("PESOL")
        .substringBefore("k")
        .toDouble()
    }
    fun formatBarcode(code: String, price: Double): String {
      val priceDoubleToInt = round(price * 100).toInt()
      val priceFormat = priceDoubleToInt.toString().padStart(5, '0')
      return "2${code.padStart(6, '0')}${priceFormat}"
    }
  }
}