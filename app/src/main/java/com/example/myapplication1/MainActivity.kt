package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.adapters.NewsAdapter
import com.example.myapplication1.adapters.TagAdapter
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.News

import com.example.myapplication1.databinding.ActivityMainBinding
import androidx.recyclerview.widget.GridLayoutManager

/**
 * 主界面Activity
 * 展示新闻列表和标签筛选功能
 * 主要功能包括：新闻列表展示、标签筛选、底部导航栏
 */
class MainActivity : AppCompatActivity() {
    
    // 视图绑定对象，用于访问布局中的控件
    private lateinit var binding: ActivityMainBinding
    // 数据管理器，负责所有数据操作
    private lateinit var dataManager: DataManager
    // 新闻列表适配器，处理新闻数据的显示
    private lateinit var newsAdapter: NewsAdapter
    // 标签适配器，处理标签的显示和选择
    private lateinit var tagAdapter: TagAdapter
    
    // 当前选中的标签，用于筛选新闻
    private var currentSelectedTag: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化数据管理器
        dataManager = DataManager(this)
        
        // 生成示例数据（如果需要的话）
        // 这个设计确保了应用在首次启动时有数据可以展示
        val sampleDataGenerator = com.example.myapplication1.data.SampleDataGenerator(this)
        sampleDataGenerator.generateSampleData()
        
        // 检查用户登录状态
        // 如果用户未登录，跳转到登录界面
        val currentUser = dataManager.getCurrentUser()
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        // 设置视图绑定
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化界面组件
        setupViews()
        setupClickListeners()
        loadData()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次回到主界面时重新加载数据，确保数据是最新的
        loadData()
    }
    
    /**
     * 设置RecyclerView和适配器
     * 这里配置了两个RecyclerView：一个用于标签，一个用于新闻列表
     */
    private fun setupViews() {
        // 设置标签RecyclerView，使用网格布局，每行3个标签
        // 这样的设计让标签显示更加紧凑，节省屏幕空间
        binding.rvTags.layoutManager = GridLayoutManager(this, 3)
        tagAdapter = TagAdapter { tag ->
            if (tag != null) {
                onTagClicked(tag)                               // 标签点击回调
            } else {
                clearFilter()                                   // null表示清除筛选
            }
        }
        binding.rvTags.adapter = tagAdapter
        
        // 设置新闻RecyclerView，使用线性布局，垂直排列
        // 这是新闻列表的标准显示方式
        binding.rvNews.layoutManager = LinearLayoutManager(this)
        newsAdapter = NewsAdapter(
            onNewsClick = { news ->
            // 点击新闻项时跳转到详情页
            val intent = Intent(this, NewsDetailActivity::class.java)
            intent.putExtra("news_id", news.id)
            startActivity(intent)
            },
            showReviewStatus = false  // 主页新闻列表不显示审核状态标签
        )
        binding.rvNews.adapter = newsAdapter
    }
    
    /**
     * 设置各种按钮的点击监听器
     * 包括底部导航栏和功能按钮
     */
    private fun setupClickListeners() {
        // 首页按钮 - 刷新当前页面
        binding.btnHome.setOnClickListener {
            // 已经在首页，执行刷新操作
            loadData()
        }
        
        // 发布按钮 - 跳转到新闻发布页面
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, PostNewsActivity::class.java))
        }
        
        // 个人资料按钮 - 跳转到用户个人页面
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        
        // 筛选按钮 - 显示/隐藏标签选择区域
        binding.btnFilter.setOnClickListener {
            toggleTagsVisibility()
        }
        
        // 清除筛选按钮 - 取消当前的标签筛选
        binding.btnClearFilter.setOnClickListener {
            clearFilter()
        }
        
        // 管理员审核按钮（仅管理员可见）
        binding.btnReviewManagement?.setOnClickListener {
            startActivity(Intent(this, ReviewManagementActivity::class.java))
        }
    }
    
    /**
     * 加载所有数据
     * 包括新闻列表和标签列表
     */
    private fun loadData() {
        // 获取已审核通过的新闻用于主页显示
        val allNews = dataManager.getPublicNews()
            .sortedByDescending { it.timestamp }
        
        // 更新新闻列表
        updateNewsList(allNews)
        
        // 加载标签数据
        val allTags = dataManager.getAllTags()
        tagAdapter.updateTags(allTags)
        
        // 根据当前筛选条件更新显示
        currentSelectedTag?.let { tag ->
            onTagClicked(tag)
        } ?: run {
            // 清除筛选状态
            updateFilterStatus(null)
        }
        
        // 显示管理员功能（如果当前用户是管理员）
        updateAdminUI()
    }
    
    /**
     * 加载标签数据
     * 从所有新闻中提取标签，用于筛选功能
     */
    private fun loadTags() {
        val tags = dataManager.getAllTags()
        tagAdapter.updateTags(tags)
        
        // 根据标签数量决定是否显示筛选功能
        if (tags.isEmpty()) {
            binding.rvTags.visibility = View.GONE
            binding.btnFilter.visibility = View.GONE
        } else {
            binding.btnFilter.visibility = View.VISIBLE
        }
    }
    
    /**
     * 加载新闻数据
     * 根据当前选中的标签进行筛选，并按时间倒序排列
     */
    private fun loadNews() {
        val newsList = if (currentSelectedTag != null) {
            // 如果有选中的标签，按标签筛选
            dataManager.getNewsByTag(currentSelectedTag!!)
        } else {
            // 否则获取所有新闻
            dataManager.getAllNews()
        }.filter { !it.isDraft }                               // 过滤掉草稿
            .sortedByDescending { it.timestamp }                // 按时间倒序排列
        
        newsAdapter.updateNews(newsList)
        
        // 更新UI状态，显示筛选信息
        if (currentSelectedTag != null) {
            binding.tvFilterStatus.text = getString(R.string.filter_status, currentSelectedTag!!)
            binding.tvFilterStatus.visibility = View.VISIBLE
            binding.btnClearFilter.visibility = View.VISIBLE
        } else {
            binding.tvFilterStatus.visibility = View.GONE
            binding.btnClearFilter.visibility = View.GONE
        }
        
        // 显示空状态提示
        // 这个设计提高了用户体验，让用户知道当前的状态
        if (newsList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = if (currentSelectedTag != null) {
                getString(R.string.no_news_with_tag)
            } else {
                getString(R.string.no_news_default)
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }
    
    /**
     * 处理标签点击事件
     * 根据标签筛选新闻并更新界面
     */
    private fun onTagClicked(tag: String) {
        if (currentSelectedTag == tag) {
            // 如果点击的是当前已选中的标签，则取消筛选
            clearFilter()
        } else {
            // 选择新的标签进行筛选
            currentSelectedTag = tag
            val filteredNews = dataManager.getPublicNews().filter { news ->
                news.tags.any { it.equals(tag, ignoreCase = true) }
            }.sortedByDescending { it.timestamp }
            
            updateNewsList(filteredNews)
            updateFilterStatus(tag)
            tagAdapter.setSelectedTag(tag)
            
            // 隐藏标签选择区域
            binding.rvTags.isVisible = false
        }
    }
    
    /**
     * 切换标签区域的显示/隐藏状态
     * 使用isVisible扩展属性简化代码
     */
    private fun toggleTagsVisibility() {
        binding.rvTags.isVisible = !binding.rvTags.isVisible
    }
    
    /**
     * 清除筛选条件
     * 显示所有新闻
     */
    private fun clearFilter() {
        currentSelectedTag = null
        val allNews = dataManager.getPublicNews()
            .sortedByDescending { it.timestamp }
        
        updateNewsList(allNews)
        updateFilterStatus(null)
        tagAdapter.setSelectedTag(null)
    }
    
    /**
     * 更新新闻列表
     */
    private fun updateNewsList(newsList: List<News>) {
        newsAdapter.updateNews(newsList)
    }
    
    /**
     * 更新筛选状态
     */
    private fun updateFilterStatus(tag: String?) {
        if (tag != null) {
            binding.tvFilterStatus.text = getString(R.string.filter_status, tag)
            binding.tvFilterStatus.visibility = View.VISIBLE
            binding.btnClearFilter.visibility = View.VISIBLE
        } else {
            binding.tvFilterStatus.visibility = View.GONE
            binding.btnClearFilter.visibility = View.GONE
        }
    }
    
    /**
     * 更新管理员功能UI
     */
    private fun updateAdminUI() {
        if (dataManager.isCurrentUserAdmin()) {
            // 当前用户是管理员，显示审核管理功能
            binding.btnReviewManagement?.visibility = View.VISIBLE
            
            // 获取待审核数量并显示
            val pendingCount = dataManager.getPendingNewsForReview().size
            if (pendingCount > 0) {
                binding.btnReviewManagement?.text = "审核管理 ($pendingCount)"
            } else {
                binding.btnReviewManagement?.text = "审核管理"
            }
        } else {
            // 普通用户，隐藏审核管理功能
            binding.btnReviewManagement?.visibility = View.GONE
        }
    }
}
