package cn.yangwanhao.billapp.ui.add

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cn.yangwanhao.billapp.BillApplication
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.databinding.FragmentAddExpenseBinding
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.entity.Dict
import cn.yangwanhao.billapp.entity.InstallmentBill
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.ui.adapter.InstallmentPreviewAdapter
import cn.yangwanhao.billapp.utils.InstallmentCalculator
import cn.yangwanhao.billapp.utils.InstallmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    // Repository
    private lateinit var dictRepository: DictRepository
    private lateinit var consumeBillRepository: ConsumeBillRepository

    // 数据
    private var categoryList: List<Dict> = emptyList()
    private var channelList: List<Dict> = emptyList()
    private var remarkList: List<Dict> = emptyList()
    private var selectedCategory: Dict? = null
    private var selectedChannel: Dict? = null

    // 日期
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())

    // 分期相关
    private val installmentPreviewAdapter = InstallmentPreviewAdapter { bill, position ->
        showEditRemarkDialog(bill, position)
    }
    private var currentInstallmentResult: InstallmentResult? = null

    // 保存成功回调
    private var onSaveSuccess: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.Theme_BillApp

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireContext().applicationContext as BillApplication
        dictRepository = app.dictRepository
        consumeBillRepository = app.consumeBillRepository

        // 初始化控件
        initViews()
        // 加载数据
        loadDataFromDatabase()
        // 设置监听器
        setupListeners()
        // 初始化分期预览
        setupInstallmentPreview()
        // 默认显示普通模式
        switchToNormalMode()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                // 最大高度为屏幕高度的 80%
                val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
                behavior.peekHeight = maxHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                it.layoutParams.height = maxHeight
                it.requestLayout()
            }
        }
        return dialog
    }

    // ========== 初始化控件 ==========
    private fun initViews() {
        // 🔥 默认选中常规模式
        selectNormalMode()
        // 默认日期
        binding.etDate.setText(dateFormat.format(calendar.time))
        // 默认开始月份
        binding.etStartMonth.setText(monthFormat.format(calendar.time))
        // 🔥 金额焦点处理（清除默认0，失焦恢复0）
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

        // 🔥 备注焦点处理（清除默认占位，失焦恢复占位）
        binding.etRemark.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.etRemark.text.toString() == "请输入或选择备注") {
                    binding.etRemark.text?.clear()
                }
            } else {
                if (binding.etRemark.text.isNullOrEmpty()) {
                    binding.etRemark.setText("请输入或选择备注")
                }
            }
        }
    }

    // ========== 加载数据 ==========
    private fun loadDataFromDatabase() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    Triple(
                        dictRepository.getAllDictValue(Constant.DICT_KEY_CONSUME_CATEGORY),
                        dictRepository.getAllDictValue(Constant.DICT_KEY_PAY_CHANNEL),
                        dictRepository.getAllDictValue(Constant.DICT_KEY_CONSUME_REMARK)
                    )
                }
                categoryList = result.first
                channelList = result.second
                remarkList = result.third

                if (categoryList.isNotEmpty()) {
                    selectedCategory = categoryList[0]
                    binding.etCategory.setText(categoryList[0].dictValue)
                }
                if (channelList.isNotEmpty()) {
                    selectedChannel = channelList[0]
                    binding.etPayChannel.setText(channelList[0].dictValue)
                }
                loadRemarkTags()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "加载数据失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ========== 设置监听器 ==========
    private fun setupListeners() {
        // 取消
        binding.tvCancel.setOnClickListener { dismiss() }

        // 保存
        binding.tvSave.setOnClickListener { saveBill() }

        // 日期选择
        binding.etDate.setOnClickListener { showDatePicker() }

        // 分类选择
        binding.etCategory.setOnClickListener { showCategoryPicker() }

        // 支付方式选择
        binding.etPayChannel.setOnClickListener { showPayChannelPicker() }

        // 开始月份选择
        binding.etStartMonth.setOnClickListener { showMonthPicker() }

        // 分期参数发生变化
        binding.etInstallmentCount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // 获得焦点：如果是 "0" 则清空
                if (binding.etInstallmentCount.text.toString() == "0") {
                    binding.etInstallmentCount.text?.clear()
                }
            } else {
                val text = binding.etInstallmentCount.text.toString()
                when {
                    text.isNullOrEmpty() -> {
                        // 失焦且为空 → 恢复 "0"
                        binding.etInstallmentCount.setText("0")
                    }
                    text == "0" -> {
                        // 失焦且内容为 "0" → 不更新预览（因为0期无效）
                        // 但保持显示 "0"，不做额外操作
                    }
                    else -> {
                        // 失焦且内容有效 → 更新预览
                        updateInstallmentPreview()
                    }
                }
            }
        }
        binding.etStartMonth.setOnFocusChangeListener { _, _ ->
            updateInstallmentPreview()
        }

        // 🔥 常规/分期按钮点击（手动控制选中状态）
        binding.btnNormalMode.setOnClickListener {
            selectNormalMode()
        }

        binding.btnInstallmentMode.setOnClickListener {
            selectInstallmentMode()
        }
    }

    // ========== 分期预览 ==========
    private fun setupInstallmentPreview() {
        binding.rvInstallmentPreview.layoutManager = LinearLayoutManager(context)
        binding.rvInstallmentPreview.adapter = installmentPreviewAdapter
    }

    private fun updateInstallmentPreview() {
        if (binding.btnInstallmentMode.isSelected) {
            try {
                val amountText = binding.etAmount.text.toString().trim()
                val totalAmount = (amountText.toDoubleOrNull() ?: 0.0) * 100
                if (totalAmount <= 0) {
                    binding.rvInstallmentPreview.visibility = View.GONE
                    return
                }

                val count = binding.etInstallmentCount.text.toString().toIntOrNull() ?: 3
                if (count <= 0) {
                    Toast.makeText(requireContext(), "期数必须大于0", Toast.LENGTH_SHORT).show()
                    return
                }

                val startMonthStr = binding.etStartMonth.text.toString().trim()
                if (startMonthStr.isEmpty()) {
                    return
                }
                val startMonth = startMonthStr.toIntOrNull() ?: return

                val baseRemark = binding.etRemark.text.toString().trim()

                val result = InstallmentCalculator.calculate(
                    totalAmount = totalAmount.toInt(),
                    count = count,
                    startMonth = startMonth,
                    baseRemark = baseRemark
                )
                currentInstallmentResult = result
                installmentPreviewAdapter.submitList(result.bills)
                binding.rvInstallmentPreview.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
                binding.rvInstallmentPreview.visibility = View.GONE
            }
        }
    }

    private fun showEditRemarkDialog(bill: InstallmentBill, position: Int) {
        val editText = android.widget.EditText(requireContext())
        editText.setText(bill.remark)
        editText.hint = "输入备注"
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("编辑备注（第${bill.installmentIndex}期）")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newRemark = editText.text.toString().trim()
                if (newRemark.isNotEmpty()) {
                    bill.remark = newRemark
                    installmentPreviewAdapter.notifyItemChanged(position)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ========== 模式切换 ==========
    private fun switchToNormalMode() {
        binding.dateLayout.visibility = View.VISIBLE
        binding.installmentParams.visibility = View.GONE
        binding.rvInstallmentPreview.visibility = View.GONE
        binding.remarkLayout.visibility = View.VISIBLE  // 显示备注
        binding.etRemark.hint = "备注"
        currentInstallmentResult = null
    }

    private fun switchToInstallmentMode() {
        binding.dateLayout.visibility = View.GONE
        binding.installmentParams.visibility = View.VISIBLE
        binding.remarkLayout.visibility = View.GONE    // 隐藏备注框
        // 备注模板不显示，因为分期使用单独备注编辑
        updateInstallmentPreview()
    }

    // ========== 公共校验方法 ==========
    /**
     * 校验金额字符串：非空、大于0、小数位不超过2位
     * @return 校验通过返回金额（分），否则返回 null
     */
    private fun validateAmount(amountText: String): Int? {
        val trimmed = amountText.trim()
        if (trimmed.isEmpty() || trimmed == "0" || trimmed == "0.0" || trimmed == "0.00") {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return null
        }
        if (trimmed.contains(".")) {
            val decimalPart = trimmed.substringAfter(".")
            if (decimalPart.length > 2) {
                Toast.makeText(requireContext(), "金额最多保留两位小数", Toast.LENGTH_SHORT).show()
                return null
            }
        }
        val amountYuan = trimmed.toDoubleOrNull()
        if (amountYuan == null || amountYuan <= 0) {
            Toast.makeText(requireContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show()
            return null
        }
        return (amountYuan * 100).toInt()
    }

    // ========== 保存 ==========
    private fun saveBill() {
        if (binding.btnNormalMode.isSelected) {
            saveNormalBill()
        } else {
            saveInstallmentBills()
        }
    }

    private fun saveNormalBill() {
        // 使用公共校验方法
        val amountText = binding.etAmount.text.toString()
        val amountFen = validateAmount(amountText) ?: return

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedChannel == null) {
            Toast.makeText(requireContext(), "请选择支付方式", Toast.LENGTH_SHORT).show()
            return
        }

        val dateStr = binding.etDate.text.toString()
        val payDate = dateStr.replace("-", "").toIntOrNull()
        if (payDate == null || payDate < 20200101) {
            Toast.makeText(requireContext(), "日期无效", Toast.LENGTH_SHORT).show()
            return
        }
        val billMonth = payDate / 100
        val remark = binding.etRemark.text.toString().trim()

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)

        val bill = ConsumeBill(
            amount = amountFen,
            categoryId = selectedCategory!!.id.toInt(),
            payChannelId = selectedChannel!!.id.toInt(),
            payDate = payDate,
            billMonth = billMonth,
            remark = remark,
            billKind = "NORMAL",
            importFileId = null,
            createTime = currentTime,
            updateTime = currentTime
        )

        lifecycleScope.launch {
            try {
                consumeBillRepository.addBill(bill)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
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

    private fun saveInstallmentBills() {
        // 🔥 先校验总金额
        val amountText = binding.etAmount.text.toString()
        val totalAmountFen = validateAmount(amountText) ?: return

        val result = currentInstallmentResult
        if (result == null || result.bills.isEmpty()) {
            Toast.makeText(requireContext(), "请先计算分期账单", Toast.LENGTH_SHORT).show()
            return
        }

        // 校验总金额是否与计算结果一致（防止用户修改金额后未重新计算）
        if (totalAmountFen != result.totalAmount) {
            Toast.makeText(requireContext(), "总金额已变更，请重新计算分期", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedChannel == null) {
            Toast.makeText(requireContext(), "请选择支付方式", Toast.LENGTH_SHORT).show()
            return
        }

        val today = dateFormat.format(Calendar.getInstance().time).replace("-", "").toInt()
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().time)

        // 构建账单列表
        val bills = result.bills.map { installment ->
            ConsumeBill(
                amount = installment.amount,
                categoryId = selectedCategory!!.id.toInt(),
                payChannelId = selectedChannel!!.id.toInt(),
                payDate = today,
                billMonth = installment.billMonth,
                remark = installment.remark,
                billKind = "INSTALLMENT",
                importFileId = null,
                createTime = currentTime,
                updateTime = currentTime
            )
        }

        lifecycleScope.launch {
            try {
                consumeBillRepository.insertAll(bills)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "分期账单保存成功（${bills.size}期）", Toast.LENGTH_SHORT).show()
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

    // ========== 选择器 ==========
    private fun showCategoryPicker() {
        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "暂无分类数据", Toast.LENGTH_SHORT).show()
            return
        }
        val names = categoryList.map { it.dictValue }.toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择分类")
            .setItems(names) { _, which ->
                selectedCategory = categoryList[which]
                binding.etCategory.setText(categoryList[which].dictValue)
            }
            .show()
    }

    private fun showPayChannelPicker() {
        if (channelList.isEmpty()) {
            Toast.makeText(requireContext(), "暂无支付方式数据", Toast.LENGTH_SHORT).show()
            return
        }
        val names = channelList.map { it.dictValue }.toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择支付方式")
            .setItems(names) { _, which ->
                selectedChannel = channelList[which]
                binding.etPayChannel.setText(channelList[which].dictValue)
            }
            .show()
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                binding.etDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showMonthPicker() {
        // 从当前开始月份解析年月
        val currentStr = binding.etStartMonth.text.toString()
        val year = if (currentStr.length == 6) currentStr.substring(0, 4).toInt() else calendar.get(Calendar.YEAR)
        val month = if (currentStr.length == 6) currentStr.substring(4).toInt() - 1 else calendar.get(Calendar.MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, _ ->
                val monthInt = selectedYear * 100 + (selectedMonth + 1)
                binding.etStartMonth.setText(monthInt.toString())
                updateInstallmentPreview()
            },
            year,
            month,
            1
        ).apply {
            // 只显示年月选择
            datePicker?.findViewById<View>(resources.getIdentifier("day", "id", "android"))?.visibility = View.GONE
        }.show()
    }

    // ========== 备注标签 ==========
    private fun loadRemarkTags() {
        binding.llRemarkTags.removeAllViews()
        if (remarkList.isEmpty()) return

        for (remark in remarkList) {
            val tagView = layoutInflater.inflate(R.layout.item_remark_tag, binding.llRemarkTags, false) as TextView
            tagView.text = remark.dictValue
            tagView.setOnClickListener {
                // 直接替换备注内容
                binding.etRemark.setText(remark.dictValue)
                // 如果在分期模式，重新计算预览
                if (binding.btnInstallmentMode.isSelected) {
                    updateInstallmentPreview()
                }
            }
            binding.llRemarkTags.addView(tagView)
        }
    }

    // ========== 回调 ==========
    fun setOnSaveSuccessListener(listener: () -> Unit) {
        onSaveSuccess = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 选中常规模式
     */
    private fun selectNormalMode() {
        // 按钮选中状态
        binding.btnNormalMode.isSelected = true
        binding.btnInstallmentMode.isSelected = false
        // 更新按钮样式
        binding.btnNormalMode.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#E8E8E8")
        )
        binding.btnNormalMode.setTextColor(android.graphics.Color.parseColor("#333333"))

        binding.btnInstallmentMode.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.WHITE
        )
        binding.btnInstallmentMode.setTextColor(android.graphics.Color.parseColor("#999999"))

        // 切换内容
        switchToNormalMode()
    }

    /**
     * 选中分期模式
     */
    private fun selectInstallmentMode() {
        // 按钮选中状态
        binding.btnInstallmentMode.isSelected = true
        binding.btnNormalMode.isSelected = false
        // 更新按钮样式
        binding.btnInstallmentMode.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#E8E8E8")
        )
        binding.btnInstallmentMode.setTextColor(android.graphics.Color.parseColor("#333333"))

        binding.btnNormalMode.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.WHITE
        )
        binding.btnNormalMode.setTextColor(android.graphics.Color.parseColor("#999999"))

        // 切换内容
        switchToInstallmentMode()
    }
}