package com.yxf.alicedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.proton.ecg.algorithm.bean.RemoteAlgorithmResult;
import com.proton.ecg.algorithm.callback.EcgAlgorithmListener;
import com.proton.ecg.algorithm.callback.RemoteAlgorithmResultListener;
import com.proton.ecg.algorithm.interfaces.impl.EcgAlgorithm;
import com.proton.ecg.algorithm.interfaces.impl.EcgCardAlgorithm;
import com.proton.ecg.algorithm.interfaces.impl.EcgPatchAlgorithm;
import com.proton.ecgcard.connector.EcgCardManager;
import com.proton.ecgpatch.connector.EcgPatchManager;
import com.proton.view.EcgRealTimeView;
import com.wms.ble.bean.ScanResult;
import com.wms.ble.callback.OnConnectListener;
import com.wms.ble.callback.OnScanListener;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity---";
    private String patchMac = "FD:0C:45:F5:F0:C2";
    private String cardMac = "D0:2E:AB:62:4C:B8";
    private EcgRealTimeView realTimeView;
    private String sn;
    private String hardVersion;
    private EcgAlgorithm algorithm;
    //这里的appId只是临时的
    private String appId = "10167ae33846fd79";

    private long measureStartTime = -1;
    private long measureEndTime = -1;
    private int status = 0;//0:未连接  1：连接中 2：已连接  3：连接断开

    private int sample;

    private String currentMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionUtils.getLocationPermission(this);
        PermissionUtils.getReadAndWritePermission(this);
        EcgCardManager.init(this);
        EcgPatchManager.init(this);
        findViewById(R.id.idScanCard).setOnClickListener(this);
        findViewById(R.id.idScanPatch).setOnClickListener(this);
        findViewById(R.id.idConnectCard).setOnClickListener(this);
        findViewById(R.id.idConnectPatch).setOnClickListener(this);
        findViewById(R.id.idDisconnectCard).setOnClickListener(this);
        findViewById(R.id.idDisconnectPatch).setOnClickListener(this);
        findViewById(R.id.idRemoteResult).setOnClickListener(this);
        realTimeView = findViewById(R.id.id_ecg_view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.idScanCard:
                scanCard();
                break;
            case R.id.idScanPatch:
                scanPatch();
                break;
            case R.id.idConnectCard:
                connCard();
                break;
            case R.id.idConnectPatch:
                connPatch();
                break;
            case R.id.idDisconnectCard:
                disconnectCard();
                break;
            case R.id.idDisconnectPatch:
                disconnectPatch();
                break;
            case R.id.idRemoteResult:
                fetchRemoteResult();
                break;
        }
    }

    private void connCard() {
        currentMac = cardMac;
        sample = 500;
        realTimeView.setSample(sample);
        EcgCardManager.init(getApplicationContext());
        EcgCardManager ecgCardManager = EcgCardManager.getInstance(cardMac);
        ecgCardManager.setNewCard(false);//设置是否是新卡
        algorithm = new EcgCardAlgorithm(new EcgAlgorithmListener() {
            @Override
            public void receiveEcgFilterData(byte[] ecgData) {
                //cegData加密后的滤波数据
                realTimeView.addEcgData(ecgData);//加载加密数据,心电图绘制库会自己进行解密操作
                if (!realTimeView.isRunning()) {
                    realTimeView.startDrawWave();
                }
            }

            @Override
            public void receiverHeartRate(int rate) {
                Log.e(TAG, "心率: " + rate);
            }

            @Override
            public void signalInterference(int signalQualityIndex) {
                //信号相关
                Log.e(TAG, "signalInterference: " + signalQualityIndex);
            }
        });
        ecgCardManager.setDataListener(new com.proton.ecgcard.connector.callback.DataListener() {
            @Override
            public void receiveEcgRawData(byte[] data) {
                if (measureStartTime == -1) {
                    measureStartTime = System.currentTimeMillis();
                }
                //data是encrypt加密后的数据
                algorithm.processEcgData(data);//进行滤波处理，得到加密后的滤波数据
            }

            @Override
            public void receiveTouchMode(int mode) {
                Log.e(TAG, "receiveTouchMode: " + mode);
            }

            @Override
            public void receivePackageNum(int packageNum) {
                Log.e(TAG, "receivePackageNum: " + packageNum);
            }

            @Override
            public void receiveBattery(Integer battery) {
                Log.e(TAG, "receiveBattery: " + battery);
            }

            @Override
            public void receiveSerial(String serial) {
                Log.e(TAG, "receiveSerial: " + serial);
                sn = serial;
            }

            @Override
            public void receiveHardVersion(String version) {
                Log.e(TAG, "receiveHardVersion: " + version);
                hardVersion = version;
            }
        });
        ecgCardManager.connectEcgCard(new OnConnectListener() {
            @Override
            public void onConnectSuccess() {
                Log.e(TAG, "onConnectSuccess...");
                status = 2;
            }

            @Override
            public void onConnectFaild() {
                Log.e(TAG, "onConnectFailed...");
                status = 3;
            }

            @Override
            public void onDisconnect(boolean b) {
                Log.e(TAG, "onDisconnect: " + b);
                status = 3;
            }
        });
    }


    private void connPatch() {
        currentMac = patchMac;
        sample = 256;
        realTimeView.setSample(sample);
        EcgPatchManager.init(this);
        EcgPatchManager ecgPatchManager = EcgPatchManager.getInstance(patchMac);
        algorithm = new EcgPatchAlgorithm(new EcgAlgorithmListener() {

            @Override
            public void receiveEcgFilterData(byte[] ecgData) {
                realTimeView.addEcgData(ecgData);
                if (!realTimeView.isRunning()) {
                    realTimeView.startDrawWave();
                }
            }

            @Override
            public void receiverHeartRate(int rate) {
                Log.e(TAG, "心率: " + rate);
            }

            @Override
            public void signalInterference(int signalQualityIndex) {
                //信号相关
                Log.e(TAG, "signalInterference: " + signalQualityIndex);
            }
        });
        ecgPatchManager.setDataListener(new com.proton.ecgpatch.connector.callback.DataListener() {
            @Override
            public void receiveEcgRawData(byte[] data) {
                if (measureStartTime == -1) {
                    measureStartTime = System.currentTimeMillis();
                }
                algorithm.processEcgData(data);
            }

            @Override
            public void receivePackageNum(int packageNum) {
                Log.e(TAG, "包序: " + packageNum);
            }

            @Override
            public void receiveFallDown(boolean isFallDown) {
                Log.e(TAG, "是否跌倒: " + isFallDown);
            }
        });
        ecgPatchManager.connectEcgPatch(new OnConnectListener() {
            @Override
            public void onConnectSuccess() {
                Log.e(TAG, "onConnectSuccess....");
                status = 2;
            }

            @Override
            public void onConnectSuccess(ScanResult scanResult) {
                Log.e(TAG, "onConnectSuccess....scanResult" + scanResult);
                status = 2;
            }

            @Override
            public void onConnectFaild() {
                Log.e(TAG, "onConnectFailed");
                status = 3;
            }

            @Override
            public void onDisconnect(boolean b) {
                //重置算法
                algorithm.reset();
                Log.e(TAG, "onDisconnect:" + b);
            }
        });
    }

    private void disconnectCard() {
        EcgCardManager.getInstance(cardMac).disConnect();
        status = 3;
    }

    private void disconnectPatch() {
        EcgPatchManager.getInstance(patchMac).disConnect();
        status = 3;
    }

    private void fetchRemoteResult() {
        if (status != 2) {
            Toast.makeText(this, "已断开连接，心电数据已被清空", Toast.LENGTH_SHORT).show();
            return;
        }
        measureEndTime = System.currentTimeMillis();
        byte[] sourceEcgData = algorithm.fetchSourceEcgData(sample);
        algorithm.setDeviceType(sample == 500 ? 0 : 1);//出入设备类型 0:心电卡  1：心电贴
        algorithm.fetchRemoteResult(appId, currentMac, sn, hardVersion, measureStartTime, measureEndTime, sourceEcgData, new RemoteAlgorithmResultListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onLoading() {

            }

            @Override
            public void onSuccess(RemoteAlgorithmResult remoteAlgorithmResult) {
                Logger.w("remoteAlgorithmResult===%s", remoteAlgorithmResult.toString());
            }

            @Override
            public void onFail(String s) {

            }
        });
    }


    private void scanCard() {
        EcgCardManager.scanDevice(new OnScanListener() {
            @Override
            public void onScanStart() {
                super.onScanStart();
                Logger.w("onScanStart...");
            }

            @Override
            public void onDeviceFound(ScanResult scanResult) {
                super.onDeviceFound(scanResult);
                Logger.w("scan res %s", scanResult.getMacaddress());
            }

            @Override
            public void onScanStopped() {
                super.onScanStopped();
                Logger.w("onScanStopped...");
            }

            @Override
            public void onScanCanceled() {
                super.onScanCanceled();
                Logger.w("onScanCanceled...");
            }
        });
    }

    private void scanPatch() {
        EcgPatchManager.scanDevice(new OnScanListener() {
            @Override
            public void onScanStart() {
                super.onScanStart();
                Logger.w("onScanStart...");
            }

            @Override
            public void onDeviceFound(ScanResult scanResult) {
                super.onDeviceFound(scanResult);
                Logger.w("scan res %s", scanResult.getMacaddress());
            }

            @Override
            public void onScanStopped() {
                super.onScanStopped();
                Logger.w("onScanStopped...");
            }

            @Override
            public void onScanCanceled() {
                super.onScanCanceled();
                Logger.w("onScanCanceled...");
            }
        });
    }

}