package com.example.harpapp.protocol

import com.google.gson.JsonObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Test kontraktowy: każdy wektor z docs/protocol/test-vectors.json (sekcja 10 specyfikacji)
@RunWith(Parameterized::class)
class HarpCodecContractTest(
    @Suppress("unused") private val name: String,
    private val vector: JsonObject
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun vectors(): List<Array<Any>> =
            TestVectors.all.map { arrayOf(it.get("name").asString, it) }
    }

    private val isValid get() = vector.get("valid").asBoolean

    @Test
    fun decodeMatchesVector() {
        val result = HarpCodec.decode(TestVectors.bytes(vector))

        if (isValid) {
            assertEquals(HarpProtocol.VERSION_MAJOR, vector.getAsJsonObject("decoded").get("ver").asInt)
            assertEquals(DecodeResult.Success(TestVectors.expectedMessage(vector)), result)
        } else {
            val expectedError = StatusCode.valueOf(vector.get("error").asString)
            assertEquals(DecodeResult.Failure(expectedError), result)
        }
    }

    @Test
    fun encodeMatchesVector() {
        assumeTrue(isValid && vector.get("roundtrip").asBoolean)

        val encoded = HarpCodec.encode(TestVectors.expectedMessage(vector))

        assertArrayEquals(TestVectors.bytes(vector), encoded)
    }
}
