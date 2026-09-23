package com.fonamp.provider.api

/** Empty query = index request; any field set = station-list request (design §2). */
data class BrowseQuery(
    val country: String? = null,
    val genre: String? = null,
    val tag: String? = null,
    val feedUrl: String? = null,
)
