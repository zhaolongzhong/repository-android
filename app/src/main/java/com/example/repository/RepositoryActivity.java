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

public class RepositoryActivity extends AppCompatActivity {
    private static final String TAG = "RepositoryActivity";
    private static final String GITHUB_USERNAME = ""; // TODO: Make sure to replace it with your github username

    public interface Delegate {
        void onResult(List<Repository> repositories);
    }

    @Nullable
    private FetchRepositoryDataAsyncTask fetchRepositoryDataAsyncTask;
    private final RepositoryAdapter repositoryAdapter = new RepositoryAdapter();

    private final Delegate fetchRepositoryDataDelegate = new Delegate() {
        @Override
        public void onResult(List<Repository> repositories) {
            repositoryAdapter.setDataNotifyDataChanged(repositories);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.repository_activity);

        // Lookup the recyclerview in activity layout
        RecyclerView repositoryRecyclerView = (RecyclerView) findViewById(R.id.repository_recycler_view);

        // Attach the adapter to the recyclerview to populate items
        repositoryRecyclerView.setAdapter(repositoryAdapter);
        // Set layout manager to position the items, e.g. a list or a grid
        repositoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        fetchRepositoryData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        if (fetchRepositoryDataAsyncTask != null) {
            fetchRepositoryDataAsyncTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void fetchRepositoryData() {
        Log.d(TAG, "fetchRepositoryData");
        // 4. Wrap in AsyncTask and execute in background thread
        fetchRepositoryDataAsyncTask = new FetchRepositoryDataAsyncTask(fetchRepositoryDataDelegate);
        fetchRepositoryDataAsyncTask.execute();
    }

    /**
     * AsyncTask
     */
    private class FetchRepositoryDataAsyncTask extends AsyncTask<String, Void, List<Repository>> {

        private final Delegate delegate;

        public FetchRepositoryDataAsyncTask(Delegate delegate) {
            this.delegate = delegate;
        }

        @Nullable
        protected List<Repository> doInBackground(String... strings) {
            Log.d(TAG, "doInBackground - start sendNetworkRequest, thread:" + Thread.currentThread());
            List<Repository> repositories = sendNetworkRequest();
            return repositories;
        }

        protected void onPostExecute(List<Repository> result) {
            Log.d(TAG, "onPostExecute - thread: " + Thread.currentThread());
            // This method is executed in the UIThread
            // with access to the result of the long running task
            delegate.onResult(result);
        }
    }

    @Nullable
    private List<Repository> sendNetworkRequest() {
        try {
            URL url = new URL("https://api.github.com/users/" + GITHUB_USERNAME +  "/repos");
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
    private List<Repository> readStream(InputStream inputStream) {
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

    private List<Repository> convertJSONArrayToUserList(JSONArray jsonArray) {
        Log.d(TAG, "convertJSONArrayToUserList");
        if (jsonArray == null) {
            return new ArrayList<>();
        }

        List<Repository> repositories = new ArrayList<>();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String description = jsonObject.getString("description");
                Repository repository = new Repository(name, description);
                repositories.add(repository);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error:" + e);
        }

        return repositories;
    }
}