package com.example.gertec_gs300;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;


import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_BMP_DISPLAY;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_START_SCAN;
import static com.android.sublcdlibrary.SubLcdConstant.CMD_PROTOCOL_UPDATE;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.os.Looper;
import android.os.Message;
import android.speech.tts.Voice;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sublcdlibrary.SubLcdException;
import com.android.sublcdlibrary.SubLcdHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;


public class GertecScanner implements SubLcdHelper.VuleCalBack {

    private Context context;
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String PERMISSION_READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private final String[] Permission = new String[] {
            PERMISSION_WRITE_STORAGE,
            PERMISSION_READ_STORAGE
    };
    private int cmdflag;
    private boolean isShowResult = false;
    private Toast toast;
    private final String TAG = "PrintService";
    String flagtxt = null;
    Handler handler = new Handler();
    Handler handler1 = new Handler();
    Handler handler2 = new Handler();
    Handler handler3 = new Handler();

    private static final int MSG_REFRESH_SHOWRESULT = 0x11;
    private static final int MSG_REFRESH_NO_SHOWRESULT = 0x12;
    private static final int MSG_REFRESH_UPGRADING_SYSTEM = 0x13;

    private Handler mainHandler = new Handler(Looper.getMainLooper());



    public GertecScanner(Context context) {
        this.context = context;
        SubLcdHelper.getInstance().init(context);
        SubLcdHelper.getInstance().SetCalBack(this::datatrigger);
    }

    public void startScan(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SubLcdHelper.getInstance().sendScan();
                    cmdflag = CMD_PROTOCOL_START_SCAN;
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_SHOWRESULT, 300);
                } catch (SubLcdException e) {
                    SubLcdHelper.getInstance().release();
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void datatrigger(String s, int cmd) {
        mainHandler.post(() -> {
            if (!TextUtils.isEmpty(s)) {
                if (cmd == cmdflag) {
                    if (cmd == CMD_PROTOCOL_UPDATE && s.equals(" data is incorrect")) {

                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                        
                    } else if (cmd == CMD_PROTOCOL_UPDATE && (s.equals("updatalogo") || s.equals("updatafilenameok") || s.equals("updatauImage") || s.equals("updataok"))) {
                        Log.i(TAG, "neglect");
                    } else if (cmd == CMD_PROTOCOL_UPDATE && (s.equals("Same_version"))) {

                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                       
                    } else {
                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);

                    }
                }
            }
        });

    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_SHOWRESULT:
                    isShowResult = true;
                    SubLcdHelper.getInstance().readData();
                    mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_SHOWRESULT, 100);
                    break;
                case MSG_REFRESH_NO_SHOWRESULT:
                    isShowResult = false;
                    SubLcdHelper.getInstance().readData();
                    mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_NO_SHOWRESULT, 100);
                    break;
                case MSG_REFRESH_UPGRADING_SYSTEM:

                    mHandler.sendEmptyMessage(MSG_REFRESH_SHOWRESULT);
                    break;
            }
            return false;
        }
    });
}
