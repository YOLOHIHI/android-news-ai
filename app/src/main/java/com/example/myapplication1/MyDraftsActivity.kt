package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication1.adapters.DraftAdapter
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.User
import com.example.myapplication1.databinding.ActivityMyDraftsBinding

/**
 * 草稿管理页面
 * 显示用户保存的所有草稿，支持编辑和删除操作
 */
class MyDraftsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMyDraftsBinding
    private lateinit var dataManager: DataManager
    private lateinit var draftAdapter: DraftAdapter
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyDraftsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        currentUser = dataManager.getCurrentUser()
        
        if (currentUser == null) {
            finish()
            return
        }
        
        setupViews()
        setupClickListeners()
        loadMyDrafts()
    }
    
    /**
     * 设置RecyclerView和草稿适配器
     * 配置编辑和删除回调函数
     */
    private fun setupViews() {
        binding.rvMyDrafts.layoutManager = LinearLayoutManager(this)
        draftAdapter = DraftAdapter(
            onEditClick = { draft ->
                // 进入编辑界面
                val intent = Intent(this, PostNewsActivity::class.java)
                intent.putExtra("draft_id", draft.id)
                startActivity(intent)
            },
            onDeleteClick = { draft ->
                // 删除草稿
                dataManager.deleteNews(draft.id)
                loadMyDrafts()
                android.widget.Toast.makeText(this, "草稿已删除", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvMyDrafts.adapter = draftAdapter
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnNewDraft.setOnClickListener {
            // 创建新草稿
            val intent = Intent(this, PostNewsActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * 加载用户的草稿列表
     * 只显示isDraft为true的新闻
     */
    private fun loadMyDrafts() {
        currentUser?.let { user ->
            val myDrafts = dataManager.getAllNews().filter { 
                it.author == user.username && it.isDraft 
            }.sortedByDescending { it.timestamp }
            
            draftAdapter.updateDrafts(myDrafts)
            
            // 更新标题显示草稿数量
            binding.tvTitle.text = getString(R.string.title_my_drafts_with_count, myDrafts.size)
            
            // 显示空状态
            if (myDrafts.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.rvMyDrafts.visibility = android.view.View.GONE
                
                // 设置空状态按钮点击事件
                binding.tvEmptyState.findViewById<android.widget.Button>(com.example.myapplication1.R.id.btn_create_first_draft)?.setOnClickListener {
                    val intent = Intent(this, PostNewsActivity::class.java)
                    startActivity(intent)
                }
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.rvMyDrafts.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadMyDrafts()  // 返回时刷新草稿列表
    }
} 