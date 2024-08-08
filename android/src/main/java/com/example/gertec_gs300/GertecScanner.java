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
import android.content.SharedPreferences;
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
    SharedPreferences sharedPref;

    private boolean isListening = false;

    private int cmdflag;
    private String scanResult = "";
    private int times = 1;
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

        this.methodChannel = channel;
        sharedPref =  context.getSharedPreferences("gs300", Context.MODE_PRIVATE);
    }

    public void startScan(){
        clearResult();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    times = 0;
                    cmdflag = CMD_PROTOCOL_START_SCAN;
                    if (SubLcdHelper.getInstance() != null){
                        SubLcdHelper.getInstance().sendScan();
                    }
                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_SHOWRESULT, 300);
                    sendScanStatusImage("READY");
                } catch(NullPointerException e){
                    e.printStackTrace();
                    Log.i(TAG, "Erro ao capturar SCAN="+e.getMessage());
                } catch (SubLcdException e) {
                    Log.i(TAG, "Erro ao capturar SCAN="+e.getMessage());
                    SubLcdHelper.getInstance().release();
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendDisplayString(String value){

        try {
            SubLcdHelper.getInstance().sendText(value, Layout.Alignment.ALIGN_NORMAL, 60);
            cmdflag = CMD_PROTOCOL_BMP_DISPLAY;
            mHandler.sendEmptyMessageDelayed(MSG_REFRESH_NO_SHOWRESULT, 300);
        } catch (SubLcdException e) {
            Log.i(TAG, "Error="+e.getMessage());
            e.printStackTrace();
        }
    }
    

    public String getScanResult(){
        Log.i(TAG, "Lendo resultado=");
        String result = sharedPref.getString("resultado", "");
        if (!TextUtils.isEmpty(result)){
            Log.i(TAG, "retornando resultado="+result);
            return result;
        }
        return result;
    }

    public void sendScanResult(String value){
        try{
            
            mainHandler.post(() -> {
                showToast("Enviando resultado:" + value);
                methodChannel.invokeMethod("onListChanged", value);
            });
         }catch (Exception e) {
            showToast("Erro ao enviar resultado"+e.getMessage());
        }
    }

    private void stopListening() {
        isListening = false;
    }

    @Override
    public void datatrigger(String s, int cmd) {
        Log.i(TAG, "data trigger string="+s);
        Log.i(TAG, "data trigger cmd="+cmd);
        mainHandler.post(() -> {
            if (!TextUtils.isEmpty(s)) {
                if (cmd == cmdflag) {
                    if (cmd == CMD_PROTOCOL_UPDATE && s.equals(" data is incorrect")) {

                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                        sendScanStatusImage("ERROR");
                        scanResult = "error";
                        
                    } else if (cmd == CMD_PROTOCOL_UPDATE && (s.equals("updatalogo") || s.equals("updatafilenameok") || s.equals("updatauImage") || s.equals("updataok"))) {
                        Log.i(TAG, "neglect");
                        sendScanStatusImage("updatelog");
                        scanResult = "error";
                    } else if (cmd == CMD_PROTOCOL_UPDATE && (s.equals("Same_version"))) {

                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "datatrigger result=" + s);
                        Log.i(TAG, "datatrigger cmd=" + cmd);
                        sendScanStatusImage("neglect");
                        scanResult = "error";
                    } else {
                        mHandler.removeMessages(MSG_REFRESH_SHOWRESULT);
                        mHandler.removeMessages(MSG_REFRESH_NO_SHOWRESULT);
                        Log.i(TAG, "success datatrigger result=" + s);
                        Log.i(TAG, "success datatrigger cmd=" + cmd);
                        showToast("Resultado====="+s);
                        scanResult = s;
                        // sendScanStatusImage("SUCCESS");
                        // showToast(s);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("resultado", s);
                        editor.apply();
                        showToast("Resultado gravado");
                    }
                }
            }
        });

    }

    public void fakeResult(){
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("resultado", "TESTE");
                editor.apply();
            }
        }, 4000);
    }

    public void clearResult(){
        scanResult = "";
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("resultado", "");
        editor.apply();
    }

    public void sendScanStatusImage(String type){
        switch(type){
            case "SUCCESS":
                showToast("QRCODE ESCANEADO COM SUCESSOX");
                // handler.postDelayed(new Runnable() {
                //     @Override
                //     public void run() {
                //         try {
                //             Bitmap bpSuccess = BitmapFactory.decodeResource(context.getResources(), R.drawable.qrcode_success);
                //             SubLcdHelper.getInstance().sendBitmap(SubLcdHelper.getInstance().doRotateBitmap(bpSuccess, 90));
                //         } catch (SubLcdException e) {
                //             e.printStackTrace();
                //         }
                //     }
                // }, 500);
                break;
            case "ERROR":
                showToast("ERRO AO LER QRCODE");
                // handler.postDelayed(new Runnable() {
                //     @Override
                //     public void run() {
                //         try {
                //             Bitmap bpError = BitmapFactory.decodeResource(context.getResources(), R.drawable.qrcode_error);
                //             SubLcdHelper.getInstance().sendBitmap(SubLcdHelper.getInstance().doRotateBitmap(bpError, 90));
                //         } catch (SubLcdException e) {
                //             e.printStackTrace();
                //         }
                //     }
                // }, 500);
                break;    
            case "READY":
                showToast("INICIANDO SCANNER");
                // handler.postDelayed(new Runnable() {
                //     @Override
                //     public void run() {
                //         try {
                //             Resources res = context.getResources();
                //             int id = R.drawable.gertec4; 
                //             Bitmap b = BitmapFactory.decodeResource(res, id);
                //             showToast(String.valueOf(b.getHeight()) + "x" + String.valueOf(b.getWidth()));
                //             //SubLcdHelper.getInstance().sendBitmap(SubLcdHelper.getInstance().doRotateBitmap(bpReady, 90));
                //             SubLcdHelper.getInstance().sendBitmap(b);
                //         } catch (Exception e) {
                //             e.printStackTrace();
                //             showToast("Erro ao Exibir ImagemX"+e.getMessage());
                //         }
                //     }
                // }, 500);
                break;
            default:
                showToast(type);    
                break;        
        }
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

    private void showToast(String text){
        mainHandler.post(() -> {
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        });
    }

    private void cancelImageAnimation(){
        imagesHandler.removeCallbacks(null);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.i(TAG, "handler trigger=" + msg);
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
