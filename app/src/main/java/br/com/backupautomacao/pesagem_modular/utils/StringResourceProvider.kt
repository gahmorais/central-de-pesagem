package br.com.backupautomacao.pesagem_modular.utils

import android.content.Context
import androidx.annotation.StringRes

class StringResourceProvider(private val applicationContext: Context) {
  fun getString(@StringRes stringRes: Int): String = applicationContext.getString(stringRes)
  fun getString(@StringRes stringRes: Int, vararg item: Any): String = applicationContext.getString(stringRes, item)
}