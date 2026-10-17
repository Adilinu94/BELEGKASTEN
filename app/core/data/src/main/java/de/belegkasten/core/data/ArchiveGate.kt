package de.belegkasten.core.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class ArchiveGate @Inject constructor() {
    private val mutex = Mutex()
    suspend fun <T> exclusive(action: suspend () -> T): T = mutex.withLock { action() }
}
