package info.blockchain.wallet.payload.data;


import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import info.blockchain.wallet.MockedResponseTest;

public class WalletBaseTest extends MockedResponseTest {

    @Test
    public void romJson_v3_1f() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v3_1.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("MyTestWallet");
        Assert.assertEquals("d78bf97da866cdda7271a8de0f2d101caf43ae6280b3c69b85bf82d367649ea7", walletBaseBody.getPayloadChecksum());
        Assert.assertNotNull(walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertEquals(5000L, (int) walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v3_2() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v3_2.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("SomeTestPassword");
        Assert.assertEquals("7416cd440f7b15182beb15614a63d5e53b3a6f65634d2b160884c131ab336b01", walletBaseBody.getPayloadChecksum());
        Assert.assertNotNull(walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertEquals(5000L, (int) walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("e5eba801-c8bc-4a64-99ba-094e12a80766", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v3_3() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v3_3.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("SomeTestPassword");
        Assert.assertEquals("fc631f8434f45c43e7040f1192b6676a8bd49e0fd00fb4848acdc0dcaa665400", walletBaseBody.getPayloadChecksum());

        Assert.assertNotNull(walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertEquals(7520, (int) walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("e5eba801-c8bc-4a64-99ba-094e12a80766", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v2_3() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v2_1.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("SomeTestPassword");

        Assert.assertEquals("110764d05c020d4818e2529ca28df9d8b96d50c694650348f885fc075f9366d5", walletBaseBody.getPayloadChecksum());
        Assert.assertNotNull(walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertEquals(5000, (int) walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("5f071985-01b5-4bd4-9d5f-c7cf570b1a2d", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v2_2() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v2_2.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("SomeTestPassword");
        Assert.assertEquals("31b162d3e1fd0b57d8b7dd1202c16604be221bde2fe0192fc0a4e7ce704d3446", walletBaseBody.getPayloadChecksum());

        Assert.assertNotNull(walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertEquals(1000, (int) walletBaseBody.getWallet().getOptions().getPbkdf2Iterations());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("5f071985-01b5-4bd4-9d5f-c7cf570b1a2d", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v1_1() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v1_1.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("mypassword");
        Assert.assertEquals("26c0477b045655bb7ba3e81fb99d7e8ce16f4571400223026169ba8e207677a4", walletBaseBody.getPayloadChecksum());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("9ebb4d4f-f36e-40d6-9a3e-5a3cca5f83d6", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v1_2() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v1_2.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("mypassword");
        Assert.assertEquals("57f97ace89c105c19c43a15f2d6e3091d457dec804243b15772d2062a32f8b7d", walletBaseBody.getPayloadChecksum());

        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("2ca9b0e4-6b82-4dae-9fef-e8b300c72aa2", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void fromJson_v1_3() throws Exception {
        URI uri = getClass().getClassLoader().getResource("wallet_v1_3.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("mypassword");
        Assert.assertEquals("a4b67f406268dced75ac5c628da854898c9a3134b7e3755311f199723d426765", walletBaseBody.getPayloadChecksum());
        Assert.assertNotNull(walletBaseBody.getWallet());
        Assert.assertEquals("4077b6d9-73b3-4d22-96d4-9f8810fec435", walletBaseBody.getWallet().getGuid());
    }

    @Test
    public void encryptAndWrapPayload() throws Exception {

        URI uri = getClass().getClassLoader().getResource("wallet_v3_1.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        /////////////
        //Decrypt
        WalletBase walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("MyTestWallet");

        //Encrypt
        Pair pair = walletBaseBody.encryptAndWrapPayload("MyTestWallet");

        //Check wallet wrapper
        WalletWrapper encryptedwalletWrapper = (WalletWrapper) pair.getRight();
        Assert.assertEquals(5000, encryptedwalletWrapper.getPbkdf2Iterations());
        Assert.assertEquals(3, encryptedwalletWrapper.getVersion());

        //Decrypt again to check payload intact
        Wallet walletBody = encryptedwalletWrapper.decryptPayload("MyTestWallet");
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBody.getGuid());

        ///////Encrypt with different iterations//////
        //Decrypt
        walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("MyTestWallet");
        walletBaseBody = walletBaseBody.withWalletBody(walletBaseBody.getWallet().updatePbkdf2Iterations(7500));

        //Encrypt
        pair = walletBaseBody.encryptAndWrapPayload("MyTestWallet");

        //Check wallet wrapper
        encryptedwalletWrapper = (WalletWrapper) pair.getRight();
        Assert.assertEquals(7500, encryptedwalletWrapper.getPbkdf2Iterations());
        Assert.assertEquals(3, encryptedwalletWrapper.getVersion());

        //Decrypt again to check payload intact
        walletBody = encryptedwalletWrapper.decryptPayload("MyTestWallet");
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBody.getGuid());

        ///////Encrypt with different password//////
        //Decrypt
        walletBaseBody = WalletBase.fromJson(walletBase).withDecryptedPayload("MyTestWallet");
        walletBaseBody = walletBaseBody.withWalletBody(walletBaseBody.getWallet().updatePbkdf2Iterations(7500));

        //Encrypt
        pair = walletBaseBody.encryptAndWrapPayload("MyNewTestWallet");

        //Check wallet wrapper
        encryptedwalletWrapper = (WalletWrapper) pair.getRight();
        Assert.assertEquals(7500, encryptedwalletWrapper.getPbkdf2Iterations());
        Assert.assertEquals(3, encryptedwalletWrapper.getVersion());

        //Decrypt again to check payload intact
        walletBody = encryptedwalletWrapper.decryptPayload("MyNewTestWallet");
        Assert.assertEquals("a09910d9-1906-4ea1-a956-2508c3fe0661", walletBody.getGuid());
    }

    @Test
    public void testToJSON() throws Exception {

        //Ensure toJson doesn't write any unintended fields
        URI uri = getClass().getClassLoader().getResource("wallet_v3_1.txt").toURI();
        String walletBase = new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);

        WalletBase walletBaseBody = WalletBase.fromJson(walletBase);
        String jsonString = walletBaseBody.toJson();

        JSONObject jsonObject = new JSONObject(jsonString);
        Assert.assertEquals(7, jsonObject.keySet().size());
    }
}