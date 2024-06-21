package com.example.demoproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileFragment extends Fragment {


    private EditText email;

    private Button saveEmail;

    private ImageView back;

    private Button changePassword;

    private FirebaseUser user;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
         View view=inflater.inflate(R.layout.fragment_edit_profile, container, false);
        email=view.findViewById(R.id.edit_profile_emailEditText);
        saveEmail=view.findViewById(R.id.edit_profile_saveEmail);
        back=view.findViewById(R.id.edit_profile_backButton);
        changePassword=view.findViewById(R.id.edit_profile_changePassword);

        user=FirebaseAuth.getInstance().getCurrentUser();
        String emailId=user.getEmail().toString();
        email.setText(emailId);
        String currentEmail=email.getText().toString();




        saveEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPasswordInputDialog();
            }
        });






        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             Navigation.findNavController(v).navigate(R.id.action_editProfileFragment_to_changePasswordFragment);
            }
        });


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
            }

        });








        return view;
    }



    private void showPasswordInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.password_dialog, null);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        builder.setView(dialogView)
                .setTitle("Enter Your Password")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = passwordInput.getText().toString();
                        if (!TextUtils.isEmpty(password)) {
                            reAuthenticateUser(password);
                        } else {
                            Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void reAuthenticateUser(String password) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Re-authentication successful, update email
                    String newEmail = email.getText().toString();
                    user.updateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Email updated successfully
                                Log.d("EditProfileFragment", "Email updated successfully!");
                                Toast.makeText(requireContext(), "Email updated successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                // Failed to update email
                                Exception error = task.getException();
                                Log.e("EditProfileFragment", "Error updating email", error);
                                Toast.makeText(requireContext(), "Error updating email: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    // Re-authentication failed
                    Exception error = task.getException();
                    Log.e("EditProfileFragment", "Error re-authenticating user", error);
                    Toast.makeText(requireContext(), "Error re-authenticating user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}