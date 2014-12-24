package csbslovenia.com.printablersacertificate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;

public class Main extends Activity {
    private static final String TAG = "Main Activity";
    private static String certificatePath;
    private static InputStream certFile;
    private static X509Certificate x509certificate;
    //String deletePassword = "prasicaklatijenujnovsakozimo";           // for testing only
    String deletePassword = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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


    /**My code**/
    private void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    public void openFileExplorer(View view) {
        int requestCode = 1;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");          // what files to choose from (all)
        startActivityForResult(intent,requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            // It was from File Picker
            case 1: {
                    if (resultCode == RESULT_CANCELED) {
                     // action cancelled
                     } else if (resultCode == RESULT_OK) {
                        // get Data
                        Uri uri = data.getData();

                        // find TextView
                        TextView textView = (TextView) findViewById(R.id.tv_foundCert);
                        // check if it is valid (for now only with extentions .p12 or .pfx)
                        Boolean valid = checkExtentions(uri.getPath());

                        if (valid) {
                            // Set certificate Path
                            setCertificatePath(uri.getPath());

                            // Show Dir
                            textView.setText(certificatePath);

                            // Unblock  password field
                            TextView etPassword = (TextView)findViewById(R.id.et_password);
                            etPassword.setEnabled(true);
                            etPassword.setText(deletePassword); ///////////////////////////////////////////////////////////////////////////////////////////////////////////

                            // Unblock  buttons
                            TextView but_get509cert = (TextView)findViewById(R.id.but_get509cert);
                            but_get509cert.setEnabled(true);

                            TextView but_getKey = (TextView)findViewById(R.id.but_getKey);
                            but_getKey.setEnabled(true);
                        } else {
                            textView.setText("I only accept p12 and pfx certificates.");
                            return;
                        }
                     }
                break;
            }
        }
    }

    // Check if file has valid extentions (dont just let any file come int)
    private Boolean checkExtentions(String path) {
        try {
            String extention = path.substring((path.lastIndexOf(".") + 1), path.length());
            if (extention.equals("pfx") || extention.equals("p12")) {
                return true;
            }
            else {
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Can not find extention in this file");
        }
        return false;
    }

    // OnClick event for getting the public key
    public void get509Cert(View view) {
        // Was set with the file manager
        File cert = new File(certificatePath);

        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(cert));
            certFile = in;
            createCertBitmap();
            in.close();
            Log.d(TAG,"Certificate loaded sucsessfully!");
        } catch (Exception e) {
            Log.d(TAG, "get509Cert. Could not load the file. Does it exist in " + cert.getAbsolutePath() + "?");
            customToast("Creation unsuccessful.");

        }
    }

    // OnClick event for getting the private key
    public void getKey(View view) {
        // Was set with the file manager
        File cert = new File(certificatePath);
        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(cert));
            certFile = in;
            createKeyBitmap();
            in.close();
            Log.d(TAG,"Certificate loaded sucsessfully!");
        } catch (Exception e) {
            Log.d(TAG, "Certificate file does not seem to be valid.");
            customToast("Certificate file does not seem to be valid");
        }
    }

    // File manager - if not present.
    public void installESFileManager(View view) {
        Uri marketUri = Uri.parse("market://details?id=com.estrongs.android.pop");
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        try {
            startActivity(marketIntent);
        } catch (ActivityNotFoundException ex) {
            Log.d(TAG, "Error, Could not launch the market application.");
        }
    }

    // Unlocks PKCS container to get to the certificate
    private void createCertBitmap() {
     // Get certificate
     String[] certAndKey = getEasyPemCert();
     String pemCert = certAndKey[0];
     String B64publicKey = certAndKey[1];

     nextActivity(pemCert,B64publicKey,"pemCert");
    }

    // Unlocks PKCS container to get to key, moves further
    private void createKeyBitmap() {
        // Because...try catch availability outside the TryCatch.
        String pemCert = null;

        // What is the certificate password?
        TextView etPassword = (TextView)findViewById(R.id.et_password);
        char[] certPassword = etPassword.getText().toString().toCharArray();

        /** Initialize class **/
        CertificateExtractor certificateExtractor = new CertificateExtractor();
        String strAlias = certificateExtractor.setKeyStore(certFile, certPassword);

        // Check if password correct
        if(strAlias == null) {
            customToast("Wrong Password");
        } else {

            // Set new password (same as old password)

            // Get BASE64 encoded and password encrypted private key (modulus and exponent only).
            String encryptedKeys = null;
            try {
                encryptedKeys = certificateExtractor.getB64EncryptedPrivateKeyFromKeystore(certPassword, certPassword, strAlias);
                //Log.d(TAG,"PE:            \n"+encryptedKeys+"\n");

                // Assigned to global variable to be able to get out Issuer, subject... etc
                x509certificate = certificateExtractor.getx509certificate(strAlias);

            } catch (Exception e) {
                Log.d(TAG, "Unable to get keys in byte form");
            }
            nextActivity(encryptedKeys, "Ne rabim", "encryptedKeys");
        }
    }

    //Retruns Base64 encoded Certificate and Key.
    private String[] getEasyPemCert(){
        // Because...try catch availability outside the TryCatch.
        String[] certAndKey = new String[2];
        String pemCert = null;
        String B64publicKey = null;

        // What is the certificate password?
        TextView etPassword = (TextView)findViewById(R.id.et_password);
        char[] certPassword = etPassword.getText().toString().toCharArray();

        /** Initialize class **/
        CertificateExtractor certificateExtractor = new CertificateExtractor();
        // put PKCS12 container to a store, check if it contains a certificate, return the Alias of the certificate
        // It is assumed there is only one 509 certificate inside, unexpected results otherwise.

        String strAlias = certificateExtractor.setKeyStore(certFile,certPassword);

        // Check if password correct
        if(strAlias == null) {
            customToast("Wrong Password");
            return null;
        }

        // Get BASE64 encoded certificate (no private key)
        try {
            pemCert = certificateExtractor.getPemCert(strAlias);
            B64publicKey = certificateExtractor.getB64PublicKey(strAlias);

            // Assigned to global variable to be accesible
            x509certificate = certificateExtractor.getx509certificate(strAlias);

        } catch (Exception e) {
            Log.d(TAG,"Certificate could not be loaded to BASE64 String");
            customToast(getResources().getString(R.string.toast_wrongPassword));

        }
        certAndKey[0] = pemCert;
        certAndKey[1] = B64publicKey;

        return certAndKey;
    }

    // Starts the next activity and brings with it some data
    private void nextActivity(String data1, String data2, String type) {
        Intent intent = new Intent(this,displayBitmap.class);
        intent.putExtra("data",data1);          // Certificate or Key
        intent.putExtra("data2",data2);         // public key or ignored
        intent.putExtra("type",type);           // Difirentiating between the 2 possible documents

        // Strings - get
        String subjectName = x509certificate.getSubjectDN().getName();
        String issuerName = x509certificate.getIssuerDN().getName();
        String notBefore = x509certificate.getNotBefore().toString();
        String notAfter = x509certificate.getNotAfter().toString();

        // Substring attributes
        subjectName = substringBetween(subjectName,"CN=",",");           //if last one empty, it means to the end.
        issuerName = substringBetween(issuerName,"OU=",",");

        intent.putExtra("subjectName",subjectName);
        intent.putExtra("issuerName",issuerName);
        intent.putExtra("notBefore",notBefore);
        intent.putExtra("notAfter",notAfter);

        startActivity(intent);
        finish();
    }

    // Substring from - to certain string
    private String substringBetween(String text, String start, String stop) {
        // First check if there even is what we are searching for
        int INstart = 0;
        if (text.indexOf(start) > 0) {
            INstart = text.indexOf(start);
            INstart = INstart + start.length();
        } else {
            // Returnt just everything
            return text;
        }

        // Cut everything before the text
        String textAfterStart = text.substring(INstart);

        // STOPing index
            // if user even gave a stopping index
        if (stop.equals("")) {
            return textAfterStart;
            // if found
        } else if (textAfterStart.indexOf(stop) > 0) {
            return textAfterStart.substring(0,textAfterStart.indexOf(stop));
        } else {
            return textAfterStart;
        }
    }

    // Toast message
    public void customToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}

/*
        // Decrypt decode the key from QR code:
        try {
            RSAPrivateKey keyR = certificateExtractor.decryptKeyFromQR(newPkPassword,encryptedKeys);
            Log.d(TAG,"PE:            \n"+keyR.getPrivateExponent().toString(36)+"\n"+keyR.getModulus().toString(36));
            customToast("PE:            \n"+keyR.getPrivateExponent().toString(36)+"\n"+keyR.getModulus().toString(36));
        } catch (Exception e) {
            Log.d(TAG,"Unable to decrypt key");
        }

    }
*/
