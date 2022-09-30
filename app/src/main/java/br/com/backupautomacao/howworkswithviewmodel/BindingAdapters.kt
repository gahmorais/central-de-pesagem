package br.com.backupautomacao.howworkswithviewmodel

import android.util.Log
import android.widget.TextView
import androidx.databinding.BindingAdapter
import br.com.backupautomacao.howworkswithviewmodel.utils.Formatter
import org.w3c.dom.Text

@BindingAdapter("productPrice")
fun bindProductPrice(textView: TextView, price: Double) {
  Log.i("TAG", "bindProductPrice: $price")
  if (price > 0.0) {
    textView.text = Formatter.formatPrice(price)
  } else {
    textView.text = ""
  }
}

@BindingAdapter("productWeight")
fun bindProductWeight(textView: TextView, weight: Double) {
  if (weight > 0.0) {
    textView.text = Formatter.formatWeight(weight)
  } else {
    textView.text = ""
  }
}