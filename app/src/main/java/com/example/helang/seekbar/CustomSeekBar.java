package com.example.helang.seekbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * 自定义拖动seekBar
 */
public class CustomSeekBar extends View {
    private static final String TAG = "CustomSeekBar";
    private int radius = 26;
    private int thumbSize = 110;
    private int voiceHeight;
    private int voiceWidth;

    private int backgroundLineSize = 4;//背景线的宽度
    private int foregroundLineSize = 8;//进度的宽度

    private int lineSize;//整条背景线的长度

    private float touchY;
    private Bitmap thumbBitmap;//拖动的轮子图片
    private Bitmap voiceBitmap;//中间那个音量图片

    private Paint paint;
    private Paint circlePaint;//绘制进度条的paint

    private RectF backgroundLineRect = new RectF();//背景矩形
    private RectF foregroundLineRect = new RectF();//进度矩形

    private RectF ovalRectF;//圆的外接正方形

    private Matrix matrix  = new Matrix();//为bitmap旋转

    private float currentDegrees = 0;//当前的进度，百分比例，不带百分号

    private OnProgressListener onProgressListener;

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    public CustomSeekBar(Context context) {
        this(context,null);
    }

    public CustomSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CustomSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initRect();
        initBitmap();
        initPaint();
    }

    /**
     * init bitmap
     */
    private void initBitmap(){
        voiceBitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.voice_bg);
        thumbBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.circle_bg);
        thumbSize = thumbBitmap.getHeight();
        voiceHeight = voiceBitmap.getHeight();
        voiceWidth = voiceBitmap.getWidth();
        radius = voiceWidth/2+10;//圆的半径比中间那个音量图标的半径大一点点
    }

    /**
     * init paint
     */
    private void initPaint() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(10);

        circlePaint = new Paint();
        circlePaint.setAntiAlias(true); // 抗锯齿
        circlePaint.setDither(true); // 防抖动
        circlePaint.setStrokeWidth(2);//线的宽度
        circlePaint.setShader(null); // 清除上一次的shader
        circlePaint.setStyle(Paint.Style.STROKE); // 设置绘制的圆为空心
        circlePaint.setShadowLayer(10, 10, 10, Color.RED);
        circlePaint.setColor(Color.WHITE); // 设置圆弧的颜色
        circlePaint.setStrokeCap(Paint.Cap.ROUND); // 把每段圆弧改成圆角的
    }

    private void initRect(){
        ovalRectF  = new RectF(thumbSize/2-radius, thumbSize/2-radius,
                thumbSize/2+radius, thumbSize/2+radius); //
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLine(canvas);
        drawThumb(canvas);
        drawCircleProgress(canvas);
    }

    /**
     * draw circle
     * @param canvas
     */
    private void drawCircleProgress(Canvas canvas){
        ovalRectF.set(thumbSize/2-radius, thumbSize/2-radius,
                thumbSize/2+radius, thumbSize/2+radius);
        float alphaAngle = currentDegrees * 360.0f / 100 * 1.0f; // 计算每次画圆弧时扫过的角度，这里计算要注意分母要转为float类型，否则alphaAngle永远为0
        canvas.drawArc(ovalRectF, -90, alphaAngle, false, circlePaint);
    }

    /**
     * draw thumb
     * @param canvas
     */
    private void drawThumb(Canvas canvas){
        //添加旋转
        canvas.translate((getWidth()-thumbSize)/2,(100-currentDegrees)/100*lineSize);
        matrix.setRotate(currentDegrees*10,thumbSize/2,thumbSize/2);
        canvas.drawBitmap(thumbBitmap,matrix,null);//旋转背景
        canvas.drawBitmap(voiceBitmap, (thumbSize-voiceWidth)/2, (thumbSize-voiceHeight)/2, null);//跟随背景
    }

    /**
     * draw lines
     * @param canvas
     */
    private void drawLine(Canvas canvas){
        //绘制背景线
        backgroundLineRect.set((getWidth()-backgroundLineSize)/2,100,
                (getWidth()+backgroundLineSize)/2, getParentHeight()-thumbSize/2);
        lineSize = getParentHeight()-thumbSize/2 - 100;
        paint.setColor(Color.rgb(61,82,89));
        canvas.drawRoundRect(backgroundLineRect, 2, 2, paint);

        //绘制进度线
        paint.setColor(Color.rgb(90,189,220));
        foregroundLineRect.set((getWidth()-foregroundLineSize)/2,(getParentHeight()-100)*(100-currentDegrees)/100-thumbSize/2+100,
                (getWidth()+foregroundLineSize)/2,getParentHeight()-thumbSize/2);
        canvas.drawRoundRect(foregroundLineRect,4,4,paint);
    }

    private int getParentHeight(){
        return getHeight();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                currentDegrees += (touchY-event.getRawY())*1f/(getParentHeight())*100.0f;
                if (currentDegrees > 100){
                    currentDegrees = 100;
                }
                if (currentDegrees<0){
                    currentDegrees = 0;
                }


                if (observableEmitter != null){
                    observableEmitter.onNext(1);
                }

                if (onProgressListener != null){
                    onProgressListener.onProgressNumber(currentDegrees);
                }
                touchY = event.getRawY();
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:

                break;
        }
        return true;
    }



    private ObservableEmitter<Integer> observableEmitter;


    /**
     *使用addBackPressure,因为拖动的事件过快，会导致在处理某些较为耗时的操作时，可能会发生内存泄漏，
     *这里使用RxJava的背压操作，主动降低被观察者的发送频率。当然这个要结合你实际的业务需求
     */
    public void addbackPressure(){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                observableEmitter = emitter;
            }
        }).sample(500,TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.e(TAG,"accept::"+integer);
                        sendProgress();
                    }
                });


    }

    /**
     * 发送进度
     */
    private void sendProgress(){
        if (onProgressListener != null){
            onProgressListener.onProgress(currentDegrees);
        }
    }

    public void setCurrentDegrees(float currentDegrees){
        this.currentDegrees = currentDegrees;
        invalidate();
    }


    public interface OnProgressListener{
        void onProgress(float progress);
        void onProgressNumber(float progress);
    }

}
