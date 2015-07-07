# pcm-encoder
Simple PCM to MP4 encoder for Android

PCMEncoder provides simple API for concatenating multiple PCM streams into one, encoded MP4 file on Android devices. It uses MediaMuxer, MediaCodec and MediaFormat, which are available since Android API 18.

Usage:
```java
PCMEncoder pcmEncoder = new PCMEncoder(outputFileBitrate, outputFileSampleRate, outputFileNumberOfChannels);
pcmEncoder.setOutputPath(outputPath);
pcmEncoder.prepare();
pcmEncoder.encode(inputStream, inputStreamSampleRate);
pcmEncoder.stop();
```

The encode method can be called multiple times (see MainActivity for more details).
