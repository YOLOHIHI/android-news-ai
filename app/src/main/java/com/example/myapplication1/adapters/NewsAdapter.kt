package com.example.myapplication1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication1.R
import com.example.myapplication1.data.News
import com.example.myapplication1.data.ReviewStatus

/**
 * 新闻列表适配器
 * 用于在RecyclerView中显示新闻列表
 * @param showReviewStatus 是否显示审核状态标签，默认为false（主页不显示）
 */
class NewsAdapter(
    private val onNewsClick: (News) -> Unit,
    private val showReviewStatus: Boolean = false
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {
    
    private var newsList = listOf<News>()
    
    /**
     * 更新新闻列表数据
     */
    fun updateNews(news: List<News>) {
        newsList = news
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(newsList[position])
    }
    
    override fun getItemCount(): Int = newsList.size
    
    /**
     * 新闻项ViewHolder
     * 负责绑定新闻数据到视图组件
     */
    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvTags: TextView = itemView.findViewById(R.id.tv_tags)
        private val tvLikes: TextView = itemView.findViewById(R.id.tv_likes)
        private val tvComments: TextView = itemView.findViewById(R.id.tv_comments)
        private val tvReviewStatus: TextView = itemView.findViewById(R.id.tv_review_status)
        
        /**
         * 绑定新闻数据到视图
         * 处理内容截断、标签显示、审核状态显示等逻辑
         */
        fun bind(news: News) {
            tvTitle.text = news.title
            // 内容过长时进行截断显示
            tvContent.text = if (news.content.length > 100) {
                news.content.substring(0, 100) + "..."
            } else {
                news.content
            }
            tvAuthor.text = "作者: ${news.author}"
            tvDate.text = news.getFormattedDate()
            tvTags.text = if (news.tags.isNotEmpty()) {
                "标签: ${news.tags.joinToString(", ")}"
            } else {
                "无标签"
            }
            tvLikes.text = "${news.likes} 赞"
            tvComments.text = "${news.comments.size} 评论"
            
            // 设置审核状态标签
            setupReviewStatus(news)
            
            // 设置点击事件
            itemView.setOnClickListener {
                onNewsClick(news)
            }
        }
        
        /**
         * 设置审核状态标签的显示
         */
        private fun setupReviewStatus(news: News) {
            // 只在允许显示审核状态的界面中显示标签（如"我的新闻"）
            if (!showReviewStatus) {
                tvReviewStatus.visibility = View.GONE
                return
            }
            
            // 根据新闻的审核状态显示标签
            when (news.reviewStatus) {
                ReviewStatus.PENDING -> {
                    tvReviewStatus.visibility = View.VISIBLE
                    tvReviewStatus.text = "审核中"
                    tvReviewStatus.background = ContextCompat.getDrawable(itemView.context, R.drawable.review_status_background)
                }
                ReviewStatus.APPROVED -> {
                    tvReviewStatus.visibility = View.VISIBLE
                    tvReviewStatus.text = "已通过"
                    tvReviewStatus.background = ContextCompat.getDrawable(itemView.context, R.drawable.review_status_approved)
                }
                ReviewStatus.REJECTED -> {
                    tvReviewStatus.visibility = View.VISIBLE
                    tvReviewStatus.text = "未通过"
                    tvReviewStatus.background = ContextCompat.getDrawable(itemView.context, R.drawable.review_status_rejected)
                }
                ReviewStatus.DRAFT -> {
                    // 草稿状态在我的新闻中不应该显示
                    tvReviewStatus.visibility = View.GONE
                }
            }
        }
    }
} 