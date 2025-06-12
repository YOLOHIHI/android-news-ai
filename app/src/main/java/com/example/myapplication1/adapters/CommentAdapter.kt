package com.example.myapplication1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.R
import com.example.myapplication1.data.Comment

/**
 * 评论列表适配器
 * 用于显示新闻的评论列表，按时间倒序排列
 */
class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    
    private var comments = listOf<Comment>()
    
    /**
     * 更新评论列表数据
     * 自动按时间倒序排列
     */
    fun updateComments(newComments: List<Comment>) {
        comments = newComments.sortedByDescending { it.timestamp }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }
    
    override fun getItemCount(): Int = comments.size
    
    /**
     * 显示评论的作者、内容和时间
     */
    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        
        fun bind(comment: Comment) {
            tvAuthor.text = comment.author
            tvContent.text = comment.content
            tvDate.text = comment.getFormattedDate()
        }
    }
} 