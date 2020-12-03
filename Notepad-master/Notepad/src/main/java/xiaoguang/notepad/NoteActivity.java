package xiaoguang.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Time;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xiaoguang.tools.Constants;
import xiaoguang.tools.SpanText;
import xiaoguang.tools.ToastUtils;

/**
 * 新增/编辑日记的activity
 */
public class NoteActivity extends Activity implements View.OnClickListener{

    private TextView timeView;
    private ImageButton btn_back;
    private ImageButton btn_save,btn_usingPic,btn_camera;
    private static final int TAKE_PHOTO = 1;
    private static final int USING_GALLERY = 2;
    public static NoteActivity addNoteActivity;
    private DbManager dbManager;

    /** 重载TextView后的Text框，与记事本的编辑框绑定 **/
    public SpanText spanContentText = null;
    /** 当前这次的图片路径 **/
    private String currImgPath = "";
    /** 当前日记的数据库id **/
    private int noteId = -1;
    /**  newOrEditState: false表示编辑 true表示新建 **/
    private boolean newOrEditState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_add);
        addNoteActivity = this;
        dbManager = DbManager.getInstance(this);

        initView();
        initEvent();

        Intent intent = getIntent();
        noteId = intent.getIntExtra(Constants.INTENT_NOTEID,-1);
        if(noteId != -1){       //读取已有
            newOrEditState = false;
            getNoteFromDb(noteId);
            spanContentText.setSelection(spanContentText.getText().toString().length());//设置光标位置
        }else{
            newOrEditState = true;
            timeView.setText(getCurTime());
            spanContentText.setText("");
        }
    }

    private void initView(){
        btn_back = (ImageButton)findViewById(R.id.id_btn_add_back);
        btn_save = (ImageButton)findViewById(R.id.id_btn_save);
        btn_usingPic = (ImageButton)findViewById(R.id.id_btn_usingpic);
        btn_camera = (ImageButton)findViewById(R.id.id_btn_start_camera);
        spanContentText = (SpanText)findViewById(R.id.id_editContent);
        timeView = (TextView)findViewById(R.id.id_viewTime);
    }

    private void initEvent(){
        btn_back.setOnClickListener(this);
        btn_save.setOnClickListener(this);
        btn_usingPic.setOnClickListener(this);
        btn_camera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_btn_add_back:  //go back
                backToMainActivity();
                break;
            case R.id.id_btn_save:
                String icoPath = getOnePicPath(spanContentText.getText().toString());
                saveNote(icoPath);
                backToMainActivity();
                break;
            case R.id.id_btn_usingpic:
                useGallery();
                break;
            case R.id.id_btn_start_camera:
                takePhoto();
                break;
        }
    }

    private void backToMainActivity() {
        Intent intent = new Intent(this,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        this.finish();
    }

    private void getNoteFromDb(int id) {
        new getDataAsyncTask().execute(id);
    }

    /**
     * 读取数据库的图片文本用，防止阻了UI，造成卡顿
     */
    class getDataAsyncTask extends AsyncTask<Integer,Void,ItemBean>{
        @Override
        protected ItemBean doInBackground(Integer... params) {
            ItemBean note = dbManager.getData(params[0]);
            return note;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ItemBean note) {
            super.onPostExecute(note);
            spanContentText.setPicText(note.getContent());
            timeView.setText(note.getTime());
        }
    }
    private void saveNote(String path){
        String text = spanContentText.getText().toString();
        if( newOrEditState == false){
            dbManager.updateData(noteId,getOnePicPath(text),text,getCurTime());
        }else {
            if(path==null) {
                path = "/no_pic/test.jpg";
                //只是为了不挂掉随便给的，加载时为空
            }
            dbManager.addNote(path, text, getCurTime());
        }
//        ToastUtils.showShort(this, "save");
    }


    /**
     * 调用相册
     */
    private void useGallery(){
        PictureSelector.create(NoteActivity.this)
                  .openGallery(PictureMimeType.ofImage())
                  .maxSelectNum(1)
                  .forResult(PictureConfig.CHOOSE_REQUEST);

    }

    /**
     * 拍照
     */
    private void takePhoto(){
        PictureSelector.create(NoteActivity.this)
                  .openGallery(PictureMimeType.ofImage())
                  .maxSelectNum(1)
                  .forResult(PictureConfig.CHOOSE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*结果回调*/
        if (requestCode == PictureConfig.CHOOSE_REQUEST) {
            if (data != null) {
                LocalMedia localMedia = PictureSelector.obtainMultipleResult(data).get(0);
                String picturePath = localMedia.getPath();
                spanContentText.addImgToText(picturePath);
            }
            MainActivity.refreshUI(); //刷新UI

        }
    }

    /**
     * 从文本中匹配出一张图片，直接选第一个匹配到的
     * @param content
     * @return 图片bitmap或者null
     */
    private String getOnePicPath(String content){
        Pattern pattern = Pattern.compile(Constants.PicPatten);
        Matcher mc = pattern.matcher(content);
        if (mc.find()){
            return mc.group();
        }
        return null;
    }

    private String getCurTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm");
        return formatter.format(new Date());
    }

    /**
     * 判断放图片的文件夹是否存在，不存在就创建
     * @param dirPath
     * @return true?false
     */
    private boolean dirExistOrMkDir(String dirPath) {
        String sdCard = Environment.getExternalStorageState();
        if (!sdCard.equals(Environment.MEDIA_MOUNTED)) {
            ToastUtils.showShort(this, "未检测到SD卡!");
            return false;
        }
        File f = new File(Environment.getExternalStorageDirectory() + "/"
                + dirPath);
        if (f.exists()) {
            return true;
        }
        boolean isSuccess = f.mkdir();
        if (isSuccess) {
            return true;
        }
        return false;
    }

    /**
     * 获取拍照的图片应该保存的路径，规则：根目录下指定目录的秒级时间+随机数.jpg
     */
    private String getCurImgPath() {
        if(!dirExistOrMkDir(Constants.IMG_DIR)){
            ToastUtils.showShort(this, "创建文件失败");
            return null;
        }
        Time time = new Time();
        Random r = new Random();
        String imgName = time.year + "" + (time.month + 1) + ""
                + time.monthDay + "" + time.minute + "" + time.second
                + "" + r.nextInt(1000) + ".jpg";
        return  Environment.getExternalStorageDirectory() + "/"
                + Constants.IMG_DIR + "/" + imgName;
    }

    /**
     * 重写按返回键，实现按两次退出效果 或者 新笔记直接返回|有内容的话询问是否放弃
     * @Override
     */
    public void onBackPressed() {
        if(noteId == -1){
            if( spanContentText.getText().length() == 0){   //getText后toString得到的不是空
                backToMainActivity();
            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("放弃编辑？");
                builder.setPositiveButton("是",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        backToMainActivity();
                    }
                });
                builder.setNegativeButton("否", null);
                builder.show();
            }
            return ;
        }
        String old = dbManager.getData(noteId).getContent();
        String newer = spanContentText.getText().toString();
        if( !old.equals(newer) ){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("放弃编辑？");
            builder.setPositiveButton("是",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    backToMainActivity();
                }
            });
            builder.setNegativeButton("否", null);
            builder.show();
        }else{
            backToMainActivity();
        }
    }
}
