package edu.temple.mapchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;


public class ChatActivity extends AppCompatActivity {

    // on screen elements
    ListView chatListView;
    EditText editText;
    Button sendButton;

    // important stored values
    String friendName;
    ArrayList<String> messages;
    ArrayAdapter<String> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // intercept data from MainActivity
        Intent intent = getIntent();
        friendName = intent.getStringExtra(MainActivity.EXTRA_FRIEND);

        setTitle(friendName);

        // hooking up on screen elements
        chatListView = findViewById(R.id.chatListView);
        editText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.messageButton);

        // initializing values
        messages = new ArrayList<>();

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
        String message = editText.getText().toString();
        editText.getText().clear();
        messages.add("me: " + message);
        adapter.notifyDataSetChanged();
        chatListView.smoothScrollToPosition(messages.size() - 1);
    }

}
