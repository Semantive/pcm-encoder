# pcm-encoder
Simple PCM to MP4 encoder

PCMEncoder provides simple API for concatenating multiple PCM streams into one, encoded MP4 file.

Usage:
```java
PCMEncoder pcmEncoder = new PCMEncoder(16000, 11025, 1);
pcmEncoder.setOutputPath(outputPath);
pcmEncoder.prepare();
pcmEncoder.encode(inputStream);
pcmEncoder.stop();
```

The encode method can be called multiple times (see MainActivity for more details).
