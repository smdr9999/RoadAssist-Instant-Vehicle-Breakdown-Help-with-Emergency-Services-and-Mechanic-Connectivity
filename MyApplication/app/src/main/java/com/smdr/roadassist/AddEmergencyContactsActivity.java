package com.smdr.roadassist;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddEmergencyContactsActivity extends AppCompatActivity {

    private EditText editTextContact;
    private Button btnAddContact;
    private DatabaseReference emergencyContactsRef;
    private FirebaseAuth mAuth;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_emergency_contacts);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        driverId = currentUser.getUid();

        // Reference emergency contacts node under the current driver
        emergencyContactsRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("emergencyContacts")
                .child(driverId);

        editTextContact = findViewById(R.id.editTextContact);
        btnAddContact = findViewById(R.id.btnAddContact);

        btnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEmergencyContact();
            }
        });
    }

    private void addEmergencyContact() {
        String contact = editTextContact.getText().toString().trim();
        if (TextUtils.isEmpty(contact)) {
            Toast.makeText(this, "Please enter a contact number", Toast.LENGTH_SHORT).show();
            return;
        }
        // If the number is 10 digits, prepend "+91"
        if (contact.length() == 10) {
            contact = "+91" + contact;
        }

        emergencyContactsRef.push().setValue(contact)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEmergencyContactsActivity.this, "Emergency contact added", Toast.LENGTH_SHORT).show();
                    editTextContact.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEmergencyContactsActivity.this, "Failed to add contact", Toast.LENGTH_SHORT).show();
                });
    }
}
