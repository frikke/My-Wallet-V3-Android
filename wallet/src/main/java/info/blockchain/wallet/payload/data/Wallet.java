package info.blockchain.wallet.payload.data;

import com.google.common.annotations.VisibleForTesting;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.NoSuchAddressException;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.payload.data.walletdto.WalletDto;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FormatsUtil;
import kotlinx.serialization.modules.SerializersModule;

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


public class Wallet {
    private final WalletDto walletDto;
    private int wrapperVersion;

    private Wallet(WalletDto walletDto) {
        this.walletDto = walletDto;
    }

    public Wallet() {
        walletDto      = new WalletDto();
        wrapperVersion = WalletWrapper.V4;
    }

    public Wallet(String defaultAccountName) throws Exception {
        WalletBody walletBodyBody = new WalletBody(defaultAccountName, true);
        ArrayList<WalletBody> walletBodies = new ArrayList<>();
        walletBodies.add(walletBodyBody);
        walletDto      = new WalletDto(walletBodies);
        wrapperVersion = WalletWrapper.V4;
    }

    public String getGuid() {
        return walletDto.getGuid();
    }

    public String getSharedKey() {
        return walletDto.getSharedKey();
    }

    public boolean isDoubleEncryption() {
        return walletDto.isDoubleEncryption();
    }

    public String getDpasswordhash() {
        return walletDto.getDpasswordhash();
    }


    public Map<String, String> getTxNotes() {
        return walletDto.getTxNotes();
    }

    public Map<String, List<Integer>> getTxTags() {
        return walletDto.getTxTags();
    }

    public List<Map<Integer, String>> getTagNames() {
        return walletDto.getTagNames();
    }

    public Options getOptions() {
        fixPbkdf2Iterations();
        return walletDto.getOptions();
    }

    public Options getWalletOptions() {
        return walletDto.getWalletOptions();
    }

    @Deprecated
    @Nullable
    public List<WalletBody> getWalletBodies() {
        return walletDto.getWalletBodies();
    }

    @Nullable
    public WalletBody getWalletBody() {
        if (walletDto.getWalletBodies() == null || walletDto.getWalletBodies().isEmpty()) {
            return null;
        }
        else {
            return walletDto.getWalletBodies().get(HD_WALLET_INDEX);
        }
    }

    public List<ImportedAddress> getImportedAddressList() {
        return walletDto.getImported();
    }

    public List<AddressBook> getAddressBooks() {
        return walletDto.getAddressBook();
    }

    public int getWrapperVersion() {
        return wrapperVersion;
    }

    public void setGuid(String guid) {
        this.walletDto.setGuid(guid);
    }

    public void setSharedKey(String sharedKey) {
        this.walletDto.setSharedKey(sharedKey);
    }

    @Deprecated
    public void setWalletBodies(List<WalletBody> walletBodies) {
        this.walletDto
            .setWalletBodies(walletBodies);
    }

    public void setWalletBody(WalletBody walletBody) {
        this.walletDto.setWalletBodies(Collections.singletonList(walletBody));
    }

    public void setImportedAddressList(List<ImportedAddress> keys) {
        this.walletDto.setImported(keys);
    }

    public void setWrapperVersion(int wrapperVersion) {
        this.wrapperVersion = wrapperVersion;
    }

    public boolean isUpgradedToV3() {
        return (walletDto.getWalletBodies() != null && walletDto.getWalletBodies().size() > 0);
    }

    public static Wallet fromJson(String json)
        throws IOException, HDWalletException {
        SerializersModule serializersModule = WalletWrapper.getSerializerForVersion(WalletWrapper.V3);
        return fromJson(json, serializersModule);
    }

    public static Wallet fromJson(String json, SerializersModule serializersModule)
        throws IOException, HDWalletException {
        WalletDto walletDto = WalletDto.fromJson(json, serializersModule);

        if (walletDto.getWalletBodies() != null) {
            ArrayList<WalletBody> walletBodyList = new ArrayList<>();

            for (WalletBody walletBody : walletDto.getWalletBodies()) {
                walletBodyList.add(
                    WalletBody.Companion.fromJson(
                        walletBody.toJson(serializersModule),
                        serializersModule
                    )
                );
            }

            walletDto.setWalletBodies(walletBodyList);
        }

        return new Wallet(walletDto);
    }

    public String toJson(SerializersModule serializersModule) {
        return this.walletDto.toJson(serializersModule);
    }

    void addHDWallet(WalletBody walletBody) {
        if (walletDto.getWalletBodies() == null) {
            walletDto.setWalletBodies(new ArrayList<>());
        }
        walletDto.getWalletBodies().add(walletBody);
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

    public void upgradeV2PayloadToV3(@Nullable String secondPassword, String defaultAccountName) throws Exception {

        //Check if payload has 2nd password
        validateSecondPassword(secondPassword);

        if (!isUpgradedToV3()) {

            //Create new hd wallet
            WalletBody walletBodyBody = new WalletBody(defaultAccountName, false);
            walletBodyBody.setWrapperVersion(wrapperVersion);
            addHDWallet(walletBodyBody);

            //Double encrypt if need
            if (!StringUtils.isEmpty(secondPassword)) {

                //Double encrypt seedHex
                String doubleEncryptedSeedHex = DoubleEncryptionFactory.encrypt(
                    walletBodyBody.getSeedHex(),
                    getSharedKey(),
                    secondPassword,
                    getOptions().getPbkdf2Iterations()
                );
                walletBodyBody.setSeedHex(doubleEncryptedSeedHex);

                //Double encrypt private keys
                for (Account account : walletBodyBody.getAccounts()) {

                    String encryptedXPriv = DoubleEncryptionFactory.encrypt(
                        account.getXpriv(),
                        getSharedKey(),
                        secondPassword,
                        getOptions().getPbkdf2Iterations()
                    );

                    account.setXpriv(encryptedXPriv);

                }
            }

            setWrapperVersion(WalletWrapper.V3);
        }
    }

    @VisibleForTesting
    public ImportedAddress addImportedAddress(ImportedAddress address,
                                              @Nullable String secondPassword)
        throws Exception {

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

            address.setPrivateKey(encryptedKey);

        }
        walletDto.getImported().add(address);
        return address;
    }

    public ImportedAddress addImportedAddressFromKey(SigningKey key,
                                                     @Nullable String secondPassword,
                                                     String device,
                                                     String apiCode)
        throws Exception {
        return addImportedAddress(ImportedAddress.Companion.fromECKey(key.toECKey(), device, apiCode), secondPassword);
    }

    public void decryptHDWallet(String secondPassword)
        throws DecryptionException,
        IOException,
        InvalidCipherTextException,
        HDWalletException {

        validateSecondPassword(secondPassword);

        WalletBody walletBody = walletDto.getWalletBodies().get(HD_WALLET_INDEX);
        walletBody.decryptHDWallet(secondPassword, walletDto.getSharedKey(), getOptions().getPbkdf2Iterations());
    }

    public void encryptAccount(Account account, String secondPassword)
        throws UnsupportedEncodingException, EncryptionException {
        //Double encryption
        if (secondPassword != null) {
            String encryptedPrivateKey = DoubleEncryptionFactory.encrypt(
                account.getXpriv(),
                walletDto.getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );
            account.setXpriv(encryptedPrivateKey);
        }
    }

    public Account addAccount(
        String label,
        @Nullable String secondPassword,
        int version
    ) throws Exception {

        validateSecondPassword(secondPassword);

        //Double decryption if need
        decryptHDWallet(secondPassword);

        WalletBody walletBody = walletDto.getWalletBodies().get(HD_WALLET_INDEX);

        walletBody.setWrapperVersion(version);

        Account account = walletBody.addAccount(label);

        //Double encryption if need
        encryptAccount(account, secondPassword);

        return account;
    }

    public ImportedAddress setKeyForImportedAddress(
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

        if (secondPassword != null) {
            //Double encryption
            String encryptedKey = Base58.encode(ecKey.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.encrypt(
                encryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );

            matchingAddressBody.setPrivateKey(encrypted2);

        }
        else {
            matchingAddressBody.setPrivateKeyFromBytes(ecKey.getPrivKeyBytes());
        }
        return matchingAddressBody;
    }

    /**
     * @deprecated Use the kotlin extension: {@link WalletExtensionsKt#nonArchivedImportedAddressStrings}
     */
    @Deprecated
    public List<String> getImportedAddressStringList() {

        List<String> addrs = new ArrayList<>(walletDto.getImported().size());
        for (ImportedAddress importedAddress : walletDto.getImported()) {
            if (!ImportedAddressExtensionsKt.isArchived(importedAddress)) {
                addrs.add(importedAddress.getAddress());
            }
        }

        return addrs;
    }

    public List<String> getImportedAddressStringList(long tag) {

        List<String> addrs = new ArrayList<>(walletDto.getImported().size());
        for (ImportedAddress importedAddress : walletDto.getImported()) {
            if (importedAddress.getTag() == tag) {
                addrs.add(importedAddress.getAddress());
            }
        }

        return addrs;
    }

    public boolean containsImportedAddress(String addr) {
        if (walletDto.getImported() == null) { return false; }
        for (ImportedAddress importedAddress : walletDto.getImported()) {
            if (importedAddress.getAddress().equals(addr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * In case wallet was encrypted with iterations other than what is specified in options, we
     * will ensure next encryption and options get updated accordingly.
     *
     * @return
     */
    private int fixPbkdf2Iterations() {

        //Use default initially
        int iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2;

        //Old wallets may contain 'wallet_options' key - we'll use this now
        if (
            walletDto.getWalletOptions() != null &&
            walletDto.getWalletOptions().getPbkdf2Iterations() > 0 &&
            walletDto.getOptions() != null
        ) {
            iterations = walletDto.getWalletOptions().getPbkdf2Iterations();
            walletDto.getOptions().setPbkdf2Iterations(iterations);
        }

        //'options' key override wallet_options key - we'll use this now
        if (walletDto.getOptions() != null && walletDto.getOptions().getPbkdf2Iterations() > 0) {
            iterations = walletDto.getOptions().getPbkdf2Iterations();
        }

        //If wallet doesn't contain 'option' - use default
        if (walletDto.getOptions() == null) {
            walletDto.setOptions(Options.Companion.getDefaultOptions());
        }

        //Set iterations
        walletDto.getOptions().setPbkdf2Iterations(iterations);

        return iterations;
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
                if (label == null || label.isEmpty()) {
                    return address;
                }
                else {
                    return label;
                }
            }
        }

        return address;
    }

    //Assume we only support 1 hdWallet
    private static final int HD_WALLET_INDEX = 0;

    private boolean isKeyUnencrypted(String data) {
        if (data == null) { return false; }
        try {
            Base58.decode(data);
            return true;
        } catch (AddressFormatException e) {
            return false;
        }
    }

    public void setImportedAddresses(List<ImportedAddress> backup) {
        walletDto.setImported(backup);
    }
}