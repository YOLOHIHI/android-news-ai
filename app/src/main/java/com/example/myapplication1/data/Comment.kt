package com.example.myapplication1.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * 评论数据类
 * 用于存储用户对新闻的评论信息
 */
data class Comment(
    val id: String = UUID.randomUUID().toString(),    // 评论唯一标识符
    val newsId: String,                                // 所属新闻的ID，用于关联评论和新闻
    val author: String,                                // 评论作者用户名
    val content: String,                               // 评论内容文本
    val timestamp: Long = System.currentTimeMillis()  // 评论发表时间戳
) {
    /**
     * 格式化评论时间显示
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
} 