package com.deeptrade.app.data.model

data class Stock(
    val symbol: String,
    val initialTargetPrice: Double,
    val initialStopLossPrice: Double,
    val aiReason: String? = null // 新增：AI 推荐理由
)