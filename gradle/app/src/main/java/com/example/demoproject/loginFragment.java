package com.example.demoproject;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.demoproject.UserModel.User;
import com.example.demoproject.UserModel.UserDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class loginFragment extends Fragment {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private FirebaseAuth firebaseAuth;
    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         view = inflater.inflate(R.layout.fragment_login, container, false);



        emailEditText = view.findViewById(R.id.login_fragment_email_editText);
        passwordEditText = view.findViewById(R.id.login_fragment_password_editText);
        loginButton = view.findViewById(R.id.login_fragment_login_button);
        registerButton = view.findViewById(R.id.register_button);
        firebaseAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser(view);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment);
            }
        });



        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserGroup(currentUser.getEmail());
        }
    }

    private void checkUserGroup(String userEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("groups")
                .whereArrayContains("users", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // User is in a group
                            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_productListFragment);
                        } else {
                            // User is not in a group
                            Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_groupFragment);
                        }
                    } else {
                        Log.e("loginFragment", "Error checking user group", task.getException());
                        // Assume error occurred, navigate to GroupFragment as fallback
                        Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_groupFragment);
                    }
                });
    }



    private void loginUser(View view) {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }



            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(getActivity(), "Login successful", Toast.LENGTH_SHORT).show();
                                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                                checkUserGroup(currentUser.getEmail());

                            } else {

                                Toast.makeText(getActivity(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    }
}