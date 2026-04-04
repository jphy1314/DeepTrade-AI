package com.deeptrade.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.deeptrade.app.ui.viewmodel.MainViewModel

/**
 * 视图层 (View)：只负责 UI 构建、用户交互事件转发、数据观察
 * 不包含任何网络请求、AI逻辑或协程手动管理
 */
class MainActivity : AppCompatActivity() {

    // 使用官方扩展库自动获取并管理 ViewModel 生命周期
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("DeepTradePrefs", Context.MODE_PRIVATE)

        // --- 1. UI 布局构建 (保持你原有的深色极简风格) ---
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            isFillViewport = true
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
            // 初始化时从本地缓存加载 Key
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
                    Toast.makeText(this@MainActivity, "API Key 已本地加密保存", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mainLayout.addView(saveBtn)

        mainLayout.addView(Space(this).apply { minimumHeight = 60 })

        // 状态看板：内容完全由 ViewModel 驱动
        val statusBoard = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            lineSpacingMultiplier = 1.2f
        }
        mainLayout.addView(statusBoard)

        mainLayout.addView(Space(this).apply { minimumHeight = 40 })

        val startAIBtn = Button(this).apply {
            text = "🧠 启动 AI 深度选股"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(40, 40, 40, 40)
        }
        mainLayout.addView(startAIBtn)

        val modeSwitch = Switch(this).apply {
            text = "当前模式：模拟盘 (资金安全)"
            setTextColor(Color.GREEN)
            textSize = 18f
            isChecked = true 
            setPadding(0, 40, 0, 40)
        }
        mainLayout.addView(modeSwitch)

        val killSwitchBtn = Button(this).apply {
            text = "🚨 一键熔断清仓 🚨"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            textSize = 22f
            setPadding(40, 40, 40, 40)
        }
        mainLayout.addView(killSwitchBtn)

        setContentView(scrollView)

        // --- 2. 核心：数据观察者 (Observe Logic) ---
        
        // 监听看板文字变化：当 ViewModel 里的数据更新，UI 自动刷新
        viewModel.statusText.observe(this) { newStatus ->
            statusBoard.text = newStatus
        }

        // 监听分析状态：分析中禁用按钮，防止重复点击，并更新 UI 反馈
        viewModel.isBusy.observe(this) { isAnalyzing ->
            startAIBtn.isEnabled = !isAnalyzing
            startAIBtn.alpha = if (isAnalyzing) 0.5f else 1.0f
            startAIBtn.text = if (isAnalyzing) "⏳ AI 正在深度思考..." else "🧠 启动 AI 深度选股"
        }

        // --- 3. 核心：用户事件转发 (UI Events) ---

        startAIBtn.setOnClickListener {
            val currentKey = prefs.getString("DEEPSEEK_API_KEY", "") ?: ""
            if (currentKey.isEmpty()) {
                Toast.makeText(this, "请先保存 API Key！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 仅仅通知 ViewModel 开始工作，Activity 不管它是怎么工作的
            viewModel.runAnalysis(currentKey)
        }

        killSwitchBtn.setOnClickListener {
            // 调用 ViewModel 的熔断逻辑
            viewModel.activateKillSwitch()
            statusBoard.setTextColor(Color.RED)
        }
    }
}