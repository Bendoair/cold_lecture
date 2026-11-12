// Browser-only helpers called from Kotlin/Wasm: PDF rendering (pdf.js), file picking and
// OPFS storage for imported decks. Binary payloads cross the Wasm boundary as base64 strings.
import { GlobalWorkerOptions, getDocument } from 'pdfjs-dist';

GlobalWorkerOptions.workerPort = new Worker(
    new URL('pdfjs-dist/build/pdf.worker.min.mjs', import.meta.url),
    { type: 'module' },
);

const documents = new Map();
let nextHandle = 1;
const memoryDecks = new Map();
let pickedFile = null;

function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
}

async function openBytes(bytes) {
    const doc = await getDocument({ data: bytes }).promise;
    const handle = nextHandle++;
    documents.set(handle, doc);
    return handle;
}

export function pdfOpenBase64(base64) {
    return openBytes(base64ToBytes(base64));
}

export async function pdfOpenStored(deckId) {
    const bytes = await loadDeckBytes(deckId);
    if (!bytes) throw new Error('Deck file missing: ' + deckId);
    return openBytes(bytes);
}

export function pdfPageCount(handle) {
    return documents.get(handle).numPages;
}

export async function pdfRenderPage(handle, pageIndex, targetWidth) {
    const page = await documents.get(handle).getPage(pageIndex + 1);
    const base = page.getViewport({ scale: 1 });
    const viewport = page.getViewport({ scale: Math.max(0.5, targetWidth / base.width) });
    const canvas = document.createElement('canvas');
    canvas.width = Math.ceil(viewport.width);
    canvas.height = Math.ceil(viewport.height);
    const context = canvas.getContext('2d');
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    await page.render({ canvas, canvasContext: context, viewport }).promise;
    page.cleanup();
    return canvas.toDataURL('image/png');
}

export async function pdfPageText(handle, pageIndex) {
    const page = await documents.get(handle).getPage(pageIndex + 1);
    const content = await page.getTextContent();
    const lines = [];
    let current = '';
    for (const item of content.items) {
        current += item.str;
        if (item.hasEOL) {
            lines.push(current);
            current = '';
        } else {
            current += ' ';
        }
    }
    if (current.trim()) lines.push(current);
    return lines.map((line) => line.trim()).filter((line) => line).join('\n');
}

export function pdfClose(handle) {
    const doc = documents.get(handle);
    if (doc) {
        doc.destroy();
        documents.delete(handle);
    }
}

// Resolves to the picked file name, or null when the dialog is dismissed.
export function pickPdfFile() {
    return new Promise((resolve) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'application/pdf,.pdf';
        input.style.display = 'none';
        let settled = false;
        const finish = (file) => {
            if (settled) return;
            settled = true;
            pickedFile = file;
            input.remove();
            resolve(file ? file.name : null);
        };
        input.addEventListener('change', () => finish(input.files && input.files[0]));
        input.addEventListener('cancel', () => finish(null));
        document.body.appendChild(input);
        input.click();
    });
}

// Stores the last picked file under deckId and resolves to its page count.
export async function importPickedFile(deckId) {
    if (!pickedFile) throw new Error('No file picked');
    const bytes = new Uint8Array(await pickedFile.arrayBuffer());
    pickedFile = null;
    const doc = await getDocument({ data: bytes.slice() }).promise;
    const pageCount = doc.numPages;
    doc.destroy();
    await saveDeckBytes(deckId, bytes);
    return pageCount;
}

async function decksDirectory() {
    if (!navigator.storage || !navigator.storage.getDirectory) return null;
    const root = await navigator.storage.getDirectory();
    return root.getDirectoryHandle('decks', { create: true });
}

async function saveDeckBytes(deckId, bytes) {
    try {
        const dir = await decksDirectory();
        if (dir) {
            const fileHandle = await dir.getFileHandle(deckId + '.pdf', { create: true });
            const writable = await fileHandle.createWritable();
            await writable.write(bytes);
            await writable.close();
            return;
        }
    } catch (error) {
        console.warn('OPFS write failed, keeping deck in memory only:', error && error.message);
    }
    memoryDecks.set(deckId, bytes);
}

async function loadDeckBytes(deckId) {
    if (memoryDecks.has(deckId)) return memoryDecks.get(deckId);
    try {
        const dir = await decksDirectory();
        if (!dir) return null;
        const fileHandle = await dir.getFileHandle(deckId + '.pdf');
        const file = await fileHandle.getFile();
        return new Uint8Array(await file.arrayBuffer());
    } catch (error) {
        return null;
    }
}

let lockActive = false;

function beforeUnloadGuard(event) {
    if (!lockActive) return;
    event.preventDefault();
    event.returnValue = '';
}

export function setLockActive(active) {
    lockActive = active;
    if (active) {
        window.addEventListener('beforeunload', beforeUnloadGuard);
    } else {
        window.removeEventListener('beforeunload', beforeUnloadGuard);
    }
}

export function requestFullscreen() {
    const root = document.documentElement;
    if (!document.fullscreenElement && root.requestFullscreen) {
        root.requestFullscreen().catch((error) => console.warn('Fullscreen refused:', error && error.message));
    }
}

export function exitFullscreen() {
    if (document.fullscreenElement && document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
    }
}

// Reports 'left' / 'returned' / 'fullscreen-exited' through callback.
export function watchPresence(callback) {
    let away = false;
    const leave = () => {
        if (!away) {
            away = true;
            callback('left');
        }
    };
    const back = () => {
        if (away && !document.hidden) {
            away = false;
            callback('returned');
        }
    };
    document.addEventListener('visibilitychange', () => (document.hidden ? leave() : back()));
    window.addEventListener('blur', leave);
    window.addEventListener('focus', back);
    document.addEventListener('fullscreenchange', () => {
        if (!document.fullscreenElement) callback('fullscreen-exited');
    });
}

export async function deleteStoredDeck(deckId) {
    memoryDecks.delete(deckId);
    try {
        const dir = await decksDirectory();
        if (dir) await dir.removeEntry(deckId + '.pdf');
    } catch (error) {
        // already gone
    }
}
