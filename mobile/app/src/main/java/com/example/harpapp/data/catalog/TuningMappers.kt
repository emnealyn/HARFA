package com.example.harpapp.data.catalog

import com.example.harpapp.data.catalog.dto.TuningCatalogDto
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.Tuning
import com.example.harpapp.domain.model.TuningType

fun TuningCatalogDto.toDomain(): Tuning? {
    if (id.isBlank()) return null
    val tuningType = enumOrNull<TuningType>(type) ?: return null
    val parsedLevers = levers.entries.associate { (note, position) ->
        (enumOrNull<NoteName>(note) ?: return null) to (enumOrNull<Accidental>(position) ?: return null)
    }
    if (parsedLevers.keys != NoteName.entries.toSet()) return null
    return Tuning(id, name, tuningType, parsedLevers)
}

private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }
