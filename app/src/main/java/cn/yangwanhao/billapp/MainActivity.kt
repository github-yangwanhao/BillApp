package cn.yangwanhao.billapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import cn.yangwanhao.billapp.ui.fragment.StatsFragment
import cn.yangwanhao.billapp.ui.fragment.ProfileFragment
import cn.yangwanhao.billapp.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    // 三个 Fragment 实例
    private val homeFragment = HomeFragment()
    private val statsFragment = StatsFragment()
    private val profileFragment = ProfileFragment()

    // 当前活跃的 Fragment
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // 初始化：先添加三个 Fragment，只显示首页
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
            .add(R.id.fragment_container, statsFragment, "stats").hide(statsFragment)
            .add(R.id.fragment_container, homeFragment, "home")
            .commit()

        // 底部导航切换监听
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_stats -> {
                    switchFragment(statsFragment)
                    true
                }
                R.id.nav_profile -> {
                    switchFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    /** 切换 Fragment */
    private fun switchFragment(target: Fragment) {
        if (target != activeFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit()
            activeFragment = target
        }
    }
}