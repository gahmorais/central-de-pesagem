package br.com.backupautomacao.pesagem_modular.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.Exception

class EventMask(private val editText: EditText, private var opt: Int) : TextWatcher {
    private var isUpdating: Boolean = false
    private var old: String = ""
    private var mask = ""

    companion object {
        const val MASCARA_IP: Int = 0
    }

    init {
        mask = typeMask(opt)
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        val str: String = unmask(charSequence.toString())
        var mascara = ""
        if (isUpdating) {
            old = str
            isUpdating = false
            return
        }

        var i = 0

        for (m in mask.toCharArray()) {
            if (m != '#' && str.length > old.length) {
                mascara += m
                continue
            }
            try {
                mascara += str[i]
            } catch (e: Exception) {
                break
            }
            i++
        }
        isUpdating = true
        editText.setText(mascara)
        editText.setSelection(mascara.length)
    }

    override fun afterTextChanged(p0: Editable?) {

    }

    private fun typeMask(type: Int): String {
        when (type) {
            0 -> {
                return "###.###.###.###"
            }
        }
        return ""
    }

    private fun unmask(s: String): String {
        return s.replace("[.]".toRegex(), "")
            .replace("[-]".toRegex(), "")
            .replace("[/]".toRegex(), "")
            .replace("[(]".toRegex(), "")
            .replace("[ ]".toRegex(), "")
            .replace("[:]".toRegex(), "")
            .replace("[)]".toRegex(), "")
    }
}