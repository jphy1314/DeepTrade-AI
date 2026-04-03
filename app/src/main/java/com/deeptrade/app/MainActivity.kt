package com.deeptrade.app

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 获取本地保存的配置
        val prefs = getSharedPreferences("DeepTradePrefs", Context.MODE_PRIVATE)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#121212")) 
        }

        val titleView = TextView(this).apply {
            text = "DeepTrade AI 交易系统"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleView)

        // ================= 新增：API Key 输入框 =================
        val apiKeyInput = EditText(this).apply {
            hint = "请粘贴真实的 DeepSeek API Key"
            setHintTextColor(Color.DKGRAY)
            setTextColor(Color.YELLOW)
            textSize = 14f
            // 读取上次保存的 Key
            setText(prefs.getString("DEEPSEEK_API_KEY", "")) 
            setPadding(0, 0, 0, 20)
        }
        mainLayout.addView(apiKeyInput)

        // ================= 新增：保存按钮 =================
        val saveBtn = Button(this).apply {
            text = "💾 保存 API Key 到本地"
            setBackgroundColor(Color.parseColor("#333333"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val inputKey = apiKeyInput.text.toString()
                if (inputKey.isNotEmpty()) {
                    // 保存到手机本地，不会泄露到 GitHub
                    prefs.edit().putString("DEEPSEEK_API_KEY", inputKey).apply()
                    Toast.makeText(context, "API Key 保存成功！", Toast.LENGTH_SHORT).show()
                }
            }
        }
        mainLayout.addView(saveBtn)
        // =========================================================

        val space = Space(this).apply { minimumHeight = 80 }
        mainLayout.addView(space)

        val modeSwitch = Switch(this).apply {
            text = "当前模式：模拟盘 (资金安全)"
            setTextColor(Color.GREEN)
            textSize = 18f
            isChecked = true 
            setPadding(0, 0, 0, 60)
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

        setContentView(mainLayout)
    }
}