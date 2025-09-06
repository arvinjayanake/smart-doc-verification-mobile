package com.arvin.smartdocmobile.model

data class VerifyResponse(
    val driving_licence_no: String?,
    val is_driving_licence: Boolean,
    val is_new_nic: Boolean,
    val is_old_nic: Boolean,
    val is_passport: Boolean,
    val nic_no: String?,
    val passport_no: String?
)