package com.nfsindustries.androidtasks.utils;

import com.google.api.services.tasks.TasksScopes;

/**
 * Holds all constants used in the project
 */

public final class Constants {

    private Constants() {
        // restrict instantiation
    }

    public static final String TASK_LIST_ID = "taskListId";
    public static final String TASK_LIST_TITLE = "taskListTitle";
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    public static final String[] SCOPES = { TasksScopes.TASKS_READONLY };
}
