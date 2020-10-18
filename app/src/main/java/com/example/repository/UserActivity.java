package com.example.repository;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UserActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public interface Delegate {
        void onResult(List<User> users);
    }

    @Nullable
    private FetchUserDataAsyncTask fetchUserDataAsyncTask;
    private final UserAdapter adapter = new UserAdapter();

    private final Delegate fetchUserDataDelegate = new Delegate() {
        @Override
        public void onResult(List<User> users) {
            adapter.setUsersNotifyDataChanged(users);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        // Lookup the recyclerview in activity layout
        RecyclerView userRecyclerView = (RecyclerView) findViewById(R.id.user_recycler_view);

        // Attach the adapter to the recyclerview to populate items
        userRecyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchUserData();
    }

    private void fetchUserData() {
        Log.d(TAG, "getUserDataTask");
        // 4. Wrap in AsyncTask and execute in background thread
        fetchUserDataAsyncTask = new FetchUserDataAsyncTask(fetchUserDataDelegate);
        fetchUserDataAsyncTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (fetchUserDataAsyncTask != null) {
            fetchUserDataAsyncTask.cancel(true);
        }
    }

    /**
     * AsyncTask
     */
    private class FetchUserDataAsyncTask extends AsyncTask<String, Void, List<User>> {

        private final Delegate delegate;

        public FetchUserDataAsyncTask(Delegate delegate) {
            this.delegate = delegate;
        }

        @Nullable
        protected List<User> doInBackground(String... strings) {
            return sendNetworkRequest();
        }

        protected void onPostExecute(List<User> result) {
            // This method is executed in the UIThread
            // with access to the result of the long running task
            delegate.onResult(result);
        }
    }

    @Nullable
    private List<User> sendNetworkRequest() {
        try {
            URL url = new URL("https://api.github.com/users/eclipsegst/followers");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try(InputStream in = urlConnection.getInputStream()) {
                return readStream(in);
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, "error: " + e);
        }

        return new ArrayList<>();
    }

    @Nullable
    private List<User> readStream(InputStream inputStream) {
        Log.d(TAG, "readStream");

        try(BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }

            JSONArray jsonArray = new JSONArray(responseStrBuilder.toString());
            return convertJSONArrayToUserList(jsonArray);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error: " + e);
        }

        return null;
    }

    private List<User> convertJSONArrayToUserList(JSONArray jsonArray) {
        Log.d(TAG, "convertJSONArrayToUserList");
        if (jsonArray == null) {
            return new ArrayList<>();
        }

        List<User> users = new ArrayList<>();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String login = jsonObject.getString("login");
                User user = new User(login);
                users.add(user);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error:" + e);
        }

        return users;
    }
}