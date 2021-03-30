package com.example.mycameraunit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private Button btnDark;
    private Button btnPicture;
    private Button btnVideo;
    private Button btnMore;
    private Button btnFace;
    private Button btnDraw;
    private ImageButton btnPhoto;
    private ImageButton btnMark;
    private ImageButton btnProportion;
    private ImageButton btnReverse;
    private AutoFitTextureView mPreviewView;
    private File mFile;
    private CameraController mCameraController;
    private Boolean isRecording=false;
    private Handler mBackgroundHandler;
    private int PICTURE_STYLE=3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        btnDraw=findViewById(R.id.btn_draw);
        btnDark=findViewById(R.id.btn_dark);
        btnFace=findViewById(R.id.btn_face);
        btnMore=findViewById(R.id.btn_more);
        btnVideo=findViewById(R.id.btn_video);
        btnReverse=findViewById(R.id.btn_reversal);
        btnPicture=findViewById(R.id.btn_picture);
        btnProportion=findViewById(R.id.btn4);
        btnMark=findViewById(R.id.btn5);
        btnPhoto=findViewById(R.id.btn_photo);
        mPreviewView=findViewById(R.id.preview_texture);
        btnDark.setOnClickListener(btnDarkClickListener);
        btnFace.setOnClickListener(btnFaceClickListener);
        btnPicture.setOnClickListener(btnPictureClickListener);
        btnVideo.setOnClickListener(btnVideoClickListener);
        btnMore.setOnClickListener(btnMoreClickListener);
        btnReverse.setOnClickListener(btnReverseClickListener);
        btnProportion.setOnClickListener(btnProportionOnclickListener);
        btnMark.setOnClickListener(btnMarkOnclickListener);


        btnDraw.setOnClickListener(pictureClickListener);
        StartBackgroundThread();
        mCameraController = new CameraController(this,mPreviewView,btnDraw,btnPhoto,mBackgroundHandler);
    }

    private View.OnClickListener btnMarkOnclickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCameraController.addWaterMark();
        }
    };

    private View.OnClickListener btnProportionOnclickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCameraController.changeProportion();
        }
    };
    private View.OnClickListener btnReverseClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCameraController.reversal();
        }
    };

    private View.OnClickListener videoClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            if (isRecording){
                Toast.makeText(MainActivity.this,"录像文件保存路劲:"+mFile.toString(),Toast.LENGTH_SHORT).show();
                isRecording=false;

                mCameraController.stopRecordingVideo();
            }else {
                mFile = new File(MainActivity.this.getExternalFilesDir(null), System.currentTimeMillis()+"_video.mp4");
                mCameraController.setVideoPath(mFile);
                isRecording=true;
                mCameraController.startRecordingVideo();
            }
        }
    };
    private View.OnClickListener pictureClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            mCameraController.takePicture();
        }
    };
    private View.OnClickListener btnDarkClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (PICTURE_STYLE){
                case 3:
                    darkInCenter();
                case 4:
                    faceInCenter();
                    break;
                case 5:
                    pictureInCenter();
                    btnDraw.setOnClickListener(pictureClickListener);
                    break;
            }

        }
    };
    private View.OnClickListener btnFaceClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (PICTURE_STYLE){
                case 1:
                    faceInCenter();
                case 2:
                    darkInCenter();
                    break;
                case 3:
                    faceInCenter();
                    break;
                case 4:
                    pictureInCenter();
                    btnDraw.setOnClickListener(pictureClickListener);
                    break;
                case 5:
                    videoInCenter();
                    btnDraw.setOnClickListener(videoClickListener);
                    break;
            }
        }
    };
    private View.OnClickListener btnPictureClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (PICTURE_STYLE){
                case 1:
                    darkInCenter();
                    break;
                case 2:
                    faceInCenter();
                    break;
                case 3:
                    pictureInCenter();
                    btnDraw.setOnClickListener(pictureClickListener);
                    break;
                case 4:
                    videoInCenter();
                    btnDraw.setOnClickListener(videoClickListener);
                    break;
                case 5:
                    moreInCenter();
                    break;
            }
        }
    };
    private View.OnClickListener btnVideoClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (PICTURE_STYLE){
                case 1:
                    faceInCenter();
                    break;
                case 2:
                    pictureInCenter();
                    btnDraw.setOnClickListener(pictureClickListener);
                    break;
                case 3:
                    videoInCenter();
                    btnDraw.setOnClickListener(videoClickListener);
                    break;
                case 4:
                    moreInCenter();
                    break;
            }
        }
    };

    private View.OnClickListener btnMoreClickListener=new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch (PICTURE_STYLE){
                case 1:
                    pictureInCenter();
                    btnDraw.setOnClickListener(pictureClickListener);
                    break;
                case 2:
                    videoInCenter();
                    btnDraw.setOnClickListener(videoClickListener);
                    break;
                case 3:
                    moreInCenter();
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        mPreviewView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            mCameraController.openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    public void StartBackgroundThread() {
        HandlerThread thread=new HandlerThread("异步线程");
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mCameraController.openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.closeCamera();
    }
    private void darkInCenter(){
        btnDark.setText("");
        btnFace.setText("");
        btnPicture.setText("夜景");
        btnVideo.setText("人像");
        btnMore.setText("拍照");
        PICTURE_STYLE=1;
    }
    private void faceInCenter(){
        btnDark.setText("");
        btnFace.setText("夜景");
        btnPicture.setText("人像");
        btnVideo.setText("拍照");
        btnMore.setText("录像");
        PICTURE_STYLE=2;
    }
    private void pictureInCenter(){
        btnDark.setText("夜景");
        btnFace.setText("人像");
        btnPicture.setText("拍照");
        btnVideo.setText("录像");
        btnMore.setText("更多");
        mCameraController.checkedToPicture();
        PICTURE_STYLE=3;
    }
    private void videoInCenter(){
        btnDark.setText("人像");
        btnFace.setText("拍照");
        btnPicture.setText("录像");
        btnVideo.setText("更多");
        btnMore.setText("");
        mCameraController.checkedToVideo();
        PICTURE_STYLE=4;
    }
    private void moreInCenter(){
        btnDark.setText("拍照");
        btnFace.setText("录像");
        btnPicture.setText("更多");
        btnVideo.setText("");
        btnMore.setText("");
        PICTURE_STYLE=5;
    }
}
