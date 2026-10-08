package com.goldmedal.hrapp.ui.dashboard.leave

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.databinding.ActivityODApplicationBinding
import com.goldmedal.hrapp.ui.leave.LeaveViewModel
import com.goldmedal.hrapp.util.getMinDateToApplyLeaves
import com.goldmedal.hrapp.util.snackbar
import com.goldmedal.hrapp.util.toast
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class ODApplicationActivity : BaseActivity(), ApiStageListener<Any> {


    private lateinit var binding: ActivityODApplicationBinding
    private val viewModel: LeaveViewModel by viewModels()

    private lateinit var minEndDate: Calendar
    private lateinit var maxStartDate: Calendar

    private var subLocationId: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityODApplicationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        viewModel.apiListener = this
        maxStartDate = Calendar.getInstance()
        minEndDate = Calendar.getInstance()

        clickListeners()


        viewModel.getLoggedInUser().observe(this, Observer { user ->
            if (user != null) {
                viewModel.userId = user.UserID

            }
        })
    }

    private fun clickListeners() {

        binding.rlSelectStartDate.setOnClickListener {

            val c = Calendar.getInstance()
            val mYear = minEndDate[Calendar.YEAR]
            val mMonth = minEndDate[Calendar.MONTH]
            val mDay = minEndDate[Calendar.DAY_OF_MONTH]


            val previousCalendar = Calendar.getInstance()
            val minDay = getMinDateToApplyLeaves(mYear, mMonth + 1, mDay)
            previousCalendar.add(Calendar.DAY_OF_MONTH, -minDay)


            val startDatePicker = DatePickerDialog(this,
                    { _, year, monthOfYear, dayOfMonth ->
                        binding.tvSelectStartDate.text = String.format(Locale.getDefault(), "%s/%d/%d", dayOfMonth.toString(), monthOfYear + 1, year)
                        minEndDate.set(year, monthOfYear, dayOfMonth)
                        viewModel.strStartDate = (monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year

                        if (viewModel.strEndDate.isNullOrEmpty()) {
                            binding.rootLayout.snackbar("Please Select End Date")
                        }

                    }, mYear, mMonth, mDay)
            startDatePicker.datePicker.minDate = previousCalendar.timeInMillis


            if (viewModel.strEndDate?.isNotEmpty() == true) {
                startDatePicker.datePicker.maxDate = maxStartDate.timeInMillis
            }
            startDatePicker.show()
        }
        binding.rlSelectEndDate.setOnClickListener {
            val mYear = maxStartDate[Calendar.YEAR]
            val mMonth = maxStartDate[Calendar.MONTH]
            val mDay = maxStartDate[Calendar.DAY_OF_MONTH]


            val previousCalendar = Calendar.getInstance()
            val minDay = getMinDateToApplyLeaves(mYear, mMonth + 1, mDay)
            previousCalendar.add(Calendar.DAY_OF_MONTH, -minDay)

            val endDatePicker = DatePickerDialog(this,
                    { view, year, monthOfYear, dayOfMonth ->
                        binding.tvSelectEndDate.text = String.format(Locale.getDefault(), "%d/%d/%d", dayOfMonth, monthOfYear + 1, year)
                        maxStartDate.set(year, monthOfYear, dayOfMonth)
                        viewModel.strEndDate = (monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year
                        if (viewModel.strStartDate.isNullOrEmpty()) {
                            binding.rootLayout.snackbar("Please Select Start Date")
                        } else {
                        }
                    }, mYear, mMonth, mDay)
            if (viewModel.strStartDate?.isNotEmpty() == true) {
                endDatePicker.datePicker.minDate = minEndDate.timeInMillis
            } else {
                endDatePicker.datePicker.minDate = previousCalendar.timeInMillis
            }
            endDatePicker.show()
        }

        binding.rlSelectSubLocation.setOnClickListener {
            startActivityForResult(Intent(this, SubLocationListActivity::class.java), LAUNCH_SUB_LOCATION_LIST)
        }
        binding.buttonApplyOD.setOnClickListener {
            viewModel.strRemarks = binding.editTextRemarks.text.toString()


            viewModel.applyOD(subLocationId ?: 0)
        }
    }


    override fun onStarted(callFrom: String) {
        binding.progressBar.start()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.progressBar.stop()
        toast("OD Applied Successfully!!!")
        finish()
    }

    override fun onError(message: String, callFrom: String, isNetworkError: Boolean) {
        binding.progressBar.stop()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LAUNCH_SUB_LOCATION_LIST) {
            if (resultCode == RESULT_OK) {
                subLocationId = data?.getIntExtra(SubLocationListActivity.SUB_LOCATION_ID, 0)
                binding.txtOnSiteLocation.visibility = View.VISIBLE
                binding.txtOnSiteLocation.text = data?.getStringExtra(SubLocationListActivity.SUB_LOCATION_NAME)
            }
        }
    }



    companion object {
        internal const val LAUNCH_SUB_LOCATION_LIST = 686
    }


}