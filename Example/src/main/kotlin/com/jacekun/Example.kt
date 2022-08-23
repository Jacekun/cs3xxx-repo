package com.jacekun

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

class Example : MainAPI() {
    private val globalTvType = TvType.Movie
    override var name = "Example"
    override var mainUrl = "https://"
    override val supportedTypes: Set<TvType> get() = setOf(TvType.NSFW)
    override val hasDownloadSupport: Boolean get() = false
    override val hasMainPage: Boolean get() = true
    override val hasQuickSearch: Boolean get() = false

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        throw NotImplementedError()
    }

    override suspend fun search(query: String): List<SearchResponse> {
        throw NotImplementedError()
    }

    override suspend fun load(url: String): LoadResponse {
        throw NotImplementedError()
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        throw NotImplementedError()
    }
}