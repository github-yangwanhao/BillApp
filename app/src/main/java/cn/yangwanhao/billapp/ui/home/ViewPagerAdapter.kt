package cn.yangwanhao.billapp.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // 统一使用 BillListAdapter
    val consumeAdapter = cn.yangwanhao.billapp.ui.adapter.BillListAdapter()
    val incomeAdapter = cn.yangwanhao.billapp.ui.adapter.BillListAdapter()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            ConsumeListFragment.newInstance(consumeAdapter)
        } else {
            IncomeListFragment.newInstance(incomeAdapter)
        }
    }
}