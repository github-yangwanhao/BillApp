package cn.yangwanhao.billapp.ui.imports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.databinding.FragmentImportMainBinding

class ImportMainFragment : Fragment() {

    private var _binding: FragmentImportMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔥 返回按钮
        binding.tvBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 支出导入
        binding.btnExpenseImport.setOnClickListener {
            findNavController().navigate(R.id.importExpenseFragment)
        }

        // 收入导入（暂未开放）
        binding.btnIncomeImport.setOnClickListener {
            android.widget.Toast.makeText(
                requireContext(),
                "收入导入功能即将开放，敬请期待",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}