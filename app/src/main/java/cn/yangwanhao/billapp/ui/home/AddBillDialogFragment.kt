package cn.yangwanhao.billapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import cn.yangwanhao.billapp.R

class AddBillDialogFragment : DialogFragment() {

    private var billType: Int = 0 // 0: 支出, 1: 收入
    private var onSaveListener: (() -> Unit)? = null

    companion object {
        fun newInstance(type: Int, listener: () -> Unit): AddBillDialogFragment {
            val fragment = AddBillDialogFragment()
            fragment.billType = type
            fragment.onSaveListener = listener
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // TODO: 替换为你实际的新增账单弹窗布局
        return inflater.inflate(R.layout.dialog_add_bill, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: 绑定 EditText (金额、备注)、Spinner (分类)、DatePicker 等控件
        // TODO: 点击保存按钮时，调用 ViewModel 插入数据，成功后调用：
        // onSaveListener?.invoke()
        // dismiss()
    }
}