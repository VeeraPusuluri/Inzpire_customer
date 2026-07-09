package com.inzpire.customer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val location: String? = null,
    val address: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("upi_id") val upiId: String? = null,
    @SerialName("bank_acct_name") val bankAcctName: String? = null,
    @SerialName("bank_acct_no") val bankAcctNo: String? = null,
    @SerialName("bank_ifsc") val bankIfsc: String? = null,
    @SerialName("kyc_status") val kycStatus: String? = null,
) {
    val hasPayoutDetails: Boolean get() = !upiId.isNullOrBlank() || !bankAcctNo.isNullOrBlank()
    val isKycVerified: Boolean get() = kycStatus == "verified"
}

/** Patch payload for `profiles` update — only the fields the profile screen can edit. */
@Serializable
data class ProfilePatch(
    val name: String? = null,
    val phone: String? = null,
    val location: String? = null,
    val address: String? = null,
    @SerialName("upi_id") val upiId: String? = null,
    @SerialName("bank_acct_name") val bankAcctName: String? = null,
    @SerialName("bank_acct_no") val bankAcctNo: String? = null,
    @SerialName("bank_ifsc") val bankIfsc: String? = null,
)
