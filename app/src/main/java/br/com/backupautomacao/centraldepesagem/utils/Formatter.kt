package br.com.backupautomacao.centraldepesagem.utils

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round

class Formatter private constructor() {
  companion object {
    fun formatPrice(value: Double): String {
      return String.format(
        Locale.ENGLISH,
        "%.2f",
        value
      ).replace(".", ",")
    }

    fun <T> formatWeight(value: T?): String {
      return String.format(Locale.ENGLISH, "%.3f Kg", value).replace(".", ",")
    }

//        fun String.removeNonAlphaCharacters(): String {
//            val regex = "[^a-zA-Z0-9.]".toRegex()
//            return replace(regex, "")
//        }

    fun formatFromToledoProtocolP05(data: ByteArray): Double {
      val s = convertByteArrayToString(data)
      val number = removeNonAlphaCharacters(s).toDouble()
      return number / 1000
    }

    fun formatDataToLoadPriceToledo(value: Double): ByteArray {
      val stx = "\u0002"
      val etx = "\u0003"
      val priceInteger = round(value * 100).toInt()
      val priceText = priceInteger.toString().padStart(6, '0')
      val data = "$stx$priceText$etx"
      return data.toByteArray()
    }

    fun formatFromUranoProtocolUseCb2(data: ByteArray): Double {
      val s = convertByteArrayToString(data)

      return removeNonAlphaCharacters(s)
        .substringAfter("PESOL")
        .substringBefore("k")
        .toDouble()
    }

    fun formatBarcode(code: String, price: Double): String {
      val priceDoubleToInt = round(price * 100).toInt()
      val priceFormat = priceDoubleToInt.toString().padStart(5, '0')
      return "2${code.padStart(6, '0')}${priceFormat}"
    }

    fun <T> formatNutriction(value: T?): String {
      if (value == null) {
        return ""
      }
      if (value is Int) {
        return String.format(Locale.ENGLISH, "%d ", value)
      }

      val doubleValue = value.toString().toDoubleOrNull()
      return if (doubleValue != null)
        String.format(Locale.ENGLISH, "%.1f ", doubleValue).replace(".", ",")
      else
        "0,0 "
    }

    fun <T> formatNutrictionMg(value: T?): String {
      if (value == null) {
        return ""
      }
      if (value is Int) {
        return String.format(Locale.ENGLISH, "%d ", value)
      }

      val doubleValue = value.toString().toDoubleOrNull()
      return if (doubleValue != null)
        String.format(Locale.ENGLISH, "%.1f ", doubleValue).replace(".", ",")
      else
        "0,0"
    }

    fun <T> formatNutrictionKcal(value: T?): String {
      if (value == null) {
        return ""
      }
      if (value is Int) {
        return String.format(Locale.ENGLISH, "%d  ", value)
      }

      val doubleValue = value.toString().toDoubleOrNull()
      return if (doubleValue != null)
        String.format(Locale.ENGLISH, "%.1f  ", doubleValue).replace(".", ",")
      else
        "0,0 "
    }

    fun formatDateTime(timestamp: Long): String {
      val formatter = SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.ENGLISH)
      return formatter.format(timestamp)
    }

    fun formatVD(value: String?): String {
      return value
        ?.replace(",", ".")
        ?.toDoubleOrNull()
        ?.let { String.format(Locale.getDefault(), "%.0f%%", it) }
        ?: ""
    }
    fun formatDate(timestamp: Long): String {
      val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
      return formatter.format(timestamp)
    }

    fun removeNonAlphaCharacters(s: String): String {
      val regex = "[^a-zA-Z0-9.]".toRegex()
      return s.replace(regex, "")
    }

    private fun convertByteArrayToString(data: ByteArray): String {
      return String(data, StandardCharsets.UTF_8)
    }

     fun isUnidadePorQuantidade(unidade: String?): Boolean {
      val un = unidade?.trim()?.lowercase() ?: return false
      return un in listOf("un", "unidade", "peça", "peca", "dúzia", "duzia", "caixa", "maco", "maço")
    }

     fun isUnidadePorPesoOuVolume(unidade: String?): Boolean {
      val un = unidade?.trim()?.lowercase() ?: return false
      return un in listOf("g", "kg", "t", "ml", "l")
    }
  }
}