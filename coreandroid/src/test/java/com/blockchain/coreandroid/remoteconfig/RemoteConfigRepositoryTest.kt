package com.blockchain.coreandroid.remoteconfig

import com.blockchain.core.experiments.cache.ExperimentsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.preferences.RemoteConfigPrefs
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) class RemoteConfigRepositoryTest() {

    private val firebaseRemoteConfig = mockk<FirebaseRemoteConfig>()
    private val remoteConfigPrefs = mockk<RemoteConfigPrefs>()
    private val experimentsStore = mockk<ExperimentsStore>()
    private val json: Json = Json

    private lateinit var remoteConfigRepository: RemoteConfigRepository

    @Before
    fun setup() {
        remoteConfigRepository = RemoteConfigRepository(
            firebaseRemoteConfig = firebaseRemoteConfig,
            remoteConfigPrefs = remoteConfigPrefs,
            experimentsStore = experimentsStore,
            json = json
        )
    }

    @Test
    fun `GIVEN deepMap for key THEN returns expected String`() = runTest {
        val result = remoteConfigRepository.deepMap("Hello")
        assertTrue { result == "Hello" }
    }

    @Test
    fun `GIVEN deepMap for key experiment == null THEN return default value`() = runTest {
        val mapReturned = mapOf("experiment-1" to 1)
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val default = "WEEKLY"
        val result = remoteConfigRepository.deepMap(experimentJsonNoKey)
        assertTrue { result == default }
    }

    @Test
    fun `GIVEN deepMap for key experiment == null and default doesn't exist THEN return throw error`() = runTest {
        val mapReturned = mapOf("experiment-1" to 1)
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val error = NoSuchElementException()
        val result = remoteConfigRepository.deepMap(experimentJsonNoKeyNoDefault)
        assertTrue { (result as Throwable).cause == error.cause }
    }

    @Test
    fun `GIVEN deepMap for key and rootJson != {returns} WHEN Data is success THEN returns expected type`() = runTest {
        val mapReturned = mapOf("experiment-1" to 1, "experiment-2" to 0)
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val toReturn = mapOf(
            "title" to "Cowboys Promotion",
            "message" to "Message 2"
        )
        val result = remoteConfigRepository.deepMap(experimentJsonKeys)
        assertTrue { result == toReturn }
    }

    @Test
    fun `GIVEN deepMap for experimentValue no existent and no default WHEN Data is success THEN returns Error`() =
        runTest {
            val mapReturned = mapOf("experiment-1" to 13)
            val flowResult = flowOf(DataResource.Data(mapReturned))
            coEvery {
                experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            } returns flowResult
            val error = NoSuchElementException()
            val result = remoteConfigRepository.deepMap(experimentJsonKeys)
            assertTrue { (result as Throwable).cause == error.cause }
        }

    @Test
    fun `GIVEN deepMap for rootJson == {returns} WHEN Data is success THEN returns Map`() =
        runTest {
            val mapReturned = mapOf("experiment-1" to 0)

            val flowResult = flowOf(DataResource.Data(mapReturned))
            coEvery {
                experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            } returns flowResult
            val toReturn = mapOf(
                "title" to "Cowboys promo",
                "message" to "Want to win suite tickets for you and 7 friends?"
            )
            val result = remoteConfigRepository.deepMap(jsonCowboysMap)
            assertTrue { result == toReturn }
        }

    @Test
    fun `GIVEN deepMap for key WHEN Data is success THEN returns expected type`() = runTest {
        val mapReturned = mapOf("experiment-2" to 1)
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val result = remoteConfigRepository.deepMap(experimentJson)
        assert(result.toString() == "BIWEEKLY")
    }

    @Test
    fun `GIVEN deepMap for key WHEN Data is Success but not key for value THEN returns default value`() = runTest {
        val mapReturned = mapOf("experiment-2" to 56)
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val default = "WEEKLY"
        val result = remoteConfigRepository.deepMap(experimentJson)
        assert(result.toString() == default)
    }

    @Test
    fun `GIVEN deepMap for key WHEN Data is Success and default doesn't exist THEN returns throw error`() = runTest {
        val mapReturned = mapOf("experiment-2" to 5)
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val error = NoSuchElementException()
        val result = remoteConfigRepository.deepMap(experimentJsonNoDefault)
        assertTrue { (result as Throwable).cause == error.cause }
    }

    @Test
    fun `GIVEN deepMap for key WHEN Data is error THEN returns default value`() = runTest {
        val flowResult = flowOf(DataResource.Error(Exception()))

        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult

        val default = "WEEKLY"
        val result = remoteConfigRepository.deepMap(experimentJson)
        assertTrue { result.toString() == default }
    }

    @Test
    fun `GIVEN deepMap for key WHEN Data is error and default doesn't exist THEN returns throw error`() = runTest {
        val flowResult = flowOf(DataResource.Error(Exception()))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val error = NoSuchElementException()
        val result = remoteConfigRepository.deepMap(experimentJsonNoDefault)
        assertTrue { (result as Throwable).cause == error.cause }
    }

    @Test
    fun `GIVEN deepMap for key WHEN experimentKey is missing THEN returns default value String`() = runTest {
        val mapReturned = emptyMap<String, Int>()
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val result = remoteConfigRepository.deepMap(experimentJsonNoKey)
        val default = "WEEKLY"
        assertTrue { result == default }
    }

    @Test
    fun `GIVEN deepMap for key WHEN experimentKey is missing THEN returns default value Map`() = runTest {
        val mapReturned = emptyMap<String, Int>()
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val result = remoteConfigRepository.deepMap(experimentJsonNoKeyMap)
        val mapDefault = mapOf("algo" to "something")
        assertTrue { result == mapDefault }
    }

    @Test
    fun `GIVEN deepMap for key WHEN experimentKey is missing THEN returns default value Array`() = runTest {
        val mapReturned = emptyMap<String, Int>()
        val flowResult = flowOf(DataResource.Data(mapReturned))
        coEvery {
            experimentsStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
        } returns flowResult
        val result = remoteConfigRepository.deepMap(experimentJsonNoKeyArray)
        val defaultArray = listOf("one", "two", "three")
        assertTrue { result == defaultArray }
    }

    companion object {

        private const val experimentJsonKeys = "{\n" +
            "  \"title\": \"Cowboys Promotion\",\n" +
            "  \"message\": {\n" +
            "    \"{returns}\": {\n" +
            "      \"experiment\": {\n" +
            "        \"experiment-1\": {\n" +
            "          \"0\": \"Message 1\",\n" +
            "          \"1\": \"Message 2\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}"

        private const val experimentJson = "{\n" +
            "   \"{returns}\":{\n" +
            "      \"experiment\":{\n" +
            "         \"experiment-2\":{\n" +
            "            \"0\":\"WEEKLY\",\n" +
            "            \"1\":\"BIWEEKLY\",\n" +
            "            \"2\":\"MONTHLY\"\n" +
            "         }\n" +
            "      }\n" +
            "   },\n" +
            "   \"default\":\"WEEKLY\"\n" +
            "}"

        private const val experimentJsonNoDefault = "{\n" +
            "  \"{returns}\": {\n" +
            "    \"experiment\": {\n" +
            "      \"experiment-2\": {\n" +
            "        \"0\": \"WEEKLY\",\n" +
            "        \"1\": \"BIWEEKLY\",\n" +
            "        \"2\": \"MONTHLY\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}"

        private const val experimentJsonNoKey = """{"{returns}":{"experiment":{}},"default":"WEEKLY"}"""
        private const val experimentJsonNoKeyNoDefault = """{"{returns}":{"experiment":{}}}"""

        private const val experimentJsonNoKeyArray =
            """{"{returns}":{"experiment":{}},"default":["one","two","three"]}"""

        private const val experimentJsonNoKeyMap = """{"{returns}":{"experiment":{}},"default":{"algo":"something"}}"""

        private const val jsonCowboysMap = "{\n" +
            "  \"{returns}\": {\n" +
            "    \"experiment\": {\n" +
            "      \"experiment-1\": {\n" +
            "        \"0\": {\n" +
            "          \"title\": \"Cowboys promo\",\n" +
            "          \"message\": \"Want to win suite tickets for you and 7 friends?\"\n" +
            "        },\n" +
            "        \"1\": {\n" +
            "          \"title\": \"Cowboys promo\",\n" +
            "          \"message\": \"Verify your ID\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}"
    }
}
