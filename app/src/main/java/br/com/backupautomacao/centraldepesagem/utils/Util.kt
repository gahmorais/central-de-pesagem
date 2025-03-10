package br.com.backupautomacao.centraldepesagem.utils

import java.text.Normalizer
import kotlin.random.Random.Default.nextDouble

class Util {
    companion object {
        fun generateRandomDouble(): Double {
            val randomNumber = nextDouble()
            return randomNumber * 100
        }

        fun generateRandomInt(): Int {
            return (0..300).random()
        }

        fun <T> transformStringToNumber(value: T): Int {
            if (value is Int) {
                return value
            }
            return wordToNumber(value as String)
        }

        private fun wordToNumber(value: String): Int {
            return when (value) {
                "um", "1" -> 1
                "dois", "2" -> 2
                "tres", "3" -> 3
                "quatro", "4" -> 4
                "cinco", "5" -> 5
                "seis", "6" -> 6
                "sete", "7" -> 7
                "oito", "8" -> 8
                "nove", "9" -> 9
                else -> 0
            }
        }

        fun removeAccents(value: String): String {
            val regexToRemoveAccents = "\\p{M}".toRegex()
            return normalize(value).replace(regexToRemoveAccents, "")
        }

        private fun normalize(input: String): String {
            return Normalizer.normalize(input, Normalizer.Form.NFKD)
        }

    }
}