package info.blockchain.wallet.payload.data;

import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.data.walletdto.WalletBaseDto;
import info.blockchain.wallet.util.FormatsUtil;
import kotlinx.serialization.modules.SerializersModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private WalletBase(WalletBaseDto walletBaseDto) {
        this.walletBaseDto = walletBaseDto;
    }

    public WalletBase() {
        walletBaseDto = new WalletBaseDto();
    }

    public String getGuid() {
        return walletBaseDto.getGuid();
    }

    public void setGuid(String guid) {
        walletBaseDto.setGuid(guid);
    }

    public void decryptPayload(@Nonnull String password)
        throws DecryptionException,
        IOException,
        UnsupportedVersionException,
        HDWalletException {

        if (!isV1Wallet()) {
            walletBody = decryptV3OrV4Wallet(password);
        }
        else {
            walletBody = decryptV1Wallet(password);
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
        walletBody = Wallet.fromJson(decryptedPayload);
        return walletBody;
    }

    public String getExtraSeed() {
        return walletBaseDto.getExtraSeed();
    }

    public void setExtraSeed(String extraSeed) {
        walletBaseDto.setExtraSeed(extraSeed);
    }

    public String getPayloadChecksum() {
        return walletBaseDto.getPayloadChecksum();
    }

    public void setPayloadChecksum(String payloadChecksum) {
        walletBaseDto.setPayloadChecksum(payloadChecksum);
    }

    public String getWarChecksum() {
        return walletBaseDto.getWarChecksum();
    }

    public void setWarChecksum(String warChecksum) {
        walletBaseDto.setWarChecksum(warChecksum);
    }

    public String getLanguage() {
        return walletBaseDto.getLanguage();
    }

    public void setLanguage(String language) {
        walletBaseDto.setLanguage(language);
    }

    public String getStorageToken() {
        return walletBaseDto.getStorageToken();
    }

    public void setStorageToken(String storageToken) {
        walletBaseDto.setStorageToken(storageToken);
    }

    public boolean isSyncPubkeys() {
        return walletBaseDto.getSyncPubkeys();
    }

    public void setSyncPubkeys(boolean syncPubkeys) {
        walletBaseDto.setSyncPubkeys(syncPubkeys);
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

        int version = walletBody.getWrapperVersion();
        int iterations = walletBody.getOptions().getPbkdf2Iterations();
        String encryptedPayload;
        SerializersModule serializersModule = WalletWrapper.getSerializerForVersion(version);
        encryptedPayload = AESUtil.encrypt(walletBody.toJson(serializersModule), password, iterations);
        WalletWrapper wrapperBody = WalletWrapper.wrap(encryptedPayload, version, iterations);

        String checkSum = new String(
            Hex.encode(
                MessageDigest.getInstance("SHA-256")
                    .digest(wrapperBody.toJson(version).getBytes(StandardCharsets.UTF_8))
            )
        );

        return Pair.of(checkSum, wrapperBody);
    }

    // HDWallet body containing private keys
    private Wallet walletBody;

    public Wallet getWalletBody() {
        return walletBody;
    }

    public void setWalletBody(Wallet walletBody) {
        this.walletBody = walletBody;
    }
}