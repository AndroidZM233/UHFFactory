package speedata.com.uhffactory;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.bean.Tag_Data;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 张明_ on 2016/12/28.
 */

public class SearchTagDialog extends Dialog implements
        android.view.View.OnClickListener, AdapterView.OnItemClickListener {

    private Button Cancle;
    private Button Action;
    private TextView Status;
    private ListView EpcList;
    private boolean inSearch = false;
    private List<EpcDataBase> firm = new ArrayList<EpcDataBase>();
    private Handler handler = null;
    private ArrayAdapter<EpcDataBase> adapter;
    private Context cont;
    private SoundPool soundPool;
    private int soundId;
    private long scant = 0;
    private CheckBox cbb;
    private IUHFService iuhfService;
    private String model;

    public SearchTagDialog(Context context, IUHFService iuhfService, String model) {
        super(context);
        // TODO Auto-generated constructor stub
        cont = context;
        this.iuhfService = iuhfService;
        this.model = model;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setreader);

        Cancle = (Button) findViewById(R.id.btn_search_cancle);
        Cancle.setOnClickListener(this);
        Action = (Button) findViewById(R.id.btn_search_action);
        Action.setOnClickListener(this);

        cbb = (CheckBox) findViewById(R.id.checkBox_beep);

        Status = (TextView) findViewById(R.id.textView_search_status);
        EpcList = (ListView) findViewById(R.id.listView_search_epclist);
        EpcList.setOnItemClickListener(this);

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        if (soundPool == null) {
            Log.e("as3992", "Open sound failed");
        }
        soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
        Log.w("as3992_6C", "id is " + soundId);

        //inventory_start(handler) 方法参考代码

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    scant++;
                    if (!cbb.isChecked()) {
                        if (scant % 10 == 0) {
                            soundPool.play(soundId, 1, 1, 0, 0, 1);
                        }
                    }
                    ArrayList<Tag_Data> ks = (ArrayList<Tag_Data>) msg.obj;
                    int i, j;
                    for (i = 0; i < ks.size(); i++) {
                        for (j = 0; j < firm.size(); j++) {
                            if (ks.get(i).epc.equals(firm.get(j).epc)) {
                                firm.get(j).valid++;
                                firm.get(j).setRssi(ks.get(i).rssi);
                                break;
                            }
                        }
                        if (j == firm.size()) {
                            firm.add(new EpcDataBase(ks.get(i).epc, 1,
                                    ks.get(i).rssi, ks.get(i).tid));
                            if (cbb.isChecked()) {
                                soundPool.play(soundId, 1, 1, 0, 0, 1);
                            }
                        }
                    }
                }
                adapter = new ArrayAdapter<EpcDataBase>(
                        cont, android.R.layout.simple_list_item_1, firm);
                EpcList.setAdapter(adapter);
                Status.setText("Total: " + firm.size());

            }
        };

        inSearch = true;
        scant = 0;
        iuhfService.inventory_start(handler);
    }


    @Override
    protected void onStop() {
        Log.w("stop", "im stopping");
        if (inSearch) {
            iuhfService.inventory_stop();
            inSearch = false;
        }
        soundPool.release();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v == Cancle) {
            EventBus.getDefault().post(new MsgEvent("failed","盘点失败"));
            soundPool.release();
            dismiss();
        } else if (v == Action) {
            EventBus.getDefault().post(new MsgEvent("success","盘点成功"));
            soundPool.release();
            dismiss();
        }

        int inventoryStop = iuhfService.inventory_stop();
    }

    class EpcDataBase {
        String epc;
        int valid;
        String rssi;
        String tid_user;

        public EpcDataBase(String e, int v, String rssi, String tid_user) {
            // TODO Auto-generated constructor stub
            epc = e;
            valid = v;
            this.rssi = rssi;
            this.tid_user = tid_user;
        }

        public String getRssi() {
            return rssi;
        }

        public void setRssi(String rssi) {
            this.rssi = rssi;
        }

        @Override
        public String toString() {
            if (TextUtils.isEmpty(tid_user)) {
                return "EPC:" + epc + "\n"
                        + "(" + "COUNT:" + valid + ")" + " RSSI:" + rssi+"\n";
            } else {
                return "EPC:" + epc + "\n"
                        + "T/U:" + tid_user + "\n"
                        + "(" + "COUNT:" + valid + ")" + " RSSI:" + rssi+"\n";
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                            long arg3) {
    }
}
