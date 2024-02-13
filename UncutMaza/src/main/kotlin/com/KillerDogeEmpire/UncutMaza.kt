package com.KillerDogeEmpire

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.VPNStatus
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixTitle
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class UncutMaza : MainAPI() {
    override var mainUrl = "https://uncutmaza.com"
    override var name = "Uncutmaza"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
            "$mainUrl/page/" to "Home", "$mainUrl/category/niks-indian-porn/page/" to "Niks Indian"
    )

    override suspend fun getMainPage(
            page: Int, request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.videos-list > article.post").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(
                list = HomePageList(
                        name = request.name, list = home, isHorizontalImages = true
                ), hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = fixTitle(this.select("a").attr("title"))
        val href = fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(
                this.select("a > div.post-thumbnail>div.post-thumbnail-container>img").attr("data-src")
        )
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..5) {
            val document = app.get(
                    "$mainUrl/page/$i?s=$query"
            ).document
            val results = document.select("article.post").mapNotNull {
                it.toSearchResult()
            }
            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }
            if (results.isEmpty()) break
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title =
                document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
        val poster =
                fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content").toString())
        val description =
                document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("div.video-player").map { res ->
            callback.invoke(
                    ExtractorLink(
                            this.name, this.name, fixUrl(
                            res.selectFirst("meta[itemprop=contentURL]")?.attr("content")?.trim()
                                    .toString()
                    ), referer = data, quality = Qualities.Unknown.value
                    )
            )
        }

        return true
    }

}