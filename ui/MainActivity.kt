package com.deeptrade.app

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.deeptrade.app.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    // 利用官方扩展库自动关联 ViewModel
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("DeepTradePrefs", Context.MODE_PRIVATE)

        // --- UI 构建 (保持你的极简黑色风格) ---
        val root = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#121212")) }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(60, 60, 60, 60)
        }
        root.addView(layout)

        // 组件定义
        val title = TextView(this).apply {
            text = "DeepTrade AI 交易系统"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        }
        layout.addView(title)

        val apiKeyInput = EditText(this).apply {
            hint = "粘贴 DeepSeek API Key"
            setHintTextColor(Color.DKGRAY)
            setTextColor(Color.YELLOW)
            setText(prefs.getString("DEEPSEEK_API_KEY", ""))
        }
        layout.addView(apiKeyInput)

        val saveBtn = Button(this).apply {
            text = "💾 保存密钥"
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
        }
        layout.addView(saveBtn)

        val statusBoard = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        layout.addView(statusBoard)

        val startBtn = Button(this).apply {
            text = "🧠 启动 AI 深度选股"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
        }
        layout.addView(startBtn)

        val killBtn = Button(this).apply {
            text = "🚨 一键熔断清仓 🚨"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
        layout.addView(killBtn)

        setContentView(root)

        // --- 核心逻辑绑定 (Data Binding) ---

        // 1. 观察文本状态
        viewModel.statusText.observe(this) { text -> statusBoard.text = text }

        // 2. 观察分析状态（UI 交互反馈）
        viewModel.isBusy.observe(this) { busy ->
            startBtn.isEnabled = !busy
            startBtn.text = if (busy) "⏳ AI 分析中..." else "🧠 启动 AI 深度选股"
        }

        // 3. 观察紧急状态
        viewModel.isEmergency.observe(this) { emergency ->
            if (emergency) statusBoard.setTextColor(Color.RED)
        }

        // --- 事件分发 ---
        saveBtn.setOnClickListener {
            val key = apiKeyInput.text.toString()
            prefs.edit().putString("DEEPSEEK_API_KEY", key).apply()
            Toast.makeText(this, "API Key 已加密持久化", Toast.LENGTH_SHORT).show()
        }

        startBtn.setOnClickListener {
            val currentKey = prefs.getString("DEEPSEEK_API_KEY", "") ?: ""
            viewModel.executeAiAnalysis(currentKey)
        }

        killBtn.setOnClickListener {
            viewModel.activateKillSwitch()
        }
    }
}