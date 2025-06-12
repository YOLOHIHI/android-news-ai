package com.example.myapplication1.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.data.News
import com.example.myapplication1.databinding.ItemDraftBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 草稿列表适配器
 * 用于显示用户保存的草稿，提供编辑和删除功能
 * 使用ViewBinding简化视图操作
 */
class DraftAdapter(
    private val onEditClick: (News) -> Unit,    // 编辑回调
    private val onDeleteClick: (News) -> Unit   // 删除回调
) : RecyclerView.Adapter<DraftAdapter.DraftViewHolder>() {

    private var drafts = listOf<News>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 更新草稿列表数据
     */
    fun updateDrafts(newDrafts: List<News>) {
        drafts = newDrafts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val binding = ItemDraftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DraftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        holder.bind(drafts[position])
    }

    override fun getItemCount(): Int = drafts.size

    /**
     * 草稿项ViewHolder
     * 处理草稿的显示和操作按钮
     */
    inner class DraftViewHolder(private val binding: ItemDraftBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(draft: News) {
            // 处理空标题的情况
            binding.tvTitle.text = if (draft.title.isBlank()) "无标题草稿" else draft.title
            binding.tvDate.text = dateFormat.format(Date(draft.timestamp))
            binding.tvTags.text = if (draft.tags.isNotEmpty()) "标签: ${draft.tags.joinToString(", ")}" else "无标签"
            
            // 显示内容预览，限制长度避免界面混乱
            val contentPreview = if (draft.content.length > 100) {
                draft.content.substring(0, 100) + "..."
            } else {
                draft.content
            }
            binding.tvContent.text = if (contentPreview.isBlank()) "暂无内容..." else contentPreview
            
            // 设置操作按钮
            binding.btnEdit.setOnClickListener {
                onEditClick(draft)
            }
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(draft)
            }
        }
    }
} 