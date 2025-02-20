package br.com.backupautomacao.centraldepesagem.utils

import android.content.Context
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes

class ResourceProvider(private val applicationContext: Context) {
    fun getString(@StringRes stringRes: Int): String {
        return applicationContext.getString(stringRes)
    }

    fun getInteger(@IntegerRes integerRes: Int): Int {
        return applicationContext.resources.getInteger(integerRes)
    }
}