# Cold Lecture

An in-lecture study app: a slide presenter with focus locking, per-slide notes, a personal concept dictionary and an after-class todo list. One Compose Multiplatform codebase runs as a **desktop app** (JVM) and in the **browser** (Kotlin/Wasm).

What each feature does for the student is described in [`docs/features/`](docs/features/):

- [Focus locking](docs/features/focus-locking.md)
- [Slide presenter](docs/features/slide-presenter.md)
- [Per-slide notes](docs/features/per-slide-notes.md)
- [Dictionary](docs/features/dictionary.md)
- [Todos](docs/features/todos.md)
- [Local insta-search](docs/features/local-search.md)
- [Next features (deferred)](docs/features/deferred.md)

## Requirements

- JDK 21 (the Gradle wrapper downloads Gradle itself)
- For the browser build: a browser with WasmGC support (Chrome/Edge 119+, Firefox 120+, Safari 18.2+). Node.js and the npm packages are downloaded by the Kotlin Gradle plugin.

## Run

| What | Command |
|---|---|
| Desktop app | `./gradlew :composeApp:run` |
| Browser, dev server with live reload | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| Browser, production bundle | `./gradlew :composeApp:wasmJsBrowserDistribution` (output in `composeApp/build/dist/wasmJs/productionExecutable/`) |
| Desktop installer for this OS | `./gradlew :composeApp:packageDistributionForCurrentOS` |
| Tests (shared + desktop) | `./gradlew :composeApp:desktopTest` |

You can serve the production browser bundle from any static file server that sends `.wasm` files as `application/wasm`. The browser build does **not** need special COOP/COEP headers.

## Where data lives

Everything is local; there is no backend.

| | Desktop | Browser |
|---|---|---|
| Database (decks, notes, dictionary, todos, settings) | `cold_lecture.db` in `~/.local/share/cold-lecture/` (Linux), `~/Library/Application Support/ColdLecture/` (macOS) or `%APPDATA%\ColdLecture\` (Windows) | SQLite in the origin-private file system (OPFS) |
| Imported PDFs | `decks/` next to the database | OPFS `decks/` directory |

If OPFS is unavailable (for example in some private browsing modes), the browser build keeps working but nothing persists after a reload.

## Project layout

```
composeApp/
  src/commonMain/      shared UI, state, Room entities/DAOs, bundled fonts + sample deck
  src/desktopMain/     desktop entry point, PDFBox renderer, file picker, window focus lock
  src/wasmJsMain/      browser entry point, pdf.js bridge, Fullscreen/visibility focus lock
  src/commonTest/      shared unit tests (run on the JVM via desktopTest)
  src/desktopTest/     Room + PDFBox tests
  web/                 JS glue bundled into the browser build:
                         glue.mjs          pdf.js rendering, file picking, OPFS deck storage, focus helpers
                         sqlite-worker.mjs Web Worker backing Room's WebWorkerSQLiteDriver (SQLite WASM + OPFS)
  schemas/             exported Room schema history
docs/features/         product docs (what, not how)
```

## Stack

- Kotlin 2.4, Compose Multiplatform 1.12, Material 3
- Room 3 (KMP) with KSP. On desktop it uses the bundled SQLite driver; in the browser it uses `androidx.sqlite:sqlite-web` with a SQLite WASM worker.
- PDF rendering with Apache PDFBox (desktop) and pdf.js (browser)

## Third-party assets

- IBM Plex Sans / Plex Mono fonts, SIL Open Font License 1.1 (see [`licenses/IBM-Plex-OFL.txt`](licenses/IBM-Plex-OFL.txt))
- pdf.js (Apache-2.0), SQLite WASM (public domain) and Apache PDFBox (Apache-2.0), pulled in as dependencies
