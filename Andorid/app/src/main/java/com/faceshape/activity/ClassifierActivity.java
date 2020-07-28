package com.faceshape.activity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.faceshape.R;
import com.faceshape.utils.BitmapUtils;
import com.faceshape.utils.MagnifyEyeUtils;
import com.faceshape.utils.SensorEventUtil;
import com.faceshape.utils.SmallFaceUtils;
import com.felipecsl.gifimageview.library.GifImageView;
import com.tenginekit.AndroidConfig;
import com.tenginekit.Face;
import com.tenginekit.model.FaceLandmarkInfo;
import com.tenginekit.model.FaceLandmarkPoint;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class ClassifierActivity extends AppCompatActivity {
    private static final String TAG = "ClassifierActivity";

    private GifImageView facingGif;
    private SwitchCompat funcSwitch;
    List<FaceLandmarkInfo> faceLandmarks;
    private boolean openFunc = false;
    private final Paint circlePaint = new Paint();
    private Paint paint = new Paint();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);
        onInit();
    }

    public void onInit() {

        facingGif = findViewById(R.id.facing_gif);

        facingGif.setBytes(readStream("v3_Trim.gif"));

        funcSwitch = findViewById(R.id.func_switch);

        funcSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                openFunc = b;
            }
        });

        com.tenginekit.Face.init(getBaseContext(),
                AndroidConfig.create()
                        .setNormalMode()
                        .openFunc(AndroidConfig.Func.Detect)
                        .openFunc(AndroidConfig.Func.Landmark)
                        .setInputImageFormat(AndroidConfig.ImageFormat.RGBA)
                        .setInputImageSize(facingGif.getGifWidth(), facingGif.getGifHeight())
                        .setOutputImageSize(facingGif.getGifWidth(), facingGif.getGifHeight())
        );


        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStrokeWidth((float) 1);
        circlePaint.setStyle(Paint.Style.STROKE);

        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth((float) 1);
        paint.setStyle(Paint.Style.FILL);

        facingGif.setOnFrameAvailable(new GifImageView.OnFrameAvailable() {
            @Override
            public Bitmap onFrameAvailable(Bitmap bitmap) {
                // bitmap RGB_565

                Bitmap out_bitmap = Bitmap.createBitmap(
                        facingGif.getGifWidth(),
                        facingGif.getGifHeight(),
                        Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(out_bitmap);

                canvas.drawBitmap(bitmap, 0, 0, null);
                bitmap.recycle();

                byte[] bytes = bitmap2Bytes(out_bitmap);
                Face.FaceDetect faceDetect = com.tenginekit.Face.detect(bytes);
                if(faceDetect.getFaceCount() > 0){
                    faceLandmarks = faceDetect.landmark2d();
                    if(faceLandmarks != null){
                        if(openFunc){
                            for (int i = 0; i < faceLandmarks.size(); i++) {
                                FaceLandmarkInfo fi = faceLandmarks.get(i);
                                out_bitmap = MagnifyEyeUtils.magnifyEye(out_bitmap, getLeftEyeCenter(fi), 40, 4);
                                out_bitmap = MagnifyEyeUtils.magnifyEye(out_bitmap, getRightEyeCenter(fi), 40, 4);
                            }
                        }
                    }
                }
                return out_bitmap;
            }
        });

        facingGif.startAnimation();


    }


    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        com.tenginekit.Face.release();
    }

    Point getLeftEyeCenter(FaceLandmarkInfo fi){
        FaceLandmarkPoint p1 = fi.landmarks.get(105);
        FaceLandmarkPoint p2 = fi.landmarks.get(113);
        return new Point((int)((p1.X + p2.X) / 2), (int)((p1.Y + p2.Y) / 2));
    }

    float getLeftEyeRadius(FaceLandmarkInfo fi){
        FaceLandmarkPoint p1 = fi.landmarks.get(105);
        FaceLandmarkPoint p2 = fi.landmarks.get(113);
        return Math.abs((float) ((p1.X - p2.X))) * 5;
    }

    Point getRightEyeCenter(FaceLandmarkInfo fi){
        FaceLandmarkPoint p1 = fi.landmarks.get(121);
        FaceLandmarkPoint p2 = fi.landmarks.get(129);
        return new Point((int)((p1.X + p2.X) / 2), (int)((p1.Y + p2.Y) / 2));
    }

    float getRightEyeRadius(FaceLandmarkInfo fi){
        FaceLandmarkPoint p1 = fi.landmarks.get(121);
        FaceLandmarkPoint p2 = fi.landmarks.get(129);
        return Math.abs((float)((p1.X - p2.X))) * 5;
    }

    public List<Point> getLeftFacePoint(FaceLandmarkInfo fi){
        List<Point> list = new ArrayList<>();
        for(int i = 53; i <= 68; i++){
            Point point = new Point((int)fi.landmarks.get(i).X, (int)fi.landmarks.get(i).Y);
            list.add(point);
        }
        return list;
    }

    public List<Point> getRightFacePoint(FaceLandmarkInfo fi){
        List<Point> list = new ArrayList<>();
        for(int i = 37; i <= 52; i++){
            Point point = new Point((int)fi.landmarks.get(i).X, (int)fi.landmarks.get(i).Y);
            list.add(point);
        }
        return list;
    }

    public Point getCenterPoint(FaceLandmarkInfo fi){
        return new Point((int)fi.landmarks.get(177).X, (int)fi.landmarks.get(177).Y);
    }

    public byte[] readStream(String fileName) {
        try{
            InputStream inStream = getResources().getAssets().open(fileName);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = -1;
            while((len = inStream.read(buffer)) != -1){
                outStream.write(buffer, 0, len);
            }
            outStream.close();
            inStream.close();
            return outStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private byte[] bitmap2Bytes(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the
        return temp;
    }
}