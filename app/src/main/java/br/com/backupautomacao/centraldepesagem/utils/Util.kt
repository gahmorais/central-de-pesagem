package br.com.backupautomacao.centraldepesagem.utils

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
  }
}