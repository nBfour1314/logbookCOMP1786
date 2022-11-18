package com.example.gallerydemo;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.BreakIterator;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button btnNext, btnPrevious, btnAdd, btnReset, btnOpenCam;
    ArrayList<String> imageURLs = new ArrayList<>();
    TextView itemLabel;
    EditText txtURL;
    ImageView myImageView;
    private int currentIndex = 0;
    private static final String FILE_NAME = "myURLs.txt";
    private int CAPTURE_CODE = 1;
    Uri uriImg;
    MaterialAlertDialogBuilder alertDialog;
    private String displayMessage, URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        // retrieve all ui elements on the form
        findAllElements();

        try {
            if (!imageURLs.isEmpty()){
                loadURLs();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            displayMessage("Read file error, file = " + FILE_NAME);
        }

        setImage();

        whenClickNext();

        whenClickPrevious();

        whenClickAdd();

        whenClickReset();

        whenClickTakePhoto();
    }

    //click TAKE PHOTO button
    private void whenClickTakePhoto() {
        btnOpenCam.setOnClickListener(v -> {
            ContentValues values = new ContentValues();
            long timestamp = System.currentTimeMillis();

            values.put(MediaStore.Images.Media.DISPLAY_NAME, timestamp);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            uriImg = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cam.putExtra(MediaStore.EXTRA_OUTPUT, uriImg);
            startActivityForResult(cam, CAPTURE_CODE);
        });
    }

    //transmit CAPTURE CODE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAPTURE_CODE){
            String path = getPathFromURI(uriImg);
            File file = new File(path);
            if(file.exists()){
                Glide.with(this).load(path).into(myImageView);
                dialogShowSavePicture();
                imageURLs.add(path);
                try {
                    saveToFile(path);
                    currentIndex = imageURLs.size() - 1;
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //get path from uri
    public String getPathFromURI(Uri uriImg) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uriImg, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    //when click RESET DATA button
    private void whenClickReset() {
        btnReset.setOnClickListener(v -> {
            dialogShowResetData();
        });
    }

    //display dialog when click RESET DATA button
    private void dialogShowResetData(){
        alertDialog = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents);
        alertDialog.setTitle("Reset");
        alertDialog.setMessage("Do you want to reset all data?");
        alertDialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(getString(R.string.reset), (dialogInterface, i) ->{
                    imageURLs.clear();
                    myImageView.setImageResource(0);
                    removeFile();
                    currentIndex = 0;

                    alertDialog = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents);
                    alertDialog.setTitle("Notification");
                    alertDialog.setMessage("Data reset successfully!");
                    alertDialog.setPositiveButton(R.string.ok, (dInterface, x) -> {
                        dialogInterface.dismiss();
                        itemLabel.setText("");
                    });
                    alertDialog.create();
                    alertDialog.show();
                });
        alertDialog.create();
        alertDialog.show();
    }

    //display dialog when click CHECK button
    private void dialogShowSavePicture(){
        alertDialog = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents);
        alertDialog.setTitle("Notification");
        alertDialog.setMessage("The photo has been saved to the device!");
        alertDialog.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            itemLabel.setText("Current index =" + currentIndex);
        });
        alertDialog.create();
        alertDialog.show();
    }

    //remove file
    private void removeFile() {
        getApplicationContext().deleteFile(FILE_NAME);
    }

    //click ADD button
    private void whenClickAdd() {
        btnAdd.setOnClickListener(v -> {
            URL = txtURL.getText().toString().trim();

            if((URL.contains("http://") || URL.contains("https://")) && (URL.length() >= 20) && (URL.contains(".png")|| URL.contains(".jpg") || URL.contains(".jpeg"))) {
                imageURLs.add(URL);

                try {
                    saveToFile(URL);
                    displayMessage = "URL added successfully!";
                } catch (IOException e) {
                    e.printStackTrace();
                    displayMessage = "Save file error!";
                }
            }
            else if (URL.isEmpty()) {
                displayMessage = "Please enter a URL!";
            }
            else if (URL.length() < 20) {
                displayMessage ="Link does not exist!";
            }
            else{
                displayMessage = "Invalid link format!";
            }
            displayDialogAdd(displayMessage);
        });
    }

    //display dialog when click ADD button
    private void displayDialogAdd(String displayMessage) {
        alertDialog = new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents);
        alertDialog.setTitle("Notification");
        alertDialog.setMessage(displayMessage);
        alertDialog.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            EditText url = findViewById(R.id.txtURL);
            url.setText("");
        });
        alertDialog.create();
        alertDialog.show();
    }

    //display message
    private void displayMessage(String message) {
        itemLabel.setText(message);
    }

    //save data into the file (myURLs)
    private void saveToFile(String url) throws IOException {
        FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(FILE_NAME, Context.MODE_APPEND);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        bufferedWriter.write(url);
        bufferedWriter.newLine();
        bufferedWriter.flush();
        bufferedWriter.close();
        outputStreamWriter.close();
    }

    //load data from the file (myURL)
    private void loadURLs() throws IOException {
        FileInputStream fileInputStream = getApplicationContext().openFileInput(FILE_NAME);
        if (fileInputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String lineData = bufferedReader.readLine();
            while (lineData != null) {
                imageURLs.add(lineData);
                lineData = bufferedReader.readLine();
            }
        }
    }

    //display image
    //load URL into ImageView by using Glide
    private void setImage() {
        int size = imageURLs.size();
        if (currentIndex >= size) {
            currentIndex = 0;
        } else if (currentIndex < 0) {
            currentIndex = size - 1;
        }
        if (size > 0) {
            Glide.with(this).load(imageURLs.get(currentIndex)).into(myImageView);
        }
    }

    //click Prev button
    private void whenClickPrevious() {
        btnPrevious.setOnClickListener(v -> {
            if(!imageURLs.isEmpty()){
                currentIndex--;
                setImage();
                displayMessage("Current index = " + currentIndex);
            }
        });
    }

    //click NEXT button
    private void whenClickNext() {
        btnNext.setOnClickListener(v -> {
            if(!imageURLs.isEmpty()){
                currentIndex++;
                setImage();
                displayMessage("Current index = " + currentIndex);
            }
        });
    }

    //get data from widget
    private void findAllElements() {
        myImageView = findViewById(R.id.imageViewFromURL);
        btnNext = findViewById(R.id.buttonNext);
        btnPrevious = findViewById(R.id.buttonPrevious);
        btnAdd = findViewById(R.id.buttonAdd);
        btnReset = findViewById(R.id.buttonReset);
        txtURL = findViewById(R.id.txtURL);
        itemLabel = findViewById(R.id.itemLabel);
        btnOpenCam = findViewById(R.id.buttonTakePhoto);
    }

}