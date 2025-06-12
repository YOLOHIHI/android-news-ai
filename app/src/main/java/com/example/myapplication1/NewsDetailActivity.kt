package com.example.myapplication1

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.adapters.CommentAdapter
import com.example.myapplication1.data.Comment
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.News
import com.example.myapplication1.databinding.ActivityNewsDetailBinding

/**
 * 新闻详情页Activity
 * 展示新闻的完整内容和相关互动功能
 * 主要功能包括：新闻内容展示、点赞功能、评论系统、新闻删除
 */
class NewsDetailActivity : AppCompatActivity() {
    
    // 视图绑定对象，用于访问布局控件
    private lateinit var binding: ActivityNewsDetailBinding
    // 数据管理器，处理所有数据操作
    private lateinit var dataManager: DataManager
    // 评论列表适配器，管理评论的显示
    private lateinit var commentAdapter: CommentAdapter
    // 当前显示的新闻对象
    private var currentNews: News? = null
    // 当前登录的用户，用于权限判断
    private var currentUser: com.example.myapplication1.data.User? = null
    // AI服务实例，用于新闻总结功能
    private lateinit var aiService: AIService
    // AI总结状态枚举
    private enum class SummaryState { IDLE, LOADING, SUCCESS, ERROR }
    // 标记是否因网络问题暂停AI功能
    private var isWaitingForNetwork = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化数据管理器和当前用户
        dataManager = DataManager(this)
        currentUser = dataManager.getCurrentUser()
        // 初始化AI服务
        aiService = AIService.getInstance()
        
        // 从Intent中获取新闻ID
        val newsId = intent.getStringExtra("news_id")
        if (newsId == null) {
            Toast.makeText(this, "新闻不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 根据ID获取新闻详情
        currentNews = dataManager.getNewsById(newsId)
        if (currentNews == null) {
            Toast.makeText(this, "新闻不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 初始化界面组件
        setupViews()
        setupClickListeners()
        loadNewsDetail()
        loadComments()
    }
    
    /**
     * 设置RecyclerView和适配器
     * 主要配置评论列表的显示
     */
    private fun setupViews() {
        binding.rvComments.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter()
        binding.rvComments.adapter = commentAdapter
    }
    
    /**
     * 设置各种按钮的点击监听器
     * 包括返回、点赞、发送评论、删除新闻等功能
     */
    private fun setupClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // 点赞按钮
        binding.btnLike.setOnClickListener {
            likeNews()
        }
        
        // 发送评论按钮
        binding.btnSendComment.setOnClickListener {
            sendComment()
        }
        
        // 删除新闻按钮（仅作者可见）
        binding.btnDeleteNews.setOnClickListener {
            showDeleteConfirmDialog()
        }
        
        // AI总结按钮
        binding.btnAiSummary.setOnClickListener {
            handleAiSummaryClick()
        }
    }
    
    /**
     * 加载新闻详情信息
     * 显示新闻的标题、内容、作者、时间、标签等信息
     * 同时处理删除按钮的显示权限
     */
    private fun loadNewsDetail() {
        currentNews?.let { news ->
            binding.tvTitle.text = news.title
            binding.tvContent.text = news.content
            binding.tvAuthor.text = "作者: ${news.author}"
            binding.tvDate.text = news.getFormattedDate()
            binding.tvTags.text = if (news.tags.isNotEmpty()) {
                "标签: ${news.tags.joinToString(", ")}"
            } else {
                "无标签"
            }
            
            // 只有作者才能看到删除按钮
            // 这个权限控制很重要，防止用户删除别人的新闻
            if (currentUser != null && currentUser!!.username == news.author) {
                binding.btnDeleteNews.visibility = View.VISIBLE
            } else {
                binding.btnDeleteNews.visibility = View.GONE
            }
            
            updateLikeButton()
        }
    }
    
    /**
     * 更新点赞按钮的状态
     * 显示点赞数量，处理点赞限制，更新按钮文本和状态
     */
    private fun updateLikeButton() {
        currentNews?.let { news ->
            binding.tvLikeCount.text = "${news.likes} 赞"
            
            if (currentUser != null) {
                val userLikeCount = news.likedBy.count { it == currentUser!!.id }
                val canLike = news.canUserLike(currentUser!!.id)
                
                // 根据是否可以点赞来设置按钮状态
                binding.btnLike.isEnabled = canLike
                binding.btnLike.text = if (canLike) {
                    "👍 点赞"
                } else {
                    "👍 已达上限"
                }
                
                // 显示用户的点赞次数
                if (userLikeCount > 0) {
                    binding.tvUserLikes.text = "你已点赞 $userLikeCount 次"
                    binding.tvUserLikes.visibility = View.VISIBLE
                } else {
                    binding.tvUserLikes.visibility = View.GONE
                }
            } else {
                // 未登录用户不能点赞
                binding.btnLike.isEnabled = false
                binding.btnLike.text = "请先登录"
            }
        }
    }
    
    /**
     * 加载评论列表
     * 获取当前新闻的所有评论并显示
     * 同时更新评论数量统计
     */
    private fun loadComments() {
        currentNews?.let { news ->
            val comments = dataManager.getCommentsByNewsId(news.id)
            commentAdapter.updateComments(comments)
            
            binding.tvCommentCount.text = "${comments.size} 条评论"
            
            // 显示空状态提示
            if (comments.isEmpty()) {
                binding.tvNoComments.visibility = View.VISIBLE
            } else {
                binding.tvNoComments.visibility = View.GONE
            }
        }
    }
    
    /**
     * 执行点赞操作
     * 检查用户登录状态和点赞限制，更新点赞数据
     */
    private fun likeNews() {
        if (currentUser == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentNews?.let { news ->
            if (news.addLike(currentUser!!.id)) {
                // addLike方法已经修改了likedBy列表并增加了点赞数
                dataManager.saveNews(news)
                updateLikeButton()
                Toast.makeText(this, "点赞成功！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "你已达到点赞上限（5次）", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 发送评论功能
     * 验证用户登录状态和评论内容，创建并保存新评论
     */
    private fun sendComment() {
        if (currentUser == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        val commentText = binding.etComment.text.toString().trim()
        if (commentText.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentNews?.let { news ->
            // 创建新评论对象
            val comment = Comment(
                newsId = news.id,
                author = currentUser!!.username,
                content = commentText
            )
            
            // 保存评论并刷新界面
            dataManager.saveComment(comment)
            binding.etComment.setText("")                       // 清空输入框
            loadComments()                                      // 重新加载评论列表
            Toast.makeText(this, "评论发表成功！", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示删除确认对话框
     * 使用AlertDialog确保用户真的想要删除新闻
     */
    private fun showDeleteConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_news_title))
            .setMessage(getString(R.string.dialog_delete_news_message))
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                deleteNews()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
    
    /**
     * 执行删除新闻操作
     * 删除新闻及其相关的所有评论数据
     * 这里调用DataManager的deleteNews方法，确保数据一致性
     */
    private fun deleteNews() {
        currentNews?.let { news ->
            // 删除新闻和所有相关数据
            dataManager.deleteNews(news.id)
            Toast.makeText(this, "新闻已删除", Toast.LENGTH_SHORT).show()
            finish()                                            // 返回上一页面
        }
    }
    
    /**
     * 处理AI总结按钮点击事件
     * 根据当前状态决定是获取新总结还是重新总结
     */
    private fun handleAiSummaryClick() {
        currentNews?.let { news ->
            // 检查是否已有缓存的总结
            if (aiService.hasCachedSummary(news.id) && binding.layoutSummaryContent.visibility == View.VISIBLE) {
                // 如果已显示总结，点击按钮重新总结
                getSummary(news)
            } else {
                // 首次获取总结或重新获取
                getSummary(news)
            }
        }
    }
    
    /**
     * 获取AI总结
     * 调用AI服务生成新闻总结并更新UI
     */
    private fun getSummary(news: News) {
        // 检查新闻内容是否为空
        if (news.content.trim().isEmpty()) {
            updateSummaryUI(SummaryState.ERROR, getString(R.string.error_ai_summary_empty_content))
            return
        }
        
        // 检查网络连接状态
        if (!NetworkUtils.isNetworkAvailable(this)) {
            isWaitingForNetwork = true
            showNetworkPermissionDialog()
            return
        }
        
        // 网络正常，清除等待标记
        isWaitingForNetwork = false
        
        // 更新UI为加载状态
        updateSummaryUI(SummaryState.LOADING)
        
        // 调用AI服务获取总结
        aiService.summarizeNews(news.id, news.content) { summary ->
            // 切换到主线程更新UI
            runOnUiThread {
                if (summary != null) {
                    // 总结成功
                    updateSummaryUI(SummaryState.SUCCESS, summary)
                } else {
                    // 总结失败
                    updateSummaryUI(SummaryState.ERROR, getString(R.string.error_ai_summary_failed))
                }
            }
        }
    }
    
    /**
     * 更新AI总结相关的UI状态
     * 根据不同状态显示对应的界面元素
     * 
     * @param state 当前状态
     * @param message 要显示的消息（成功时为总结内容，失败时为错误信息）
     */
    private fun updateSummaryUI(state: SummaryState, message: String? = null) {
        when (state) {
            SummaryState.IDLE -> {
                // 初始状态
                binding.btnAiSummary.text = getString(R.string.btn_ai_summary)
                binding.btnAiSummary.isEnabled = true
                binding.btnAiSummary.visibility = View.VISIBLE
                binding.progressSummary.visibility = View.GONE
                binding.layoutSummaryContent.visibility = View.GONE
                binding.tvSummaryError.visibility = View.GONE
            }
            
            SummaryState.LOADING -> {
                // 加载中状态
                binding.btnAiSummary.text = getString(R.string.btn_ai_summary_loading)
                binding.btnAiSummary.isEnabled = false
                binding.btnAiSummary.visibility = View.VISIBLE
                // 添加进度条淡入动画
                binding.progressSummary.alpha = 0f
                binding.progressSummary.visibility = View.VISIBLE
                ObjectAnimator.ofFloat(binding.progressSummary, "alpha", 0f, 1f).apply {
                    duration = 300
                    start()
                }
                binding.layoutSummaryContent.visibility = View.GONE
                binding.tvSummaryError.visibility = View.GONE
            }
            
            SummaryState.SUCCESS -> {
                // 成功状态 - 隐藏按钮，不再显示重新总结选项
                binding.btnAiSummary.visibility = View.GONE
                binding.progressSummary.visibility = View.GONE
                
                // 显示总结内容
                if (message != null) {
                    binding.tvSummaryContent.text = message
                }
                
                // 添加内容区域展开动画
                binding.layoutSummaryContent.alpha = 0f
                binding.layoutSummaryContent.translationY = -50f
                binding.layoutSummaryContent.visibility = View.VISIBLE
                
                val animatorSet = AnimatorSet()
                val fadeIn = ObjectAnimator.ofFloat(binding.layoutSummaryContent, "alpha", 0f, 1f)
                val slideIn = ObjectAnimator.ofFloat(binding.layoutSummaryContent, "translationY", -50f, 0f)
                
                animatorSet.playTogether(fadeIn, slideIn)
                animatorSet.duration = 400
                animatorSet.interpolator = AccelerateDecelerateInterpolator()
                animatorSet.start()
                
                binding.tvSummaryError.visibility = View.GONE
            }
            
            SummaryState.ERROR -> {
                // 错误状态
                binding.btnAiSummary.text = getString(R.string.btn_ai_summary)
                binding.btnAiSummary.isEnabled = true
                binding.btnAiSummary.visibility = View.VISIBLE
                binding.progressSummary.visibility = View.GONE
                binding.layoutSummaryContent.visibility = View.GONE
                binding.tvSummaryError.visibility = View.VISIBLE
                
                // 显示错误信息
                if (message != null) {
                    binding.tvSummaryError.text = message
                }
            }
        }
    }
    
    /**
     * 显示网络权限对话框
     * 当检测到无网络连接时，提示用户并提供打开设置的选项
     */
    private fun showNetworkPermissionDialog() {
        // 获取详细的网络状态信息
        val networkStatus = NetworkUtils.getNetworkStatusDetail(this)
        val detailedMessage = "${getString(R.string.dialog_network_permission_message)}\n\n当前状态：$networkStatus"
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_network_permission_title))
            .setMessage(detailedMessage)
            .setPositiveButton(getString(R.string.dialog_open_settings)) { _, _ ->
                // 跳转到网络设置页面
                openNetworkSettings()
            }
            .setNegativeButton(getString(R.string.dialog_retry)) { _, _ ->
                // 重新尝试获取总结
                currentNews?.let { news ->
                    getSummary(news)
                }
            }
            .setNeutralButton(getString(R.string.dialog_cancel), null)
            .setCancelable(true)
            .show()
    }
    
    /**
     * 打开网络设置页面
     * 引导用户到系统设置中检查和配置网络连接
     */
    private fun openNetworkSettings() {
        try {
            // 尝试打开WiFi设置页面
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // 如果WiFi设置不可用，打开通用网络设置
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                try {
                    // 最后尝试打开系统设置主页
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                } catch (e3: Exception) {
                    // 如果所有设置页面都无法打开，显示Toast提示
                    Toast.makeText(this, "无法打开设置页面，请手动检查网络连接", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 当用户从设置页面返回时，自动检查网络状态
        if (isWaitingForNetwork && NetworkUtils.isNetworkAvailable(this)) {
            // 网络已恢复，提示用户并自动重试
            isWaitingForNetwork = false
            
            val networkType = NetworkUtils.getNetworkType(this)
            Toast.makeText(this, "网络连接已恢复 ($networkType)，正在重新获取AI总结...", Toast.LENGTH_SHORT).show()
            
            // 自动重新尝试获取总结
            currentNews?.let { news ->
                getSummary(news)
            }
        } else if (binding.tvSummaryError.visibility == View.VISIBLE && NetworkUtils.isNetworkAvailable(this)) {
            // 如果当前显示错误状态，但网络正常，提示用户可以重试
            Toast.makeText(this, "网络连接正常，您可以重新尝试AI总结功能", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Activity销毁时清理AI缓存，释放内存
        // 注意：这里只是为了演示，实际上可能希望保留缓存直到应用退出
        // aiService.clearCache()
    }
} 