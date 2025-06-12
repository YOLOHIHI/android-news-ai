package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.adapters.CommentAdapter
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.User
import com.example.myapplication1.databinding.ActivityProfileBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 用户个人资料页面
 * 显示用户的基本信息、统计数据和个人评论
 * 提供注销、数据导出等功能
 */
class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private lateinit var dataManager: DataManager
    private lateinit var commentAdapter: CommentAdapter
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        currentUser = dataManager.getCurrentUser()
        
        if (currentUser == null) {
            // 用户未登录，跳转到登录页面
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setupViews()
        setupClickListeners()
        loadUserData()
    }
    
    /**
     * 设置RecyclerView显示用户评论
     */
    private fun setupViews() {
        binding.rvMyComments.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter()
        binding.rvMyComments.adapter = commentAdapter
    }
    
    /**
     * 设置各种按钮的点击事件
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
        
        binding.btnMyDrafts.setOnClickListener {
            showMyDrafts()
        }
        
        binding.btnMyNews.setOnClickListener {
            showMyNews()
        }
        

    }
    
    /**
     * 加载用户数据和统计信息
     * 包括基本信息、新闻统计、评论统计等
     */
    private fun loadUserData() {
        currentUser?.let { user ->
            binding.tvUsername.text = user.username
            binding.tvUserId.text = getString(R.string.label_user_id, user.id.substring(0, 8) + "...")
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.tvRegistrationDate.text = getString(R.string.label_registration_date, sdf.format(Date(user.registrationDate)))
            
            // 加载用户评论
            val userComments = dataManager.getAllCommentsByUser(user.username)
            commentAdapter.updateComments(userComments)
            binding.tvMyCommentsTitle.text = getString(R.string.label_my_comments, userComments.size)
            binding.tvCommentStat.text = userComments.size.toString()
            
            // 计算获得的总点赞数
            val userNews = dataManager.getAllNews().filter { it.author == user.username }
            val totalLikes = userNews.sumOf { it.likes }
            binding.tvTotalLikes.text = totalLikes.toString()
            
            // 计算发布的新闻数量
            val publishedNews = userNews.filter { !it.isDraft }
            binding.tvNewsCount.text = publishedNews.size.toString()
            
            // 计算草稿数量
            val drafts = userNews.filter { it.isDraft }
            binding.tvDraftCount.text = drafts.size.toString()
            

        }
    }
    

    
    /**
     * 用户注销功能
     */
    private fun logout() {
        dataManager.clearCurrentUser()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * 跳转到草稿管理页面
     */
    private fun showMyDrafts() {
        val intent = Intent(this, MyDraftsActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * 跳转到我的新闻页面
     */
    private fun showMyNews() {
        val intent = Intent(this, MyNewsActivity::class.java)
        startActivity(intent)
    }
    

} 