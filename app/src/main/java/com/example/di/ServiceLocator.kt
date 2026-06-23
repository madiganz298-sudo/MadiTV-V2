package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.IptvRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ServiceLocator {
    @Volatile
    private var database: AppDatabase? = null
    
    @Volatile
    private var repository: IptvRepository? = null

    @Volatile
    private var okHttpClient: OkHttpClient? = null

    fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "m4ditv_database"
            ).build().also { database = it }
        }
    }

    fun getOkHttpClient(): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build().also { okHttpClient = it }
        }
    }

    fun getIptvRepository(context: Context): IptvRepository {
        return repository ?: synchronized(this) {
            repository ?: IptvRepository(
                getDatabase(context),
                getOkHttpClient()
            ).also { repository = it }
        }
    }
}
