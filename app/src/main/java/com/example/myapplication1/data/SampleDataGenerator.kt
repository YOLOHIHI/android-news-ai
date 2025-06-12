package com.example.myapplication1.data

import android.content.Context

/**
 * 数据生成器
 */
class SampleDataGenerator(private val context: Context) {
    
    private val dataManager = DataManager(context)

    /**
     * 生成示例数据的
     * 创建示例用户、新闻和评论
     */
    fun generateSampleData() {
        // 检查是否已存在admin用户，如果不存在则重新生成用户数据
        val adminUser = dataManager.getUserByUsername("admin")
        if (adminUser == null) {
            generateUsers()
        }
        
        // 检查是否已存在新闻数据，避免重复生成
        if (dataManager.getAllNews().isNotEmpty()) {
            return // 新闻数据已存在，直接返回
        }
        
        generateNewsAndComments()
    }
    
    /**
     * 生成用户数据
     */
    private fun generateUsers() {
        
        // 创建管理员用户
        val adminUser = User(
            id = "admin",
            username = "admin",
            password = "12345",
            email = "admin@example.com"
        )
        
        // 创建示例用户
        // 这里创建了三个不同的用户，用于模拟多用户环境
        val user1 = User(
            id = "user1",
            username = "张三",
            password = "123456",  // 添加示例用户密码
            email = "zhangsan@example.com"
        )
        
        val user2 = User(
            id = "user2", 
            username = "李四",
            password = "123456",  // 添加示例用户密码
            email = "lisi@example.com"
        )
        
        val user3 = User(
            id = "user3",
            username = "王五",
            password = "123456",  // 添加示例用户密码
            email = "wangwu@example.com"
        )
        
        // 保存用户数据到文件
        dataManager.saveUser(adminUser)  // 首先保存管理员用户
        dataManager.saveUser(user1)
        dataManager.saveUser(user2)
        dataManager.saveUser(user3)
    }
    
    /**
     * 生成新闻和评论数据
     */
    private fun generateNewsAndComments() {
        
        // 创建示例新闻
        // 涵盖了不同的主题和标签，展示应用的分类功能
        val news1 = News(
            title = "人工智能技术的最新突破",
            content = "近日，人工智能领域取得了重大突破。研究人员开发出了一种新的深度学习算法，能够更好地理解自然语言，并在多个基准测试中取得了优异的成绩。这项技术的应用前景广阔，预计将在医疗、教育、金融等多个领域发挥重要作用。专家表示，这一突破标志着人工智能技术向更加智能化的方向迈进了一大步。",
            author = "张三",
            tags = listOf("科技", "人工智能", "深度学习"),
            likes = 15,
            likedBy = mutableListOf("user2", "user3", "user1", "user2"), // 模拟多次点赞
            timestamp = System.currentTimeMillis() - 86400000 // 1天前发布
        )
        
        val news2 = News(
            title = "体育赛事精彩回顾",
            content = "昨晚的足球比赛可谓精彩纷呈，双方球员在场上展现了高超的技艺和顽强的拼搏精神。比赛过程跌宕起伏，观众们为之喝彩。最终，主队以2:1的比分险胜客队，赢得了这场关键的胜利。赛后，教练表示对球员们的表现非常满意，并对接下来的比赛充满信心。",
            author = "李四",
            tags = listOf("体育", "足球", "比赛"),
            likes = 8,
            likedBy = mutableListOf("user1", "user3"),
            timestamp = System.currentTimeMillis() - 43200000 // 12小时前发布
        )
        
        val news3 = News(
            title = "健康生活小贴士",
            content = "保持健康的生活方式对每个人都很重要。专家建议，我们应该保持规律的作息时间，每天进行适量的运动，多吃蔬菜水果，少吃油腻食物。此外，保持良好的心态也是健康生活的重要组成部分。定期体检可以帮助我们及时发现和预防疾病。让我们一起努力，过上更加健康的生活！",
            author = "王五",
            tags = listOf("健康", "生活", "养生"),
            likes = 12,
            likedBy = mutableListOf("user1", "user2", "user1"),
            timestamp = System.currentTimeMillis() - 21600000 // 6小时前发布
        )
        
        val news4 = News(
            title = "旅游攻略分享",
            content = "最近去了一趟云南，风景真的很美！推荐大家去大理和丽江，古城的韵味让人流连忘返。当地的美食也很棒，过桥米线、鲜花饼都值得一试。如果时间充裕，建议在那里多待几天，慢慢体验当地的文化和风土人情。记得带好防晒用品，高原的紫外线比较强。",
            author = "张三",
            tags = listOf("旅游", "云南", "攻略"),
            likes = 6,
            likedBy = mutableListOf("user2", "user3"),
            timestamp = System.currentTimeMillis() - 10800000 // 3小时前发布
        )
        
        // 保存新闻数据
        dataManager.saveNews(news1)
        dataManager.saveNews(news2)
        dataManager.saveNews(news3)
        dataManager.saveNews(news4)
        
        // 创建示例评论
        // 为每条新闻添加一些评论，增加互动性
        val comment1 = Comment(
            newsId = news1.id,
            author = "李四",
            content = "这个技术确实很有前景，期待能早日应用到实际生活中！"
        )
        
        val comment2 = Comment(
            newsId = news1.id,
            author = "王五",
            content = "人工智能的发展速度真是令人惊叹，希望能带来更多便利。"
        )
        
        val comment3 = Comment(
            newsId = news2.id,
            author = "张三",
            content = "这场比赛我也看了，确实很精彩！主队的表现太棒了。"
        )
        
        val comment4 = Comment(
            newsId = news3.id,
            author = "李四",
            content = "健康生活确实很重要，谢谢分享这些实用的建议。"
        )
        
        val comment5 = Comment(
            newsId = news4.id,
            author = "王五",
            content = "云南真的很美，我也想去看看！谢谢分享攻略。"
        )
        
        // 保存评论数据
        dataManager.saveComment(comment1)
        dataManager.saveComment(comment2)
        dataManager.saveComment(comment3)
        dataManager.saveComment(comment4)
        dataManager.saveComment(comment5)
    }
} 