package cn.yangwanhao.billapp.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomeViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2  // 支出 + 收入

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ConsumeListFragment()
            1 -> IncomeListFragment()
            else -> ConsumeListFragment()
        }
    }
}