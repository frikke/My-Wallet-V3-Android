package piuk.blockchain.androidcore.data.payload

import com.blockchain.wallet.Seed
import com.blockchain.wallet.SeedAccessWithoutPrompt
import info.blockchain.wallet.exceptions.HDWalletException
import io.reactivex.rxjava3.core.Maybe

internal class PayloadDataManagerSeedAccessAdapter(
    private val payloadDataManager: PayloadDataManager
) : SeedAccessWithoutPrompt {

    override fun seed(validatedSecondPassword: String?): Maybe<Seed> {
        return Maybe.concat(
            seed,
            Maybe.defer { getSeedGivenPassword(validatedSecondPassword) }
        ).firstElement()
    }

    override val seed: Maybe<Seed>
        get() {
            return getSeedWithoutPassword()
        }

    private fun getSeedWithoutPassword(): Maybe<Seed> =
        getSeedGivenPassword(null)

    private fun getSeedGivenPassword(validatedSecondPassword: String?): Maybe<Seed> {
        return try {
            validatedSecondPassword?.let {
                payloadDataManager.decryptHDWallet(
                    validatedSecondPassword
                )
            }
            val hdWallet = payloadDataManager.wallet?.walletBody
            hdWallet?.getHdSeed()?.let { hdSeed ->
                Maybe.just(Seed(hdSeed = hdSeed))
            } ?: Maybe.empty()
        } catch (hd: HDWalletException) {
            Maybe.empty()
        }
    }
}
