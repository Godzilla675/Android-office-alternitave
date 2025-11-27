package com.officesuite.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.databinding.ItemQuickActionBinding

class QuickActionsAdapter(
    private val actions: List<QuickAction>,
    private val onActionClick: (QuickAction) -> Unit
) : RecyclerView.Adapter<QuickActionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(actions[position])
    }

    override fun getItemCount(): Int = actions.size

    inner class ViewHolder(
        private val binding: ItemQuickActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(action: QuickAction) {
            binding.textTitle.text = action.title
            binding.imageIcon.setImageResource(action.iconRes)
            binding.root.setOnClickListener { onActionClick(action) }
        }
    }
}
