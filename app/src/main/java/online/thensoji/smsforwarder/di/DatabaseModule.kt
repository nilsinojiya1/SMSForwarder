package online.thensoji.smsforwarder.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import online.thensoji.smsforwarder.data.AppDatabase
import online.thensoji.smsforwarder.data.ForwardedMessageDao
import online.thensoji.smsforwarder.data.SmsPartDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "sms_forwarder_db")
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideForwardedMessageDao(db: AppDatabase): ForwardedMessageDao = db.forwardedMessageDao()

    @Provides
    fun provideSmsPartDao(db: AppDatabase): SmsPartDao = db.smsPartDao()
}
