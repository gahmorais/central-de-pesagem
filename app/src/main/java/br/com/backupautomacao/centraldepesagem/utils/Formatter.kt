package br.com.backupautomacao.centraldepesagem.utils

import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

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

    fun formatDataToLoadTareToledo(value: Double): ByteArray {
      val stx = "\u0001"
      val etx = "\u0003"
      val tareInteger = round(value * 100).toInt()
      val tareText = tareInteger.toString().padStart(6, '0')
      val data = "$stx$tareText$etx"
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

      /*
      fun formatPorcoes(
          weight: Double?,
          qtdPorcaoStr: String?,
          unidadePorcao: String?,
          qtdPorcoesDeclarada: String?
      ): String {
          val pesoProdutoKilo = weight ?: 0.0
          val pesoProdutoGrama = pesoProdutoKilo * 1000
          val pesoPorcao = qtdPorcaoStr?.toDoubleOrNull() ?: 0.0

          Timber.d("Peso do produto: $pesoProdutoKilo, Peso da porção: $pesoPorcao")

          val porcoesCalculadas = if (pesoProdutoGrama > 0 && pesoPorcao > 0) {
              val calculo = pesoProdutoGrama / pesoPorcao
              Timber.d("Porções calculadas (antes de arredondar): $calculo")
              kotlin.math.round(calculo)
          } else {
              0.0
          }

          Timber.d("Porções finais: $porcoesCalculadas")

          return if (porcoesCalculadas > 0) {
              "Cerca de ${porcoesCalculadas.toInt()} porções"
          } else {
              qtdPorcoesDeclarada.orEmpty()
          }
      }
      */

      fun formatPorcoes(
          weight: Double?,
          qtdPorcaoStr: String?,
          unidadePorcao: String?,
          qtdPorcoesDeclarada: String?
      ): String {
          val pesoProdutoKilo = weight ?: 0.0
          val pesoProdutoGrama = pesoProdutoKilo * 1000
          val pesoPorcao = qtdPorcaoStr?.toDoubleOrNull() ?: 0.0

          Timber.d("Peso do produto: $pesoProdutoKilo kg, Peso da porção: $pesoPorcao g")

          if (pesoProdutoGrama <= 0 || pesoPorcao <= 0) {
              Timber.d("Peso do produto ou peso da porção inválido, retornando declarado")
              return qtdPorcoesDeclarada.orEmpty()
          }

          val calculo = pesoProdutoGrama / pesoPorcao
          Timber.d("Porções calculadas (antes de arredondar): $calculo")

          val porcoesArredondadas = kotlin.math.round(calculo)
          Timber.d("Porções arredondadas: $porcoesArredondadas")

          return when {
              porcoesArredondadas <= 0 -> qtdPorcoesDeclarada.orEmpty()
              calculo == porcoesArredondadas -> " ${porcoesArredondadas.toInt()} porções"
              else -> "Cerca de ${porcoesArredondadas.toInt()} porções"
          }
      }


      fun <T> formatNutriction(value: T?): String {
          if (value == null) return ""

          val doubleValue = value.toString().toDoubleOrNull()
          if (doubleValue != null) {
              return if (doubleValue % 1.0 == 0.0) {
                  String.format(Locale.ENGLISH, "%.0f ", doubleValue).replace(".", ",")
              } else {
                  if (doubleValue >= 10) {
                      String.format(Locale.ENGLISH, "%.0f ", doubleValue).replace(".", ",")
                  } else {
                      String.format(Locale.ENGLISH, "%.1f ", doubleValue).replace(".", ",")
                  }
              }
          }
          return "0"
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