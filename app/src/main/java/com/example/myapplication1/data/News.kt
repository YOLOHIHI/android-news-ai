package com.example.myapplication1.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * 新闻数据类
 */
data class News(
    val id: String = UUID.randomUUID().toString(),    // 新闻唯一ID，使用UUID确保全局唯一性
    val title: String,                                 // 新闻标题，必填字段
    val content: String,                               // 新闻内容，支持长文本
    val author: String,                                // 作者用户名，用于权限控制和显示
    val tags: List<String>,                           // 标签列表，用于分类和筛选功能
    val images: List<String> = emptyList(),           // 图片路径列表，预留字段用于多媒体扩展
    var likes: Int = 0,                               // 点赞总数，使用var因为需要动态更新
    val likedBy: MutableList<String> = mutableListOf(), // 点赞用户列表，用于防止重复点赞
    val comments: MutableList<Comment> = mutableListOf(), // 评论列表，存储所有相关评论
    val timestamp: Long = System.currentTimeMillis(), // 发布时间戳，用于排序和显示
    val isDraft: Boolean = false,                      // 是否为草稿，用于区分已发布和草稿状态
    val reviewStatus: ReviewStatus = ReviewStatus.APPROVED, // 审核状态，默认为已通过（兼容旧数据）
    val reviewInfo: ReviewInfo? = null                 // 审核详细信息，可选字段
) {
    /**
     * 格式化显示时间
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * 检查用户是否可以继续点赞
     * 限制每个用户最多点赞5次
     */
    fun canUserLike(userId: String): Boolean {
        return likedBy.count { it == userId } < 5
    }
    
    /**
     * 添加点赞功能
     */
    fun addLike(userId: String): Boolean {
        return if (canUserLike(userId)) {
            likedBy.add(userId)                       // 记录点赞用户
            likes++                                   // 增加点赞计数
            true                                      // 返回成功标志
        } else {
            false                                     // 达到限制，返回失败
        }
    }
    
    /**
     * 检查新闻是否可以在主页显示
     * 只有审核通过的非草稿新闻才能显示
     */
    fun isPubliclyVisible(): Boolean {
        return !isDraft && reviewStatus == ReviewStatus.APPROVED
    }
    
    /**
     * 获取审核状态的中文描述
     */
    fun getReviewStatusText(): String {
        return when (reviewStatus) {
            ReviewStatus.DRAFT -> "草稿"
            ReviewStatus.PENDING -> "审核中"
            ReviewStatus.APPROVED -> "已发布"
            ReviewStatus.REJECTED -> "审核不通过"
        }
    }
}

/**
 * 新闻审核状态枚举
 */
enum class ReviewStatus {
    DRAFT,      // 草稿状态
    PENDING,    // 待审核状态
    APPROVED,   // 审核通过
    REJECTED    // 审核拒绝
}

/**
 * 审核详细信息数据类
 */
data class ReviewInfo(
    val submittedAt: Long,                           // 提交审核时间戳
    val reviewedAt: Long? = null,                    // 审核完成时间戳
    val reviewerId: String? = null,                  // 审核员用户ID
    val reviewComment: String = "",                  // 人工审核意见
    val aiSuggestion: String? = null,                // AI审核建议完整内容
    val aiDecision: String? = null                   // AI审核结论（通过/不通过/需要考虑）
) {
    /**
     * 格式化审核时间显示
     */
    fun getFormattedSubmittedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(submittedAt))
    }
    
    /**
     * 格式化审核完成时间显示
     */
    fun getFormattedReviewedTime(): String? {
        return reviewedAt?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }
} 