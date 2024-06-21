package com.example.demoproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.demoproject.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment {

    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private Button changePasswordButton;
    private FirebaseUser user;

    private FrameLayout progressOverlay;
    private ImageView backButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);
        currentPasswordInput = view.findViewById(R.id.currentPasswordInput);
        newPasswordInput = view.findViewById(R.id.newPasswordInput);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        backButton=view.findViewById(R.id.change_password_backButton);
        progressOverlay = view.findViewById(R.id.progress_overlay);
        user = FirebaseAuth.getInstance().getCurrentUser();

        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressOverlay.setVisibility(View.VISIBLE);
                changePassword();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).popBackStack();

            }
        });

        return view;
    }

    private void changePassword() {
        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword)) {
            Toast.makeText(requireContext(), "Please enter both current and new passwords", Toast.LENGTH_SHORT).show();
            progressOverlay.setVisibility(View.GONE);
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Password updated successfully
                                Log.d("ChangePasswordFragment", "Password updated successfully!");
                                Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                progressOverlay.setVisibility(View.GONE);
                                Navigation.findNavController(requireView()).popBackStack();
                            } else {
                                // Failed to update password
                                progressOverlay.setVisibility(View.GONE);
                                Exception error = task.getException();
                                Log.e("ChangePasswordFragment", "Error updating password", error);
                                Toast.makeText(requireContext(), "Error updating password: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    // Re-authentication failed
                    progressOverlay.setVisibility(View.GONE);
                    Exception error = task.getException();
                    Log.e("ChangePasswordFragment", "Error re-authenticating user", error);
                    Toast.makeText(requireContext(), "Error re-authenticating user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
