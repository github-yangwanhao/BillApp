package cn.yangwanhao.billapp.ui.home.add

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import cn.yangwanhao.billapp.dto.InstallmentBillDto
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import cn.yangwanhao.billapp.ui.adapter.InstallmentPreviewAdapter
import cn.yangwanhao.billapp.ui.dialog.MonthRangePickerDialog
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
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt

class AddExpenseDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var dictRepository: DictRepository
    private lateinit var consumeBillRepository: ConsumeBillRepository

    private var categoryList: List<Dict> = emptyList()
    private var channelList: List<Dict> = emptyList()
    private var remarkList: List<Dict> = emptyList()
    private var selectedCategory: Dict? = null
    private var selectedChannel: Dict? = null

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())

    private val installmentPreviewAdapter = InstallmentPreviewAdapter { bill, position ->
        showEditRemarkDialog(bill, position)
    }
    private var currentInstallmentResult: InstallmentResult? = null

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

        initViews()
        loadDataFromDatabase()
        setupListeners()
        setupInstallmentPreview()
        switchToNormalMode()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
                behavior.peekHeight = maxHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                it.layoutParams.height = maxHeight
                it.requestLayout()
            }
        }
        return dialog
    }

    private fun initViews() {
        selectNormalMode()
        binding.etDate.setText(dateFormat.format(calendar.time))
        binding.tvMonthRangeDisplay.text = "请选择"

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

    private fun setupListeners() {
        binding.tvCancel.setOnClickListener { dismiss() }
        binding.tvSave.setOnClickListener { saveBill() }
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etCategory.setOnClickListener { showCategoryPicker() }
        binding.etPayChannel.setOnClickListener { showPayChannelPicker() }

        binding.llMonthRangePicker.setOnClickListener {
            if (!binding.btnInstallmentMode.isSelected) {
                Toast.makeText(requireContext(), "请先切换到分期模式", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showMonthRangePicker()
        }

        binding.etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (binding.btnInstallmentMode.isSelected) {
                    updateInstallmentPreviewFromCurrentState()
                }
            }
        })

        binding.btnNormalMode.setOnClickListener { selectNormalMode() }
        binding.btnInstallmentMode.setOnClickListener { selectInstallmentMode() }
    }

    private fun showMonthRangePicker() {
        val dialog = MonthRangePickerDialog(requireContext())
        dialog.setOnConfirmListener { sYear, sMonth, eYear, eMonth ->
            val displayText = "${sYear}年${sMonth}月 → ${eYear}年${eMonth}月"
            binding.tvMonthRangeDisplay.text = displayText
            updateInstallmentPreviewFromCurrentState()
        }
        dialog.show()
    }

    private fun updateInstallmentPreviewFromCurrentState() {
        if (!binding.btnInstallmentMode.isSelected) return

        val rangeText = binding.tvMonthRangeDisplay.text.toString()
        if (rangeText == "请选择") {
            binding.rvInstallmentPreview.visibility = View.GONE
            binding.tvInstallmentCountHint.visibility = View.GONE
            return
        }

        val pattern = Regex("""(\d{4})年(\d{1,2})月 → (\d{4})年(\d{1,2})月""")
        val matchResult = pattern.find(rangeText)
        if (matchResult == null) {
            binding.rvInstallmentPreview.visibility = View.GONE
            binding.tvInstallmentCountHint.visibility = View.GONE
            return
        }

        val startYear = matchResult.groupValues[1].toInt()
        val startMonth = matchResult.groupValues[2].toInt()
        val endYear = matchResult.groupValues[3].toInt()
        val endMonth = matchResult.groupValues[4].toInt()

        val amountText = binding.etAmount.text.toString().trim()
        val totalAmount = (amountText.toDoubleOrNull() ?: 0.0) * 100
        if (totalAmount <= 0) {
            binding.rvInstallmentPreview.visibility = View.GONE
            binding.tvInstallmentCountHint.visibility = View.GONE
            return
        }

        val baseRemark = binding.etRemark.text.toString().trim()

        try {
            val result = InstallmentCalculator.calculateByYearMonth(
                totalAmount = totalAmount.toInt(),
                startYear = startYear,
                startMonth = startMonth,
                endYear = endYear,
                endMonth = endMonth,
                baseRemark = baseRemark
            )
            currentInstallmentResult = result
            installmentPreviewAdapter.submitList(result.bills)
            binding.rvInstallmentPreview.visibility = View.VISIBLE
            binding.tvInstallmentCountHint.visibility = View.VISIBLE
            binding.tvInstallmentCountHint.text = "共 ${result.installmentCount} 期"
        } catch (e: Exception) {
            e.printStackTrace()
            binding.rvInstallmentPreview.visibility = View.GONE
            binding.tvInstallmentCountHint.visibility = View.GONE
        }
    }

    private fun setupInstallmentPreview() {
        binding.rvInstallmentPreview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = installmentPreviewAdapter
            // 🔥 保持默认嵌套滚动行为，配合 NestedScrollView 工作
            // 不设置 isNestedScrollingEnabled 以保持默认 true
        }
    }

    @Suppress("DEPRECATION")
    private fun updateInstallmentPreview() {
        // 保留空实现
    }

    private fun showEditRemarkDialog(bill: InstallmentBillDto, position: Int) {
        val editText = EditText(requireContext())
        editText.setText(bill.remark)
        editText.hint = "输入备注"
        AlertDialog.Builder(requireContext())
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

    private fun switchToNormalMode() {
        binding.dateLayout.visibility = View.VISIBLE
        binding.installmentParams.visibility = View.GONE
        binding.rvInstallmentPreview.visibility = View.GONE
        binding.tvInstallmentCountHint.visibility = View.GONE
        binding.remarkLayout.visibility = View.VISIBLE
        binding.etRemark.hint = "备注"
        currentInstallmentResult = null
    }

    private fun switchToInstallmentMode() {
        binding.dateLayout.visibility = View.GONE
        binding.installmentParams.visibility = View.VISIBLE
        binding.remarkLayout.visibility = View.GONE
        if (binding.tvMonthRangeDisplay.text.toString() != "请选择") {
            updateInstallmentPreviewFromCurrentState()
        }
    }

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

    private fun saveBill() {
        if (binding.btnNormalMode.isSelected) {
            saveNormalBill()
        } else {
            saveInstallmentBills()
        }
    }

    private fun saveNormalBill() {
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
        val currentTime = Date()

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
        val amountText = binding.etAmount.text.toString()
        val totalAmountFen = validateAmount(amountText) ?: return

        val result = currentInstallmentResult
        if (result == null || result.bills.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择分期月份范围", Toast.LENGTH_SHORT).show()
            return
        }

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
        val currentTime = Date()

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

    private fun showCategoryPicker() {
        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "暂无分类数据", Toast.LENGTH_SHORT).show()
            return
        }
        val names = categoryList.map { it.dictValue }.toTypedArray()
        AlertDialog.Builder(requireContext())
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
        AlertDialog.Builder(requireContext())
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

    private fun loadRemarkTags() {
        binding.llRemarkTags.removeAllViews()
        if (remarkList.isEmpty()) return

        for (remark in remarkList) {
            val tagView = layoutInflater.inflate(R.layout.item_remark_tag, binding.llRemarkTags, false) as TextView
            tagView.text = remark.dictValue
            tagView.setOnClickListener {
                binding.etRemark.setText(remark.dictValue)
                if (binding.btnInstallmentMode.isSelected) {
                    updateInstallmentPreviewFromCurrentState()
                }
            }
            binding.llRemarkTags.addView(tagView)
        }
    }

    fun setOnSaveSuccessListener(listener: () -> Unit) {
        onSaveSuccess = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun selectNormalMode() {
        binding.btnNormalMode.isSelected = true
        binding.btnInstallmentMode.isSelected = false
        binding.btnNormalMode.backgroundTintList = ColorStateList.valueOf("#E8E8E8".toColorInt())
        binding.btnNormalMode.setTextColor("#333333".toColorInt())
        binding.btnInstallmentMode.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnInstallmentMode.setTextColor("#999999".toColorInt())
        switchToNormalMode()
    }

    private fun selectInstallmentMode() {
        binding.btnInstallmentMode.isSelected = true
        binding.btnNormalMode.isSelected = false
        binding.btnInstallmentMode.backgroundTintList = ColorStateList.valueOf("#E8E8E8".toColorInt())
        binding.btnInstallmentMode.setTextColor("#333333".toColorInt())
        binding.btnNormalMode.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnNormalMode.setTextColor("#999999".toColorInt())
        switchToInstallmentMode()
    }
}