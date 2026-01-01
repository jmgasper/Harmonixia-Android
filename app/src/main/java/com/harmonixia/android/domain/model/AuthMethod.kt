package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AuthMethod {
    @SerialName("token")
    TOKEN,
    @SerialName("username_password")
    USERNAME_PASSWORD
}
