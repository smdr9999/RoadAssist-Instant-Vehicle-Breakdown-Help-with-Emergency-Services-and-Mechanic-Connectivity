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

public class LoginActivity extends AppCompatActivity {

    private EditText editTextPhone, editTextOTP;
    private Button buttonSendOTP, buttonVerifyOTP;
    private TextView textViewRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String verificationId;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        // Initialize Firebase App Check with Debug provider (for development only)
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        Log.d(TAG, "Firebase App Check initialized with Debug provider.");

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        // Reference to the "Users" node (phone numbers stored as raw 10-digit strings)
        usersRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users");

        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOTP = findViewById(R.id.editTextOTP);
        buttonSendOTP = findViewById(R.id.buttonSendOTP);
        buttonVerifyOTP = findViewById(R.id.buttonVerifyOTP);
        textViewRegister = findViewById(R.id.textViewRegister);

        buttonSendOTP.setOnClickListener(v -> {
            Log.d(TAG, "Send OTP button clicked");
            sendOTP();
        });

        buttonVerifyOTP.setOnClickListener(v -> {
            Log.d(TAG, "Verify OTP button clicked");
            verifyOTP();
        });

        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void sendOTP() {
        // Read the phone number entered by the user.
        String rawPhoneNumber = editTextPhone.getText().toString().trim();

        // Validate that it is a 10-digit number.
        if (TextUtils.isEmpty(rawPhoneNumber) || rawPhoneNumber.length() != 10) {
            Toast.makeText(this, "Enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid phone number entered: " + rawPhoneNumber);
            return;
        }

        Log.d(TAG, "Checking if phone number is registered: " + rawPhoneNumber);

        // Query the "Users" node for the raw phone number.
        usersRef.orderByValue().equalTo(rawPhoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Log.d(TAG, "Phone number is registered. Sending OTP.");
                            // Prepare the E.164 formatted phone number by prepending "+91".
                            String phoneNumberForOTP = "+91" + rawPhoneNumber;
                            sendOTPForRegisteredNumber(phoneNumberForOTP);
                        } else {
                            Log.w(TAG, "Phone number not registered: " + rawPhoneNumber);
                            Toast.makeText(LoginActivity.this, "This number is not registered. Please register first.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking phone number registration: " + error.getMessage());
                        Toast.makeText(LoginActivity.this, "Error checking registration status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendOTPForRegisteredNumber(String phoneNumber) {
        Log.d(TAG, "Sending OTP to: " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Log.d(TAG, "onVerificationCompleted: Credential received");
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException e) {
                        Log.e(TAG, "onVerificationFailed: " + e.getMessage(), e);
                        Toast.makeText(LoginActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String s,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(s, token);
                        verificationId = s;
                        Log.d(TAG, "onCodeSent: Verification ID received: " + s);
                        Toast.makeText(LoginActivity.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                        // Optionally, make OTP field visible here if it was initially hidden.
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
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
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential: Success, userId: " +
                                Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, UserTypeSelectionActivity.class));
                        finish();
                    } else {
                        Log.e(TAG, "signInWithCredential: Failed", task.getException());
                        Toast.makeText(LoginActivity.this, "OTP Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
