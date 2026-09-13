package cn.yangwanhao.billapp.ui.home.add

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import cn.yangwanhao.billapp.BillApplication
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.databinding.FragmentIncomeAddBinding
import cn.yangwanhao.billapp.entity.Dict
import cn.yangwanhao.billapp.entity.IncomeBill
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.repository.IncomeBillRepository
import cn.yangwanhao.billapp.ui.dialog.CustomDatePickerDialog
import cn.yangwanhao.billapp.ui.dialog.SingleMonthPickerDialog
import com.google.android.flexbox.FlexboxLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class IncomeAddDialogFragment : DialogFragment() {

    private var _binding: FragmentIncomeAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictRepository: DictRepository
    private lateinit var incomeBillRepository: IncomeBillRepository

    private var categoryList: List<Dict> = emptyList()
    private var remarkList: List<Dict> = emptyList()
    private var selectedCategory: Dict? = null

    // 日期和月份
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDate: Date = calendar.time
    private var selectedMonth: Int = calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)

    private var onSaveSuccess: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.window?.setGravity(Gravity.TOP)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireContext().applicationContext as BillApplication
        dictRepository = app.dictRepository
        incomeBillRepository = app.incomeBillRepository

        initViews()
        loadData()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    private fun initViews() {
        // 初始化金额：默认0，点击清空，失焦恢复
        binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.etAmount.text.toString() == "0") {
                    binding.etAmount.text?.clear()
                }
            } else {
                if (binding.etAmount.text.isNullOrEmpty()) {
                    binding.etAmount.setText("0")
                }
            }
        }

        // 默认日期
        binding.tvDate.text = dateFormat.format(selectedDate)

        // 默认月份
        updateMonthDisplay()
    }

    private fun updateMonthDisplay() {
        val year = selectedMonth / 100
        val month = selectedMonth % 100
        binding.tvMonth.text = "${year}年${month}月"
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Pair(
                        dictRepository.getAllDictValue(Constant.DICT_KEY_INCOME_CATEGORY),
                        dictRepository.getAllDictValue(Constant.DICT_KEY_INCOME_REMARK)
                    )
                }
                categoryList = result.first
                remarkList = result.second

                // 默认选中第一个分类
                if (categoryList.isNotEmpty()) {
                    selectedCategory = categoryList[0]
                    binding.tvCategory.text = categoryList[0].dictValue
                }

                // 加载备注标签
                loadRemarkTags()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "加载数据失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRemarkTags() {
        binding.llRemarkTags.removeAllViews()
        if (remarkList.isEmpty()) return

        // 间距大小（单位 px）
        val spacing = dpToPx(6)

        for (remark in remarkList) {
            val tagView = TextView(requireContext()).apply {
                text = remark.dictValue
                textSize = 18f
                setTextColor(Color.parseColor("#495057"))
                setBackgroundResource(R.drawable.bg_tag_unselected)
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    // 🔥 1. 如果当前标签已经是选中状态，则取消选中并清空输入框
                    if (isSelected) {
                        isSelected = false
                        setBackgroundResource(R.drawable.bg_tag_unselected)
                        setTextColor(Color.parseColor("#495057"))
                        binding.etRemark.setText("")
                        return@setOnClickListener
                    }

                    // 🔥 2. 取消所有标签的选中状态
                    for (i in 0 until binding.llRemarkTags.childCount) {
                        val child = binding.llRemarkTags.getChildAt(i)
                        if (child is TextView) {
                            child.isSelected = false
                            child.setBackgroundResource(R.drawable.bg_tag_unselected)
                            child.setTextColor(Color.parseColor("#495057"))
                        }
                    }

                    // 🔥 3. 选中当前标签
                    isSelected = true
                    setBackgroundResource(R.drawable.bg_tag_selected)
                    setTextColor(Color.WHITE)

                    // 🔥 4. 将标签文本填入备注输入框
                    binding.etRemark.setText(text)
                }
            }

            // 使用 FlexboxLayout.LayoutParams 设置 margin
            val layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = spacing
                marginEnd = spacing
                topMargin = spacing / 2
                bottomMargin = spacing / 2
            }
            tagView.layoutParams = layoutParams

            binding.llRemarkTags.addView(tagView)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupListeners() {
        // 取消和关闭
        binding.tvCancel.setOnClickListener { dismiss() }
        binding.tvClose.setOnClickListener { dismiss() }

        // 确认
        binding.btnConfirm.setOnClickListener { saveIncome() }

        // 分类选择
        binding.llCategory.setOnClickListener {
            showCategoryPicker()
        }

        // 日期选择
        binding.llDate.setOnClickListener {
            showDatePicker()
        }

        // 月份选择
        binding.llMonth.setOnClickListener {
            showMonthPicker()
        }
    }

    private fun showCategoryPicker() {
        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "暂无分类数据", Toast.LENGTH_SHORT).show()
            return
        }
        val names = categoryList.map { it.dictValue }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("选择收入分类")
            .setItems(names) { _, which ->
                selectedCategory = categoryList[which]
                binding.tvCategory.text = categoryList[which].dictValue
            }
            .show()
    }

    private fun showDatePicker() {
        val dialog = CustomDatePickerDialog(
            context = requireContext(),
            maxDate = Calendar.getInstance()
        ) { year, month, day ->
            calendar.set(year, month - 1, day)
            selectedDate = calendar.time
            binding.tvDate.text = dateFormat.format(selectedDate)
        }
        dialog.show()
    }

    private fun showMonthPicker() {
        val now = Calendar.getInstance()
        val maxYearMonth = now.get(Calendar.YEAR) * 100 + (now.get(Calendar.MONTH) + 1)
        val dialog = SingleMonthPickerDialog(
            context = requireContext(),
            maxYearMonth = maxYearMonth
        )
        dialog.setOnConfirmListener { year, month ->
            selectedMonth = year * 100 + month
            updateMonthDisplay()
        }
        dialog.show()
    }

    private fun saveIncome() {
        // 校验金额
        val amountText = binding.etAmount.text.toString().trim()
        if (amountText.isEmpty() || amountText == "0" || amountText == "0.0" || amountText == "0.00") {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }
        val amountYuan = amountText.toDoubleOrNull()
        if (amountYuan == null || amountYuan <= 0) {
            Toast.makeText(requireContext(), "请输入有效金额", Toast.LENGTH_SHORT).show()
            return
        }
        val amountFen = (amountYuan * 100).toInt()

        // 校验分类
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "请选择收入分类", Toast.LENGTH_SHORT).show()
            return
        }

        // 校验备注
        val remark = binding.etRemark.text.toString().trim()
        if (TextUtils.isEmpty(remark)) {
            Toast.makeText(requireContext(), "请输入备注", Toast.LENGTH_SHORT).show()
            return
        }

        // 入账日期（postDate）格式 yyyyMMdd
        val postDateStr = binding.tvDate.text.toString().replace("-", "")
        val postDate = postDateStr.toIntOrNull()
        if (postDate == null || postDate < 20200101) {
            Toast.makeText(requireContext(), "日期无效", Toast.LENGTH_SHORT).show()
            return
        }

        // 所属月份已选
        val billMonth = selectedMonth

        // 构建 IncomeBill
        val now = Date()
        val incomeBill = IncomeBill(
            amount = amountFen,
            categoryId = selectedCategory!!.id.toInt(),
            postDate = postDate,
            billMonth = billMonth,
            remark = remark,
            createTime = now,
            updateTime = now
        )

        lifecycleScope.launch {
            try {
                // 插入数据库
                incomeBillRepository.addBill(incomeBill)  // 需要在 IncomeBillRepository 中实现 addBill 方法（目前没有，我们要添加）
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "收入保存成功", Toast.LENGTH_SHORT).show()
                    onSaveSuccess?.invoke()
                    dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setOnSaveSuccessListener(listener: () -> Unit) {
        onSaveSuccess = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}