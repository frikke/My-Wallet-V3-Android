package info.blockchain.wallet.ethereum

import com.blockchain.outcome.Outcome
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.internal.createInstance
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import info.blockchain.wallet.ethereum.node.EthJsonRpcRequest
import info.blockchain.wallet.ethereum.node.EthJsonRpcResponse
import info.blockchain.wallet.ethereum.node.RequestType
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito

class EthAccountApiTest {
    private val ethEndpoints: EthEndpoints = mock()
    private val ethNodeEndpoints: EthNodeEndpoints = mock()
    private val apiCode = "1234"
    private val subject: EthAccountApi = EthAccountApi(ethEndpoints, ethNodeEndpoints, apiCode)

    @Test
    fun getEthAccount() {
        val addresses = arrayListOf("firstAddress", "secondAddress")

        val expectedResponse = hashMapOf(
            addresses[0] to mock<EthAddressResponse>(),
            addresses[1] to mock()
        )

        whenever(
            ethEndpoints.getEthAccount(addresses.joinToString(","))
        ).thenReturn(
            Observable.just(expectedResponse)
        )

        subject.getEthAddress(addresses).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedResponse
            }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getBalanceForAccount() {
        val address = "firstAddress"
        val mockBalanceResponse: EthJsonRpcResponse = mock()
        val expectedResponse = Outcome.Success(mockBalanceResponse)

        runTest {
            whenever(
                ethNodeEndpoints.processRequest(
                    withAnyRequestMatching(
                        EthJsonRpcRequest.create(
                            params = arrayOf(address),
                            type = RequestType.GET_BALANCE
                        )
                    )
                )
            ).thenReturn(expectedResponse)
            val result = subject.postEthNodeRequest(requestType = RequestType.GET_BALANCE, address, "latest")
            assertEquals(expectedResponse, result)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getIfContract_returns_false() {
        val address = "address"
        val mockContractResponse: EthJsonRpcResponse = mock {
            on { result }.thenReturn("")
        }
        val expectedResponse = Outcome.Success(mockContractResponse)

        runTest {
            whenever(
                ethNodeEndpoints.processRequest(
                    withAnyRequestMatching(
                        EthJsonRpcRequest.create(
                            params = arrayOf(address),
                            type = RequestType.IS_CONTRACT
                        )
                    )
                )
            ).thenReturn(expectedResponse)
            val result = subject.postEthNodeRequest(requestType = RequestType.IS_CONTRACT, address)
            assertEquals(expectedResponse, result)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun pushTx() {
        val rawTx = ""
        val txHash = "0xc88ac065147b34f7a4965f9b0dc539f7863468da61a73b14eb0f8f0fcbb72e5a"
        val mockContractResponse: EthJsonRpcResponse = mock {
            on { result }.thenReturn(txHash)
        }
        val expectedResponse = Outcome.Success(mockContractResponse)

        runTest {
            whenever(
                ethNodeEndpoints.processRequest(
                    withAnyRequestMatching(
                        EthJsonRpcRequest.create(
                            params = arrayOf(rawTx),
                            type = RequestType.PUSH_TRANSACTION
                        )
                    )
                )
            ).thenReturn(expectedResponse)
            val result = subject.postEthNodeRequest(requestType = RequestType.PUSH_TRANSACTION, rawTx)
            assertEquals(expectedResponse, result)
            assertEquals(txHash, (result as Outcome.Success).value.result)
        }
    }
}

private fun withAnyRequestMatching(request: EthJsonRpcRequest): EthJsonRpcRequest {
    return Mockito.argThat(RequestMatcher(request)) ?: createInstance()
}

private class RequestMatcher(val request: EthJsonRpcRequest) : ArgumentMatcher<EthJsonRpcRequest> {
    override fun matches(argument: EthJsonRpcRequest?): Boolean {
        return if (argument == null) {
            false
        } else {
            request.id == argument.id && request.method == argument.method && request.params == request.params
        }
    }
}
