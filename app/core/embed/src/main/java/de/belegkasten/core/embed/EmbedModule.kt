package de.belegkasten.core.embed

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Swap the swap point for the real model: replace HashEmbedder with
// MediaPipeEmbedder once the EmbeddingGemma pack is wired (Phase 3.5).
@Module
@InstallIn(SingletonComponent::class)
abstract class EmbedModule {
    @Binds
    @Singleton
    abstract fun bindEmbedder(impl: HashEmbedder): Embedder
}
