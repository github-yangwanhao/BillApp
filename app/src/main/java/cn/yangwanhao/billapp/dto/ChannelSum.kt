package cn.yangwanhao.billapp.dto

/**
 * 支付方式聚合结果
 */
data class ChannelSum(
    val channelId: Int,
    val channelName: String,    // 由 Repository 填充
    val total: Int
)