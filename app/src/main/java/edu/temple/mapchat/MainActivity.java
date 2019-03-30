package edu.temple.mapchat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

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
            String message = parent.getItemAtPosition(position).toString();
            intent.putExtra(EXTRA_FRIEND, message);
            startActivity(intent);
        }
    };
}