package cn.yangwanhao.billapp.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.yangwanhao.billapp.ui.profile.ProfileFragment
import cn.yangwanhao.billapp.ui.statistics.StatsFragment

class HomeViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                // 这里需要传入 Adapter，根据你的实际逻辑调整
                // 如果 ConsumeListFragment 需要 Adapter，用 newInstance
                // 如果不需要，直接用构造函数
                ConsumeListFragment()
            }
            1 -> StatsFragment()
            2 -> ProfileFragment()
            else -> ConsumeListFragment()
        }
    }
}