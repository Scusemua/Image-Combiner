package com.example.fitnessscreenshotmerger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.fitnessscreenshotmerger.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    // One Button
    Button BSelectImage;


    ImageView IVPreviewImage;

    EditText ETWorkoutName;

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> oldSomeActivityResultLauncher;

    ActivityResultLauncher<Intent> someActivityResultLauncher;

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private void saveToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        // File directory = new File(getExternalFilesDir(null), "/combined_fitness_images");
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File screenshotsDir = new File(picturesDirectory, "screenshots");
        screenshotsDir.mkdirs();

        String workoutName = ETWorkoutName.getText().toString();
        String dateFormatted = dateFormat.format(new Timestamp(System.currentTimeMillis()));
        String fileName;

        if (workoutName.length() > 0) {
            fileName = workoutName + "-" + dateFormatted + ".png";
        } else {
            fileName = dateFormatted + ".png";
        }

        // Create imageDir
        File mypath=new File(screenshotsDir,fileName);

        FileOutputStream fos = null;
        try {
            System.out.println("Opening FileOutputStream for: \"" + mypath + "\"");
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            boolean res = bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);

            if (res) {
                System.out.println("Successfully wrote combined image.");
                MediaScannerConnection.scanFile(this,
                        new String[] { mypath.toString() }, null,
                        (path, uri) -> {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        });
            }
            else {
                System.out.println("Failed to write combined image.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // directory.getAbsolutePath();
    }

    public static Bitmap resize_old(Bitmap img, int newW, int newH) {
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale((float)newW / img.getWidth(), (float)newH / img.getHeight());

        return Bitmap.createScaledBitmap(img, newW, newH, false);
        //return Bitmap.createBitmap(img, 0, 0, newW, newH, matrix, false);
    }

    @SuppressLint("Range")
    public String getImageFilePath(Uri uri) {
        File file = new File(uri.getPath());
        String[] filePath = file.getPath().split(":");
        String image_id = filePath[filePath.length - 1];

        Cursor cursor = getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{image_id}, null);
        if (cursor!=null) {
            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            return imagePath;
        }

        return null;
    }

    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        System.out.println("Device Height: " + displayMetrics.heightPixels + " pixels, Device Width: " + displayMetrics.widthPixels + ", Combined: " + (displayMetrics.heightPixels + displayMetrics.widthPixels)  + " pixels.");
        
        // register the UI widgets with their appropriate IDs
        BSelectImage = findViewById(R.id.BSelectImage);
        IVPreviewImage = findViewById(R.id.IVPreviewImage);
        ETWorkoutName = findViewById(R.id.ETWorkoutName);
        // handle the Choose Image button to trigger
        // the image chooser function
        BSelectImage.setOnClickListener(v -> imageChooser());

//        oldSomeActivityResultLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    int heightPixels = -1;
//                    int widthPixels = -1;
//                    int combinedDimension = -1;
//
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        // There are no request codes
//                        Intent data = result.getData();
//                        ClipData clipData = data.getClipData();
//
//                        System.out.println("data: " + data.toString());
//
//                        if (clipData == null) {
//                            System.out.println("ERROR: No result from ClipData.");
//                            return;
//                        }
//
//                        List<Bitmap> images = new ArrayList<>();
//                        for (int i = 0; i < clipData.getItemCount(); i++) {
//                            ClipData.Item item = clipData.getItemAt(i);
//                            Uri imageUri = item.getUri();
//                            System.out.println("Decoding: " + imageUri);
//                            Bitmap img = null;
//                            try {
//                                img = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                            if (img == null) {
//                                System.out.println("ERROR: Could not decode image \"" + imageUri + "\"");
//                            }
//
//                            System.out.println("Image Height: " + img.getHeight() + ", Width: " + img.getWidth());
//                            images.add(img);
//
//                            if (i == 0) {
//                                int dim1 = img.getWidth();
//                                int dim2 = img.getHeight();
//
//                                if (dim1 > dim2) {
//                                    heightPixels = dim1;
//                                    widthPixels = dim2;
//                                } else {
//                                    heightPixels = dim2;
//                                    widthPixels = dim1;
//                                }
//
//                                combinedDimension = heightPixels + widthPixels;
//
//                                System.out.println("Current --> Height: " + heightPixels + ", Width: " + widthPixels + ", Combined: " + combinedDimension);
//                            } else {
//                                if((img.getHeight() != heightPixels && img.getHeight() != widthPixels) ||
//                                        (img.getWidth() != heightPixels && img.getWidth() != widthPixels)) {
//                                    System.out.println("Inconsistent dimensions!");
//
//                                    runOnUiThread(new Runnable() {
//                                        public void run() {
//                                            Toast errorToast = Toast.makeText(MainActivity.this, "Error: the images that you selected have inconsistent width and height dimensions.", Toast.LENGTH_SHORT);
//                                            errorToast.show();
//                                        }
//                                    });
//                                }
//                            }
//                        }
//
//                        System.out.println("Officially --> Height: " + heightPixels + ", Width: " + widthPixels + ", Combined: " + combinedDimension);
//
//                        Bitmap tallest = null;
//                        List<Bitmap> landscape = new ArrayList<>();
//                        List<Bitmap> vertical = new ArrayList<>();
//
//                        for (int i = 0; i < images.size(); i++) {
//                            Bitmap img = images.get(i);
//
//                            if (img.getWidth() == heightPixels) {
//                                System.out.println("Adding image " + img.get);
//                                landscape.add(img);
//                            }
//                            else if (img.getWidth() == widthPixels) {
//                                if (img.getHeight() == heightPixels) {
//                                    vertical.add(img);
//                                } else {
//                                    tallest = img;
//                                }
//                            }
//                        }
//
//                        if (tallest != null) {
//                            double aspectRatio = (double)tallest.getHeight() / (double)tallest.getWidth();
//                            int adjustedHeight = heightPixels;
//                            int adjustedWidth = (int)(adjustedHeight / aspectRatio);
//                            System.out.println("Tallest height: " + tallest.getHeight());
//                            Bitmap resized = resize(tallest, adjustedWidth, adjustedHeight);
//                            vertical.add(resized);
//                        }
//
//                        int totalWidth = 0;
//                        for (Bitmap img : vertical) {
//                            totalWidth += img.getWidth();
//                        }
//                        totalWidth += heightPixels; // For the landscape stacked.
//
//                        Bitmap combined = Bitmap.createBitmap(totalWidth, heightPixels, images.get(0).getConfig());
//                        Canvas canvas = new Canvas(combined);
//                        canvas.drawBitmap(vertical.get(0), 0, 0, null);
//                        canvas.drawBitmap(landscape.get(0), widthPixels, 0, null);
//                        canvas.drawBitmap(landscape.get(1), widthPixels, widthPixels, null);
//                        canvas.drawBitmap(vertical.get(1), combinedDimension, 0, null);
//
//                        System.out.println("combined Image Height: " + combined.getHeight() + ", combined Width: " + combined.getWidth());
//                        IVPreviewImage.setImageBitmap(combined);
//
//                        saveToInternalStorage(combined);
//                    }
//                });

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        assert data != null;
                        ClipData clipData = data.getClipData();

                        if (clipData == null) {
                            System.out.println("ERROR: No result from ClipData.");
                            return;
                        }

                        List<Bitmap> images = new ArrayList<>();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri imageUri = item.getUri();
                            System.out.println("Decoding: " + imageUri);
                            Bitmap img = null;
                            try {
                                img = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            if (img == null) {
                                System.out.println("ERROR: Could not decode image \"" + imageUri + "\"");
                            }

                            assert img != null;
                            System.out.println("Image Height: " + img.getHeight() + ", Width: " + img.getWidth());
                            images.add(img);
                        }

                        List<Bitmap> landscape = new ArrayList<>();
                        List<Bitmap> vertical = new ArrayList<>();
                        for (Bitmap img : images) {
                            if (img.getWidth() > img.getHeight()) {
                                System.out.println("Found horizontal/landscape image. Width: " + img.getWidth() + ", Height: " + img.getHeight());
                                landscape.add(img);
                            } else {
                                System.out.println("Found vertical/portrait image. Width: " + img.getWidth() + ", Height: " + img.getHeight());
                                vertical.add(img);
                            }
                        }

                        if (landscape.size() != 2) {
                            Toast errorToast = Toast.makeText(MainActivity.this, "Expected two landscape images. Instead, got " + landscape.size(), Toast.LENGTH_SHORT);
                            errorToast.show();
                            return;
                        }

                        if (vertical.size() != 2) {
                            Toast errorToast = Toast.makeText(MainActivity.this, "Expected two vertical images. Instead, got " + vertical.size(), Toast.LENGTH_SHORT);
                            errorToast.show();
                            return;
                        }

                        int totalHeight = landscape.get(0).getHeight() + landscape.get(1).getHeight();

                        for (int i = 0; i < vertical.size(); i++) {
                            Bitmap currentImage = vertical.get(i);

                            // Compute the aspect ratio of the tallest image.
                            double aspectRatio = (double)currentImage.getHeight() / (double)currentImage.getWidth();
                            // We'll resize the taller of the two images so that its height matches the shorter image.
                            // Maintain the same aspect ratio (for the tallest image) so it doesn't get skewed.
                            int adjustedWidth = (int)(totalHeight / aspectRatio);

                            System.out.println("Resizing vertical image #" + (i+1));
                            System.out.println("Height: " + currentImage.getHeight() + " --> " + totalHeight);
                            System.out.println("Width: " + currentImage.getWidth() + " --> " + adjustedWidth);

                            // Resize the taller image.
                            Bitmap resized = resize(currentImage, adjustedWidth, totalHeight);
                            // Replace the original image with the resized version.
                            vertical.set(i, resized);
                        }

                        int totalWidth = landscape.get(0).getWidth() + vertical.get(0).getWidth() +  vertical.get(1).getWidth();

                        int maxWidth = Math.max(landscape.get(0).getWidth(), landscape.get(1).getWidth());

                        Bitmap combined = Bitmap.createBitmap(totalWidth, totalHeight, images.get(0).getConfig());
                        Canvas canvas = new Canvas(combined);
                        canvas.drawBitmap(vertical.get(0), 0, 0, null);
                        canvas.drawBitmap(landscape.get(0), vertical.get(0).getWidth(), 0, null);
                        canvas.drawBitmap(landscape.get(1), vertical.get(0).getWidth(), landscape.get(0).getHeight(), null);
                        canvas.drawBitmap(vertical.get(1), maxWidth + vertical.get(0).getWidth(), 0, null);

                        IVPreviewImage.setImageBitmap(combined);

                        saveToInternalStorage(combined);
                    } else {
                        Toast errorToast = Toast.makeText(MainActivity.this, "Cannot create merged image, as activity had error result code: " + result.getResultCode(), Toast.LENGTH_SHORT);
                        errorToast.show();
                    }
                });
    }

    // this function is triggered when
    // the Select Image Button is clicked
    void imageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        someActivityResultLauncher.launch(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}