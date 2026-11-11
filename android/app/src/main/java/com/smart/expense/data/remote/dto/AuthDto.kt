package com.smart.expense.data.remote.dto

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val preferredLanguage: String = "en"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val data: UserData? = null
)

data class UserData(
    val _id: String,
    val name: String,
    val email: String,
    val preferredLanguage: String,
    val token: String
)

