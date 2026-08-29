package com.goldmedal.hrapp.data.network


object GlobalConstant {

    const val TYPE_NO_DATA = 10000

    const val BASE_URL = "https://api.goldmedalindia.in/api/"
    const val BASE_NET_URL = "https://api.goldmedalindia.net/api/"
    const val TEST_BASE_URL = "https://goldapi-uat.goldmedalindia.in/api/hrm/v1/"
    const val HRM_BASE_URL = "https://goldapi.goldmedalindia.in/api/hrm/v1.0/"
    const val HRM_BASE_NET_URL = "https://goldapi.goldmedalindia.net/api/hrm/v1.0/"
    private const val IS_LIVE = true // todo - change value for live
    var BASE_URL_MAIN = if(IS_LIVE) HRM_BASE_URL else TEST_BASE_URL

    const val SUCCESS_CODE = "200"
    const val NO_DATA_CODE = "2002"
    const val PUNCH_TYPE = "MOBILE"
    const val CLIENT_ID = "HRM_347362"
    const val CLIENT_SECRET =
        "8njmLe/g9ih+6wLxYx/O4D56N+1q7sR71CzZb4uJLhIeFQNiIzMnxm1kZAIUHyxtwM+CIkYw9ct7CCebDTIQPh9oyOBPz/bpdf+7oM6cU="
    const val APP_TYPE = "hrm"

    const val IMAGE_DIRECTORY = "/hrapp"

    const val ATTENDANCE_SUMMARY = "ATTENDANCE_SUMMARY"

    const val COMMON_IMAGE_UPLOAD_API = "common_image_upload"
    const val ADD_COMPANY_DETAILS_API = "add_company_details"
    const val GET_COMPANY_DETAILS_API = "get_company_details"
    const val DELETE_COMPANY_DETAILS_API = "delete_company_details"
    const val BLOCK_MONTH_DATE_API = "block_month_date"
}
