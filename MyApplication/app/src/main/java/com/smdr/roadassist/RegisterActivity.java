package com.smdr.roadassist;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextPhone, editTextOTP;
    private Button buttonSendOTP, buttonVerifyOTP;
    private TextView textViewLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String verificationId;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        // Initialize Firebase App Check with Debug provider (for development only)
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        Log.d(TAG, "Firebase App Check initialized with Debug provider.");

        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        // Reference to "Users" node (assumes numbers are stored as raw 10-digit strings)
        mDatabase = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users");

        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOTP = findViewById(R.id.editTextOTP);
        buttonSendOTP = findViewById(R.id.buttonSendOTP);
        buttonVerifyOTP = findViewById(R.id.buttonVerifyOTP);
        textViewLogin = findViewById(R.id.textViewLogin);

        buttonSendOTP.setOnClickListener(v -> {
            Log.d(TAG, "Send OTP button clicked");
            sendOTP();
        });
        buttonVerifyOTP.setOnClickListener(v -> {
            Log.d(TAG, "Verify OTP button clicked");
            verifyOTP();
        });
        textViewLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    private void sendOTP() {
        // Get the raw phone number as entered by the user (assumed to be 10 digits)
        String rawPhoneNumber = editTextPhone.getText().toString().trim();

        if (TextUtils.isEmpty(rawPhoneNumber) || rawPhoneNumber.length() != 10) {
            Toast.makeText(this, "Enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid phone number: " + rawPhoneNumber);
            return;
        }

        // For sending OTP, we need the full number in E.164 format.
        String phoneNumberForOTP = "+91" + rawPhoneNumber;

        Log.d(TAG, "Checking if phone number is already registered: " + rawPhoneNumber);
        // Check if the raw phone number already exists in the "Users" node.
        mDatabase.orderByValue().equalTo(rawPhoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // The phone number exists, so ask the user to log in.
                            Log.w(TAG, "Phone number already registered: " + rawPhoneNumber);
                            Toast.makeText(RegisterActivity.this, "This phone number is already registered. Please log in directly.", Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "Phone number not registered, sending OTP to: " + phoneNumberForOTP);
                            // Proceed to send OTP.
                            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                                    .setPhoneNumber(phoneNumberForOTP)
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(RegisterActivity.this)
                                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        @Override
                                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                            Log.d(TAG, "onVerificationCompleted: Credential received");
                                            signUpWithPhoneAuthCredential(credential);
                                        }

                                        @Override
                                        public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
                                            Log.e(TAG, "onVerificationFailed: " + e.getMessage(), e);
                                            Toast.makeText(RegisterActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                            super.onCodeSent(s, token);
                                            verificationId = s;
                                            Log.d(TAG, "onCodeSent: Verification ID received: " + s);
                                            Toast.makeText(RegisterActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .build();

                            PhoneAuthProvider.verifyPhoneNumber(options);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        Log.e(TAG, "Error checking phone number registration: " + error.getMessage());
                        Toast.makeText(RegisterActivity.this, "Error checking registration status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifyOTP() {
        String otpCode = editTextOTP.getText().toString().trim();

        if (TextUtils.isEmpty(otpCode) || otpCode.length() != 6) {
            Toast.makeText(this, "Enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid OTP entered: " + otpCode);
            return;
        }

        Log.d(TAG, "Verifying OTP: " + otpCode);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otpCode);
        signUpWithPhoneAuthCredential(credential);
    }

    private void signUpWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        // Store the raw phone number (without +91) in the Realtime Database.
                        String rawPhoneNumber = editTextPhone.getText().toString().trim();
                        mDatabase.child(userId).setValue(rawPhoneNumber)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Log.d(TAG, "User phone number stored successfully for userId: " + userId);
                                    } else {
                                        Log.e(TAG, "Failed to store user phone number", dbTask.getException());
                                    }
                                });
                        Log.d(TAG, "signInWithCredential: Success, userId = " + userId);
                        Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, UserTypeSelectionActivity.class));
                        finish();
                    } else {
                        Log.e(TAG, "signInWithCredential: Failed", task.getException());
                        Toast.makeText(RegisterActivity.this, "OTP Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
