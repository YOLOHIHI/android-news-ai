package com.example.myapplication1

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.News
import com.example.myapplication1.data.ReviewStatus
import com.example.myapplication1.data.User
import com.example.myapplication1.databinding.ActivityReviewDetailBinding

/**
 * 新闻详细审核Activity
 * 管理员专用界面，提供AI审核建议和人工审核操作
 */
class ReviewDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReviewDetailBinding
    private lateinit var dataManager: DataManager
    private lateinit var aiReviewService: AIReviewService
    private var currentNews: News? = null
    private var currentUser: User? = null
    
    // AI审核状态枚举
    private enum class AIReviewState { IDLE, LOADING, SUCCESS, ERROR }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        aiReviewService = AIReviewService.getInstance()
        currentUser = dataManager.getCurrentUser()
        
        // 检查管理员权限
        if (currentUser == null || !dataManager.isCurrentUserAdmin()) {
            finish()
            return
        }
        
        // 获取新闻ID
        val newsId = intent.getStringExtra("news_id")
        if (newsId == null) {
            Toast.makeText(this, "新闻不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 获取新闻详情
        currentNews = dataManager.getNewsById(newsId)
        if (currentNews == null) {
            Toast.makeText(this, "新闻不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 检查新闻是否为待审核状态
        if (currentNews!!.reviewStatus != ReviewStatus.PENDING) {
            Toast.makeText(this, "该新闻已完成审核", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupClickListeners()
        loadNewsDetail()
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnAiReview.setOnClickListener {
            performAIReview()
        }
        
        binding.btnApprove.setOnClickListener {
            showApproveDialog()
        }
        
        binding.btnReject.setOnClickListener {
            showRejectDialog()
        }
    }
    
    /**
     * 加载新闻详情信息
     */
    private fun loadNewsDetail() {
        currentNews?.let { news ->
            binding.tvNewsTitle.text = news.title
            binding.tvNewsContent.text = news.content
            binding.tvNewsAuthor.text = "作者: ${news.author}"
            binding.tvNewsTime.text = "提交时间: ${news.reviewInfo?.getFormattedSubmittedTime() ?: "未知"}"
            binding.tvNewsTags.text = if (news.tags.isNotEmpty()) {
                "标签: ${news.tags.joinToString(", ")}"
            } else {
                "无标签"
            }
            
            // 显示已有的AI审核结果（如果存在）
            news.reviewInfo?.let { reviewInfo ->
                if (reviewInfo.aiDecision != null && reviewInfo.aiSuggestion != null) {
                    updateAIReviewUI(AIReviewState.SUCCESS, reviewInfo.aiDecision!!, reviewInfo.aiSuggestion!!)
                }
            }
        }
    }
    
    /**
     * 执行AI审核
     */
    private fun performAIReview() {
        currentNews?.let { news ->
            // 检查网络连接
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "网络连接失败，无法进行AI审核", Toast.LENGTH_SHORT).show()
                return
            }
            
            updateAIReviewUI(AIReviewState.LOADING)
            
            aiReviewService.reviewNews(
                newsId = news.id,
                newsTitle = news.title,
                newsContent = news.content
            ) { result ->
                runOnUiThread {
                    if (result != null) {
                        // AI审核成功
                        updateAIReviewUI(AIReviewState.SUCCESS, result.decision, result.reason)
                        
                        // 保存AI审核结果到数据库
                        dataManager.updateNewsAIReview(
                            newsId = news.id,
                            aiDecision = result.decision,
                            aiSuggestion = result.reason
                        )
                        
                        // 刷新当前新闻对象
                        currentNews = dataManager.getNewsById(news.id)
                    } else {
                        // AI审核失败
                        updateAIReviewUI(AIReviewState.ERROR)
                    }
                }
            }
        }
    }
    
    /**
     * 更新AI审核UI状态
     */
    private fun updateAIReviewUI(state: AIReviewState, decision: String = "", reason: String = "") {
        when (state) {
            AIReviewState.IDLE -> {
                binding.btnAiReview.text = "获取AI审核建议"
                binding.btnAiReview.isEnabled = true
                binding.layoutAiLoading.visibility = View.GONE
                binding.layoutAiContent.visibility = View.GONE
            }
            
            AIReviewState.LOADING -> {
                binding.btnAiReview.text = "AI审核中..."
                binding.btnAiReview.isEnabled = false
                binding.layoutAiLoading.visibility = View.VISIBLE
                binding.layoutAiContent.visibility = View.GONE
            }
            
            AIReviewState.SUCCESS -> {
                binding.btnAiReview.text = "重新获取AI建议"
                binding.btnAiReview.isEnabled = true
                binding.layoutAiLoading.visibility = View.GONE
                binding.layoutAiContent.visibility = View.VISIBLE
                
                // 设置决策颜色
                val textColor = when {
                    decision.contains("✔️通过") -> android.graphics.Color.parseColor("#4CAF50")
                    decision.contains("❌不通过") -> android.graphics.Color.parseColor("#F44336")
                    else -> android.graphics.Color.parseColor("#FF9800")
                }
                
                binding.tvAiDecision.text = decision
                binding.tvAiDecision.setTextColor(textColor)
                binding.tvAiSuggestion.text = reason
            }
            
            AIReviewState.ERROR -> {
                binding.btnAiReview.text = "获取AI审核建议"
                binding.btnAiReview.isEnabled = true
                binding.layoutAiLoading.visibility = View.GONE
                binding.layoutAiContent.visibility = View.GONE
                Toast.makeText(this, "AI审核失败，请检查网络连接后重试", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示批准发布对话框
     */
    private fun showApproveDialog() {
        AlertDialog.Builder(this)
            .setTitle("批准发布")
            .setMessage("确认批准这条新闻发布吗？")
            .setPositiveButton("批准发布") { _, _ ->
                approveNews()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示拒绝发布对话框
     */
    private fun showRejectDialog() {
        AlertDialog.Builder(this)
            .setTitle("拒绝发布")
            .setMessage("确认拒绝这条新闻发布吗？")
            .setPositiveButton("拒绝发布") { _, _ ->
                rejectNews()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 批准新闻发布
     */
    private fun approveNews(): Boolean {
        currentNews?.let { news ->
            if (dataManager.approveNews(news.id, "")) {
                Toast.makeText(this, "新闻已批准发布", Toast.LENGTH_SHORT).show()
                finish()
                return true
            }
        }
        Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show()
        return false
    }
    
    /**
     * 拒绝新闻发布
     */
    private fun rejectNews(): Boolean {
        currentNews?.let { news ->
            if (dataManager.rejectNews(news.id, "")) {
                Toast.makeText(this, "新闻已拒绝发布", Toast.LENGTH_SHORT).show()
                finish()
                return true
            }
        }
        Toast.makeText(this, "操作失败", Toast.LENGTH_SHORT).show()
        return false
    }
} 