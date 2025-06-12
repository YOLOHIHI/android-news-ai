package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.adapters.PendingNewsAdapter
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.News
import com.example.myapplication1.data.User
import com.example.myapplication1.databinding.ActivityReviewManagementBinding

/**
 * 审核管理Activity
 * 管理员专用界面，用于查看和管理所有待审核的新闻
 */
class ReviewManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReviewManagementBinding
    private lateinit var dataManager: DataManager
    private lateinit var pendingNewsAdapter: PendingNewsAdapter
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        currentUser = dataManager.getCurrentUser()
        
        // 检查管理员权限
        if (currentUser == null || !dataManager.isCurrentUserAdmin()) {
            finish()
            return
        }
        
        setupViews()
        setupClickListeners()
        loadPendingNews()
    }
    
    /**
     * 设置RecyclerView和适配器
     */
    private fun setupViews() {
        binding.rvPendingNews.layoutManager = LinearLayoutManager(this)
        pendingNewsAdapter = PendingNewsAdapter(emptyList()) { news ->
            // 点击新闻项跳转到详细审核页面
            val intent = Intent(this, ReviewDetailActivity::class.java)
            intent.putExtra("news_id", news.id)
            startActivity(intent)
        }
        binding.rvPendingNews.adapter = pendingNewsAdapter
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadPendingNews()
        }
    }
    
    /**
     * 加载待审核新闻列表
     */
    private fun loadPendingNews() {
        val pendingNews = dataManager.getPendingNewsForReview()
        
        pendingNewsAdapter.updateNews(pendingNews)
        
        // 更新标题显示待审核数量
        binding.tvTitle.text = "待审核新闻 (${pendingNews.size})"
        
        // 显示空状态
        if (pendingNews.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvPendingNews.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvPendingNews.visibility = View.VISIBLE
        }
        
        // 更新统计信息
        updateStatistics(pendingNews)
    }
    
    /**
     * 更新统计信息显示
     */
    private fun updateStatistics(pendingNews: List<News>) {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000 // 24小时前
        val oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000 // 7天前
        
        val todayCount = pendingNews.count { news ->
            news.reviewInfo?.submittedAt?.let { it > oneDayAgo } ?: false
        }
        
        val weekCount = pendingNews.count { news ->
            news.reviewInfo?.submittedAt?.let { it > oneWeekAgo } ?: false
        }
        
        binding.tvTodayCount.text = "今日提交：$todayCount 条"
        binding.tvWeekCount.text = "本周提交：$weekCount 条"
        binding.tvTotalCount.text = "总计待审：${pendingNews.size} 条"
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回时刷新数据，确保审核状态及时更新
        loadPendingNews()
    }
} 