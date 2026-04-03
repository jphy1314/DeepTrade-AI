package com.deeptrade.app

import kotlin.math.floor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ================= 1. 数据结构 =================
data class HoldRecord(
    val symbol: String, 
    var buyPrice: Double, 
    var amount: Double, 
    val initialTargetPrice: Double, 
    val initialStopLossPrice: Double, 
    var highestPriceSinceBuy: Double 
)

// ================= 2. 接口定义 =================
interface BrokerApi {
    suspend fun getAccountBalance(): Double
    suspend fun buyStock(symbol: String, amount: Double, price: Double): Boolean
    suspend fun sellStock(symbol: String, amount: Double, price: Double): Boolean
    suspend fun getTransactionFee(amount: Double, price: Double): Double
    suspend fun getCurrentPrice(symbol: String): Double
}

interface DeepSeekAnalyzer {
    suspend fun analyzeAndPickStocks(marketData: String): List<HoldRecord>
}

interface NotificationService {
    fun sendPushNotification(title: String, message: String)
}

// ================= 3. 真实的 DeepSeek AI 调用类 =================
class RealDeepSeekAnalyzer(private val apiKey: String) : DeepSeekAnalyzer {
    
    private val client = OkHttpClient()

    override suspend fun analyzeAndPickStocks(marketData: String): List<HoldRecord> {
        return withContext(Dispatchers.IO) { // 放在后台线程执行网络请求
            try {
                // 1. 构建给 AI 的提示词 (强制要求它只返回干净的 JSON)
                val prompt = """
                    你是一个顶级的量化金融分析师。请分析以下市场数据，挑选最具有上涨潜力的3只股票。
                    【严格警告】你必须只返回一个 JSON 数组，不要包含任何多余的文字、不要Markdown代码块！
                    格式范例：[{"symbol":"AAPL","targetPrice":150.0,"stopLossPrice":140.0}]
                    市场数据：$marketData
                """.trimIndent()

                // 2. 构造 DeepSeek 官方要求的请求体
                val jsonBody = JSONObject().apply {
                    put("model", "deepseek-chat")
                    val message = JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                    put("messages", JSONArray().put(message))
                    put("temperature", 0.1) // 温度设低，让 AI 更加冷静和严谨
                }

                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())

                // 3. 构建 HTTP 请求
                val request = Request.Builder()
                    .url("https://api.deepseek.com/chat/completions")
                    .post(body)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()

                // 4. 发送请求
                val response = client.newCall(request).execute()
                val responseStr = response.body?.string() ?: ""
                
                // 5. 剥离 JSON，提取 AI 核心回答
                val responseJson = JSONObject(responseStr)
                val aiContent = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                // 清理 AI 有时会带上的 Markdown 标记 (如 ```json ... ```)
                val cleanJsonStr = aiContent.replace("```json", "").replace("```", "").trim()
                
                // 6. 解析成我们的 HoldRecord 列表
                val jsonArray = JSONArray(cleanJsonStr)
                val results = mutableListOf<HoldRecord>()
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    results.add(
                        HoldRecord(
                            symbol = item.getString("symbol"),
                            buyPrice = 0.0, // 初始为0，稍后券商接口会更新真实买入价
                            amount = 0.0,   // 初始为0，稍后券商接口会更新真实数量
                            initialTargetPrice = item.getDouble("targetPrice"),
                            initialStopLossPrice = item.getDouble("stopLossPrice"),
                            highestPriceSinceBuy = 0.0
                        )
                    )
                }
                
                println("AI 成功选出 3 只股票: $results")
                results // 成功返回结果
                
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果 API 请求失败或 JSON 解析失败，返回空列表，保护资金安全
                emptyList() 
            }
        }
    }
}

// ================= 4. 全自动交易引擎核心 =================
class AutoTradeEngine(
    private val realBroker: BrokerApi,
    private val mockBroker: BrokerApi,
    private val aiAnalyzer: DeepSeekAnalyzer,
    private val notifier: NotificationService
) {
    var isPaperTrading: Boolean = true // 默认模拟盘
    var isKillSwitchActive: Boolean = false // 熔断开关

    private val activeBroker: BrokerApi
        get() = if (isPaperTrading) mockBroker else realBroker

    // 一键熔断清仓
    suspend fun triggerKillSwitch(holdings: MutableList<HoldRecord>) {
        isKillSwitchActive = true
        notifier.sendPushNotification("⚠️ 警告", "触发一键熔断！正尝试清仓...")
        val iterator = holdings.iterator()
        while (iterator.hasNext()) {
            val hold = iterator.next()
            val currentPrice = activeBroker.getCurrentPrice(hold.symbol)
            if (activeBroker.sellStock(hold.symbol, hold.amount, currentPrice)) {
                iterator.remove()
            }
        }
    }

    // AI 选股并资金均分买入
    suspend fun startTradingCycle(marketData: String) {
        if (isKillSwitchActive) return
        
        val totalBalance = activeBroker.getAccountBalance()
        
        // ============= 调用上方写好的 API 代码 =============
        val top3Stocks = aiAnalyzer.analyzeAndPickStocks(marketData)
        
        if (top3Stocks.isEmpty()) {
            println("AI 未能输出有效股票或调用失败，本轮放弃交易。")
            return
        }

        // 资金不放一个篮子：留10%备用金，剩下资金等分3份
        val allocationPerStock = (totalBalance * 0.9) / 3.0
        
        top3Stocks.forEach { stock ->
            val currentPrice = activeBroker.getCurrentPrice(stock.symbol)
            val affordShares = floor(allocationPerStock / currentPrice)
            
            if (affordShares > 0 && activeBroker.buyStock(stock.symbol, affordShares, currentPrice)) {
                // 记录买入成本，以供之后监控和止盈
                stock.buyPrice = currentPrice
                stock.amount = affordShares
                stock.highestPriceSinceBuy = currentPrice
                
                notifier.sendPushNotification("✅ 买入建仓", "股票代码: ${stock.symbol}, 数量: $affordShares")
            }
        }
    }

    // 监控持仓与移动止盈
    suspend fun monitorAndSell(holdings: MutableList<HoldRecord>) {
        if (isKillSwitchActive) return
        
        val iterator = holdings.iterator()
        while (iterator.hasNext()) {
            val hold = iterator.next()
            val currentPrice = activeBroker.getCurrentPrice(hold.symbol)
            
            // 记录买入后的历史最高价 (用于移动止盈)
            if (currentPrice > hold.highestPriceSinceBuy) {
                hold.highestPriceSinceBuy = currentPrice
            }

            // 计算包含手续费在内的净利润
            val fee = activeBroker.getTransactionFee(hold.amount, currentPrice)
            val netProfit = (currentPrice - hold.buyPrice) * hold.amount - fee
            
            // 移动止盈线：历史最高点回撤 5%
            val trailingStopPrice = hold.highestPriceSinceBuy * 0.95 

            // 卖出条件判断
            val isHardStopLoss = currentPrice <= hold.initialStopLossPrice
            val isTrailingStopWin = (hold.highestPriceSinceBuy >= hold.initialTargetPrice) && 
                                    (currentPrice <= trailingStopPrice) && 
                                    (netProfit > 0) // 前提：扣除手续费必须有收益

            if (isHardStopLoss || isTrailingStopWin) {
                if (activeBroker.sellStock(hold.symbol, hold.amount, currentPrice)) {
                    val reason = if (isHardStopLoss) "跌破止损线" else "触发移动止盈"
                    notifier.sendPushNotification("💰 自动卖出", "代码: ${hold.symbol}, 原因: $reason, 净利润: $netProfit")
                    iterator.remove() // 成功卖出后从持仓中移除
                }
            }
        }
    }
}