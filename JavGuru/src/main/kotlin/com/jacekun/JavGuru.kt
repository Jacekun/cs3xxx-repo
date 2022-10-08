package com.jacekun

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.app
import org.jsoup.Jsoup

class JavGuru : MainAPI() {
    private val DEV = "DevDebug"
    private val globaltvType = TvType.Movie

    override var name = "Jav Guru"
    override var mainUrl = "https://jav.guru"
    override val supportedTypes = setOf(TvType.NSFW)
    override val hasDownloadSupport = false
    override val hasMainPage = true
    override val hasQuickSearch = false

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val html = app.get(mainUrl).text
        val document = Jsoup.parse(html)
        val all = ArrayList<HomePageList>()

        val mainbody = document.getElementsByTag("body").select("div#page")
            .select("div#content").select("div#primary")
            .select("main")

        //Log.i(DEV, "Main body => $mainbody")
        // Fetch row title
        val title = "Homepage"
        // Fetch list of items and map
        mainbody.select("div.row").let { inner ->
            val elements: List<SearchResponse> = inner.map {

                val innerArticle = it.select("div.column")
                    .select("div.inside-article").select("div.imgg")
                //Log.i(DEV, "Inner content => $innerArticle")
                val aa = innerArticle.select("a").firstOrNull()
                val link = fixUrl(aa?.attr("href") ?: "")

                val imgArticle = aa?.select("img")
                val name = imgArticle?.attr("alt") ?: "<No Title>"
                var image = imgArticle?.attr("src") ?: ""
                val year = null

                MovieSearchResponse(
                    name,
                    link,
                    this.name,
                    globaltvType,
                    image,
                    year,
                    null,
                )
            }

            all.add(
                HomePageList(
                    title, elements
                )
            )
        }
        return HomePageResponse(all)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query}"
        val html = app.get(url).text
        val document = Jsoup.parse(html).select("main.site-main").select("div.row")

        return document.map {
            val aa = it.select("div.column").select("div.inside-article")
                .select("div.imgg").select("a")
            val imgrow = aa.select("img")

            val href = fixUrl(aa.attr("href"))
            val title = imgrow.attr("alt")
            val image = imgrow.attr("src").trim('\'')
            val year = Regex("(?<=\\/)(.[0-9]{3})(?=\\/)")
                .find(image)?.groupValues?.get(1)?.toIntOrNull()

            MovieSearchResponse(
                title,
                href,
                this.name,
                globaltvType,
                image,
                year
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url).text
        val document = Jsoup.parse(response)

        //Log.i(DEV, "Url => ${url}")
        val body = document.getElementsByTag("body")
            .select("div#page")
            .select("div#content").select("div.content-area")
            .select("main").select("article")
            .select("div.inside-article").select("div.content")
            .select("div.posts")

        //Log.i(DEV, "Result => ${body}")
        val poster = body.select("div.large-screenshot").select("div.large-screenimg")
            .select("img").attr("src")
        val title = body.select("h1.titl").text()
        val descript = body.select("div.wp-content").select("p").firstOrNull()?.text()
        val streamUrl = ""
        val year = body.select("div.infometa > div.infoleft > ul > li")
            ?.get(1)?.text()?.takeLast(10)?.substring(0, 4)?.toIntOrNull()

        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = globaltvType,
            dataUrl = streamUrl,
            posterUrl = poster,
            year = year,
            plot = descript,
            comingSoon = true
        )
    }
}