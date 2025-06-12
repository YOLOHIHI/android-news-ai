package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.data.DataManager
import com.example.myapplication1.data.User
import com.example.myapplication1.databinding.ActivityLoginBinding
import java.util.*

/**
 * 登录注册Activity
 * 负责用户的登录和注册功能
 */
class LoginActivity : AppCompatActivity() {
    
    // 视图绑定对象，用于访问布局控件
    private lateinit var binding: ActivityLoginBinding
    // 数据管理器，处理用户数据的存储和验证
    private lateinit var dataManager: DataManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化数据管理器
        dataManager = DataManager(this)
        
        // 设置按钮点击监听器
        setupClickListeners()
    }
    
    /**
     * 设置按钮点击事件
     * 分别处理登录和注册两个不同的操作
     */
    private fun setupClickListeners() {
        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener {
            performLogin()
        }
        
        // 注册按钮点击事件
        binding.btnRegister.setOnClickListener {
            performRegister()
        }
    }
    
    /**
     * 执行登录操作
     * 验证用户名和密码，检查用户是否存在
     */
    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        // 基本输入验证
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 查找用户并验证密码
        val existingUser = dataManager.getUserByUsername(username)
        if (existingUser != null) {
            if (existingUser.password == password) {
                // 登录成功，保存当前用户状态
                dataManager.saveCurrentUser(existingUser.id)
                Toast.makeText(this, "登录成功！欢迎回来 $username", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                // 密码错误
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 用户不存在
            Toast.makeText(this, "用户不存在，请先注册", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 注册操作
     * 验证输入格式，检查用户名是否已存在，创建新用户
     * 5位字符的最小长度限制
     */
    private fun performRegister() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        // 基本输入验证
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 长度验证 - 确保用户名和密码都至少5位字符
        // 这个设计参考了常见的安全标准
        if (username.length < 5) {
            Toast.makeText(this, "用户名至少需要5位字符", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 5) {
            Toast.makeText(this, "密码至少需要5位字符", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 检查用户名是否已存在
        val existingUser = dataManager.getUserByUsername(username)
        if (existingUser != null) {
            Toast.makeText(this, "用户名已存在，请选择其他用户名", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 创建新用户并保存
        // 使用UUID确保用户ID的唯一性
        val newUser = User(
            id = UUID.randomUUID().toString(),
            username = username,
            password = password
        )
        dataManager.saveUser(newUser)
        dataManager.saveCurrentUser(newUser.id)                 // 注册后自动登录
        Toast.makeText(this, "注册成功！欢迎 $username", Toast.LENGTH_SHORT).show()
        navigateToMain()
    }
    
    /**
     * 跳转到主界面
     */
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 