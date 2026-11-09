package com.video.downloader.downloadvideo.videodownloader.presentation.question

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.databinding.ItemQuestionBinding

class QuestionAdapter(
    private val questions: List<String>,
    private val onSelectionChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private val selectedIndices = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position], position)
    }

    override fun getItemCount(): Int = questions.size

    fun hasSelection(): Boolean = selectedIndices.isNotEmpty()

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: String, position: Int) {
            binding.tvQuestionOption.text = question
            val isSelected = selectedIndices.contains(position)

            if (isSelected) {
                binding.tvQuestionOption.setBackgroundResource(R.drawable.bg_question_item_selected)
                binding.tvQuestionOption.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            } else {
                binding.tvQuestionOption.setBackgroundResource(R.drawable.bg_question_item_unselected)
                binding.tvQuestionOption.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            }

            binding.root.setOnClickListener {
                if (selectedIndices.contains(position)) {
                    selectedIndices.remove(position)
                } else {
                    selectedIndices.add(position)
                }
                notifyItemChanged(position)
                onSelectionChanged(selectedIndices.isNotEmpty())
            }
        }
    }
}
