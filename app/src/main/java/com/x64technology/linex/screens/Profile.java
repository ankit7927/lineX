package com.x64technology.linex.screens;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.x64technology.linex.R;
import com.x64technology.linex.database.contact.ContactViewModel;
import com.x64technology.linex.databinding.ActivityProfileBinding;
import com.x64technology.linex.models.Contact;
import com.x64technology.linex.services.SocketManager;
import com.x64technology.linex.services.UserPreference;
import com.x64technology.linex.utils.Constants;
import com.x64technology.linex.utils.EnDecoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import io.socket.client.Socket;

public class Profile extends AppCompatActivity {
    ActivityProfileBinding profileBinding;
    UserPreference userPreference;
    Intent intent;
    ContactViewModel contactViewModel;
    Contact contact;
    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(profileBinding.getRoot());

        initVars();

        setCallbacks();
    }

    private void initVars() {
        intent = getIntent();
        userPreference = new UserPreference(this);

        layoutUpdates();



        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        socket = SocketManager.socket;
    }

    private void setCallbacks() {
        profileBinding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        profileBinding.proReqAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                if (contact.reqType.equals(Constants.REQUEST_SENT)) {
                    try {
                        jsonObject.put(Constants.TO, contact.userId);
                        jsonObject.put(Constants.FROM, userPreference.userPref.getString(Constants.STR_USERID, ""));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    socket.emit(Constants.EVENT_REQUEST_CANCELED, jsonObject);
                    contactViewModel.delete(contact);
                    finish();
                } else {
                    try {
                        jsonObject.put(Constants.TO, contact.userId);
                        jsonObject.put(Constants.FROM, userPreference.userPref.getString(Constants.STR_USERID, ""));
                        jsonObject.put(Constants.STR_NAME, userPreference.userPref.getString(Constants.STR_NAME, ""));
                        jsonObject.put(Constants.STR_DPLINK, userPreference.userPref.getString(Constants.STR_DPLINK, "link from fb user"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    socket.emit(Constants.EVENT_REQUEST_ACCEPTED, jsonObject);
                    contact.reqType = Constants.REQUEST_ACCEPTED;
                    contactViewModel.update(contact);
                    layoutUpdates();
                    // TODO create chat table and message table for this user
                }
            }
        });

        profileBinding.proReqReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(Constants.TO, contact.userId);
                    jsonObject.put(Constants.FROM, userPreference.userPref.getString(Constants.STR_USERID, ""));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                socket.emit(Constants.EVENT_REQUEST_REJECTED, jsonObject);
                contactViewModel.delete(contact);
                finish();
            }
        });
    }

    private void layoutUpdates() {

        if (intent.hasExtra("contact")) {
            contact = (Contact) intent.getSerializableExtra("contact");
            profileBinding.toolbar.setTitle("Contact info");
            profileBinding.proName.setText(contact.name);
            profileBinding.proContactCode.setText(String.format(Locale.getDefault(), "cc: %s", contact.userId));
            profileBinding.proInfo.setText(contact.reqType);

            switch (contact.reqType) {
                case Constants.REQUEST_ACCEPTED:
                    profileBinding.proReqAccept.setVisibility(View.GONE);
                    profileBinding.proReqReject.setVisibility(View.GONE);
                    break;
                case Constants.REQUEST_REJECTED:
                    profileBinding.proMessage.setText("Retry Connection");
                    profileBinding.proDisconnect.setVisibility(View.GONE);
                    profileBinding.proReqAccept.setVisibility(View.GONE);
                    profileBinding.proReqReject.setVisibility(View.GONE);
                    break;

                case Constants.REQUEST_RECEIVED:
                    profileBinding.proMessage.setVisibility(View.GONE);
                    profileBinding.proDisconnect.setVisibility(View.GONE);
                    break;
                case Constants.REQUEST_SENT:
                    profileBinding.proReqAccept.setText("Delete Request");
                    profileBinding.proReqReject.setVisibility(View.GONE);
                    profileBinding.proMessage.setVisibility(View.GONE);
                    profileBinding.proDisconnect.setVisibility(View.GONE);
            }
        } else {
            String name = userPreference.userPref.getString(Constants.STR_NAME, ""); // this will be given by firebase user
            // this will be given by firebase user
            String userId = userPreference.userPref.getString(Constants.STR_USERID, "");

            profileBinding.proName.setText(name);
            profileBinding.proContactCode.setText(String.format(Locale.getDefault(), "cc: %s", userId));
            profileBinding.proInfo.setText("some new notification");

            profileBinding.proReqAccept.setVisibility(View.GONE);
            profileBinding.proReqReject.setVisibility(View.GONE);
            profileBinding.proMessage.setVisibility(View.GONE);
            profileBinding.proDisconnect.setVisibility(View.GONE);
        }
    }
}