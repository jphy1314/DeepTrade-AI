package com.deeptrade.app

import kotlin.math.floor

// 1. 定义数据结构
data class HoldRecord(
    val symbol: String, 
    val buyPrice: Double, 
    var amount: Double, 
    val initialTargetPrice: Double, 
    val initialStopLossPrice: Double, 
    var highestPriceSinceBuy: Double 
)

// 2. 所需接口
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

// 3. AI 交易引擎核心 (包含熔断、移动止盈、资金分配)
class AutoTradeEngine(
    private val realBroker: BrokerApi,
    private val mockBroker: BrokerApi,
    private val aiAnalyzer: DeepSeekAnalyzer,
    private val notifier: NotificationService
) {
    var isPaperTrading: Boolean = true // 默认开启模拟盘
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

    // 选股与资金均分买入
    suspend fun startTradingCycle(marketData: String) {
        if (isKillSwitchActive) return
        val totalBalance = activeBroker.getAccountBalance()
        val top3Stocks = aiAnalyzer.analyzeAndPickStocks(marketData)
        
        // 留10%现金，剩下资金分配给3只股票
        val allocationPerStock = (totalBalance * 0.9) / 3.0
        
        top3Stocks.forEach { stock ->
            val currentPrice = activeBroker.getCurrentPrice(stock.symbol)
            val affordShares = floor(allocationPerStock / currentPrice)
            if (affordShares > 0 && activeBroker.buyStock(stock.symbol, affordShares, currentPrice)) {
                notifier.sendPushNotification("✅ 买入", "买入 ${stock.symbol}, 数量: $affordShares")
            }
        }
    }

    // 监控与移动止盈
    suspend fun monitorAndSell(holdings: MutableList<HoldRecord>) {
        if (isKillSwitchActive) return
        val iterator = holdings.iterator()
        while (iterator.hasNext()) {
            val hold = iterator.next()
            val currentPrice = activeBroker.getCurrentPrice(hold.symbol)
            
            if (currentPrice > hold.highestPriceSinceBuy) {
                hold.highestPriceSinceBuy = currentPrice
            }

            val fee = activeBroker.getTransactionFee(hold.amount, currentPrice)
            val netProfit = (currentPrice - hold.buyPrice) * hold.amount - fee
            val trailingStopPrice = hold.highestPriceSinceBuy * 0.95 // 最高点回撤5%

            if (currentPrice <= hold.initialStopLossPrice || 
               (hold.highestPriceSinceBuy >= hold.initialTargetPrice && currentPrice <= trailingStopPrice && netProfit > 0)) {
                
                if (activeBroker.sellStock(hold.symbol, hold.amount, currentPrice)) {
                    notifier.sendPushNotification("💰 卖出", "卖出 ${hold.symbol}, 净利润: $netProfit")
                    iterator.remove()
                }
            }
        }
    }
}