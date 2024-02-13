package com.KillerDogeEmpire

import android.content.Context
import com.KillerDogeEmpire.SxyPrn
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SxyPrnProvider : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(SxyPrn())
    }
}