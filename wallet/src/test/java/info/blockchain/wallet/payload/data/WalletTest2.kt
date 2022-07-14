package info.blockchain.wallet.payload.data

import org.bitcoinj.core.AddressFormatException
import org.junit.Test

class WalletTest2 {

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `if seedhex is missing payload should not desirialised normally`() {
        Wallet.fromJson(defPayload.replace("seed_hex", "seedHex"), 4)
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `if xpub is missing payload should not desirialised normally`() {
        Wallet.fromJson(defPayload.replace("xpub", "21312"), 4)
    }

    @Test(expected = AddressFormatException::class)
    fun `if seedHex  is not valid payload should desirialised but HD wallet exception is expected`() {
        Wallet.fromJson(defPayload.replace("c7920c5e1a93602bbd052b52b66aeb4e", "21wqdsas"), 4)
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `if xpriv is missing payload should not desirialised normally`() {
        Wallet.fromJson(defPayload.replace("xpriv", "21312"), 4)
    }

    @Test
    fun `if metadataHDNode is missing payload should desirialised normally`() {
        val wallet = Wallet.fromJson(
            defPayload.replace(
                "metadataHDNode",
                "metadataHDNodeasdas"
            ),
            4
        )
        assertDefault(wallet)
        assertBody(wallet.walletBody!!)
    }

    @Test
    fun `if hd_wallets is missing payload should desirialised normally and no accounts should present`() {
        val wallet = Wallet.fromJson(
            defPayload.replace(
                "hd_wallets",
                "12312"
            ),
            4
        )
        assertDefault(wallet)
        assert(wallet.walletBodies!!.isEmpty())
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `if receiveAccount and changeAccount from cache missing payload serialisation should fail`() {
        val wallet = Wallet.fromJson(
            defPayload.replace("changeAccount", "1231")
                .replace("receiveAccount", "wddas"),
            4
        )
        assertDefault(wallet)
        assertBody(wallet.walletBodies!![0])
    }

    @Test
    fun `payload should be desiriased normally`() {
        val wallet = Wallet.fromJson(defPayload, 4)
        assertDefault(wallet)
        assertBody(wallet.walletBodies!![0])
    }

    private fun assertDefault(wallet: Wallet) {
        assert(wallet.txNotes.isEmpty())
        assert(wallet.guid == "dc2095df-0675-47d3-86e5-178ac9220f15")
        assert(wallet.importedAddressList.isEmpty())
        assert(wallet.sharedKey == "20cd6a2b-bff7-4bfa-ac66-090bda6c5b87")
        assert(wallet.options == Options(pbkdf2Iterations = 5000, feePerKb = 10000, logoutTime = 600000L))
    }

    private fun assertBody(walletBody: WalletBody) {
        assert(walletBody.seedHex == "c7920c5e1a93602bbd052b52b66aeb4e")
        assert(walletBody.mnemonicVerified)
        assert(walletBody.defaultAccountIdx == 0)
    }
}

private const val defPayload = "{\n" +
    "  \"tx_notes\": {},\n" +
    "  \"guid\": \"dc2095df-0675-47d3-86e5-178ac9220f15\",\n" +
    "  \"metadataHDNode\": \"xprv9tvfAiw1NyzXRDepGL7kpysBUTFpNSkJTdXSVcSmgUWApwp5BEG28bwdxWRjEn6WgzAz1sunGPToat7nn7sU1arn2nUj2fsJoSLMqxXB9rr\",\n" +
    "  \"tx_names\": [],\n" +
    "  \"address_book\": [],\n" +
    "  \"keys\": [],\n" +
    "  \"hd_wallets\": [\n" +
    "    {\n" +
    "      \"accounts\": [\n" +
    "        {\n" +
    "          \"label\": \"Private Key Wallet\",\n" +
    "          \"default_derivation\": \"bech32\",\n" +
    "          \"derivations\": [\n" +
    "            {\n" +
    "              \"type\": \"legacy\",\n" +
    "              \"purpose\": 44,\n" +
    "              \"xpriv\": \"1arxidi\",\n" +
    "              \"xpub\": \"1midi\",\n" +
    "              \"cache\": {\n" +
    "                \"receiveAccount\": \"asasdas\",\n" +
    "                \"changeAccount\": \"dsffsadfadfas\"\n" +
    "              },\n" +
    "              \"address_labels\": []\n" +
    "            },\n" +
    "            {\n" +
    "              \"type\": \"bech32\",\n" +
    "              \"purpose\": 84,\n" +
    "              \"xpriv\": \"adsgadsgsdg\",\n" +
    "              \"xpub\": \"Mitsotaki miesaiga\",\n" +
    "              \"cache\": {\n" +
    "                \"receiveAccount\": \"xpub6Dw9X8tPV9ELRypCJxgSnHUMMuVHbo77BDSkeEpJwZZf9TwwtYNnDBaVU8ZXtDqfjU4WZ92dT8axK3t5KEDwAC99LXEW1GjRnrZowfZhayh\",\n" +
    "                \"changeAccount\": \"xpub6Dw9X8tPV9ELT7sLcSGjsLESPJyFxtVZpzE8RG69a9Xh44ADniuj3Rcip2gDF84md42S3YcU9gL4EAJAibRqYtHUhqPovScX3qWKtfeKhUr\"\n" +
    "              },\n" +
    "              \"address_labels\": []\n" +
    "            }\n" +
    "          ]\n" +
    "        }\n" +
    "      ],\n" +
    "      \"seed_hex\": \"c7920c5e1a93602bbd052b52b66aeb4e\",\n" +
    "      \"passphrase\": \"\",\n" +
    "      \"mnemonic_verified\": true,\n" +
    "      \"default_account_idx\": 0\n" +
    "    }\n" +
    "  ],\n" +
    "  \"sharedKey\": \"20cd6a2b-bff7-4bfa-ac66-090bda6c5b87\",\n" +
    "  \"options\": {\n" +
    "    \"pbkdf2_iterations\": 5000,\n" +
    "    \"fee_per_kb\": 10000,\n" +
    "    \"logout_time\": 600000\n" +
    "  }\n" +
    "}"
