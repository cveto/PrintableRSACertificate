package csbslovenia.com.printablersacertificate;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.crypto.SecretKey;

/**
 * Created by Cveto on 21.8.2014.
 */

public class CertificateExtractor {
private static final String TAG = "CertificateExtractor";
private static KeyStore myStore = null;

    // Unlock the Container and set the store. (Private key is still protected after this, so its okay to save it to global variable)
    public String setKeyStore(InputStream isCert, char[] certPassword) {
        try {
            KeyStore myStore = KeyStore.getInstance("PKCS12");
            myStore.load(isCert, certPassword);

            /**Find a certificate in the PKCS12 container (should be just one in Sigen-ca)**/
            Enumeration<String> eAliases = myStore.aliases();

            while (eAliases.hasMoreElements()) {
                String strAlias = eAliases.nextElement();
                Log.d(TAG, "Alias: " + strAlias);

                if (myStore.isKeyEntry(strAlias)) {
                    Log.d(TAG, "KeyStore is a key entry and was saved. " + strAlias);
                    this.myStore = myStore;
                    return strAlias;
                }
            }
        } catch(Exception e){
                Log.d(TAG, "Store unreachable");

        }
        return null;
    }

    // Gets x509 certificate and encodes it to Base64
    public String getPemCert(String strAlias) throws KeyStoreException,CertificateEncodingException{

        // get Certificate from container
        X509Certificate cert =(X509Certificate) this.myStore.getCertificate(strAlias);

        // Transformt certificate to PEM
        String pemCert = Base64.encodeToString(cert.getEncoded(),Base64.NO_WRAP);
        String cert_begin = "-----BEGIN CERTIFICATE-----";
        String end_cert = "-----END CERTIFICATE-----";

        // Add Linebreaks to make a valid PEM (just header is enough)
        //pemCert = cert_begin + "\\n" + pemCert.replaceAll("(.{64})", "$1\\\\n") + "\\n" + end_cert;           //Line breaks everywhere
        //pemCert = cert_begin + "\n" + pemCert.replaceAll("(.{64})", "$1\n") + "\n" + end_cert;           //Real line breaks
        //pemCert = cert_begin + "\\n" + pemCert + "\\n" + end_cert;                                            //Line breaks after Header and Footer only
        pemCert = cert_begin + "\n" + pemCert + "\n" + end_cert;                                            // Real Line breaks after Header and Footer only
        return pemCert;
    }

    // Gets the X509 certificate from Store
    public X509Certificate getx509certificate(String strAlias) throws KeyStoreException,CertificateEncodingException{
        // get Certificate from container
        X509Certificate cert =(X509Certificate) this.myStore.getCertificate(strAlias);
        return cert;
    }

    // Gets the WHOLE Private key raw from Store
    private RSAPrivateKey getPrivateKey(String strAlias, char[] password) throws KeyStoreException,UnrecoverableKeyException,NoSuchAlgorithmException,InvalidKeySpecException{

        /**Private Key**/
        // get private exponent and modulus out of the Store,
        RSAPrivateCrtKey pk = (RSAPrivateCrtKey) this.myStore.getKey(strAlias, password);
        return pk;
    }

    // Reduces the private key to modulus and private exponent only
    private byte[] privateKeysToByteReduced(RSAPrivateKey pk) {
        // get byte for Private key modulus and Private Exponent
        byte[] BYpkMod = pk.getModulus().toByteArray();
        byte[] BYpkPe = pk.getPrivateExponent().toByteArray();

        // Get their sizes in byte form
        byte[] BYpkModSize = intToByte(BYpkMod.length);
        byte[] BYpkPeSize = intToByte(BYpkPe.length);

        //concencate those 4 together. ModSize - Mod - PeSize - Pe.
        byte[] BYconcencated = new byte[4+BYpkMod.length+4+BYpkPe.length];      // size of an int is 4 bytes (32 bits)
        ByteBuffer target = ByteBuffer.wrap(BYconcencated);
        target.put(BYpkModSize);
        target.put(BYpkMod);
        target.put(BYpkPeSize);
        target.put(BYpkPe);

        return BYconcencated;
    }

    // Reduces the public key to modulus and public exponent only
    private byte[] publicKeysToByteReduced(RSAPublicKey pk) {
        // get byte for Private key modulus and Private Exponent
        byte[] BYpkMod = pk.getModulus().toByteArray();
        byte[] BYpkPe = pk.getPublicExponent().toByteArray();

        //Log.d(TAG,pk.getModulus().toString(36));
        //Log.d(TAG,pk.getPublicExponent().toString(36));

        // Get their sizes in byte form
        byte[] BYpkModSize = intToByte(BYpkMod.length);
        byte[] BYpkPeSize = intToByte(BYpkPe.length);

        // concencate those 4 together. ModSize - Mod - PeSize - Pe.
        byte[] BYconcencated = new byte[4+BYpkMod.length+4+BYpkPe.length];      // size of an int is 4 bytes (32 bits)
        ByteBuffer target = ByteBuffer.wrap(BYconcencated);
        target.put(BYpkModSize);
        target.put(BYpkMod);
        target.put(BYpkPeSize);
        target.put(BYpkPe);

        return BYconcencated;
    }

    // Transforms bytes from QR code back to the RSAPrivateKey
    private RSAPrivateKey byteToKey(byte[] BYsource) throws NoSuchAlgorithmException,InvalidKeySpecException{
        int intSize = 4;

        // Read 4 bytes - get length of Mod:
        byte[] BYmodLenght = new byte[intSize];
        System.arraycopy(BYsource, 0, BYmodLenght, 0, intSize);
        Integer modLenght = byteToInt(BYmodLenght);

        // Get Modulus
        byte[] BYmod = new byte[modLenght];
        System.arraycopy(BYsource,intSize,BYmod,0,modLenght);     //start from 4 to mod length
        BigInteger mod = new BigInteger(BYmod);

        // Get Private Exponent Length
        byte[] BYpeLenght = new byte[intSize];
        System.arraycopy(BYsource, intSize+modLenght, BYmodLenght, 0, intSize);
        Integer peLenght = byteToInt(BYmodLenght);

        // Get Private Exponent
        byte[] BYpe = new byte[peLenght];
        System.arraycopy(BYsource,intSize+modLenght+intSize,BYpe,0,peLenght);     //start from 4 to mod length
        BigInteger pe = new BigInteger(BYpe);

        // Recreate the private Key
        RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(mod,pe);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKeyR = (RSAPrivateKey) factory.generatePrivate(privateSpec);

        return privateKeyR;
    }

    // Transforms byte to Integer (Integer must be in byte already).
    private Integer byteToInt(byte[] BYsource) {
        ByteBuffer wrapped = ByteBuffer.wrap(BYsource);
        Integer result = wrapped.getInt();
        return result;
    }

    // Gets bytes from an Integer.
    private byte[] intToByte(Integer number) {
        // put size of it to byte array:
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);		// since integer has 4 bytes.
        byteBuffer.putInt(number);		// 257 probably. I am saving the number 257 to 4 bytes
        byte[] BYnumber = byteBuffer.array();
        return BYnumber;
    }

    // Gets Base64 encrypted and ecnoded private key
    public String getB64EncryptedPrivateKeyFromKeystore (char[] newPassword, char[] p12Password, String strAlias) throws KeyStoreException,UnrecoverableKeyException,NoSuchAlgorithmException,InvalidKeySpecException
    {
        // Get the key from PKCS12
        RSAPrivateKey key = getPrivateKey(strAlias,p12Password);

        // Turn the Modulus and Private Exponent from key to bytes (part of the key is thrown away
        byte[] byteKeys = privateKeysToByteReduced(key);

        // generate salt for Encryption
        final byte[] salt = AESCrypto.generateSalt();

        // create key from password and salt. Name it DK for Derived Key.
        SecretKey DK = AESCrypto.deriveKeyPbkdf2(salt,newPassword);

        // Encrypt using byte, key and salt. You shall receive ||| salt ] IV ] ciphertext ||||in BASE_64_NoWrap readable form.
        String st_cipherText = AESCrypto.encryptBytes(byteKeys,DK,salt);

        /** Concatenate title to the ciphertext**/
        // st_chiperText = st_chiperText.concat("]"+Crypto.toBase64(st_title.getBytes(Charset.forName("UTF-8"))));
        return st_cipherText;
    }

    // Gets Base64 encoded public key
    public String getB64PublicKey (String strAlias) throws CertificateEncodingException,KeyStoreException,UnrecoverableKeyException,NoSuchAlgorithmException,InvalidKeySpecException
    {
        // Get public key
        X509Certificate cert = (X509Certificate) myStore.getCertificate(strAlias);
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
        Log.d(TAG,"Got the public keys, problem not here");

        // Get the reduced version of the public key
        byte[] byteKeys = publicKeysToByteReduced(publicKey);
        Log.d(TAG,"Got the bytes, problem not here also");

        // no encryption necessary

        //return the public key in Base64 format
        String B64keys = Base64.encodeToString(byteKeys,Base64.NO_WRAP);
        return B64keys;
    }

    // not needed here
    // Transforms Base64 encoded chipertext and makes a Java RSAPrivateKey
    public RSAPrivateKey decryptKeyFromQR(char[] password, String ciphertext) throws NoSuchAlgorithmException,InvalidKeySpecException {

        // Decrypt bytes
        byte[] keysDecrypted = AESCrypto.decryptPbkdf2toBytes(ciphertext,password);

        // Recreate Key
        RSAPrivateKey key = byteToKey(keysDecrypted);

        return key;
    }

}
