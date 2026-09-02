package com.tnb88.torboxprovider

import android.content.Context
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class TorBoxProvider(private val context: Context) : MainAPI() {
    override var mainUrl = "https://api.themoviedb.org/3"
    override var name = "TorBox Việt"
    override var lang = "vi"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)

    override val mainPage = mainPageOf(
        "trending/all/week" to "Phim đang thịnh hành",
        "movie/popular" to "Phim lẻ phổ biến",
        "tv/popular" to "Phim bộ phổ biến",
        "discover/tv?with_keywords=210024%7C222243&sort_by=popularity.desc" to "Anime phổ biến"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val separator = if (request.data.contains('?')) '&' else '?'
        val url = "$mainUrl/${request.data}${separator}api_key=$TMDB_KEY&language=vi-VN&include_adult=false&page=$page"
        val results = parseMediaList(JSONObject(app.get(url).text))
        return newHomePageResponse(
            listOf(HomePageList(request.name, results, true)),
            results.isNotEmpty()
        )
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$mainUrl/search/multi?api_key=$TMDB_KEY&language=vi-VN&include_adult=false&query=$encoded&page=1"
        return parseMediaList(JSONObject(app.get(url).text))
    }

    private fun parseMediaList(root: JSONObject): List<SearchResponse> {
        val array = root.optJSONArray("results") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = item.optString("media_type").ifBlank {
                    if (item.has("title")) "movie" else "tv"
                }
                if (type !in setOf("movie", "tv")) continue
                val id = item.optInt("id")
                if (id <= 0) continue
                val title = item.optString("title").ifBlank {
                    item.optString("name").ifBlank { "Chưa rõ tên" }
                }
                val posterPath = item.optString("poster_path")
                val mediaUrl = "https://www.themoviedb.org/$type/$id"
                add(newMovieSearchResponse(title, mediaUrl, if (type == "movie") TvType.Movie else TvType.TvSeries) {
                    posterUrl = image(posterPath, "w500")
                })
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val match = Regex("themoviedb\\.org/(movie|tv)/(\\d+)").find(url)
            ?: error("Liên kết TMDB không hợp lệ")
        val mediaType = match.groupValues[1]
        val id = match.groupValues[2]
        val detailUrl = "$mainUrl/$mediaType/$id?api_key=$TMDB_KEY&language=vi-VN&append_to_response=external_ids,credits"
        val detail = JSONObject(app.get(detailUrl).text)

        var overview = detail.optString("overview")
        if (overview.isBlank()) {
            overview = runCatching {
                JSONObject(app.get("$mainUrl/$mediaType/$id?api_key=$TMDB_KEY&language=en-US").text)
                    .optString("overview")
            }.getOrDefault("")
        }

        val title = detail.optString("title").ifBlank {
            detail.optString("name").ifBlank { "Chưa rõ tên" }
        }
        val poster = image(detail.optString("poster_path"), "w500")
        val backdrop = image(detail.optString("backdrop_path"), "w1280")
        val date = detail.optString("release_date").ifBlank { detail.optString("first_air_date") }
        val yearValue = date.take(4).toIntOrNull()
        val imdb = detail.optJSONObject("external_ids")?.optString("imdb_id").orEmpty()
        val genres = detail.optJSONArray("genres")?.strings("name").orEmpty()
        val cast = detail.optJSONObject("credits")?.optJSONArray("cast")?.let { castArray ->
            buildList {
                for (index in 0 until minOf(castArray.length(), 12)) {
                    val person = castArray.optJSONObject(index) ?: continue
                    val actorName = person.optString("name")
                    if (actorName.isBlank()) continue
                    add(ActorData(Actor(actorName, image(person.optString("profile_path"), "w185").orEmpty())))
                }
            }
        }

        if (mediaType == "movie") {
            return newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                playbackData(imdb, 0, 0)
            ) {
                plot = overview
                posterUrl = poster
                backgroundPosterUrl = backdrop
                year = yearValue
                tags = genres
                actors = cast
            }
        }

        val seasons = detail.optJSONArray("seasons") ?: JSONArray()
        val episodes = buildList {
            for (seasonIndex in 0 until seasons.length()) {
                val season = seasons.optJSONObject(seasonIndex) ?: continue
                val seasonNumber = season.optInt("season_number")
                val episodeCount = season.optInt("episode_count")
                if (seasonNumber <= 0 || episodeCount <= 0) continue
                val seasonPoster = image(season.optString("poster_path"), "w500") ?: poster
                for (episodeNumber in 1..episodeCount) {
                    add(newEpisode(playbackData(imdb, seasonNumber, episodeNumber)) {
                        name = "Tập $episodeNumber"
                        this.season = seasonNumber
                        episode = episodeNumber
                        posterUrl = seasonPoster
                    })
                }
            }
        }

        val isAnime = genres.any { it.equals("Hoạt Hình", true) || it.equals("Animation", true) }
        return newTvSeriesLoadResponse(
            title,
            url,
            if (isAnime) TvType.Anime else TvType.TvSeries,
            episodes
        ) {
            plot = overview
            posterUrl = poster
            backgroundPosterUrl = backdrop
            year = yearValue
            tags = genres
            actors = cast
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val token = TorBoxConfig.token(context)
        if (token.isBlank()) {
            throw IllegalStateException("Chưa cấu hình TorBox. Mở Cài đặt của TorBox Việt để nhập mã nhanh hoặc API key.")
        }

        val request = JSONObject(data)
        val imdb = request.optString("imdb")
        val season = request.optInt("season")
        val episode = request.optInt("episode")
        if (!imdb.matches(Regex("tt\\d+"))) {
            throw IllegalStateException("Phim này chưa có mã IMDb nên chưa thể tìm nguồn TorBox.")
        }

        loadSubtitles(imdb, season, episode, subtitleCallback)

        val encodedKey = URLEncoder.encode(token, "UTF-8")
        val config = "sort=qualitysize%7Climit=25%7CTorBox=$encodedKey"
        val kind = if (season > 0) "series" else "movie"
        val mediaId = if (season > 0) "$imdb:$season:$episode" else imdb
        val endpoint = "$TORRENTIO/$config/stream/$kind/$mediaId.json"
        val streams = JSONObject(app.get(endpoint).text).optJSONArray("streams") ?: JSONArray()

        var added = 0
        val candidates = buildList {
            for (index in 0 until streams.length()) {
                val stream = streams.optJSONObject(index) ?: continue
                val diagnostic = (stream.optString("name") + " " + stream.optString("title"))
                    .lowercase(Locale.ROOT)
                if (diagnostic.contains("error") ||
                    diagnostic.contains("invalid") ||
                    diagnostic.contains("api key") ||
                    diagnostic.contains("apikey") ||
                    diagnostic.contains("token!")
                ) continue
                val directUrl = stream.optString("url")
                if (!directUrl.startsWith("http")) continue
                add(StreamSource.from(stream, directUrl))
            }
        }.sortedWith(compareByDescending<StreamSource> { it.quality }.thenByDescending { it.sizeBytes })

        candidates.take(25).forEach { source ->
            callback(newExtractorLink(name, source.label, source.url) {
                quality = source.quality
                referer = ""
            })
            added++
        }

        if (added == 0) {
            throw IllegalStateException("TorBox chưa trả nguồn phát trực tiếp. Hãy kiểm tra mã kích hoạt/API key hoặc chọn phim khác.")
        }
        return true
    }

    private suspend fun loadSubtitles(
        imdb: String,
        season: Int,
        episode: Int,
        callback: (SubtitleFile) -> Unit
    ) {
        runCatching {
            val path = if (season > 0) {
                "subtitles/series/$imdb:$season:$episode.json"
            } else {
                "subtitles/movie/$imdb.json"
            }
            val array = JSONObject(app.get("$SUBTITLES/$path").text).optJSONArray("subtitles") ?: JSONArray()
            for (index in 0 until minOf(array.length(), 40)) {
                val item = array.optJSONObject(index) ?: continue
                val subtitleUrl = item.optString("url")
                if (!subtitleUrl.startsWith("http")) continue
                val language = item.optString("lang").ifBlank { "Phụ đề" }
                callback(SubtitleFile(language, subtitleUrl))
            }
        }
    }

    private fun playbackData(imdb: String, season: Int, episode: Int): String =
        JSONObject().put("imdb", imdb).put("season", season).put("episode", episode).toString()

    private fun image(path: String?, size: String): String? =
        path?.takeIf { it.isNotBlank() && it != "null" }?.let { "https://image.tmdb.org/t/p/$size$it" }

    private fun JSONArray.strings(field: String): List<String> = buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.optString(field)?.takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    private data class StreamSource(
        val url: String,
        val label: String,
        val quality: Int,
        val sizeBytes: Long
    ) {
        companion object {
            private val qualityRegex = Regex("(?i)(2160p|1080p|720p|480p|4k)")
            private val sizeRegex = Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(TB|GB|MB)")
            private val codecRegex = Regex("(?i)(Dolby[ .]?Vision|DV|HDR10\\+?|HDR|HEVC|H\\.?265|x265|AV1|H\\.?264|x264|REMUX|WEB[- .]?DL|BLU[- ]?RAY)")

            fun from(json: JSONObject, url: String): StreamSource {
                val raw = listOf(json.optString("name"), json.optString("title"))
                    .filter { it.isNotBlank() }.joinToString("\n")
                val qualityText = qualityRegex.find(raw)?.value?.uppercase(Locale.ROOT).orEmpty()
                val quality = when (qualityText) {
                    "2160P", "4K" -> Qualities.P2160.value
                    "1080P" -> Qualities.P1080.value
                    "720P" -> Qualities.P720.value
                    "480P" -> Qualities.P480.value
                    else -> Qualities.Unknown.value
                }
                val sizeMatch = sizeRegex.find(raw)
                val sizeText = sizeMatch?.value?.uppercase(Locale.ROOT).orEmpty()
                val sizeBytes = sizeMatch?.let {
                    val amount = it.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
                    val multiplier = when (it.groupValues[2].uppercase(Locale.ROOT)) {
                        "TB" -> 1_099_511_627_776.0
                        "GB" -> 1_073_741_824.0
                        else -> 1_048_576.0
                    }
                    (amount * multiplier).toLong()
                } ?: 0L
                val codecs = codecRegex.findAll(raw).map { it.value.uppercase(Locale.ROOT) }
                    .distinct().take(3).toList()
                val parts = buildList {
                    add("TorBox")
                    if (qualityText.isNotBlank()) add(qualityText.replace("2160P", "4K"))
                    addAll(codecs)
                    if (sizeText.isNotBlank()) add(sizeText)
                }
                val release = json.optString("title").lineSequence().firstOrNull()
                    ?.take(80)?.trim().orEmpty()
                val label = parts.joinToString(" • ") + if (release.isBlank()) "" else "\n$release"
                return StreamSource(url, label, quality, sizeBytes)
            }
        }
    }

    private companion object {
        // Public TMDB v3 key already used by the upstream TorraStream provider.
        const val TMDB_KEY = "1865f43a0549ca50d341dd9ab8b29f49"
        const val TORRENTIO = "https://torrentio.strem.fun"
        const val SUBTITLES = "https://opensubtitles-v3.strem.io"
    }
}
