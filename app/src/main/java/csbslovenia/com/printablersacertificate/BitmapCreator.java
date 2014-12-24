package csbslovenia.com.printablersacertificate;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Cveto on 23.8.2014.
 */
public class BitmapCreator {
    // for file deletion
    static File file;

    // For saving to Flash
    static String st_title = null;

    // A4 document: 300DPI
    int bitmapWidth;
    int bitmapHeight;

    // Bitmap, Canvas, Paint
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    //Assets (so the fonts work outside of an activity)
    AssetManager asset;

    // Create size of bitmap, new canvas new paint assets
    public void createBitmapCanvasPaint(int bitmapWidth, int bitmapHeight, AssetManager asset) {
        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;

        this.bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(bitmap);
        this.paint = new Paint();

        //asset
        this.asset = asset;

        //background
        paint.setColor(Color.WHITE);
        canvas.drawRect(0,0,bitmapWidth,bitmapHeight,paint);
    }

    // Puts background around QR code
    public void greenBackground(int top, int left, int size) {
        int qrWidth = size;
        int qrHeight = size;
        int qrLineWidth = this.bitmapWidth/140;


        // Portrat A5 orientation dimensions of halved landscape A4
        int qrPosTop = top;
        int qrPosBottom = top + size;
        int qrPosLeft = left;
        int qrPosRight = left + size;

        paint.setColor(Color.GREEN);
        canvas.drawRect(qrPosLeft - qrLineWidth, qrPosTop - qrLineWidth, qrPosRight + qrLineWidth, qrPosBottom + qrLineWidth, paint);
    }

    // Returns Bitmap from this class
    public Bitmap getBitmap() {
        return this.bitmap;
    }

    // Takes text, creates QR code and puts it on the bitmap.
    public void putQRtoBitmap(String data, int top, int left, int size) {

        int qrPosLeft = left;
        int qrPosTop = top;
        int qrWidth = size;
        int qrHeight = size;

        int[] pixels = getQRpixels(data, qrWidth, qrHeight, "UTF-8");
        bitmap.setPixels(pixels, 0, qrWidth, qrPosLeft, qrPosTop, qrWidth, qrHeight);
    }

    // Same as above, but customized
    public void drawQRPublicKey(String data, int top) {
        // Size
        int qrWidth = milimetersToPixels(40);
        int qrHeight = qrWidth;

        // Position
        int qrPosTop = milimetersToPixels(top);
        int qrPosLeft = bitmapWidth-qrWidth-milimetersToPixels(25);

        greenBackground(qrPosTop,qrPosLeft,qrWidth);

        int[] pixels = getQRpixels(data, qrWidth, qrHeight, "UTF-8");
        bitmap.setPixels(pixels, 0, qrWidth, qrPosLeft, qrPosTop, qrWidth, qrHeight);
    }

    // Not used anymore. I think
    public void drawLine() {
        int width = this.bitmapWidth / 300;

        int startx = bitmapWidth - width/2;
        int stopx = bitmapWidth + width/2;
        int starty = 0;
        int stopy = this.bitmapHeight;

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        canvas.drawRect(startx,starty,stopx,stopy,paint);
    }

    /** ZXING magic**/
    public int[] getQRpixels(String input, int width, int height, String encoding) {
        //Size of pixels array
        int[] pixels = new int[width * height];

        // set QR background and pixel color
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;

        Map<EncodeHintType, Object> hints = null;

        hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, encoding);

        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix result = writer.encode(input, BarcodeFormat.QR_CODE, width, width, hints);

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return pixels;
    }

    /** Placing text **/
    public void drawText(String text,Integer left, Integer top, Integer textSize) {
        left = milimetersToPixels(left);
        top = milimetersToPixels(top);
        textSize = (int) fontPoints(textSize);

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);

        Typeface tf = Typeface.createFromAsset(asset, "fonts/URANIA_CZECH.ttf");   //Typeface tf = Typeface.create("monospace",Typeface.BOLD);
        paint.setTypeface(tf);

        canvas.drawText(text,left,top, paint);
    }

    public void drawTitle(String text) {
        Integer left = milimetersToPixels(25);
        Integer top = bitmapHeight / 100 * 10;
        int textSize = (int) fontPoints(28);

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);

        Typeface tf = Typeface.createFromAsset(asset, "fonts/URANIA_CZECH.ttf");   //Typeface tf = Typeface.create("monospace",Typeface.BOLD);
        paint.setTypeface(tf);

        canvas.drawText(text,left,top, paint);
    }

    public void drawUnderTitle(String text) {
        Integer top = bitmapHeight / 100 * 15;
        Integer left = milimetersToPixels(25);
        Integer right = bitmapWidth - left;

        int textSize = (int) fontPoints(12);
        float spacing = (float) 1.1;

        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);

        Typeface tf = Typeface.createFromAsset(asset, "fonts/URANIA_CZECH.ttf");   //Typeface tf = Typeface.create("monospace",Typeface.BOLD);
        paint.setTypeface(tf);

        drawJustifiedText(text,textSize,spacing,left,right,top,false);
    }

    // Transforms points to pixels - because we are all used to Msword text sizes
    private float fontPoints(int points) {
           float bitmapHeightmm = 297;
           float sizeInches = (float)points / 72;
           float sizeMilimeters = sizeInches * 254 / 10;
           return sizeMilimeters * bitmapHeight / bitmapHeightmm;
       }

    // For positioning on the document with linerly.
    private int milimetersToPixels(int milimeters) {
        float pixels = (float) bitmapHeight / (float) 297 * (float) milimeters;
        return (int) pixels;
    }

    /**Draw Justified Text**/
    private float drawJustifiedText(
            String text,
            int textSize,
            float textSpacing,
            int xLeft,
            int xRight,
            int yTop,
            Boolean justifyShortLine) {


        // Starting conditions
        String text_line = "";
        String text_rest = text;

        // Top part of text starts there, not bottom.
        yTop = yTop+textSize*3/4;

        //  How many fit?
        int maxTextOnLine = paint.breakText(text_rest, 0, text_rest.length(), true, xRight - xLeft, null);

        // Counter for y position
        int i = 0;

        /**For text that doesn't fit in one line**/
        if (text_rest.length() > maxTextOnLine+1) {         //why + 1? It was putting half lines on new line. needs testing
            do {
                //How many characters fit on the line?
                maxTextOnLine = paint.breakText(text_rest, 0, text_rest.length(), true, xRight - xLeft, null);

                // Find where to remove partial word.
                while (text_rest.charAt(maxTextOnLine-1) != " ".charAt(0)) {
                    maxTextOnLine--;
                }

                // Implement delimiter for new line /n
                // Yet to be implemented

                // Split string on two strings - Remove partial word.
                text_line = text_rest.substring(0, maxTextOnLine);
                text_rest = text_rest.substring(maxTextOnLine, text_rest.length());

                // Draw jutified Text
                justifyCalculation(text_line,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i);

                // increment for next line
                i++;
            }  while (text_rest.length() > text_line.length());
        }

        /** For Last or Only line. **/
        text_line = text_rest;
        if (justifyShortLine) {
            justifyCalculation(text_line,xLeft,xRight,yTop,textSize,textSpacing,paint,canvas,i);
        } else {
            // No justification
            canvas.drawText(text_line, 0, text_line.length(), xLeft, yTop + i * textSize * textSpacing, paint);
        }
        return yTop + i * textSize * textSpacing;
    }
    private void justifyCalculation(String text_line,int xLeft,int xRight,int yTop,int textSize,float textSpacing,Paint paint, Canvas canvas, int i) {
        // Create array of words
        String[] words = createArrayOfWords(text_line);
        int numberOfWords = words.length;
        int numberOfSpaces = numberOfWords-1;

        // Length of the words without spaces;
        float lenWordsNoSpaces = measureLenOfStringsInStringArray(words,paint);

        // How much whitespace is there to fill?
        float emtpySpaceInLine = xRight-xLeft-lenWordsNoSpaces;

        // How many per space
        float emptySpacePerSpace = emtpySpaceInLine/numberOfSpaces;

        // Display data
        float cumulativeLength = 0;
        for (int j=0;j<numberOfWords;j++) {
            canvas.drawText(words[j],xLeft + cumulativeLength,yTop + i * textSize * textSpacing,paint);
            cumulativeLength += paint.measureText(words[j]) + emptySpacePerSpace;
        }

    }
    private String[] createArrayOfWords(String string) {
        String[] words = string.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].replaceAll(" ", "");
        }
        return words;
    }
    private float measureLenOfStringsInStringArray(String[] stringArray, Paint paint) {
        float num = 0;
        for (int i = 0; i < stringArray.length; i++) {
            num += paint.measureText(stringArray[i]);
        }
        return num;
    }
}