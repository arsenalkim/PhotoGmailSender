package myapp.android.tbooki.photogmailsender;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;


public class MainActivity extends Activity {
    Button mShutter;
    MyCameraSurface mSurface;

    String mSavedFileName[] = new String[20];
    String mPath;
    String FileName;
    int mCount;

    static final String PIC_FOLDER = "SendPhotoMail";
    static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + PIC_FOLDER;

//    private static final String SMTP_AUTH_USER = "arsenalkim@gmail.com";
//    private static final String SMTP_AUTH_PWD  = "s9ck33kw!d";
    private static final String SMTP_AUTH_USER = "607ny58@gmail.com";
    private static final String SMTP_AUTH_PWD  = "80456789";
    private Handler mHandler;
    protected static final int MSG_ID = 1;
    private mThread t;
    ProgressDialog dialog ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurface = (MyCameraSurface)findViewById(R.id.previewFrame);

        mShutter = (Button)findViewById(R.id.capture);
        mShutter.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mSurface.mCamera.autoFocus(mAutoFocus);
            }
        });

        File fRoot = new File(ROOT_PATH);
        if ( !fRoot.exists() ) if ( !fRoot.mkdir() ) {
            Toast.makeText(this, "사진을 저장할 폴더가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Email
        Button button = (Button) this.findViewById(R.id.send);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                t = new mThread(MainActivity.this);
                t.start();
            }
        });

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                dialog.dismiss();
                switch (msg.what) {
                    case MSG_ID:
                        Toast.makeText(MainActivity.this, (String) msg.obj + "\n", Toast.LENGTH_SHORT);
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    // 포커싱 성공하면 촬영 허가
    Camera.AutoFocusCallback mAutoFocus = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mShutter.setEnabled(success);
            mSurface.mCamera.takePicture(null, null, mPicture);
        }
    };

    // 사진 저장
    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Calendar calendar = Calendar.getInstance();
            FileName = String.format("KwangMyung-Jeonkuk-%02d%02d%02d-%02d%02d%02d-%02d.jpg",
                    calendar.get(Calendar.YEAR) % 100, calendar.get(Calendar.MONTH)+1,
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                    mCount);

            mPath = ROOT_PATH + "/" + FileName;
            File file = new File(mPath);

            try {
                FileOutputStream fos;
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                return;
            }

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.parse("file://" + mPath);
            intent.setData(uri);
            sendBroadcast(intent);

            Toast.makeText(getApplicationContext(), "사진이 저장 되었습니다" + mPath, Toast.LENGTH_SHORT).show();

            // Alert Dialog
            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle("방금 찍은 사진을 메일에 담을까요?");
            alert.setPositiveButton("메일에 담기", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // ....
                    mSavedFileName[mCount++] = mPath;
                }
            });
//            alert.setNeutralButton("?? ?? ??", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    // ....
//                    ;
//                }
//            });
            alert.setNegativeButton("닫기", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // ....
                }
            });

            alert.show();

            camera.startPreview();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Email
    private class mThread extends Thread {
        public mThread(Context c) {
            dialog = new ProgressDialog(c);
            dialog.setTitle("잠시만요...");
            dialog.setMessage("사진을 보내는 중입니다.");
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.show();
        }

        @Override
        public void run() {
            GmailSender sender = new GmailSender(SMTP_AUTH_USER, SMTP_AUTH_PWD);
            try {
//                boolean result = sender.sendMail("607ny58@gmail.com",
//                        "arsenalkim@gmail.com",
//                        "Test....",
//                        "Testinasdfasdfasdf");
                boolean result = sender.sendMailFiles("607ny58@gmail.com",
                        "arsenalkim@gmail.com",                 // multiple recipient (separator - comma ",")
                        "광명시 전국오토바이 입니다.",
                        "안녕하세요. 광명시 전국오토바이 입니다. 사진 첨부 합니다.",
                        mCount,
                        mSavedFileName);
                Message m = new Message();
                m.what = MSG_ID;
                if (result)
                    m.obj = new String("이메일 전송 성공");
                else
                    m.obj = new String("이메일 전송 실패");
                mHandler.sendMessage(m);
            } catch (Exception e) {
                e.printStackTrace();
                Message m = new Message();
                m.what = MSG_ID;
                m.obj = new String("이메일 전송 실패");
                mHandler.sendMessage(m);
            }
        }
    }
}