package info.blockchain.wallet

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import java.io.IOException
import java.lang.RuntimeException
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

abstract class MockedResponseTest {
    protected var mockInterceptor: MockInterceptor? = null
    @Before fun initBlockchainFramework() {
        mockInterceptor = MockInterceptor()
    }

    @Before fun setupRxCalls() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { schedulerCallable: Scheduler? -> TrampolineScheduler.instance() }
        RxJavaPlugins.setComputationSchedulerHandler { schedulerCallable: Scheduler? -> TrampolineScheduler.instance() }
        RxJavaPlugins.setNewThreadSchedulerHandler { schedulerCallable: Scheduler? -> TrampolineScheduler.instance() }
    }

    @After fun tearDownRxCalls() {
        RxJavaPlugins.reset()
    }

    fun newOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(mockInterceptor!!) // Mock responses
            .addInterceptor(ApiInterceptor()) // Extensive logging
            .build()
    }

    fun getRetrofit(url: String?, client: OkHttpClient?): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }

    protected fun loadResourceContent(resourceFile: String?): String {
        return try {
            val uri = javaClass.classLoader.getResource(resourceFile).toURI()
            String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
