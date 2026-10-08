package com.goldmedal.hrapp.ui.dashboard.leave

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.ODListData
import com.goldmedal.hrapp.databinding.ActivityODRecordsBinding
import com.goldmedal.hrapp.ui.leave.LeaveViewModel
import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.snackbar
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ODRecordsActivity : BaseActivity(), ApiStageListener<Any>{

    private lateinit var binding: ActivityODRecordsBinding
    private val viewModel: LeaveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityODRecordsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        viewModel.apiListener = this

        viewModel.getLoggedInUser().observe(this, Observer { user ->
            if (user != null) {
                viewModel.getODList(user.UserID)
            }
        })

    }


    //RecyclerView Data source
    private fun List<ODListData?>.toData(): List<ODRecordsItem?> {
        return this.map {
            ODRecordsItem(it,this@ODRecordsActivity)
        }
    }

    private fun initRecyclerView(data: List<ODRecordsItem?>) {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@ODRecordsActivity)
            adapter = mAdapter
        }
    }

    private fun bindUI(list: List<ODListData?>?) = Coroutines.main {
        list?.let {
            initRecyclerView(it.toData())
        }
    }

    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.viewCommon.hide()
        bindUI(_object as List<ODListData?>)
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