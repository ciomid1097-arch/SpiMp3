package com.spimp3.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int?,
    val trackCount: Int,
    val durationMs: Long,
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
)

data class MusicFolder(
    val path: String,
    val name: String,
    val trackCount: Int,
)
