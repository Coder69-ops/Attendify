package com.example.attendify.ui.common;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.attendify.R;

public class PermissionDialogFragment extends DialogFragment {

    public interface PermissionDialogListener {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_ICON_RES_ID = "icon_res_id";

    private PermissionDialogListener listener;
    private String title;
    private String message;
    private int iconResId;

    public static PermissionDialogFragment newInstance(String title, String message, int iconResId) {
        PermissionDialogFragment fragment = new PermissionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_ICON_RES_ID, iconResId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setPermissionDialogListener(PermissionDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Attendify_Dialog);

        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            message = getArguments().getString(ARG_MESSAGE);
            iconResId = getArguments().getInt(ARG_ICON_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_permission_explanation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup UI components
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView messageTextView = view.findViewById(R.id.messageTextView);
        ImageView iconImageView = view.findViewById(R.id.permissionIcon);
        Button grantButton = view.findViewById(R.id.grantButton);
        Button notNowButton = view.findViewById(R.id.notNowButton);

        // Set values
        titleTextView.setText(title);
        messageTextView.setText(message);
        if (iconResId != 0) {
            iconImageView.setImageResource(iconResId);
        }

        // Set click listeners
        grantButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPermissionGranted();
            }
            dismiss();
        });

        notNowButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPermissionDenied();
            }
            dismiss();
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (listener != null) {
            listener.onPermissionDenied();
        }
    }
} 