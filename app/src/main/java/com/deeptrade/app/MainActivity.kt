package com.deeptrade.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("DeepTradePrefs", Context.MODE_PRIVATE)

        // 1. 最外层加上 ScrollView，防止内容太多屏幕放不下
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(60, 60, 60, 60)
        }
        scrollView.addView(mainLayout)

        // 2. 标题
        val titleView = TextView(this).apply {
            text = "DeepTrade AI 交易系统"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleView)

        // 3. API Key 设置区
        val apiKeyInput = EditText(this).apply {
            hint = "请粘贴真实的 DeepSeek API Key"
            setHintTextColor(Color.DKGRAY)
            setTextColor(Color.YELLOW)
            textSize = 14f
            setText(prefs.getString("DEEPSEEK_API_KEY", "")) 
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(apiKeyInput)

        val saveBtn = Button(this).apply {
            text = "💾 保存 API Key"
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val inputKey = apiKeyInput.text.toString()
                if (inputKey.isNotEmpty()) {
                    prefs.edit().putString("DEEPSEEK_API_KEY", inputKey).apply()
                    Toast.makeText(context, "API Key 保存成功！", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mainLayout.addView(saveBtn)

        // 分割线
        mainLayout.addView(Space(this).apply { minimumHeight = 60 })

        // ================= 新增：股票信息展示看板 =================
        val statusBoard = TextView(this).apply {
            text = "📊 账户状态看板\n总资产：$100,000.00 (模拟)\n当前持仓：0 只\n系统状态：待机中..."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        mainLayout.addView(statusBoard)

        mainLayout.addView(Space(this).apply { minimumHeight = 40 })

        // ================= 新增：启动 AI 分析的“点火”按钮 =================
        val startAIBtn = Button(this).apply {
            text = "🧠 启动 AI 深度选股"
            setBackgroundColor(Color.parseColor("#4CAF50")) // 绿色按钮
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(40, 40, 40, 40)
            
            setOnClickListener {
                val currentKey = prefs.getString("DEEPSEEK_API_KEY", "") ?: ""
                if (currentKey.isEmpty()) {
                    Toast.makeText(context, "请先在上方输入 API Key！", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                statusBoard.text = "⏳ DeepSeek 正在深度分析市场数据...\n请稍候..."
                
                // 启动后台协程，调用真正的 AI 接口
                CoroutineScope(Dispatchers.Main).launch {
                    val analyzer = RealDeepSeekAnalyzer(currentKey)
                    
                    // 模拟喂给 AI 的当天大盘数据（后续可替换为抓取新浪财经或 Yahoo 的真实数据）
                    val mockMarketData = "今日大盘普涨。苹果(AAPL)发布全新AI手机，订单超预期；英伟达(NVDA)新一代芯片供不应求，财报指引极佳；特斯拉(TSLA)中国区销量大增；微软(MSFT)云业务增长放缓。"
                    
                    // 等待 AI 返回选股结果
                    val picks = analyzer.analyzeAndPickStocks(mockMarketData)
                    
                    if (picks.isNotEmpty()) {
                        var displayTxt = "✅ AI 选股完成！建议建仓：\n\n"
                        picks.forEach { stock ->
                            displayTxt += "📌 代码: ${stock.symbol}\n   目标卖出价: $${stock.initialTargetPrice}\n   止损平仓价: $${stock.initialStopLossPrice}\n\n"
                        }
                        displayTxt += "🤖 资金分配系统已就绪，等待买入..."
                        statusBoard.text = displayTxt
                    } else {
                        statusBoard.text = "❌ AI 调用失败。\n可能原因：API Key 错误、网络不通，或 AI 返回的不是标准 JSON。"
                    }
                }
            }
        }
        mainLayout.addView(startAIBtn)

        mainLayout.addView(Space(this).apply { minimumHeight = 80 })

        // 4. 模式切换与熔断 (保持不变)
        val modeSwitch = Switch(this).apply {
            text = "当前模式：模拟盘 (资金安全)"
            setTextColor(Color.GREEN)
            textSize = 18f
            isChecked = true 
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(modeSwitch)

        val killSwitchBtn = Button(this).apply {
            text = "🚨 一键熔断清仓 🚨"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            textSize = 22f
            setPadding(40, 40, 40, 40)
            setOnClickListener {
                statusBoard.text = "🛑 熔断已激活！\n所有 AI 买入已停止，正在市价强平所有仓位！"
                statusBoard.setTextColor(Color.RED)
            }
        }
        mainLayout.addView(killSwitchBtn)

        setContentView(scrollView)
    }
}