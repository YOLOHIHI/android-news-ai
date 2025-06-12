package com.example.myapplication1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.R

/**
 * 标签适配器
 * 用于显示新闻标签，支持选择和取消选择
 */
class TagAdapter(private val onTagClick: (String?) -> Unit) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {
    
    private var tags = listOf<String>()
    private var selectedTag: String? = null  // 当前选中的标签

    /**
     * 更新标签列表
     */
    fun updateTags(newTags: List<String>) {
        tags = newTags
        notifyDataSetChanged()
    }
    
    /**
     * 设置选中的标签
     */
    fun setSelectedTag(tag: String?) {
        selectedTag = tag
        notifyDataSetChanged()
    }
    
    /**
     * 清除标签选择
     */
    fun clearSelection() {
        selectedTag = null
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }
    
    override fun getItemCount(): Int = tags.size
    
    /**
     * 处理标签的显示和选择状态
     */
    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTag: TextView = itemView.findViewById(R.id.tv_tag)
        
        fun bind(tag: String) {
            tvTag.text = tag
            
            // 根据选择状态更新外观
            if (tag == selectedTag) {
                tvTag.setBackgroundResource(R.drawable.tag_selected_background)
                tvTag.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                tvTag.setBackgroundResource(R.drawable.tag_background)
                tvTag.setTextColor(itemView.context.getColor(R.color.tag_text_color))
            }
            
            itemView.setOnClickListener {
                // 切换功能：如果已选中则取消选择
                if (tag == selectedTag) {
                    onTagClick(null) // 传递null表示取消选择
                } else {
                    onTagClick(tag) // 传递标签进行选择
                }
            }
        }
    }
} 