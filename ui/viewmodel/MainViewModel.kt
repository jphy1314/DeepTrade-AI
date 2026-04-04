package com.deeptrade.app.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeptrade.app.data.repository.MarketRepository
import com.deeptrade.app.RealDeepSeekAnalyzer
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // 状态驱动 UI：文字、加载状态、警报状态
    val statusText = MutableLiveData<String>("📊 账户状态看板\n系统状态：待机中...")
    val isBusy = MutableLiveData<Boolean>(false)
    val isEmergency = MutableLiveData<Boolean>(false)

    fun executeAiAnalysis(apiKey: String) {
        if (apiKey.isBlank()) {
            statusText.value = "⚠️ 请先配置有效的 API Key！"
            return
        }

        viewModelScope.launch {
            isBusy.value = true
            statusText.value = "🌍 正在抓取实时市场行情..."

            val prompt = MarketRepository.fetchSinaMarketData()
            statusText.value = "⏳ 行情已送达，AI 正在计算深度量化模型..."

            try {
                val analyzer = RealDeepSeekAnalyzer(apiKey)
                val picks = analyzer.analyzeAndPickStocks(prompt)

                if (picks.isNotEmpty()) {
                    val display = StringBuilder("✅ AI 分析完成：\n\n")
                    picks.forEach { stock ->
                        display.append("📌 代码: ${stock.symbol}\n")
                        display.append(" 止盈: $${stock.initialTargetPrice} | 止损: $${stock.initialStopLossPrice}\n\n")
                    }
                    display.append("🤖 资金管理模块已就绪，等待入场信号...")
                    statusText.value = display.toString()
                } else {
                    statusText.value = "❌ AI 未能匹配到符合当前风控标准的标的。"
                }
            } catch (e: Exception) {
                statusText.value = "❌ 运行失败: ${e.localizedMessage}"
            } finally {
                isBusy.value = false
            }
        }
    }

    fun activateKillSwitch() {
        isEmergency.value = true
        statusText.value = "🚨 熔断系统激活！\n[风控中心] 正在强制平仓所有头寸，所有买入指令已拦截。"
    }
}