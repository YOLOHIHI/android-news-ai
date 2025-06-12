package com.example.myapplication1

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.News
import com.example.myapplication1.data.ReviewStatus
import com.example.myapplication1.databinding.ActivityPostNewsBinding

/**
 * 新闻发布Activity
 * 支持新建新闻、编辑已发布新闻、编辑草稿等功能
 * 实现了草稿保存机制，用户可以随时保存未完成的内容
 */
class PostNewsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPostNewsBinding
    private lateinit var dataManager: DataManager
    private var editingNews: News? = null  // 当前编辑的新闻对象，null表示新建
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        
        // 检查是否为编辑模式
        val newsId = intent.getStringExtra("news_id")
        val draftId = intent.getStringExtra("draft_id")
        
        if (newsId != null) {
            editingNews = dataManager.getNewsById(newsId)
            editingNews?.let { loadNewsForEditing(it) }
        } else if (draftId != null) {
            editingNews = dataManager.getNewsById(draftId)
            editingNews?.let { loadDraftForEditing(it) }
        }
        
        setupClickListeners()
    }
    
    /**
     * 加载已发布新闻进行编辑
     */
    private fun loadNewsForEditing(news: News) {
        binding.etTitle.setText(news.title)
        binding.etContent.setText(news.content)
        binding.etTags.setText(news.tags.joinToString(", "))
        binding.btnPublish.text = getString(R.string.btn_update_news)
        binding.tvTitle.text = getString(R.string.title_edit_news)
    }
    
    /**
     * 加载草稿进行编辑
     */
    private fun loadDraftForEditing(draft: News) {
        binding.etTitle.setText(draft.title)
        binding.etContent.setText(draft.content)
        binding.etTags.setText(draft.tags.joinToString(", "))
        binding.btnPublish.text = getString(R.string.btn_publish)
        binding.tvTitle.text = getString(R.string.title_edit_draft)
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnSaveDraft.setOnClickListener {
            saveNews(isDraft = true)
        }
        
        binding.btnPublish.setOnClickListener {
            saveNews(isDraft = false)
        }
    }
    
    /**
     * 保存新闻或草稿
     * @param isDraft 是否保存为草稿
     */
    private fun saveNews(isDraft: Boolean) {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val tagsText = binding.etTags.text.toString().trim()
        
        // 基本验证
        if (title.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUser = dataManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 处理标签，支持逗号分隔
        val tags = if (tagsText.isNotEmpty()) {
            tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        
        val news = if (editingNews != null) {
            // 更新现有新闻
            editingNews!!.copy(
                title = title,
                content = content,
                tags = tags,
                isDraft = isDraft,
                // 保持原有的审核状态，除非是从草稿提交审核
                reviewStatus = if (!isDraft && editingNews!!.reviewStatus == ReviewStatus.DRAFT) {
                    ReviewStatus.DRAFT  // 草稿状态，等待后续提交审核
                } else {
                    editingNews!!.reviewStatus
                }
            )
        } else {
            // 创建新新闻
            News(
                title = title,
                content = content,
                author = currentUser.username,
                tags = tags,
                isDraft = isDraft,
                reviewStatus = if (isDraft) ReviewStatus.DRAFT else ReviewStatus.DRAFT  // 新创建的非草稿也先设为DRAFT
            )
        }
        
        dataManager.saveNews(news)
        
        // 如果不是草稿，则提交审核
        if (!isDraft) {
            val submitSuccess = dataManager.submitNewsForReview(news.id)
            if (submitSuccess) {
                Toast.makeText(this, "新闻已提交审核，请等待管理员审核", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "提交审核失败", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // 显示相应的成功消息
        val message = if (isDraft) {
            if (editingNews != null) "草稿已更新" else "草稿已保存"
        } else {
            if (editingNews != null) "新闻已重新提交审核" else "新闻已提交审核"
        }
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
} 