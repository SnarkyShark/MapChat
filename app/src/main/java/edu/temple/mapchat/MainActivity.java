package edu.temple.mapchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import java.util.ArrayList;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity {

    // on screen elements
    EditText usernameEditText;
    Button usernameButton;
    ListView listView;

    // important stored values
    String username;
    ArrayList<String> friends;
    public static final String EXTRA_FRIEND = "";
    Context context;

    // service
    KeyService mService;
    boolean mBound = false;

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
        context = this;

        // set username
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
    }

    /**
     * Set username
     * Generate public & private keys
     */
    private void setUsername() {
        username = usernameEditText.getText().toString();
        if (username.compareTo("") == 0)
            username = "default";
        setTitle("username: " + username);
        // TODO: uncomment this
        //mService.genMyKeyPair(username);
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
                    Toast.makeText(context, "You don't have " + friendName + "'s key!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

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
            mService.testGiveThisManAKey("paul");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}