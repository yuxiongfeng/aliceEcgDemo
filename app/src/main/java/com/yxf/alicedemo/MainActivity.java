package com.yxf.alicedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.proton.ecg.algorithm.bean.RemoteAlgorithmResult;
import com.proton.ecg.algorithm.callback.EcgAlgorithmListener;
import com.proton.ecg.algorithm.callback.RemoteAlgorithmResultListener;
import com.proton.ecg.algorithm.interfaces.IEcgAlgorithm;
import com.proton.ecg.algorithm.interfaces.impl.EcgCardAlgorithm;
import com.proton.ecgcard.connector.EcgCardManager;
import com.proton.ecgcard.connector.callback.DataListener;
import com.proton.view.EcgRealTimeView;
import com.wms.ble.bean.ScanResult;
import com.wms.ble.callback.OnConnectListener;
import com.wms.ble.callback.OnScanListener;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String card_mac = "80:6F:B0:88:E3:C8";
    private EcgRealTimeView realTimeView;
    private String sn;
    private String hardVersion;
    private IEcgAlgorithm algorithm;
    //这里的appId只是临时的
    private String appId = "5407dafeec67d620";

    private long measureStartTime = -1;
    private long measureEndTime = -1;
    private int status = 0;//0:未连接  1：连接中 2：已连接  3：连接断开

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionUtils.getLocationPermission(this);
        PermissionUtils.getReadAndWritePermission(this);
        EcgCardManager.init(this);
        findViewById(R.id.idScan).setOnClickListener(this);
        findViewById(R.id.idConnect).setOnClickListener(this);
        findViewById(R.id.idDisconnect).setOnClickListener(this);
        findViewById(R.id.idRemoteResult).setOnClickListener(this);
        realTimeView = findViewById(R.id.id_ecg_view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.idScan:
                scan();
                break;
            case R.id.idConnect:
                conn();
                break;
            case R.id.idDisconnect:
                disconnect();
                break;
            case R.id.idRemoteResult:
                fetchRemoteResult();
                break;
        }
    }

    private void scan() {
        Logger.w("startScan...");
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

    private void conn() {
        DataListener dataListener = new DataListener() {
            @Override
            public void receiveEcgRawData(byte[] data) {
                super.receiveEcgRawData(data);
                realTimeView.addEcgData(data);
                if (measureStartTime != -1) {
                    measureStartTime = System.currentTimeMillis();
                }
            }

            @Override
            public void receiveSerial(String serial) {
                super.receiveSerial(serial);
                sn = serial;
            }

            @Override
            public void receiveHardVersion(String version) {
                super.receiveHardVersion(version);
                hardVersion = version;
            }
        };

        OnConnectListener connectListener = new OnConnectListener() {
            @Override
            public void onConnectSuccess() {
                super.onConnectSuccess();
                Logger.w("onConnectSuccess...");
                status = 2;
            }

            @Override
            public void onConnectSuccess(ScanResult scanResult) {
                super.onConnectSuccess(scanResult);
                Logger.w("onConnectSuccess...");
                status = 2;
            }

            @Override
            public void onConnectFaild() {
                super.onConnectFaild();
                Logger.w("onConnectFaild...");
            }

            @Override
            public void onDisconnect(boolean b) {
                super.onDisconnect(b);
                Logger.w("onDisconnect. %b", b);
            }

            @Override
            public void onDeviceNeedUpdate() {
                super.onDeviceNeedUpdate();
            }
        };
        EcgCardManager.getInstance(card_mac)
                .setDataListener(dataListener)
                .connectEcgCard(connectListener);
    }

    private void disconnect() {
        EcgCardManager.getInstance(card_mac).disConnect();
        status = 3;
    }

    private void fetchRemoteResult() {
        if (status != 2) {
            Toast.makeText(this, "已断开连接，心电数据已被清空", Toast.LENGTH_SHORT).show();
            return;
        }
        measureEndTime = System.currentTimeMillis();
        algorithm = new EcgCardAlgorithm(new EcgAlgorithmListener());
        List<Float> sourceData = EcgCardManager.getInstance(card_mac).getSourceData();

        algorithm.fetchRemoteResult(appId, card_mac, sn, hardVersion, measureStartTime, measureEndTime, sourceData, new RemoteAlgorithmResultListener() {
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
}