package cn.yangwanhao.billapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import cn.yangwanhao.billapp.databinding.ActivityMainBinding
import cn.yangwanhao.billapp.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // 持有 HomeFragment 引用，用于切换 ViewPager2
    var homeFragment: HomeFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取 NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 🔥 底部导航栏点击事件：控制 ViewPager2 切换
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    homeFragment?.setCurrentTab(0)
                    true
                }
                R.id.nav_stats -> {
                    homeFragment?.setCurrentTab(1)
                    true
                }
                R.id.nav_profile -> {
                    homeFragment?.setCurrentTab(2)
                    true
                }
                else -> false
            }
        }

        // 监听目的地变化：进入导入页面时隐藏底部导航栏
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.importMainFragment, R.id.importExpenseFragment -> {
                    binding.bottomNavView.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}