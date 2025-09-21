package br.com.backupautomacao.pesagem_modular.ui.zpl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.backupautomacao.pesagem_modular.databinding.ItemZplLabelBinding
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ZplLabelAdapter(
    private val labels: List<ZplLabel>,
    private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<ZplLabelAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemZplLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LabelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        holder.bind(labels[position])
    }

    override fun getItemCount(): Int = labels.size

    inner class LabelViewHolder(private val binding: ItemZplLabelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(label: ZplLabel) {
            binding.label = label
            binding.executePendingBindings()
            binding.ivLabelPreview.setImageBitmap(null)

            coroutineScope.launch(Dispatchers.IO) {
                val bitmap = renderZplToImage(label.zplCode)
                withContext(Dispatchers.Main) {
                    binding.ivLabelPreview.setImageBitmap(bitmap)
                }
            }
        }

        private fun renderZplToImage(zplCode: String): Bitmap? {
            val client = OkHttpClient()
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val requestBody = zplCode.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("http://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/")
                .addHeader("Accept", "image/png")
                .post(requestBody)
                .build()

            return try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            }
        }
    }
}
