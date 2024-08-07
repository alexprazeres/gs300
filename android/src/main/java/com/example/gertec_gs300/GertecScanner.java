package com.example.gertec_gs300;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import android.content.res.Resources;
import android.content.Intent;
import com.example.gertec_gs300.R;

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

import androidx.appcompat.app.AppCompatActivity;

import com.android.sublcdlibrary.SubLcdException;
import com.android.sublcdlibrary.SubLcdHelper;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

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
    List < String > consulta;
    private boolean isListening = false;

    private int cmdflag;
    private boolean isShowResult = false;
    private Toast toast;
    private final String TAG = "PrintService";
    String flagtxt = null;
    Handler handler = new Handler();
    Handler imagesHandler = new Handler();
    Handler handler1 = new Handler();
    Handler handler2 = new Handler();
    Handler handler3 = new Handler();

    private int[] imageIds = {
            R.drawable.animation1,
            R.drawable.animation2,
            R.drawable.animation3,
            R.drawable.animation4,
            R.drawable.animation5,
            R.drawable.animation6,
            R.drawable.animation7,
            R.drawable.animation8,
            R.drawable.animation9,
            R.drawable.animation10,
            R.drawable.animation11
    };

    private int index = 0;
    private int delay = 1000;
    

    private static final int MSG_REFRESH_SHOWRESULT = 0x11;
    private static final int MSG_REFRESH_NO_SHOWRESULT = 0x12;
    private static final int MSG_REFRESH_UPGRADING_SYSTEM = 0x13;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private MethodChannel methodChannel;


    public GertecScanner(Context context, MethodChannel channel) {
        this.context = context;
        SubLcdHelper.getInstance().init(context);
        SubLcdHelper.getInstance().SetCalBack(this::datatrigger);
        consulta = new ArrayList < String > ();
        this.methodChannel = channel;
    }

    public void startScan(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    consulta = new ArrayList < String > ();
                    SubLcdHelper.getInstance().sendScan();
                    cmdflag = CMD_PROTOCOL_START_SCAN;
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_SHOWRESULT, 300);
                    sendScanStatusImage("READY");
                } catch (SubLcdException e) {
                    SubLcdHelper.getInstance().release();
                    e.printStackTrace();
                }
            }
        });
    }
    

    public String getScanResult(){
        isListening = true;
        mainHandler.post(() -> {
            try {
                Thread.sleep(1000);
                if (isListening) {
                    if (!consulta.isEmpty() ) {
                        System.out.println("RETORNANDO RSULTADO ========");
                        methodChannel.invokeMethod("onListChanged", consulta);
                        stopListening();
                    }else{
                        getScanResult();
                    }
                }else{
                    System.out.println("PAROU");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        return "";
    }

    public void sendScanResult(){
         mainHandler.post(() -> {
            methodChannel.invokeMethod("onListChanged", consulta);
        });
    }

    private void stopListening() {
        isListening = false;
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
                        sendScanStatusImage("ERROR");
                        
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
                        consulta.add(s);
                        sendScanResult();
                        sendScanStatusImage("SUCCESS");
                    }
                }
            }
        });

    }

    public void sendScanStatusImage(String type){
        Bitmap bp = BitmapFactory.decodeResource(context.getResources(), R.drawable.qrcode_ready);
        switch(type){
            case "SUCCESS":
                bp = BitmapFactory.decodeResource(context.getResources(), R.drawable.qrcode_success);
                break;
            case "ERROR":
                bp = BitmapFactory.decodeResource(context.getResources(), R.drawable.qrcode_error);
                break;        
        }
        final Bitmap bpF = bp;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    SubLcdHelper.getInstance().sendBitmap(SubLcdHelper.getInstance().doRotateBitmap(bpF, 90));
                } catch (SubLcdException e) {
                    e.printStackTrace();
                }
            }
        }, 500);
    }

    public void sendReadyStatus(){
        for (int i = 0; i < imageIds.length; i++) {
            imagesHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (index < imageIds.length) {
                        try {
                            final Bitmap bpx = BitmapFactory.decodeResource(context.getResources(), imageIds[index]);
                            SubLcdHelper.getInstance().sendBitmap(SubLcdHelper.getInstance().doRotateBitmap(bpx, 90));
                        } catch (SubLcdException e) {
                            e.printStackTrace();
                        }
                        index++;
                    }
                }
            }, delay * i);
        }
    }

    private void cancelImageAnimation(){
        imagesHandler.removeCallbacks(null);
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
