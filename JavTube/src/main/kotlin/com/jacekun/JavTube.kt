package com.jacekun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.Jsoup

class JavTube : MainAPI() {
    private val DEV = "DevDebug"
    private val globaltvType = TvType.NSFW
    override var name = "JavTube"
    override var mainUrl = "https://javtube.watch"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport = true
    override val hasMainPage = true
    override val hasQuickSearch = false

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(mainUrl).document
        val all = ArrayList<HomePageList>()

        // Fetch row title
        val title = "Latest videos"
        // Fetch list of items and map
        val inner = document.selectFirst("div.videos-list")?.select("article") ?: return HomePageResponse(all)
        //Log.i(DEV, "Inner => $inner")
        val elements: List<SearchResponse> = inner.mapNotNull {

            //Log.i(DEV, "Inner content => $innerArticle")
            val aa = it.select("a").last() ?: return@mapNotNull null
            val link = fixUrlNull(aa.attr("href")) ?: return@mapNotNull null

            val imgArticle = aa.select("img")
            val name = imgArticle.attr("alt") ?: ""
            var image = imgArticle.attr("data-src")
            if (image.isNullOrEmpty()) {
                image = imgArticle.attr("src")
            }

            MovieSearchResponse(
                name = name,
                url = link,
                apiName = this.name,
                type = globaltvType,
                posterUrl = image,
                year = null,
                id = null,
            )
        }.distinctBy { a -> a.url }

        all.add(
            HomePageList(
                title, elements
            )
        )

        return HomePageResponse(all.filter { a -> a.list.isNotEmpty() })
    }

    override suspend fun search(query: String): List<MovieSearchResponse> {
        val url = "$mainUrl/search/$query"
        val document = app.get(url).document.select("article#post")

        return document.mapNotNull {
            val innerA = it?.selectFirst("a") ?: return@mapNotNull null
            val linkUrl = fixUrlNull(innerA.attr("href")) ?: return@mapNotNull null
            if (linkUrl.startsWith("https://javtube.watch/tag/")) {
                //Log.i(DEV, "Result => (innerA) $innerA")
                return@mapNotNull null
            }

            val title = innerA.select("header.entry-header").text()
            val imgLink = innerA.select("img")
            var image = imgLink.attr("data-src")
            if (image.isNullOrEmpty()) {
                image = imgLink.attr("src")
            }
            val year = null

            MovieSearchResponse(
                name = title,
                url = linkUrl,
                apiName = this.name,
                type = globaltvType,
                posterUrl = image,
                year = year
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        //Log.i(DEV, "Result => ${body}")

        // Video details
        val content = document.selectFirst("article#post")?.select("div.video-player")
        //Log.i(DEV, "Result => (content) $content")
        val title = content?.select("meta[itemprop=\"name\"]")?.attr("content") ?: ""
        val descript =content?.select("meta[itemprop=\"description\"]")?.attr("content")
        //Log.i(DEV, "Result => (descript) $descript")
        val year = null

        // Poster Image
        val poster = content?.select("meta[itemprop=\"thumbnailUrl\"]")?.attr("content")
        //Log.i(DEV, "Result => (poster) $poster")

        //TODO: Fetch links
        //Video stream
        val streamUrl: String =  try {
            val strPost = "post(\"https://javtube.watch/hash-javtubewatch\""
            val scripts = document.select("script").toString()
            val idxA = scripts.indexOf(strPost)
            val firstParse = scripts.substring(idxA)
            val idxB = firstParse.indexOf("function")

            val secondParse = firstParse.substring(strPost.length, idxB).trim().trim(',')
                .replace("num:", "\"num\":")
                .replace(":'", ":\"")
                .replace("'}", "\"}")
                .trim().trim(',')
                .trimEnd('}')
            "$secondParse,\"url\":\"${url}\"}"
        } catch (e: Exception) {
            Log.i(DEV, "Result => Exception (load) $e")
            ""
        }
        Log.i(DEV, "streamUrl => $streamUrl")
        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = globaltvType,
            dataUrl = streamUrl,
            posterUrl = poster,
            year = year,
            plot = descript,
        )
    }

    //TODO: LoadLinks
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        //NNN
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = "${this.name} VIP HD",
                url = "https://biblescreen.faithlifecdn.com/biblescreen/bibleScreen/playlist.m3u8",//"https://files.catbox.moe/9czzyk.mp4",
                referer = data,
                quality = Qualities.P2160.value,
                isM3u8 = true
            )
        )
        AppUtils.tryParseJson<JsonRequest?>(data)?.let { reqdata ->
            Log.i(DEV, "Referer => ${reqdata.url}")
            app.post(
                url = "$mainUrl/hash-javtubewatch",
                referer = reqdata.url,
                data = mapOf(
                    Pair("migboob", reqdata.migboob),
                    Pair("mix", reqdata.mix),
                    Pair("num", reqdata.num),
                ),
                headers = mapOf(
                    Pair("Origin", mainUrl),
                    Pair("Sec-Fetch-Mode", "cors"),
                    Pair("User-Agent", USER_AGENT),
                )
            ).let { postreq ->
                Log.i(DEV, "Post => (${postreq.code}) ${postreq.text}")

                val doc = Jsoup.parse(postreq.text)
                val src = doc.selectFirst("iframe")?.attr("src") ?: ""
                Log.i(DEV, "Post Url => $src")

                val id = src.trimEnd('/').split("/").last()
                val newUrl = "https://fembed-hd.com/api/source/${id}"
                Log.i(DEV, "newUrl => $newUrl")
                loadExtractor(
                    url = newUrl,
                    referer = reqdata.url,
                    callback = callback,
                    subtitleCallback = subtitleCallback
                )
                //TODO: Fix headers, returning 403 Forbidden
                /*val headers = mapOf(
                    Pair("Host", "javjav.top"),
                    Pair("Origin", src),
                    Pair("Referer", reqdata.url),
                    Pair("User-Agent", USER_AGENT),
                )
                Log.i(DEV, "headers => ${headers.toJson()}")
                val postlink = app.post(
                    url = newUrl,
                    headers = mapOf(
                        Pair("Host", "javjav.top"),
                        Pair("Origin", src),
                        Pair("Referer", reqdata.url),
                        Pair("User-Agent", USER_AGENT),
                    )
                )
                Log.i(DEV, "Post Link => (${postlink.code}) ${postlink.text}")

                val streamLinks = AppUtils.tryParseJson<JsonResponse?>(postlink.text)?.data ?: listOf()
                streamLinks.forEach{ stream ->
                    callback.invoke(
                        ExtractorLink(
                            source = "JavTube",
                            name = name,
                            url = stream.file,
                            referer = reqdata.url,
                            quality = getQualityFromName(stream.label)
                        )
                    )
                }*/
            }
        }

        return true
    }

    private data class JsonRequest(
        @JsonProperty("migboob") val migboob: String,
        @JsonProperty("mix") val mix: String,
        @JsonProperty("num") val num: String,
        @JsonProperty("url") val url: String
    )

    private data class JsonResponse(
        @JsonProperty("success") val success: Boolean,
        @JsonProperty("data") val data: List<JsonResponseData>?
    )
    private data class JsonResponseData(
        @JsonProperty("file") val file: String,
        @JsonProperty("label") val label: String,
        //val type: String // Mp4
    )
}