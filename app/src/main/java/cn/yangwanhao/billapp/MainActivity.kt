package cn.yangwanhao.billapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import cn.yangwanhao.billapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取 NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 🔥 底部导航点击：使用 Navigation 切换顶级 Fragment
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            val targetId = when (menuItem.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_stats -> R.id.statsFragment
                R.id.nav_profile -> R.id.profileFragment
                else -> return@setOnItemSelectedListener false
            }

            // 如果已经在目标页，不重复导航
            if (navController.currentDestination?.id == targetId) {
                return@setOnItemSelectedListener true
            }

            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false, true)
                .build()
            navController.navigate(targetId, null, navOptions)
            true
        }

        // 监听目的地变化：同步底部导航高亮 + 控制显隐
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 同步高亮
            val selectedMenuId = when (destination.id) {
                R.id.homeFragment -> R.id.nav_home
                R.id.statsFragment -> R.id.nav_stats
                R.id.profileFragment -> R.id.nav_profile
                else -> null
            }
            if (selectedMenuId != null && binding.bottomNavView.selectedItemId != selectedMenuId) {
                binding.bottomNavView.selectedItemId = selectedMenuId
            }

            // 进入导入页面时隐藏底部导航栏
            when (destination.id) {
                R.id.importMainFragment, R.id.importExpenseFragment, R.id.importIncomeFragment -> {
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