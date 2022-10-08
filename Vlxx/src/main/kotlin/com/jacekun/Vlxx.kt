package com.jacekun

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.nicehttp.NiceResponse

class Vlxx : MainAPI() {
    private val DEV = "DevDebug"
    private val globaltvType = TvType.Movie

    override var name = "Vlxx"
    override var mainUrl = "https://vlxx.sex"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport = false
    override val hasMainPage = true
    override val hasQuickSearch = false
    private val interceptor = CloudflareKiller()

    private suspend fun getPage(url: String, referer: String): NiceResponse {
        var count = 0
        var resp = app.get(url, referer = referer, interceptor = interceptor)
        Log.i(DEV, "Page Response => ${resp}")
//        while (!resp.isSuccessful) {
//            resp = app.get(url, interceptor = interceptor)
//            count++
//            if (count > 4) {
//                return resp
//            }
//        }
        return resp
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val apiName = this.name
        val document = getPage(mainUrl, mainUrl).document
        val all = ArrayList<HomePageList>()
        val title = "Homepage"
        Log.i(DEV, "Fetching videos..")
        val elements = document.select("div#video-list > div.video-item")
            .mapNotNull {
                val firstA = it.selectFirst("a")
                val link = fixUrlNull(firstA?.attr("href")) ?: return@mapNotNull null
                val img = it.selectFirst("img")?.attr("data-original")
                val name = it.selectFirst("div.video-name")?.text() ?: it.text()
                Log.i(DEV, "Result => $link")
                newMovieSearchResponse(
                    name = name,
                    url = link,
                    type = globaltvType,
                ) {
                    //this.apiName = apiName
                    this.posterUrl = img
                }
            }.distinctBy { it.url }

        if (elements.isNotEmpty()) {
            all.add(
                HomePageList(
                    title, elements
                )
            )
        }
        return HomePageResponse(all)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return getPage("$mainUrl/search/${query}/", mainUrl).document
            .select("#container .box .video-list")
            .mapNotNull {
            val link = fixUrlNull(it.select("a").attr("href")) ?: return@mapNotNull null
            val imgArticle = it.select(".video-image").attr("src")
            val name = it.selectFirst(".video-name")?.text() ?: ""
            val year = null

            newMovieSearchResponse(
                name = name,
                url = link,
                type = globaltvType,
            ) {
                //this.apiName = apiName
                this.posterUrl = imgArticle
                this.year = year
                this.posterHeaders = interceptor.getCookieHeaders(url).toMap()
            }
        }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val apiName = this.name
        val doc = getPage(url, url).document

        val container = doc.selectFirst("div#container")
        val title = container?.selectFirst("h2")?.text() ?: "No Title"
        val descript = container?.selectFirst("div.video-description")?.text()
        val year = null
        val poster = null //No image on load page
        return newMovieLoadResponse(
            name = title,
            url = url,
            dataUrl = url,
            type = globaltvType,
        ) {
            this.apiName = apiName
            this.posterUrl = poster
            this.year = year
            this.plot = descript
            this.posterHeaders = interceptor.getCookieHeaders(url).toMap()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val pathSplits = data.split("/")
        val id = pathSplits[pathSplits.size - 2]
        Log.i(DEV, "Data -> ${data} id -> ${id}")
        val res = app.post(
            "${mainUrl}/ajax.php",
            headers = interceptor.getCookieHeaders(data).toMap(),
            data = mapOf(
                Pair("vlxx_server", "1"),
                Pair("id", id),
                Pair("server", "1"),
            ),
            referer = mainUrl
        ).text
        Log.i(DEV, "res ${res}")

        val json = getParamFromJS(res, "var opts = {\\r\\n\\t\\t\\t\\t\\t\\tsources:", "}]")
        Log.i(DEV, "json ${json}")
        json?.let {
            tryParseJson<List<Sources?>>(it)?.forEach { vidlink ->
                vidlink?.file?.let { file ->
                    callback.invoke(
                        ExtractorLink(
                            source = file,
                            name = this.name,
                            url = file,
                            referer = data,
                            quality = getQualityFromName(vidlink.label),
                            isM3u8 = file.endsWith("m3u8")
                        )
                    )
                }
            }
        }
        return true

    }

    private fun getParamFromJS(str: String, key: String, keyEnd: String): String? {
        try {
            val firstIndex = str.indexOf(key) + key.length // 4 to index point to first char.
            val temp = str.substring(firstIndex)
            val lastIndex = temp.indexOf(keyEnd) + (keyEnd.length)
            val jsonConfig = temp.substring(0, lastIndex) //
            Log.i(DEV, "jsonConfig ${jsonConfig}")

            val re = jsonConfig.replace("\\r", "")
                .replace("\\t", "")
                .replace("\\\"", "\"")
                .replace("\\\\\\/", "/")
                .replace("\\n", "")

            return re
        } catch (e: Exception) {
            //e.printStackTrace()
            logError(e)
        }
        return null
    }

    data class Sources(
        @JsonProperty("file") val file: String?,
        @JsonProperty("type") val type: String?,
        @JsonProperty("label") val label: String?
    )
}