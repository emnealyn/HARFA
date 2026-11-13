package com.example.harpapp.data.repository

import com.example.harpapp.data.catalog.TuningCatalogDataSource
import com.example.harpapp.data.catalog.dto.TuningCatalogDto
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.TuningType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningRepositoryImplTest {

    private class FakeTuningCatalogDataSource(
        private val tunings: List<TuningCatalogDto>
    ) : TuningCatalogDataSource {
        override fun getTunings(): List<TuningCatalogDto> = tunings
        override fun getTuning(tuningId: String): TuningCatalogDto? = tunings.firstOrNull { it.id == tuningId }
    }

    private val warnings = mutableListOf<String>()

    private fun levers(vararg changed: Pair<String, String>): Map<String, String> =
        NoteName.entries.associate { it.name to "NATURAL" } + changed

    private fun repository(vararg tunings: TuningCatalogDto) =
        TuningRepositoryImpl(FakeTuningCatalogDataSource(tunings.toList())) { warnings += it }

    @Test
    fun `poprawny wpis katalogu mapuje sie na strojenie domenowe`() = runTest {
        val repo = repository(TuningCatalogDto("g-major", "G-dur", "MAJOR", levers("F" to "SHARP")))

        val tuning = repo.getTuning("g-major")!!

        assertEquals("G-dur", tuning.name)
        assertEquals(TuningType.MAJOR, tuning.type)
        assertEquals(Accidental.SHARP, tuning.levers[NoteName.F])
        assertEquals(NoteName.entries.toSet(), tuning.levers.keys)
        assertEquals(Accidental.SHARP, tuning.toInstrumentState().lever(NoteName.F))
    }

    @Test
    fun `nieznane id zwraca null bez ostrzezenia`() = runTest {
        assertNull(repository().getTuning("missing"))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `bledny typ zwraca null i loguje ostrzezenie`() = runTest {
        val repo = repository(TuningCatalogDto("x", "X", "LYDIAN", levers()))

        assertNull(repo.getTuning("x"))
        assertEquals(1, warnings.size)
    }

    @Test
    fun `bledna pozycja dzwigni zwraca null`() = runTest {
        val repo = repository(TuningCatalogDto("x", "X", "MAJOR", levers("F" to "DOUBLE_SHARP")))

        assertNull(repo.getTuning("x"))
    }

    @Test
    fun `niepelna mapa dzwigni zwraca null`() = runTest {
        val repo = repository(TuningCatalogDto("x", "X", "MAJOR", levers() - "B"))

        assertNull(repo.getTuning("x"))
    }
}
