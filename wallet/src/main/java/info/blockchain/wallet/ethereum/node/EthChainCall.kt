package info.blockchain.wallet.ethereum.node

import com.blockchain.outcome.Outcome
import java.io.IOException
import java.lang.reflect.Type
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EthChainCall<R>(
    private val delegate: Call<R>,
    private val successType: Type
) : Call<Outcome<EthChainError, R>> {

    override fun enqueue(callback: Callback<Outcome<EthChainError, R>>) = delegate.enqueue(
        object : Callback<R> {

            // Map the response to Outcome.Failure or Success
            override fun onResponse(call: Call<R>, response: Response<R>) {
                callback.onResponse(this@EthChainCall, Response.success(response.toOutcome()))
            }

            // Check if the error is an IOException (i.e. a network error on the client) or an unknown error
            // Always return Response.success as the error is wrapped into Outcome.Failure
            override fun onFailure(call: Call<R>, throwable: Throwable) {
                val error = when (throwable) {
                    is IOException -> EthChainError.NetworkError(throwable)
                    else -> EthChainError.UnknownError(throwable)
                }
                callback.onResponse(this@EthChainCall, Response.success(Outcome.Failure(error)))
            }
        }
    )

    override fun clone(): Call<Outcome<EthChainError, R>> = EthChainCall(delegate.clone(), successType)

    override fun execute(): Response<Outcome<EthChainError, R>> = Response.success(delegate.execute().toOutcome())

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun cancel() = delegate.cancel()

    override fun isCanceled(): Boolean = delegate.isCanceled

    override fun request(): Request = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()

    private fun Response<R>.toOutcome(): Outcome<EthChainError, R> {
        // Http error response (4xx - 5xx)
        val body = body()
        return when {
            !isSuccessful ->
                Outcome.Failure(EthChainError.HttpError(throwable = Throwable(errorBody()?.toString() ?: "")))
            // Http success response with body
            body != null -> Outcome.Success(body)
            else -> Outcome.Failure(EthChainError.UnknownError(throwable = Throwable(errorBody()?.toString() ?: "")))
        }
    }
}
