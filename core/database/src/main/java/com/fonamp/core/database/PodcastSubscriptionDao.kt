package com.fonamp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastSubscriptionDao {
    @Query("SELECT * FROM podcast_subscriptions ORDER BY subscribedAt DESC")
    fun getAllSubscriptions(): Flow<List<PodcastSubscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun subscribe(subscription: PodcastSubscription)

    @Query("DELETE FROM podcast_subscriptions WHERE feedUrl = :feedUrl")
    suspend fun unsubscribe(feedUrl: String)

    @Query("SELECT EXISTS(SELECT 1 FROM podcast_subscriptions WHERE feedUrl = :feedUrl)")
    fun isSubscribed(feedUrl: String): Flow<Boolean>
}