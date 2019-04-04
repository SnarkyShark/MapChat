package edu.temple.mapchat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

public class KeyService extends Service {

    KeyPair kp;
    PublicKey storedPublicKey;
    PrivateKey storedPrivateKey;
    Map <String, String> storedKeys;
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        KeyService getService() {
            // Return this instance of KeyService so clients can call public methods
            return KeyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(" keytrack", "we bound once");

        storedKeys = new HashMap<String, String>();

        return mBinder;
    }

    public void genMyKeyPair(String user) {

        try {
            if (storedPublicKey == null || storedPrivateKey == null) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                kp = kpg.generateKeyPair();
                storedPublicKey = kp.getPublic();
                storedPrivateKey = kp.getPrivate();
                Log.e(" keytrack", user + "made public key: " + storedPublicKey);
                Log.e(" keytrack", user + "made private key: " + storedPublicKey);

            } else {
                kp = new KeyPair(storedPublicKey, storedPrivateKey);
                Log.e(" keytrack", user + "changed public key: " + storedPublicKey);
                Log.e(" keytrack", user + "changed private key: " + storedPublicKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Returns PEM-formatted public key
     */
    String getMyPublicKey(){
        if(storedKeys != null){
            PublicKey key = storedPublicKey;
            byte[] keyBytes = key.getEncoded();

            String encodedKey = Base64.encodeToString(keyBytes,Base64.DEFAULT);
            String retVal = "-----BEGIN PUBLIC KEY-----\n"+encodedKey+"-----END PUBLIC KEY-----";
            Log.d("Public Key Export", retVal);
            return retVal;
        }
        return "";
    }

    public void storePublicKey (String partnerName, String publicKey) {
        String storeKey = publicKey.replace("-----BEGIN PUBLIC KEY-----\n", "");
        storeKey = storeKey.replace("-----END PUBLIC KEY-----", "");
        storedKeys.put(partnerName, storeKey);
        Log.e(" keytrack",  "stored " + storeKey + " for: " + partnerName);

    }

    public String encrypt (String partnerName, String plaintext) {
        Log.e(" keytrack",  "for: " + partnerName + ", msg: " + plaintext);
        try {
            RSAPublicKey publicKey = getPublicKey(partnerName);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
            Log.e(" keytrack", "for: " + partnerName + ", before encode: " + encryptedBytes.toString());

            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt (String cipherText, String sender) {
        Log.e(" keytrack", "from: " + sender + ", before decode: " + cipherText);

        byte[] encrypted = Base64.decode(cipherText, Base64.DEFAULT);
        Log.e(" keytrack", "from: " + sender + ", after decode: " + encrypted.toString());

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, storedPrivateKey);
            return new String(cipher.doFinal(encrypted));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public RSAPublicKey getPublicKey(String partnerName) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKey = storedKeys.get(partnerName);
        if (publicKey == null) return null;
        else { // if it gets this far, it better be a real key
            byte[] publicBytes = Base64.decode(publicKey, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        }
    }

    public void resetMyKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        storedPublicKey = kp.getPublic();
        storedPrivateKey = kp.getPrivate();
    }

    public void resetPublicKey(String partnerName) {
        storedKeys.remove(partnerName);
    }

    public void testGiveThisManAKey(String user) {
        genMyKeyPair(user);
        storePublicKey(user, getMyPublicKey());
    }
}
