package com.deeptrade.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 创建整体垂直布局 (暗黑风格)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#121212")) // 极客黑背景
        }

        // 2. 软件标题
        val titleView = TextView(this).apply {
            text = "DeepTrade AI 自动交易系统"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 80)
        }
        mainLayout.addView(titleView)

        // 3. 模拟盘 / 实盘 切换开关 (建议3 集成)
        val modeSwitch = Switch(this).apply {
            text = "当前模式：模拟盘 (资金安全)"
            setTextColor(Color.GREEN)
            textSize = 18f
            isChecked = true // 默认在左侧模拟盘模式
            setPadding(0, 0, 0, 60)
            
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    text = "当前模式：模拟盘 (资金安全)"
                    setTextColor(Color.GREEN)
                } else {
                    text = "当前模式：🚨 实盘交易 (危险)"
                    setTextColor(Color.RED)
                    Toast.makeText(context, "警告：已切入实盘，AI将动用真实资金！", Toast.LENGTH_LONG).show()
                }
            }
        }
        mainLayout.addView(modeSwitch)

        // 4. 系统运行状态文字
        val statusView = TextView(this).apply {
            text = "系统状态：AI 正在监控市场...\n\n当前总资产：$0.00\n今日收益：$0.00\n持仓数量：0只"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 100)
        }
        mainLayout.addView(statusView)

        // 5. 巨大的【一键熔断】按钮 (建议1 集成)
        val killSwitchBtn = Button(this).apply {
            text = "🚨 一键熔断清仓 🚨"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            textSize = 22f
            setPadding(40, 40, 40, 40)
            
            setOnClickListener {
                // 点击后触发熔断报警
                Toast.makeText(context, "已触发熔断！正在强平所有仓位...", Toast.LENGTH_LONG).show()
                statusView.text = "系统状态：🛑 熔断已激活，停止所有买入！"
                statusView.setTextColor(Color.RED)
            }
        }
        mainLayout.addView(killSwitchBtn)

        // 将拼接好的界面显示到屏幕上
        setContentView(mainLayout)
    }
}