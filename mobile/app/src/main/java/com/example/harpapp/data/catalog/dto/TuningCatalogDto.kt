package com.example.harpapp.data.catalog.dto

data class TuningCatalogDto(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val levers: Map<String, String> = emptyMap()
)
