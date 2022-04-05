package info.blockchain.wallet.ethereum.node

import com.blockchain.outcome.Outcome
import java.lang.reflect.Type
import retrofit2.Call
import retrofit2.CallAdapter

class EthChainCallAdapter<R>(
    private val successType: Type
) : CallAdapter<R, Call<Outcome<EthChainError, R>>> {

    // Wrap the original Call into an OutcomeCall
    override fun adapt(call: Call<R>): Call<Outcome<EthChainError, R>> = EthChainCall(call, successType)

    override fun responseType(): Type = successType
}
