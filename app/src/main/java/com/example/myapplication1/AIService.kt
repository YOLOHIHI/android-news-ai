package com.example.myapplication1

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * AI服务类
 * 负责所有AI相关功能的API调用和数据管理
 * 使用单例模式确保全局唯一实例，支持会话缓存
 */
class AIService private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: AIService? = null
        
        /**
         * 获取AIService单例实例
         */
        fun getInstance(): AIService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIService().also { INSTANCE = it }
            }
        }
    }
    
    // OkHttp客户端实例
    private val client = OkHttpClient()
    
    // 会话级缓存，存储已总结的新闻内容
    // Key: newsId, Value: 总结内容
    private val summaryCache: MutableMap<String, String> = mutableMapOf()
    
    // API配置常量
    private val apiKey = "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"
    private val apiUrl = "https://openrouter.ai/api/v1/chat/completions"
    
    /**
     * 获取新闻总结
     * 支持缓存机制，避免重复API调用
     * 
     * @param newsId 新闻ID，用于缓存管理
     * @param contentToSummarize 待总结的新闻内容
     * @param callback 结果回调，成功返回总结内容，失败返回null
     */
    fun summarizeNews(newsId: String, contentToSummarize: String, callback: (String?) -> Unit) {
        // 检查缓存，如果已有总结则直接返回
        val cachedSummary = getCachedSummary(newsId)
        if (cachedSummary != null) {
            callback(cachedSummary)
            return
        }
        
        // 内容预处理：检查是否为空或过短
        val processedContent = preprocessContent(contentToSummarize)
        if (processedContent.isBlank()) {
            callback(null)
            return
        }
        
        // 构造API请求JSON
        val json = JSONObject().apply {
            put("model", "openai/chatgpt-4o-latest")
            put("temperature", 0.5)
            put("max_tokens", 800)
            put("messages", JSONArray().apply {
                // 设定AI角色
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "你是一个总结新闻的助手。请准确、简洁地总结用户提供的新闻内容。字数不得超过100字")
                })
                // 用户输入的待总结内容
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", processedContent)
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
                            // 缓存成功的总结结果
                            cacheSummary(newsId, trimmedReply)
                            callback(trimmedReply)
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
     * 获取缓存的总结内容
     * 
     * @param newsId 新闻ID
     * @return 缓存的总结内容，如果不存在则返回null
     */
    fun getCachedSummary(newsId: String): String? {
        return summaryCache[newsId]
    }
    
    /**
     * 缓存总结内容
     * 
     * @param newsId 新闻ID
     * @param summary 总结内容
     */
    private fun cacheSummary(newsId: String, summary: String) {
        // 限制缓存大小，防止内存溢出
        if (summaryCache.size >= 50) {
            // 移除最旧的缓存项（简单实现，可优化为LRU）
            val firstKey = summaryCache.keys.firstOrNull()
            if (firstKey != null) {
                summaryCache.remove(firstKey)
            }
        }
        summaryCache[newsId] = summary
    }
    
    /**
     * 清理所有缓存
     * 在Activity销毁或需要释放内存时调用
     */
    fun clearCache() {
        summaryCache.clear()
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
        if (processed.length > 5000) {
            processed = processed.substring(0, 5000) + "..."
        }
        
        return processed
    }
    
    /**
     * 检查是否有缓存的总结
     * 
     * @param newsId 新闻ID
     * @return 是否存在缓存
     */
    fun hasCachedSummary(newsId: String): Boolean {
        return summaryCache.containsKey(newsId)
    }
} 