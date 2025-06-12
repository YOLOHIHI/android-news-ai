package com.example.myapplication1.data

import android.content.Context
import java.io.File
import java.io.FileWriter

/**
 * 数据管理器类
 * 负责所有数据的持久化存储和读取
 * 主要管理用户、新闻、评论三类数据，以及当前登录用户状态
 */
class DataManager(private val context: Context) {
    
    // 数据存储目录和文件定义
    private val dataDir = File(context.filesDir, "app_data")        // 应用数据目录
    private val newsFile = File(dataDir, "news.txt")                // 新闻数据文件
    private val commentsFile = File(dataDir, "comments.txt")        // 评论数据文件
    private val usersFile = File(dataDir, "users.txt")             // 用户数据文件
    private val currentUserFile = File(dataDir, "current_user.txt") // 当前登录用户文件
    
    init {
        // 初始化时确保数据目录存在
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }
    
    // ==================== 新闻相关操作 ====================
    
    /**
     * 保存新闻数据
     * 支持新增和更新操作，通过ID判断是否为已存在的新闻
     */
    fun saveNews(news: News) {
        val newsList = getAllNews().toMutableList()
        val existingIndex = newsList.indexOfFirst { it.id == news.id }
        if (existingIndex >= 0) {
            newsList[existingIndex] = news                          // 更新已存在的新闻
        } else {
            newsList.add(news)                                      // 添加新的新闻
        }
        saveNewsList(newsList)
    }
    
    /**
     * 获取所有新闻列表
     * 从文件中读取并解析所有新闻数据
     * 使用mapNotNull来过滤解析失败的数据
     */
    fun getAllNews(): List<News> {
        if (!newsFile.exists()) return emptyList()
        
        return try {
            val lines = newsFile.readLines()
            lines.mapNotNull { line ->
                parseNewsFromLine(line)                             // 解析每一行数据
            }
        } catch (e: Exception) {
            emptyList()                                             // 异常时返回空列表
        }
    }
    
    /**
     * 根据标签筛选新闻
     */
    fun getNewsByTag(tag: String): List<News> {
        return getAllNews().filter { news ->
            news.tags.any { it.equals(tag, ignoreCase = true) }
        }
    }
    
    /**
     * 根据ID获取特定新闻
     * 用于新闻详情页面的数据获取
     */
    fun getNewsById(id: String): News? {
        return getAllNews().find { it.id == id }
    }
    
    /**
     * 删除新闻
     * 同时删除相关的评论数据，保持数据一致性
     * 这个设计考虑了数据的完整性，避免孤立的评论数据
     */
    fun deleteNews(id: String) {
        val newsList = getAllNews().filter { it.id != id }
        saveNewsList(newsList)
        
        // 同时删除相关评论，保持数据一致性
        deleteCommentsByNewsId(id)
    }
    
    /**
     * 保存新闻列表到文件
     */
    private fun saveNewsList(newsList: List<News>) {
        try {
            FileWriter(newsFile).use { writer ->
                newsList.forEach { news ->
                    writer.write(newsToLine(news) + "\n")           // 每条新闻占一行
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()                                     // 记录异常信息，便于调试
        }
    }
    
    /**
     * 将新闻对象序列化为字符串
     * 对特殊字符进行转义以防止数据格式错误
     */
    private fun newsToLine(news: News): String {
        val reviewInfoStr = news.reviewInfo?.let { info ->
            "${info.submittedAt},${info.reviewedAt ?: ""},${escapeString(info.reviewerId ?: "")},${escapeString(info.reviewComment)},${escapeString(info.aiSuggestion ?: "")},${escapeString(info.aiDecision ?: "")}"
        } ?: ""
        
        return "${news.id}|${escapeString(news.title)}|${escapeString(news.content)}|${escapeString(news.author)}|${news.tags.joinToString(",")}|${news.images.joinToString(",")}|${news.likes}|${news.likedBy.joinToString(",")}|${news.timestamp}|${news.isDraft}|${news.reviewStatus}|${escapeString(reviewInfoStr)}"
    }
    
    /**
     * 字符串转义方法
     * 转义可能破坏数据格式的特殊字符
     */
    private fun escapeString(input: String): String {
        return input
            .replace("\\", "\\\\")  // 反斜杠必须最先处理
            .replace("|", "\\|")    // 转义管道符
            .replace("\n", "\\n")   // 转义换行符
            .replace("\r", "\\r")   // 转义回车符
    }
    
    /**
     * 字符串反转义方法
     * 恢复转义的特殊字符
     */
    private fun unescapeString(input: String): String {
        return input
            .replace("\\r", "\r")   // 恢复回车符
            .replace("\\n", "\n")   // 恢复换行符  
            .replace("\\|", "|")    // 恢复管道符
            .replace("\\\\", "\\")  // 反斜杠必须最后处理
    }
    
    /**
     * 从字符串解析新闻对象
     * 对转义的特殊字符进行反转义
     * 支持新旧数据格式的兼容性
     */
    private fun parseNewsFromLine(line: String): News? {
        return try {
            val parts = line.split("|")
            if (parts.size >= 10) {
                val comments = getCommentsByNewsId(parts[0])        // 获取相关评论
                
                // 解析审核状态（兼容旧格式）
                val reviewStatus = if (parts.size >= 11) {
                    try {
                        ReviewStatus.valueOf(parts[10])
                    } catch (e: Exception) {
                        ReviewStatus.APPROVED  // 旧数据默认为已通过
                    }
                } else {
                    ReviewStatus.APPROVED  // 旧数据默认为已通过
                }
                
                // 解析审核信息（兼容旧格式）
                val reviewInfo = if (parts.size >= 12 && parts[11].isNotEmpty()) {
                    try {
                        val reviewInfoStr = unescapeString(parts[11])
                        val reviewParts = reviewInfoStr.split(",")
                        if (reviewParts.size >= 6) {
                            ReviewInfo(
                                submittedAt = reviewParts[0].toLongOrNull() ?: System.currentTimeMillis(),
                                reviewedAt = reviewParts[1].toLongOrNull(),
                                reviewerId = unescapeString(reviewParts[2]).takeIf { it.isNotEmpty() },
                                reviewComment = unescapeString(reviewParts[3]),
                                aiSuggestion = unescapeString(reviewParts[4]).takeIf { it.isNotEmpty() },
                                aiDecision = unescapeString(reviewParts[5]).takeIf { it.isNotEmpty() }
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } else null
                
                News(
                    id = parts[0],
                    title = unescapeString(parts[1]),
                    content = unescapeString(parts[2]),
                    author = unescapeString(parts[3]),
                    tags = if (parts[4].isNotEmpty()) parts[4].split(",") else emptyList(),
                    images = if (parts[5].isNotEmpty()) parts[5].split(",") else emptyList(),
                    likes = parts[6].toIntOrNull() ?: 0,
                    likedBy = if (parts[7].isNotEmpty()) parts[7].split(",").toMutableList() else mutableListOf(),
                    comments = comments.toMutableList(),
                    timestamp = parts[8].toLongOrNull() ?: System.currentTimeMillis(),
                    isDraft = parts[9].toBooleanStrictOrNull() ?: false,
                    reviewStatus = reviewStatus,
                    reviewInfo = reviewInfo
                )
            } else null
        } catch (e: Exception) {
            null                                                    // 解析失败返回null
        }
    }
    
    // ==================== 评论相关操作 ====================
    
    /**
     * 保存评论数据
     */
    fun saveComment(comment: Comment) {
        try {
            FileWriter(commentsFile, true).use { writer ->         // true表示追加模式
                writer.write(commentToLine(comment) + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取指定新闻的所有评论
     */
    fun getCommentsByNewsId(newsId: String): List<Comment> {
        if (!commentsFile.exists()) return emptyList()
        
        return try {
            val lines = commentsFile.readLines()
            lines.mapNotNull { line ->
                parseCommentFromLine(line)
            }.filter { it.newsId == newsId }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取指定用户的所有评论
     * 用于用户个人页面显示评论历史
     */
    fun getAllCommentsByUser(username: String): List<Comment> {
        if (!commentsFile.exists()) return emptyList()
        
        return try {
            val lines = commentsFile.readLines()
            lines.mapNotNull { line ->
                parseCommentFromLine(line)
            }.filter { it.author == username }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 删除指定新闻的所有评论
     */
    private fun deleteCommentsByNewsId(newsId: String) {
        if (!commentsFile.exists()) return
        
        try {
            val lines = commentsFile.readLines()
            val remainingComments = lines.mapNotNull { line ->
                parseCommentFromLine(line)
            }.filter { it.newsId != newsId }                       // 过滤掉要删除的评论
            
            FileWriter(commentsFile).use { writer ->
                remainingComments.forEach { comment ->
                    writer.write(commentToLine(comment) + "\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 评论对象序列化
     * 对评论内容进行转义以防止数据格式错误
     */
    private fun commentToLine(comment: Comment): String {
        return "${comment.id}|${comment.newsId}|${escapeString(comment.author)}|${escapeString(comment.content)}|${comment.timestamp}"
    }
    
    /**
     * 评论对象反序列化
     * 对转义的字符进行反转义
     */
    private fun parseCommentFromLine(line: String): Comment? {
        return try {
            val parts = line.split("|")
            if (parts.size >= 5) {
                Comment(
                    id = parts[0],
                    newsId = parts[1],
                    author = unescapeString(parts[2]),
                    content = unescapeString(parts[3]),
                    timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // ==================== 用户相关操作 ====================
    
    /**
     * 保存用户数据
     * 支持用户信息的新增和更新
     * 通过ID判断是否为已存在用户
     */
    fun saveUser(user: User) {
        val users = getAllUsers().toMutableList()
        val existingIndex = users.indexOfFirst { it.id == user.id }
        if (existingIndex >= 0) {
            users[existingIndex] = user                             // 更新已存在用户
        } else {
            users.add(user)                                         // 添加新用户
        }
        saveUsersList(users)
    }
    
    /**
     * 获取所有用户列表
     * 主要用于用户查找和验证
     */
    fun getAllUsers(): List<User> {
        if (!usersFile.exists()) return emptyList()
        
        return try {
            val lines = usersFile.readLines()
            lines.mapNotNull { line ->
                parseUserFromLine(line)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据ID查找用户
     * 用于获取用户详细信息
     */
    fun getUserById(id: String): User? {
        return getAllUsers().find { it.id == id }
    }
    
    /**
     * 根据用户名查找用户
     * 主要用于登录验证和用户名重复检查
     */
    fun getUserByUsername(username: String): User? {
        return getAllUsers().find { it.username == username }
    }
    
    /**
     * 保存用户列表到文件
     */
    private fun saveUsersList(users: List<User>) {
        try {
            FileWriter(usersFile).use { writer ->
                users.forEach { user ->
                    writer.write(userToLine(user) + "\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 用户对象序列化
     * 对用户信息进行转义以防止数据格式错误
     */
    private fun userToLine(user: User): String {
        return "${user.id}|${escapeString(user.username)}|${escapeString(user.password)}|${escapeString(user.email)}|${escapeString(user.avatar)}|${user.totalLikes}|${user.totalComments}|${user.registrationDate}"
    }
    
    /**
     * 用户对象反序列化
     * 对转义的字符进行反转义
     */
    private fun parseUserFromLine(line: String): User? {
        return try {
            val parts = line.split("|")
            // 兼容旧格式（包含followedTags）和新格式
            if (parts.size >= 8) {
                User(
                    id = parts[0],
                    username = unescapeString(parts[1]),
                    password = unescapeString(parts[2]),
                    email = unescapeString(parts[3]),
                    avatar = unescapeString(parts[4]),
                    totalLikes = parts[5].toIntOrNull() ?: 0,
                    totalComments = parts[6].toIntOrNull() ?: 0,
                    registrationDate = if (parts.size >= 9) parts[8].toLongOrNull() ?: System.currentTimeMillis()
                                     else parts[7].toLongOrNull() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // ==================== 当前用户状态管理 ====================
    
    /**
     * 保存当前登录用户ID
     */
    fun saveCurrentUser(userId: String) {
        try {
            FileWriter(currentUserFile).use { writer ->
                writer.write(userId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取当前登录用户
     * 读取用户ID后查找完整的用户信息
     */
    fun getCurrentUser(): User? {
        if (!currentUserFile.exists()) return null
        
        return try {
            val userId = currentUserFile.readText().trim()
            getUserById(userId)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 清除当前用户状态
     * 用于用户退出登录
     */
    fun clearCurrentUser() {
        if (currentUserFile.exists()) {
            currentUserFile.delete()
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取唯一的标签列表
     * 用于主界面的标签筛选功能
     */
    fun getAllTags(): List<String> {
        return getAllNews()
            .filter { it.isPubliclyVisible() }  // 只统计公开可见的新闻标签
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }
    
    // ==================== 审核系统相关操作 ====================
    
    /**
     * 检查当前用户是否为管理员
     */
    fun isCurrentUserAdmin(): Boolean {
        val currentUser = getCurrentUser()
        return currentUser?.username == "admin"
    }
    
    /**
     * 检查指定用户是否为管理员
     */
    fun isUserAdmin(userId: String): Boolean {
        val user = getUserById(userId)
        return user?.username == "admin"
    }
    
    /**
     * 获取所有待审核的新闻
     * 仅管理员可调用
     */
    fun getPendingNewsForReview(): List<News> {
        if (!isCurrentUserAdmin()) {
            return emptyList()
        }
        return getAllNews().filter { it.reviewStatus == ReviewStatus.PENDING }
            .sortedBy { it.reviewInfo?.submittedAt ?: 0 }
    }
    
    /**
     * 提交新闻审核
     * 将新闻状态从DRAFT改为PENDING
     */
    fun submitNewsForReview(newsId: String): Boolean {
        val news = getNewsById(newsId) ?: return false
        val currentUser = getCurrentUser() ?: return false
        
        // 只有作者才能提交审核
        if (news.author != currentUser.username) return false
        
        // 只有草稿状态的新闻才能提交审核
        if (news.reviewStatus != ReviewStatus.DRAFT) return false
        
        val updatedNews = news.copy(
            reviewStatus = ReviewStatus.PENDING,
            reviewInfo = ReviewInfo(
                submittedAt = System.currentTimeMillis()
            )
        )
        
        saveNews(updatedNews)
        return true
    }
    
    /**
     * 批准新闻发布
     * 仅管理员可调用
     */
    fun approveNews(newsId: String, reviewComment: String): Boolean {
        if (!isCurrentUserAdmin()) return false
        
        val news = getNewsById(newsId) ?: return false
        if (news.reviewStatus != ReviewStatus.PENDING) return false
        
        val currentUser = getCurrentUser() ?: return false
        val updatedReviewInfo = news.reviewInfo?.copy(
            reviewedAt = System.currentTimeMillis(),
            reviewerId = currentUser.id,
            reviewComment = reviewComment
        ) ?: ReviewInfo(
            submittedAt = System.currentTimeMillis(),
            reviewedAt = System.currentTimeMillis(),
            reviewerId = currentUser.id,
            reviewComment = reviewComment
        )
        
        val updatedNews = news.copy(
            reviewStatus = ReviewStatus.APPROVED,
            reviewInfo = updatedReviewInfo
        )
        
        saveNews(updatedNews)
        return true
    }
    
    /**
     * 拒绝新闻发布
     * 仅管理员可调用
     */
    fun rejectNews(newsId: String, reviewComment: String): Boolean {
        if (!isCurrentUserAdmin()) return false
        
        val news = getNewsById(newsId) ?: return false
        if (news.reviewStatus != ReviewStatus.PENDING) return false
        
        val currentUser = getCurrentUser() ?: return false
        val updatedReviewInfo = news.reviewInfo?.copy(
            reviewedAt = System.currentTimeMillis(),
            reviewerId = currentUser.id,
            reviewComment = reviewComment
        ) ?: ReviewInfo(
            submittedAt = System.currentTimeMillis(),
            reviewedAt = System.currentTimeMillis(),
            reviewerId = currentUser.id,
            reviewComment = reviewComment
        )
        
        val updatedNews = news.copy(
            reviewStatus = ReviewStatus.REJECTED,
            reviewInfo = updatedReviewInfo
        )
        
        saveNews(updatedNews)
        return true
    }
    
    /**
     * 更新新闻的AI审核建议
     * 仅管理员可调用
     */
    fun updateNewsAIReview(newsId: String, aiDecision: String, aiSuggestion: String): Boolean {
        if (!isCurrentUserAdmin()) return false
        
        val news = getNewsById(newsId) ?: return false
        if (news.reviewStatus != ReviewStatus.PENDING) return false
        
        val updatedReviewInfo = news.reviewInfo?.copy(
            aiDecision = aiDecision,
            aiSuggestion = aiSuggestion
        ) ?: ReviewInfo(
            submittedAt = System.currentTimeMillis(),
            aiDecision = aiDecision,
            aiSuggestion = aiSuggestion
        )
        
        val updatedNews = news.copy(reviewInfo = updatedReviewInfo)
        saveNews(updatedNews)
        return true
    }
    
    /**
     * 获取用户的审核状态统计
     */
    fun getUserReviewStats(username: String): Map<ReviewStatus, Int> {
        val userNews = getAllNews().filter { it.author == username }
        return mapOf(
            ReviewStatus.DRAFT to userNews.count { it.reviewStatus == ReviewStatus.DRAFT },
            ReviewStatus.PENDING to userNews.count { it.reviewStatus == ReviewStatus.PENDING },
            ReviewStatus.APPROVED to userNews.count { it.reviewStatus == ReviewStatus.APPROVED },
            ReviewStatus.REJECTED to userNews.count { it.reviewStatus == ReviewStatus.REJECTED }
        )
    }
    
    /**
     * 获取所有公开可见的新闻（已审核通过的非草稿）
     * 用于主页显示
     */
    fun getPublicNews(): List<News> {
        return getAllNews().filter { it.isPubliclyVisible() }
    }
} 