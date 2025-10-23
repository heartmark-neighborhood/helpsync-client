package com.example.helpsync.data

data class Location(
    val latitude: Double,
    val longitude: Double
)

data class Evaluation(
    val rating: Int,
    val comment: String?
)
