// Implements the androidx.sqlite WebWorkerSQLiteDriver message protocol on top of SQLite WASM.
// Databases persist in OPFS through the SAH pool VFS (no COOP/COEP headers required);
// when OPFS is unavailable the database falls back to a transient in-memory file.
import sqlite3InitModule from '@sqlite.org/sqlite-wasm';

let sqlite3 = null;
let sahPool = null;

const databases = new Map();
const statements = new Map();
let nextDatabaseId = 1;
let nextStatementId = 1;

function reply(id, data) {
    postMessage({ id, data });
}

function fail(id, error) {
    postMessage({ id, error: error && error.message ? error.message : String(error) });
}

function openDatabase(fileName) {
    if (sahPool && fileName && fileName !== ':memory:') {
        const path = fileName.startsWith('/') ? fileName : '/' + fileName;
        return new sahPool.OpfsSAHPoolDb(path);
    }
    return new sqlite3.oo1.DB(':memory:', 'ct');
}

function openRequest(id, data) {
    try {
        const databaseId = nextDatabaseId++;
        databases.set(databaseId, openDatabase(data.fileName));
        reply(id, { databaseId });
    } catch (error) {
        fail(id, error);
    }
}

function prepareRequest(id, data) {
    const database = databases.get(data.databaseId);
    if (!database) {
        fail(id, 'Invalid database ID: ' + data.databaseId);
        return;
    }
    try {
        const statementId = nextStatementId++;
        const statement = database.prepare(data.sql);
        statements.set(statementId, statement);
        const columnNames = [];
        for (let i = 0; i < statement.columnCount; i++) {
            columnNames.push(sqlite3.capi.sqlite3_column_name(statement, i));
        }
        reply(id, {
            statementId,
            parameterCount: sqlite3.capi.sqlite3_bind_parameter_count(statement),
            columnNames,
        });
    } catch (error) {
        fail(id, error);
    }
}

function stepRequest(id, data) {
    const statement = statements.get(data.statementId);
    if (!statement) {
        fail(id, 'Invalid statement ID: ' + data.statementId);
        return;
    }
    try {
        const result = { rows: [], columnTypes: [] };
        statement.reset();
        statement.clearBindings();
        for (let i = 0; i < data.bindings.length; i++) {
            statement.bind(i + 1, data.bindings[i]);
        }
        while (statement.step()) {
            if (!result.columnTypes.length) {
                for (let i = 0; i < statement.columnCount; i++) {
                    result.columnTypes.push(sqlite3.capi.sqlite3_column_type(statement, i));
                }
            }
            result.rows.push(statement.get([]));
        }
        reply(id, result);
    } catch (error) {
        fail(id, error);
    }
}

function closeRequest(id, data) {
    try {
        if (data.statementId) {
            const statement = statements.get(data.statementId);
            if (statement) {
                statement.finalize();
                statements.delete(data.statementId);
            }
        }
        if (data.databaseId) {
            const database = databases.get(data.databaseId);
            if (database) {
                database.close();
                databases.delete(data.databaseId);
            }
        }
    } catch (error) {
        fail(id, error);
    }
}

const handlers = {
    open: openRequest,
    prepare: prepareRequest,
    step: stepRequest,
    close: closeRequest,
};

function handleMessage(event) {
    const request = event.data;
    if (!request || request.data == null || request.data.cmd == null) {
        fail(request && request.id, 'Invalid request');
        return;
    }
    const handler = handlers[request.data.cmd];
    if (handler) {
        handler(request.id, request.data);
    } else {
        fail(request.id, "Unknown command: '" + request.data.cmd + "'");
    }
}

const pending = [];
onmessage = (event) => {
    if (sqlite3) {
        handleMessage(event);
    } else {
        pending.push(event);
    }
};

sqlite3InitModule().then(async (instance) => {
    try {
        sahPool = await instance.installOpfsSAHPoolVfs({ name: 'cold-lecture-pool' });
    } catch (error) {
        console.warn('OPFS unavailable, using in-memory database:', error && error.message);
    }
    sqlite3 = instance;
    while (pending.length) {
        handleMessage(pending.shift());
    }
});
