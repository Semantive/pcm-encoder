package com.semantive.pcm_encoder;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        Button single = (Button) findViewById(R.id.single);
        single.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File tmpFile = File.createTempFile("single_" + System.currentTimeMillis(), ".m4a", getExternalCacheDir());
                    encodeSingleFile(tmpFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Exception while creating tmp file", e);
                }
            }
        });

        Button multiple = (Button) findViewById(R.id.multiple);
        multiple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File tmpFile = File.createTempFile("multiple_" + System.currentTimeMillis(), ".m4a", getExternalCacheDir());
                    encodeMultipleFiles(tmpFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Exception while creating tmp file", e);
                }
            }
        });
    }

    private void encodeSingleFile(final String outputPath) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(encodeTask(1, outputPath));
    }

    private void encodeMultipleFiles(final String outputPath) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(encodeTask(10, outputPath));
    }

    private Runnable encodeTask(final int numFiles, final String outputPath) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    final PCMEncoder pcmEncoder = new PCMEncoder(16000, 11025, 1);
                    pcmEncoder.setOutputPath(outputPath);
                    pcmEncoder.prepare();
                    for (int i = 0; i < numFiles; i++) {
                        Log.d(TAG, "Encoding: " + i);
                        InputStream inputStream = getAssets().open("test.wav");
                        inputStream.skip(44);
                        pcmEncoder.encode(inputStream, 11025);
                    }
                    pcmEncoder.stop();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Encoded file to: " + outputPath, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "Cannot create FileInputStream", e);
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
