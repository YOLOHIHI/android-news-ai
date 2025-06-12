package com.example.myapplication1

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * 网络工具类
 * 提供网络状态检查功能，优化AI功能的用户体验
 */
object NetworkUtils {
    
    /**
     * 检查设备是否连接到网络
     * 兼容不同Android版本的网络检查方式
     * 
     * @param context 上下文对象
     * @return 是否有网络连接
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 及以上版本的检查方式
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Android 6.0 以下版本的检查方式
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * 获取网络类型描述
     * 用于显示更详细的网络状态信息
     * 
     * @param context 上下文对象
     * @return 网络类型描述字符串
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            
            when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动网络"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
                else -> "未知网络"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "移动网络"
                ConnectivityManager.TYPE_ETHERNET -> "以太网"
                else -> "未知网络"
            }
        }
    }
    
    /**
     * 检查是否为计费网络（移动数据）
     * 可用于提醒用户AI功能可能产生流量费用
     * 
     * @param context 上下文对象
     * @return 是否为计费网络
     */
    fun isMeteredNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.isActiveNetworkMetered
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }
    
    /**
     * 获取详细的网络状态信息
     * 用于诊断网络连接问题
     * 
     * @param context 上下文对象
     * @return 网络状态描述
     */
    fun getNetworkStatusDetail(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                "无网络连接"
            } else {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                when {
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi已连接"
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动网络已连接"
                    networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网已连接"
                    else -> "网络连接异常"
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo?.isConnected == true) {
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi已连接"
                    ConnectivityManager.TYPE_MOBILE -> "移动网络已连接"
                    ConnectivityManager.TYPE_ETHERNET -> "以太网已连接"
                    else -> "网络已连接"
                }
            } else {
                "无网络连接"
            }
        }
    }
    
    /**
     * 检查是否有WiFi连接
     * 优先推荐用户使用WiFi进行AI功能
     * 
     * @param context 上下文对象
     * @return 是否连接到WiFi
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }
} 