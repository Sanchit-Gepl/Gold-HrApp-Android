package com.goldmedal.hrapp.ui.leftdrawer

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.R
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.AttendanceRegularizationListData
import com.goldmedal.hrapp.data.model.DefaultMessageData
import com.goldmedal.hrapp.data.model.RespondLeavesData
import com.goldmedal.hrapp.databinding.ActivityODRecordsBinding
import com.goldmedal.hrapp.databinding.ActivityRegularizationHistoryBinding
import com.goldmedal.hrapp.ui.dashboard.attendance.AttendanceViewModel

import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.snackbar
import com.goldmedal.hrapp.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegularizationHistoryActivity : BaseActivity(), ApiStageListener<Any>,RegularizationHistoryItem.OnCancelClickedListener {
    private lateinit var binding: ActivityRegularizationHistoryBinding
    private val viewModel: AttendanceViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegularizationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)


        viewModel.apiListener = this


        viewModel.getLoggedInUser().observe(this, Observer { user ->
            if (user != null) {
                viewModel.getAttendanceRegularizeList(user.UserID)
            }
        })
    }

    //RecyclerView Data source
    private fun List<AttendanceRegularizationListData?>.toData(): List<RegularizationHistoryItem?> {
        return this.map {
            RegularizationHistoryItem(it,this@RegularizationHistoryActivity,this@RegularizationHistoryActivity)
        }
    }

    private fun initRecyclerView(data: List<RegularizationHistoryItem?>) {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@RegularizationHistoryActivity)
            adapter = mAdapter
        }
    }

    private fun bindUI(list: List<AttendanceRegularizationListData?>?) = Coroutines.main {
        list?.let {
            initRecyclerView(it.toData())
        }
    }

    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.viewCommon.hide()


        if (callFrom == "attendance_reg_list") {
            bindUI(_object as List<AttendanceRegularizationListData?>)
        }

        if (callFrom == "cancel_reg_request") {
            val data = _object as List<DefaultMessageData>
            data[0].StatusMessage?.let { toast(it) }
            finish()
        }

    }

    override fun onError(message: String, callFrom: String, isNetworkError: Boolean) {
        if (isNetworkError) {
            binding.viewCommon.showNoInternet()
        } else {
            binding.viewCommon.showNoData()
        }
        binding.rootLayout.snackbar(message)
    }

    override fun onValidationError(message: String, callFrom: String) {
        binding.rootLayout.snackbar(message)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCancelClicked(recordId: Int?) {
        MaterialAlertDialogBuilder(this@RegularizationHistoryActivity)
                .setMessage(resources.getString(R.string.supporting_text))
                .setNegativeButton(resources.getString(R.string.str_cancel)) { dialog, which ->

                }
                .setPositiveButton(resources.getString(R.string.str_ok)) { dialog, which ->
                    viewModel.getLoggedInUser().observe(this, { user ->
                        if (user != null) {
                            viewModel.cancelRegularizationRequest(user.UserID,recordId)
                        }

                    })
                }
                .show()
    }
}