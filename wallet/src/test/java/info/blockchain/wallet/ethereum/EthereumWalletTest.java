package info.blockchain.wallet.ethereum;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.RawTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;

import info.blockchain.balance.AssetInfo;
import info.blockchain.wallet.MockedResponseTest;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.payload.data.Derivation;

public class EthereumWalletTest extends MockedResponseTest {
    private final String defaultLabel = "My Wallet";
    private final HashMap<AssetInfo, String> allLabels = new HashMap<>();
    private final HashMap<AssetInfo, String> erc20Labels = new HashMap<>();

    private EthereumWallet subject;

    @Before
    public void setup() {
//        allLabels.put(CryptoCurrency.ETHER, ethereumLabel);
//        allLabels.put(CryptoCurrency.PAX, paxLabel);
//        allLabels.put(CryptoCurrency.USDT, usdtLabel);
//        allLabels.put(CryptoCurrency.DGLD, dgldLabel);
//        allLabels.put(CryptoCurrency.AAVE, aaveLabel);
//        allLabels.put(CryptoCurrency.YFI, yfiLabel);
//
//        erc20Labels.put(CryptoCurrency.PAX, paxLabel);
//        erc20Labels.put(CryptoCurrency.USDT, usdtLabel);
//        erc20Labels.put(CryptoCurrency.DGLD, dgldLabel);
//        erc20Labels.put(CryptoCurrency.AAVE, aaveLabel);
//        erc20Labels.put(CryptoCurrency.YFI, yfiLabel);
    }

    private HDWallet getWallet(String seedHex) {
        return HDWalletFactory.restoreWallet(
            Language.US,
            seedHex,
            "",
            1, Derivation.LEGACY_PURPOSE
        );
    }

    private HDWallet getWallet1() {
        //bicycle balcony prefer kid flower pole goose crouch century lady worry flavor
        return getWallet("15e23aa73d25994f1921a1256f93f72c");
    }

    private HDWallet getWallet2() {
        //radar blur cabbage chef fix engine embark joy scheme fiction master release
        return getWallet("b0a30c7e93a58094d213c4c0aaba22da");
    }

    private HDWallet getWallet3() {
        //search present horn clip convince wash motion want sea desert admit increase =jaxx
        return getWallet("c21541b615a2f9eee417b7c1e7740eb9");
    }

    @Test
    public void constructor1(){

        HDWallet wallet = getWallet1();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), defaultLabel);

        //Assert
        EthereumAccount account = subject.getAccount();
        Assert.assertFalse(subject.hasSeen());
        Assert.assertEquals(0, subject.getTxNotes().size());

        Assert.assertEquals(
            "60e2d382449758aab3866585dc69a946e3566bca0eea274b9073cb60da636133",
            account.deriveSigningKey(wallet.getMasterKey()).getPrivateKeyAsHex());

        Assert.assertTrue(subject.getAccount().getAddress()
            .equalsIgnoreCase("0x14f2BD143692B14D170c34b2eE25EE5FC61e8570"));
    }

    @Test
    public void constructor2() {

        HDWallet wallet = getWallet2();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), defaultLabel);

        //Assert
        EthereumAccount account = subject.getAccount();
        Assert.assertFalse(subject.hasSeen());
        Assert.assertEquals(0, subject.getTxNotes().size());

        Assert.assertEquals(
            "b96e9ccb774cc33213cbcb2c69d3cdae17b0fe4888a1ccd343cbd1a17fd98b18",
             account.deriveSigningKey(wallet.getMasterKey()).getPrivateKeyAsHex()
        );

        Assert.assertTrue(subject.getAccount().getAddress()
            .equalsIgnoreCase("0xaC39b311DCEb2A4b2f5d8461c1cdaF756F4F7Ae9"));
    }

    @Test
    public void constructor3() {

        HDWallet wallet = getWallet3();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        //Act
        subject = new EthereumWallet(wallet.getMasterKey(), defaultLabel);

        //Assert
        EthereumAccount account = subject.getAccount();
        Assert.assertFalse(subject.hasSeen());
        Assert.assertEquals(0, subject.getTxNotes().size());

        Assert.assertEquals(
            "6e1ae089604577d31f25617297e4f50ef1b06376d7b04419c7e82e2507927857",
                    account.deriveSigningKey(wallet.getMasterKey()).getPrivateKeyAsHex()
        );

        Assert.assertTrue(subject.getAccount().getAddress()
            .equalsIgnoreCase("0x351e4184A9aBe6B71a2a7a71c2628c47cC861e51"));
    }

    @Test
    public void load() throws Exception {

        //Arrange
        HDWallet wallet = getWallet3();

        EthereumWallet eth = new EthereumWallet(wallet.getMasterKey(), defaultLabel);
        eth.setHasSeen(true);

        //Act
        subject = EthereumWallet.load(eth.toJson(true), true);

        //Assert
        Assert.assertTrue(subject.hasSeen());
        Assert.assertEquals(eth.toJson(true), subject.toJson(true));
    }

    @Test
    public void loadEmptyWallet() throws Exception {

        //Arrange

        //Act
        subject = EthereumWallet.load(null, true);

        //Assert
        Assert.assertNull(subject);
    }

//@Test
//public void paxErc20Created() {
//    //Arrange
//    HDWallet wallet = getWallet3();
//
//    //Act
//    subject = new EthereumWallet(wallet.getMasterKey(), defaultLabel);
//
//    //Assert
//    Erc20TokenData tokenData = subject.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME);
//    Assert.assertNotNull(tokenData);
//}

//@Test
//public void paxErc20UpdateWallet() throws Exception {
//    //Arrange
//    String json = getStringFromResource(this, "eth_wallet_no_pax.json");
//    subject = EthereumWallet.fromJson(json);
//
//    //Act
//    boolean wasUpdated = subject.updateErc20Tokens(erc20Labels);
//
//    //Assert
//    Assert.assertTrue(wasUpdated);
//    Erc20TokenData tokenData = subject.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME);
//    Assert.assertNotNull(tokenData);
//}

//@Test
//public void load_with_erc20_txNote() throws Exception {
//
//    //Arrange
//    HDWallet wallet = getWallet3();
//
//    EthereumWallet eth = new EthereumWallet(wallet.getMasterKey(), defaultLabel);
//    eth.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME)
//        .putTxNote("one", "two");
//
//    //Act
//    subject = EthereumWallet.load(eth.toJson());
//
//    //Assert
//    Erc20TokenData tokenData = subject.getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME);
//    Assert.assertEquals(tokenData.getTxNotes().size(), 1);
//    Assert.assertEquals(eth.toJson(), subject.toJson());
//}

    @Test
    public void signTransaction() {
        HDWallet wallet = getWallet1();

        //Arrange
        LinkedList<Pair> responseList = new LinkedList<>();
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        responseList.add(Pair.of(404, "{\"message\":\"Not Found\"}"));
        mockInterceptor.setResponseList(responseList);

        subject = new EthereumWallet(wallet.getMasterKey(), defaultLabel);
        RawTransaction tx = createEtherTransaction();

        //Act
        EthereumAccount account = subject.getAccount();
        byte[] signTransaction = account.signTransaction(
            tx, wallet.getMasterKey()
        );

        //Assert
        Assert.assertEquals(
            "0xf85580010a840add5355887fffffffffffffff8025a08024957602ed" +
            "99025d0e9e6b76baf878b66e52472c71766c3b5826804d2f9469a0522b" +
            "18b3e63a7b53f27657448115b0e987a0e14b9d8732ebf0c553e328f3cfab",
            Numeric.toHexString(signTransaction));
    }

    private static RawTransaction createEtherTransaction() {
        return RawTransaction.createEtherTransaction(
            BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN, "0xadd5355",
            BigInteger.valueOf(Long.MAX_VALUE));
    }
}
