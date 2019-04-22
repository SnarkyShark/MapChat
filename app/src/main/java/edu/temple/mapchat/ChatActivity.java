package edu.temple.mapchat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ChatActivity extends AppCompatActivity {

    // on screen elements
    ListView chatListView;
    EditText editText;
    Button sendButton;

    // important stored values
    String username, friendName;
    ArrayList<String> messages;
    ArrayAdapter<String> adapter;
    private final String sendMessageURL = "https://kamorris.com/lab/send_message.php";

    // key service
    KeyService mService;
    boolean mBound = false;

    // store values
    private String mSavedChatTag;
    private SharedPreferences mPrefs;
    public static final String CHAT_TAG_PREFIX = "CHAT_LOG_";

    // receive messages
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String to = intent.getStringExtra("to");
            String sender = intent.getStringExtra("partner");
            String content = intent.getStringExtra("message");
            //content = mService.decrypt(content, sender); // TODO: decrypt message
            Log.d("rectrack", "to: " + to + ", sender: " + sender + ", friendName: " + ", content: " + content);
            if(sender.equals(friendName)) {
                messages.add("sender: " + content);
                adapter.notifyDataSetChanged();
                chatListView.smoothScrollToPosition(messages.size() - 1);
            }
            // else not our message
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // intercept data from MainActivity
        Intent intent = getIntent();
        username = intent.getStringExtra(MainActivity.USERNAME_EXTRA);
        friendName = intent.getStringExtra(MainActivity.EXTRA_FRIEND);

        setTitle(friendName);

        // hooking up on screen elements
        chatListView = findViewById(R.id.chatListView);
        editText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.messageButton);

        // retrieving messages
        mPrefs = getSharedPreferences("myMapChatApp", Context.MODE_PRIVATE);
        String jsonChatLog = mPrefs.getString(mSavedChatTag, "");
        if(!jsonChatLog.equals("")){
            messages = parseLogJson(jsonChatLog);
        }
        else{
            messages = new ArrayList<>();
        }

        // list of messages
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, messages);
        chatListView.setAdapter(adapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    /**
     * Send message
     */
    private void sendMessage() {
        // display for ourselves
        String message = editText.getText().toString();
        editText.getText().clear();
        messages.add("me: " + message);
        adapter.notifyDataSetChanged();
        chatListView.smoothScrollToPosition(messages.size() - 1);

        // now send to server
        final String encryptedMessage = message; // TODO: encrypt it
        //final String encryptedMessage = mService.encrypt(message, friendName);

        Log.d("sendtrack", "username: " + username + ", friendName: " + friendName + ", message: " + encryptedMessage);
        if(username == null || friendName.equals("")|| message == null){
            return;
        }
        StringRequest stringRequest = new StringRequest(Request.Method.POST, sendMessageURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("sendtrack", "response: " + response); //the response contains the result from the server, a json string or any other object returned by your server

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace(); //log the error resulting from the request for diagnosis/debugging
                Log.d("sendtrack", "error: " + error); //the response contains the result from the server, a json string or any other object returned by your server
            }
        }){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> postMap = new HashMap<>();
                postMap.put("user", username);
                postMap.put("partneruser", "" + friendName);
                postMap.put("message", ""+ encryptedMessage);
                return postMap;
            }
        };
        Volley.newRequestQueue(this).add(stringRequest);
        Log.d("sendtrack", "added the request to the queue");
    }

    /**
     * set up Key Service
     * register messageReceiver
     */
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("new_message"));

        Intent intent = new Intent(this, KeyService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.e(" keytrack", "we tried to bind");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /**
     * Store/Retrieve message history
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        String json = logToJson();
        mPrefs.edit().putString(mSavedChatTag, json).apply();
    }

    private String logToJson(){
        Gson gson = new Gson();
        return gson.toJson(messages);
    }

    private ArrayList<String> parseLogJson(String jsonChatLog) {
        Gson gson = new Gson();
        if(!jsonChatLog.equals("")) {
            Type type = new TypeToken<ArrayList<String>>(){}.getType();
            return gson.fromJson(jsonChatLog, type);
        }
        else{
            return null;
        }
    }
}
