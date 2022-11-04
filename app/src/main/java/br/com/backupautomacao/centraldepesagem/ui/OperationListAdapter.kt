package br.com.backupautomacao.centraldepesagem.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.backupautomacao.centraldepesagem.databinding.OperationListItemBinding
import br.com.backupautomacao.centraldepesagem.db.model.Operation
import br.com.backupautomacao.centraldepesagem.utils.Formatter

class OperationListAdapter :
  ListAdapter<Operation, OperationListAdapter.OperationListViewHolder>(OperationComparator()) {
  private lateinit var binding: OperationListItemBinding

  class OperationListViewHolder(private val binding: OperationListItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(operation: Operation) {
      binding.apply {
        tvDescription.text = operation.description
        tvDate.text = Formatter.formatDate(operation.date)
        tvUsername.text = operation.username
        tvWeight.text = Formatter.formatWeight(operation.weight)
        tvTotalPrice.text = Formatter.formatPrice(operation.totalPrice)
        tvPricePerKg.text = Formatter.formatPrice(operation.pricePerKilogram)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationListViewHolder {
    binding = OperationListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return OperationListViewHolder(binding)
  }

  override fun onBindViewHolder(holder: OperationListViewHolder, position: Int) {
    holder.bind(getItem(position))
  }
}

class OperationComparator : DiffUtil.ItemCallback<Operation>() {
  override fun areItemsTheSame(oldItem: Operation, newItem: Operation): Boolean {
    return oldItem == newItem
  }

  override fun areContentsTheSame(oldItem: Operation, newItem: Operation): Boolean {
    return oldItem.id == newItem.id
  }

}