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
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.json.JSONObject
import org.jsoup.nodes.Element

class Pornhits : MainAPI() {
    override var mainUrl = "https://www.pornhits.com"
    override var name = "Pornhits"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl/videos.php?p=%d&s=l" to "Latest",
        "$mainUrl/videos.php?p=%d&s=pd" to "Popular last day",
        "$mainUrl/videos.php?p=%d&s=bd" to "Top Rated (day)",
        "$mainUrl/videos.php?p=%d&s=pw" to "Popular last week",
        "$mainUrl/videos.php?p=%d&s=bw" to "Top Rated (week)",
        "$mainUrl/videos.php?p=%d&s=pm" to "Popular last month",
        "$mainUrl/videos.php?p=%d&s=bm" to "Top Rated (month)",
    )

    override suspend fun getMainPage(
        page: Int, request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page)).document
        val home =
            document.select("div.main-content section.main-container div.list-videos article.item")
                .mapNotNull {
                    it.toSearchResult()
                }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name, list = home, isHorizontalImages = true
            ), hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.item-info h2.title")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("a div.img img").attr("data-original"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..15) {
            val document = app.get(
                "$mainUrl/videos.php?p=${i}&q=${query.trim().replace(" ", "+")}"
            ).document
            val results =
                document.select("div.main-content section.main-container div.list-videos article.item")
                    .mapNotNull {
                        it.toSearchResult()
                    }
            searchResponse.addAll(results)
            if (results.isEmpty()) break
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title =
            document.selectFirst("section.video-holder div.video-info div.info-holder article#tab_video_info.tab-content div.headline h1")
                ?.text()
                ?: ""
        val poster = fixUrlNull(
            document.selectXpath("//script[contains(text(),'var schemaJson')]").first()?.data()
                ?.replace("\"", "")
                ?.substringAfter("thumbnailUrl:")
                ?.substringBefore(",uploadDate:")
                ?.trim() ?: ""
        )
        val tags =
            document.select(" section.video-holder div.video-info div.info-holder article#tab_video_info.tab-content div.block-details div.info h3.item a")
                .map { it.text() }
        val recommendations =
            document.select("div.related-videos div.list-videos article.item")
                .mapNotNull {
                    it.toSearchResult()
                }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val script =
            document.selectXpath("//script[contains(text(),'let vpage_data')]").first()?.html()
        var isVHQ = false
        if (script != null && script.contains("VHQ")) {
            isVHQ = true
        }
        val pattern = Regex("""window\.initPlayer\((.*])\);""")
        val matchResult = pattern.find(script ?: "")

        val jsonArray = matchResult?.groups?.get(1)?.value

        val encodedString = getEncodedString(jsonArray) ?: ""

        val decodedString = customBase64Decoder(encodedString)

        val videos = JSONObject("{ videos:$decodedString}").getJSONArray("videos")
        val externalLinkList = mutableListOf<ExtractorLink>()
        for (i in 0 until videos.length()) {
            val video = videos.getJSONObject(i)
            var quality = Qualities.Unknown.value
            var isM3u8 = false
            if (video.getString("format").contains("lq")) {
                quality = Qualities.P480.value
            }
            if (video.getString("format").contains("hq")) {
                quality = Qualities.P720.value
            }
            var url = customBase64Decoder(video.getString("video_url"))
            if (isVHQ) {
                url = "$url&f=video.m3u8"
                isM3u8 = true
                quality = Qualities.Unknown.value
            }
            externalLinkList.add(
                ExtractorLink(
                    this.name,
                    this.name,
                    fixUrl(url),
                    referer = mainUrl,
                    quality = quality,
                    isM3u8 = isM3u8
                )
            )
            if (isVHQ) break
        }

        externalLinkList.forEach(callback)
        return true
    }

    private fun customBase64Decoder(encodedString: String): String {
        val base64CharacterSet = "АВСDЕFGHIJKLМNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,~"
        var decodedString = ""
        var currentIndex = 0

        Regex("[^АВСЕМA-Za-z0-9.,~]").find(encodedString)?.let {
            println("Error decoding URL")
        }

        val sanitizedString = encodedString.replace("[^АВСЕМA-Za-z0-9.,~]".toRegex(), "")

        do {
            val firstCharIndex = base64CharacterSet.indexOf(sanitizedString[currentIndex++])
            val secondCharIndex = base64CharacterSet.indexOf(sanitizedString[currentIndex++])
            val thirdCharIndex = base64CharacterSet.indexOf(sanitizedString[currentIndex++])
            val fourthCharIndex = base64CharacterSet.indexOf(sanitizedString[currentIndex++])

            val reconstructedFirstChar = (firstCharIndex shl 2) or (secondCharIndex shr 4)
            val reconstructedSecondChar = ((15 and secondCharIndex) shl 4) or (thirdCharIndex shr 2)
            val lastPart = ((3 and thirdCharIndex) shl 6) or fourthCharIndex

            decodedString += reconstructedFirstChar.toChar().toString()
            if (64 != thirdCharIndex) {
                decodedString += reconstructedSecondChar.toChar().toString()
            }
            if (64 != fourthCharIndex) {
                decodedString += lastPart.toChar().toString()
            }
        } while (currentIndex < sanitizedString.length)
        return java.net.URLDecoder.decode(decodedString, "UTF-8")
    }

    private fun getEncodedString(json: String?): String? {
        val stringPattern = Regex("""'([^']+)',""")

        val stringMatch = stringPattern.find(json ?: "")

        return when {
            stringMatch != null -> stringMatch.groups[1]?.value
            else -> null
        }
    }

}