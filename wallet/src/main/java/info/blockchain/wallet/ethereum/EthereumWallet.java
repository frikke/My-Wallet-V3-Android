package info.blockchain.wallet.ethereum;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import info.blockchain.balance.AssetCatalogue;
import info.blockchain.balance.AssetInfo;
import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.keys.MasterKey;

public class EthereumWallet {

    public static final int METADATA_TYPE_EXTERNAL = 5;
    private static final int ACCOUNT_INDEX = 0;

    private final EthereumWalletDto walletDto;

    public EthereumWallet(EthereumWalletDto walletDto) {
        this.walletDto = walletDto;
    }

    /**
     * Creates new Ethereum wallet and derives account from provided wallet seed.
     *
     * @param walletMasterKey DeterministicKey of root node
     * @param label the default label for non custodial assets
     */
    public EthereumWallet(
        MasterKey walletMasterKey,
        String label
    ) {
        ArrayList<EthereumAccount> accounts = new ArrayList<>();
        accounts.add(
            EthereumAccount.Companion.deriveAccount(
                walletMasterKey.toDeterministicKey(),
                ACCOUNT_INDEX,
                label
            )
        );

        walletDto = new EthereumWalletDto(accounts);
    }

    public String toJson(boolean withKotlinX) throws JsonProcessingException {
        return walletDto.toJson(withKotlinX);
    }

    /**
     * Loads existing Ethereum wallet from derived Ethereum metadata node.
     *
     * @return Existing Ethereum wallet or Null if no existing Ethereum wallet found.
     */
    public static EthereumWallet load(String walletJson, boolean withKotlinX) throws IOException {

        if (walletJson != null) {
            EthereumWalletDto wallet = EthereumWalletDto.fromJson(walletJson, withKotlinX);

            // Web can store an empty EthereumWalletData object
            if (wallet.getWalletData() == null || wallet.getWalletData().getAccounts().isEmpty()) {
                return null;
            } else {
                return new EthereumWallet(wallet);
            }
        } else {
            return null;
        }
    }

    public boolean hasSeen() {
        return walletDto.getWalletData().getHasSeen();
    }

    /**
     * Set flag to indicate that user has acknowledged their ether wallet.
     */
    public void setHasSeen(boolean hasSeen) {
        walletDto.getWalletData().setHasSeen(hasSeen);
    }

    /**
     * @return Single Ethereum account
     */
    public EthereumAccount getAccount() {

        if (walletDto.getWalletData().getAccounts().isEmpty()) {
            return null;
        }

        return walletDto.getWalletData().getAccounts().get(ACCOUNT_INDEX);
    }

    public void renameAccount(String newLabel){
        EthereumAccount account = getAccount();
        account.setLabel(newLabel);
        ArrayList<EthereumAccount> accounts = new ArrayList<>();
        accounts.add(account);
        walletDto.getWalletData().setAccounts(accounts);
    }

    public HashMap<String, String> getTxNotes() {
        return walletDto.getWalletData().getTxNotes();
    }

    public void putTxNotes(String txHash, String txNote) {
        HashMap<String, String> notes = walletDto.getWalletData().getTxNotes();
        notes.put(txHash, txNote);
    }

    public void removeTxNotes(String txHash) {
        HashMap<String, String> notes = walletDto.getWalletData().getTxNotes();
        notes.remove(txHash);
    }

    @Deprecated // Eth payload last tx features are no longer used
    public void setLastTransactionHash(String txHash) {
        walletDto.getWalletData().setLastTx(txHash);
    }

    @Deprecated // Eth payload last tx features are no longer used
    public void setLastTransactionTimestamp(long timestamp) {
        walletDto.getWalletData().setLastTxTimestamp(timestamp);
    }

    @Nullable
    public Erc20TokenData getErc20TokenData(String tokenName) {
        return walletDto.getWalletData().getErc20Tokens().get(tokenName.toLowerCase());
    }

}
