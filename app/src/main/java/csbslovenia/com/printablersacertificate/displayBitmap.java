package csbslovenia.com.printablersacertificate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.print.PrintHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import csbslovenia.com.printablersacertificate.R;

public class displayBitmap extends Activity {
    // start the constructor
    BitmapCreator bitmapCreator = new BitmapCreator();

    // need this for Save, Print, Share
    static Bitmap bitmap;
    static File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Block screenshots
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_display_bitmap);

        // Data from previous document
        Intent previousIntent = getIntent();
        String data = previousIntent.getExtras().getString("data");             // Certificate or Private key
        String data2 = previousIntent.getExtras().getString("data2");           // Public key or " ";
        String type = previousIntent.getExtras().getString("type");             // pemCert or encryptedKeys

        String issuerName = previousIntent.getExtras().getString("issuerName");
        String subjectName = previousIntent.getExtras().getString("subjectName");
        String notBefore = previousIntent.getExtras().getString("notBefore");
        String notAfter = previousIntent.getExtras().getString("notAfter");

        // Create the bitmap with the Main QR code
        createDocument(data, type);

        // Add Other data from Strings and previous intent
        String bm_title;
        String bm_underTitle;

        if (type.equals("pemCert")) {
            // put title to bitmap
            bm_title = getResources().getString(R.string.bm_titleCert);
            // Put under title to bitmap
            bm_underTitle = getResources().getString(R.string.bm_underTitleCert);
            // Put key-only QR code to bitmap
            bitmapCreator.drawQRPublicKey(data2,115);

            // Under QR code
            bitmapCreator.drawText("Certified Public Key. Certificate",30,220,15);
            bitmapCreator.drawText("Public Key Only",145,160,14);

        } else if (type.equals("encryptedKeys")) {
            bm_title = getResources().getString(R.string.bm_titleKey);
            bm_underTitle = getResources().getString(R.string.bm_underTitleKey);
            bitmapCreator.drawText("Encrypted Private Key",78,205,15);
            bitmapCreator.drawText("! ! ! KEEP SECRET ! ! !",80,210,15);

        } else {
            return;
        }

        bitmapCreator.drawTitle(bm_title);
        bitmapCreator.drawUnderTitle(bm_underTitle);

        // get the bitmap from Bitmap creator
        bitmap = bitmapCreator.getBitmap();


        // From certificate:
        bitmapCreator.drawText("Issuer: "+issuerName,25,235,14);
        bitmapCreator.drawText("Subject Name: "+subjectName,25,240,14);
        bitmapCreator.drawText("Not valid before : "+notBefore,25,245,14);
        bitmapCreator.drawText("Not valid after: "+notAfter,25,250,14);

        // show the bitmap
        ImageView myImage = (ImageView) findViewById(R.id.result);
        myImage.setImageBitmap(bitmap);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_bitmap, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Building the Bitmap
    private void createDocument(String data, String type) {
        // Size of bitmap
        final int DPI = 300;                // you may change this
        final int A4PORTRAIT = 3510;    // dont change this size
        final int A4LANDSCAPE = 2481;   // dont change this

        int bitmapWidth = A4LANDSCAPE * DPI / 300;
        int bitmapHeight = A4PORTRAIT * DPI / 300;

        //  Initiate the BitmapCreator
        bitmapCreator.createBitmapCanvasPaint(bitmapWidth, bitmapHeight, getAssets());

        // Size of QR code:
        int qrWidth = bitmapWidth * 10 / 25;
        int qrHeight = qrWidth;

        // Position of QR code:
        int qrPosTop = milimetersToPixels(DPI, 115);         // in milimeters
        int qrPosLeft = bitmapWidth / 2 - qrWidth / 2;     // Centered in the middle

            // More to the left for pemCert
            if (type.equals("pemCert")) {
                qrWidth = milimetersToPixels(DPI,100);
                qrHeight = qrWidth;
                qrPosTop = milimetersToPixels(DPI, 115);
                qrPosLeft = milimetersToPixels(DPI, 25);
            }

        // Green background for QR code
        bitmapCreator.greenBackground(qrPosTop, qrPosLeft, qrHeight);

        // PutQRcode to bitmap
        bitmapCreator.putQRtoBitmap(data, qrPosTop, qrPosLeft, qrHeight);
    }

    // converts milimeters to Pixels, accordint to the DPI of this file
    private int milimetersToPixels(int DPI,int milimeters) {
        float pixels = (float) DPI / (float) 25.4 * (float) milimeters;             // 1 inch  = 2,54 cm = 25,4mm
        return (int) pixels;
    }

    // Toast message
    public void customToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    /**PRINT**/
    // Print the bitmap. Works great for printing with Photosmart, but not on saving to PDF or drive.
    public void printBitmap(MenuItem mi) {
        if (!PrintHelper.systemSupportsPrint()) {
            //Toast.makeText(this, "Printing from this phone not supporeted.", Toast.LENGTH_SHORT).show();
            customToast("Printing from this phone not suppored.");
            return;
        }
        PrintHelper photoPrinter = new PrintHelper(this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.setOrientation(PrintHelper.ORIENTATION_PORTRAIT);
        photoPrinter.setColorMode(PrintHelper.COLOR_MODE_COLOR);
        photoPrinter.printBitmap("PaperVault", bitmap);
    }

    /**SAVE PNG. Preferably to SD card**/
    public void saveBitmap(MenuItem mi) {
        customToast("Just a moment, please...");

        String folder = "/PaperVault/temp";
        if (mi.getItemId() == R.id.save) {
            folder = "/PaperVault";
        }

        // compress
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

        // store
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/PrintableRSA");
        dir.mkdirs();

        //This file is local
        File file = new File(dir, "Key_" +"_"+System.currentTimeMillis()+".png");

        try {
            file.createNewFile();
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.flush();
            fo.close();

            // Putting to gallery doesnt work
            // MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(), file.getName(), file.getName());

            customToast("Image stored to:\n"+file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**SHARE to other apps. Due to the large size of the Bitmap, the file must be saved first. Not okay for security...**/
    public void shareBitmap(MenuItem mi) {
        // Toasts will have to be put in a new Tread, since they appear too late.
        customToast("Just a moment, please.");

        // I intend to send a PNG image
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");

        // Compress the bitmap to PNG
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

        // Temporarily store the image to Flash
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/PrintableRSA/temp");
        dir.mkdirs();

        // This file is static - so I can delete it in the next method
        file = new File(dir, "temp.png");

        try {
            file.createNewFile();
            FileOutputStream fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.flush();
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Log.d("Florjan: dir and file getname", dir.getAbsolutePath() + "/" + file.getName());
        //Log.d("Florjan: Current", file.getPath());
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + file.getPath()));

        //startActivity(Intent.createChooser(share, "Share Image"));
        startActivityForResult(Intent.createChooser(share,"Share Image"),1);

        //Delete Temporary file:
        // I need a better solution for this. I tried not saving it to flash in the first place, but that made the app crash.
        // file.delete();        // deletes it too fast
        file.deleteOnExit();     // sometimes works???


    }

    /**Exit Button**/
    public void exit(MenuItem mi) {
        System.exit(0);
    }


}
