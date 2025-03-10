package br.com.backupautomacao.centraldepesagem.ui

import android.widget.TextView
import androidx.databinding.BindingAdapter
import br.com.backupautomacao.centraldepesagem.utils.Formatter

@Suppress("unused")
@BindingAdapter("productPrice")
fun bindProductPrice(textView: TextView, price: Double) {
    if (price > 0.0) {
        textView.text = Formatter.formatPrice(price)
    } else {
        textView.text = ""
    }
}

@Suppress("unused")
@BindingAdapter("productWeight")
fun bindProductWeight(textView: TextView, value: Double) {
    if (value > 0.0) {
        textView.text = Formatter.formatWeight(value)
    } else {
        textView.text = ""
    }
}