package com.example.hospitalbedmanager.dataclasses

data class User(
    val username: String,
    val role: Role
)

enum class Role {
    NURSE,
    PORTER,
    DOCTOR
}
