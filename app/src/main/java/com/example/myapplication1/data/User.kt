package com.example.myapplication1.data

/**
 * 用户数据类
 * 定义了应用中用户的基本信息结构
 */
data class User(
    val id: String,                                    // 用户唯一标识符，用于区分不同用户
    val username: String,                              // 用户名，用于登录和显示
    val password: String = "",                         // 用户密码，默认为空字符串（兼容旧数据）
    val email: String = "",                           // 邮箱地址，预留字段，暂未使用
    val avatar: String = "",                          // 头像路径，预留字段，可用于后续头像功能
    val totalLikes: Int = 0,                          // 用户获得的总点赞数，用于统计用户影响力
    val totalComments: Int = 0,                       // 用户发表的总评论数，用于活跃度统计
    val registrationDate: Long = System.currentTimeMillis() // 注册时间戳，记录用户注册时间
) 