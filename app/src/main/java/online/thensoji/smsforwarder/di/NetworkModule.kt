package online.thensoji.smsforwarder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import online.thensoji.smsforwarder.BuildConfig
import online.thensoji.smsforwarder.network.api.TelegramApiService
import online.thensoji.smsforwarder.network.datasource.TelegramRemoteDataSource
import online.thensoji.smsforwarder.network.datasource.TelegramRemoteDataSourceImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TELEGRAM_BASE_URL = "https://api.telegram.org/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool())
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TELEGRAM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramApiService(retrofit: Retrofit): TelegramApiService {
        return retrofit.create(TelegramApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTelegramRemoteDataSource(
        apiService: TelegramApiService
    ): TelegramRemoteDataSource {
        return TelegramRemoteDataSourceImpl(apiService)
    }
}
