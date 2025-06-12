package com.example.myapplication1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.R
import com.example.myapplication1.data.News
import com.example.myapplication1.data.ReviewStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * 待审核新闻列表适配器
 * 用于显示管理员待审核的新闻列表
 */
class PendingNewsAdapter(
    private var newsList: List<News>,
    private val onNewsClickListener: (News) -> Unit
) : RecyclerView.Adapter<PendingNewsAdapter.PendingNewsViewHolder>() {

    /**
     * 日期格式化器，用于显示提交时间
     */
    private val dateFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingNewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_news, parent, false)
        return PendingNewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: PendingNewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }

    override fun getItemCount(): Int = newsList.size

    /**
     * 更新新闻列表数据
     */
    fun updateNews(newNewsList: List<News>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }

    /**
     * ViewHolder类，负责绑定单个新闻项的数据
     */
    inner class PendingNewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvContentPreview: TextView = itemView.findViewById(R.id.tv_content_preview)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvSubmitTime: TextView = itemView.findViewById(R.id.tv_submit_time)
        private val tvTags: TextView = itemView.findViewById(R.id.tv_tags)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

        fun bind(news: News) {
            // 设置标题
            tvTitle.text = news.title

            // 设置内容预览（截取前100个字符）
            val contentPreview = if (news.content.length > 100) {
                news.content.substring(0, 100) + "..."
            } else {
                news.content
            }
            tvContentPreview.text = contentPreview

            // 设置作者信息
            tvAuthor.text = "作者：${news.author}"

            // 设置提交时间
            val submitTime = news.reviewInfo?.submittedAt ?: news.timestamp
            tvSubmitTime.text = dateFormatter.format(Date(submitTime))

            // 设置标签（如果有）
            if (news.tags.isNotEmpty()) {
                tvTags.isVisible = true
                tvTags.text = "标签：${news.tags.joinToString(", ")}"
            } else {
                tvTags.isVisible = false
            }

            // 设置审核状态
            when (news.reviewStatus) {
                ReviewStatus.PENDING -> {
                    tvStatus.text = "待审核"
                    tvStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                }
                ReviewStatus.APPROVED -> {
                    tvStatus.text = "已通过"
                    tvStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_dark))
                }
                ReviewStatus.REJECTED -> {
                    tvStatus.text = "未通过"
                    tvStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_red_dark))
                }
                else -> {
                    tvStatus.text = "草稿"
                    tvStatus.setBackgroundColor(itemView.context.getColor(android.R.color.darker_gray))
                }
            }

            // 设置点击监听器
            itemView.setOnClickListener {
                onNewsClickListener(news)
            }
        }
    }
} 