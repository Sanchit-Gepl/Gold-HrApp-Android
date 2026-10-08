package com.goldmedal.hrapp.ui.dashboard.leave

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.databinding.ActivityApplyShortLeaveBinding
import com.goldmedal.hrapp.ui.leave.LeaveViewModel
import com.goldmedal.hrapp.util.getMinDateToApplyLeaves
import com.goldmedal.hrapp.util.toast
import dagger.hilt.android.AndroidEntryPoint
import org.angmarch.views.OnSpinnerItemSelectedListener
import java.util.*

@AndroidEntryPoint
class ApplyShortLeaveActivity : BaseActivity(), ApiStageListener<Any> {

    private lateinit var binding: ActivityApplyShortLeaveBinding
    private val viewModel: LeaveViewModel by viewModels()

    private lateinit var date: Calendar
    private val dataset = LinkedList(listOf("Select","1","2","3","4","5","6","7","8","9"))

    private var noOfHrs = "Select"

    private var strDate : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyShortLeaveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)


        viewModel.apiListener = this
        date = Calendar.getInstance()
        initSpinner()
        clickListeners()

        viewModel.getLoggedInUser().observe(this, { user ->
            if (user != null) {
                viewModel.userId = user.UserID

            }
        })

    }

    private fun clickListeners() {

        binding.rlSelectDate.setOnClickListener {


            val mYear = date[Calendar.YEAR]
            val mMonth = date[Calendar.MONTH]
            val mDay = date[Calendar.DAY_OF_MONTH]


            val previousCalendar = Calendar.getInstance()
            val minDay = getMinDateToApplyLeaves(mYear, mMonth + 1, mDay)
            previousCalendar.add(Calendar.DAY_OF_MONTH, -minDay)


            val datePicker = DatePickerDialog(this,
                    { _, year, monthOfYear, dayOfMonth ->
                        binding.tvSelectStartDate.text = String.format(Locale.getDefault(), "%s/%d/%d", dayOfMonth.toString(), monthOfYear + 1, year)
                        date.set(year, monthOfYear, dayOfMonth)
                        strDate = (monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year



                    }, mYear, mMonth, mDay)
            datePicker.datePicker.minDate = previousCalendar.timeInMillis
            datePicker.show()
        }



        binding.buttonApply.setOnClickListener {
            viewModel.strRemarks = binding.editTextRemarks.text.toString()


            viewModel.applySL(strDate,noOfHrs)
        }
    }

    private fun initSpinner() {
        binding.niceSpinner.attachDataSource(dataset as MutableList<Int>)

        binding.niceSpinner.onSpinnerItemSelectedListener =
                OnSpinnerItemSelectedListener { parent, view, position, id ->
                    val item = parent.getItemAtPosition(position) as String
                    noOfHrs = item

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
        toast(message)

    }

    override fun onValidationError(message: String, callFrom: String) {
        toast(message)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}