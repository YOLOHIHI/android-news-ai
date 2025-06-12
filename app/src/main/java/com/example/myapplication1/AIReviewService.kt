package com.example.myapplication1

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * AI新闻审核服务类
 * 负责调用AI API进行新闻内容审核
 * 使用单例模式确保全局唯一实例
 */
class AIReviewService private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: AIReviewService? = null
        
        /**
         * 获取AIReviewService单例实例
         */
        fun getInstance(): AIReviewService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIReviewService().also { INSTANCE = it }
            }
        }
    }
    
    // OkHttp客户端实例
    private val client = OkHttpClient()
    
    // API配置常量
    private val apiKey = "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"
    private val apiUrl = "https://openrouter.ai/api/v1/chat/completions"
    
    /**
     * 对新闻内容进行AI审核
     * 
     * @param newsId 新闻ID，用于标识
     * @param newsTitle 新闻标题
     * @param newsContent 新闻内容
     * @param callback 结果回调，成功返回审核结果，失败返回null
     */
    fun reviewNews(
        newsId: String,
        newsTitle: String, 
        newsContent: String,
        callback: (AIReviewResult?) -> Unit
    ) {
        // 内容预处理：检查是否为空或过短
        val processedTitle = preprocessContent(newsTitle)
        val processedContent = preprocessContent(newsContent)
        
        if (processedTitle.isBlank() || processedContent.isBlank()) {
            callback(null)
            return
        }
        
        // 构造待审核的完整内容
        val fullContent = "标题：$processedTitle\n\n内容：$processedContent"
        
        // 构造API请求JSON
        val json = JSONObject().apply {
            put("model", "openai/chatgpt-4o-latest")
            put("temperature", 0.3)  // 较低温度确保输出稳定
            put("max_tokens", 200)   // 限制输出长度
            put("messages", JSONArray().apply {
                // 设定AI审核专家角色
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", """
                        请你扮演一位新闻审核专家，根据以下新闻内容进行判断，并输出以下三种审核情况之一：
                        • ✔️通过：内容真实、客观、中立、符合法规
                        • ❌不通过：存在虚假、煽动、违法、敏感或误导信息
                        • ⚠️需要考虑：内容存在争议、缺乏来源、语义模糊或需人工判断
                        
                        输出格式为：
                        • ✔️通过：分析理由：<不超过60字>
                        • ❌不通过：分析理由：<不超过60字>
                        • ⚠️需要考虑：分析理由：<不超过60字>
                        
                        请严格按照上述格式输出，确保理由简洁明了。
                    """.trimIndent())
                })
                // 用户提供的新闻内容
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", fullContent)
                })
            })
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()
        
        // 异步执行API调用
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 网络请求失败
                callback(null)
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        // HTTP错误响应
                        callback(null)
                        return
                    }
                    
                    try {
                        // 解析API响应
                        val resStr = response.body?.string()
                        if (resStr != null) {
                            val reply = JSONObject(resStr)
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                            
                            val trimmedReply = reply.trim()
                            // 解析审核结果
                            val reviewResult = parseReviewResult(trimmedReply)
                            callback(reviewResult)
                        } else {
                            callback(null)
                        }
                    } catch (e: Exception) {
                        // JSON解析失败
                        callback(null)
                    }
                }
            }
        })
    }
    
    /**
     * 解析AI返回的审核结果
     * 
     * @param aiResponse AI的原始回复
     * @return 解析后的审核结果
     */
    private fun parseReviewResult(aiResponse: String): AIReviewResult? {
        return try {
            when {
                aiResponse.contains("✔️通过") -> {
                    val reason = extractReason(aiResponse, "✔️通过")
                    AIReviewResult(
                        decision = "✔️通过",
                        reason = reason,
                        fullResponse = aiResponse
                    )
                }
                aiResponse.contains("❌不通过") -> {
                    val reason = extractReason(aiResponse, "❌不通过")
                    AIReviewResult(
                        decision = "❌不通过",
                        reason = reason,
                        fullResponse = aiResponse
                    )
                }
                aiResponse.contains("⚠️需要考虑") -> {
                    val reason = extractReason(aiResponse, "⚠️需要考虑")
                    AIReviewResult(
                        decision = "⚠️需要考虑",
                        reason = reason,
                        fullResponse = aiResponse
                    )
                }
                else -> {
                    // 无法识别的格式，返回原始回复
                    AIReviewResult(
                        decision = "⚠️需要考虑",
                        reason = "AI回复格式异常，需要人工判断",
                        fullResponse = aiResponse
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从AI回复中提取分析理由
     * 
     * @param response AI完整回复
     * @param decision 审核决定标识
     * @return 提取的理由文本
     */
    private fun extractReason(response: String, decision: String): String {
        return try {
            val pattern = "$decision：分析理由：(.+?)(?:\n|$)".toRegex()
            val matchResult = pattern.find(response)
            matchResult?.groupValues?.get(1)?.trim() ?: "无具体理由"
        } catch (e: Exception) {
            "理由解析失败"
        }
    }
    
    /**
     * 内容预处理
     * 清理特殊字符，限制长度，确保API调用稳定性
     * 
     * @param content 原始内容
     * @return 处理后的内容
     */
    private fun preprocessContent(content: String): String {
        var processed = content.trim()
        
        // 移除多余的空白字符
        processed = processed.replace(Regex("\\s+"), " ")
        
        // 限制内容长度，避免API调用超限
        if (processed.length > 3000) {
            processed = processed.substring(0, 3000) + "..."
        }
        
        return processed
    }
    
    /**
     * AI审核结果数据类
     */
    data class AIReviewResult(
        val decision: String,       // 审核决定：✔️通过/❌不通过/⚠️需要考虑
        val reason: String,         // 分析理由（不超过60字）
        val fullResponse: String    // AI完整回复内容
    )
} 