package dev.coldlecture.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.coldlecture.data.Repositories
import dev.coldlecture.data.ThemePreference
import dev.coldlecture.notes.NoteEditorState
import dev.coldlecture.search.LocalSearch
import dev.coldlecture.search.SearchHit
import dev.coldlecture.state.FocusSession
import dev.coldlecture.state.LectureController
import dev.coldlecture.ui.components.CollapsibleSection
import dev.coldlecture.ui.components.IconAction
import dev.coldlecture.ui.dictionary.DictionaryBody
import dev.coldlecture.ui.dictionary.DictionaryUiState
import dev.coldlecture.ui.focus.FocusOverlays
import dev.coldlecture.ui.mindmap.MindmapView
import dev.coldlecture.ui.notes.NotesBody
import dev.coldlecture.ui.search.SearchBar
import dev.coldlecture.ui.search.SearchResults
import dev.coldlecture.ui.slides.DeckOrganizeDialog
import dev.coldlecture.ui.slides.DeckOrganizeDraft
import dev.coldlecture.ui.slides.MIN_ZOOM
import dev.coldlecture.ui.slides.SlideHeader
import dev.coldlecture.ui.slides.SlideViewer
import dev.coldlecture.ui.slides.knownCoursesFrom
import dev.coldlecture.ui.theme.Lecture
import dev.coldlecture.ui.todos.TodoDrawer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val CompactWidth = 1000.dp
private val SidebarWidth = 350.dp

@Composable
fun LectureShell(
    repos: Repositories,
    controller: LectureController,
    session: FocusSession,
    notesEditor: NoteEditorState,
    search: LocalSearch,
    theme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    interactivity: Int,
    onInteractivityChange: (Int) -> Unit,
    animals: Boolean,
    onAnimalsChange: (Boolean) -> Unit,
) {
    val decks by repos.decks.decks.collectAsState(emptyList())
    val entries by repos.dictionary.entries.collectAsState(emptyList())
    val links by repos.dictionary.links.collectAsState(emptyList())
    val todos by repos.todos.todos.collectAsState(emptyList())
    val dictState = remember { DictionaryUiState() }
    val searchFocus = remember { FocusRequester() }
    val viewerFocus = remember { FocusRequester() }

    var notesExpanded by remember { mutableStateOf(true) }
    var dictExpanded by remember { mutableStateOf(true) }
    var todosOpen by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(MIN_ZOOM) }
    var sidebarPreference by remember { mutableStateOf<Boolean?>(null) }
    var mainView by remember { mutableStateOf(MainView.Slides) }

    fun openTerm(entryId: Long) {
        dictState.creating = false
        dictState.editing = null
        dictState.selectedId = entryId
        dictExpanded = true
        sidebarPreference = true
    }

    val deck = controller.activeDeck
    LaunchedEffect(deck?.id, controller.page) {
        deck?.let { notesEditor.bind(it.id, controller.page) }
    }
    val currentDecks by rememberUpdatedState(decks)
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(search.query, search.scope, controller.pageTexts.size) }.collectLatest {
            delay(140)
            search.run(controller.pageTexts.toMap(), currentDecks)
        }
    }
    LaunchedEffect(deck?.id) {
        if (deck != null) {
            zoom = MIN_ZOOM
            runCatching { viewerFocus.requestFocus() }
        }
    }

    fun openHit(hit: SearchHit) {
        when (hit) {
            is SearchHit.Slide -> controller.goTo(hit.pageIndex)
            is SearchHit.Note -> decks.firstOrNull { it.id == hit.deckId }?.let { controller.openDeck(it, hit.pageIndex) }
            is SearchHit.Term -> openTerm(hit.entryId)
        }
        search.clear()
        if (hit !is SearchHit.Term) runCatching { viewerFocus.requestFocus() }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Lecture.colors.backdrop, Lecture.colors.backdropEdge)))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.K && (event.isAltPressed || event.isCtrlPressed) -> {
                        search.open = true
                        runCatching { searchFocus.requestFocus() }
                        true
                    }
                    event.key == Key.Escape && search.open -> {
                        search.clear()
                        true
                    }
                    event.key == Key.Escape && controller.pendingImport != null -> {
                        controller.cancelImport()
                        true
                    }
                    event.key == Key.Escape && todosOpen -> {
                        todosOpen = false
                        true
                    }
                    else -> false
                }
            },
    ) {
        controller.pendingImport?.let { pending ->
            DeckOrganizeDialog(
                heading = "Organize imported deck",
                draft = DeckOrganizeDraft(
                    title = pending.suggestedTitle,
                    courseCode = deck?.courseCode ?: "",
                    courseTitle = deck?.courseTitle ?: "",
                ),
                knownCourses = knownCoursesFrom(decks),
                confirmLabel = "Add deck",
                onConfirm = { draft ->
                    controller.confirmImport(draft.title, draft.courseCode, draft.courseTitle)
                },
                onDismiss = controller::cancelImport,
            )
        }
        val compact = maxWidth < CompactWidth
        val sidebarVisible = sidebarPreference ?: !compact
        val searchDismissible = search.open

        fun toggleNotes() {
            notesExpanded = !notesExpanded
            if (compact && notesExpanded) dictExpanded = false
        }

        fun toggleDict() {
            dictExpanded = !dictExpanded
            if (compact && dictExpanded) notesExpanded = false
        }

        val sidebar: @Composable (Modifier) -> Unit = { modifier ->
            Sidebar(
                modifier = modifier,
                notesExpanded = notesExpanded,
                dictExpanded = dictExpanded,
                onToggleNotes = ::toggleNotes,
                onToggleDict = ::toggleDict,
                notesSubtitle = if (deck != null) "Slide ${controller.page + 1}" else null,
                dictSubtitle = "${entries.size} terms",
                onAddTerm = {
                    dictState.startCreate()
                    dictExpanded = true
                    if (compact) notesExpanded = false
                },
                notes = { NotesBody(notesEditor, Modifier.fillMaxSize()) },
                dictionary = { DictionaryBody(dictState, entries, links, repos.dictionary, Modifier.fillMaxSize()) },
            )
        }

        Column(Modifier.fillMaxSize()) {
            TopBar(
                session = session,
                deck = deck,
                compact = compact,
                sidebarVisible = sidebarVisible,
                onToggleSidebar = { sidebarPreference = !sidebarVisible },
                mainView = mainView,
                onMainViewChange = { mainView = it },
                animals = animals,
                theme = theme,
                onThemeChange = onThemeChange,
                interactivity = interactivity,
                onInteractivityChange = onInteractivityChange,
                onAnimalsChange = onAnimalsChange,
            )
            Row(Modifier.weight(1f).fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (mainView) {
                        MainView.Slides -> {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Lecture.colors.panel),
                            ) {
                                SlideHeader(controller, decks, zoom, onZoomChange = { zoom = it }, modifier = Modifier.fillMaxWidth())
                                SlideViewer(controller, zoom, viewerFocus, Modifier.weight(1f).fillMaxWidth())
                                SearchBar(
                                    search = search,
                                    focusRequester = searchFocus,
                                    shortcutLabel = "Alt+K",
                                    onSubmitFirst = { search.results.firstOrNull()?.let(::openHit) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (searchDismissible) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .padding(bottom = 54.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = search::clear,
                                        ),
                                )
                            }
                            SearchResultsPopup(
                                visible = search.open && search.query.isNotBlank(),
                                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 14.dp, end = 14.dp, bottom = 62.dp),
                            ) {
                                SearchResults(search, onSelect = ::openHit)
                            }
                        }
                        MainView.Mindmap -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Lecture.colors.panel),
                            ) {
                                MindmapView(
                                    entries = entries,
                                    links = links,
                                    onSelectTerm = ::openTerm,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                if (sidebarVisible && !compact) {
                    sidebar(Modifier.width(SidebarWidth).fillMaxHeight())
                }
            }
            BottomBar(
                session = session,
                openTodos = todos.count { !it.done },
                todosOpen = todosOpen,
                onToggleTodos = { todosOpen = !todosOpen },
            )
        }

        if (compact) {
            AnimatedVisibility(
                visible = sidebarVisible,
                enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(160)),
                exit = slideOutHorizontally(tween(180)) { it } + fadeOut(tween(120)),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, bottom = 52.dp, end = 10.dp),
            ) {
                sidebar(
                    Modifier
                        .width(SidebarWidth.coerceAtMost(maxWidth - 20.dp))
                        .fillMaxHeight()
                        .shadow(20.dp, RoundedCornerShape(12.dp)),
                )
            }
        }

        AnimatedVisibility(
            visible = todosOpen,
            enter = slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(180)) { it / 3 } + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 52.dp),
        ) {
            TodoDrawer(todos, repos.todos, onClose = { todosOpen = false })
        }

        FocusOverlays(session)
    }
}

@Composable
private fun SearchResultsPopup(visible: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)) + slideInVertically(tween(160)) { it / 8 },
        exit = fadeOut(tween(100)),
        modifier = modifier,
    ) { content() }
}

@Composable
private fun Sidebar(
    modifier: Modifier,
    notesExpanded: Boolean,
    dictExpanded: Boolean,
    onToggleNotes: () -> Unit,
    onToggleDict: () -> Unit,
    notesSubtitle: String?,
    dictSubtitle: String,
    onAddTerm: () -> Unit,
    notes: @Composable () -> Unit,
    dictionary: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CollapsibleSection(
            title = "Notes",
            icon = Icons.Default.Edit,
            expanded = notesExpanded,
            onToggle = onToggleNotes,
            subtitle = notesSubtitle,
            fillHeight = true,
            modifier = Modifier.fillMaxWidth().let { if (notesExpanded) it.weight(if (dictExpanded) 0.42f else 1f) else it },
        ) { notes() }
        CollapsibleSection(
            title = "Dictionary",
            icon = Icons.Default.Info,
            expanded = dictExpanded,
            onToggle = onToggleDict,
            subtitle = dictSubtitle,
            fillHeight = true,
            modifier = Modifier.fillMaxWidth().let { if (dictExpanded) it.weight(if (notesExpanded) 0.58f else 1f) else it },
            actions = { IconAction(Icons.Default.Add, "Add term", onClick = onAddTerm, tint = Lecture.colors.accent, size = 26.dp) },
        ) { dictionary() }
    }
}
