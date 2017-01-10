package com.busyweb.firebaselogindemo.firebase;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by BusyWeb on 1/9/2017.
 */

public class MyFirebaseShared {

    private static MyFirebaseShared firebaseShared = null;
    public static MyFirebaseShared getInstance() {
        if (firebaseShared == null) {
            firebaseShared = new MyFirebaseShared();
        }
        return firebaseShared;
    }

    // Update Web Api Client Id: found from Google APIs Console project Web client
    // google-services.json
    //"client_id": "xxxxxxx.apps.googleusercontent.com"
    //"client_type": 3
    public static final String WEB_CLIENT_ID = "your-web-client-id.apps.googleusercontent.com";

    public static FirebaseUser FbUser = null;
    public static String FbRefreshToken = "";

    public static GoogleSignInAccount GsAccount = null;

    public static GoogleApiClient GaClient = null;

    public static FcmUser ServerUser = null;

    public class FcmUser {
        public int Id;
        public String Email;
        public String UserId;   // Firebase User Id
        //public String GoogleAccountId;
        public String DeviceToken;

        public FcmUser() {}
        public FcmUser(String json) {
            try {
                if (json != null && json != "" && !json.equalsIgnoreCase("null") && json.startsWith("{")) {
                    // check single json (no array)
                    JSONObject jsonObject = new JSONObject(json);
                    Object objId = jsonObject.get("Id");
                    Object objEmail = jsonObject.get("Email");
                    Object objUserId = jsonObject.get("UserId");
                    Object objDeviceToken = jsonObject.get("DeviceToken");
                    Id = (!isObjectNullOrEmpty(objId) ? (Integer)objId : -1);
                    Email = (!isObjectNullOrEmpty(objEmail) ? (String)objEmail : "");
                    UserId = (!isObjectNullOrEmpty(objUserId) ? (String)objUserId : "");
                    DeviceToken = (!isObjectNullOrEmpty(objDeviceToken) ? (String)objDeviceToken : "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private FcmUser getNewUser(String json) {
        return new FcmUser(json);
    }
    public static FcmUser GetServerUser(String json) {
        FcmUser user = getInstance().getNewUser(json);
        return user;
    }

    public static void ResetAuthorization() {
        FbUser = null;
        FbRefreshToken = "";
        GsAccount = null;
        GaClient = null;
    }

    public static void UpdateUserInformation(Context context, FcmUser serverUser) {
        try {
            if (serverUser == null || serverUser.Id < 0) {
                MyFirebaseShared.ServerUser = null;
                return;
            }
            MyFirebaseShared.ServerUser = serverUser;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String GetUser(String email) {
        JSONObject params = new JSONObject();
        try {
            params.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return MyFirebaseShared.SendRequestToServer(MyFirebaseShared.GetUserUrl, params);
    }

    public static String RegisterUser(FirebaseUser firebaseUser) {
        return RegisterUser(
                firebaseUser.getEmail(),
                firebaseUser.getUid(),
                FirebaseInstanceId.getInstance().getToken());
    }

    public static String RegisterUser(String email, String uid, String deviceToken) {
        JSONObject params = new JSONObject();
        try {
            params.put("email", email);
            params.put("uid", uid);
            //params.put("googleAccountId", gid);
            params.put("deviceToken", deviceToken);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return MyFirebaseShared.SendRequestToServer(MyFirebaseShared.RegisterUserUrl, params);
    }

    public static String UnRegisterUser(FirebaseUser firebaseUser) {
        return UnRegisterUser(firebaseUser.getEmail());
    }
    public static String UnRegisterUser(String email) {
        JSONObject params = new JSONObject();
        try {
            params.put("email", email);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        return MyFirebaseShared.SendRequestToServer(MyFirebaseShared.UnRegisterUserUrl, params);
    }

    // server side implementation
    public static boolean Debug = true;
    public static String ServerUrl = "http://www.busywww.com";
    public static String ServerUrlDebug = "http://localhost:8080";
    public static String GetServerUrl() {
        return (Debug ? ServerUrlDebug : ServerUrl);
    }
    public static final String ServiceName = "FirebaseCloudMessageDemo.asmx";
    public static final Integer ServicePort = 80;
    public static final String RegisterUserUrl = String.format("%s/%s/RegisterUser", GetServerUrl(), ServiceName);
    public static final String UnRegisterUserUrl = String.format("%s/%s/UnRegisterUser", GetServerUrl(), ServiceName);
    public static final String GetUserUrl = String.format("%s/%s/GetUser", GetServerUrl(), ServiceName);

    public static String SendRequestToServer(String url, JSONObject params) {
        String response = "";
        try {
            URL endpoint = new URL(url);
            //String body = params.toString();
            Uri.Builder builder = new Uri.Builder();
            Iterator<String> iterator = params.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = params.getString(key);
                builder.appendQueryParameter(key, value);
            }
            String body = builder.build().getEncodedQuery();

            HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            outputStream.write(body.getBytes());
            outputStream.close();

            int status = connection.getResponseCode();
            if (status == 200) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private boolean isObjectNullOrEmpty(Object object) {
        if (object == null) {
            return true;
        }
        if (object instanceof String) {
            String val = (String)object;
            if (val == null || val.equalsIgnoreCase("") || val.length() < 1) {
                return true;
            } else {
                return false;
            }
        }
        if (object instanceof Integer) {
            Integer valInt = (Integer)object;
            if (valInt == null || valInt < 0) {
                return true;
            } else {
                return false;
            }
        }
        if (object instanceof java.util.Date) {
            Date valDate = (Date)object;
            if (valDate == null || valDate.toString().length() < 1) {
                return true;
            } else {
                return false;
            }
        }
        if (object instanceof Boolean) {
            Boolean valBool = (Boolean)object;
            if (valBool == null) {
                return true;
            } else {
                return valBool;
            }
        }
        return true;
    }
}
