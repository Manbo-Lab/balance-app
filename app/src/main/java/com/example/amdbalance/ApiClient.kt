package com.example.amdbalance

import org.json.JSONObject

/** AMD /api/v1/usage 接口的解析模型 */
data class DailyUsage(
    val limitUsd: Double,
    val usedUsd: Double,
    val remainingUsd: Double,
    val requests: Long,
    val tokens: Long,
    val resetZone: String,
    val models: List<ModelUsage>
)

data class ModelUsage(
    val model: String,
    val requests: Long,
    val costUsd: Double
)

object ApiClient {
    private const val BASE = "https://developer.amd.com.cn/radeon/api/v1/usage"

    fun fetchUsage(key: String): DailyUsage {
        val conn = java.net.URL(BASE).openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != 200) {
                throw RuntimeException("接口返回 HTTP $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            return parse(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String): DailyUsage {
        val root = JSONObject(text)
        val today = root.optJSONObject("today") ?: root.optJSONObject("all_time") ?: JSONObject()

        val models = mutableListOf<ModelUsage>()
        val arr = root.optJSONArray("by_model")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                models.add(
                    ModelUsage(
                        model = m.optString("model", "未知"),
                        requests = m.optLong("requests", 0),
                        costUsd = m.optDouble("cost", 0.0)
                    )
                )
            }
        }
        models.sortByDescending { it.costUsd }

        return DailyUsage(
            limitUsd = root.optDouble("daily_cost_limit_usd", 5.0),
            usedUsd = root.optDouble("daily_cost_used_usd", 0.0),
            remainingUsd = root.optDouble("daily_cost_remaining_usd", 0.0),
            requests = today.optLong("requests", 0),
            tokens = today.optLong("total_tokens", 0),
            resetZone = root.optString("daily_reset_timezone", "Asia/Shanghai"),
            models = models
        )
    }
}