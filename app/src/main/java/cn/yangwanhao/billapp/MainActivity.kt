package cn.yangwanhao.billapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import cn.yangwanhao.billapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 开启 edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔥 根布局：只处理 top/left/right，不动 bottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)   // 🔥 bottom 传 0
            insets
        }

        // 🔥 底部导航栏：单独处理 bottom，让背景延伸到底
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, bars.bottom)
            insets
        }

        // 获取 NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 底部导航点击：使用 Navigation 切换顶级 Fragment
        binding.bottomNavView.setOnItemSelectedListener { menuItem ->
            val targetId = when (menuItem.itemId) {
                R.id.nav_home -> R.id.homeFragment
                R.id.nav_stats -> R.id.statsFragment
                R.id.nav_profile -> R.id.profileFragment
                else -> return@setOnItemSelectedListener false
            }

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
            val selectedMenuId = when (destination.id) {
                R.id.homeFragment -> R.id.nav_home
                R.id.statsFragment -> R.id.nav_stats
                R.id.profileFragment -> R.id.nav_profile
                else -> null
            }
            if (selectedMenuId != null && binding.bottomNavView.selectedItemId != selectedMenuId) {
                binding.bottomNavView.selectedItemId = selectedMenuId
            }

            when (destination.id) {
                R.id.importMainFragment, R.id.importExpenseFragment, R.id.importIncomeFragment,
                R.id.exportFragment -> {
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