package cn.yangwanhao.billapp.utils

import cn.yangwanhao.billapp.R

object CategoryIconHelper {

    /**
     * 根据分类名称返回图标资源 ID 和背景色
     * @return Pair(图标资源ID, 颜色值)
     */
    fun getBillListIcon(categoryName: String): Pair<Int, Int> {
        return when {
            categoryName.contains("餐饮") || categoryName.contains("美食") ->
                Pair(R.drawable.ic_bill_list_category_food, 0xFFFF922B.toInt())
            categoryName.contains("交通") || categoryName.contains("出行") ->
                Pair(R.drawable.ic_bill_list_category_transport, 0xFF4DABF7.toInt())
            categoryName.contains("住房") || categoryName.contains("物业") ->
                Pair(R.drawable.ic_bill_list_category_house, 0xFF51CF66.toInt())
            categoryName.contains("生活") || categoryName.contains("日用") ->
                Pair(R.drawable.ic_bill_list_category_daily_goods, 0xFF51CF66.toInt())
            categoryName.contains("休闲") || categoryName.contains("娱乐") ->
                Pair(R.drawable.ic_bill_list_home_category_funny, 0xFFF06595.toInt())
            categoryName.contains("医疗") || categoryName.contains("保健") ->
                Pair(R.drawable.ic_bill_list_category_medical, 0xFFFF6B6B.toInt())
            categoryName.contains("图书") || categoryName.contains("教育") ->
                Pair(R.drawable.ic_bill_list_category_education, 0xFFFFD93D.toInt())
            categoryName.contains("充值") || categoryName.contains("缴费") ->
                Pair(R.drawable.ic_bill_list_category_recharge, 0xFF20C997.toInt())
            categoryName.contains("电子") || categoryName.contains("通讯") ->
                Pair(R.drawable.ic_bill_list_category_phone, 0xFF20C997.toInt())
            categoryName.contains("服饰") || categoryName.contains("美容") ->
                Pair(R.drawable.ic_bill_list_category_fashion_beauty, 0xFFCC5DE8.toInt())
            categoryName.contains("人情") || categoryName.contains("礼金") ->
                Pair(R.drawable.ic_bill_list_category_gift, 0xFFFF922B.toInt())
            categoryName.contains("酒店") || categoryName.contains("旅行") ->
                Pair(R.drawable.ic_bill_list_category_travel, 0xFF20C997.toInt())
            else ->
                Pair(R.drawable.ic_bill_list_category_other, 0xFF868E96.toInt())  // 其他/自定义
        }
    }
}