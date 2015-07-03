package com.semantive.pcm_encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author Anna Stępień <anna.stepien@semantive.com>
 * @since 02.07.15
 *
 * PCMEncoder allows encoding multiple input streams of PCM data into one, compressed audio file.
 */

public class PCMEncoder {

    private static final String TAG = "PCMEncoder";

    private static final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";

    private static final int CODEC_TIMEOUT = 5000;

    private int bitrate;
    private int sampleRate;
    private int channelCount;
    private int bufferSize;
    private int batchSize;

    private MediaFormat mediaFormat;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private ByteBuffer[] codecInputBuffers;
    private ByteBuffer[] codecOutputBuffers;
    private MediaCodec.BufferInfo bufferInfo;
    private String outputPath;
    private int audioTrackId;
    private int totalBytesRead;
    private double presentationTimeUs;

    public PCMEncoder(final int bitrate, final int sampleRate, int channelCount) {
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;

        this.bufferSize = 2 * sampleRate;
        this.batchSize = 20 * bufferSize;
    }

    public void setOutputPath(final String outputPath) {
        this.outputPath = outputPath;
    }

    public void prepare() {
        if (outputPath == null) {
            throw new IllegalStateException("The output path must be set first!");
        }
        try {
            mediaFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, sampleRate, channelCount);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

            mediaCodec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

            codecInputBuffers = mediaCodec.getInputBuffers();
            codecOutputBuffers = mediaCodec.getOutputBuffers();

            bufferInfo = new MediaCodec.BufferInfo();

            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            totalBytesRead = 0;
            presentationTimeUs = 0;
        } catch (IOException e) {
            Log.e(TAG, "Exception while initializing PCMEncoder", e);
        }
    }

    public void stop() {
        Log.d(TAG, "Stopping PCMEncoder");
        mediaCodec.stop();
        mediaCodec.release();
        mediaMuxer.stop();
        mediaMuxer.release();
    }

    public void encode(InputStream inputStream) throws IOException {
        Log.d(TAG, "Starting encoding of InputStream");
        byte[] tempBuffer = new byte[bufferSize];
        boolean hasMoreData = true;
        boolean stop = false;

        do {
            int inputBufferIndex = 0;
            int currentBatchRead = 0;
            while (inputBufferIndex != -1 && hasMoreData && currentBatchRead <= batchSize) {
                inputBufferIndex = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT);

                if (inputBufferIndex >= 0) {
                    ByteBuffer buffer = codecInputBuffers[inputBufferIndex];
                    buffer.clear();

                    int bytesRead = inputStream.read(tempBuffer, 0, buffer.limit());
                    if (bytesRead == -1) {
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, (long) presentationTimeUs, 0);
                        hasMoreData = false;
                        stop = true;
                    } else {
                        totalBytesRead += bytesRead;
                        currentBatchRead += bytesRead;
                        buffer.put(tempBuffer, 0, bytesRead);
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytesRead, (long) presentationTimeUs, 0);
                        presentationTimeUs = 1000000L * (totalBytesRead / 2) / sampleRate;
                    }
                }
            }

            int outputBufferIndex = 0;
            while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT);
                if (outputBufferIndex >= 0) {
                    ByteBuffer encodedData = codecOutputBuffers[outputBufferIndex];
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && bufferInfo.size != 0) {
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    } else {
                        mediaMuxer.writeSampleData(audioTrackId, codecOutputBuffers[outputBufferIndex], bufferInfo);
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mediaFormat = mediaCodec.getOutputFormat();
                    audioTrackId = mediaMuxer.addTrack(mediaFormat);
                    mediaMuxer.start();
                }
            }
        } while (!stop);

        inputStream.close();
        Log.d(TAG, "Finished encoding of InputStream");
    }
}