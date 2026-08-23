package cn.yangwanhao.billapp.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cn.yangwanhao.billapp.BillApplication
import cn.yangwanhao.billapp.R
import cn.yangwanhao.billapp.common.Constant
import cn.yangwanhao.billapp.entity.ConsumeBill
import cn.yangwanhao.billapp.entity.Dict
import cn.yangwanhao.billapp.repository.ConsumeBillRepository
import cn.yangwanhao.billapp.repository.DictRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExpenseDialogFragment : BottomSheetDialogFragment() {

    // ========== 控件 ==========
    private lateinit var etAmount: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etCategory: TextInputEditText
    private lateinit var etPayChannel: TextInputEditText
    private lateinit var etRemark: TextInputEditText
    private lateinit var llRemarkTags: ViewGroup

    // ========== 日期 ==========
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ========== Repository ==========
    private lateinit var dictRepository: DictRepository
    private lateinit var consumeBillRepository: ConsumeBillRepository

    // ========== 数据列表 ==========
    private var categoryList: List<Dict> = emptyList()
    private var channelList: List<Dict> = emptyList()
    private var remarkList: List<Dict> = emptyList()

    // ========== 当前选中项 ==========
    private var selectedCategory: Dict? = null
    private var selectedChannel: Dict? = null

    // ========== 保存成功回调 ==========
    private var onSaveSuccess: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.Theme_BillApp

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ========== 初始化控件 ==========
        etAmount = view.findViewById(R.id.etAmount)
        etDate = view.findViewById(R.id.etDate)
        etCategory = view.findViewById(R.id.etCategory)
        etPayChannel = view.findViewById(R.id.etPayChannel)
        etRemark = view.findViewById(R.id.etRemark)
        llRemarkTags = view.findViewById(R.id.llRemarkTags)

        // ========== 获取 Repository ==========
        val app = requireContext().applicationContext as BillApplication
        dictRepository = app.dictRepository
        consumeBillRepository = app.consumeBillRepository

        // ========== 金额框焦点处理 ==========
        etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (etAmount.text.toString() == "0") {
                    etAmount.text?.clear()
                }
            } else {
                if (etAmount.text.isNullOrEmpty()) {
                    etAmount.setText("0")
                }
            }
        }
        // ========== 备注框焦点处理 ==========
        etRemark.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (etRemark.text.toString() == "请输入或选择备注") {
                    etRemark.text?.clear()
                }
            } else {
                if (etRemark.text.isNullOrEmpty()) {
                    etRemark.setText("请输入或选择备注")
                }
            }
        }

        // ========== 默认日期 ==========
        etDate.setText(dateFormat.format(calendar.time))

        // ========== 取消按钮 ==========
        view.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }

        // ========== 保存按钮（实际保存逻辑） ==========
        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            saveBill()
        }

        // ========== 日期选择 ==========
        etDate.setOnClickListener { showDatePicker() }
        etCategory.setOnClickListener { showCategoryPicker() }
        etPayChannel.setOnClickListener { showPayChannelPicker() }

        loadDataFromDatabase()
    }

    /**
     * 设置保存成功回调（由外部调用方传入）
     */
    fun setOnSaveSuccessListener(listener: () -> Unit) {
        this.onSaveSuccess = listener
    }

    // ============================================================
    //  保存账单
    // ============================================================

    private fun saveBill() {
        // 1. 获取金额
        val amountText = etAmount.text.toString().trim()

        // 验证是否为空或为0
        if (amountText.isEmpty() || amountText == "0" || amountText == "0.0" || amountText == "0.00") {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔥 验证小数位数（最多两位）
        if (amountText.contains(".")) {
            val decimalPart = amountText.substringAfter(".")
            if (decimalPart.length > 2) {
                Toast.makeText(requireContext(), "金额最多保留两位小数", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 验证是否为有效数字
        val amountYuan = amountText.toDoubleOrNull()
        if (amountYuan == null || amountYuan <= 0) {
            Toast.makeText(requireContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show()
            return
        }
        val amountFen = (amountYuan * 100).toInt()

        // 2. 验证分类和支付方式
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedChannel == null) {
            Toast.makeText(requireContext(), "请选择支付方式", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. 解析日期
        val dateStr = etDate.text.toString()
        val payDate = dateStr.replace("-", "").toIntOrNull()
        if (payDate == null || payDate < 20200101) {
            Toast.makeText(requireContext(), "日期无效", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. 生成账单月份
        val billMonth = payDate / 100

        // 5. 获取备注
        val remark = etRemark.text.toString().trim()

        // 6. 获取当前时间
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 7. 构建 ConsumeBill 对象
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

        // 8. 执行保存
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    consumeBillRepository.addBill(bill)
                }
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

    // ============================================================
    //  数据加载
    // ============================================================

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
                    etCategory.setText(categoryList[0].dictValue)
                }
                if (channelList.isNotEmpty()) {
                    selectedChannel = channelList[0]
                    etPayChannel.setText(channelList[0].dictValue)
                }

                loadRemarkTags()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "加载数据失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ============================================================
    //  选择器
    // ============================================================

    private fun showCategoryPicker() {
        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "暂无分类数据", Toast.LENGTH_SHORT).show()
            return
        }
        val names = categoryList.map { it.dictValue }.toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择分类")
            .setItems(names) { _, which ->
                val selected = categoryList[which]
                selectedCategory = selected
                etCategory.setText(selected.dictValue)
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
                val selected = channelList[which]
                selectedChannel = selected
                etPayChannel.setText(selected.dictValue)
            }
            .show()
    }

    private fun loadRemarkTags() {
        llRemarkTags.removeAllViews()
        if (remarkList.isEmpty()) return

        for (remark in remarkList) {
            val tagView = layoutInflater.inflate(R.layout.item_remark_tag, llRemarkTags, false) as TextView
            tagView.text = remark.dictValue
            // 🔥 点击标签直接替换输入框内容
            tagView.setOnClickListener {
                etRemark.setText(remark.dictValue)
            }
            llRemarkTags.addView(tagView)
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}