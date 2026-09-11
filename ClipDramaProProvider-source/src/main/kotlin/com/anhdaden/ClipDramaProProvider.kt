package com.anhdaden

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer

class ClipDramaProProvider : MainAPI() {
    override var mainUrl = DEFAULT_DOMAIN
    override var name = "Clip Drama Pro"
    override var lang = "vi"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "banner" to "Phim Nổi Bật",
        "51002744" to "Mới Cập Nhật",
        "51002745" to "Bảng Xếp Hạng",
        "51002746" to "Thân Phận Bí Ẩn",
        "51002747" to "Tình Yêu Sét Đánh",
        "51002748" to "Phim Xúc Động",
        "51002749" to "Anh Hùng Trở Lại",
    )

    private suspend fun activeDomain(): String {
        mainUrl = ClipDramaDomain.resolve()
        return mainUrl
    }

    private suspend fun fetchPage(url: String, referer: String? = null): PageProps {
        val response = app.get(url, headers = WEB_HEADERS, referer = referer)
        val script = response.document.selectFirst("script#__NEXT_DATA__")
            ?: throw IllegalStateException("ReelShort đã thay đổi cấu trúc trang")
        val json = script.data().ifBlank { script.html() }
        return tryParseJson<NextRoot>(json)?.props?.pageProps
            ?: throw IllegalStateException("Không đọc được dữ liệu ReelShort")
    }

    private suspend fun fetchHome(): Pair<PageProps, org.jsoup.nodes.Document> {
        val domain = activeDomain()
        val response = app.get("$domain/vi/", headers = WEB_HEADERS)
        val script = response.document.selectFirst("script#__NEXT_DATA__")
            ?: throw IllegalStateException("ReelShort đã thay đổi cấu trúc trang")
        val json = script.data().ifBlank { script.html() }
        val props = tryParseJson<NextRoot>(json)?.props?.pageProps
            ?: throw IllegalStateException("Không đọc được dữ liệu ReelShort")
        return props to response.document
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = activeDomain()
        val (homeProps, homeDocument) = fetchHome()
        val info = homeProps.fallback[HOME_INFO_KEY]
            ?: homeProps.fallback.values.firstOrNull { it.bookShelfList.isNotEmpty() }
            ?: throw IllegalStateException("Không tìm thấy danh mục ReelShort")

        if (request.data == "banner") {
            val items = info.banners.mapNotNull { banner ->
                val book = banner.jumpParam ?: return@mapNotNull null
                book.toSearchResponse(domain, banner.pic)
            }.distinctBy { it.url }
            return newHomePageResponse(request.name, items, false)
        }

        val shelfId = request.data
        val shelf = info.bookShelfList.firstOrNull { it.shelfId == shelfId }
        val books: List<BookItem>
        val hasNext: Boolean
        if (page <= 1) {
            books = shelf?.allBooks.orEmpty()
            hasNext = books.isNotEmpty()
        } else {
            val shelfPath = homeDocument.select("a[href*='/shelf/']")
                .map { it.attr("href") }
                .firstOrNull { it.substringBefore('?').trimEnd('/').endsWith("-$shelfId") }
                ?: throw IllegalStateException("Không tìm thấy đường dẫn danh mục")
            val shelfUrl = absoluteUrl(shelfPath, domain)
            val props = fetchPage("${shelfUrl.trimEnd('/')}/$page", "$domain/vi/")
            books = props.list
            hasNext = page < (props.totalPage ?: page)
        }

        val items = books.mapNotNull { it.toSearchResponse(domain) }.distinctBy { it.url }
        return newHomePageResponse(request.name, items, hasNext && items.isNotEmpty())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = activeDomain()
        val keyword = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
        val props = fetchPage("$domain/vi/search?keywords=$keyword&page=1", "$domain/vi/")
        return props.books.mapNotNull { it.toSearchResponse(domain) }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = activeDomain()
        val path = runCatching { URI(url.substringBefore('?')).rawPath }.getOrNull()
            ?: "/vi/movie/${url.substringBefore('?').substringAfterLast('/')}"
        val canonicalUrl = "$domain$path"
        val data = fetchPage(canonicalUrl, "$domain/vi/").data
            ?: throw IllegalStateException("Không tìm thấy nội dung phim")
        val title = data.bookTitle.ifBlank { "Clip Drama" }
        val bookId = data.bookId.ifBlank {
            Regex("-([a-zA-Z0-9]+)$").find(path)?.groupValues?.getOrNull(1).orEmpty()
        }
        if (bookId.isBlank()) throw IllegalStateException("Không đọc được mã phim")

        val episodes = data.onlineBase.sortedBy { it.serialNumber }.mapNotNull { item ->
            if (item.chapterId.isBlank()) return@mapNotNull null
            val serial = item.serialNumber.takeIf { it > 0 } ?: return@mapNotNull null
            val episodeUrl = "$domain/vi/episodes/episode-$serial-${slugify(title)}-$bookId-${item.chapterId}"
            newEpisode(EpisodePayload(episodeUrl, serial).toJson()) {
                name = "Tập $serial"
                episode = serial
                posterUrl = item.videoPic.ifBlank { data.bookPic }
            }
        }
        if (episodes.isEmpty()) throw IllegalStateException("Phim chưa có tập")

        return newTvSeriesLoadResponse(title, canonicalUrl, TvType.TvSeries, episodes) {
            posterUrl = data.bookPic
            backgroundPosterUrl = data.onlineBase.firstOrNull()?.videoPic?.ifBlank { null }
            plot = data.specialDesc
            tags = (data.tags + data.tagList.map { it.displayText }).filter { it.isNotBlank() }.distinct()
            showStatus = if (data.chapterCount > 0 && episodes.size >= data.chapterCount) {
                ShowStatus.Completed
            } else {
                ShowStatus.Ongoing
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val payload = tryParseJson<EpisodePayload>(data) ?: return false
        val domain = activeDomain()
        val path = runCatching { URI(payload.pageUrl.substringBefore('?')).rawPath }.getOrNull()
            ?: return false
        val pageUrl = "$domain$path"
        val episode = fetchPage(pageUrl, pageUrl).data ?: return false
        val stream = episode.videoUrl.trim()
        if (stream.isBlank()) return false

        val headers = WEB_HEADERS + mapOf("Referer" to pageUrl)
        val generated = if (stream.contains(".m3u8", true)) {
            runCatching {
                M3u8Helper.generateM3u8(name, stream, pageUrl, headers = headers)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        if (generated.isNotEmpty()) {
            generated.forEach(callback)
        } else {
            callback(
                newExtractorLink(
                    source = name,
                    name = "$name - Tập ${payload.serial}",
                    url = stream,
                    type = if (stream.contains(".m3u8", true)) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                ) {
                    referer = pageUrl
                    quality = Qualities.Unknown.value
                    this.headers = headers
                },
            )
        }
        return true
    }

    private fun BookItem.toSearchResponse(domain: String, posterOverride: String? = null): SearchResponse? {
        if (bookId.isBlank() || bookTitle.isBlank()) return null
        return newTvSeriesSearchResponse(
            bookTitle,
            "$domain/vi/movie/${slugify(bookTitle)}-$bookId",
            TvType.TvSeries,
        ) {
            posterUrl = bookPic.ifBlank { posterOverride }
        }
    }

    private fun absoluteUrl(raw: String, domain: String): String = when {
        raw.startsWith("https://") || raw.startsWith("http://") -> raw
        raw.startsWith("//") -> "https:$raw"
        else -> "$domain/${raw.trimStart('/')}"
    }

    private fun slugify(value: String): String {
        val normalized = Normalizer.normalize(
            value.replace('Đ', 'D').replace('đ', 'd'),
            Normalizer.Form.NFD,
        ).replace(Regex("\\p{M}+"), "")
        return normalized.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "movie" }
    }

    companion object {
        private const val DEFAULT_DOMAIN = "https://www.reelshort.com"
        private const val HOME_INFO_KEY = "/api/ms/hall/webInfo"
        private val WEB_HEADERS = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Accept-Language" to "vi-VN,vi;q=0.9,en-US;q=0.7,en;q=0.6",
        )
    }
}

private object ClipDramaDomain {
    private const val FALLBACK = "https://www.reelshort.com"
    private const val REMOTE = "https://raw.githubusercontent.com/TNB88/FSHARE/refs/heads/main/domainclipdrama.txt"
    private const val CACHE_MS = 5 * 60 * 1000L

    @Volatile private var cached = FALLBACK
    @Volatile private var validUntil = 0L

    suspend fun resolve(): String {
        val now = System.currentTimeMillis()
        if (now < validUntil) return cached
        val candidate = runCatching {
            app.get(REMOTE).text.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("https://") }
                ?.trimEnd('/')
        }.getOrNull()
        if (!candidate.isNullOrBlank() && candidate.matches(Regex("https://[^\\s/]+(?:/.*)?"))) {
            cached = candidate
        }
        validUntil = now + CACHE_MS
        return cached
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NextRoot(
    val props: NextProps = NextProps(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NextProps(
    @JsonProperty("pageProps") val pageProps: PageProps = PageProps(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PageProps(
    val fallback: Map<String, HomeInfo> = emptyMap(),
    val books: List<BookItem> = emptyList(),
    val list: List<BookItem> = emptyList(),
    val data: DetailData? = null,
    val page: Int? = null,
    @JsonProperty("totalPage") val totalPage: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class HomeInfo(
    val banners: List<BannerItem> = emptyList(),
    @JsonProperty("bookShelfList") val bookShelfList: List<ShelfItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BannerItem(
    val pic: String = "",
    @JsonProperty("jump_param") val jumpParam: BookItem? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ShelfItem(
    @JsonProperty("bs_id") val shelfId: String = "",
    val books: List<BookItem> = emptyList(),
    @JsonProperty("book_list") val bookList: List<BookItem> = emptyList(),
) {
    val allBooks: List<BookItem> get() = books.ifEmpty { bookList }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class BookItem(
    @JsonProperty("book_id") val bookId: String = "",
    @JsonProperty("book_title") val bookTitle: String = "",
    @JsonProperty("book_pic") val bookPic: String = "",
    @JsonProperty("special_desc") val specialDesc: String = "",
    @JsonProperty("chapter_count") val chapterCount: Int = 0,
    @JsonProperty("paid_start") val paidStart: Int = 0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DetailData(
    @JsonProperty("book_id") val bookId: String = "",
    @JsonProperty("book_title") val bookTitle: String = "",
    @JsonProperty("book_pic") val bookPic: String = "",
    @JsonProperty("special_desc") val specialDesc: String = "",
    @JsonProperty("chapter_count") val chapterCount: Int = 0,
    @JsonProperty("online_base") val onlineBase: List<EpisodeItem> = emptyList(),
    @JsonProperty("video_url") val videoUrl: String = "",
    val tag: List<String> = emptyList(),
    @JsonProperty("tag_list") val tagList: List<TagItem> = emptyList(),
) {
    val tags: List<String> get() = tag
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class EpisodeItem(
    @JsonProperty("chapter_id") val chapterId: String = "",
    @JsonProperty("serial_number") val serialNumber: Int = 0,
    @JsonProperty("video_pic") val videoPic: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TagItem(
    val text: String = "",
    @JsonProperty("tag_name") val tagName: String = "",
) {
    val displayText: String get() = text.ifBlank { tagName }
}

private data class EpisodePayload(
    val pageUrl: String,
    val serial: Int,
)
