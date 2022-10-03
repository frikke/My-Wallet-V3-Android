package info.blockchain.wallet.ethereum

import info.blockchain.wallet.ethereum.EthereumAccount.Companion.deriveAccount
import info.blockchain.wallet.ethereum.EthereumWalletDto.Companion.fromJson
import info.blockchain.wallet.keys.MasterKey
import java.util.Locale

class EthereumWallet(private val walletDto: EthereumWalletDto) {

    constructor(walletMasterKey: MasterKey, label: String) : this(
        EthereumWalletDto(
            listOf(
                deriveAccount(
                    walletMasterKey.toDeterministicKey(),
                    ACCOUNT_INDEX,
                    label
                ).ethAccountDto
            )
        )
    )

    fun toJson(): String =
        walletDto.toJson()

    /**
     * @return Single Ethereum account
     */
    val account: EthereumAccount
        get() = EthereumAccount(walletDto.walletData.accounts[ACCOUNT_INDEX])
    val hasSeen: Boolean
        get() = walletDto.walletData.hasSeen

    fun renameAccount(newLabel: String): EthereumWallet {
        return EthereumWallet(walletDto.renameAccount(newLabel))
    }

    fun getTxNotes(): Map<String, String> {
        return walletDto.walletData.txNotes
    }

    fun getErc20TokenData(tokenName: String): Erc20TokenData? {
        return walletDto.walletData.erc20Tokens[tokenName.lowercase(Locale.getDefault())]
    }

    fun withUpdatedTxNotes(hash: String, note: String): EthereumWallet {
        return EthereumWallet(walletDto = walletDto.updateTxNotes(hash, note))
    }

    fun updateTxNoteForErc20(hash: String, note: String, erc20: Erc20TokenData): EthereumWallet {
        return EthereumWallet(walletDto = walletDto.updateTxNoteForErc20(hash, note, erc20))
    }

    fun withCheckSummedAccount(): EthereumWallet {
        return EthereumWallet(walletDto = walletDto.withCheckedSummedAccount())
    }

    companion object {
        private const val ACCOUNT_INDEX = 0
        fun load(walletJson: String?): EthereumWallet? =
            walletJson?.let {
                EthereumWallet(fromJson(it))
            }
    }
}
