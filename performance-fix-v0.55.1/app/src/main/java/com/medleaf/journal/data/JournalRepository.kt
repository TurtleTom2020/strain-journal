package com.medleaf.journal.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.text.Normalizer

class JournalRepository(private val database: AppDatabase, private val context: Context) {
    private val dao = database.journalDao()
    private val bundledCatalogue by lazy(::starterCatalogue)
    val reviews = dao.reviews()
    val sessions = dao.sessions()
    // Keep the APK's catalogue available even before (or if) the Room seed completes.
    // Persisted custom/online records replace matching bundled IDs without hiding fallback names.
    val catalogue = dao.catalogue().map { persisted ->
        mergeCatalogueLayers(bundledCatalogue, persisted)
    }.flowOn(Dispatchers.Default)
    fun review(id: Long) = dao.review(id)
    suspend fun initialiseCatalogue() {
        // Upserting stable bundled IDs safely adds catalogue improvements to existing installs.
        kotlinx.coroutines.withContext(Dispatchers.Default) { dao.upsertCatalogue(bundledCatalogue) }
    }
    suspend fun save(review: Review): Long {
        rememberCustomProduct(review)
        return dao.insertReview(review)
    }
    suspend fun update(review: Review) {
        rememberCustomProduct(review)
        dao.updateReview(review)
    }
    suspend fun saveSession(session: MedicationSession) = dao.insertSession(session)
    suspend fun allReviews() = dao.allReviews()
    suspend fun allSessions() = dao.allSessions()
    suspend fun allCatalogue() = dao.allCatalogue()
    suspend fun mergeCatalogue(products: List<CatalogueProduct>) = dao.upsertCatalogue(products)
    suspend fun searchOnlineCatalogue(query: String) = OnlineCatalogue.search(query)
    suspend fun cacheCatalogueProduct(product: CatalogueProduct) = dao.upsertCatalogue(listOf(product.copy(updatedAt = System.currentTimeMillis())))
    suspend fun importCatalogue(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Could not open catalogue file")
        val products = parseCatalogue(text)
        require(products.isNotEmpty()) { "No valid products found" }
        dao.upsertCatalogue(products)
        return products.size
    }

    private suspend fun rememberCustomProduct(review: Review) {
        val key = normalise(review.strainName)
        if (key.isBlank()) return
        val known = CatalogueSearch.exact(dao.allCatalogue(), review.strainName, review.brand, review.cultivator)?.let {
            normalise(Cultivators.canonical(it.brand)) == normalise(Cultivators.canonical(review.brand)) &&
                normalise(Cultivators.canonical(it.producer)) == normalise(Cultivators.canonical(review.cultivator))
        } == true
        if (!known) dao.upsertCatalogue(listOf(CatalogueProduct(
            id = "custom-$key-${normalise(review.brand)}-${normalise(review.cultivator).ifBlank { "unspecified" }}", productName = review.strainName.trim(), cultivarName = review.strainName.trim(),
            brand = review.brand.trim(), producer = review.cultivator.trim(), cultivarType = review.cultivarType,
            commonTerpenes = review.terpenes, flavours = listOf(review.aroma, review.flavour).filter(String::isNotBlank).joinToString(", "),
            sourceName = "Your catalogue", isCustom = true
        )))
    }
    suspend fun importReviews(reviews: List<Review>, sessions: List<MedicationSession>): Pair<Int, Int> = database.withTransaction {
        val existing = dao.allReviews()
        val ids = mutableMapOf<Long, Long>()
        val keys = existing.associateBy { it.createdAt to it.strainName.trim().lowercase() }.toMutableMap()
        var addedReviews = 0
        for (review in reviews) {
            val key = review.createdAt to review.strainName.trim().lowercase()
            val match = keys[key]
            if (match != null) ids[review.id] = match.id
            else {
                val newId = dao.insertReview(review.copy(id = 0))
                ids[review.id] = newId
                keys[key] = review.copy(id = newId)
                addedReviews++
            }
        }
        val sessionKeys = dao.allSessions().map { Triple(it.takenAt, it.strainName, it.notes) }.toMutableSet()
        var addedSessions = 0
        for (session in sessions) {
            val key = Triple(session.takenAt, session.strainName, session.notes)
            if (sessionKeys.add(key)) {
                dao.insertSession(session.copy(id = 0, reviewId = session.reviewId?.let(ids::get)))
                addedSessions++
            }
        }
        addedReviews to addedSessions
    }

    internal fun starterCatalogue(): List<CatalogueProduct> {
        val detailedProducts = OfflineCatalogue.products()
        val genetics = StrainReferences.all().map { ref -> CatalogueProduct(
            id = "reference-${normalise(ref.name)}", productName = ref.name, cultivarName = ref.name,
            genetics = ref.genetics, commonTerpenes = ref.commonTerpenes.joinToString(", "),
            flavours = ref.flavours.joinToString(", "), sourceName = "Terpfolio genetics reference",
            lastVerified = "2026-09-14"
        ) }
        val ukProducts = listOf(
            CatalogueProduct("uk-upstate-alien-mintz", "Alien Mintz", "Alien Mintz", "Upstate", "Upstate", aliases = "Alien Mints, Upstate Alien Mintz", sourceName = "UK medical product reference", lastVerified = "2026-09-16"),
            CatalogueProduct("uk-all-nations-mac-doughnut", "Mac Doughnuts", "Mac Doughnuts", "All Nations", "All Nations", "MAC-1 × Birthday Cake", "Hybrid", "Myrcene, Limonene, Caryophyllene, Linalool", "", "Mac Doughnut, MAC Doughnut, All Nations GM T27", "GM T27", "UK medical product reference", "https://ukflwr.com/product/5592/all-nations-mac-daddy/", "2026-09-16"),
            CatalogueProduct("uk-green-karat-king-sherb", "King Sherb", "King Sherb", "Green Karat", "Green Karat", "OGKB V2.1 × Blue Sherbert", "Hybrid", "Myrcene, Limonene, Caryophyllene, Linalool", "Gas, berry, cream", "King Sherbet, GK King Sherb", "GK KS", "Green Karat product information; cultivar references", "", "2026-09-20"),
            CatalogueProduct("uk-green-karat-pave-s1", "Pavé S1", "Pavé S1", "Green Karat", "Green Karat", "Paris OG × The Menthol", "Hybrid", "Limonene, Caryophyllene, Linalool", "Menthol, gas, earthy", "Pave S1, GK Pave", "GK PV", "Terpfolio UK product reference", "", "2026-09-14"),
            CatalogueProduct("uk-green-karat-loud-cake", "Loud Cake", "Loud Cake", "Green Karat", "Green Karat", "4561 S1 × Pancakes", "Indica-dominant hybrid", "Limonene, Caryophyllene, Linalool, Humulene", "", "GK Loud Cake, Green Karat Loud Cake", "GK LC", "Green Karat UK product reference; established cultivar reference", "https://thecannabispages.com/single-tag/green-karat/", "2026-09-20"),
            CatalogueProduct("reference-rainbow-pave", "Rainbow Pave", "Rainbow Pavé", genetics = "Apples & Bananas × Pavé", cultivarType = "Hybrid", aliases = "Rainbow Pavé", sourceName = "Aeriz and breeder cultivar reference", sourceUrl = "https://strains.aeriz.com/strain/rainbow-pave/", lastVerified = "2026-09-20"),
            CatalogueProduct("product-carmel-masterpiece", "Masterpiece", "Masterpiece", "Carmel", "Carmel", "Cap Junky × Platinum", "Hybrid", aliases = "Carmel Masterpiece", sourceName = "Carmel cultivar reference", sourceUrl = "https://itsupinsmoke.com/product/carmel-masterpiece-lto/", lastVerified = "2026-09-20"),
            CatalogueProduct("uk-papers-yoda-cap", "Yoda Cap", "Yoda Cap", "Papers", "Papers", "Baby Yoda × Cap Junky", "Hybrid", "Caryophyllene, Limonene, Myrcene", "Berry, menthol, gas", "Yodacap", "PAP YC", "Terpfolio UK product reference", "", "2026-09-14"),
            CatalogueProduct("uk-mamedica-cap-junky", "Cap Junky", "Cap Junky", "Mamedica", "Mamedica", "Alien Cookies × Kush Mints #11", "Hybrid", "Caryophyllene, Limonene, Myrcene", "Diesel, pepper, fruit", "Capjunky", "", "Terpfolio UK product reference", "", "2026-09-14"),
            CatalogueProduct("uk-nice-lemon-cherry-gelato", "Lemon Cherry Gelato", "Lemon Cherry Gelato", "N!CE", "N!CE", "Sunset Sherbet × Girl Scout Cookies", "Hybrid", "Caryophyllene, Limonene, Linalool", "Cherry, lemon, sweet", "LCG", "", "Terpfolio UK product reference", "", "2026-09-14")
        )
        // Detailed product records are ordered first.  The legacy list is kept for stable IDs
        // and upgrade compatibility, while search ranking favours the richer branded records.
        val rich = detailedProducts + ukProducts + genetics
        val richNames = rich.flatMap { listOf(it.productName, it.cultivarName) }.map(::normalise).toSet()
        val nameIndex = BundledStrainIndex.products().filterNot { normalise(it.productName) in richNames }
        return (detailedProducts + ukProducts + genetics + nameIndex).distinctBy { it.id }
    }

    private fun parseCatalogue(text: String): List<CatalogueProduct> {
        val array = JSONArray(text)
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("productName").trim()
            if (name.isBlank()) return@mapNotNull null
            CatalogueProduct(
                id = item.optString("id").ifBlank { "import-${normalise(name)}-${normalise(item.optString("brand"))}" },
                productName = name, cultivarName = item.optString("cultivarName", name), brand = item.optString("brand"),
                producer = item.optString("producer"), genetics = item.optString("genetics"), cultivarType = item.optString("cultivarType", "Hybrid"),
                commonTerpenes = item.optString("commonTerpenes"), flavours = item.optString("flavours"), aliases = item.optString("aliases"),
                productCode = item.optString("productCode"), sourceName = item.optString("sourceName", "Imported catalogue"),
                sourceUrl = item.optString("sourceUrl"), lastVerified = item.optString("lastVerified"), updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun normalise(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "").lowercase().filter(Char::isLetterOrDigit)
}

internal fun mergeCatalogueLayers(
    bundled: List<CatalogueProduct>,
    persisted: List<CatalogueProduct>
): List<CatalogueProduct> = buildMap<String, CatalogueProduct> {
    bundled.forEach { put(it.id, it) }
    persisted.forEach { put(it.id, it) }
}.values.sortedBy { it.productName.lowercase() }
