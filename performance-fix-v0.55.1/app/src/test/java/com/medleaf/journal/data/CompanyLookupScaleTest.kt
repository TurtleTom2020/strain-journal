package com.medleaf.journal.data

import org.junit.Assert.*
import org.junit.Test

class CompanyLookupScaleTest {
    @Test fun repeatedCatalogueNamesKeepSameCompanySuggestions() {
        val companies = listOf("Carmel", "Holly Mood", "3PL Ventures", "Thunderchild", "")
        val repeated = List(1056) { companies[it % companies.size] }
        assertEquals(Cultivators.all(companies), Cultivators.all(repeated))
        assertEquals(Cultivators.search("holly", companies), Cultivators.search("holly", repeated))
        assertEquals("HollyMood", Cultivators.resolve("Hollywood", repeated))
    }
    @Test fun indexedAliasesRetainAccentsAndCompanySuffixes() {
        assertEquals("Plantations Cérès", CultivatorBrands.resolve("Plantations Ceres Ltd")?.name)
        assertEquals("HollyMood", CultivatorBrands.resolve("Holly Mood Limited")?.name)
        assertEquals("N!CE", CultivatorBrands.resolve("NICE")?.name)
        assertNull(CultivatorBrands.resolve("Unknown supplier"))
        assertNull(CultivatorBrands.resolve(""))
    }
}
