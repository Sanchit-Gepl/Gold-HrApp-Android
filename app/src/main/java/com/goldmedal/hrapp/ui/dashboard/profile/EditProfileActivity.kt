package com.goldmedal.hrapp.ui.dashboard.profile

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.goldmedal.hrapp.BaseActivity
import com.goldmedal.hrapp.R
import com.goldmedal.hrapp.common.ApiStageListener
import com.goldmedal.hrapp.databinding.ActivityEditProfileBinding
import com.goldmedal.hrapp.util.snackbar
import com.goldmedal.hrapp.util.toast
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class EditProfileActivity : BaseActivity(), ApiStageListener<Any> {

    private lateinit var binding: ActivityEditProfileBinding


    private val viewModel: ProfileViewModel by viewModels()
    private var userId: Int? = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        viewModel.apiListener = this
        viewModel.getLoggedInUser().observe(this, Observer { user ->
            if (user != null) {
                userId = user.UserID

                binding.editTextPersonalEmail.setText(user.PersonalEmail)
                binding.editTextMobileNo.setText(user.MobileNumber)
//                binding.editTextHomeAddress.setText(user.HomeAddress)
                binding.editTextOfficeEmail.setText(user.Officialemail)

                binding.editTextFatherName.setText(user.FatherName)
                binding.editTextMotherName.setText(user.MotherName)

                if (!user.FatherDOB.isNullOrEmpty()) {
                    binding.placeholderFatherDOB.visibility = View.VISIBLE
                    binding.txtFatherDOB.text = user.FatherDOB
                    viewModel.strFatherDOB = user.FatherDOB
                }
                if (!user.MotherDOB.isNullOrEmpty()) {
                    binding.placeholderMotherDOB.visibility = View.VISIBLE
                    binding.txtMotherDOB.text = user.MotherDOB
                    viewModel.strMotherDOB = user.MotherDOB
                }
                //1 - Married
                //2 - Single


                if (user.MaritalID.equals("1")) {
                    binding.radioMarried.isChecked = true
                } else {
                    binding.radioUmmarried.isChecked = true
                }

                toggleChildrenInfo()
            }
        })

        clickListeners()
    }


    private fun clickListeners() {

        binding.txtFatherDOB.setOnClickListener {
            showDOBDialog(binding.txtFatherDOB)
        }

        binding.txtMotherDOB.setOnClickListener {
            showDOBDialog(binding.txtMotherDOB)
        }

        binding.txtSpouseDOB.setOnClickListener { showDOBDialog(binding.txtSpouseDOB) }
        binding.txtAnniversaryDate.setOnClickListener { showDOBDialog(binding.txtAnniversaryDate) }
        binding.txtFirstChildDOB.setOnClickListener { showDOBDialog(binding.txtFirstChildDOB) }
        binding.txtSecondChildDOB.setOnClickListener { showDOBDialog(binding.txtSecondChildDOB) }
        binding.txtThirdChildDOB.setOnClickListener { showDOBDialog(binding.txtThirdChildDOB) }


        binding.radioGroupMaritalStatus.setOnCheckedChangeListener { radioGroup, checkedId ->


            toggleChildrenInfo()


        }


        binding.buttonSubmit.setOnClickListener {
            viewModel.editProfile(userId = userId, strPersonalEmail = binding.editTextPersonalEmail.text.toString(), strOfficeEmail = binding.editTextOfficeEmail.text.toString(), strMobileNo = binding.editTextMobileNo.text.toString(),
                    strHomeAddress = binding.editTextOfficeEmail.text.toString(), strFatherName = binding.editTextFatherName.text.toString(), strMotherName = binding.editTextMotherName.text.toString(),
                    strSpouseName = binding.editTextSpouseName.text.toString(), strChildName1 = binding.editTextFirstChildName.text.toString(), strChildName2 = binding.editTextSecondChildName.text.toString(),
                    strChildName3 = binding.editTextThirdChildName.text.toString(), maritalStatus = if (binding.radioMarried.isChecked) 1 else 2)
        }
    }

    private fun toggleChildrenInfo() {
        viewModel.getLoggedInUser().observe(this, Observer { user ->
            if (user != null) {

                if (binding.radioMarried.isChecked) {


                    binding.layoutChildren.visibility = View.VISIBLE

                    if (!user.SpouseName.isNullOrEmpty()) {
                        binding.editTextSpouseName.setText(user.SpouseName)
                    }
                    if (!user.SpouseDOB.isNullOrEmpty()) {
                        binding.placeholderSpouseDOB.visibility = View.VISIBLE
                        binding.txtSpouseDOB.text = user.SpouseDOB
                        viewModel.strSpouseDOB = user.SpouseDOB
                    }

                    if (!user.AnniversaryDOB.isNullOrEmpty()) {
                        binding.placeholderAnniversaryDOB.visibility = View.VISIBLE
                        binding.txtAnniversaryDate.text = user.AnniversaryDOB
                        viewModel.strAnniversaryDate = user.AnniversaryDOB
                    }


//Children
                    if (!user.ChildName1.isNullOrEmpty()) {
                        binding.editTextFirstChildName.setText(user.ChildName1)
                    }
                    if (!user.ChildDOB1.isNullOrEmpty()) {
                        binding.placeholderFirstChildDOB.visibility = View.VISIBLE
                        binding.txtFirstChildDOB.text = user.ChildDOB1
                        viewModel.strChild1DOB = user.ChildDOB1
                    }


                    if (!user.ChildName2.isNullOrEmpty()) {
                        binding.editTextSecondChildName.setText(user.ChildName2)
                    }
                    if (!user.ChildDOB2.isNullOrEmpty()) {
                        binding.placeholderSecondChildDOB.visibility = View.VISIBLE
                        binding.txtSecondChildDOB.text = user.ChildDOB2
                        viewModel.strChild2DOB = user.ChildDOB2
                    }


                    if (!user.ChildName3.isNullOrEmpty()) {
                        binding.editTextThirdChildName.setText(user.ChildName3)
                    }
                    if (!user.ChildDOB3.isNullOrEmpty()) {
                        binding.placeholderThirdChildDOB.visibility = View.VISIBLE
                        binding.txtThirdChildDOB.text = user.ChildDOB3
                        viewModel.strChild3DOB = user.ChildDOB3
                    }
                } else {
                    binding.layoutChildren.visibility = View.GONE
                }


            }

        })
    }

    private fun showDOBDialog(textView: TextView) {
        val c = Calendar.getInstance()
        val mYear = c[Calendar.YEAR]
        val mMonth = c[Calendar.MONTH]
        val mDay = c[Calendar.DAY_OF_MONTH]

        val endDatePicker = DatePickerDialog(
                this, R.style.SpinnerDatePickerStyle,
                { view, year, monthOfYear, dayOfMonth ->

                    val formattedString = String.format(
                            Locale.getDefault(),
                            "%d/%d/%d",
                            dayOfMonth,
                            monthOfYear + 1,
                            year)

                    if (textView == binding.txtFatherDOB) {
                        binding.placeholderFatherDOB.visibility = View.VISIBLE
                        binding.txtFatherDOB.text = formattedString
                        viewModel.strFatherDOB = formattedString

                    } else if (textView == binding.txtMotherDOB) {
                        binding.placeholderMotherDOB.visibility = View.VISIBLE
                        binding.txtMotherDOB.text = formattedString
                        viewModel.strMotherDOB =
                                formattedString
                    } else if (textView == binding.txtSpouseDOB) {
                        binding.placeholderSpouseDOB.visibility = View.VISIBLE
                        binding.txtSpouseDOB.text = formattedString
                        viewModel.strSpouseDOB = formattedString
                    } else if (textView == binding.txtAnniversaryDate) {
                        binding.placeholderAnniversaryDOB.visibility = View.VISIBLE
                        binding.txtAnniversaryDate.text = formattedString
                        viewModel.strAnniversaryDate = formattedString
                    } else if (textView == binding.txtFirstChildDOB) {
                        binding.placeholderFirstChildDOB.visibility = View.VISIBLE
                        binding.txtFirstChildDOB.text = formattedString
                        viewModel.strChild1DOB = formattedString
                    } else if (textView == binding.txtSecondChildDOB) {
                        binding.placeholderSecondChildDOB.visibility = View.VISIBLE
                        binding.txtSecondChildDOB.text = formattedString
                        viewModel.strChild2DOB = formattedString
                    } else if (textView == binding.txtThirdChildDOB) {
                        binding.placeholderThirdChildDOB.visibility = View.VISIBLE
                        binding.txtThirdChildDOB.text = formattedString
                        viewModel.strChild3DOB = formattedString
                    }
                }, mYear, mMonth, mDay
        )
        endDatePicker.datePicker.maxDate = c.timeInMillis
        endDatePicker.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EditProfileActivity::class.java))
        }
    }

    override fun onStarted(callFrom: String) {
        binding.progressBar.start()
    }

    override fun onSuccess(_object: List<Any?>, callFrom: String) {
        binding.progressBar.stop()
        finish()
        toast("Your Profile was updated")


    }

    override fun onError(message: String, callFrom: String, isNetworkError: Boolean) {
        binding.progressBar.stop()
        binding.rootLayout.snackbar(message)
    }

    override fun onValidationError(message: String, callFrom: String) {
        binding.rootLayout.snackbar(message)
    }
}