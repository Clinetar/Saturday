package com.AI.clinetar.Saturday

import com.AI.clinetar.Saturday.data.MessagesResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MessagesResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `extracts text from a plain reply`() {
        val body = """
            {"id":"msg_1","role":"assistant","model":"claude-opus-5",
             "content":[{"type":"text","text":"Hello there."}],
             "stop_reason":"end_turn"}
        """.trimIndent()

        val parsed = json.decodeFromString(MessagesResponse.serializer(), body)
        assertEquals("Hello there.", parsed.text())
    }

    @Test
    fun `ignores non-text blocks such as thinking`() {
        val body = """
            {"content":[
              {"type":"thinking","thinking":"...", "text":null},
              {"type":"text","text":"Final answer."}
            ]}
        """.trimIndent()

        val parsed = json.decodeFromString(MessagesResponse.serializer(), body)
        assertEquals("Final answer.", parsed.text())
    }
}
