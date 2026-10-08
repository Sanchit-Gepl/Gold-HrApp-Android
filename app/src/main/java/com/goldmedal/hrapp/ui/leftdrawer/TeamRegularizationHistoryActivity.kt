package com.goldmedal.hrapp.ui.leftdrawer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.AttendanceRegularizeByUserData
import com.goldmedal.hrapp.data.model.ODApprovalData
import com.goldmedal.hrapp.databinding.ActivityTeamRegularizationHistoryBinding
import com.goldmedal.hrapp.ui.dashboard.attendance.AttendanceViewModel
import com.goldmedal.hrapp.ui.dashboard.leave.ODApprovalItem
import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.snackbar
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class TeamRegularizationHistoryActivity : BaseActivity() , ApiStageListener<Any> {
    private lateinit var binding: ActivityTeamRegularizationHistoryBinding
    private val viewModel: AttendanceViewModel by viewModels()
    private var filteredList: List<AttendanceRegularizeByUserData?> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeamRegularizationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)


        viewModel.apiListener = this

        viewModel.getLoggedInUser().observe(this, { user ->
            if (user != null) {
                viewModel.getAttendanceRegularizeByUser(user.UserID)
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
    private fun List<AttendanceRegularizeByUserData?>.toData(): List<TeamRegularizationHistoryItem?> {
        return this.map {
           TeamRegularizationHistoryItem(it,this@TeamRegularizationHistoryActivity)
        }
    }

    private fun initRecyclerView(data: List<TeamRegularizationHistoryItem?>) {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@TeamRegularizationHistoryActivity)
            adapter = mAdapter
        }
    }

    private fun bindUI(list: List<AttendanceRegularizeByUserData?>?) = Coroutines.main {
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

    private fun filterUI(list: List<AttendanceRegularizeByUserData?>?) = Coroutines.main {
        list?.let {
            filterList(it.toData())
        }
    }

    private fun filterList(data: List<TeamRegularizationHistoryItem?>) = Coroutines.main {
        val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            adapter = mAdapter
        }
        if(data.isEmpty()){
            binding.viewCommon.showNoData()
        }else{
            binding.viewCommon.hide()
        }
    }

    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.viewCommon.hide()
        filteredList = _object as List<AttendanceRegularizeByUserData?>

        bindUI(filteredList)
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