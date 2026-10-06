package com.medleaf.journal.data

import com.medleaf.journal.R
import java.text.Normalizer

data class CultivatorBrand(
    val name: String,
    val logoResource: Int,
    val aliases: Set<String> = emptySet(),
    val artworkBounds: LogoArtworkBounds,
    /** Perceived-size correction applied to visible artwork, never to transparent canvas padding. */
    val detailScale: Float = 1f,
    val monochrome: Boolean = false
)

data class LogoArtworkBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** Single source of truth for bundled cultivator identities and their offline logos. */
object CultivatorBrands {
    val all = listOf(
        CultivatorBrand("Thunderchild", R.drawable.cultivator_thunderchild, setOf("Thunderchild Cultivation"), LogoArtworkBounds(0, 0, 3095, 483), monochrome = true),
        CultivatorBrand("Weeco", R.drawable.company_weeco, artworkBounds = LogoArtworkBounds(60, 48, 539, 171), monochrome = true),
        CultivatorBrand("Avextra", R.drawable.company_avextra, artworkBounds = LogoArtworkBounds(213, 90, 387, 130), monochrome = true),
        CultivatorBrand("Clearleaf", R.drawable.company_clearleaf, artworkBounds = LogoArtworkBounds(241, 68, 359, 152), monochrome = true),
        CultivatorBrand("IPS", R.drawable.company_ips, artworkBounds = LogoArtworkBounds(177, 20, 422, 200), monochrome = false),
        CultivatorBrand("Herdade das Barrocas", R.drawable.company_herdade_das_barrocas, aliases = setOf(), artworkBounds = LogoArtworkBounds(175, 20, 425, 200), monochrome = true),
        CultivatorBrand("PurpleFarm", R.drawable.company_purplefarm, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 71, 560, 148), monochrome = true),
        CultivatorBrand("LOT420", R.drawable.company_lot420, aliases = setOf("Lot420", "Lot420\u2122"), artworkBounds = LogoArtworkBounds(40, 40, 560, 180), monochrome = true),
        CultivatorBrand("Lumir", R.drawable.company_lumir, aliases = setOf(), artworkBounds = LogoArtworkBounds(192, 20, 407, 200), monochrome = false),
        CultivatorBrand("SafriCanna", R.drawable.company_safricanna, aliases = setOf(), artworkBounds = LogoArtworkBounds(157, 73, 443, 147), monochrome = true),
        CultivatorBrand("Dalgety", R.drawable.company_dalgety, aliases = setOf(), artworkBounds = LogoArtworkBounds(55, 20, 545, 200), monochrome = false),
        CultivatorBrand("Hilltop Leaf", R.drawable.company_hilltop_leaf, aliases = setOf(), artworkBounds = LogoArtworkBounds(44, 20, 556, 200), monochrome = true),
        CultivatorBrand("ANTG", R.drawable.company_antg, aliases = setOf("Australian Natural Therapeutics Group"), artworkBounds = LogoArtworkBounds(40, 55, 560, 164), monochrome = true),
        CultivatorBrand("EastCann", R.drawable.company_eastcann, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 48, 560, 172), monochrome = true),
        CultivatorBrand("Next Friday", R.drawable.company_next_friday, aliases = setOf(), artworkBounds = LogoArtworkBounds(141, 20, 458, 200), monochrome = true),
        CultivatorBrand("Origine Nature", R.drawable.company_origine_nature, aliases = setOf(), artworkBounds = LogoArtworkBounds(50, 20, 550, 200), monochrome = true),
        CultivatorBrand("Cake & Caviar", R.drawable.company_cake_caviar, aliases = setOf(), artworkBounds = LogoArtworkBounds(189, 20, 410, 200), monochrome = true),
        CultivatorBrand("Qwest", R.drawable.company_qwest, aliases = setOf(), artworkBounds = LogoArtworkBounds(63, 20, 537, 200), monochrome = true),
        CultivatorBrand("Rubicon Organics", R.drawable.company_rubicon_organics, aliases = setOf(), artworkBounds = LogoArtworkBounds(237, 85, 362, 135), monochrome = true),
        CultivatorBrand("Miracle Valley", R.drawable.company_miracle_valley, aliases = setOf("Miracle Valley Canada"), artworkBounds = LogoArtworkBounds(40, 44, 560, 175), monochrome = true),
        CultivatorBrand("GreenSeal", R.drawable.company_greenseal, aliases = setOf("GreenSeal Cannabis", "Greenseal"), artworkBounds = LogoArtworkBounds(209, 20, 390, 200), monochrome = true),
        CultivatorBrand("Cielo Verde", R.drawable.company_cielo_verde, aliases = setOf(), artworkBounds = LogoArtworkBounds(112, 20, 488, 200), monochrome = true),
        CultivatorBrand("KRFT", R.drawable.company_krft, aliases = setOf(), artworkBounds = LogoArtworkBounds(219, 75, 380, 145), monochrome = true),
        CultivatorBrand("Northern Green Canada", R.drawable.company_northern_green_canada, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 93, 560, 126), monochrome = true),
        CultivatorBrand("Sitka Legends", R.drawable.company_sitka_legends, aliases = setOf("Sitka"), artworkBounds = LogoArtworkBounds(165, 20, 435, 200), monochrome = true),
        CultivatorBrand("Four20 Pharma", R.drawable.company_four20_pharma, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 46, 560, 174), monochrome = false),
        CultivatorBrand("J.R. Strain", R.drawable.company_j_r_strain, aliases = setOf("JR Strain"), artworkBounds = LogoArtworkBounds(214, 20, 385, 200), monochrome = true),
        CultivatorBrand("Island Canna", R.drawable.company_island_canna, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 79, 560, 140), monochrome = true),
        CultivatorBrand("British Cannabis", R.drawable.company_british_cannabis, aliases = setOf(), artworkBounds = LogoArtworkBounds(150, 35, 450, 185), monochrome = false),
        CultivatorBrand("GrowLab Organics", R.drawable.company_growlab_organics, aliases = setOf(), artworkBounds = LogoArtworkBounds(196, 20, 404, 200), monochrome = true),
        CultivatorBrand("Canopy Growth", R.drawable.company_canopy_growth, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 29, 560, 190), monochrome = true),
        CultivatorBrand("Cantourage", R.drawable.company_cantourage, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 68, 560, 152), monochrome = true),
        CultivatorBrand("Avaay Medical", R.drawable.company_avaay_medical, aliases = setOf(), artworkBounds = LogoArtworkBounds(40, 23, 560, 196), monochrome = true),
        CultivatorBrand("Little Green Pharma", R.drawable.company_little_green_pharma, aliases = setOf(), artworkBounds = LogoArtworkBounds(188, 20, 411, 200), monochrome = true),
        CultivatorBrand("1964 Supply Co.", R.drawable.cultivator_1964_supply_co, setOf("1964", "1964 Supply"), LogoArtworkBounds(207, 20, 392, 200)),
        CultivatorBrand("4C Labs", R.drawable.cultivator_4c_labs, setOf("4 C Labs"), LogoArtworkBounds(40, 57, 560, 162)),
        CultivatorBrand("All Nations", R.drawable.cultivator_all_nations, artworkBounds = LogoArtworkBounds(137, 25, 462, 195)),
        CultivatorBrand("Aurora", R.drawable.cultivator_aurora, setOf("Aurora Medical"), LogoArtworkBounds(40, 65, 560, 155)),
        CultivatorBrand("Bedrocan", R.drawable.cultivator_bedrocan, artworkBounds = LogoArtworkBounds(40, 72, 560, 147)),
        CultivatorBrand("Big Narstie Medical", R.drawable.cultivator_big_narstie_medical, setOf("Big Narstie"), LogoArtworkBounds(48, 20, 552, 200)),
        CultivatorBrand("Carmel", R.drawable.cultivator_carmel, setOf("Carmel Cannabis"), LogoArtworkBounds(141, 20, 459, 200)),
        CultivatorBrand("Cheers Cannabis", R.drawable.cheers_cannabis, setOf("Cheers"), LogoArtworkBounds(40, 33, 560, 186)),
        CultivatorBrand("Craft Botanics", R.drawable.cultivator_craft_botanics, artworkBounds = LogoArtworkBounds(37, 73, 563, 146)),
        CultivatorBrand("Curaleaf", R.drawable.cultivator_curaleaf, artworkBounds = LogoArtworkBounds(40, 60, 560, 160)),
        CultivatorBrand("Doja", R.drawable.cultivator_doja, setOf("Doja Medical", "Doja Exclusive", "Doja Pak"), artworkBounds = LogoArtworkBounds(178, 25, 422, 195)),
        CultivatorBrand("Ghost Drops", R.drawable.cultivator_ghost_drops, artworkBounds = LogoArtworkBounds(153, 25, 447, 195), detailScale = 1.15f),
        CultivatorBrand("Glass Pharms", R.drawable.cultivator_glass_pharms, setOf("Glass Pharma", "Glass Pharmacy"), LogoArtworkBounds(91, 25, 509, 195)),
        CultivatorBrand("Green Karat", R.drawable.cultivator_green_karat, artworkBounds = LogoArtworkBounds(157, 25, 443, 195), detailScale = 1.30f),
        CultivatorBrand("Greyscales", R.drawable.cultivator_greyscales, setOf("Grayscales"), LogoArtworkBounds(40, 82, 560, 138)),
        CultivatorBrand("Grow Pharma", R.drawable.cultivator_grow_pharma, setOf("Grow"), LogoArtworkBounds(42, 20, 557, 200)),
        CultivatorBrand("HollyMood", R.drawable.cultivator_hollymood, setOf("Holly Mood", "Hollywood", "HollyMood Farms", "Hollymood Farms Co., Ltd."), LogoArtworkBounds(0, 0, 640, 87)),
        CultivatorBrand("HighGreens", R.drawable.cultivator_highgreens, setOf("High Greens"), LogoArtworkBounds(125, 23, 475, 197)),
        CultivatorBrand("Kasa Verde", R.drawable.cultivator_kasa_verde, artworkBounds = LogoArtworkBounds(203, 25, 397, 195)),
        CultivatorBrand("Mamedica", R.drawable.cultivator_mamedica, artworkBounds = LogoArtworkBounds(40, 75, 560, 144)),
        CultivatorBrand("N!CE", R.drawable.cultivator_nice, setOf("NICE", "Nice"), LogoArtworkBounds(77, 25, 523, 195), detailScale = .75f),
        CultivatorBrand("Papers Craft Co.", R.drawable.cultivator_papers_craft_co, setOf("Papers", "Papers Craft", "Papers Craft Co"), LogoArtworkBounds(108, 22, 492, 198)),
        CultivatorBrand("Peace Naturals", R.drawable.cultivator_peace_naturals, artworkBounds = LogoArtworkBounds(94, 20, 506, 200)),
        CultivatorBrand("Plantations Cérès", R.drawable.cultivator_plantations_ceres, setOf("Plantations Ceres", "Ceres", "Cérès"), LogoArtworkBounds(203, 10, 397, 210)),
        CultivatorBrand("Pure Sunfarms", R.drawable.cultivator_pure_sunfarms, setOf("Pure Sun Farms", "Pure Sunfarms"), LogoArtworkBounds(40, 71, 560, 149)),
        CultivatorBrand("SOMAÍ", R.drawable.cultivator_somai, setOf("SOMAI", "Somai Pharmaceuticals"), LogoArtworkBounds(213, 20, 387, 200)),
        CultivatorBrand("Tilray Medical", R.drawable.cultivator_tilray_medical, setOf("Tilray"), LogoArtworkBounds(40, 60, 560, 159)),
        CultivatorBrand("Upstate", R.drawable.cultivator_upstate, artworkBounds = LogoArtworkBounds(40, 55, 560, 164)),
        CultivatorBrand("Vasco Cannabis", R.drawable.cultivator_vasco_cannabis, setOf("Vasco"), LogoArtworkBounds(40, 35, 560, 185))
    )

    private fun normalise(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
        .replace("!", "i")
        .filter(Char::isLetterOrDigit)

    private val identitiesByAlias by lazy {
        buildMap<String, CultivatorBrand> {
            all.forEach { brand ->
                (brand.aliases + brand.name).forEach { alias ->
                    val key = normalise(alias)
                    if (key !in this) put(key, brand)
                }
            }
        }
    }

    fun resolve(value: String): CultivatorBrand? {
        val key = normalise(value)
        if (key.isBlank()) return null
        return identitiesByAlias[key]
            ?: identitiesByAlias[key.removeSuffix("limited")]
            ?: identitiesByAlias[key.removeSuffix("ltd")]
    }

    fun search(value: String, limit: Int = 8): List<CultivatorBrand> {
        val key = normalise(value)
        if (key.isBlank()) return all.take(limit)
        return all.mapNotNull { brand ->
            val candidates = (brand.aliases + brand.name).map(::normalise)
            val score = when {
                candidates.any { it == key } -> 0
                candidates.any { it.startsWith(key) } -> 1
                candidates.any { it.contains(key) } -> 2
                else -> return@mapNotNull null
            }
            score to brand
        }.sortedWith(compareBy<Pair<Int, CultivatorBrand>> { it.first }.thenBy { it.second.name })
            .map { it.second }.take(limit)
    }
}
