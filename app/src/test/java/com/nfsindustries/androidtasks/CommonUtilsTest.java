package com.nfsindustries.androidtasks;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;

import com.nfsindustries.androidtasks.utils.CommonUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CommonUtils unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class CommonUtilsTest {

    @Test
    public void notNullCommonUtilsTest() throws Exception {
        Context context = mock(Context.class);
        Activity activity = mock(Activity.class);
        CommonUtils commonUtils = new CommonUtils(context, activity);
        assertNotNull(commonUtils);
    }

    @Test
    public void deviceOfflineTest() throws Exception {
        Context context = mock(Context.class);
        Activity activity = mock(Activity.class);
        ConnectivityManager connMgr = mock(ConnectivityManager.class);
        when(activity.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connMgr);
        CommonUtils commonUtils = new CommonUtils(context, activity);
        assertFalse(commonUtils.isDeviceOnline());
    }
}