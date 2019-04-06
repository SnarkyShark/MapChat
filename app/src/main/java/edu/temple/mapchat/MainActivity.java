package edu.temple.mapchat;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {

    // list of friends
    ListView listView;
    ArrayList<String> friends;
    public static final String EXTRA_FRIEND = "";

    // username
    String username;
    EditText usernameEditText;
    Button usernameButton;
    private SharedPreferences sharedPref;
    private static final String USER_PREF_KEY = "USERNAME_PREF";
    Context context;

    // service
    KeyService mService;
    boolean mBound = false;

    // NFC Beam
    NfcAdapter nfcAdapter;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hooking up screen elements
        usernameEditText = findViewById(R.id.usernameEditText);
        usernameButton = findViewById(R.id.usernameButton);
        listView = findViewById(R.id.listView);

        // initialize values
        friends = new ArrayList<>();
        sharedPref = getSharedPreferences("myMapChatApp", Context.MODE_PRIVATE);

        // check/set username
        checkUsername();
        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUsername();
            }
        });

        // list of friends
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, friends);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(messageClickedHandler);
        getFriends();

        // Android Beam
        Intent nfcIntent = new Intent(this, MainActivity.class);
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.setNdefPushMessageCallback(this, this);
    }

    /**
     * Check username
     * Set username
     * Generate public & private keys
     */

    private void checkUsername() {
        String storedUsername = sharedPref.getString(USER_PREF_KEY, null);
        if(storedUsername != null) {
            username = storedUsername;
            setTitle("username: " + username);
        }
        else
            Toast.makeText(this, "please enter a username", Toast.LENGTH_SHORT).show();
        // TODO: post current location
    }

    private void setUsername() {
        boolean usernameReady;

        // generate new username
        username = usernameEditText.getText().toString();
        usernameReady = username.compareTo("") != 0;

        if (usernameReady) { // generate keypair
            Log.e( "usertrack", "username is not 0");
            mService.genMyKeyPair(username);
            usernameReady = sharedPref.edit().putString(USER_PREF_KEY, username).commit();
        }

        if (usernameReady) {
            setTitle("username: " + username);
        }
        else {
            Toast.makeText(this, "please enter a valid username", Toast.LENGTH_SHORT).show();
            Log.e( "usertrack", "username wasn't saved to shared preferences");
        }// TODO: post current location
    }

    /**
     * Friends ListView
     */
    public void getFriends() {

        friends.add("ron");
        friends.add("paul");
        friends.add("blart");
        friends.add("mall");
        friends.add("cop");
        friends.add("drop");
        friends.add("top");
        friends.add("crop");
        friends.add("mop");
    }

    // Launch ChatActivity
    private AdapterView.OnItemClickListener messageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            Intent intent = new Intent(parent.getContext(), ChatActivity.class);
            String friendName = parent.getItemAtPosition(position).toString();

            // check whether friendName has a key or not
            try {
                if (mService.getPublicKey(friendName) != null) {
                    intent.putExtra(EXTRA_FRIEND, friendName);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(parent.getContext(), "You don't have " + friendName + "'s key!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Beam Stuff
     */

    // Set Beam Payload
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String payload = setKey();
        NdefRecord record = NdefRecord.createTextRecord(null, payload);
        return new NdefMessage(new NdefRecord[]{record});
    }

    private String setKey() {
        String pubKey = mService.getMyPublicKey();
        if(pubKey.equals("")){
            Log.d("SEND EMPTY KEY", "KEY WAS EMPTY!");
            return "";
        }
        else{
            return "{\"user\":\""+ username +"\",\"key\":\""+ pubKey +"\"}";
            //Log.d("SENT KEY PAYLOAD", payload);
        }
    }

    // Accept Beam Payload
    void processIntent(Intent intent) {
        String payload = new String(
                ((NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0])
                        .getRecords()[0]
                        .getPayload());
        //Lop off the 'en' language code.
        String jsonString = payload.substring(3);
        if(jsonString.equals("")){
            Log.d("Message Recieved?", "Message was empty!");
        }
        else {
            try {
                JSONObject json = new JSONObject(jsonString);
                String owner = json.getString("user");
                String pemKey = json.getString("key");

                if(mBound) {
                    mService.storePublicKey(owner, pemKey);
                    Log.e(" beamtrack", "key stored successfully");
                }
                else
                    Log.e(" beamtrack", "key not stored!");

            } catch (JSONException e) {
                Log.e("JSON Exception", "Convert problem", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e( " beamtrack", "We resumed");

        // Get the intent from Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Log.e( " beamtrack", "We discovered an NDEF");
            processIntent(getIntent());
        }
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // look for new intents
    }

    /**
     * Service Stuff
     */

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, KeyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.e(" keytrack", "we tried to bind");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            KeyService.LocalBinder binder = (KeyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.e(" keytrack", "connected to the service");

            // TODO: Remove later
            // paul has a key already
            //mService.testGiveThisManAKey("paul");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}