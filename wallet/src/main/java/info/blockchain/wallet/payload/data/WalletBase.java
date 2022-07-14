package info.blockchain.wallet.payload.data;

import com.blockchain.serialization.JsonSerializableAccount;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.data.walletdto.WalletBaseDto;
import info.blockchain.wallet.util.FormatsUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.spongycastle.crypto.paddings.BlockCipherPadding;
import org.spongycastle.crypto.paddings.ISO10126d2Padding;
import org.spongycastle.crypto.paddings.ISO7816d4Padding;
import org.spongycastle.crypto.paddings.ZeroBytePadding;
import org.spongycastle.util.encoders.Hex;


public class WalletBase {

    private static final int DEFAULT_PBKDF2_ITERATIONS_V1_A = 1;
    private static final int DEFAULT_PBKDF2_ITERATIONS_V1_B = 10;

    private final WalletBaseDto walletBaseDto;
    // HDWallet body containing private keys.
    private Wallet wallet;

    private WalletBase(WalletBaseDto walletBaseDto) {
        this.walletBaseDto = walletBaseDto;
    }

    public WalletBase(Wallet wallet) {
        this.wallet   = wallet;
        walletBaseDto = WalletBaseDto.Companion.withDefaults();
    }

    private WalletBase(WalletBaseDto walletBaseDto, Wallet walletBody) {
        this.walletBaseDto = walletBaseDto;
        this.wallet        = walletBody;
    }

    public WalletBase withWalletBody(Wallet walletBody) {
        return new WalletBase(walletBaseDto, walletBody);
    }

    public Wallet getWallet() {
        return wallet;
    }

    public WalletBase withUpdatedDerivationsForAccounts(List<Account> accounts) {
        return withWalletBody(wallet.withUpdatedDerivationsForAccounts(accounts));
    }

    public WalletBase withUpdatedDefaultDerivationTypeForAccounts(List<Account> accounts) {
        return withWalletBody(wallet.withUpdatedDefaultDerivationTypeForAccounts(accounts));
    }


    private Wallet decryptPayload(@Nonnull String password)
        throws DecryptionException,
        IOException,
        UnsupportedVersionException,
        HDWalletException {

        if (!isV1Wallet()) {
            return decryptV3OrV4Wallet(password);
        }
        else {
            return decryptV1Wallet(password);
        }
    }

    private Wallet decryptV3OrV4Wallet(String password) throws IOException,
        DecryptionException,
        UnsupportedVersionException,
        HDWalletException {

        WalletWrapper walletWrapperBody = WalletWrapper.fromJson(walletBaseDto.getPayload());
        return walletWrapperBody.decryptPayload(password);
    }

    /*
        No need to encrypt V1 wallet again. We will force user to upgrade to V3
     */
    private Wallet decryptV1Wallet(String password)
        throws DecryptionException, IOException, HDWalletException {

        String decrypted = null;
        int succeededIterations = -1000;

        int[] iterations = { DEFAULT_PBKDF2_ITERATIONS_V1_A, DEFAULT_PBKDF2_ITERATIONS_V1_B };
        int[] modes = { AESUtil.MODE_CBC, AESUtil.MODE_OFB };
        BlockCipherPadding[] paddings = {
            new ISO10126d2Padding(),
            new ISO7816d4Padding(),
            new ZeroBytePadding(),
            null // NoPadding
        };

        outerloop:
        for (int iteration : iterations) {
            for (int mode : modes) {
                for (BlockCipherPadding padding : paddings) {
                    try {
                        decrypted = AESUtil.decryptWithSetMode(
                            walletBaseDto.getPayload(),
                            password,
                            iteration,
                            mode,
                            padding
                        );
                        //Ensure it's parsable
                        new JSONObject(decrypted);

                        succeededIterations = iteration;
                        break outerloop;

                    } catch (Exception e) {
                        //                        e.printStackTrace();
                    }
                }
            }
        }

        if (decrypted == null || succeededIterations < 0) {
            throw new DecryptionException("Failed to decrypt");
        }

        String decryptedPayload = decrypted;
        wallet = Wallet.fromJson(decryptedPayload, 1);
        return wallet;
    }

    public WalletBase withUpdatedChecksum(String checksum) {
        return new WalletBase(
            walletBaseDto.withUpdatedPayloadCheckSum(checksum),
            wallet
        );
    }

    public WalletBase withDecryptedPayload(String password) throws
        HDWalletException, IOException, DecryptionException, UnsupportedVersionException {
        return new WalletBase(
            walletBaseDto, decryptPayload(password)
        );
    }

    public WalletBase withSyncedPubKeys() {
        return new WalletBase(
            walletBaseDto.withSyncedKeys(),
            wallet
        );
    }

    public boolean isV1Wallet() {
        return !FormatsUtil.isValidJson(walletBaseDto.getPayload());
    }

    public static WalletBase fromJson(String json) throws IOException {
        return new WalletBase(WalletBaseDto.fromJson(json));
    }

    public String toJson() {
        return walletBaseDto.toJson();
    }

    public Pair encryptAndWrapPayload(String password)
        throws EncryptionException, NoSuchAlgorithmException {

        int version = wallet.getWrapperVersion();
        int iterations = wallet.getOptions().getPbkdf2Iterations();
        String encryptedPayload = AESUtil.encrypt(wallet.toJson(), password, iterations);
        WalletWrapper wrapperBody = WalletWrapper.wrap(encryptedPayload, version, iterations);

        String checkSum = new String(
            Hex.encode(
                MessageDigest.getInstance("SHA-256")
                    .digest(wrapperBody.toJson().getBytes(StandardCharsets.UTF_8))
            )
        );

        return Pair.of(checkSum, wrapperBody);
    }

    public String getPayloadChecksum() {
        return walletBaseDto.getPayloadChecksum();
    }

    public boolean isSyncPubkeys() {
        return walletBaseDto.getSyncPubkeys();
    }

    public WalletBase withUpdatedAccountLabel(JsonSerializableAccount account, String label) {
        return withWalletBody(wallet.updateAccountLabel((Account) account, label));
    }

    public WalletBase withUpdatedAccountState(JsonSerializableAccount account, boolean isArchived) {
        return withWalletBody(wallet.updateArchivedState(account, isArchived));
    }

    public WalletBase withMnemonicState(boolean verified) {
        return withWalletBody(wallet.updateMnemonicVerifiedState(verified));
    }
}