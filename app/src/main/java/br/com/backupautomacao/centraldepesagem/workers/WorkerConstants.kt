package br.com.backupautomacao.centraldepesagem.workers

import androidx.work.Data

const val WORK_RESULT = "result"
const val PRINT_TASK = "print_task"
const val PRINT_LABEL = "print_label"
const val PRINTER_TYPE = "printer_type"

fun createDataMessage(message: String): Data {
    return Data.Builder().putString(WORK_RESULT, message).build()
}
