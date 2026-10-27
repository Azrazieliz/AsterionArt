package com.asterion.art.brain

import kotlinx.coroutines.sync.Mutex

/** One llama.cpp singleton and one KV/model lifetime at a time across all creative domains. */
object BrainInferenceLock {
    val mutex=Mutex()
}
