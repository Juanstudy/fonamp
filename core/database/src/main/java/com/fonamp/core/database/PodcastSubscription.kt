package com.fonamp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcast_subscriptions")
data class PodcastSubscription(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val artworkUri: String?,
    val author: String?,
    val subscribedAt: Long = System.currentTimeMillis()
)