package com.harmonixia.android.di

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.memory.MemoryCache
import coil3.network.CacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.Options
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.harmonixia.android.data.local.AuthTokenProvider
import com.harmonixia.android.ui.util.AudioFileAlbumArtFetcher
import com.harmonixia.android.util.ImageQualityManager
import com.harmonixia.android.util.BitmapPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageQualityManager(
        @ApplicationContext context: Context
    ): ImageQualityManager {
        return ImageQualityManager(context)
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        imageQualityManager: ImageQualityManager,
        okHttpClient: OkHttpClient,
        authTokenProvider: AuthTokenProvider
    ): ImageLoader {
        val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()
        val isExpandedDevice = context.resources.configuration.screenWidthDp >= 840
        val memoryCachePercent = if (isExpandedDevice) 0.30 else 0.25
        val authenticatedClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val token = authTokenProvider.getAuthToken()
                val authenticatedRequest = if (
                    token.isNotBlank() && request.header("Authorization").isNullOrBlank()
                ) {
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
                chain.proceed(authenticatedRequest)
            }
            .build()

        return ImageLoader.Builder(context)
            .components {
                add(AudioFileAlbumArtFetcher.Factory())
                add(SvgDecoder.Factory())
                add(
                    DedupingNetworkFetcherFactory(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { authenticatedClient },
                            cacheStrategy = { CacheStrategy.DEFAULT }
                        )
                    )
                )
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryCachePercent)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .bitmapPool {
                BitmapPool.Builder()
                    .maxSizePercent(context, 0.5)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(IMAGE_DISK_CACHE_SIZE_BYTES)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .allowRgb565(bitmapConfig == Bitmap.Config.RGB_565)
            .bitmapConfig(bitmapConfig)
            .placeholder { ColorDrawable(PLACEHOLDER_COLOR).asImage() }
            .error { ColorDrawable(ERROR_COLOR).asImage() }
            .crossfade(150)
            .build()
    }

    private const val IMAGE_DISK_CACHE_SIZE_BYTES = 500L * 1024 * 1024
    private const val PLACEHOLDER_COLOR = 0xFFE0E0E0.toInt()
    private const val ERROR_COLOR = 0xFFFFCDD2.toInt()
}

private fun ImageLoader.Builder.bitmapPool(initializer: () -> BitmapPool) = apply {
    extras[BitmapPool.EXTRAS_KEY] = initializer()
}

private class DedupingNetworkFetcherFactory(
    private val delegate: Fetcher.Factory<Uri>
) : Fetcher.Factory<Uri> {
    override fun create(
        data: Uri,
        options: Options,
        imageLoader: ImageLoader
    ): Fetcher? {
        val fetcher = delegate.create(data, options, imageLoader) ?: return null
        val cacheKey = options.diskCacheKey ?: data.toString()
        return DedupingFetcher(cacheKey, fetcher)
    }
}

private class DedupingFetcher(
    private val cacheKey: String,
    private val delegate: Fetcher
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return NetworkRequestDeduper.withLock(cacheKey) { delegate.fetch() }
    }
}

private object NetworkRequestDeduper {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val mutex = locks.getOrPut(key) { Mutex() }
        mutex.lock()
        return try {
            block()
        } finally {
            mutex.unlock()
            if (!mutex.isLocked) {
                locks.remove(key, mutex)
            }
        }
    }
}
