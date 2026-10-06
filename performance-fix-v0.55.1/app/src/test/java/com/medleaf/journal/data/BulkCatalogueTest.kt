package com.medleaf.journal.data

import org.junit.Assert.*
import org.junit.Test

class BulkCatalogueTest {
    @Test fun bulkListingsAreSearchableOfflineAcrossSuppliers() {
        val products = OfflineCatalogue.products()
        assertTrue(BulkCatalogue.products().size >= 900)
        assertTrue(products.size > 900)
        assertTrue(CatalogueSearch.search(products, "Scoops Gas Cream Cake", 30).any { it.brand == "Scoops" })
        assertTrue(CatalogueSearch.search(products, "Noidecs Humble Warrior", 30).any { it.productName == "Humble Warrior" })
        assertTrue(Cultivators.search("broken coast").contains("Broken Coast"))
        assertEquals(products.size, products.distinctBy { it.id }.size)
        println("CATALOGUE_COUNTS products=${products.size} brands=${products.map { Cultivators.canonical(it.brand) }.filter(String::isNotBlank).distinct().size} growers=${products.map { Cultivators.canonical(it.producer) }.filter(String::isNotBlank).distinct().size} logos=${CultivatorBrands.all.size}")
    }
    @Test fun batchCodesRemainAliasesAndVerifiedGrowersKeepTheirOwnProfile() {
        val products = OfflineCatalogue.products()
        val pie = CatalogueSearch.exact(products, "MA GGP T26", "Mamedica")!!
        assertEquals("Thunderchild Cultivation LP", pie.producer)
        val cap = CatalogueSearch.exact(products, "Cap Junky", "Mamedica", "Cheers Cannabis")!!
        assertEquals("Cheers Cannabis", cap.producer)
        assertEquals(listOf("Caryophyllene", "Limonene", "Myrcene"), cap.terpeneList())
        assertTrue(CatalogueSearch.search(products, "Hexacan HEXA02 T25 Lemon Jane", 30).any { it.productName == "Lemon Jane" })
        val marker = CatalogueSearch.exact(products, "Permanent Marker", "Doja", "Hollywood")!!
        assertEquals("HollyMood", marker.producer)
    }
    @Test fun bulkMergeKeepsExistingIdsAndSupplierFacts() {
        val original = CatalogueProduct(id = "custom-original", productName = "Lemon Jane", brand = "Hexacan", producer = "My recorded grower", commonTerpenes = "My recorded profile", genetics = "My recorded genetics", isCustom = true)
        val merged = BulkCatalogue.merge(listOf(original)).first { it.id == original.id }
        assertEquals(original.producer, merged.producer)
        assertEquals(original.commonTerpenes, merged.commonTerpenes)
        assertEquals(original.genetics, merged.genetics)
        assertTrue(merged.isCustom)
        assertTrue(merged.aliases.contains("HEXA02 T25"))
    }
    @Test fun newLogosAreSeparateFromTheProductBrand() {
        val review = Review(strainName = "Cap Junky", brand = "Mamedica", cultivator = "PurpleFarm", thoughts = "Keep my review")
        assertTrue(review.hasSeparateBrandAndCultivator())
        assertNotNull(CultivatorBrands.resolve("PurpleFarm"))
        assertNotNull(CultivatorBrands.resolve("Herdade das Barrocas"))
        assertNotEquals(CultivatorBrands.resolve("PurpleFarm")!!.logoResource, CultivatorBrands.resolve("Mamedica")!!.logoResource)
        assertEquals("Keep my review", review.thoughts)
        assertEquals(63, CultivatorBrands.all.size)
        assertNull(CultivatorBrands.resolve("Thunderchild Cultivation LP"))
    }
    @Test fun everyBulkRecordHasSourceAndGrowersHaveExplicitProvenance() {
        assertTrue(BulkCatalogue.products().all { it.sourceUrl.startsWith("https://") && it.lastVerified == "2026-10-06" && (it.producer.isBlank() || it.sourceName.contains("cultivation statement")) })
        assertEquals(BulkCatalogue.products().size, BulkCatalogue.products().distinctBy { it.id }.size)
    }
}
