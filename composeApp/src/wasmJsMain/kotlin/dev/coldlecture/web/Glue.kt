@file:JsModule("cold-lecture-web")

package dev.coldlecture.web

import kotlin.js.Promise

external fun pdfOpenBase64(base64: String): Promise<JsNumber>
external fun pdfOpenStored(deckId: String): Promise<JsNumber>
external fun pdfPageCount(handle: Int): Int
external fun pdfRenderPage(handle: Int, pageIndex: Int, targetWidth: Int): Promise<JsString>
external fun pdfPageText(handle: Int, pageIndex: Int): Promise<JsString>
external fun pdfClose(handle: Int)

external fun pickPdfFile(): Promise<JsString?>
external fun importPickedFile(deckId: String): Promise<JsNumber>
external fun deleteStoredDeck(deckId: String): Promise<JsAny?>

external fun setLockActive(active: Boolean)
external fun requestFullscreen()
external fun exitFullscreen()
external fun watchPresence(callback: (String) -> Unit)
