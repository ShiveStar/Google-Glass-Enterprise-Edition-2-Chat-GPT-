package com.example.chatgpt_ee2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.youtubescrape.GlassGestureDetector.Gesture;
import com.example.youtubescrape.GlassGestureDetector;
import com.example.youtubescrape.GlassGestureDetector.OnGestureListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import android.Manifest;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements OnGestureListener {
    private String apiKey = "";
    private String model = "";
    private GlassGestureDetector glassGestureDetector;
    private static final int REQUEST_CODE = 999;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private void extract_key(){
        try {
            // Open the file
            FileInputStream fis = new FileInputStream(new File("/sdcard/key.txt"));

            // Create a BufferedReader
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            // Read the file line by line
            String line;
            while ((line = reader.readLine()) != null) {
                boolean flag = line.contains("gpt");
                if(flag){
                    model = line;
                    Log.d("Model Selection:",model);
                }else{
                    apiKey = line;
                    Log.d("API KEY:",apiKey);
                }

            }

            // Close the BufferedReader and FileInputStream
            reader.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("App","SD ERROR");
        }
    }
    private Bitmap getImage(){
        Random rand = new Random();
        int imgNumber = rand.nextInt(14) + 1;  // Generate a random number between 1 and 14
        Bitmap originalImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("img" + 2, "drawable", getPackageName()));
        // Specify the new width and height
        int newWidth = 580+80;
        int newHeight = 874+80;
        // Resize the image
        Bitmap resizedImage = Bitmap.createScaledBitmap(originalImage, newWidth, newHeight, false);
        return resizedImage;
    }
    private class SendQuestionTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... questions) {
            String question = questions[0];
            OutputStream os = null;
            BufferedReader reader = null;
            try {
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setSSLSocketFactory(new com.example.james.glassgpt.Tls12SocketFactory());
                // Set the request method to POST
                conn.setRequestMethod("POST");

                // Set the request headers
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);

                // Enable input and output streams
                conn.setDoOutput(true);

                // Write the request body
                String requestBody = "{\"messages\":[{\"role\":\"system\",\"content\":\"You are a extremely concise assistant. No more than 75 characters per answer.\"},{\"role\":\"user\",\"content\":\"" + question + "\"}],\"max_tokens\":32, \"model\": \""+model+"\"}";

                os = conn.getOutputStream();
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);

                // Get the response code
                int responseCode = conn.getResponseCode();
                Log.d("CODE:",String.valueOf(responseCode));

                // Read the response
                InputStream stream;
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    stream = conn.getInputStream();
                } else {
                    stream = conn.getErrorStream();
                }
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                Log.d("RESPONSE:", response.toString());

                String responseString = response.toString();
                String searchString = "content";
                int startIndex = responseString.indexOf(searchString) + searchString.length()+3;
                int endIndex = responseString.indexOf("},", startIndex);
                String content = responseString.substring(startIndex, endIndex);
                Log.d("CONTENT:", content);
                response.append(content);
                // Return true if the request was successful, false otherwise
                return content;
            } catch (IOException e) {
                // Handle any IO exceptions that occur
                e.printStackTrace();
                return "NONE";
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Log the response
        @Override
        protected void onPostExecute(String result) {
            Log.d("RESPONSE:", result);
            setContentView(R.layout.activity_main);

            ImageView myImageView = (ImageView) findViewById(R.id.image_view);
            myImageView.setImageBitmap(getImage());
            TextView myTextView = (TextView) findViewById(R.id.text_view);
            myTextView.setText(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_menu);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user asynchronously
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            // Permission has already been granted
            extract_key();
        }


        glassGestureDetector = new GlassGestureDetector(this, this);
    }
    private void detectSpeech() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            final List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d("app", "results: " + results.toString());
            if (results != null && results.size() > 0 && !results.get(0).isEmpty()) {
                String speechResult = results.get(0);
                Log.d("app", speechResult);
                new SendQuestionTask().execute(speechResult);

            }
        } else {
            Log.d("app", "Result not OK");
            detectSpeech();
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return glassGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onGesture(Gesture gesture) {
        switch (gesture) {
            case SWIPE_DOWN:
                Log.d("App", "Swipe Down!");
                finish();
                return true;
            case SWIPE_UP:
                Log.d("App","Swipe up!");
                return true;
            case TAP:
                Log.d("App", "TAPPED!");
                detectSpeech();
                return true;
            case SWIPE_FORWARD:
                Log.d("App", "swipe forward");
                return true;
            case SWIPE_BACKWARD:
                Log.d("App", "swipe backward");
                return true;
            case TWO_FINGER_SWIPE_FORWARD:
                Log.d("App", "double forward");
                return true;
            case TWO_FINGER_SWIPE_BACKWARD:
                Log.d("App", "double backward");
                return true;
            default:
                return false;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}