package edu.temple.mapchat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

public class KeyService extends Service {

    KeyPair kp;
    PublicKey storedPublicKey;
    PrivateKey storedPrivateKey;
    HashMap <String, String> storedKeys;
    private final IBinder mBinder = new LocalBinder();

    // stored values
    private SharedPreferences sharedPref;
    private final String STORED_KEYS_PREF = "STORED_KEYS_PREF";
    private final String PUBLIC_KEY_PREF = "PUBLIC_KEY_PREF";
    private final String PRIVATE_KEY_PREF = "PRIVATE_KEY_PREF";


    public class LocalBinder extends Binder {
        KeyService getService() {
            // Return this instance of KeyService so clients can call public methods
            return KeyService.this;
        }
    }

    public KeyService() {
        storedKeys = new HashMap<>();
    }

    // retrieve stored values
    @Override
    public void onCreate() {
        super.onCreate();

        sharedPref = getSharedPreferences("keyServicePrefs", Context.MODE_PRIVATE);

        String jsonKeysString = sharedPref.getString(STORED_KEYS_PREF, "");
        String publicKeyString = sharedPref.getString(PUBLIC_KEY_PREF, "");
        String privateKeyString = sharedPref.getString(PRIVATE_KEY_PREF, "");


        if(!jsonKeysString.equals("")) {
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            storedKeys = new Gson().fromJson(jsonKeysString, type);
        }
        if(!publicKeyString.equals("") && !privateKeyString.equals("")) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                byte[] encodedPublicKey = Base64.decode(publicKeyString, Base64.DEFAULT);
                X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedPublicKey);
                storedPublicKey = keyFactory.generatePublic(pubSpec);

                byte[] encodedPrivateKey = Base64.decode(privateKeyString, Base64.DEFAULT);
                PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
                //X509EncodedKeySpec privSpec = new X509EncodedKeySpec(encodedPrivateKey); // goin' with my gut
                storedPrivateKey = keyFactory.generatePrivate(privSpec);

                kp = new KeyPair(storedPublicKey, storedPrivateKey);
                Log.e(" keytrack", "retrieved public: " + storedPublicKey);
                Log.e(" keytrack", "retrieved private: " + storedPrivateKey);

            } catch (Exception e) {
                Log.e(" keytrack", "couldn't retrieve keys");
            }
        }
        else
            Log.e(" keytrack", "didn't retrieve any keys");

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(" keytrack", "we bound once");

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
                Log.e(" keytrack", user + "made private key: " + storedPrivateKey);

            } else {
                resetMyKeyPair();
                Log.e(" keytrack", user + "changed public key: " + storedPublicKey);
                Log.e(" keytrack", user + "changed private key: " + storedPrivateKey);
            }
        } catch (Exception e) {
            Log.e(" keytrack", "ran into an issue generating the keypair");
        }

        String encodeStorePub = Base64.encodeToString(storedPublicKey.getEncoded(), Base64.DEFAULT);
        sharedPref.edit().putString(PUBLIC_KEY_PREF, encodeStorePub).apply();

        String encodeStorePriv = Base64.encodeToString(storedPrivateKey.getEncoded(), Base64.DEFAULT);
        sharedPref.edit().putString(PRIVATE_KEY_PREF, encodeStorePriv).apply();

        Log.e(" keytrack", "stored public: " + encodeStorePub);
        Log.e(" keytrack", "stored private: " + encodeStorePriv);
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
        String mapString = new JSONObject(storedKeys).toString();
        sharedPref.edit().putString(STORED_KEYS_PREF, mapString).apply();

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

    public void resetMyKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            storedPublicKey = kp.getPublic();
            storedPrivateKey = kp.getPrivate();
        } catch (Exception e) {
            Log.e(" keytrack", "ran into an issue resetting the keypair");
        }
    }

    public void resetPublicKey(String partnerName) {
        storedKeys.remove(partnerName);
    }

    public void testGiveThisManAKey(String user) {
        genMyKeyPair(user);
        storePublicKey(user, getMyPublicKey());
    }
}
