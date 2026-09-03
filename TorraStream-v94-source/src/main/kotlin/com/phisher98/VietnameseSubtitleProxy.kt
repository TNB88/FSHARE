package com.phisher98

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

/** Serves legacy OpenSubtitles .gz files as plain SRT over device loopback. */
internal object VietnameseSubtitleProxy {
    private const val MAX_SUBTITLE_BYTES = 6 * 1024 * 1024
    private val sources = ConcurrentHashMap<String, String>()
    private val cache = ConcurrentHashMap<String, ByteArray>()
    private val workers = Executors.newCachedThreadPool()

    @Volatile
    private var port: Int = 0

    @Synchronized
    fun start(): Boolean {
        if (port > 0) return true
        return runCatching {
            val server = ServerSocket(0, 32, InetAddress.getByName("127.0.0.1"))
            port = server.localPort
            Thread {
                while (!server.isClosed) {
                    runCatching { server.accept() }.getOrNull()?.let { socket ->
                        workers.execute { handle(socket) }
                    }
                }
            }.apply {
                name = "TorraStreamVietnameseSubtitleProxy"
                isDaemon = true
                start()
            }
            true
        }.getOrDefault(false)
    }

    fun register(remoteUrl: String): String? {
        if (!remoteUrl.startsWith("https://") || !start()) return null
        val id = MessageDigest.getInstance("SHA-256")
            .digest(remoteUrl.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
        sources[id] = remoteUrl
        return "http://127.0.0.1:$port/subtitle/$id.srt"
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10_000
            val reader = client.getInputStream().bufferedReader(Charsets.US_ASCII)
            val requestLine = reader.readLine().orEmpty()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }
            val path = requestLine.split(' ').getOrNull(1).orEmpty()
            val id = Regex("/subtitle/([a-f0-9]{32})\\.srt").find(path)?.groupValues?.get(1)
            val source = id?.let(sources::get)
            if (id == null || source == null) {
                respond(client, 404, "text/plain; charset=utf-8", "Not found".toByteArray())
                return
            }
            val bytes = cache[id] ?: runCatching { downloadAndUnzip(source) }.getOrNull()
            if (bytes == null) {
                respond(client, 502, "text/plain; charset=utf-8", "Subtitle unavailable".toByteArray())
                return
            }
            cache[id] = bytes
            if (cache.size > 60) cache.keys.firstOrNull()?.let(cache::remove)
            respond(client, 200, "application/x-subrip; charset=utf-8", bytes)
        }
    }

    private fun downloadAndUnzip(source: String): ByteArray {
        val connection = URL(source).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "CloudStream TorraStream Viet")
            val input = if (
                source.endsWith(".gz", true) ||
                connection.contentType.orEmpty().contains("gzip", true)
            ) GZIPInputStream(connection.inputStream) else connection.inputStream
            input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_SUBTITLE_BYTES) error("Subtitle is too large")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun respond(socket: Socket, status: Int, contentType: String, body: ByteArray) {
        val reason = if (status == 200) "OK" else if (status == 404) "Not Found" else "Bad Gateway"
        socket.getOutputStream().use { output ->
            val headers = buildString {
                append("HTTP/1.1 $status $reason\r\n")
                append("Content-Type: $contentType\r\n")
                append("Content-Length: ${body.size}\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Cache-Control: private, max-age=3600\r\n")
                append("Connection: close\r\n\r\n")
            }.toByteArray(Charsets.US_ASCII)
            output.write(headers)
            output.write(body)
            output.flush()
        }
    }
}
