package cn.yangwanhao.billapp.ui.statistics

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import cn.yangwanhao.billapp.databinding.FragmentCategoryDrillSheetBinding
import cn.yangwanhao.billapp.dto.DrillItem
import cn.yangwanhao.billapp.ui.adapter.DrillListAdapter
import cn.yangwanhao.billapp.utils.CategoryIconHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategoryDrillSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_CATEGORY_ID = "categoryId"
        private const val ARG_CATEGORY_NAME = "categoryName"
        private const val ARG_IS_INCOME = "isIncome"
        private const val ARG_PERIOD_TEXT = "periodText"
        private const val ARG_START = "start"
        private const val ARG_END = "end"

        fun newInstance(
            categoryId: Int,
            categoryName: String,
            isIncome: Boolean,
            periodText: String,
            start: Int,
            end: Int
        ): CategoryDrillSheetFragment {
            val f = CategoryDrillSheetFragment()
            f.arguments = Bundle().apply {
                putInt(ARG_CATEGORY_ID, categoryId)
                putString(ARG_CATEGORY_NAME, categoryName)
                putBoolean(ARG_IS_INCOME, isIncome)
                putString(ARG_PERIOD_TEXT, periodText)
                putInt(ARG_START, start)
                putInt(ARG_END, end)
            }
            return f
        }
    }

    private var _binding: FragmentCategoryDrillSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DrillSheetViewModel by viewModels()
    private val adapter = DrillListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryDrillSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val categoryId = args.getInt(ARG_CATEGORY_ID)
        val categoryName = args.getString(ARG_CATEGORY_NAME) ?: ""
        val isIncome = args.getBoolean(ARG_IS_INCOME)
        val periodText = args.getString(ARG_PERIOD_TEXT) ?: ""
        val start = args.getInt(ARG_START)
        val end = args.getInt(ARG_END)

        // 头部图标
        val iconData = if (isIncome) {
            CategoryIconHelper.getIncomeIcon(categoryName)
        } else {
            CategoryIconHelper.getBillListIcon(categoryName)
        }
        binding.ivSheetIcon.setImageResource(iconData.first)
        (binding.ivSheetIcon.background as? GradientDrawable)?.setColor(iconData.second)

        binding.tvSheetName.text = categoryName

        // 列表
        binding.rvDrillList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDrillList.adapter = adapter

        // 关闭
        binding.btnSheetClose.setOnClickListener { dismiss() }

        // 数据
        viewModel.loadDrillList(categoryId, start, end, isIncome)
        viewModel.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            renderMetaAndTotal(periodText, list)
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun renderMetaAndTotal(periodText: String, list: List<DrillItem>) {
        binding.tvSheetMeta.text = "$periodText · 共 ${list.size} 笔"
        val total = list.sumOf { it.amount }
        binding.tvSheetTotal.text = "¥${String.format("%,.2f", total / 100.0)}"
    }

    override fun onStart() {
        super.onStart()
        // 让 BottomSheet 默认展开到 70% 高度
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        dialog?.behavior?.apply {
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
            maxHeight = (resources.displayMetrics.heightPixels * 0.7).toInt()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}