package com.goldmedal.hrapp.ui.dashboard.leave

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.SLHistoryData
import com.goldmedal.hrapp.databinding.ActivityShortLeaveHistoryBinding
import com.goldmedal.hrapp.ui.leave.LeaveViewModel
import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.snackbar
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ShortLeaveHistoryActivity : BaseActivity(), ApiStageListener<Any> {

    private lateinit var binding: ActivityShortLeaveHistoryBinding
    private val viewModel: LeaveViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShortLeaveHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        viewModel.apiListener = this

        viewModel.getLoggedInUser().observe(this, { user ->
            if (user != null) {
                viewModel.getSLHistory(user.UserID)
            }
        })

    }

    //RecyclerView Data source
    private fun List<SLHistoryData?>.toData(): List<SLHistoryItem?> {
        return this.map {
            SLHistoryItem(it)
        }
    }

    private fun initRecyclerView(data: List<SLHistoryItem?>) {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@ShortLeaveHistoryActivity)
            adapter = mAdapter
        }
    }

    private fun bindUI(list: List<SLHistoryData?>?) = Coroutines.main {
        list?.let {
            initRecyclerView(it.toData())
        }
    }

    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.viewCommon.hide()
        bindUI(_object as List<SLHistoryData?>)
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
}