package com.nfsindustries.androidtasks;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.tasks.model.*;
import com.nfsindustries.androidtasks.utils.CommonUtils;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.nfsindustries.androidtasks.utils.Constants.REQUEST_ACCOUNT_PICKER;
import static com.nfsindustries.androidtasks.utils.Constants.REQUEST_AUTHORIZATION;
import static com.nfsindustries.androidtasks.utils.Constants.REQUEST_GOOGLE_PLAY_SERVICES;
import static com.nfsindustries.androidtasks.utils.Constants.REQUEST_PERMISSION_GET_ACCOUNTS;
import static com.nfsindustries.androidtasks.utils.Constants.SCOPES;
import static com.nfsindustries.androidtasks.utils.Constants.TASK_LIST_ID;
import static com.nfsindustries.androidtasks.utils.Constants.TASK_LIST_TITLE;

/**
 * TaskList Class, sets all views and make requests do get a list of tasks
 * Future improvement: remove private classes from here and create a separate class for AsyncTask
 * Future improvement: offline cache support
 */
public class TasksActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {

    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    CommonUtils commonUtils;
    ArrayAdapter<String> adapter;
    ListView listView;
    TextView taskListTitleTextView;
    String taskListId;

    /**
     * Create the activity, sets views and start requests
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskListId = getIntent().getStringExtra(TASK_LIST_ID);
        final String taskListTile = getIntent().getStringExtra(TASK_LIST_TITLE);
        Log.d("Current TaskList ID", taskListId);

        setContentView(R.layout.activity_tasks);
        listView = (ListView) findViewById(R.id.tasksListView);
        taskListTitleTextView = (TextView) findViewById(R.id.taskListTitleTextView);
        taskListTitleTextView.setText(taskListTile);
        commonUtils = new CommonUtils(this, this);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage(getString(R.string.loading));

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        getResultsFromApi();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!commonUtils.isGooglePlayServicesAvailable()) {
            commonUtils.acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!commonUtils.isDeviceOnline()) {
            final Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG);
            toast.show();
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(getString(R.string.accNamePreference), null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.contacts_permission_req),
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    final Toast toast = Toast.makeText(this, getString(R.string.install_play_services), Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(getString(R.string.accNamePreference), accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * An asynchronous task that handles the Google Tasks API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.tasks.Tasks mService = null;
        private Exception mLastError = null;

        /**
         * Start a request to get the list of tasks from a given tasklistfrom Googles servers
         * @param credential current Google Account Credential
         */
        MakeRequestTask(GoogleAccountCredential credential) {
            final HttpTransport transport = AndroidHttp.newCompatibleTransport();
            final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.tasks.Tasks.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(getString(R.string.app_name))
                    .build();
        }

        /**
         * Background task to call Google Tasks API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getTasksDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of tasks of a given list.
         * @return List of Strings describing tasks, or an empty list if
         *         there are no tasks found.
         * @throws IOException
         */
        private List<String> getTasksDataFromApi() throws IOException {
            // List all tasks
            final List<String> taskListInfo = new ArrayList<>();
            final com.google.api.services.tasks.model.Tasks defaultTaskListItems = mService.tasks().list(taskListId)
                    .execute();
            if (defaultTaskListItems != null) {
                for(Task task: defaultTaskListItems.getItems())
                    taskListInfo.add(task.getTitle());
            }
            return taskListInfo;
        }

        /**
         * Start animating the progress bar when the request task start
         */
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        /**
         * Callback when the request is finished, then parse and display results
         * @param output list of task lists titles
         */
        @Override
        protected void onPostExecute(final List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                final Toast toast = Toast.makeText(TasksActivity.this, getString(R.string.no_results),
                        Toast.LENGTH_SHORT);
                toast.show();
            } else {
                // specify an adapter
                if (output != null) {
                    adapter = new ArrayAdapter<>(TasksActivity.this,
                            android.R.layout.simple_list_item_1, output);
                    listView.setAdapter(adapter);
                }
                Log.d("DATA_RCV", output.toString());
            }
        }

        /**
         * Callback when the async task is cancelled
         * Display error messages on Toast Notifications
         */
        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    commonUtils.showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    final String errorMsg = getString(R.string.error_ocurred) + "\n"
                            + mLastError.getMessage() + "\n" + mLastError.toString();
                    final Toast toast = Toast.makeText(TasksActivity.this, errorMsg, Toast.LENGTH_LONG);
                    toast.show();
                    mLastError.printStackTrace();
                }
            } else {
                final Toast toast = Toast.makeText(TasksActivity.this, getString(R.string.request_cancelled),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}