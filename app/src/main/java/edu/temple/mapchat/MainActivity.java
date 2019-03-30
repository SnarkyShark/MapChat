package edu.temple.mapchat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    // on screen elements
    EditText usernameEditText;
    Button usernameButton;

    // important stored values
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hooking up screen elements
        usernameEditText = findViewById(R.id.usernameEditText);
        usernameButton = findViewById(R.id.usernameButton);

        // set username
        usernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUsername();
            }
        });

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
}
