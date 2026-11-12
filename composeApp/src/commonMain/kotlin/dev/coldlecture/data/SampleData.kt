package dev.coldlecture.data

const val SAMPLE_DECK_ID = "sample-lecture-04"
const val SAMPLE_DECK_RESOURCE = "files/sample_lecture.pdf"

/** Fills an empty database with the bundled lecture, a starter glossary and a few todos. */
suspend fun seedIfEmpty(database: AppDatabase) {
    val now = nowMillis()
    val deckDao = database.deckDao()
    if (deckDao.count() == 0) {
        deckDao.upsert(
            LectureDeck(
                id = SAMPLE_DECK_ID,
                title = "Lecture 04 · Integrals & Stokes' Theorem",
                courseCode = "MAT-304",
                courseTitle = "Advanced Calculus & Vector Fields",
                source = DeckSource.BUNDLED,
                pageCount = 9,
                createdAt = now,
            ),
        )
        database.noteDao().upsert(
            SlideNote(
                deckId = SAMPLE_DECK_ID,
                pageIndex = 6,
                text = "- Watch out: **Δf** here is the Laplace-Beltrami operator applied to the scalar boundary.\n" +
                    "- Prof mentioned this exact integral is on the *Midterm 2*.",
                updatedAt = now,
            ),
        )
    }

    val dictDao = database.dictionaryDao()
    if (dictDao.count() == 0) {
        val ids = mutableMapOf<String, Long>()
        val starter = listOf(
            Triple("Function", "A relation that associates each element x ∈ X with exactly one output y ∈ Y.", "Calculus"),
            Triple("Limit", "The value a function approaches as its input approaches some point.", "Calculus"),
            Triple("Differential", "Linear approximation of the change of a function: df = f'(x) dx.", "Calculus"),
            Triple("Laplacian", "Δf = div(grad f); sum of the unmixed second partial derivatives.", "Vector calculus"),
            Triple("Kernel", "The set of inputs a linear map sends to zero.", "Linear algebra"),
            Triple("Exterior derivative", "Operator d taking k-forms to (k+1)-forms, with d∘d = 0.", "Differential forms"),
            Triple("Stokes' theorem", "∫∂Ω ω = ∫Ω dω: the boundary integral equals the integral of the derivative inside.", "Vector calculus"),
        )
        for ((term, definition, tag) in starter) {
            ids[term] = dictDao.insert(DictEntry(term = term, definition = definition, tag = tag, createdAt = now))
        }
        listOf(
            "Function" to "Differential",
            "Function" to "Laplacian",
            "Function" to "Kernel",
            "Function" to "Limit",
            "Differential" to "Exterior derivative",
            "Exterior derivative" to "Stokes' theorem",
            "Laplacian" to "Stokes' theorem",
        ).forEach { (a, b) ->
            val x = ids.getValue(a)
            val y = ids.getValue(b)
            dictDao.insertLink(ConceptLink(minOf(x, y), maxOf(x, y)))
        }
    }

    val todoDao = database.todoDao()
    if (todoDao.count() == 0) {
        listOf(
            "Redo Ex 2: flux through the unit sphere",
            "Problem set 4 — exercises 3, 7, 11",
            "Ask about orientation sign convention in office hours",
        ).forEachIndexed { i, text -> todoDao.insert(TodoItem(text = text, createdAt = now + i)) }
    }
}
