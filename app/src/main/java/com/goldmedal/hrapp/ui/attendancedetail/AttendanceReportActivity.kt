package com.goldmedal.hrapp.ui.attendancedetail

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.R
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.common.HexColors
import com.goldmedal.hrapp.data.CalendarObj
import com.goldmedal.hrapp.data.adapters.CustomLegendAdapter
import com.goldmedal.hrapp.data.model.AttendanceDetailsData
import com.goldmedal.hrapp.data.model.AttendanceHistoryData
import com.goldmedal.hrapp.databinding.ActivityAttendanceDetailBinding
import com.goldmedal.hrapp.ui.dashboard.attendance.*
import com.goldmedal.hrapp.util.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AttendanceReportActivity : BaseActivity(), OnDateSelectedListener, ApiStageListener<Any> {
    private lateinit var binding: ActivityAttendanceDetailBinding
    private  val viewModel: AttendanceViewModel by viewModels()
    private var item: AttendanceHistoryData? = null

    private lateinit var adapter: CustomLegendAdapter
    private var attendanceData: List<AttendanceDetailsData>? = null


    private var minimumDate: Long = 0
    private var maximumDate: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttendanceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        viewModel.apiListener = this


        item = intent.getSerializableExtra("item") as? AttendanceHistoryData


        binding.calendarView.setOnDateChangedListener(this)
        binding.calendarView.showOtherDates = MaterialCalendarView.SHOW_ALL


        val instance = LocalDate.now()
        binding.calendarView.setSelectedDate(instance)

        val min = LocalDate.of(instance.year - 1, Month.JANUARY, 1)
        val max = LocalDate.of(instance.year, Month.DECEMBER, 31)

        minimumDate = (instance.year - 1).toLong()
        maximumDate = (instance.year).toLong()


        val formatDate = formatDateString(instance.toString(),"yyyy-MM-dd","dd/MM/yyyy")
        binding.textViewDate.text = formatDate

        binding.calendarView.state()?.edit()?.setMinimumDate(min)?.setMaximumDate(max)?.commit()

        binding.calendarView.addDecorators(
                HighlightWeekendsDecorator()
        )
        binding.calendarView.invalidateDecorators()


        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.add(Calendar.YEAR, -1)
        val previous = calendar.time

        viewModel.strStartDate = previous.toString("yyyy-MM-dd")
        viewModel.strEndDate = today.toString("yyyy-MM-dd")

        configLegends()

        viewModel.getLoggedInUser().observe(this, Observer { user ->


            if (user != null) {
                viewModel.getAttendanceDetails(item?.UserID)
            }

        })
        binding.textViewDate.setOnClickListener {
            val c = Calendar.getInstance()
            val c1 = Calendar.getInstance()

            val mYear = c.get(Calendar.YEAR)
            val mMonth = c.get(Calendar.MONTH)
            val mDay = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this,
                    { view, year, monthOfYear, dayOfMonth ->

                        val selectedDate = CalendarDay.from(year, monthOfYear + 1, dayOfMonth)
                        binding.calendarView.selectedDate = selectedDate
                        binding.calendarView.setCurrentDate(selectedDate, true)


                        val formattedSelectedDate = formatDateString(selectedDate.date.toString(),"yyyy-MM-dd","dd/MM/yyyy")
                        binding.textViewDate.text = formattedSelectedDate

                        Log.d("diff date - -- ", "" + selectedDate + " -- - " + CalendarDay.today())
                        attendanceData?.let { setAddress(it, selectedDate.date) }
                    }, mYear, mMonth, mDay)


            Log.d("min max date - - - ", " -- - " + (mYear - 2).toLong() + " - - - " + (mYear).toLong())

            c.set(Calendar.MONTH, 0)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.YEAR, mYear - 1)
            datePickerDialog.datePicker.minDate = c.timeInMillis

            c1.set(Calendar.MONTH, 11)
            c1.set(Calendar.DAY_OF_MONTH, 31)
            c1.set(Calendar.YEAR, mYear)
            datePickerDialog.datePicker.maxDate = c1.timeInMillis


            datePickerDialog.show()
        }
    }


    private fun configLegends() {


        val attInfo = arrayListOf<String>()
        attInfo.add("Present")
        attInfo.add("Absent")
        attInfo.add("Half Day")
        attInfo.add("Missed Punch")
        attInfo.add("Holiday")

        val colors = arrayListOf<HexColors>()
        colors.add(HexColors("#228B22")) //Green
        colors.add(HexColors("#AD160F")) //Red
        colors.add(HexColors("#2577E7")) //Blue
        colors.add(HexColors("#ff9922")) //Yellow
        colors.add(HexColors("#ffaa66cc")) //Black

        adapter = CustomLegendAdapter(colors, attInfo)
        binding.recyclerViewLegends.adapter = adapter
        binding.recyclerViewLegends.layoutManager = GridLayoutManager(this, 3)


        binding.textViewEmployeeName.text = item?.Username ?: "-"


        val avatar = if (item?.GenderId == 1) R.drawable.male_avatar else R.drawable.female_avatar

        Glide.with(this)
                .load(item?.ProfilePhoto)
                .fitCenter()
                .placeholder(avatar)
                .into(binding.imgProfilePic)

    }


    private fun setAddress(prevAttendanceData: List<AttendanceDetailsData?>, selectedDate: LocalDate) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val formattedDate = formatter.format(parser.parse(selectedDate.toString()))
        Log.d("formattedDate - - - ", "" + formattedDate)


        val prevAttendanceObj = prevAttendanceData.filter {

            (it?.DisplayDate ?: "-").contains(formattedDate.toString())
        }
        Log.d("prev - - - ", "" + prevAttendanceObj)

        if (prevAttendanceObj.isNullOrEmpty()) {
            binding.txtPunchTime.text = "-"
            binding.txtPunchOutTime.text = "-"

            binding.txtPunchLocation.text = "-"
            binding.txtPunchOutLocation.text = "-"


        } else {
            binding.txtPunchTime.text = prevAttendanceObj[0]?.FirstIn
            binding.txtPunchOutTime.text = prevAttendanceObj[0]?.LastOut


            //Check First PunchType
            if (prevAttendanceObj[0]?.FirstInPunchType.equals(TAG_PUNCH_TYPE_MOBILE, ignoreCase = true)) {
                binding.txtPunchLocation.text = getAddressFromLatLong(this, prevAttendanceObj[0]?.FirstInLatitude?.toDoubleOrNull()
                        ?: 0.0,
                        prevAttendanceObj[0]?.FirstInLongitude?.toDoubleOrNull() ?: 0.0)
            } else {
                binding.txtPunchLocation.text = prevAttendanceObj[0]?.FirstInLocation
            }


            //Check Last PunchType
            if (prevAttendanceObj[0]?.LastOutPunchType.equals(TAG_PUNCH_TYPE_MOBILE, ignoreCase = true)) {
                binding.txtPunchOutLocation.text = getAddressFromLatLong(this, prevAttendanceObj[0]?.LastOutLatitude?.toDoubleOrNull()
                        ?: 0.0,
                        prevAttendanceObj[0]?.LastOutLongitude?.toDoubleOrNull() ?: 0.0)
            } else {
                binding.txtPunchOutLocation.text = prevAttendanceObj[0]?.LastOutLocation
            }

            binding.txtWorkingHrs.text = prevAttendanceObj[0]?.TotalHours + " of working hours"
        }


    }

    override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
        binding.calendarView.invalidateDecorators()
        Log.d("---", "----")


        val formatDate = formatDateString(date.date.toString(),"yyyy-MM-dd","dd/MM/yyyy")
        binding.textViewDate.text = formatDate

        //  - - - - - - Hit api for current attendance data - - - - - -
        Log.d("diff date - -- ", "" + date + " -- - " + CalendarDay.today())
        attendanceData?.let {

            setAddress(it, date.date)
            binding.scrollView.post {
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private suspend fun setDecorator() {
        //CPU intensive task so run it on the Default dispatcher
        withContext(Dispatchers.Default) {
            val calendarDays = ArrayList<CalendarObj>()

            if ((attendanceData?.size ?: 0) > 0) {
                for (i in 0..(attendanceData?.size!! - 1)) {
                    val obj = CalendarObj()

                    var current: String?
                    current = attendanceData!![i].DisplayDate
                    Log.d("Current - - - ", "____" + current)

                    if (current.isNullOrEmpty()) {
                        current = "10-01-2020"
                    }

                    val dateParts = current.split("-")


                    obj.days = dateParts[0].toInt()
                    obj.month = dateParts[1].toInt()
                    obj.year = dateParts[2].toInt()
                    obj.status = attendanceData!![i].status
                    Log.d("date obj  - - - -", "- - - " + obj.days + "- - - -" + obj.month + "- - - -" + obj.year + " - - - - " + obj.status)
                    calendarDays.add(obj)
                }
            }


            val calAbsent: MutableList<CalendarDay> = ArrayList()
            val calPresent: MutableList<CalendarDay> = ArrayList()
            val halfDay: MutableList<CalendarDay> = ArrayList()
            val holiday: MutableList<CalendarDay> = ArrayList()
         //   val weekend: MutableList<CalendarDay> = ArrayList()
            val punchMiss: MutableList<CalendarDay> = ArrayList()

            //Weekend
            //Absent
            //Present
            //Holiday
            //Checkout Missing
            //Half Day

            for (i in calendarDays.indices) {
                when {
                    calendarDays[i].status.equals(TAG_ABSENT, ignoreCase = true) -> {
                        calAbsent.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                    calendarDays[i].status.equals(TAG_PRESENT, ignoreCase = true) -> {
                        calPresent.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                    calendarDays[i].status.equals(TAG_HALF_DAY, ignoreCase = true) -> {
                        halfDay.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                    calendarDays[i].status.equals(TAG_HOLIDAY, ignoreCase = true) -> {
                        holiday.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                    calendarDays[i].status.equals(TAG_WEEKEND, ignoreCase = true) -> {
                       // weekend.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                    calendarDays[i].status.equals(TAG_CHECKOUT_MISSING, ignoreCase = true) -> {
                        punchMiss.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                    else -> {
                      holiday.add(CalendarDay.from(LocalDate.of(calendarDays[i].year, calendarDays[i].month, calendarDays[i].days)))
                    }
                }
            }


            // Since we are updating the UI, do the operation on the Main dispatcher
            withContext(Dispatchers.Main) {
                binding.calendarView.addDecorator(EventDecorator(this@AttendanceReportActivity, calPresent, 0))
                binding.calendarView.addDecorator(EventDecorator(this@AttendanceReportActivity, calAbsent, 1))
                binding.calendarView.addDecorator(EventDecorator(this@AttendanceReportActivity, halfDay, 2))
                binding.calendarView.addDecorator(EventDecorator(this@AttendanceReportActivity, punchMiss, 3))
                binding.calendarView.addDecorator(EventDecorator(this@AttendanceReportActivity, holiday, 4))
//                binding.calendarView.addDecorator(EventDecorator(this@AttendanceReportActivity, weekend, 5))
            }
        }

    }

    override fun onStarted(callFrom: String) {
        binding.progressBar.start()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {

        binding.progressBar.stop()

        Coroutines.main {

            attendanceData = _object as? List<AttendanceDetailsData>

            if (attendanceData.isNullOrEmpty()) {
                binding.rootLayout.snackbar("No data found")
                return@main
            }

            setDecorator()
            attendanceData?.let { setAddress(it, CalendarDay.today().date) }
        }
    }

    override fun onError(message: String, callFrom: String, isNetworkError: Boolean) {
        binding.progressBar.stop()
        binding.rootLayout.snackbar(message)
    }

    override fun onValidationError(message: String, callFrom: String) {

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val TAG_ABSENT = "ABSENT"
        const val TAG_PRESENT = "PRESENT"
        const val TAG_HALF_DAY = "HALF DAY"
        const val TAG_HOLIDAY = "HOLIDAY"
        const val TAG_WEEKEND = "WEEKEND"
        const val TAG_CHECKOUT_MISSING = "CHECKOUT MISSING"

        const val TAG_PUNCH_TYPE_MOBILE = "MOBILE"
        const val TAG_CHECKOUT_BIOMETRIC = "BIOMETRIC"
    }
}


