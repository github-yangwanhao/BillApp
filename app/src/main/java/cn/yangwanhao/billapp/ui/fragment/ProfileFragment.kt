package cn.yangwanhao.billapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cn.yangwanhao.billapp.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置版本号
        val tvVersion: TextView = view.findViewById(R.id.tv_version)
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            tvVersion.text = "版本 ${packageInfo.versionName}"
        } catch (e: Exception) {
            tvVersion.text = "版本 1.0.0"
        }

        // 🔥 文件导入入口
        val btnFileImport: View? = view.findViewById(R.id.btn_file_import)
        btnFileImport?.setOnClickListener {
            // 使用 Navigation 跳转到导入主页面
            findNavController().navigate(R.id.importMainFragment)
        }
    }
}