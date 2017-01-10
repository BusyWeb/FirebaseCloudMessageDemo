package com.busyweb.firebaselogindemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.busyweb.firebaselogindemo.firebase.MyFirebaseMessagingService;
import com.busyweb.firebaselogindemo.firebase.MyFirebaseShared;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "FirebaseLoginDemo";
    private static final int REQUEST_SIGN_IN_ID = 9999;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private GoogleApiClient mGoogleApiClient;

    private Activity mActivity;
    private Context mContext;

    private TextView mTextViewHello;
    private SignInButton mSignInButton;
    private Button mSignOutButton;
    private ProgressDialog mProgressDialog;

    private Button mRegisterButton;
    private Button mUnRegisterButton;
    private TextView mTextViewMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mSignInButton.setSize(SignInButton.SIZE_WIDE);
        mSignInButton.setOnClickListener(this);

        mSignOutButton = (Button) findViewById(R.id.sign_out_button);
        mSignOutButton.setOnClickListener(this);

        mTextViewHello = (TextView) findViewById(R.id.textViewHello);

        mRegisterButton = (Button) findViewById(R.id.buttonRegister);
        mRegisterButton.setOnClickListener(this);

        mUnRegisterButton = (Button) findViewById(R.id.buttonUnRegister);
        mUnRegisterButton.setOnClickListener(this);

        mTextViewMessage = (TextView) findViewById(R.id.textViewMessage);

        prepareApp();
    }

    private void prepareApp() {
        try {
            mActivity = this;
            mContext = this;

            GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(MyFirebaseShared.WEB_CLIENT_ID)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                    .build();

            mFirebaseAuth = FirebaseAuth.getInstance();

            mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    MyFirebaseShared.FbUser = firebaseAuth.getCurrentUser();
                    MyFirebaseShared.FbRefreshToken = FirebaseInstanceId.getInstance().getToken();

                    if (MyFirebaseShared.FbUser != null) {
                        // user signed in
                        Log.i(TAG, "User signed in (uid): " + MyFirebaseShared.FbUser.getUid());

                        // check if user has registered
                        checkRegistrationStatus(MyFirebaseShared.FbUser.getEmail());

                    } else {
                        Log.i(TAG, "User signed out.");

                        MyFirebaseShared.ServerUser = null;
                        updateRegistrationStatus();
                    }

                    updateAppUi(MyFirebaseShared.FbUser);
                }
            };

            MyFirebaseMessagingService.MessageReceivedListener messageReceivedListener =
                    new MyFirebaseMessagingService.MessageReceivedListener() {
                        @Override
                        public void MessageReceived(final String message) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextViewMessage.setText(message);
                                }
                            });
                        }
                    };
            MyFirebaseMessagingService.SetMessageReceivedListener(messageReceivedListener);

        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SIGN_IN_ID) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                updateAppUi(null);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.i(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        showProgressDialog("Wait...");

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.i(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.i(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        hideProgressDialog();
                    }
                });
    }

    private void showProgressDialog(final String message) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = ProgressDialog.show(mContext, null, message);
            }
        });
    }

    private void hideProgressDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.buttonRegister:
                registerUser();
                break;
            case R.id.buttonUnRegister:
                unregisterUser();
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    private void signIn() {
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(intent, REQUEST_SIGN_IN_ID);
    }

    private void signOut() {
        mFirebaseAuth.signOut();

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateAppUi(null);
                    }
                }
        );
    }

    private void registerUser() {
        if (MyFirebaseShared.FbUser == null) {
            Toast.makeText(mContext, "Please sign-in first, and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        new RegisterUserTask().execute(MyFirebaseShared.FbUser);
    }

    private void unregisterUser() {
        if (MyFirebaseShared.FbUser == null) {
            Toast.makeText(mContext, "Please sign-in first, and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        new UnRegisterUserTask().execute(MyFirebaseShared.FbUser);
    }

    private void revokeAuthorization() {
        mFirebaseAuth.signOut();

        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateAppUi(null);
                    }
                }
        );
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, connectionResult.toString());
    }


    private void updateAppUi(final FirebaseUser user) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (user == null) {
                    mTextViewHello.setText("Sign In");
                    mSignInButton.setVisibility(View.VISIBLE);
                    mSignOutButton.setVisibility(View.GONE);
                } else {
                    mTextViewHello.setText(user.getEmail());
                    mSignInButton.setVisibility(View.INVISIBLE);
                    mSignOutButton.setVisibility(View.VISIBLE);
                }            }
        });
    }

    private void updateRegistrationStatus() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MyFirebaseShared.ServerUser != null) {
                    mRegisterButton.setEnabled(false);
                    mUnRegisterButton.setEnabled(true);
                    mTextViewMessage.setText("NO MESSAGE");
                } else {
                    mRegisterButton.setEnabled(true);
                    mUnRegisterButton.setEnabled(false);
                    mTextViewMessage.setText("NO MESSAGE");
                }
            }
        });
    }


    private void checkRegistrationStatus(String email) {
        new GetUserTask().execute(email);
    }

    private class GetUserTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            showProgressDialog("Wait...");
        }

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            result = MyFirebaseShared.GetUser(strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.equalsIgnoreCase("") && result.length() > 0) {
                // found user from server
                // set user
                MyFirebaseShared.ServerUser = MyFirebaseShared.GetServerUser(result);
            } else {
                MyFirebaseShared.ServerUser = null;
            }

            hideProgressDialog();

            updateRegistrationStatus();
        }

    }

    private class RegisterUserTask extends AsyncTask<FirebaseUser, Void, String> {

        @Override
        protected void onPreExecute() {
            showProgressDialog("Wait...");
        }

        @Override
        protected String doInBackground(FirebaseUser... firebaseUsers) {
            String result = "";
            result = MyFirebaseShared.RegisterUser(firebaseUsers[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.equalsIgnoreCase("") && result.length() > 0) {
                // set user
                MyFirebaseShared.ServerUser = MyFirebaseShared.GetServerUser(result);
            } else {
                MyFirebaseShared.ServerUser = null;
                // failed, do nothing
                Toast.makeText(mContext, "Failed to register user, please try again.", Toast.LENGTH_SHORT).show();
            }

            hideProgressDialog();

            updateRegistrationStatus();
        }

    }

    private class UnRegisterUserTask extends AsyncTask<FirebaseUser, Void, String> {

        @Override
        protected void onPreExecute() {
            showProgressDialog("Wait...");
        }

        @Override
        protected String doInBackground(FirebaseUser... firebaseUsers) {
            String result = "";
            result = MyFirebaseShared.UnRegisterUser(firebaseUsers[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.equalsIgnoreCase("") && result.length() > 0) {
                // return OK: "OK"
                if (result.equalsIgnoreCase("\"ok\"")) {
                    // success
                    MyFirebaseShared.ServerUser = null;
                    Toast.makeText(mContext, "Successfully unregistered.", Toast.LENGTH_SHORT).show();
                } else {
                    // failed, do nothing
                    Toast.makeText(mContext, "Failed to un-register user, please try again.", Toast.LENGTH_SHORT).show();
                }

            } else {
                // failed, do nothing
                Toast.makeText(mContext, "Failed to un-register user, please try again.", Toast.LENGTH_SHORT).show();
            }

            hideProgressDialog();

            updateRegistrationStatus();
        }

    }
}
