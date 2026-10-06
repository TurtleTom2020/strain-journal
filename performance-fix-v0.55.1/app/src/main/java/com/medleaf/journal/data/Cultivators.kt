package com.medleaf.journal.data

import java.text.Normalizer

object Cultivators {
    private fun normalise(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "").lowercase().replace("!", "i").filter(Char::isLetterOrDigit)

    fun canonical(value: String): String = CultivatorBrands.resolve(value)?.name ?: when (normalise(value)) {
        "hollywood", "hollymood", "hollymoodfarms" -> "HollyMood"
        "herdadedasbarrocas" -> "Herdade das Barrocas"
        else -> value.trim()
    }
    private val knownNames by lazy { CultivatorBrands.all.map { it.name } + CompanyNames.all }

    fun all(extraNames: List<String> = emptyList()): List<String> =
        (knownNames + extraNames.distinct())
            .map(::canonical).filter(String::isNotBlank).distinctBy(::normalise).sortedBy(String::lowercase)

    fun resolve(value: String, extraNames: List<String> = emptyList()): String? =
        all(extraNames).firstOrNull { normalise(it) == normalise(canonical(value)) }

    fun search(text: String, extraNames: List<String> = emptyList(), limit: Int = 8): List<String> {
        val query = normalise(canonical(text))
        return all(extraNames).filter { query.isBlank() || normalise(it).contains(query) }.take(limit)
    }
}
