package online.thensoji.smsforwarder.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import online.thensoji.smsforwarder.domain.repository.TelegramRepository
import online.thensoji.smsforwarder.repository.TelegramRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTelegramRepository(
        impl: TelegramRepositoryImpl
    ): TelegramRepository
}

