package speedata.com.uhffactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.utils.SharedXmlUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button pandian;
    private Button read;
    private TextView tvGreen;
    private TextView tvRed;
    private IUHFService iuhfService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        initView();
        SharedXmlUtil.getInstance(MainActivity.this).write("modle", "");
        try {
            iuhfService = UHFManager.getUHFService(MainActivity.this);
            newWakeLock();
            org.greenrobot.eventbus.EventBus.getDefault().register(this);
        } catch (Exception e) {
            e.printStackTrace();
            tvRed.setVisibility(View.VISIBLE);
            tvRed.setText("*模块不识别*");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hall.success");
        filter.addAction("com.geomobile.hallremove");
        registerReceiver(receiver, filter);

    }

    private void initView() {
        pandian = (Button) findViewById(R.id.pandian);
        read = (Button) findViewById(R.id.read);

        pandian.setOnClickListener(this);
        read.setOnClickListener(this);
        tvGreen = (TextView) findViewById(R.id.tvGreen);
        tvGreen.setOnClickListener(this);
        tvRed = (TextView) findViewById(R.id.tvRed);
        tvRed.setOnClickListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void mEventBus(MsgEvent msgEvent) {
        String type = msgEvent.getType();
        String msg = (String) msgEvent.getMsg();
        if ("failed".equals(type)) {
            tvRed.setVisibility(View.VISIBLE);
            tvRed.append(msg + " ");
        } else if ("success".equals(type)) {
            tvGreen.setVisibility(View.VISIBLE);
            tvGreen.append(msg + " ");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pandian:
                SearchTagDialog searchTag = new SearchTagDialog(this, iuhfService, "");
                searchTag.show();
                break;
            case R.id.read:
                String read_area = iuhfService.read_area(3, "0", "6", "0");
                if (read_area == null) {
                    EventBus.getDefault().post(new MsgEvent("failed", "读失败"));
                } else {
                    int length = read_area.length();
                    if (length != 24) {
                        EventBus.getDefault().post(new MsgEvent("failed", "读失败"));
                    } else {
                        EventBus.getDefault().post(new MsgEvent("success", "读成功"));
                    }
                }

                int writeArea = iuhfService.write_area(3, "0", "0", "6", "000011112222333344445555");
                if (writeArea != 0) {
                    EventBus.getDefault().post(new MsgEvent("failed", "写失败"));
                } else {
                    EventBus.getDefault().post(new MsgEvent("success", "写成功"));
                }
                break;
        }
    }


    private PowerManager pM = null;
    private PowerManager.WakeLock wK = null;
    private int init_progress = 0;

    private void newWakeLock() {
        init_progress++;
        pM = (PowerManager) getSystemService(POWER_SERVICE);
        if (pM != null) {
            wK = pM.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, "lock3992");
            if (wK != null) {
                wK.acquire();
                init_progress++;
            }
        }

        if (init_progress == 1) {
            Log.w("3992_6C", "wake lock init failed");
        }
    }

    private boolean openDev() {
        if (iuhfService.OpenDev() != 0) {
            new AlertDialog.Builder(this).setTitle("警告！").setMessage("上电失败").setPositiveButton("确定", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    finish();
                }
            }).show();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ----------");
        UHFManager.closeUHFService();
        wK.release();
        EventBus.getDefault().unregister(this);
        iuhfService = null;
        unregisterReceiver(receiver);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ----");
        try {
            if (openDev()) return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: --");
        try {
            iuhfService.CloseDev();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private long mkeyTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.ACTION_DOWN:
                if ((System.currentTimeMillis() - mkeyTime) > 2000) {
                    mkeyTime = System.currentTimeMillis();
                    Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * com.geomobile.hallremove
     * 监听背夹离开主机的广播
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.hall.success")) {
            } else {
                int id = android.os.Process.myPid();
                if (id != 0) {
                    android.os.Process.killProcess(id);
                }
            }
        }
    };
}
