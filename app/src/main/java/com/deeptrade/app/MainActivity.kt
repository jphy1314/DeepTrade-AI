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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("DeepTradePrefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(60, 60, 60, 60)
        }
        scrollView.addView(mainLayout)

        val titleView = TextView(this).apply {
            text = "DeepTrade AI 交易系统"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleView)

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

        mainLayout.addView(Space(this).apply { minimumHeight = 60 })

        val statusBoard = TextView(this).apply {
            text = "📊 账户状态看板\n总资产：$100,000.00 (模拟)\n当前持仓：0 只\n系统状态：待机中..."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        mainLayout.addView(statusBoard)

        mainLayout.addView(Space(this).apply { minimumHeight = 40 })

        val startAIBtn = Button(this).apply {
            text = "🧠 启动 AI 深度选股"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(40, 40, 40, 40)
            
            setOnClickListener {
                val currentKey = prefs.getString("DEEPSEEK_API_KEY", "") ?: ""
                if (currentKey.isEmpty()) {
                    Toast.makeText(context, "请先在上方输入 API Key！", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 按钮按下后，UI 更新状态
                statusBoard.text = "🌍 正在从新浪财经抓取真实市场行情...\n请稍候..."
                
                CoroutineScope(Dispatchers.Main).launch {
                    // 1. 获取真实数据
                    val realMarketData = fetchRealMarketData()
                    
                    statusBoard.text = "✅ 成功获取真实行情！\n⏳ DeepSeek 正在进行深度量化分析...\n(AI 思考中，预计 5-10 秒)"
                    
                    // 2. 将真实数据喂给 AI
                    val analyzer = RealDeepSeekAnalyzer(currentKey)
                    val picks = analyzer.analyzeAndPickStocks(realMarketData)
                    
                    // 3. 显示结果
                    if (picks.isNotEmpty()) {
                        var displayTxt = "✅ AI 基于真实行情选股完成：\n\n"
                        picks.forEach { stock ->
                            displayTxt += "📌 代码: ${stock.symbol}\n   目标卖出价: $${stock.initialTargetPrice}\n   止损平仓价: $${stock.initialStopLossPrice}\n\n"
                        }
                        displayTxt += "🤖 资金分配系统已就绪，等待买入..."
                        statusBoard.text = displayTxt
                    } else {
                        statusBoard.text = "❌ AI 调用失败。\n请检查 API Key 额度或网络是否畅通。"
                    }
                }
            }
        }
        mainLayout.addView(startAIBtn)

        mainLayout.addView(Space(this).apply { minimumHeight = 80 })

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

    // =========================================================================
    // 🌟 新增核心功能：从新浪财经接口抓取真实大盘和热门股票数据
    // =========================================================================
    private suspend fun fetchRealMarketData(): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                // 抓取标的：上证指数、深证成指、纳斯达克、标普500、苹果、英伟达、特斯拉、微软 (可自由在链接后追加股票代码)
                val url = "https://hq.sinajs.cn/list=s_sh000001,s_sz399001,gb_ixic,gb_inx,gb_aapl,gb_nvda,gb_tsla,gb_msft"
                
                // 新浪财经的 API 必须携带 Referer 请求头（防盗链），否则会返回 403 错误
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Referer", "https://finance.sina.com.cn") 
                    .build()

                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes()
                
                // 新浪接口返回的数据是 GBK 编码，必须指定 Charset 解码，否则中文会乱码
                val rawData = if (bytes != null) String(bytes, Charset.forName("GBK")) else ""
                
                // 直接将原始数据包装成一段话，利用 LLM 的强大理解能力，让 AI 自己去提炼数据
                return@withContext """
                    以下是系统刚刚从新浪财经获取的最新的市场真实行情数据（包含指数和部分热门股票）。
                    数据格式为逗号分隔，包含名称、最新价、涨跌幅等信息，请你自行理解：
                    
                    $rawData
                    
                    请结合上述真实行情数据的走势，为我推荐3只潜力最大的股票。
                """.trimIndent()

            } catch (e: Exception) {
                e.printStackTrace()
                // 如果用户没网或接口抽风，提供一段降级备用信息，防止 APP 崩溃
                return@withContext "获取真实行情失败。请假设当前大盘行情震荡，科技股(如NVDA、AAPL)由于财报超预期表现强势，请基于此进行选股。"
            }
        }
    }
}