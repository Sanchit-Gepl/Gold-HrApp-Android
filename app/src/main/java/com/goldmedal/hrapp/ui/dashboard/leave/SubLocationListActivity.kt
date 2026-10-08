package com.goldmedal.hrapp.ui.dashboard.leave

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.SubLocationsData
import com.goldmedal.hrapp.databinding.ActivitySubLocationListBinding
import com.goldmedal.hrapp.ui.leave.LeaveViewModel
import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.getAddressFromLatLong
import com.goldmedal.hrapp.util.snackbar
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SubLocationListActivity : BaseActivity() ,  ApiStageListener<Any>,OnListClickListener {


    private lateinit var binding: ActivitySubLocationListBinding


private val viewModel: LeaveViewModel by viewModels()
    private var subLocationList: List<SubLocationsData?> = mutableListOf()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubLocationListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        viewModel.apiListener = this

        viewModel.getLoggedInUser().observe(this, { user ->
            if (user != null) {

                viewModel.userId = user.UserID
                viewModel.getSubLocationList()
            }
        })
    }

    //RecyclerView Data source
    private fun List<SubLocationsData?>.toData(): List<SubLocationsItem?> {
        return this.map {
            SubLocationsItem(it, this@SubLocationListActivity)
        }
    }

    private fun initRecyclerView(data: List<SubLocationsItem?>) {
       val mAdapter = GroupAdapter<GroupieViewHolder>().apply {
            addAll(data)
        }

        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@SubLocationListActivity)
            addItemDecoration(DividerItemDecoration(this@SubLocationListActivity, DividerItemDecoration.VERTICAL))
            adapter = mAdapter
        }
    }

    private fun bindUI(list: List<SubLocationsData?>?) = Coroutines.main {
        list?.let {
            initRecyclerView(it.toData())
        }
    }

    private fun bindCoordinates() = Coroutines.main {
        withContext(Dispatchers.Default) {
            subLocationList.let {
                //get address from coordinates and bind it to dataset
                subLocationList.map {
                    if (!it?.Lat.isNullOrEmpty()) {
                        it?.SubLocationAddress = getAddressFromLatLong(this@SubLocationListActivity, it?.Lat?.toDoubleOrNull()
                                ?: 0.0, it?.Long?.toDoubleOrNull() ?: 0.0)
                    }
                }
            }
        }
    }

    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.viewCommon.hide()
        subLocationList = _object as List<SubLocationsData?>
        bindCoordinates()
        bindUI(subLocationList)
        }
    override fun onError(message: String, callFrom: String, isNetworkError: Boolean) {
        if (isNetworkError) {
            binding.viewCommon.showNoInternet()
        } else {
            binding.viewCommon.showNoDataImage()
        }
        binding.rootLayout.snackbar(message)
    }

    override fun onValidationError(message: String, callFrom: String) {
        binding.rootLayout.snackbar(message)
    }


    //Recycler View Item Clicked
    override fun onItemClick(model: SubLocationsData?) {
        val intent = intent
        intent.putExtra(SUB_LOCATION_ID, model?.SublocationID)
        intent.putExtra(SUB_LOCATION_NAME, model?.SublocationName)
        setResult(Activity.RESULT_OK, intent)
        finish()

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    companion object {
        const val SUB_LOCATION_ID = "sub_location_id"
        const val SUB_LOCATION_NAME = "sub_location_name"

    }
}