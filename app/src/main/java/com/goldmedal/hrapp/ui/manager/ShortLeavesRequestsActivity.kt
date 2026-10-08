package com.goldmedal.hrapp.ui.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.R
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.DefaultMessageData
import com.goldmedal.hrapp.data.model.SLForApprovalData
import com.goldmedal.hrapp.databinding.ActivityShortLeavesRequestsBinding
import com.goldmedal.hrapp.ui.leave.LeaveViewModel
import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class ShortLeavesRequestsActivity : BaseActivity(), ApiStageListener<Any> ,SLRequestsItem.OnApprovalClickListener {
//Commit
    private lateinit var binding: ActivityShortLeavesRequestsBinding
    private var filteredList: List<SLForApprovalData?> = emptyList()
    private val viewModel: LeaveViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShortLeavesRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        viewModel.apiListener = this

        viewModel.getLoggedInUser().observe(this, { user ->
            if (user != null) {
                viewModel.getShortLeaveForApproval(user.UserID)
            }
        })

        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                filter(charSequence.toString())
            }
            override fun afterTextChanged(editable: Editable) {}
        })
    }

    //RecyclerView Data source
    private fun List<SLForApprovalData?>.toData(): List<SLRequestsItem?> {
        return this.map {
            SLRequestsItem(it, this@ShortLeavesRequestsActivity)
        }
    }

    private fun initRecyclerView(data: List<SLRequestsItem?>) {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@ShortLeavesRequestsActivity)
            addItemDecoration(DividerItemDecoration(this@ShortLeavesRequestsActivity, DividerItemDecoration.VERTICAL))
            adapter = mAdapter
        }
    }

    private fun bindUI(list: List<SLForApprovalData?>?) = Coroutines.main {
        list?.let {
            initRecyclerView(it.toData())
        }
    }

    private fun filter(text: String) {
        Coroutines.io {
            val filteredNames = filteredList.filter {
                (it?.EmployeeName?.lowercase(Locale.getDefault())?.contains(text.lowercase(Locale.getDefault())) == true)
            }
            filterUI(filteredNames)
        }
    }

    private fun filterList(data: List<SLRequestsItem?>) = Coroutines.main {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            adapter = mAdapter
        }
        if (data.isEmpty()) {
            binding.viewCommon.showNoData()
        } else {
            binding.viewCommon.hide()
        }
    }

    private fun filterUI(list: List<SLForApprovalData?>?) = Coroutines.main {
        list?.let {
            filterList(it.toData())
        }
    }


    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {

        binding.viewCommon.hide()

        if (callFrom == "SLForApproval") {
            filteredList = _object as List<SLForApprovalData?>
            bindUI(filteredList)
        }
        if (callFrom == "approveRejectSL") {
            val data = _object as List<DefaultMessageData?>
            binding.rootLayout.snackbar(data[0]?.StatusMessage ?: "Success!")
            viewModel.getLoggedInUser().observe(this, Observer { user ->
                if (user != null) {
                    viewModel.getShortLeaveForApproval(user.UserID)
                }
            })
        }
    }

    override fun onError(message: String, callFrom: String, isNetworkError: Boolean) {
        binding.viewCommon.hide()
        if (callFrom == "SLForApproval") {
            if (isNetworkError) {
                binding.viewCommon.showNoInternet()
            } else {
                binding.viewCommon.showNoData()
            }
            bindUI(ArrayList())

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

    override fun onApproval(item: SLForApprovalData?, type: Int) {
        MaterialAlertDialogBuilder(this)
                .setMessage(if (type == 1) resources.getString(R.string.accept_text) else resources.getString(R.string.reject_text))
                .setNegativeButton(resources.getString(R.string.str_cancel)) { dialog, which ->

                }
                .setPositiveButton(resources.getString(R.string.str_ok)) { dialog, which ->
                    viewModel.getLoggedInUser().observe(this, Observer { user ->
                        if (user != null) {

                            viewModel.approveRejectSL(user.UserID, item?.ID, type)
                        }

                    })
                }
                .show()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ShortLeavesRequestsActivity::class.java))
        }
    }
}