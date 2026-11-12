package dev.coldlecture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.coldlecture.data.Repositories
import dev.coldlecture.data.SettingsRepository
import dev.coldlecture.data.ThemePreference
import dev.coldlecture.notes.NoteEditorState
import dev.coldlecture.notes.NoteStore
import dev.coldlecture.platform.AppPlatform
import dev.coldlecture.search.LocalSearch
import dev.coldlecture.state.FocusSession
import dev.coldlecture.state.LectureController
import dev.coldlecture.ui.shell.LectureShell
import dev.coldlecture.ui.theme.ColdLectureTheme
import kotlinx.coroutines.launch

private sealed interface Startup {
    data object Loading : Startup
    data object Ready : Startup
    data class Failed(val message: String) : Startup
}

@Composable
fun App(platform: AppPlatform) {
    val scope = rememberCoroutineScope()
    val repos = remember { Repositories(platform.database) }
    var startup by remember { mutableStateOf<Startup>(Startup.Loading) }
    var theme by remember { mutableStateOf(ThemePreference.System) }
    var interactivity by remember { mutableIntStateOf(SettingsRepository.DEFAULT_INTERACTIVITY) }
    var animals by remember { mutableStateOf(true) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        ThemePreference.System -> systemDark
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }

    LaunchedEffect(Unit) {
        startup = try {
            theme = repos.settings.themePreference()
            interactivity = repos.settings.interactivityPercent()
            animals = repos.settings.animalsEnabled()
            Startup.Ready
        } catch (t: Throwable) {
            Startup.Failed(t.message ?: t::class.simpleName ?: "Unknown error")
        }
    }

    ColdLectureTheme(dark = dark) {
        when (val state = startup) {
            Startup.Loading -> Centered { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp)) }
            is Startup.Failed -> Centered {
                Text(
                    "Could not open local storage: ${state.message}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(32.dp),
                )
            }
            Startup.Ready -> {
                val session = remember { FocusSession(platform.focus, scope) }
                val controller = remember { LectureController(repos, platform.deckFiles, session, scope) }
                val notesEditor = remember {
                    NoteEditorState(
                        store = object : NoteStore {
                            override suspend fun load(deckId: String, pageIndex: Int) = repos.notes.load(deckId, pageIndex)
                            override suspend fun save(deckId: String, pageIndex: Int, text: String) = repos.notes.save(deckId, pageIndex, text)
                        },
                        scope = scope,
                    )
                }
                val search = remember { LocalSearch(repos.notes, repos.dictionary) }
                val decks by repos.decks.decks.collectAsState(null)

                LaunchedEffect(decks) {
                    val list = decks ?: return@LaunchedEffect
                    if (controller.activeDeck != null || list.isEmpty()) return@LaunchedEffect
                    val savedId = repos.settings.get(SettingsRepository.ACTIVE_DECK)
                    controller.openDeck(list.firstOrNull { it.id == savedId } ?: list.first())
                }

                LectureShell(
                    repos = repos,
                    controller = controller,
                    session = session,
                    notesEditor = notesEditor,
                    search = search,
                    theme = theme,
                    onThemeChange = { next ->
                        theme = next
                        scope.launch { repos.settings.put(SettingsRepository.THEME_PREFERENCE, next.storage) }
                    },
                    interactivity = interactivity,
                    onInteractivityChange = { next ->
                        interactivity = next
                        scope.launch { repos.settings.put(SettingsRepository.INTERACTIVITY, next.toString()) }
                    },
                    animals = animals,
                    onAnimalsChange = { next ->
                        animals = next
                        scope.launch { repos.settings.put(SettingsRepository.ANIMALS, next.toString()) }
                    },
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        content()
    }
}
