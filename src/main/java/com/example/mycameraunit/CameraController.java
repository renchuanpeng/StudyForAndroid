package com.example.mycameraunit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

public class CameraController {

    private File mFile_video;
    private File mFile_photo;
    private MainActivity mActivity;
    private String mCameraId = "0";
    private Handler mBackgroundHandler;
    private CameraDevice mCameraDevice;
    private CameraManager manager;
    private AutoFitTextureView mPreviewView;
    private Size mPreviewSize = new Size(1920, 1080);
    private Size mVideoSize = new Size(1920, 1080);
    public static final float PREVIEW_SIZE_RATIO_OFFSET = 0.01f;
    public Button button;
    private float mTargetRatio = 1.333f;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequest;
    private MediaRecorder mMediaRecorder;
    private ImageReader mImageReader;
    private ImageButton btnPhoto;
    private boolean mWaterMark=false;

    public CameraController(MainActivity activity, AutoFitTextureView mPreviewView, Button button, ImageButton btnPhoto, Handler mBackgroundHandler) {
        this.mPreviewView = mPreviewView;
        this.mActivity = activity;
        this.button = button;
        this.mBackgroundHandler = mBackgroundHandler;
        this.btnPhoto = btnPhoto;
    }


    public void setVideoPath(File mFile) {
        this.mFile_video = mFile;
    }

    public void startRecordingVideo() {
        closeSession();
        choosePreviewAndCaptureSize();
        setUpMediaRecorder();
        SurfaceTexture texture = mPreviewView.getSurfaceTexture();
        texture.setDefaultBufferSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        try {
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            Surface surface = new Surface(texture);
            mCaptureRequest.addTarget(surface);
            //注意:未写此句会报错Stop Failed，MediaRecorder对象的Surface需添加到录像中去.
            mCaptureRequest.addTarget(mMediaRecorder.getSurface());

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mMediaRecorder.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    updatePreview();
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Drawable drawable = mActivity.getResources().getDrawable(R.drawable.btn_draw_onclick);
                            button.setCompoundDrawables(drawable, null, null, null);
                        }
                    });

                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createImageReader() {
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                saveImage(reader);
            }

        }, mBackgroundHandler);
//        mActivity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mPreviewView.setAspectRatio(mPreviewSize.getHeight(),mPreviewView.getWidth());
//            }
//        });
    }

    private void saveImage(ImageReader reader) {
        Image image = reader.acquireNextImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(mFile_photo);
            Bitmap bitmapSrc = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Paint mPaint = new Paint();
            mPaint.setTextSize(50);
            mPaint.setColor(Color.WHITE);
            Bitmap bitmapNew = Bitmap.createBitmap(bitmapSrc.getWidth(), bitmapSrc.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapNew);
            canvas.drawBitmap(bitmapSrc, 0, 0, null);

            if (mWaterMark) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss
                Date date = new Date(System.currentTimeMillis());
                if (mTargetRatio==1.333f) {
                    canvas.drawText(simpleDateFormat.format(date), 760, 1050, mPaint);
                }else if(mTargetRatio==1.777f){
                    canvas.drawText(simpleDateFormat.format(date), 1150, 1050, mPaint);
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] newBytes = byteArrayOutputStream.toByteArray();
                fileOutputStream.write(newBytes);
            }else {
                fileOutputStream.write(bytes);
            }
            final Bitmap thumbnail = compressbySample(bitmapNew, Bitmap.CompressFormat.JPEG, 80, 60);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnPhoto.setImageBitmap(thumbnail);
                }
            });
            Toast.makeText(mActivity, "保存路径：" + mFile_photo.toString(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void takePicture() {
        closeSession();
        mFile_photo = new File(Environment.getExternalStorageDirectory(), "/TestForAndroid/" + System.currentTimeMillis() + ".jpg");

        //SurfaceTexture texture=mPreviewView.getSurfaceTexture();
//        texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        try {
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            Surface surface=new Surface(texture);
//            mCaptureRequest.addTarget(surface);
            mCaptureRequest.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.capture(mCaptureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                            }
                        }, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    createCameraPreviewSession();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void setUpMediaRecorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mFile_video.getPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeSession() {

        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    public void stopRecordingVideo() {
        // UI
        // Stop recording
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaRecorder != null) {

                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                }
            }
        });


        //mNextVideoAbsolutePath = null;
        //startPreview();
//        closeSession();
        createCameraPreviewSession();
    }

    public void closeCamera() {
        closeSession();
        closeMediaRecorder();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void closeMediaRecorder() {
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }
        //??
        mMediaRecorder = new MediaRecorder();
        manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    choosePreviewAndCaptureSize();
                    createImageReader();
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    mCameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                    mActivity.finish();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = mPreviewView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(surfaceTexture);
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequest.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    setParams();
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        try {
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                    super.onCaptureProgressed(session, request, partialResult);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                }

                @Override
                public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                    super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                }

                @Override
                public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                    super.onCaptureSequenceAborted(session, sequenceId);
                }

                @Override
                public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
                    super.onCaptureBufferLost(session, request, target, frameNumber);
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setParams() {
        mCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        mCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
    }

    public Size getPreviewSize(Size[] mapPreview, float targetRatio, int screenWidth) {
        Size previewSize = null;
        int minOffSize = Integer.MAX_VALUE;
        for (int i = 0; i < mapPreview.length; i++) {
            float ratio = mapPreview[i].getWidth() / (float) mapPreview[i].getHeight();
            if (Math.abs(ratio - targetRatio) > PREVIEW_SIZE_RATIO_OFFSET) {
                continue;
            }
            int diff = Math.abs(mapPreview[i].getHeight() - screenWidth);
            if (diff <= minOffSize) {
                previewSize = mapPreview[i];
                minOffSize = diff;
            }
        }
        return previewSize;
    }

    private void choosePreviewAndCaptureSize() {
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = manager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap map = cameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] previewSizeMap = map.getOutputSizes(SurfaceTexture.class);
        //Size[] videoSizeMap=map.getOutputSizes(MediaRecorder.class);
        int screenWidth = getScreenWidth(mActivity.getApplicationContext());
        mPreviewSize = getPreviewSize(previewSizeMap, mTargetRatio, screenWidth);
        //mVideoSize=getVideoSize(mTargetRatio,videoSizeMap);
        mVideoSize = mPreviewSize;
        Log.d("yanweitim", "mPreviewSize.width = " + mPreviewSize.getWidth());
        Log.d("yanweitim", "mPreviewSize.height = " + mPreviewSize.getHeight());
        Log.d("yanweitim", "mVideoSize.width = " + mVideoSize.getWidth());
        Log.d("yanweitim", "mVideoSize.height = " + mVideoSize.getHeight());
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreviewView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
        });
    }

    private int getScreenWidth(Context applicationContext) {
        return applicationContext.getResources().getDisplayMetrics().widthPixels;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(mActivity,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

    public void reversal() {
        closeCamera();
        mCameraId = mCameraId == "1" ? "0" : "1";
        openCamera();
    }

    public void checkedToVideo() {
        closeCamera();
        mTargetRatio = 1.777f;
        openCamera();
    }

    public void checkedToPicture() {
        closeCamera();
        mTargetRatio = 1.333f;
        openCamera();
    }

    public void changeProportion() {
        closeCamera();
        mTargetRatio = mTargetRatio == 1.333f ? 1.777f : 1.333f;
        openCamera();
    }

    public void addWaterMark() {
        mWaterMark=mWaterMark==true?false:true;
        if (mWaterMark == true) {
            Toast.makeText(mActivity, "水印", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mActivity, "取消水印", Toast.LENGTH_SHORT).show();
        }
        closeCamera();
        openCamera();
    }

    public Bitmap compressbySample(Bitmap image, Bitmap.CompressFormat compressFormat, int requestWidth, int requestHeight) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(compressFormat, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inPurgeable = true;
        options.inJustDecodeBounds = true;//只读取图片的头信息，不去解析真是的位图
        BitmapFactory.decodeStream(isBm, null, options);
        options.inSampleSize = calculateInSampleSize(options, requestWidth, requestHeight);
        //-------------inBitmap------------------
        options.inMutable = true;
        try {
            Bitmap inBitmap = Bitmap.createBitmap(options.outWidth, options.outHeight, Bitmap.Config.RGB_565);
            if (inBitmap != null) {
                options.inBitmap = inBitmap;
            }
        } catch (OutOfMemoryError e) {
            options.inBitmap = null;
            System.gc();
        }

        //---------------------------------------

        options.inJustDecodeBounds = false;//真正的解析位图
        isBm.reset();
        Bitmap compressBitmap;
        try {
            compressBitmap = BitmapFactory.decodeStream(isBm, null, options);//把ByteArrayInputStream数据生成图片
        } catch (OutOfMemoryError e) {
            compressBitmap = null;
            System.gc();
        }

        return compressBitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int originalWidth = options.outWidth;//1080
        int originalHeight = options.outHeight;//2400
        //275
        int inSampleSize = 1;
        if (originalHeight > reqHeight || originalWidth > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) originalHeight / (float) reqHeight);
            final int widthRatio = Math.round((float) originalWidth / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }
}

