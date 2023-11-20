package com.example.myapplication;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import android.os.Environment;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.nio.charset.StandardCharsets;
import android.util.Log;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONObject;



public class Gpt extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final String TAG = "MainActivity";
    private OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Uri pdfUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewAnswer = findViewById(R.id.textViewAnswer); // Initialize TextView for the answer

        Button buttonUploadPdf = findViewById(R.id.buttonUploadPdf);
        buttonUploadPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
        });

        Button buttonAsk = findViewById(R.id.buttonAsk);
        buttonAsk.setOnClickListener(v -> {
            EditText questionInput = findViewById(R.id.editTextQuestion);
            String question = questionInput.getText().toString().trim(); // Trim to remove any leading or trailing whitespace
            if (!question.isEmpty()) {
                // This is where you should process the question to get an answer
                // For now, we're using a placeholder method
                //String answer = processQuestionWithLLM(question);
                // Write the answer to a file
                generateAnswer(question); // Call generateAnswer instead of processQuestionWithLLM
            } else {
                Toast.makeText(MainActivity.this, "Please enter a question.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                pdfUri = data.getData();
                Cursor cursor = null;
                String displayName = null;

                try {
                    cursor = getContentResolver().query(pdfUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex != -1) {
                            displayName = cursor.getString(columnIndex);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                TextView selectedFileName = findViewById(R.id.textViewSelectedFile);
                if (displayName != null) {
                    selectedFileName.setText(displayName);
                    selectedFileName.setVisibility(View.VISIBLE); // Make the TextView visible
                }
            }
        }
    }
    private TextView textViewAnswer;


    private void generateAnswer(String question) {
        Log.d(TAG, "question:"+question);
        String extractedText = extractTextFromPdf(pdfUri);
        // Simulate getting an answer. Replace this with your actual method to get an answer.
        String answer = processQuestionWithLLM(question, extractedText);

        // Update the TextView on the main thread
        //runOnUiThread(() -> textViewAnswer.setText(answer));

    }


    private void writeAnswerToFile(String question, String answer) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "answer.txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        // Check if the device is running on API 29 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        }

        Uri uri;
        // Use the appropriate URI based on the API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        } else {
            // For API level 28 and below, use MediaStore.Files.getContentUri
            uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        }

        if (uri == null) {
            Toast.makeText(this, "Failed to create new MediaStore record.", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            os.write(("Question: " + question + "\n").getBytes());
            os.write(("Answer: " + answer).getBytes());
            Toast.makeText(this, "Answer saved to the Downloads directory", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save the answer", Toast.LENGTH_SHORT).show();
        }
    }


    private String processQuestionWithLLM(String question, String pdfText) {
        // Placeholder for the API URL you would be using.
        String OPENAI_URL = "https://api.openai.com/v1/engines/text-davinci-003/completions";

        // Placeholder for the API key.
        String OPENAI_API_KEY = "*****";
        //String data = "{\"prompt\":\"" + question + " \\\\n " + pdfText + "\",\"max_tokens\":150}";
        String data = question;// + "\n search above question in below text \n" + pdfText;

        //Log.d(TAG, "data:"+data);
        try {
            String json = "{\"prompt\":\"" + data + "\",\"max_tokens\":10}";
            Log.d(TAG, "json:"+json);
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(OPENAI_URL)
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error connecting to OpenAI", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        Log.d(TAG, "response2:"+responseData);
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            String outputText = jsonObject.getJSONArray("choices").getJSONObject(0).getString("text");
                            runOnUiThread(() -> textViewAnswer.setText(outputText.trim()));
                            Log.d(TAG, "response:"+outputText.trim());
                            writeAnswerToFile(question, outputText.trim());
                            //return outputText.trim();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error parsing response", Toast.LENGTH_SHORT).show());
                        }

                    } else {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred: " + e.getMessage();
        }
        return "";
    }

    private String processQuestionWithLLM1(String question, String pdfText) {
        // Placeholder for the API URL you would be using.
        String apiUrl = "https://api.openai.com/v1/engines/davinci-codex/completions";

        // Placeholder for the API key.
        String apiKey = "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";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String data = "{\"prompt\":\"" + question + " \\\\n " + pdfText + "\",\"max_tokens\":150}";
            Log.d(TAG, "data:"+data);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Parse the JSON response to extract the answer.
            // The structure of the response JSON will depend on the API's response format.
            // You'll need to parse the JSON accordingly to extract the "choices" or "text" field.

            // This is a placeholder return value.
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred: " + e.getMessage();
        }
        // Set up the request body with the question.
        // This is a complex task that requires proper error handling, threading, and API response parsing.
        //return "Simulated answer for: " + question;
    }


    private String extractTextFromPdf(Uri pdfUri) {
        PdfReader reader = null;
        try {
            InputStream is = getContentResolver().openInputStream(pdfUri);
            reader = new PdfReader(is);
            StringBuilder text = new StringBuilder();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                text.append(PdfTextExtractor.getTextFromPage(reader, i)).append("\n");
            }
            return text.toString();
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }


}
