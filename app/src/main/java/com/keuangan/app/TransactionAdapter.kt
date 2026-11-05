package com.keuangan.app

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat

class TransactionAdapter(
    private val currencyFormat: NumberFormat,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var transactions = listOf<Transaction>()

    fun setData(items: List<Transaction>) {
        transactions = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = transactions[position]
        holder.bind(item)
    }

    override fun getItemCount() = transactions.size

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvItemTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvItemDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvItemAmount)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivTypeIcon)

        fun bind(item: Transaction) {
            tvTitle.text = item.title
            tvDate.text = "${item.category} · ${item.formattedDate}"
            tvAmount.text = if (item.isIncome) "+${currencyFormat.format(item.amount)}" else "-${currencyFormat.format(item.amount)}"
            tvAmount.setTextColor(
                itemView.context.getColor(if (item.isIncome) R.color.income_green else R.color.expense_red)
            )
            ivIcon.setImageResource(getCategoryIcon(item.category))
            itemView.setOnClickListener { onItemClick(item) }
        }

        private fun getCategoryIcon(category: String): Int = when (category) {
            "Makanan" -> R.drawable.ic_restaurant
            "Transport" -> R.drawable.ic_directions_car
            "Tagihan" -> R.drawable.ic_receipt
            "Belanja" -> R.drawable.ic_shopping_bag
            "Hiburan" -> R.drawable.ic_entertainment
            "Kesehatan" -> R.drawable.ic_health
            "Gaji" -> R.drawable.ic_wallet
            else -> R.drawable.ic_add
        }
    }
}
