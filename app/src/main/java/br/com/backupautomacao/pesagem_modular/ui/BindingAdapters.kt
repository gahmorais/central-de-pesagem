package br.com.backupautomacao.pesagem_modular.ui

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import br.com.backupautomacao.pesagem_modular.utils.Formatter

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
@BindingAdapter("productTare")
fun bindProducTare(textView: TextView, tare: Double) {
  if (tare >= 0.0) {
    textView.text = Formatter.formatWeight(tare)
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

@Suppress("unused")
@BindingAdapter("android:text")
fun setIntInText(view: EditText, value: Int?) {
  val newValue = value?.toString() ?: ""
  if (view.text.toString() != newValue) {
    view.setText(newValue)
  }
}

@Suppress("unused")
@InverseBindingAdapter(attribute = "android:text")
fun getIntFromText(view: EditText): Int {
  return view.text.toString().toIntOrNull() ?: 0
}

@Suppress("unused")
@BindingAdapter("android:textAttrChanged")
fun setIntListener(view: EditText, listener: InverseBindingListener?) {
  view.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
      listener?.onChange()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
  })
}


/*
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
*/