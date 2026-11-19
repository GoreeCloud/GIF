package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class OpenverseClientTest {
    @Test fun parsesOpenLicensedGifOnly() {
        val json = """{"results":[
          {"id":"12345678","title":"Purring cat","url":"https://example.org/a.gif","thumbnail":"https://example.org/thumb/","filetype":"gif","license":"by","license_version":"4.0","attribution":"Cat by Example","foreign_landing_url":"https://example.org/cat","mature":false},
          {"id":"12345679","title":"Unsafe","url":"http://example.org/b.gif","filetype":"gif"},
          {"id":"12345670","title":"Not GIF","url":"https://example.org/c.png","filetype":"png"},
          {"id":"12345671","title":"Mature","url":"https://example.org/d.gif","filetype":"gif","mature":true}
        ]}"""
        val items = OpenverseClient.parseResults(json)
        assertEquals(1, items.size)
        assertEquals("Purring cat", items[0].title)
        assertEquals("BY 4.0", items[0].license)
        assertEquals("image/gif", items[0].mimeType)
    }
}
