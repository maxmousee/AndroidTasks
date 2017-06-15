package com.nfsindustries.androidtasks.async;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.tasks.model.*;
import com.nfsindustries.androidtasks.R;
import com.nfsindustries.androidtasks.utils.CommonUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nfsindustries.androidtasks.utils.Constants.REQUEST_AUTHORIZATION;

/**
 * An asynchronous task that handles the Google Tasks API call.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */

public class RequestTask extends AsyncTask<Void, Void, List<String>> {
    private com.google.api.services.tasks.Tasks mService = null;
    private Exception mLastError = null;
    private TextView mOutputText;
    private ProgressDialog mProgress;
    private Context context;
    private Activity activity;

    public RequestTask(GoogleAccountCredential credential, Context aContext, Activity activity,
                       TextView outputTextView, ProgressDialog progressDialog) {
        this.mOutputText = outputTextView;
        this.mProgress = progressDialog;
        this.context = aContext;
        this.activity = activity;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.tasks.Tasks.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(aContext.getString(R.string.app_name))
                .build();
    }

    /**
     * Background task to call Google Tasks API.
     * @param params no parameters needed for this task.
     */
    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    /**
     * Fetch a list of the first 10 task lists.
     * @return List of Strings describing task lists, or an empty list if
     *         there are no task lists found.
     * @throws IOException
     */
    private List<String> getDataFromApi() throws IOException {
        // List up to 10 task lists.
        List<String> taskListInfo = new ArrayList<String>();
        TaskLists result = mService.tasklists().list()
                .setMaxResults(Long.valueOf(10))
                .execute();
        List<TaskList> tasklists = result.getItems();
        if (tasklists != null) {
            for (TaskList tasklist : tasklists) {
                taskListInfo.add(String.format("%s (%s)\n",
                        tasklist.getTitle(),
                        tasklist.getId()));
                Log.d("task", tasklist.toString());
            }
        }
        return taskListInfo;
    }


    @Override
    protected void onPreExecute() {
        mOutputText.setText("");
        mProgress.show();
    }

    @Override
    protected void onPostExecute(List<String> output) {
        mProgress.hide();
        if (output == null || output.size() == 0) {
            mOutputText.setText("No results returned.");
        } else {
            output.add(0, "Data retrieved using the Google Tasks API:");
            mOutputText.setText(TextUtils.join("\n", output));
        }
    }

    @Override
    protected void onCancelled() {
        mProgress.hide();
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                CommonUtils commonUtils = new CommonUtils(this.context, this.activity);
                commonUtils.showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                ((Activity) context).startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        REQUEST_AUTHORIZATION);
            } else {
                mOutputText.setText("The following error occurred:\n"
                        + mLastError.getMessage());
            }
        } else {
            mOutputText.setText("Request cancelled.");
        }
    }
}
