package com.androbito.tilfaz.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.androbito.tilfaz.core.model.Account
import com.androbito.tilfaz.core.model.DomainError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class XtreamRequestInterceptorTest {
    private val server = MockWebServer()
    private var account: Account? = null
    private var userAgent = "TilfazTV/test"

    private lateinit var api: XtreamApi

    @Before
    fun setUp() {
        server.start()
        account = Account(server.url("/").toString().trimEnd('/'), "ayoub", "p@ss word")
        val client = OkHttpClient.Builder()
            .addInterceptor(XtreamRequestInterceptor({ account }, { userAgent }))
            .build()
        api = Retrofit.Builder()
            .baseUrl(XtreamApi.PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(XtreamApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `routes the placeholder host to the account server and appends credentials`() = runTest {
        server.enqueue(MockResponse().setBody("""[{"category_id":"1","category_name":"News"}]"""))

        val categories = api.liveCategories()

        val recorded = server.takeRequest()
        assertThat(recorded.requestUrl?.queryParameter("action")).isEqualTo("get_live_categories")
        assertThat(recorded.requestUrl?.queryParameter("username")).isEqualTo("ayoub")
        assertThat(recorded.requestUrl?.queryParameter("password")).isEqualTo("p@ss word")
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("TilfazTV/test")
        assertThat(categories.single().categoryName).isEqualTo("News")
    }

    @Test
    fun `missing account becomes a NotSignedIn domain error`() = runTest {
        account = null

        val result = safeApiCall { api.liveCategories() }

        assertThat(result.exceptionOrNull()?.toDomainException()?.error).isEqualTo(DomainError.NotSignedIn)
    }

    @Test
    fun `http 401 becomes Unauthorized and 503 becomes Server`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(503))

        assertThat(safeApiCall { api.liveCategories() }.exceptionOrNull()?.toDomainException()?.error).isEqualTo(DomainError.Unauthorized)
        assertThat(safeApiCall { api.liveCategories() }.exceptionOrNull()?.toDomainException()?.error).isEqualTo(DomainError.Server(503))
    }

    @Test
    fun `html instead of json becomes BadResponse`() = runTest {
        server.enqueue(MockResponse().setBody("<html>Blocked</html>"))

        val error = safeApiCall { api.liveCategories() }.exceptionOrNull()?.toDomainException()?.error

        assertThat(error).isInstanceOf<DomainError.BadResponse>()
    }
}
