package cn.yangwanhao.billapp.base

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(updateLanguage(newBase!!))
    }

    private fun updateLanguage(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.CHINA)
        return context.createConfigurationContext(config)
    }
}