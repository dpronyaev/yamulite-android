package dev.pdv.yamulite.data.music

import dev.pdv.yamulite.data.music.dto.AlbumDto
import dev.pdv.yamulite.data.music.dto.AlbumWithTracksDto
import dev.pdv.yamulite.data.music.dto.ArtistBriefDto
import dev.pdv.yamulite.data.music.dto.ArtistDto
import dev.pdv.yamulite.data.music.dto.TrackDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResults(
    val tracks: List<TrackDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val albums: List<AlbumDto> = emptyList(),
)

enum class SearchType(val apiValue: String) {
    Tracks("track"),
    Artists("artist"),
    Albums("album"),
}

@Singleton
class MusicRepository @Inject constructor(
    private val api: MusicApi,
) {
    private val uidMutex = Mutex()
    private var cachedUid: Long? = null

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    suspend fun uid(): Long = uidMutex.withLock {
        cachedUid?.let { return@withLock it }
        val uid = api.accountStatus().result.account.uid
        cachedUid = uid
        uid
    }

    suspend fun search(text: String, type: SearchType): SearchResults {
        if (text.isBlank()) return SearchResults()
        val res = api.search(text, type.apiValue).result
        return SearchResults(
            tracks = res.tracks?.results.orEmpty(),
            artists = res.artists?.results.orEmpty(),
            albums = res.albums?.results.orEmpty(),
        )
    }

    suspend fun favorites(): List<TrackDto> {
        val u = uid()
        val refs = api.likedTracks(u).result.library.tracks
        if (refs.isEmpty()) {
            _likedIds.value = emptySet()
            return emptyList()
        }
        val ids = refs.joinToString(",") { it.id }
        val full = api.tracksByIds(ids).result
        val byId = full.associateBy { it.id }
        val ordered = refs.mapNotNull { byId[it.id] }
        _likedIds.value = ordered.map { it.id }.toSet()
        return ordered
    }

    suspend fun like(trackId: String): Result<Unit> = runCatching {
        val u = uid()
        api.likeTracks(u, trackId)
        _likedIds.update { it + trackId }
    }

    suspend fun unlike(trackId: String): Result<Unit> = runCatching {
        val u = uid()
        api.unlikeTracks(u, trackId)
        _likedIds.update { it - trackId }
    }

    suspend fun artistBrief(artistId: Long): ArtistBriefDto =
        api.artistBriefInfo(artistId).result

    suspend fun album(albumId: Long): AlbumWithTracksDto =
        api.albumWithTracks(albumId).result

    fun resetSession() {
        cachedUid = null
        _likedIds.value = emptySet()
    }
}
