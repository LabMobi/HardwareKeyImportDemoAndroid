package mobi.lab.keyimportdemo.infrastructure.di

import dagger.Binds
import dagger.Module
import mobi.lab.keyimportdemo.domain.gateway.AuthGateway
import mobi.lab.keyimportdemo.domain.gateway.CryptoClientGateway
import mobi.lab.keyimportdemo.domain.gateway.CryptoServerGateway
import mobi.lab.keyimportdemo.domain.gateway.LoggerGateway
import mobi.lab.keyimportdemo.infrastructure.auth.AuthProvider
import mobi.lab.keyimportdemo.infrastructure.common.logger.Logger
import mobi.lab.keyimportdemo.infrastructure.crypto.CryptoClient
import mobi.lab.keyimportdemo.infrastructure.crypto.CryptoServer

@Module(includes = [GatewayModule.Definitions::class])
object GatewayModule {

    @Module
    internal interface Definitions {
        @Binds fun bindAuthGateway(impl: AuthProvider): AuthGateway
        @Binds fun bindLoggerGateway(impl: Logger): LoggerGateway
        @Binds fun bindCryptoServerGateway(impl: CryptoServer): CryptoServerGateway
        @Binds fun bindCryptoClientGateway(impl: CryptoClient): CryptoClientGateway
    }
}
