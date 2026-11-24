package com.nuvio.app.features.livetv

import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.streams.StreamLaunch

// Episodic items need episode selection; channel catalogs need no meta resource to play.
internal fun MetaPreview.liveTvStreamLaunch(profileId: Int): StreamLaunch? =
    if (type.equals("series", ignoreCase = true)) null else StreamLaunch(
        profileId = profileId, type = type, videoId = id, parentMetaId = id,
        parentMetaType = type, title = name, poster = poster, logo = logo, background = banner,
    )

internal fun LiveTvChannel.toPlayerLaunch(profileId: Int) = PlayerLaunch(
    profileId = profileId, title = name, streamTitle = name, sourceUrl = streamUrl,
    sourceHeaders = headers, streamType = streamType, poster = logoUrl,
    providerName = playlistName ?: "Live TV", contentType = "tv", videoId = id,
    parentMetaId = id, parentMetaType = "tv", playbackOrigin = "live_tv",
)

internal fun LiveTvVodPlayback.toPlayerLaunch(profileId: Int): PlayerLaunch {
    val type = if (episodeNumber == null) "movie" else "series"
    return PlayerLaunch(
        profileId = profileId, title = title, streamTitle = episodeTitle ?: title,
        sourceUrl = streamUrl, sourceHeaders = headers, streamType = streamType, poster = posterUrl,
        seasonNumber = seasonNumber, episodeNumber = episodeNumber, episodeTitle = episodeTitle,
        episodeThumbnail = episodeThumbnail, providerName = "Xtream", contentType = type,
        videoId = id, parentMetaId = id, parentMetaType = type, playbackOrigin = "live_tv_vod",
    )
}
