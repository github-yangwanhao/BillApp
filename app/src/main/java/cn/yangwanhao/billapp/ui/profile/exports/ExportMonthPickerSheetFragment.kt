package cn.yangwanhao.billapp.ui.profile.exports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cn.yangwanhao.billapp.databinding.FragmentExportMonthPickerBinding

class ExportMonthPickerSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_MONTHS = "months"

        fun newInstance(months: IntArray): ExportMonthPickerSheetFragment {
            val f = ExportMonthPickerSheetFragment()
            f.arguments = Bundle().apply {
                putIntArray(ARG_MONTHS, months)
            }
            return f
        }
    }

    private var _binding: FragmentExportMonthPickerBinding? = null
    private val binding get() = _binding!!

    private val adapter = ExportMonthAdapter { monthInt ->
        onMonthSelected?.invoke(monthInt)
        dismiss()
    }

    private var onMonthSelected: ((Int) -> Unit)? = null

    fun setOnMonthSelectedListener(listener: (Int) -> Unit) {
        onMonthSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportMonthPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMonths.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMonths.adapter = adapter

        val months = arguments?.getIntArray(ARG_MONTHS)?.toList() ?: emptyList()
        adapter.submitList(months)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}