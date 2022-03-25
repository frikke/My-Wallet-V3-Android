package info.blockchain.wallet.ethereum.data

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import java.util.HashMap

class EthAddressResponseMap {

    private val map: MutableMap<String, EthAddressResponse> = HashMap()

    @JsonAnyGetter
    fun getEthAddressResponseMap(): Map<String, EthAddressResponse> {
        return map
    }

    @JsonAnySetter
    fun setEthAddressResponseMap(name: String, value: EthAddressResponse) {
        map[name] = value
    }
}
