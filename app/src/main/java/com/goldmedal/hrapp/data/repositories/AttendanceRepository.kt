package com.goldmedal.hrapp.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.goldmedal.hrapp.data.db.AppDatabase
import com.goldmedal.hrapp.data.db.entities.GetAllAttendanceData
import com.goldmedal.hrapp.data.db.entities.GetCurrentAttendanceData
import com.goldmedal.hrapp.data.network.GlobalConstant
import com.goldmedal.hrapp.data.network.MyApi
import com.goldmedal.hrapp.data.network.SafeApiRequest
import com.goldmedal.hrapp.data.network.responses.*
import com.goldmedal.hrapp.data.preferences.PreferenceProvider
import com.goldmedal.hrapp.util.ApiException
import com.goldmedal.hrapp.util.Coroutines
import com.goldmedal.hrapp.util.NoInternetException
import com.goldmedal.hrapp.util.getIPAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.temporal.ChronoUnit
import java.net.SocketTimeoutException
import javax.inject.Inject

private const val MINIMUM_INTERVAL = 5

class AttendanceRepository @Inject constructor(private val api: MyApi,
                                               private val db: AppDatabase,
                                               private val prefs: PreferenceProvider) : SafeApiRequest() {

    private val attendanceData = MutableLiveData<List<GetAllAttendanceData>>()

    init {
        attendanceData.observeForever { saveAllAttendanceData(it) }
    }

    // - - -  - for all attendance - - - - - -
    private suspend fun fetchAllAttendanceData(userId: Int?, fromDate: String, toDate: String) {

        val lastSavedAt = prefs.getLastSavedAt()
        withContext(Dispatchers.Main) {

            if (lastSavedAt == null || isFetchNeeded(LocalDateTime.parse(lastSavedAt))) {
                try {
                    val response = apiRequest { api.getAllAttendanceData(userId!!, fromDate, toDate, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET) }
                    response.attendanceData?.let {
                        attendanceData.postValue(it)
                    }
                } catch (e: ApiException) {
                } catch (e: NoInternetException) {
                    print("Internet not available")
                } catch (e: SocketTimeoutException) {
                    print("Internet not available")
                }
            }
        }
    }

    suspend fun getAllAttendanceData(userId: Int?, strStartDate: String, strEndDate: String): LiveData<List<GetAllAttendanceData>> {
        return withContext(Dispatchers.Main) {
            fetchAllAttendanceData(userId, strStartDate, strEndDate)
            retrieveAllAttendanceData()
        }


    }


    private fun isFetchNeeded(savedAt: LocalDateTime): Boolean {
        return ChronoUnit.HOURS.between(savedAt, LocalDateTime.now()) > MINIMUM_INTERVAL
    }

    fun saveAllAttendanceData(attendanceData: List<GetAllAttendanceData>) {
        prefs.saveLastSavedAt(LocalDateTime.now().toString())
        Coroutines.io {
            removeAllAttendanceData()
            db.getAllAttendanceDao().insertAttendanceData(attendanceData)
        }

    }


    fun retrieveAllAttendanceData() = db.getAllAttendanceDao().getAttendanceData()


    suspend fun punchAttendance(intUserId: Int, strComment: String, strImage: String, strDeviceId: String, strLatitude: String, strLongitude: String,strLocation: String): InsertPunchResponse {
        return apiRequest {
            api.insertPunchAttendance(intUserId, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET
                    , strImage, strComment, getIPAddress(true), strLatitude, strLongitude, strDeviceId, GlobalConstant.PUNCH_TYPE, "ANDROID",strLocation)
        }
    }


    suspend fun removeAllAttendanceData() = db.getAllAttendanceDao().removeAttendanceData()

    // - - - - - -  for current attendance -  - - - - - - - - -


    //FROM DATE AND TO DATE ARE SAME
    suspend fun currentAttendanceData(userId: Int, strDate: String): GetCurrentAttendanceResponse {
        return apiRequest {
            api.getCurrentAttendanceData(userId, strDate, strDate, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }

    // - - - - - -  for team attendance details-  - - - - - - - - -
    suspend fun getAttendanceDetails(userId: Int, strStartDate: String, strEndDate: String): AttendanceDetailsResponse {
        return apiRequest {
            api.getAttendanceDetails(userId, strStartDate, strEndDate, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }


    suspend fun getAttendanceSummary(userId: Int, strStartDate: String, strEndDate: String): AttendanceSummaryResponse {
        return apiRequest {
            api.getAttendanceSummary(userId, strStartDate, strEndDate, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }



    // - - - - - -   for Applying Attendance Regularization  - - - - - - - - - - -
    suspend fun insertAttendanceRegularization(userId: Int, punchDate: String,oldPunchIn:String,oldPunchOut:String,newPunchIn:String,newPunchOut:String,
                                               reason:String,remark:String): DefaultMessageResponse {
        return apiRequest {
            api.insertAttendanceRegularization(userId, punchDate, oldPunchIn,oldPunchOut,newPunchIn,newPunchOut,reason,remark, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }
    // - - - - - -  API for Regularization Attendance Approval List - - - - - - - - - - -
    suspend fun getAttendanceRegularizeForApproval(userId: Int): RegularizeAttendanceApprovalResponse {
        return apiRequest {
            api.getAttendanceRegularizeForApproval(userId, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }



//    // - - - - - -  API for Regularization Attendance Approval List - - - - - - - - - - -
//    suspend fun getAttendanceRegularizeForApproval(userId: Int): RegularizeAttendanceApprovalResponse {
//        return apiRequest {
//            api.getAttendanceRegularizeForApproval(userId, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
//        }
//    }

    // - - - - - -  API for Regularization Attendance History  - - - - - - - - - - -
    suspend fun getAttendanceRegularizeList(userId: Int): AttendanceRegularizationListResponse {
        return apiRequest {
            api.getAttendanceRegularizeList(userId, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }


    // - - - - - -  API for accepted/rejected records of  Regularization Attendance by admin - - - - - - - - - - -
    suspend fun getAttendanceRegularizeByUser(userId: Int): AttendanceRegularizeByUserResponse {
        return apiRequest {
            api.getAttendanceRegularizeByUser(userId, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }








    // - - - - - -  API for Accept/Reject Regularization Attendance  - - - - - - - - - - -
    suspend fun insertApprovedDisapprovedAttendanceReg(userId: Int,uniqueId:Int, approvalType: Int): DefaultMessageResponse {
        return apiRequest {
            api.insertApprovedDisapprovedAttendanceReg(userId,uniqueId, "",approvalType, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }


    // - - - - - -  API for Cancel Regularization Request  - - - - - - - - - - -
    suspend fun cancelRegularizationRequest(userId: Int,uniqueId:Int): DefaultMessageResponse {
        return apiRequest {
            api.cancelRegularizationRequest(userId,uniqueId, GlobalConstant.CLIENT_ID, GlobalConstant.CLIENT_SECRET)
        }
    }





    suspend fun saveCurrentAttendanceData(attendanceData: List<GetCurrentAttendanceData>) = db.getCurrentAttendanceDao().insertCurrentAttendanceData(attendanceData)

    fun retrieveCurrentAttendanceData() = db.getCurrentAttendanceDao().getCurrentAttendanceData()

    suspend fun removeCurrentAttendanceData() = db.getCurrentAttendanceDao().removeCurrentAttendanceData()

    fun getUser() = db.getUserDao().getUser()
}