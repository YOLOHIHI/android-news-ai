package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.adapters.NewsAdapter
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.User
import com.example.myapplication1.databinding.ActivityMyNewsBinding

/**
 * 我的新闻页面
 * 显示当前用户发布的所有新闻（不包括草稿）
 * 支持点击查看新闻详情
 */
class MyNewsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMyNewsBinding
    private lateinit var dataManager: DataManager
    private lateinit var newsAdapter: NewsAdapter
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        currentUser = dataManager.getCurrentUser()
        
        if (currentUser == null) {
            finish()
            return
        }
        
        setupViews()
        setupClickListeners()
        loadMyNews()
    }
    
    /**
     * 设置RecyclerView和适配器
     */
    private fun setupViews() {
        binding.rvMyNews.layoutManager = LinearLayoutManager(this)
        newsAdapter = NewsAdapter(
            onNewsClick = { news ->
            val intent = Intent(this, NewsDetailActivity::class.java)
            intent.putExtra("news_id", news.id)
            startActivity(intent)
            },
            showReviewStatus = true  // 在我的新闻中显示审核状态标签
        )
        binding.rvMyNews.adapter = newsAdapter
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    /**
     * 加载用户发布的新闻
     * 过滤掉草稿，只显示已发布的新闻
     */
    private fun loadMyNews() {
        currentUser?.let { user ->
            val myNews = dataManager.getAllNews().filter { 
                it.author == user.username && !it.isDraft 
            }.sortedByDescending { it.timestamp }
            
            newsAdapter.updateNews(myNews)
            
            // 更新标题显示新闻数量
            binding.tvTitle.text = getString(R.string.title_my_news_with_count, myNews.size)
            
            // 显示空状态
            if (myNews.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.rvMyNews.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.rvMyNews.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadMyNews()  // 返回时刷新数据
    }
} 