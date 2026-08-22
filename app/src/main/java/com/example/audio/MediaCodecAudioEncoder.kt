package com.example.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MediaCodecAudioEncoder(
    private val sampleRate: Int = 16_000,
    private val channelCount: Int = 1,
    private val bitrate: Int = 64_000
) : AudioEncoder {

    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private var presentationTimeUs = 0L
    private val bufferInfo = MediaCodec.BufferInfo()
    private val timeoutUs = 10_000L

    companion object {
        private const val TAG = "MediaCodecAudioEncoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
    }

    override fun start(outputFile: File) {
        try {
            val format = MediaFormat.createAudioFormat(MIME_TYPE, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            trackIndex = -1
            muxerStarted = false
            presentationTimeUs = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaCodec encoder", e)
            throw e
        }
    }

    override fun encode(pcmData: ShortArray, samplesRead: Int) {
        val encoder = codec ?: return
        if (samplesRead <= 0) return

        val pcmBytes = ByteArray(samplesRead * 2)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData, 0, samplesRead)

        var offset = 0
        while (offset < pcmBytes.size) {
            val inputBufferIndex = encoder.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputBufferIndex) ?: continue
                inputBuffer.clear()
                val chunkSize = Math.min(pcmBytes.size - offset, inputBuffer.remaining())
                inputBuffer.put(pcmBytes, offset, chunkSize)
                offset += chunkSize

                val pts = presentationTimeUs
                encoder.queueInputBuffer(inputBufferIndex, 0, chunkSize, pts, 0)

                val samplesEncoded = chunkSize / (2 * channelCount)
                presentationTimeUs += (samplesEncoded * 1_000_000L) / sampleRate
            }

            drainEncoder(false)
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val encoder = codec ?: return
        val currentMuxer = muxer ?: return

        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    Log.w(TAG, "Format changed twice")
                } else {
                    val newFormat = encoder.outputFormat
                    trackIndex = currentMuxer.addTrack(newFormat)
                    currentMuxer.start()
                    muxerStarted = true
                }
            } else if (outputBufferIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        currentMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(outputBufferIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    override fun finish() {
        try {
            val encoder = codec ?: return
            val inputBufferIndex = encoder.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex >= 0) {
                encoder.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing encoder stream", e)
        }
    }

    override fun close() {
        try {
            codec?.stop()
        } catch (_: Exception) {}
        try {
            codec?.release()
        } catch (_: Exception) {}
        codec = null

        try {
            if (muxerStarted) {
                muxer?.stop()
            }
        } catch (_: Exception) {}
        try {
            muxer?.release()
        } catch (_: Exception) {}
        muxer = null
        muxerStarted = false
    }
}
