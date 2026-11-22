package io.github.yohann69.sing

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/** Streams the phone's mic to the Mac: 24 kHz mono 16-bit, 20 ms UDP packets prefixed with the session key and a sequence number. */
class PhoneMicSender {
    @Volatile private var running = false
    private var worker: Thread? = null

    val isRunning get() = running

    @SuppressLint("MissingPermission")
    fun start(host: String, port: Int, keyB64: String) {
        stop()
        val key = Base64.decode(keyB64, Base64.DEFAULT)
        running = true
        worker = thread(name = "phone-mic", priority = Thread.MAX_PRIORITY) {
            val rate = 24000
            val samples = 480
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, samples * 4))
            val socket = DatagramSocket()
            val addr = InetAddress.getByName(host)
            val pcm = ShortArray(samples)
            var seq = 0
            rec.startRecording()
            try {
                while (running) {
                    var read = 0
                    while (read < samples && running) read += rec.read(pcm, read, samples - read).coerceAtLeast(0)
                    val buf = ByteBuffer.allocate(12 + samples * 2)
                    buf.put(key, 0, 8)
                    buf.order(ByteOrder.BIG_ENDIAN).putInt(seq++)
                    buf.order(ByteOrder.LITTLE_ENDIAN)
                    for (s in pcm) buf.putShort(s)
                    socket.send(DatagramPacket(buf.array(), buf.position(), addr, port))
                }
            } finally {
                rec.stop(); rec.release(); socket.close()
            }
        }
    }

    fun stop() {
        running = false
        worker?.join(300)
        worker = null
    }
}
