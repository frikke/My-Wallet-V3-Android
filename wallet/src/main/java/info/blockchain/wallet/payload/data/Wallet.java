package info.blockchain.wallet.payload.data;

import com.blockchain.serialization.JsonSerializableAccount;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.NoSuchAddressException;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.payload.HDWalletsContainer;
import info.blockchain.wallet.payload.data.walletdto.WalletDto;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FormatsUtil;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.params.MainNetParams;
import org.spongycastle.crypto.InvalidCipherTextException;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Wallet {

    private final WalletDto walletDto;
    private final int wrapperVersion;
    /**
     * We need to keep a reference of those as they persist the HDWallets container
     */
    private final List<WalletBody> walletBodies;

    /**
     * from Json after loading the payload
     *
     * @param walletDto
     * @param wrapperVersion
     */
    private Wallet(WalletDto walletDto, int wrapperVersion) {
        this.walletDto      = walletDto;
        this.wrapperVersion = wrapperVersion;
        if (walletDto.getWalletBodies() != null) {
            walletBodies = walletDto.getWalletBodies().stream()
                .map(walletBodyDto -> new WalletBody(wrapperVersion, walletBodyDto, new HDWalletsContainer()))
                .collect(Collectors.toList());
        }
        else { walletBodies = Collections.emptyList(); }
    }

    private Wallet(WalletDto walletDto, List<WalletBody> walletBodies, int wrapperVersion) {
        this.walletDto      = walletDto;
        this.wrapperVersion = wrapperVersion;
        this.walletBodies   = walletBodies;
    }

    /**
     * Creating a new wallet
     */
    public Wallet(String defaultAccountName) throws Exception {
        this(WalletBody.Companion.create(defaultAccountName, true));
    }

    /**
     * Recover from mnemonic
     *
     * @param walletBody
     */
    public Wallet(WalletBody walletBody) {
        walletDto      = new WalletDto(Collections.singletonList(walletBody.getWalletBodyDto()));
        walletBodies   = Collections.singletonList(walletBody);
        wrapperVersion = WalletWrapper.V4;
    }

    public String getGuid() {
        return walletDto.getGuid();
    }

    public String getSharedKey() {
        return walletDto.getSharedKey();
    }

    public boolean isDoubleEncryption() {
        return walletDto.getDoubledEncrypted();
    }

    public String getDpasswordhash() {
        return walletDto.getDpasswordhash();
    }


    public Map<String, String> getTxNotes() {
        return walletDto.getTxNotes();
    }

    public Options getOptions() {
        return walletDto.getOptions();
    }

    @Nullable
    public List<WalletBody> getWalletBodies() {
        return walletBodies;
    }

    @Nullable
    public WalletBody getWalletBody() {
        if (walletBodies.isEmpty()) {
            return null;
        }
        else {
            return walletBodies.get(WalletBody.HD_DEFAULT_WALLET_INDEX);
        }
    }

    /**
     * TODO this is a mutable list when it come to kotlin. make it immutable by converting the file to kotlin
     */
    public List<ImportedAddress> getImportedAddressList() {
        return walletDto.getImported();
    }

    public int getWrapperVersion() {
        return wrapperVersion;
    }

    public Wallet updateDefaultIndex(int index) {
        if (getWalletBody() == null) { return this; }
        WalletBody walletBody = getWalletBody().updateDefaultindex(index);
        return withUpdatedBodiesAndVersion(Collections.singletonList(walletBody), getWrapperVersion());
    }

    public Wallet withUpdatedDerivationsForAccounts(List<Account> accounts) {
        if (getWalletBody() == null) { return this; }
        WalletBody walletBody = getWalletBody().updateDerivationsForAccounts(accounts);
        return withUpdatedBodiesAndVersion(Collections.singletonList(walletBody), getWrapperVersion());
    }

    public Wallet withUpdatedDefaultDerivationTypeForAccounts(List<Account> accounts) {
        if (getWalletBody() == null) { return this; }
        WalletBody walletBody = getWalletBody().updateDefaultDerivationTypeForAccounts(accounts);
        return withUpdatedBodiesAndVersion(Collections.singletonList(walletBody), getWrapperVersion());
    }

    public Wallet updateAccountLabel(Account account, String label) {
        if (getWalletBody() == null) { return this; }
        WalletBody walletBody = getWalletBody().updateAccountLabel(account, label);
        return withUpdatedBodiesAndVersion(Collections.singletonList(walletBody), getWrapperVersion());
    }

    public boolean isUpgradedToV3() {
        return (walletDto.getWalletBodies() != null && walletDto.getWalletBodies().size() > 0);
    }


    public static Wallet fromJson(String json, int wrapperVersion)
        throws IOException, HDWalletException {
        WalletDto walletDto = WalletDto.fromJson(json);

        return new Wallet(walletDto, wrapperVersion);
    }

    public String toJson() {
        return this.walletDto.toJson();
    }


    /**
     * Checks imported address and hd keys for possible double encryption corruption
     */
    public boolean isEncryptionConsistent() {
        ArrayList<String> keyList = new ArrayList<>();

        if (getImportedAddressList() != null) {
            List<ImportedAddress> importedAddresses = getImportedAddressList();
            for (ImportedAddress importedAddress : importedAddresses) {
                String privateKey = importedAddress.getPrivateKey();
                // Filter watch-only addresses, which still exist in some wallets
                if (privateKey != null) {
                    keyList.add(privateKey);
                }
            }
        }

        if (getWalletBodies() != null && getWalletBodies().size() > 0) {
            for (WalletBody walletBody : getWalletBodies()) {
                List<Account> accounts = walletBody.getAccounts();
                for (Account account : accounts) {
                    keyList.add(account.getXpriv());
                }
            }
        }

        return isEncryptionConsistent(isDoubleEncryption(), keyList);
    }

    boolean isEncryptionConsistent(boolean isDoubleEncrypted, List<String> keyList) {
        boolean consistent = true;
        for (String key : keyList) {
            if (isDoubleEncrypted) {
                consistent = FormatsUtil.isKeyEncrypted(key);
            }
            else {
                consistent = FormatsUtil.isKeyUnencrypted(key);
            }

            if (!consistent) {
                break;
            }
        }
        return consistent;
    }

    public void validateSecondPassword(@Nullable String secondPassword) throws DecryptionException {

        if (isDoubleEncryption()) {
            DoubleEncryptionFactory.validateSecondPassword(
                getDpasswordhash(),
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );
        }
        else if (!isDoubleEncryption() && secondPassword != null) {
            throw new DecryptionException("Double encryption password specified on non double encrypted wallet.");
        }
    }

    /**
     * Upgrades a wallet from v2 to v3 and returns the Upgraded wallet.
     *
     * @param secondPassword
     * @param defaultAccountName
     * @return
     * @throws Exception
     */
    public Wallet upgradeV2PayloadToV3(@Nullable String secondPassword, String defaultAccountName) throws Exception {

        //Check if payload has 2nd password
        validateSecondPassword(secondPassword);

        if (!isUpgradedToV3()) {

            //Create new V3 hd wallet
            WalletBody walletBody = WalletBody.Companion.create(defaultAccountName, false);

            //Double encrypt if need
            if (!StringUtils.isEmpty(secondPassword)) {

                //Double encrypt seedHex
                String doubleEncryptedSeedHex = DoubleEncryptionFactory.encrypt(
                    walletBody.getSeedHex(),
                    getSharedKey(),
                    secondPassword,
                    getOptions().getPbkdf2Iterations()
                );
                walletBody = walletBody.updateSeedHex(doubleEncryptedSeedHex);

                //Double encrypt private keys
                for (Account account : walletBody.getAccounts()) {
                    String encryptedXPriv = DoubleEncryptionFactory.encrypt(
                        account.getXpriv(),
                        getSharedKey(),
                        secondPassword,
                        getOptions().getPbkdf2Iterations()
                    );

                    walletBody = walletBody.replaceAccount(
                        account,
                        account.withEncryptedPrivateKey(encryptedXPriv)
                    );
                }
            }
            return new Wallet(walletDto.addWalletBody(walletBody.getWalletBodyDto()), 3);
        }
        else { return this; }
    }

    public ImportedAddress importedAddressFromKey(SigningKey key,
                                                  @Nullable String secondPassword,
                                                  String device,
                                                  String apiCode)
        throws Exception {

        ImportedAddress address = ImportedAddress.Companion.fromECKey(key.toECKey(), device, apiCode);
        validateSecondPassword(secondPassword);

        if (secondPassword != null) {
            //Double encryption
            String unencryptedKey = address.getPrivateKey();

            String encryptedKey = DoubleEncryptionFactory.encrypt(
                unencryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );

            address = address.withUpdatePrivateKey(encryptedKey);

        }
        return address;
    }

    public Wallet addImportedAddress(ImportedAddress address) {
        return new Wallet(
            walletDto.addImportedAddress(address),
            walletBodies,
            wrapperVersion
        );
    }

    public void decryptHDWallet(String secondPassword)
        throws DecryptionException,
        IOException,
        InvalidCipherTextException,
        HDWalletException {

        validateSecondPassword(secondPassword);

        assert getWalletBodies() != null;
        WalletBody walletBody = getWalletBodies().get(WalletBody.HD_DEFAULT_WALLET_INDEX);
        walletBody.decryptHDWallet(secondPassword, walletDto.getSharedKey(), getOptions().getPbkdf2Iterations());
    }

    /**
     * This method adds always a v4 account
     *
     * @param label
     * @param secondPassword
     * @return
     * @throws Exception
     */
    public Wallet addAccount(
        String label,
        @Nullable String secondPassword
    ) throws Exception {

        validateSecondPassword(secondPassword);

        //Double decryption if need
        decryptHDWallet(secondPassword);

        WalletBody walletBody = walletBodies.get(WalletBody.HD_DEFAULT_WALLET_INDEX).withNewAccount(
            label,
            secondPassword,
            getSharedKey(),
            getOptions().getPbkdf2Iterations()
        );

        return new Wallet(
            walletDto.replaceWalletBody(walletBodies.get(WalletBody.HD_DEFAULT_WALLET_INDEX).getWalletBodyDto(), walletBody.getWalletBodyDto()),
            walletBodies.stream().map(item ->
                                      {
                                          if (item.equals(walletBodies.get(WalletBody.HD_DEFAULT_WALLET_INDEX))) {
                                              return walletBody;
                                          }
                                          else {
                                              return item;
                                          }
                                      }
            ).collect(Collectors.toList()),
            wrapperVersion
        );
    }

    public Wallet updateAccount(Account oldAccount, Account newAccount) {
        if (walletDto.getWalletBodies() == null) { return this; }

        List<WalletBodyDto> walletBodyDtos = walletDto.getWalletBodies().stream()
            .filter(dto -> dto.getAccounts().contains(oldAccount)).collect(Collectors.toList());

        if (walletBodyDtos.size() == 0) { return this; }

        List<Account> accounts = walletBodyDtos.get(WalletBody.HD_DEFAULT_WALLET_INDEX).getAccounts();
        accounts.set(
            accounts.indexOf(oldAccount), newAccount
        );
        return new Wallet(
            walletDto.replaceWalletBody(
                walletBodyDtos.get(WalletBody.HD_DEFAULT_WALLET_INDEX),
                walletBodyDtos.get(WalletBody.HD_DEFAULT_WALLET_INDEX).withUpdatedAccounts(accounts)
            ),
            walletBodies.stream().map(item -> item.replaceAccount(oldAccount, newAccount)).collect(Collectors.toList()),
            wrapperVersion
        );

    }

    /**
     * Updates the private key of an Imported address if found and  returns updated Wallet payload.
     *
     * @param key
     * @param secondPassword
     * @return
     * @throws DecryptionException
     * @throws UnsupportedEncodingException
     * @throws EncryptionException
     * @throws NoSuchAddressException
     */

    public Wallet updateKeyForImportedAddress(
        SigningKey key,
        @Nullable String secondPassword
    ) throws DecryptionException,
        UnsupportedEncodingException,
        EncryptionException,
        NoSuchAddressException {

        ECKey ecKey = key.toECKey();
        validateSecondPassword(secondPassword);

        List<ImportedAddress> addressList = getImportedAddressList();

        String address = LegacyAddress.fromKey(
            MainNetParams.get(),
            ecKey
        ).toString();

        ImportedAddress matchingAddressBody = null;

        for (ImportedAddress addressBody : addressList) {
            if (addressBody.getAddress().equals(address)) {
                matchingAddressBody = addressBody;
            }
        }

        if (matchingAddressBody == null) {
            throw new NoSuchAddressException("No matching address found for key");
        }

        ImportedAddress updatedAddress;
        String encryptedKey = Base58.encode(ecKey.getPrivKeyBytes());
        if (secondPassword != null) {
            //Double encryption

            String doubleEncryptedKey = DoubleEncryptionFactory.encrypt(
                encryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );

            updatedAddress = matchingAddressBody.withUpdatePrivateKey(doubleEncryptedKey);
        }
        else {
            updatedAddress = matchingAddressBody.withUpdatePrivateKey(encryptedKey);
        }
        return new Wallet(
            walletDto.replaceImportedAddress(matchingAddressBody, updatedAddress),
            walletBodies,
            wrapperVersion
        );
    }

    /**
     * @deprecated Use the kotlin extension: {@link WalletExtensionsKt#nonArchivedImportedAddressStrings}
     */
    @Deprecated
    public List<String> getImportedAddressStringList() {

        List<String> addrs = new ArrayList<>(walletDto.getImported().size());
        for (ImportedAddress importedAddress : walletDto.getImported()) {
            if (!importedAddress.isArchived()) {
                addrs.add(importedAddress.getAddress());
            }
        }

        return addrs;
    }


    public boolean containsImportedAddress(String addr) {
        for (ImportedAddress importedAddress : walletDto.getImported()) {
            if (importedAddress.getAddress().equals(addr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns label if match found, otherwise just returns address.
     *
     * @param address
     */
    public String getLabelFromImportedAddress(String address) {

        List<ImportedAddress> addresses = getImportedAddressList();

        for (ImportedAddress importedAddress : addresses) {
            if (importedAddress.getAddress().equals(address)) {
                String label = importedAddress.getLabel();
                if (label.isEmpty()) {
                    return address;
                }
                else {
                    return label;
                }
            }
        }

        return address;
    }


    public Wallet withUpdatedBodiesAndVersion(List<WalletBody> walletBodies, int version) {
        return new Wallet(
            walletDto.replaceWalletBodies(
                walletBodies.stream()
                    .map(WalletBody::getWalletBodyDto).collect(Collectors.toList())),
            walletBodies,
            version
        );
    }

    public Wallet updateArchivedState(JsonSerializableAccount account, boolean isArchived) {
        if (getWalletBody() == null) { return this; }
        if (account instanceof ImportedAddress) {
            return new Wallet(
                walletDto.updateArchivedStateOfImportedAddr((ImportedAddress) account, isArchived),
                walletBodies,
                wrapperVersion
            );
        }
        else if (account instanceof Account) {
            WalletBody walletBody = getWalletBody().updateAccountState((Account) account, isArchived);
            return withUpdatedBodiesAndVersion(Collections.singletonList(walletBody), getWrapperVersion());
        }
        throw new IllegalStateException("Unknown to payload account type " + account);
    }

    public Wallet updateMnemonicVerifiedState(boolean verified) {
        return withUpdatedBodiesAndVersion(
            Collections.singletonList(walletBodies.get(WalletBody.HD_DEFAULT_WALLET_INDEX).updateMnemonicState(verified)),
            wrapperVersion
        );
    }

    public Wallet updatePbkdf2Iterations(int iterations) {
        return new Wallet(
            walletDto.withPbkdf2Iterations(iterations),
            walletBodies,
            wrapperVersion
        );
    }

    public Wallet updateTxNotes(String transactionHash, String notes) {
        return new Wallet(
            walletDto.withUpdatedNotes(transactionHash, notes),
            walletBodies,
            wrapperVersion
        );
    }
}