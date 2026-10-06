package com.medleaf.journal.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.medleaf.journal.R
import com.medleaf.journal.BuildConfig
import com.medleaf.journal.MedLeafApplication
import com.medleaf.journal.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Calendar
import java.util.UUID

private enum class Destination(val route: String, val label: String) {
    JOURNAL("journal", "My Reviews"), GENETICS("genetics", "Genetics"), COMMUNITY("community", "Community"), DIARY("diary", "Journal")
}
private enum class ReviewView(val label: String) { DETAILS("Details"), TILES("Tiles"), LIST("List") }
private val bundledReportedProducts by lazy { OfflineCatalogue.products() }
private fun Review.reportedTerpNames(): List<String> =
    CatalogueSearch.exact(bundledReportedProducts, strainName, brand, cultivator)?.terpeneList()?.takeIf { it.isNotEmpty() }
        ?: StrainReferences.exact(strainName)?.commonTerpenes?.takeIf { it.isNotEmpty() }
        ?: terpenes.split(",").map(String::trim).filter(String::isNotBlank)
private fun terpReviews(reviews: List<Review>, terpene: String) = reviews.filter { review -> review.reportedTerpNames().any { it.equals(terpene, true) } }
private fun scoreTone(score: Int): Color = when {
    score >= 8 -> Color(0xFF4F674C)
    score >= 5 -> Color(0xFF53442C)
    else -> Color(0xFF484548)
}
@Composable private fun CultivatorIdentity(company: String, compact: Boolean = false, label: String = "CULTIVATOR", secondary: Boolean = false) {
    if (company.isBlank()) return
    CultivatorBrands.resolve(company)?.let { identity ->
        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 6.dp)) {
            if (!compact) Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (compact) {
                Image(rememberAsyncImagePainter(identity.logoResource), "${identity.name} logo", Modifier.width(96.dp).height(34.dp), contentScale = ContentScale.Fit, colorFilter = if (identity.name == "HollyMood" || identity.monochrome) ColorFilter.tint(MaterialTheme.colorScheme.onSurface) else null)
            } else {
                val context = LocalContext.current
                val bitmap = remember(identity.logoResource) { ImageBitmap.imageResource(context.resources, identity.logoResource) }
                val bounds = identity.artworkBounds
                // Size the cropped visible artwork, not the transparent 600x220 source canvas.
                // This keeps every brand on the same deterministic vertical rhythm.
                val preferredHeight = 44f * identity.detailScale * if (secondary) .78f else 1f
                val preferredWidth = preferredHeight * bounds.width / bounds.height
                val visibleWidth = preferredWidth.coerceAtMost(if (secondary) 112f else 210f)
                val visibleHeight = (visibleWidth * bounds.height / bounds.width).coerceAtMost(if (secondary) 44f else 56f)
                Image(
                    painter = BitmapPainter(bitmap, IntOffset(bounds.left, bounds.top), IntSize(bounds.width, bounds.height)),
                    contentDescription = "${identity.name} logo",
                    modifier = Modifier.width(visibleWidth.dp).height(visibleHeight.dp),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = if (identity.name == "HollyMood" || identity.monochrome) ColorFilter.tint(MaterialTheme.colorScheme.onSurface) else null
                )
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!compact) Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(company, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun CompanyPicker(value: String, onValueChange: (String) -> Unit, label: String, hint: String, extraNames: List<String> = emptyList()) {
    Field(value, onValueChange, label, hint)
    Spacer(Modifier.height(8.dp))
    val matches = remember(value, extraNames) { Cultivators.search(value, extraNames) }
    val exact = remember(value, extraNames) { Cultivators.resolve(value, extraNames) }
    if (exact == null && matches.isNotEmpty()) {
        Text("Known companies", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(matches, key = { it }) { company ->
                ElevatedCard(onClick = { onValueChange(company) }, modifier = Modifier.width(116.dp).height(52.dp), shape = RoundedCornerShape(11.dp)) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                        CultivatorIdentity(company, compact = true)
                    }
                }
            }
        }
    } else if (exact != null) Row(verticalAlignment = Alignment.CenterVertically) {
        CultivatorIdentity(exact, compact = true)
        Spacer(Modifier.width(8.dp)); Icon(Icons.Default.CheckCircle, "Recognised company", tint = MaterialTheme.colorScheme.primary)
    } else if (value.isNotBlank()) Text("Custom company — it will be saved using its name.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
private fun profileHeadline(reviews: List<Review>): String {
    val recorded = reviews.filter { it.reportedTerpNames().isNotEmpty() }
    if (recorded.isEmpty()) return "Add reviews with terpenes to see your patterns"
    val groups = recorded.flatMap { review -> review.reportedTerpNames().map { it.lowercase() to review } }
        .groupBy({ it.first }, { it.second })
    fun pretty(value: String) = value.replaceFirstChar(Char::uppercase)
    if (recorded.size >= 20) {
        val pairs = recorded.flatMap { review ->
            review.reportedTerpNames().map(String::lowercase).sorted().let { names ->
                names.flatMapIndexed { index, first -> names.drop(index + 1).map { second -> (first to second) to review } }
            }
        }.groupBy({ it.first }, { it.second })
        val best = pairs.filterValues { it.size >= 5 }.maxByOrNull { it.value.map(Review::overallRating).average() }
        if (best != null) return "Together, ${pretty(best.key.first)} + ${pretty(best.key.second)} average ${"%.1f".format(best.value.map(Review::overallRating).average())}/10 across ${best.value.size} reviews"
    }
    if (recorded.size >= 8) {
        val rated = groups.filterValues { it.size >= 3 }.entries.sortedByDescending { it.value.map(Review::overallRating).average() }.take(2)
        if (rated.isNotEmpty()) return "★ Highest-rated: ${rated.joinToString(" · ") { pretty(it.key) }} · avg ${"%.1f".format(rated.first().value.map(Review::overallRating).average())}/10 (${rated.first().value.size} reviews)"
    }
    val frequent = groups.entries.sortedByDescending { it.value.size }.take(2)
    return "Most reviewed: ${frequent.joinToString(" · ") { pretty(it.key) }}"
}

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {
    val reviews = repository.reviews.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sessions = repository.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val catalogue = repository.catalogue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var onlineCatalogueResults by mutableStateOf<List<CatalogueProduct>>(emptyList()); private set
    var onlineCatalogueSearching by mutableStateOf(false); private set
    var onlineCatalogueError by mutableStateOf<String?>(null); private set
    var onlineCatalogueQuery by mutableStateOf(""); private set
    var onlineCatalogueCompleted by mutableStateOf(false); private set
    private var onlineSearchJob: kotlinx.coroutines.Job? = null
    private var onlineSearchGeneration = 0L
    init { viewModelScope.launch { repository.initialiseCatalogue() } }
    fun review(id: Long) = repository.review(id)
    fun save(review: Review, done: () -> Unit) = viewModelScope.launch { repository.save(review); done() }
    fun saveUpdate(review: Review, done: () -> Unit) = viewModelScope.launch { repository.update(review); done() }
    fun save(session: MedicationSession, done: () -> Unit) = viewModelScope.launch { repository.saveSession(session); done() }
    fun toggleFavourite(review: Review) = viewModelScope.launch { repository.update(review.copy(isFavourite = !review.isFavourite)) }
    fun importCatalogue(uri: Uri, done: (Result<Int>) -> Unit) = viewModelScope.launch { done(runCatching { repository.importCatalogue(uri) }) }
    fun searchOnlineCatalogue(query: String) {
        val requested = query.trim()
        val generation = ++onlineSearchGeneration
        onlineSearchJob?.cancel()
        onlineSearchJob = viewModelScope.launch {
            onlineCatalogueResults = emptyList(); onlineCatalogueError = null
            onlineCatalogueSearching = false; onlineCatalogueCompleted = false; onlineCatalogueQuery = requested
            if (requested.isBlank()) return@launch
            kotlinx.coroutines.delay(350)
            onlineCatalogueSearching = true
            runCatching { repository.searchOnlineCatalogue(requested) }
                .onSuccess {
                    if (generation == onlineSearchGeneration) {
                        onlineCatalogueResults = it
                        onlineCatalogueCompleted = true
                        if (it.isNotEmpty()) repository.mergeCatalogue(it)
                    }
                }
                .onFailure {
                    if (generation == onlineSearchGeneration && it !is kotlinx.coroutines.CancellationException) {
                        onlineCatalogueError = "Online catalogue unavailable"
                        onlineCatalogueCompleted = true
                    }
                }
            if (generation == onlineSearchGeneration) onlineCatalogueSearching = false
        }
    }
    fun cacheCatalogueProduct(product: CatalogueProduct) = viewModelScope.launch { repository.cacheCatalogueProduct(product) }
}

private suspend fun importReviewPhoto(context: android.content.Context, source: Uri): Uri = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(source).orEmpty()
    val extension = when {
        mime.contains("png", true) -> "png"
        mime.contains("webp", true) -> "webp"
        mime.contains("heic", true) || mime.contains("heif", true) -> "heic"
        else -> "jpg"
    }
    val directory = File(context.filesDir, "review-photos").apply { mkdirs() }
    val destination = File(directory, "${UUID.randomUUID()}.$extension")
    try {
        context.contentResolver.openInputStream(source)?.use { input -> destination.outputStream().use(input::copyTo) }
            ?: error("Could not open selected photo")
        Uri.fromFile(destination)
    } catch (error: Exception) {
        destination.delete()
        throw error
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedLeafApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE) }
    var appTheme by remember { mutableStateOf(runCatching { AppTheme.valueOf(preferences.getString("theme", AppTheme.MIDNIGHT.name)!!) }.getOrDefault(AppTheme.MIDNIGHT)) }
    MedLeafTheme(appTheme) {
        MedLeafAppContent(appTheme) {
            appTheme = it
            preferences.edit().putString("theme", it.name).apply()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedLeafAppContent(appTheme: AppTheme, onThemeChange: (AppTheme) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as MedLeafApplication
    val vm: JournalViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>) = JournalViewModel(app.repository) as T
    })
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route.orEmpty()
    val roots = Destination.entries
    val reviewsGrid = rememberLazyGridState()
    val showReviewFab by remember { derivedStateOf { reviewsGrid.firstVisibleItemIndex > 1 } }
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val catalogue by vm.catalogue.collectAsStateWithLifecycle()
    var moreOpen by remember { mutableStateOf(false) }
    var backupOpen by remember { mutableStateOf(false) }
    var selectedTerp by remember { mutableStateOf<String?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }
    var restoreConfirm by remember { mutableStateOf(false) }
    var backupBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            runCatching { BackupArchive.export(context, uri, app.repository) }
                .onSuccess { Toast.makeText(context, "Backup saved: $it", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(context, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show() }
            backupBusy = false
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            runCatching { BackupArchive.restore(context, uri, app.repository) }
                .onSuccess { Toast.makeText(context, "Restored ${it.first} reviews and ${it.second} journal entries; duplicates skipped", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(context, "Restore failed: ${it.message}", Toast.LENGTH_LONG).show() }
            backupBusy = false
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            runCatching {
                val output = context.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")
                output.bufferedWriter(Charsets.UTF_8).use { it.write(reviewsCsv(reviews)) }
            }.onSuccess { Toast.makeText(context, "Reviews exported", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val catalogueImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCatalogue(uri) { result -> result
            .onSuccess { Toast.makeText(context, "$it catalogue products added or updated", Toast.LENGTH_LONG).show() }
            .onFailure { Toast.makeText(context, "Catalogue update failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    val background = when (appTheme) {
        AppTheme.MIDNIGHT -> Brush.verticalGradient(listOf(Color(0xFF050607), Color(0xFF24282B), Color(0xFF090A0B)))
        AppTheme.FOREST -> Brush.verticalGradient(listOf(Color(0xFF06140E), Color(0xFF123C2A), Color(0xFF07140F)))
        AppTheme.REGGAE -> Brush.linearGradient(listOf(Color(0xFF15120E), Color(0xFF322A14), Color(0xFF102B18)))
        AppTheme.STRAIN_WALL -> Brush.verticalGradient(listOf(Color(0xFF08090A), Color(0xFF191C1E), Color(0xFF070809)))
        AppTheme.CLINICAL -> Brush.verticalGradient(listOf(Color(0xFFF7FAF6), Color(0xFFE8F2EC)))
    }
    Box(Modifier.fillMaxSize().background(background)) {
    if (appTheme == AppTheme.STRAIN_WALL) StrainNameBackdrop()
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent), title = {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Terp", fontFamily = FontFamily.Serif, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2f).sp, lineHeight = 41.sp)
                        Text("folio", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 38.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.2f).sp, lineHeight = 41.sp, color = MaterialTheme.colorScheme.secondary)
                        Image(painterResource(R.drawable.terpfolio_leaf), contentDescription = null, modifier = Modifier.padding(start = 4.dp, bottom = 5.dp).size(26.dp))
                    }
                    Text("Know what works for you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                }
            }, actions = {
                Box {
                    IconButton(onClick = { moreOpen = true }) { Icon(Icons.Default.MoreVert, "More options") }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { moreOpen = false; nav.navigate("settings") })
                        DropdownMenuItem(text = { Text("Theme") }, leadingIcon = { Icon(Icons.Default.Palette, null) }, onClick = { moreOpen = false; nav.navigate("appearance") })
                        DropdownMenuItem(text = { Text("Backup / Restore") }, leadingIcon = { Icon(Icons.Default.FileDownload, null) }, onClick = { moreOpen = false; backupOpen = true })
                        DropdownMenuItem(text = { Text("About") }, leadingIcon = { Icon(Icons.Default.Info, null) }, onClick = { moreOpen = false; aboutOpen = true })
                    }
                }
            })
        },
        bottomBar = {
            if (roots.any { current == it.route }) NavigationBar {
                roots.forEach { item ->
                    val iconColor = when (item) {
                        Destination.JOURNAL -> MaterialTheme.colorScheme.secondary
                        Destination.GENETICS -> if (appTheme == AppTheme.CLINICAL) Color(0xFF805F30) else Color(0xFFD0AE70) // warm amber: lineage
                        Destination.COMMUNITY -> if (appTheme == AppTheme.CLINICAL) Color(0xFF296D56) else Color(0xFF8FBFAF) // sea-glass green
                        Destination.DIARY -> if (appTheme == AppTheme.CLINICAL) Color(0xFF6D4C82) else Color(0xFFBBA7D2) // muted lavender: notes
                    }
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = { nav.navigate(item.route) { popUpTo(Destination.JOURNAL.route); launchSingleTop = true } },
                        icon = {
                            Image(
                                painter = painterResource(when (item) {
                                    Destination.JOURNAL -> R.drawable.terpfolio_reviews
                                    Destination.GENETICS -> R.drawable.terpfolio_genetics
                                    Destination.COMMUNITY -> R.drawable.terpfolio_community
                                    Destination.DIARY -> R.drawable.terpfolio_journal
                                }),
                                contentDescription = null,
                                modifier = Modifier.size(when (item) { Destination.JOURNAL -> 36.dp; Destination.DIARY -> 34.dp; else -> 31.dp }),
                                contentScale = ContentScale.Fit
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = iconColor,
                            unselectedIconColor = iconColor,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = if (item == Destination.JOURNAL) MaterialTheme.colorScheme.secondaryContainer else iconColor.copy(alpha = .18f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (current == Destination.JOURNAL.route) AnimatedVisibility(visible = showReviewFab, enter = fadeIn() + scaleIn(initialScale = .8f), exit = fadeOut() + scaleOut(targetScale = .8f)) {
                FloatingActionButton(onClick = { nav.navigate("add-review") }, containerColor = Color.Transparent, elevation = FloatingActionButtonDefaults.elevation(5.dp)) {
                    Box(Modifier.size(56.dp).background(Brush.verticalGradient(listOf(Color(0xFF78BE90), Color(0xFF176B48))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add review", tint = Color.White) }
                }
            }
            if (current == Destination.DIARY.route) FloatingActionButton(onClick = { nav.navigate("add-session") }) { Icon(Icons.Default.Add, "Add session") }
        }
    ) { padding ->
        NavHost(nav, startDestination = Destination.JOURNAL.route, Modifier.padding(padding)) {
            composable(Destination.JOURNAL.route) { JournalScreen(vm, reviewsGrid, onOpen = { nav.navigate("review/$it") }, onDiscover = { nav.navigate("discover") }, onAdd = { nav.navigate("add-review") }, onInsights = { nav.navigate("insights") }, onTerp = { selectedTerp = it }) }
            composable(Destination.GENETICS.route) { GeneticsScreen(vm, onOpenReview = { nav.navigate("review/$it") }) }
            composable(Destination.COMMUNITY.route) { CommunityScreen(vm, onOpen = { nav.navigate("review/$it") }) }
            composable(Destination.DIARY.route) { DiaryScreen(vm) }
            composable("add-review") { ReviewEditor(catalogue = catalogue, rememberedCompanies = reviews.flatMap { listOf(it.brand, it.cultivator) }, onlineResults = vm.onlineCatalogueResults, onlineSearching = vm.onlineCatalogueSearching, onlineError = vm.onlineCatalogueError, onlineQuery = vm.onlineCatalogueQuery, onlineCompleted = vm.onlineCatalogueCompleted, onOnlineSearch = vm::searchOnlineCatalogue, onCacheProduct = vm::cacheCatalogueProduct, onSave = { vm.save(it) { nav.popBackStack() } }, onCancel = { nav.popBackStack() }) }
            composable("edit-review/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val existing by vm.review(entry.arguments?.getLong("id") ?: 0).collectAsStateWithLifecycle(initialValue = null)
                existing?.let { review -> ReviewEditor(initial = review, catalogue = catalogue, rememberedCompanies = reviews.flatMap { listOf(it.brand, it.cultivator) }, onlineResults = vm.onlineCatalogueResults, onlineSearching = vm.onlineCatalogueSearching, onlineError = vm.onlineCatalogueError, onlineQuery = vm.onlineCatalogueQuery, onlineCompleted = vm.onlineCatalogueCompleted, onOnlineSearch = vm::searchOnlineCatalogue, onCacheProduct = vm::cacheCatalogueProduct, onSave = { vm.saveUpdate(it) { nav.popBackStack() } }, onCancel = { nav.popBackStack() }) }
            }
            composable("add-session") { SessionEditor(vm, onSave = { vm.save(it) { nav.popBackStack() } }, onCancel = { nav.popBackStack() }) }
            composable("review/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val id = it.arguments?.getLong("id") ?: 0
                ReviewDetail(vm, id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("edit-review/$id") }, onTerp = { selectedTerp = it })
            }
            composable("appearance") { AppearanceScreen(appTheme, onThemeChange, onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(onTheme = { nav.navigate("appearance") }, onBackup = { backupOpen = true }, onCatalogueUpdate = { catalogueImport.launch(arrayOf("application/json", "text/json", "text/plain")) }, catalogueCount = catalogue.size, onBack = { nav.popBackStack() }) }
            composable("discover") { DiscoverScreen(vm, onBack = { nav.popBackStack() }) }
            composable("insights") { InsightScreen(vm, onBack = { nav.popBackStack() }, onTerp = { selectedTerp = it }) }
        }
    }
    if (backupOpen) AlertDialog(
        onDismissRequest = { backupOpen = false },
        title = { Text("Backup and restore") },
        text = { Text("Save your reviews, journal entries and photos in one private .zip file. Restore merges missing entries without deleting what is already on this device. Keep the file somewhere safe and private.") },
        confirmButton = { TextButton(onClick = { backupOpen = false; backup.launch("Terpfolio-backup.zip") }, enabled = !backupBusy) { Text("Create backup") } },
        dismissButton = { Column {
            TextButton(onClick = { backupOpen = false; restoreConfirm = true }, enabled = !backupBusy) { Text("Restore backup") }
            TextButton(onClick = { backupOpen = false; export.launch("Terpfolio-reviews.csv") }, enabled = reviews.isNotEmpty()) { Text("CSV only") }
        } }
    )
    if (restoreConfirm) AlertDialog(
        onDismissRequest = { restoreConfirm = false }, title = { Text("Restore a Terpfolio backup?") },
        text = { Text("Existing reviews and photos will not be overwritten. Missing reviews and journal entries from your backup will be added. Only open a backup you created and trust.") },
        confirmButton = { TextButton(onClick = { restoreConfirm = false; restore.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Choose backup") } },
        dismissButton = { TextButton(onClick = { restoreConfirm = false }) { Text("Cancel") } }
    )
    selectedTerp?.let { terpene -> ModalBottomSheet(onDismissRequest = { selectedTerp = null }) {
        TerpeneSheet(reviews, terpene, onReview = { selectedTerp = null; nav.navigate("review/$it") })
    } }
    if (aboutOpen) AlertDialog(
        onDismissRequest = { aboutOpen = false },
        title = { Text("About Terpfolio") },
        text = { Text("Personal Strain Reviews and Genetic Guide\nVersion ${BuildConfig.VERSION_NAME}\n\nKeep track of what works for you. Your reviews stay on this device unless you choose to export them.") },
        confirmButton = { TextButton(onClick = { aboutOpen = false }) { Text("Close") } }
    )
    }
}

@Composable private fun JournalScreen(vm: JournalViewModel, gridState: LazyGridState, onOpen: (Long) -> Unit, onDiscover: () -> Unit, onAdd: () -> Unit, onInsights: () -> Unit, onTerp: (String) -> Unit) {
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var favouritesOnly by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("Newest") }
    var sortOpen by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf(ReviewView.DETAILS) }
    var viewOpen by remember { mutableStateOf(false) }
    val filtered = reviews.filter { (!favouritesOnly || it.isFavourite) && (query.isBlank() || listOf(it.strainName, it.brand, it.terpenes, it.effects, it.helpedSymptoms).any { text -> text.contains(query, true) }) }
    val shown = when (sort) {
        "Highest rating" -> filtered.sortedByDescending { it.overallRating }
        "Lowest rating" -> filtered.sortedBy { it.overallRating }
        "Oldest" -> filtered.sortedBy { it.reviewDate }
        "Name A–Z" -> filtered.sortedBy { it.strainName.lowercase() }
        "Highest THC" -> filtered.sortedByDescending { it.thcPercent ?: -1.0 }
        "Highest CBD" -> filtered.sortedByDescending { it.cbdPercent ?: -1.0 }
        "Dominant terpene" -> filtered.sortedBy { it.reportedTerpNames().firstOrNull().orEmpty() }
        "Cultivator" -> filtered.sortedBy { it.brand.lowercase() }
        else -> filtered.sortedByDescending { it.reviewDate }
    }
    LazyVerticalGrid(columns = GridCells.Fixed(if (view == ReviewView.TILES) 2 else 1), state = gridState, modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp)) {
      item(key = "intro", span = { GridItemSpan(maxLineSpan) }) { Column {
        Spacer(Modifier.height(14.dp))
        Text("My Reviews", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 27.sp, lineHeight = 33.sp))
        Text(if (reviews.isEmpty()) "A record of what genuinely works for you" else "${reviews.size} reviews · ${reviews.map { it.strainName.lowercase() }.distinct().size} strains", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ElevatedCard(onClick = onInsights, modifier = Modifier.fillMaxWidth().padding(top = 9.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.terpfolio_leaf), null, Modifier.size(26.dp)); Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your Terp Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                    Text(profileHeadline(reviews), fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(13.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = .55f))
      } }
      item(key = "actions", span = { GridItemSpan(maxLineSpan) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReviewAction(Modifier.weight(1f), "Discover Similar", "Find strains like what you enjoy", Icons.Default.Explore, Color(0xFF68BC91), onDiscover)
            ReviewAction(Modifier.weight(1f), "Add Review", "Record your experience", Icons.Default.Add, Color(0xFFC3A46D), onAdd)
        }
      }
      item(key = "controls", span = { GridItemSpan(maxLineSpan) }) { Column {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search strains, effects or terpenes") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { favouritesOnly = !favouritesOnly }, contentPadding = PaddingValues(horizontal = 8.dp), border = BorderStroke(1.dp, if (favouritesOnly) Color(0xFF76BD98) else MaterialTheme.colorScheme.outline), modifier = Modifier.widthIn(min = 100.dp)) { Icon(if (favouritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, modifier = Modifier.size(18.dp), tint = Color(0xFF82C5A3)); Spacer(Modifier.width(4.dp)); Text("Favourites", fontSize = 12.sp, maxLines = 1) }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { sortOpen = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 7.dp)) { Icon(Icons.Default.Sort, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Sort: $sort", modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp)) }
                DropdownMenu(sortOpen, { sortOpen = false }) { listOf("Newest", "Oldest", "Highest rating", "Lowest rating", "Name A–Z", "Cultivator", "Highest THC", "Highest CBD", "Dominant terpene").forEach { option -> DropdownMenuItem({ Text(option) }, { sort = option; sortOpen = false }) } }
            }
            Box {
                OutlinedIconButton(onClick = { viewOpen = true }) { Icon(Icons.Default.ViewList, "View: ${view.label}") }
                DropdownMenu(viewOpen, { viewOpen = false }) { ReviewView.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, leadingIcon = { Icon(if (view == option) Icons.Default.Check else when(option) { ReviewView.TILES -> Icons.Default.GridView; ReviewView.LIST -> Icons.Default.ViewList; else -> Icons.Default.ViewAgenda }, null) }, onClick = { view = option; viewOpen = false }) } }
            }
        }
      } }
      if (shown.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyState(if (reviews.isEmpty()) "Your strain journal is empty" else "No reviews match that search", "Tap Add Review to record what you tried and how well it worked.") }
      else gridItems(shown, key = { it.id }) { review ->
          when (view) {
              ReviewView.DETAILS -> ReviewCard(review, onOpen, { vm.toggleFavourite(review) }, onTerp = onTerp)
              ReviewView.TILES -> ReviewCard(review, onOpen, { vm.toggleFavourite(review) }, compact = true, onTerp = onTerp)
              ReviewView.LIST -> ReviewListRow(review, onOpen)
          }
      }
    }
}

@Composable private fun ReviewAction(modifier: Modifier, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier.height(78.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.elevatedCardColors(containerColor = accent.copy(alpha = .13f))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, maxLines = 2, fontSize = 9.sp, lineHeight = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable private fun CommunityScreen(vm: JournalViewModel, onOpen: (Long) -> Unit) {
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val public = reviews.filter { it.visibility == "PUBLIC" }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Community reviews", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Public reviews are shown without medication-diary entries.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        if (public.isEmpty()) EmptyState("Nothing public yet", "Choose Public when saving a review. Cloud publishing activates after backend setup.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(public, key = { it.id }) { ReviewCard(it, onOpen, { vm.toggleFavourite(it) }) } }
    }
}

@Composable private fun DiaryScreen(vm: JournalViewModel) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Private medication journal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Session entries stay on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        if (sessions.isEmpty()) EmptyState("No sessions recorded", "Track dose, device, temperature, symptoms and relief.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sessions, key = { it.id }) { s -> Card { Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(s.strainName, fontWeight = FontWeight.Bold); Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(s.takenAt)), style = MaterialTheme.typography.labelSmall) }
                Text(listOfNotNull(s.amountGrams?.let { "$it g" }, s.method, s.temperatureC?.let { "$it°C" }).joinToString(" • "))
                if (s.reliefAfter > 0) Text("Relief: ${s.reliefAfter}/10")
                if (s.notes.isNotBlank()) Text(s.notes, maxLines = 3, overflow = TextOverflow.Ellipsis)
            } } }
        }
    }
}

@Composable private fun ReviewCard(review: Review, onOpen: (Long) -> Unit, onFavourite: () -> Unit, compact: Boolean = false, onTerp: (String) -> Unit = {}) {
    val favouriteScale by animateFloatAsState(if (review.isFavourite) 1.14f else 1f, label = "Favourite")
    ElevatedCard(Modifier.fillMaxWidth().clickable { onOpen(review.id) }.animateContentSize(), shape = RoundedCornerShape(16.dp)) { Column {
        Box(Modifier.fillMaxWidth()) {
            if (review.photos().isNotEmpty()) Image(rememberAsyncImagePainter(review.photos().first()), null, Modifier.fillMaxWidth().height(if (compact) 110.dp else 180.dp), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxWidth().height(if (compact) 110.dp else 142.dp).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant))), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.terpfolio_leaf), contentDescription = "Cannabis leaf", modifier = Modifier.size(if (compact) 70.dp else 126.dp), contentScale = ContentScale.Fit) }
            Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(if (compact) 34.dp else 58.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f)))))
            Surface(Modifier.align(Alignment.TopEnd).padding(if (compact) 5.dp else 12.dp), shape = RoundedCornerShape(50), color = scoreTone(review.overallRating)) { Text("★ ${review.overallRating}/10", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 11.sp else 14.sp) }
        }
        Column(Modifier.padding(horizontal = if (compact) 10.dp else 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(review.strainName, style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = if (compact) 2 else 3); if (review.brand.isNotBlank()) Text(review.brand, style = MaterialTheme.typography.titleMedium.copy(fontSize = if (compact) 12.sp else 16.sp, letterSpacing = .25.sp), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold, maxLines = 1) }; IconButton(onFavourite, modifier = Modifier.size(if (compact) 36.dp else 48.dp)) { Icon(if (review.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.graphicsLayer { scaleX = favouriteScale; scaleY = favouriteScale }) } }
            if (!compact) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(review.cultivarType, review.thcPercent?.let { "$it% THC" }.orEmpty(), review.cbdPercent?.let { "$it% CBD" }.orEmpty()).filter { it.isNotBlank() }.forEach { value -> Surface(shape = RoundedCornerShape(50), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = Color.Transparent) { Text(value, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, maxLines = 1) } } }
            if (review.reportedTerpNames().isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.terpfolio_leaf), null, Modifier.size(if (compact) 17.dp else 22.dp)); Spacer(Modifier.width(7.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                    review.reportedTerpNames().forEachIndexed { index, name ->
                        if (index > 0) Text("  •  ", color = Color(0xFFD0AE70))
                        Text(name, Modifier.clickable { onTerp(name) }, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD0AE70), maxLines = 1)
                    }
                }
            }
            if (!compact) { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(review.reviewDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (review.helpedSymptoms.isNotBlank()) Text("Helped: ${review.helpedSymptoms}", Modifier.widthIn(max = 190.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall) } }
        }
    } }
}

@Composable private fun ReviewListRow(review: Review, onOpen: (Long) -> Unit) {
    ElevatedCard(onClick = { onOpen(review.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (review.photos().isNotEmpty()) Image(rememberAsyncImagePainter(review.photos().first()), null, Modifier.size(55.dp), contentScale = ContentScale.Crop)
            else Image(painterResource(R.drawable.terpfolio_leaf), null, Modifier.size(55.dp).padding(5.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(review.strainName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(review.brand, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, maxLines = 1); Text(review.reportedTerpNames().take(2).joinToString(" • "), color = Color(0xFFD0AE70), fontSize = 11.sp, maxLines = 1) }
            Text("★ ${review.overallRating}/10", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable private fun ReviewEditor(initial: Review? = null, catalogue: List<CatalogueProduct>, rememberedCompanies: List<String>, onlineResults: List<CatalogueProduct>, onlineSearching: Boolean, onlineError: String?, onlineQuery: String, onlineCompleted: Boolean, onOnlineSearch: (String) -> Unit, onCacheProduct: (CatalogueProduct) -> Unit, onSave: (Review) -> Unit, onCancel: () -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.strainName.orEmpty()) }; var brand by remember(initial?.id) { mutableStateOf(initial?.brand.orEmpty()) }; var type by remember(initial?.id) { mutableStateOf(initial?.cultivarType ?: "Hybrid") }
    var cultivator by remember(initial?.id) { mutableStateOf(initial?.cultivator.orEmpty()) }
    var brandIsCultivator by remember(initial?.id) { mutableStateOf(initial == null || (initial.cultivator.isNotBlank() && !initial.hasSeparateBrandAndCultivator())) }
    var thc by remember(initial?.id) { mutableStateOf(initial?.thcPercent?.toString().orEmpty()) }; var cbd by remember(initial?.id) { mutableStateOf(initial?.cbdPercent?.toString().orEmpty()) }; var terpenes by remember(initial?.id) { mutableStateOf(initial?.terpenes.orEmpty()) }
    var aroma by remember(initial?.id) { mutableStateOf(initial?.aroma.orEmpty()) }; var flavour by remember(initial?.id) { mutableStateOf(initial?.flavour.orEmpty()) }; var thoughts by remember(initial?.id) { mutableStateOf(initial?.thoughts.orEmpty()) }
    var symptoms by remember(initial?.id) { mutableStateOf(initial?.helpedSymptoms.orEmpty()) }; var effects by remember(initial?.id) { mutableStateOf(initial?.effects.orEmpty()) }; var sideEffects by remember(initial?.id) { mutableStateOf(initial?.sideEffects.orEmpty()) }
    var overall by remember(initial?.id) { mutableIntStateOf(initial?.overallRating ?: 7) }; var effectiveness by remember(initial?.id) { mutableIntStateOf(initial?.effectiveness ?: 7) }; var flavourRating by remember(initial?.id) { mutableIntStateOf(initial?.flavourRating ?: 7) }; var appearance by remember(initial?.id) { mutableIntStateOf(initial?.appearanceRating ?: 7) }; var value by remember(initial?.id) { mutableIntStateOf(initial?.valueRating ?: 7) }
    var images by remember(initial?.id) { mutableStateOf(initial?.photos()?.map(Uri::parse) ?: emptyList()) }; var visibility by remember(initial?.id) { mutableStateOf(initial?.visibility ?: "PRIVATE") }
    var reviewDate by remember(initial?.id) { mutableLongStateOf(initial?.reviewDate ?: System.currentTimeMillis()) }; var batchNumber by remember(initial?.id) { mutableStateOf(initial?.batchNumber.orEmpty()) }
    var pricePaid by remember(initial?.id) { mutableStateOf(initial?.pricePaid?.toString().orEmpty()) }; var packSize by remember(initial?.id) { mutableStateOf(initial?.packSizeGrams?.toString().orEmpty()) }
    var selectedCatalogueId by remember(initial?.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photosImporting by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { picked ->
        val selected = picked.take(5 - images.size)
        if (selected.isNotEmpty()) scope.launch {
            photosImporting = true
            val imported = selected.mapNotNull { uri -> runCatching { importReviewPhoto(context, uri) }.getOrNull() }
            images = (images + imported).distinct().take(5)
            photosImporting = false
            if (imported.size != selected.size) Toast.makeText(context, "One or more photos could not be imported", Toast.LENGTH_LONG).show()
        }
    }
    fun useProduct(product: CatalogueProduct) {
        selectedCatalogueId = product.id
        name = product.productName
        if (initial == null) {
            brand = Cultivators.canonical(product.brand)
            cultivator = Cultivators.canonical(product.producer)
            brandIsCultivator = brand.isNotBlank() && cultivator.isNotBlank() && brand.equals(cultivator, true)
        }
        if (type == "Hybrid" && product.cultivarType.isNotBlank()) type = product.cultivarType
        if (terpenes.isBlank()) terpenes = product.commonTerpenes
        if (product.sourceUrl.isNotBlank()) onCacheProduct(product)
    }
    val companyNames = remember(catalogue, rememberedCompanies) {
        (catalogue.flatMap { listOf(it.brand, it.producer) } + rememberedCompanies).filter(String::isNotBlank).distinct()
    }
    val effectiveCultivator = if (brandIsCultivator) brand else cultivator
    val exactCatalogueMatch by produceState<CatalogueProduct?>(null, catalogue, name, brand, effectiveCultivator, selectedCatalogueId) {
        this.value = withContext(Dispatchers.Default) {
            catalogue.firstOrNull { it.id == selectedCatalogueId }
                ?: CatalogueSearch.exact(catalogue, name, brand, effectiveCultivator)
        }
    }
    val localSuggestions by produceState<List<CatalogueProduct>>(emptyList(), catalogue, name) {
        this.value = withContext(Dispatchers.Default) { CatalogueSearch.search(catalogue, name) }
    }
    val onlineSuggestions by produceState<List<CatalogueProduct>>(emptyList(), onlineResults, name) {
        this.value = withContext(Dispatchers.Default) { CatalogueSearch.search(onlineResults, name) }
    }
    // A partial query has no exact match. Do not compare null == null here: that previously
    // suppressed every autocomplete result until a complete catalogue name was entered.
    val selectedExactMatch = CatalogueSearch.shouldHideSuggestions(selectedCatalogueId, exactCatalogueMatch)
    val suggestions = if (selectedExactMatch) emptyList() else
        (localSuggestions + onlineSuggestions).distinctBy { "${it.productName.lowercase()}|${it.brand.lowercase()}|${it.producer.lowercase()}" }.take(8)
    LaunchedEffect(name) { onOnlineSearch(name) }
    LaunchedEffect(brand, brandIsCultivator) {
        if (brandIsCultivator) cultivator = brand
    }
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onCancel) { Icon(Icons.Default.ArrowBack, "Back") }; Text(if (initial == null) "New strain review" else "Edit strain review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
        item {
            Text("Photos (${images.size}/5)", fontWeight = FontWeight.SemiBold)
            if (images.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(images, key = { _, uri -> uri.toString() }) { index, uri ->
                    ElevatedCard(shape = RoundedCornerShape(12.dp)) { Box(Modifier.size(145.dp)) {
                        Image(rememberAsyncImagePainter(uri), "Review photo ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Surface(Modifier.align(Alignment.TopEnd).padding(3.dp), shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .7f)) {
                            IconButton({ images = images - uri }, Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Delete photo", tint = Color.White) }
                        }
                        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Color.Black.copy(alpha = .72f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (index > 0) images = images.toMutableList().also { list -> val item = list.removeAt(index); list.add(index - 1, item) } }, enabled = index > 0, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.ChevronLeft, "Move photo left", tint = Color.White) }
                                IconButton(onClick = { if (index > 0) images = listOf(uri) + images.filterNot { it == uri } }, modifier = Modifier.size(34.dp)) { Icon(if (index == 0) Icons.Default.Star else Icons.Default.StarBorder, if (index == 0) "Cover photo" else "Set as cover photo", tint = if (index == 0) MaterialTheme.colorScheme.secondary else Color.White) }
                                IconButton(onClick = { if (index < images.lastIndex) images = images.toMutableList().also { list -> val item = list.removeAt(index); list.add(index + 1, item) } }, enabled = index < images.lastIndex, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.ChevronRight, "Move photo right", tint = Color.White) }
                            }
                        }
                    } }
                }
            }
            if (images.isNotEmpty()) Text("The first photo is the cover. Use the star or arrows to reorder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (photosImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (images.size < 5) OutlinedButton({ picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth(), enabled = !photosImporting) { Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text(if (photosImporting) "Importing original photos…" else if (images.isEmpty()) "Add up to 5 photos" else "Add more photos") }
        }
        item { Field(name, { name = it; selectedCatalogueId = null }, "Strain name *") }
        item {
            val exactMatch = exactCatalogueMatch
            if (suggestions.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Catalogue matches", fontWeight = FontWeight.SemiBold)
                suggestions.forEach { product ->
                    ElevatedCard(onClick = { useProduct(product) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.productName, fontWeight = FontWeight.Bold)
                                Text(listOf(product.brand, product.producer.takeUnless { it.equals(product.brand, true) }.orEmpty(), product.cultivarName.takeUnless { it.equals(product.productName, true) }.orEmpty()).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                                if (product.genetics.isNotBlank()) Text(product.genetics, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("Use", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text("Tap a result to add reference terpenes and flavour. Your own effects, strength and batch stay personal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (exactMatch != null && selectedCatalogueId == exactMatch.id) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (exactMatch.isCustom) "YOUR CATALOGUE" else "CATALOGUE PRODUCT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(listOf(exactMatch.brand, exactMatch.producer).filter(String::isNotBlank).distinct().joinToString(" · "), style = MaterialTheme.typography.titleMedium)
                        if (exactMatch.genetics.isNotBlank()) Text("Genetics: ${exactMatch.genetics}")
                        Text("Source: ${exactMatch.sourceName}${if (exactMatch.lastVerified.isNotBlank()) " · verified ${exactMatch.lastVerified}" else ""}", style = MaterialTheme.typography.bodySmall)
                        Text("Catalogue details are a reference. Your THC/CBD and batch values are never replaced.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { useProduct(exactMatch) }) { Text("Fill available catalogue details") }
                    }
                }
            } else if (onlineSearching && onlineQuery.equals(name.trim(), ignoreCase = true)) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Searching the UK catalogue…") }
            } else if (onlineError != null && onlineQuery.equals(name.trim(), ignoreCase = true)) {
                Text("Online catalogue unavailable — offline results remain available and you can continue entering the product manually.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else if (name.isNotBlank() && onlineCompleted && onlineQuery.equals(name.trim(), ignoreCase = true)) {
                Text("No catalogue match — you can continue entering the product manually.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            CompanyPicker(brand, { brand = it }, "Brand", "Search or enter the product brand", companyNames)
        }
        item {
            Row(Modifier.fillMaxWidth().clickable { brandIsCultivator = !brandIsCultivator }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = brandIsCultivator, onCheckedChange = { brandIsCultivator = it })
                Text("Brand is also the cultivator", fontWeight = FontWeight.SemiBold)
            }
            if (!brandIsCultivator) {
                Spacer(Modifier.height(8.dp))
                CompanyPicker(cultivator, { cultivator = it }, "Cultivator", "Search or enter the cultivator", companyNames)
            }
        }
        item { Text("Cultivar type", fontWeight = FontWeight.SemiBold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Indica", "Hybrid", "Sativa").forEach { FilterChip(type == it, { type = it }, { Text(it) }) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Field(thc, { thc = it.filter { c -> c.isDigit() || c == '.' } }, "THC %") }; Box(Modifier.weight(1f)) { Field(cbd, { cbd = it.filter { c -> c.isDigit() || c == '.' } }, "CBD %") } } }
        item {
            val calendar = Calendar.getInstance().apply { timeInMillis = reviewDate }
            OutlinedButton(onClick = {
                android.app.DatePickerDialog(context, { _, year, month, day ->
                    reviewDate = Calendar.getInstance().apply { set(year, month, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Date: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(reviewDate))}") }
        }
        item { Field(batchNumber, { batchNumber = it }, "Batch number (optional)", "e.g. BN240715") }
        item {
            Text("Price and value", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { Field(pricePaid, { pricePaid = it.filter { c -> c.isDigit() || c == '.' } }, "Price paid (£)", "e.g. 130") }
                Box(Modifier.weight(1f)) { Field(packSize, { packSize = it.filter { c -> c.isDigit() || c == '.' } }, "Pack size (g)", "e.g. 10") }
            }
            val price = pricePaid.toDoubleOrNull(); val grams = packSize.toDoubleOrNull()
            if (price != null && grams != null && grams > 0) Text("£${"%.2f".format(price / grams)} per gram", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
            Text("This stays with this review, so your value rating reflects what you actually paid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Field(aroma, { aroma = it }, "Aroma") }; item { Field(flavour, { flavour = it }, "Flavour") }
        item { Text("YOUR EXPERIENCE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary); Text("Only record effects you actually noticed.", style = MaterialTheme.typography.bodySmall) }
        item { Field(symptoms, { symptoms = it }, "Symptoms helped", "e.g. pain, anxiety, sleep") }; item { Field(effects, { effects = it }, "Effects you felt", "e.g. calm, focused, sleepy") }; item { Field(sideEffects, { sideEffects = it }, "Unwanted effects") }
        item { RatingSlider("Overall", overall) { overall = it } }; item { RatingSlider("Effectiveness", effectiveness) { effectiveness = it } }; item { RatingSlider("Flavour", flavourRating) { flavourRating = it } }; item { RatingSlider("Appearance", appearance) { appearance = it } }; item { RatingSlider("Value", value) { value = it } }
        item { Field(thoughts, { thoughts = it }, "Your thoughts", "What stood out? Would you get it again?", 5) }
        item { Text("Who can see this?", fontWeight = FontWeight.SemiBold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("PRIVATE" to "Only me", "PUBLIC" to "Public").forEach { (key, label) -> FilterChip(visibility == key, { visibility = key }, { Text(label) }, leadingIcon = { Icon(if (key == "PRIVATE") Icons.Default.Lock else Icons.Default.Public, null) }) } }; if (visibility == "PUBLIC") Text("Diary sessions are never included.", style = MaterialTheme.typography.bodySmall) }
        item { Button(onClick = { onSave(Review(id=initial?.id ?: 0, strainName=name.trim(), brand=brand.trim(), cultivator=if (brandIsCultivator) brand.trim() else cultivator.trim(), cultivarType=type, thcPercent=thc.toDoubleOrNull(), cbdPercent=cbd.toDoubleOrNull(), terpenes=terpenes, aroma=aroma, flavour=flavour, thoughts=thoughts, effectiveness=effectiveness, flavourRating=flavourRating, appearanceRating=appearance, valueRating=value, overallRating=overall, helpedSymptoms=symptoms, effects=effects, sideEffects=sideEffects, imageUri=images.firstOrNull()?.toString(), imageUris=images.joinToString("\n"), reviewDate=reviewDate, batchNumber=batchNumber.trim(), pricePaid=pricePaid.toDoubleOrNull(), packSizeGrams=packSize.toDoubleOrNull(), isFavourite=initial?.isFavourite ?: false, visibility=visibility, createdAt=initial?.createdAt ?: System.currentTimeMillis())) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (initial == null) "Save review" else "Save changes") } }
    }
}

@Composable private fun SessionEditor(vm: JournalViewModel, onSave: (MedicationSession) -> Unit, onCancel: () -> Unit) {
    val reviews by vm.reviews.collectAsStateWithLifecycle(); var strain by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var method by remember { mutableStateOf("Dry herb vaporiser") }; var temp by remember { mutableStateOf("") }; var before by remember { mutableIntStateOf(5) }; var relief by remember { mutableIntStateOf(5) }; var notes by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onCancel) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Record medication", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
        if (reviews.isNotEmpty()) item { Text("Choose a reviewed strain"); Column { reviews.take(8).forEach { r -> AssistChip({ strain = r.strainName }, { Text(r.strainName) }, leadingIcon = if (strain == r.strainName) { { Icon(Icons.Default.Check, null) } } else null) } } }
        item { Field(strain, { strain = it }, "Strain name *") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f)) { Field(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, "Amount (g)") }; Box(Modifier.weight(1f)) { Field(temp, { temp = it.filter(Char::isDigit) }, "Temperature °C") } } }
        item { Field(method, { method = it }, "Method") }; item { RatingSlider("Symptoms before", before) { before = it } }; item { RatingSlider("Relief after", relief) { relief = it } }; item { Field(notes, { notes = it }, "Private notes", minLines = 4) }
        item { Button({ onSave(MedicationSession(strainName=strain.trim(), amountGrams=amount.toDoubleOrNull(), method=method, temperatureC=temp.toIntOrNull(), symptomsBefore=before, reliefAfter=relief, notes=notes)) }, enabled=strain.isNotBlank(), modifier=Modifier.fillMaxWidth()) { Text("Save private session") } }
    }
}

@Composable private fun GalleryTile(photo: String, index: Int, modifier: Modifier, more: Int = 0, onOpen: (Int) -> Unit) {
    Box(modifier.clip(RoundedCornerShape(11.dp)).clickable { onOpen(index) }) {
        Image(rememberAsyncImagePainter(photo), "Open photo ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (more > 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f)), contentAlignment = Alignment.Center) { Text("+$more", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun CompactPhotoGallery(photos: List<String>, onOpen: (Int) -> Unit) {
    val gap = 5.dp
    when (photos.size) {
        0 -> Unit
        1 -> GalleryTile(photos[0], 0, Modifier.fillMaxWidth().height(126.dp), onOpen = onOpen)
        2 -> Row(Modifier.fillMaxWidth().height(126.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            photos.forEachIndexed { index, photo -> GalleryTile(photo, index, Modifier.weight(1f).fillMaxHeight(), onOpen = onOpen) }
        }
        3 -> Row(Modifier.fillMaxWidth().height(126.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            GalleryTile(photos[0], 0, Modifier.weight(1.3f).fillMaxHeight(), onOpen = onOpen)
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                GalleryTile(photos[1], 1, Modifier.fillMaxWidth().weight(1f), onOpen = onOpen)
                GalleryTile(photos[2], 2, Modifier.fillMaxWidth().weight(1f), onOpen = onOpen)
            }
        }
        else -> Row(Modifier.fillMaxWidth().height(126.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                GalleryTile(photos[0], 0, Modifier.fillMaxWidth().weight(1f), onOpen = onOpen)
                GalleryTile(photos[2], 2, Modifier.fillMaxWidth().weight(1f), onOpen = onOpen)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                GalleryTile(photos[1], 1, Modifier.fillMaxWidth().weight(1f), onOpen = onOpen)
                GalleryTile(photos[3], 3, Modifier.fillMaxWidth().weight(1f), more = (photos.size - 4).coerceAtLeast(0), onOpen = { onOpen(if (photos.size > 4) 4 else 3) })
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable private fun ZoomablePhoto(photo: String) {
    var scale by remember(photo) { mutableFloatStateOf(1f) }
    var offset by remember(photo) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Image(
        rememberAsyncImagePainter(photo), "Full-size review photo",
        Modifier.fillMaxSize().clipToBounds().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
            .pointerInput(photo) { detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f }) }
            .transformable(transformState, canPan = { scale > 1f }),
        contentScale = ContentScale.Fit
    )
}

@Composable private fun FullScreenGallery(photos: List<String>, initialIndex: Int, onClose: () -> Unit) {
    if (photos.isEmpty()) return
    val state = rememberPagerState(initialPage = initialIndex.coerceIn(0, photos.lastIndex), pageCount = { photos.size })
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = state, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
                ZoomablePhoto(photos[page])
            }
            Surface(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp), shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .65f)) { IconButton(onClose) { Icon(Icons.Default.Close, "Close gallery", tint = Color.White) } }
            Surface(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(18.dp), shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = .65f)) { Text("${state.currentPage + 1} / ${photos.size}", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Color.White) }
            if (photos.size > 1) Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { state.animateScrollToPage(state.currentPage - 1) } }, enabled = state.currentPage > 0) { Icon(Icons.Default.ChevronLeft, "Previous photo", tint = if (state.currentPage > 0) Color.White else Color.Gray) }
                Text("Swipe for next / previous", color = Color.White)
                IconButton(onClick = { scope.launch { state.animateScrollToPage(state.currentPage + 1) } }, enabled = state.currentPage < photos.lastIndex) { Icon(Icons.Default.ChevronRight, "Next photo", tint = if (state.currentPage < photos.lastIndex) Color.White else Color.Gray) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ReportedStrainData(genetics: String, terpenes: List<String>, onTerp: (String) -> Unit) {
    if (genetics.isBlank() && terpenes.isEmpty()) return
    Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .70f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (genetics.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.reference_genetics), null, Modifier.size(24.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(9.dp))
                Column { Text("LINEAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(genetics, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            }
            if (terpenes.isNotEmpty()) {
                if (genetics.isNotBlank()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.Top) {
                    Image(painterResource(R.drawable.terpfolio_leaf), null, Modifier.size(24.dp)); Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("REPORTED TERPENE PROFILE", fontSize = 10.sp, letterSpacing = .7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            terpenes.forEachIndexed { index, terpene ->
                                // Separator and name are one non-breaking layout item, so a bullet
                                // can never be stranded on the previous or following line.
                                Text(
                                    text = if (index == 0) terpene else "•\u00A0$terpene",
                                    modifier = Modifier.clickable { onTerp(terpene) },
                                    color = Color(0xFFD0AE70),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun AromaFlavourCard(aromas: List<String>, flavours: List<String>) {
    if (aromas.isEmpty() && flavours.isEmpty()) return
    Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("AROMA & FLAVOUR (REPORTED)", fontSize = 10.sp, letterSpacing = .8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (aromas.isNotEmpty()) ReportedSensoryItem(R.drawable.reference_aroma, "Aroma", aromas, Modifier.weight(1f))
                if (flavours.isNotEmpty()) ReportedSensoryItem(R.drawable.reference_flavour, "Flavour", flavours, Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun ReportedSensoryItem(iconResource: Int, label: String, values: List<String>, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconResource), null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(7.dp))
        Column { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(values.joinToString(" • "), color = Color(0xFFD0AE70), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable private fun PersonalReviewItem(icon: ImageVector, label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private data class ReviewRating(val label: String, val score: Int, val iconResource: Int)

@Composable private fun ReviewRatingGrid(ratings: List<ReviewRating>) {
    if (ratings.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ratings.chunked(2).forEach { rowRatings ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowRatings.forEach { rating -> RatingTile(rating, Modifier.weight(1f)) }
                if (rowRatings.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun RatingTile(rating: ReviewRating, modifier: Modifier = Modifier) {
    Surface(modifier.height(48.dp), shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .56f)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(rating.iconResource), null, Modifier.size(28.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(7.dp))
            Column { Text(rating.label, style = MaterialTheme.typography.labelSmall, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1); Text("${rating.score}/10", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium, lineHeight = 19.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun ReviewDetail(vm: JournalViewModel, id: Long, onBack: () -> Unit, onEdit: () -> Unit, onTerp: (String) -> Unit) {
    val review by vm.review(id).collectAsStateWithLifecycle(initialValue = null)
    val catalogue by vm.catalogue.collectAsStateWithLifecycle()
    val r = review ?: return EmptyState("Review not found", "It may have been removed.")
    val photos = r.photos()
    var galleryIndex by remember(id) { mutableStateOf<Int?>(null) }
    val product = CatalogueSearch.exact(catalogue, r.strainName, r.brand, r.cultivator)?.takeUnless { it.isCustom }
    val builtIn = StrainReferences.exact(r.strainName)
    val genetics = product?.genetics?.takeIf(String::isNotBlank) ?: builtIn?.genetics.orEmpty()
    val reportedTerpenes = product?.terpeneList()?.takeIf { it.isNotEmpty() } ?: builtIn?.commonTerpenes.orEmpty()
    val reportedAromas = builtIn?.reportedAromas.orEmpty()
    val reportedFlavours = product?.flavourList()?.takeIf { it.isNotEmpty() } ?: builtIn?.flavours.orEmpty()
    val hasStrainReference = genetics.isNotBlank() || reportedTerpenes.isNotEmpty()
    val hasSensoryReference = reportedAromas.isNotEmpty() || reportedFlavours.isNotEmpty()
    val ratings = listOf(
        ReviewRating("Effectiveness", r.effectiveness, R.drawable.rating_effectiveness_premium),
        ReviewRating("Flavour", r.flavourRating, R.drawable.rating_flavour_premium),
        ReviewRating("Appearance", r.appearanceRating, R.drawable.rating_appearance_premium),
        ReviewRating("Value", r.valueRating, R.drawable.rating_value_premium)
    ).filter { it.score > 0 }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Spacer(Modifier.weight(1f)); TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") } } }
        item {
            if (photos.isEmpty()) TextButton(onClick = onEdit, modifier = Modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Icon(Icons.Default.AddAPhoto, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Add photos") }
            else { CompactPhotoGallery(photos) { galleryIndex = it }; Spacer(Modifier.height(12.dp)) }
        }
        item { Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Text(r.strainName, Modifier.weight(1f), fontFamily = FontFamily.Serif, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("★ ${r.overallRating}/10", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary, maxLines = 1); IconButton({ vm.toggleFavourite(r) }, Modifier.size(40.dp)) { Icon(if (r.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = MaterialTheme.colorScheme.tertiary) } }
            }
            val effectiveCultivator = r.effectiveCultivator()
            if (r.brand.isNotBlank() || effectiveCultivator.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                if (r.hasSeparateBrandAndCultivator()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(Modifier.width(210.dp)) {
                            CultivatorIdentity(effectiveCultivator, label = "CULTIVATOR")
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                            CultivatorIdentity(r.brand, label = "BRAND", secondary = true)
                        }
                    }
                } else {
                    CultivatorIdentity(r.brand.ifBlank { effectiveCultivator }, label = if (r.cultivator.isBlank()) "BRAND" else "BRAND & CULTIVATOR")
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(10.dp))
            }
            val specification = listOfNotNull(r.cultivarType.takeIf(String::isNotBlank), r.thcPercent?.let { "$it% THC" }, r.cbdPercent?.let { "$it% CBD" }).joinToString("  •  ")
            if (specification.isNotBlank()) Text(specification, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val metadata = buildList { add("Reviewed ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.reviewDate))}"); if (r.batchNumber.isNotBlank()) add("Batch ${r.batchNumber}") }
            Text(metadata.joinToString("  •  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hasStrainReference) { Spacer(Modifier.height(10.dp)); ReportedStrainData(genetics, reportedTerpenes, onTerp) }
            if (hasSensoryReference) { Spacer(Modifier.height(10.dp)); AromaFlavourCard(reportedAromas, reportedFlavours) }
            if (hasStrainReference || hasSensoryReference) Spacer(Modifier.height(8.dp))
            if (r.pricePaid != null) Row(Modifier.padding(start = 2.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Sell, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("£${"%.2f".format(r.pricePaid)}${r.packSizeGrams?.takeIf { it > 0 }?.let { "  •  ${it}g  •  £${"%.2f".format(r.pricePaid / it)}/g" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(14.dp))
            if (r.helpedSymptoms.isNotBlank() || r.thoughts.isNotBlank() || ratings.isNotEmpty()) Text("Your Review", fontFamily = FontFamily.Serif, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            if (r.helpedSymptoms.isNotBlank()) { Spacer(Modifier.height(10.dp)); PersonalReviewItem(Icons.Default.CheckCircleOutline, "Helped", r.helpedSymptoms) }
            if (r.thoughts.isNotBlank()) { Spacer(Modifier.height(10.dp)); PersonalReviewItem(Icons.Default.Chat, "Thoughts", r.thoughts) }
            if (ratings.isNotEmpty()) { Spacer(Modifier.height(14.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp)); ReviewRatingGrid(ratings) }
            Spacer(Modifier.height(12.dp))
            AssistChip({}, { Text(if (r.visibility == "PUBLIC") "Public review" else "Private review") }, leadingIcon = { Icon(if (r.visibility == "PUBLIC") Icons.Default.Public else Icons.Default.Lock, null) })
        } }
    }
    galleryIndex?.let { FullScreenGallery(photos, it) { galleryIndex = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun GeneticsScreen(vm: JournalViewModel, onOpenReview: (Long) -> Unit) {
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val catalogue by vm.catalogue.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<CatalogueProduct?>(null) }
    val matches = (CatalogueSearch.search(catalogue, query, 30) + CatalogueSearch.search(vm.onlineCatalogueResults, query, 30))
        .distinctBy { "${it.productName.lowercase()}|${it.brand.lowercase()}" }.take(30)
    LaunchedEffect(query) { vm.searchOnlineCatalogue(query) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { Text("Product catalogue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Search products, cultivars, brands, genetics, aliases or product codes", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(query, { query = it; selected = CatalogueSearch.exact(catalogue, it) }, Modifier.fillMaxWidth(), placeholder = { Text("Try King Sherb or Green Karat") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true) }
        if (selected == null) {
            if (query.isBlank()) item { EmptyState("Explore UK products", "${catalogue.size} products are available offline. Search by prescribed product, cultivar or company.") }
            else if (matches.isEmpty() && vm.onlineCatalogueSearching && vm.onlineCatalogueQuery.equals(query.trim(), true)) item { Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Searching the UK catalogue…") } }
            else if (matches.isEmpty() && vm.onlineCatalogueError != null && vm.onlineCatalogueQuery.equals(query.trim(), true)) item { EmptyState("Online catalogue unavailable", "Offline results remain available. You can still review the product manually and Terpfolio will remember it locally.") }
            else if (matches.isEmpty() && vm.onlineCatalogueCompleted && vm.onlineCatalogueQuery.equals(query.trim(), true)) item { EmptyState("No catalogue match", "You can still review it manually. Terpfolio will remember it locally after you save the review.") }
            else items(matches, key = { it.id }) { product -> ElevatedCard(onClick = { selected = product; query = product.productName }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(product.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(listOf(product.brand, product.cultivarName.takeUnless { it.equals(product.productName, true) }.orEmpty()).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.tertiary); if (product.genetics.isNotBlank()) Text(product.genetics); Text(product.terpeneList().joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        } else {
            val product = selected!!
            item { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(product.productName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(product.brand.ifBlank { product.producer }, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold) }; Icon(Icons.Default.AccountTree, null, Modifier.size(34.dp)) }
                if (product.cultivarName.isNotBlank() && !product.cultivarName.equals(product.productName, true)) Detail("Cultivar", product.cultivarName)
                Detail("Genetics", product.genetics)
                Detail("Product code", product.productCode)
                HorizontalDivider()
                Text("Commonly reported terpenes", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { product.terpeneList().forEach { terpene -> SuggestionChip({}, { Text(terpene) }) } }
                if (product.flavourList().isNotEmpty()) { Text("Reference flavour profile", fontWeight = FontWeight.Bold); Text(product.flavourList().joinToString(" • ")) }
                Text("Source: ${product.sourceName}${if (product.lastVerified.isNotBlank()) " · verified ${product.lastVerified}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Reference information can vary by producer, phenotype and batch. Always use your dispensing label for medicine strength.", style = MaterialTheme.typography.bodySmall)
            } } }
            val journalMatches = reviews.filter { it.strainName.equals(product.productName, true) }
            if (journalMatches.isNotEmpty()) item { Text("Your ${product.productName} reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(journalMatches, key = { it.id }) { review -> ReviewCard(review, onOpenReview, { vm.toggleFavourite(review) }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun DiscoverScreen(vm: JournalViewModel, onBack: () -> Unit) {
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    var flavours by remember { mutableStateOf(setOf<String>()) }
    var terpenes by remember { mutableStateOf(setOf<String>()) }
    var learnFromReviews by remember { mutableStateOf(true) }
    val favourites = reviews.filter { it.isFavourite || it.overallRating >= 8 }
    val likedReferences = if (learnFromReviews) favourites.mapNotNull { StrainReferences.exact(it.strainName) } else emptyList()
    val likedTerpenes = if (learnFromReviews) favourites.flatMap(Review::reportedTerpNames).toSet() else emptySet()
    val likedFlavours = likedReferences.flatMap { it.flavours }.toSet()
    val wantedTerpenes = terpenes + likedTerpenes
    val wantedFlavours = flavours + likedFlavours
    val reviewedNames = reviews.map { it.strainName.lowercase() }.toSet()
    val suggestions = if (wantedTerpenes.isEmpty() && wantedFlavours.isEmpty()) emptyList() else
        StrainReferences.all().filterNot { it.name.lowercase() in reviewedNames }
            .map { reference ->
                val matchedTerpenes = reference.commonTerpenes.filter { candidate -> wantedTerpenes.any { it.equals(candidate, true) } }
                val matchedFlavours = reference.flavours.filter { candidate -> wantedFlavours.any { it.equals(candidate, true) } }
                reference to (matchedTerpenes + matchedFlavours)
            }.filter { it.second.isNotEmpty() }.sortedWith(compareByDescending<Pair<StrainReference, List<String>>> { it.second.size }.thenBy { it.first.name }).take(12)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Find your next strain", style = MaterialTheme.typography.headlineSmall) } }
        item { Text("Explore shared terpenes and flavours from the app’s small reference catalogue. Your high-rated reviews help choose what to compare; this is not a medical prediction.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text("Flavours you like", style = MaterialTheme.typography.titleLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Gas", "Diesel", "Citrus", "Grape", "Berry", "Mint", "Earthy", "Sweet", "Cream", "Pine").forEach { choice -> FilterChip(choice in flavours, { flavours = if (choice in flavours) flavours - choice else flavours + choice }, { Text(choice) }) } } }
        item { Text("Terpenes you want to explore", style = MaterialTheme.typography.titleLarge); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Limonene", "Caryophyllene", "Myrcene", "Linalool", "Pinene", "Humulene").forEach { choice -> FilterChip(choice in terpenes, { terpenes = if (choice in terpenes) terpenes - choice else terpenes + choice }, { Text(choice) }) } } }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(learnFromReviews, { learnFromReviews = it }); Column { Text("Use strains I rated 8+ or favourited"); Text("Reported strain profiles plus your personal ratings; not a medical prediction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item { HorizontalDivider(); Text("Potential matches", style = MaterialTheme.typography.headlineSmall) }
        if (suggestions.isEmpty()) item { Text("Choose a flavour or terpene to see matches. You can also rate or favourite a known strain first.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(suggestions, key = { it.first.name }) { (reference, matches) ->
            ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(reference.name, style = MaterialTheme.typography.titleLarge)
                Text(reference.genetics, style = MaterialTheme.typography.bodyMedium)
                Text("Shared profile: ${matches.joinToString(" · ")}", color = MaterialTheme.colorScheme.secondary)
                val liked = favourites.filter { review -> review.reportedTerpNames().any { terp -> matches.any { it.equals(terp, true) } } }.take(2)
                if (liked.isNotEmpty() && learnFromReviews) Text("Also reported in your highly rated ${liked.joinToString(" and ") { it.strainName }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            } }
        }
        item { Text("These are similarities, not a guarantee of effects, quality or suitability. Check the actual cultivar and batch details before ordering.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun InsightScreen(vm: JournalViewModel, onBack: () -> Unit, onTerp: (String) -> Unit) {
    val reviews by vm.reviews.collectAsStateWithLifecycle()
    val names = reviews.flatMap(Review::reportedTerpNames).distinctBy(String::lowercase).sortedWith(compareByDescending<String> { terpReviews(reviews, it).size }.thenBy { it })
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Your Terp Profile", style = MaterialTheme.typography.headlineSmall) } }
        item { Text("Based on reported strain profiles and the effects you personally recorded. A shared terpene does not mean a strain will work the same way for you.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (names.isEmpty()) item { EmptyState("No reference patterns yet", "More recognised strains are needed before Terpfolio can compare reported profiles.") }
        items(names, key = { it.lowercase() }) { name ->
            val matched = terpReviews(reviews, name)
            ElevatedCard(onClick = { onTerp(name) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.terpfolio_leaf), null, Modifier.size(27.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary); Text("${matched.size} ${if (matched.size == 1) "review" else "reviews"} · personal average ${"%.1f".format(matched.map { it.overallRating }.average())}/10", style = MaterialTheme.typography.bodySmall) }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable private fun TerpeneSheet(reviews: List<Review>, name: String, onReview: (Long) -> Unit) {
    val matched = terpReviews(reviews, name)
    val effects = matched.flatMap { it.effects.split(",") }.map(String::trim).filter(String::isNotBlank).groupingBy(String::lowercase).eachCount().entries.sortedByDescending { it.value }.take(4)
    val catalogue = StrainReferences.all().filter { reference -> reference.commonTerpenes.any { it.equals(name, true) } }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(R.drawable.terpfolio_leaf), null, Modifier.size(29.dp)); Spacer(Modifier.width(9.dp)); Text(name, style = MaterialTheme.typography.headlineSmall) } }
        item { Text("YOUR RECORDS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary) }
        item { Text("${matched.size} ${if (matched.size == 1) "review" else "reviews"} ${if (matched.isEmpty()) "so far" else "· average personal rating ${"%.1f".format(matched.map { it.overallRating }.average())}/10"}", style = MaterialTheme.typography.titleMedium) }
        if (effects.isNotEmpty()) item { Text("Effects you wrote down in these reviews: ${effects.joinToString(" · ") { "${it.key} (${it.value})" }}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (catalogue.isNotEmpty()) item { Text("IN THE CATALOGUE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary); Text("Listed for ${catalogue.size} reference strains, including ${catalogue.take(3).joinToString(" · ") { it.name }}.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text("These are correlations in your notes, not effects caused by this terpene or predictions about a new strain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(matched, key = { it.id }) { review -> ElevatedCard(onClick = { onReview(review.id) }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text(review.strainName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("★ ${review.overallRating}/10", color = MaterialTheme.colorScheme.secondary) } } }
    }
}

@Composable private fun SettingsScreen(onTheme: () -> Unit, onBackup: () -> Unit, onCatalogueUpdate: () -> Unit, catalogueCount: Int, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        ElevatedCard(onClick = onTheme, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Palette, null); Spacer(Modifier.width(16.dp)); Text("Choose theme") } }
        ElevatedCard(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(16.dp)); Text("Backup and restore") } }
        ElevatedCard(onClick = onCatalogueUpdate, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SystemUpdateAlt, null); Spacer(Modifier.width(16.dp)); Column { Text("Update product catalogue"); Text("$catalogueCount products available offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        Text("Import a Terpfolio catalogue JSON file without reinstalling the app. Existing reviews, custom products and batch strengths stay untouched.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Your reviews remain on this device unless you export them. A full backup includes photos and private journal entries; keep it private.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun AppearanceScreen(current: AppTheme, onThemeChange: (AppTheme) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Column { Text("Choose your look", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("You can change this whenever you like", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        items(AppTheme.entries) { option -> ElevatedCard(onClick = { onThemeChange(option) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = if (current == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (current == option) Icons.Default.CheckCircle else Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column { Text(option.title, fontWeight = FontWeight.Bold); Text(option.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        item { Text("Custom backgrounds and artwork are planned next. Background images will include a strength control so text stays readable.", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable private fun Field(value:String, change:(String)->Unit, label:String, hint:String="", minLines:Int=1) = TextField(
    value = value,
    onValueChange = change,
    modifier = Modifier.fillMaxWidth(),
    label = { Text(label) },
    placeholder = { if (hint.isNotBlank()) Text(hint) },
    minLines = minLines,
    shape = RoundedCornerShape(12.dp),
    colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .82f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f),
        focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
    )
)
@Composable private fun RatingSlider(label:String, value:Int, change:(Int)->Unit) { Column { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text(label, fontWeight=FontWeight.SemiBold); Text("$value/10") }; Slider(value.toFloat(), { change(it.toInt()) }, valueRange=0f..10f, steps=9) } }
@Composable private fun Detail(label:String, value:String) { if(value.isNotBlank()) Column { Text(label, style=MaterialTheme.typography.labelLarge, color=MaterialTheme.colorScheme.primary); Text(value) } }
private fun reviewsCsv(reviews: List<Review>): String {
    fun cell(value: Any?): String {
        val raw = value?.toString().orEmpty()
        // Spreadsheet applications may execute cells beginning with a formula prefix.
        val first = raw.trimStart().firstOrNull()
        val safe = if (first == '=' || first == '+' || first == '-' || first == '@') "'$raw" else raw
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    val header = listOf("Strain", "Brand", "Cultivator", "Type", "THC %", "CBD %", "Terpenes", "Aroma", "Flavour", "Thoughts", "Effectiveness /10", "Flavour rating /10", "Appearance /10", "Value /10", "Overall /10", "Symptoms helped", "Effects felt", "Unwanted effects", "Review date (Unix ms)", "Batch number", "Price paid (£)", "Pack size (g)", "Price per gram (£)", "Favourite", "Visibility", "Created (Unix ms)")
    val lines = reviews.map { r -> listOf(r.strainName, r.brand, r.effectiveCultivator(), r.cultivarType, r.thcPercent, r.cbdPercent, r.terpenes, r.aroma, r.flavour, r.thoughts, r.effectiveness, r.flavourRating, r.appearanceRating, r.valueRating, r.overallRating, r.helpedSymptoms, r.effects, r.sideEffects, r.reviewDate, r.batchNumber, r.pricePaid, r.packSizeGrams, if (r.pricePaid != null && r.packSizeGrams != null && r.packSizeGrams > 0) r.pricePaid / r.packSizeGrams else null, r.isFavourite, r.visibility, r.createdAt).joinToString(",") { cell(it) } }
    return "\uFEFF" + (listOf(header.joinToString(",") { cell(it) }) + lines).joinToString("\r\n", postfix = "\r\n")
}
@Composable private fun StrainNameBackdrop() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.strain_wall_mylar_v2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = .10f),
                        Color.Black.copy(alpha = .42f),
                        Color.Black.copy(alpha = .50f)
                    )
                )
            )
        )
    }
}
@Composable private fun EmptyState(title:String, body:String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Default.Spa, null, Modifier.size(54.dp), tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text(title, style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold); Text(body, color=MaterialTheme.colorScheme.onSurfaceVariant) } } }
