package com.goldmedal.hrapp.ui.leftdrawer

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.data.model.AttendanceDetailsData

import com.goldmedal.hrapp.databinding.ActivityTimeCardBinding
import com.goldmedal.hrapp.ui.dashboard.attendance.AttendanceFragment.Companion.TAG_ABSENT
import com.goldmedal.hrapp.ui.dashboard.attendance.AttendanceFragment.Companion.TAG_CHECKOUT_MISSING
import com.goldmedal.hrapp.ui.dashboard.attendance.AttendanceFragment.Companion.TAG_PRESENT
import com.goldmedal.hrapp.ui.dashboard.attendance.AttendanceViewModel
import com.goldmedal.hrapp.ui.dialogs.RegularizeAttendanceDialog
import com.goldmedal.hrapp.util.*
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import dagger.hilt.android.AndroidEntryPoint

import java.util.*
import kotlin.math.roundToInt

@AndroidEntryPoint
class TimeCardActivity : BaseActivity(), ApiStageListener<Any>,AdapterCallbackListener {
    private lateinit var binding: ActivityTimeCardBinding

    private val viewModel: AttendanceViewModel by viewModels()


    private lateinit var minEndDate: Calendar
    private lateinit var maxStartDate: Calendar
    private lateinit var groupLayoutManager: LinearLayoutManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        binding = ActivityTimeCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        viewModel.apiListener = this

        maxStartDate = Calendar.getInstance()
        minEndDate = Calendar.getInstance()
//        minEndDate.add(Calendar.MONTH, -1)
        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.add(Calendar.MONTH, -1)
        val previous = calendar.time

        viewModel.strStartDate = previous.toString("yyyy-MM-dd")
        viewModel.strEndDate = today.toString("yyyy-MM-dd")

        binding.txtStartDate.text = previous.toString("dd/MM/yyyy")
        binding.txtEndDate.text = today.toString("dd/MM/yyyy")

        intent?.let {
            val sevenDayCalendar = Calendar.getInstance()
            val sevenDayToday = sevenDayCalendar.time
            val minDay = getMinDateToApplyLeaves(sevenDayCalendar[Calendar.YEAR], sevenDayCalendar[Calendar.MONTH] + 1, sevenDayCalendar[Calendar.DAY_OF_MONTH])

            if (minDay > 7) {
                sevenDayCalendar.add(Calendar.DAY_OF_MONTH, -7)
            } else {
                sevenDayCalendar.add(Calendar.DAY_OF_MONTH, -minDay)
            }

            val sevenDayPrevious = sevenDayCalendar.time

            viewModel.strStartDate = sevenDayPrevious.toString("yyyy-MM-dd")
            viewModel.strEndDate = sevenDayToday.toString("yyyy-MM-dd")

            binding.txtStartDate.text = sevenDayPrevious.toString("dd/MM/yyyy")
            binding.txtEndDate.text = sevenDayToday.toString("dd/MM/yyyy")
        }



        viewModel.getLoggedInUser().observe(this) { user ->
            if (user != null) {
                viewModel.getAttendanceDetails(user.UserID)
            }
        }

        clickListeners()


    }

    private fun clickListeners() {


        binding.txtStartDate.setOnClickListener {
            val mYear = minEndDate[Calendar.YEAR]
            val mMonth = minEndDate[Calendar.MONTH]
            val mDay = minEndDate[Calendar.DAY_OF_MONTH]

            val previousCalendar = Calendar.getInstance()
            val minDay = getMinDateToApplyLeaves(
                previousCalendar[Calendar.YEAR],
                previousCalendar[Calendar.MONTH] + 1,
                previousCalendar[Calendar.DAY_OF_MONTH])
//            Log.d("TAG", "Min Day - $minDay, Year-$mYear, Month-$mMonth, Day-$mDay")

            previousCalendar.add(Calendar.DAY_OF_MONTH, -minDay)

            val startDatePicker = DatePickerDialog(this,
                { view, year, monthOfYear, dayOfMonth ->
                    binding.txtStartDate.text = String.format(
                        Locale.getDefault(),
                        "%s/%d/%d",
                        dayOfMonth.toString(),
                        monthOfYear + 1,
                        year
                    )
                    minEndDate.set(year, monthOfYear, dayOfMonth)
                    viewModel.strStartDate = year.toString() + "-" + (monthOfYear + 1).toString() + "-" + dayOfMonth
                        //(monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year


                }, mYear, mMonth, mDay
            )
            // Use monthStartDateRegularization from API as minDate, fallback to previousCalendar
            val minDateCalendar = if (!viewModel.monthStartDateRegularization.isNullOrEmpty()) {
                getCalendarFromDateTimeString(viewModel.monthStartDateRegularization!!)
            } else {
                previousCalendar
            }
            startDatePicker.datePicker.minDate = minDateCalendar.timeInMillis

            // Use monthEndDateRegularization from API as maxDate
            if (!viewModel.monthEndDateRegularization.isNullOrEmpty()) {
                startDatePicker.datePicker.maxDate = getCalendarFromDateTimeString(viewModel.monthEndDateRegularization!!).timeInMillis
            } else if (viewModel.strEndDate?.isNotEmpty() == true) {
                startDatePicker.datePicker.maxDate = maxStartDate.timeInMillis
            }
//            Log.d("TAG", "End Date - ${viewModel.strStartDate}")
            startDatePicker.show()
        }

        binding.txtEndDate.setOnClickListener {
            try {
                val c = Calendar.getInstance()
                val mYear = maxStartDate[Calendar.YEAR]
                val mMonth = maxStartDate[Calendar.MONTH]
                val mDay = maxStartDate[Calendar.DAY_OF_MONTH]

                val previousCalendar = Calendar.getInstance()
                val minDay = getMinDateToApplyLeaves(mYear, mMonth + 1, mDay)
                previousCalendar.add(Calendar.DAY_OF_MONTH, -minDay)

                val endDatePicker = DatePickerDialog(this,
                    { view, year, monthOfYear, dayOfMonth ->
                        binding.txtEndDate.text = String.format(
                            Locale.getDefault(),
                            "%d/%d/%d",
                            dayOfMonth,
                            monthOfYear + 1,
                            year
                        )
                        maxStartDate.set(year, monthOfYear, dayOfMonth)
                        viewModel.strEndDate = year.toString() + "-" + (monthOfYear + 1).toString() + "-" + dayOfMonth
                        //(monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year
                    }, mYear, mMonth, mDay
                )
                // Set maxDate from API (monthEndDateRegularization), fallback to today
                if (!viewModel.monthEndDateRegularization.isNullOrEmpty()) {
                    endDatePicker.datePicker.maxDate = getCalendarFromDateTimeString(viewModel.monthEndDateRegularization!!).timeInMillis
                } else {
                    endDatePicker.datePicker.maxDate = c.timeInMillis
                }

                // Set minDate: selected start date, else monthStartDateRegularization, else previousCalendar
                if (viewModel.strStartDate?.isNotEmpty() == true) {
                    endDatePicker.datePicker.minDate = getCalendarFromDate(viewModel.strStartDate!!).timeInMillis
                } else if (!viewModel.monthStartDateRegularization.isNullOrEmpty()) {
                    endDatePicker.datePicker.minDate = getCalendarFromDateTimeString(viewModel.monthStartDateRegularization!!).timeInMillis
                } else {
                    endDatePicker.datePicker.minDate = previousCalendar.timeInMillis
                }
                endDatePicker.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.fabSelectDate.setOnClickListener {

            viewModel.getLoggedInUser().observe(this) { user ->
                if (user != null) {
                    viewModel.getAttendanceDetails(user.UserID)
                }
            }
        }

    }

    //RecyclerView Data source
    private fun List<AttendanceDetailsData?>.toExpandableData(): List<ExpandableTimeCardItem?> {
        return this.map {
            ExpandableTimeCardItem(it, this@TimeCardActivity)
        }
    }

    private fun List<AttendanceDetailsData?>.toChildData(): List<ChildTimeCardItem?> {
        return this.map {
            ChildTimeCardItem(it, this@TimeCardActivity, this@TimeCardActivity)
        }
    }

    private fun initRecyclerView(
        parentList: List<ExpandableTimeCardItem?>,
        childList: List<ChildTimeCardItem?>
    ) {
        groupLayoutManager = LinearLayoutManager(this)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()

        binding.rvList.apply {
            layoutManager = groupLayoutManager
            adapter = groupAdapter
        }

        // todo - test
        groupAdapter.apply {
            for (i in parentList.indices) {
                add(ExpandableGroup(parentList[i]).apply {
                    add(Section(childList[i]))
                })
            }
        }
    }

    private fun bindUI(list: List<AttendanceDetailsData?>?) = Coroutines.main {
        list?.let {
            initRecyclerView(it.toExpandableData(), it.toChildData())
        }
    }

    private fun showAttendanceRegDialog(
        punchDate: String?,
        oldPunchInTime: String?,
        oldPunchOutTime: String?
    ) {

        val dialogFragment = RegularizeAttendanceDialog.newInstance()
        val bundle = Bundle()
        bundle.putString("punchDate", punchDate)
        bundle.putString("oldPunchInTime", oldPunchInTime)
        bundle.putString("oldPunchOutTime", oldPunchOutTime)
        dialogFragment.arguments = bundle
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("attendance_dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        ft.let { dialogFragment.show(it, "attendance_dialog") }

    }

    override fun onStarted(callFrom: String) {
        binding.viewCommon.showProgressBar()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.viewCommon.hide()
        val data = _object as List<AttendanceDetailsData?>
        bindWorkingHours(data)
        val presentCount = data.filter {
            (it?.status.equals(TAG_PRESENT, ignoreCase = true))
        }

        val absentCount = data.filter {
            (it?.status.equals(TAG_ABSENT, ignoreCase = true))
        }

        val missedPunchCount = data.filter {
            (it?.status.equals(TAG_CHECKOUT_MISSING, ignoreCase = true))
        }

        binding.txtPresentDays.text = presentCount.size.toString()
        binding.txtAbsentDays.text = absentCount.size.toString()
        binding.txtMissedPunchDays.text = missedPunchCount.size.toString()


        val mutableList: MutableList<AttendanceDetailsData> =
            data as MutableList<AttendanceDetailsData>
        mutableList.reverse()
        bindUI(mutableList)

    }


    private fun bindWorkingHours(data: List<AttendanceDetailsData?>) {
        var totalTime = 0.0
        var timeArr: List<String>?
        for (i in data.indices) {
            timeArr = data[i]?.TotalHours?.split(":")
            if (timeArr?.size ?: 0 > 1) {
                totalTime += addWorkingHours(
                    timeArr?.get(0)?.toInt() ?: 0, timeArr?.get(1)?.toDouble()
                        ?: 0.0
                )
            }
        }

        val formattedTotalTime = String.format("%.2f", totalTime)
        val arr = formattedTotalTime.split("\\.".toRegex()).toTypedArray()
        val intArr = IntArray(2)
        intArr[0] = arr[0].toInt()
        intArr[1] = arr[1].toInt()
        binding.txtTotalWorkHrs.text = formatWorkingHours(intArr[0], intArr[1])
    }

    private fun addWorkingHours(hours: Int, min: Double): Double {
        return hours + (min / 60)
    }

    private fun formatWorkingHours(hours: Int, min: Int): String {
        return String.format("%02d:%02d", hours, ((min.toDouble() * 60) / 100).roundToInt())
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCallback(punchDate: String?, oldPunchInTime: String?, oldPunchOutTime: String?) {
        showAttendanceRegDialog(punchDate, oldPunchInTime, oldPunchOutTime)


    }


    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TimeCardActivity::class.java)
            intent.putExtra("callFrom", "")
            context.startActivity(intent)
        }
    }
}