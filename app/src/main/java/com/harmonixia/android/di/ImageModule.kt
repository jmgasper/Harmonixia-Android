package com.harmonixia.android.di

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import coil3.ImageLoader
import coil3.asImage
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import com.harmonixia.android.data.local.AuthTokenProvider
import com.harmonixia.android.util.ImageQualityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        authTokenProvider: AuthTokenProvider
    ): ImageLoader {
        val imageQualityManager = ImageQualityManager(context)
        val bitmapConfig = imageQualityManager.getOptimalBitmapConfig()
        val isExpandedDevice = context.resources.configuration.screenWidthDp >= 840
        val memoryCachePercent = if (isExpandedDevice) 0.25 else 0.20
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
                add(OkHttpNetworkFetcherFactory(callFactory = { authenticatedClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryCachePercent)
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
            .crossfade(true)
            .build()
    }

    private const val IMAGE_DISK_CACHE_SIZE_BYTES = 300L * 1024 * 1024
    private const val PLACEHOLDER_COLOR = 0xFFE0E0E0.toInt()
    private const val ERROR_COLOR = 0xFFFFCDD2.toInt()
}
