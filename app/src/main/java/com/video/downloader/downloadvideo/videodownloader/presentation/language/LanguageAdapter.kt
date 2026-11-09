package com.video.downloader.downloadvideo.videodownloader.presentation.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.databinding.ItemLanguageBinding
import com.video.downloader.downloadvideo.videodownloader.domain.model.LanguageModel

class LanguageAdapter(
    private val languages: List<LanguageModel>,
    private var selectedIndex: Int,
    private val onSelect: (LanguageModel, Int) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = languages.size

    fun updateSelectedIndex(index: Int) {
        val prev = selectedIndex
        selectedIndex = index
        notifyItemChanged(prev)
        notifyItemChanged(selectedIndex)
    }

    fun getSelectedLanguage(): LanguageModel = languages[selectedIndex]

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageModel, isSelected: Boolean) {
            binding.tvLangTitle.text = item.title
            binding.ivLangFlag.setImageResource(item.flagDrawableRes)

            if (isSelected) {
                binding.layoutLangItem.setBackgroundResource(R.drawable.bg_language_item_selected)
                binding.tvLangTitle.setTextColor(Color.WHITE)
                binding.ivLangRadio.setImageResource(R.drawable.rated)
            } else {
                binding.layoutLangItem.setBackgroundResource(R.drawable.bg_card_social)
                binding.tvLangTitle.setTextColor(Color.WHITE)
                binding.ivLangRadio.setImageResource(R.drawable.unrate)
            }

            binding.root.setOnClickListener {
                onSelect(item, adapterPosition)
            }
        }
    }
}
