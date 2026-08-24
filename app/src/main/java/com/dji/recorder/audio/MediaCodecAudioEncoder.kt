package com.dji.recorder.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.AudioFormatType
import de.sciss.jump3r.mp3.BitStream
import de.sciss.jump3r.mp3.GainAnalysis
import de.sciss.jump3r.mp3.ID3Tag
import de.sciss.jump3r.mp3.Lame
import de.sciss.jump3r.mp3.LameGlobalFlags
import de.sciss.jump3r.mp3.MPEGMode
import de.sciss.jump3r.mp3.Parse
import de.sciss.jump3r.mp3.Presets
import de.sciss.jump3r.mp3.PsyModel
import de.sciss.jump3r.mp3.Quantize
import de.sciss.jump3r.mp3.QuantizePVT
import de.sciss.jump3r.mp3.Reservoir
import de.sciss.jump3r.mp3.Takehiro
import de.sciss.jump3r.mp3.VBRTag
import de.sciss.jump3r.mp3.Version
import de.sciss.jump3r.mpg.Common
import de.sciss.jump3r.mpg.Interface
import de.sciss.jump3r.mpg.MPGLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 专业音频编码引擎：
 * 1. MP3 格式：使用正统工业级 LAME MP3 编码器生成真实的 .mp3 文件。
 * 2. AAC 格式：使用硬件 MediaCodec 输出标准的 .m4a 文件。
 * 3. WAV 格式：原生无损直通。
 */
class MediaCodecAudioEncoder {

    private val TAG = "MediaCodecAudioEncoder"

    suspend fun encodePcmFile(
        pcmWavFile: File,
        config: AudioConfig,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        if (!pcmWavFile.exists() || pcmWavFile.length() <= 44) {
            return@withContext pcmWavFile
        }

        return@withContext when (config.format) {
            AudioFormatType.WAV -> pcmWavFile
            AudioFormatType.MP3 -> encodeToMp3Lame(pcmWavFile, config, onProgress)
            AudioFormatType.AAC -> encodeToAacM4a(pcmWavFile, config, onProgress)
        }
    }

    /**
     * 使用正统 LAME MP3 引擎将 PCM WAV 转换为高品质 .mp3
     * 关键修复：16位 PCM 采样必须左移 16 位 (shl 16) 映射到 LAME 32位幅度空间，确保音量饱满正常！
     */
    private fun encodeToMp3Lame(
        pcmWavFile: File,
        config: AudioConfig,
        onProgress: (Float) -> Unit
    ): File {
        val outputFile = File(
            pcmWavFile.parentFile,
            pcmWavFile.nameWithoutExtension + ".mp3"
        )

        val lame = Lame()
        val ga = GainAnalysis()
        val bs = BitStream()
        val p = Presets()
        val qupvt = QuantizePVT()
        val qu = Quantize()
        val vbr = VBRTag()
        val ver = Version()
        val id3 = ID3Tag()
        val rv = Reservoir()
        val tak = Takehiro()
        val parse = Parse()
        val mpg = MPGLib()
        val intf = Interface()
        val common = Common()
        val psy = PsyModel()

        // 完整绑定 LAME 各子模块网络拓扑
        lame.setModules(ga, bs, p, qupvt, qu, vbr, ver, id3, mpg)
        bs.setModules(ga, mpg, ver, vbr)
        p.setModules(lame)
        qupvt.setModules(tak, rv, psy)
        qu.setModules(bs, rv, qupvt, tak)
        vbr.setModules(lame, bs, ver)
        id3.setModules(bs, ver)
        rv.setModules(bs)
        tak.setModules(qupvt)
        parse.setModules(ver, id3, p)
        mpg.setModules(intf, common)
        intf.setModules(vbr, common)

        val gfp: LameGlobalFlags = lame.lame_init() ?: return pcmWavFile

        try {
            val sampleRate = config.sampleRate
            val channels = if (config.isStereo) 2 else 1
            val bitrate = config.bitrateKbps

            gfp.num_channels = channels
            gfp.in_samplerate = sampleRate
            gfp.out_samplerate = sampleRate
            gfp.brate = bitrate
            gfp.mode = if (channels == 1) MPEGMode.MONO else MPEGMode.STEREO
            gfp.quality = 2 // 高品质广播级
            id3.id3tag_init(gfp)
            gfp.write_id3tag_automatic = false
            gfp.findReplayGain = true

            val initRes = lame.lame_init_params(gfp)
            if (initRes < 0) {
                Log.e(TAG, "LAME init params failed with code: $initRes")
                lame.lame_close(gfp)
                return pcmWavFile
            }

            val totalBytes = pcmWavFile.length() - 44
            var bytesProcessed = 0L

            FileOutputStream(outputFile).use { fos ->
                FileInputStream(pcmWavFile).use { fis ->
                    fis.skip(44) // 跳过 WAV 头部

                    val chunkSize = 4096
                    val byteBuf = ByteArray(chunkSize)
                    val intBufL = IntArray(chunkSize / (2 * channels))
                    val intBufR = if (channels == 2) IntArray(chunkSize / (2 * channels)) else null
                    val mp3Buf = ByteArray((1.25 * (chunkSize / 2) + 7200).toInt())

                    var read: Int
                    while (fis.read(byteBuf).also { read = it } != -1) {
                        val numSamples = read / (2 * channels)
                        val bb = ByteBuffer.wrap(byteBuf, 0, read).order(ByteOrder.LITTLE_ENDIAN)

                        for (i in 0 until numSamples) {
                            // 关键：LAME 32-bit 整型编码器要求 16-bit 采样占据高 16 位 (shl 16)
                            intBufL[i] = bb.short.toInt() shl 16
                            if (channels == 2) {
                                intBufR?.set(i, bb.short.toInt() shl 16)
                            }
                        }

                        val encodedBytes = if (channels == 1) {
                            lame.lame_encode_buffer_int(gfp, intBufL, intBufL, numSamples, mp3Buf, 0, mp3Buf.size)
                        } else {
                            lame.lame_encode_buffer_int(gfp, intBufL, intBufR, numSamples, mp3Buf, 0, mp3Buf.size)
                        }

                        if (encodedBytes > 0) {
                            fos.write(mp3Buf, 0, encodedBytes)
                        }

                        bytesProcessed += read
                        onProgress((bytesProcessed.toFloat() / totalBytes) * 0.9f)
                    }

                    // 冲刷 LAME 缓冲区
                    val flushBytes = lame.lame_encode_flush(gfp, mp3Buf, 0, mp3Buf.size)
                    if (flushBytes > 0) {
                        fos.write(mp3Buf, 0, flushBytes)
                    }
                }
            }

            lame.lame_close(gfp)
            onProgress(1.0f)
            Log.i(TAG, "Genuine LAME MP3 encoded successfully with full volume: ${outputFile.name} (${outputFile.length()} bytes)")
            return outputFile

        } catch (e: Throwable) {
            Log.e(TAG, "Error encoding to genuine MP3", e)
            try { lame.lame_close(gfp) } catch (ignored: Exception) {}
            return pcmWavFile
        }
    }

    /**
     * 使用硬件 MediaCodec 编码为标准的 .m4a (AAC)
     */
    private fun encodeToAacM4a(
        pcmWavFile: File,
        config: AudioConfig,
        onProgress: (Float) -> Unit
    ): File {
        val outputFile = File(
            pcmWavFile.parentFile,
            pcmWavFile.nameWithoutExtension + ".m4a"
        )

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val channelCount = if (config.isStereo) 2 else 1
            val sampleRate = config.sampleRate
            val bitRate = config.bitrateKbps * 1000

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate,
                channelCount
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val totalBytes = pcmWavFile.length() - 44
            var bytesReadTotal = 0L

            FileInputStream(pcmWavFile).use { fis ->
                fis.skip(44)
                val inputChunk = ByteArray(4096)
                var isEos = false

                while (!isEos) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()

                        val bytesRead = fis.read(inputChunk)
                        if (bytesRead <= 0) {
                            codec.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isEos = true
                        } else {
                            inputBuffer?.put(inputChunk, 0, bytesRead)
                            val ptsUs = (bytesReadTotal * 1000000L) / (sampleRate * channelCount * 2)
                            codec.queueInputBuffer(inputBufferIndex, 0, bytesRead, ptsUs, 0)
                            bytesReadTotal += bytesRead
                            onProgress((bytesReadTotal.toFloat() / totalBytes) * 0.7f)
                        }
                    }

                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    while (outputBufferIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }

                            if (bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }

                        codec.releaseOutputBuffer(outputBufferIndex, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    }

                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = codec.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }

            var drainOutputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            while (drainOutputIndex >= 0) {
                val encodedData = codec.getOutputBuffer(drainOutputIndex)
                if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }
                codec.releaseOutputBuffer(drainOutputIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                drainOutputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            }

            onProgress(1.0f)
            Log.i(TAG, "Encoded M4A file created successfully: ${outputFile.name}")
            return outputFile

        } catch (e: Exception) {
            Log.e(TAG, "Error encoding to M4A", e)
            return pcmWavFile
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing codec", e)
            }
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing muxer", e)
            }
        }
    }
}
