package com.example.helang.seekbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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
    private static final int radius = 65;//中间圆形进度条的半径
    private static final int thumbSize = 200;

    private int backgroundLineSize = 10;//背景线的宽度
    private int foregroundLineSize = 18;//进度的宽度

    private int lineSize;//整条背景线的长度

    private float touchY;
    private Bitmap thumbBitmap;

    private Paint paint;
    private Paint circlePaint;//绘制进度条的paint

    private RectF backgroundLineRect = new RectF();//背景矩形
    private RectF foregroundLineRect = new RectF();//进度矩形

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
        initBitmap();
        initPaint();
    }

    /**
     * init bitmap
     */
    private void initBitmap(){
        thumbBitmap = drawableToBitmap(thumbSize,getResources().getDrawable(R.drawable.circle));
    }

    /**
     * init paint
     */
    private void initPaint() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(10);

        //初始化圆形进度条的Paint
        circlePaint = new Paint();
        circlePaint.setAntiAlias(true); // 抗锯齿
        circlePaint.setDither(true); // 防抖动
        circlePaint.setStrokeWidth(10);
        circlePaint.setShader(null); // 清除上一次的shader
        circlePaint.setStyle(Paint.Style.STROKE); // 设置绘制的圆为空心
        circlePaint.setShadowLayer(10, 10, 10, Color.RED);
        circlePaint.setColor(Color.BLUE); // 设置圆弧的颜色
        circlePaint.setStrokeCap(Paint.Cap.ROUND); // 把每段圆弧改成圆角的
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
        RectF oval = new RectF(thumbSize/2-radius, thumbSize/2-radius,
                thumbSize/2+radius, thumbSize/2+radius); // 圆的外接正方形
        float alphaAngle = currentDegrees * 360.0f / 100 * 1.0f; // 计算每次画圆弧时扫过的角度，这里计算要注意分母要转为float类型，否则alphaAngle永远为0
        canvas.drawArc(oval, -90, alphaAngle, false, circlePaint);
    }

    /**
     * draw thumb
     * @param canvas
     */
    private void drawThumb(Canvas canvas){
        //添加旋转,Matrix是Bitmap旋转的关键，用于bitmap一些补间动画的操作
        canvas.translate((getWidth()-thumbSize)/2,(100-currentDegrees)/100*lineSize);
        Matrix matrix  = new Matrix();
        matrix.setRotate(currentDegrees*10,thumbSize/2,thumbSize/2);
        canvas.drawBitmap(thumbBitmap,matrix,null);//旋转背景
    }

    /**
     * draw lines
     * @param canvas
     */
    private void drawLine(Canvas canvas){
        //绘制背景线
        backgroundLineRect.set((getWidth()-backgroundLineSize)/2,thumbSize/2,
                (getWidth()+backgroundLineSize)/2, getParentHeight()-thumbSize/2);
        lineSize = getParentHeight() - thumbSize;//去掉被thumb挡住的一部分长度
        paint.setColor(Color.rgb(61,82,89));
        canvas.drawRoundRect(backgroundLineRect, backgroundLineSize/2, backgroundLineSize/2, paint);


        //绘制进度线
        paint.setColor(Color.rgb(90,189,220));//进度线的颜色
        foregroundLineRect.set((getWidth()-foregroundLineSize)/2,
                (getParentHeight()-thumbSize)*(100-currentDegrees)/100+thumbSize/2,
                (getWidth()+foregroundLineSize)/2,getParentHeight()-thumbSize/2);
        canvas.drawRoundRect(foregroundLineRect,foregroundLineSize/2,foregroundLineSize/2,paint);
    }



    /**
     * get ParentHeight
     * @return
     */
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
                touchY = event.getRawY();//记录开始的Y值
                break;
            case MotionEvent.ACTION_MOVE:
                currentDegrees += (touchY-event.getRawY())*1f/(getParentHeight())*100.0f;//当前进度值（100为满）
                if (currentDegrees > 100){//超出去的不计算，默认为100
                    currentDegrees = 100;
                }
                if (currentDegrees<0){//超出去的不计算，默认为0
                    currentDegrees = 0;
                }

                if (observableEmitter != null){//使用背压发送
                    observableEmitter.onNext(1);
                }else {//直接发送
                    sendProgress();
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
     *增加背压，防止发射拖动的事件过快,导致内存溢出
     */
    public void addBackPressure(){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                observableEmitter = emitter;
            }
        }).sample(500, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
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

    /**
     * 设置当前进度
     * @param currentDegrees
     */
    public void setCurrentDegrees(float currentDegrees){
        this.currentDegrees = currentDegrees;
        invalidate();
    }


    public interface OnProgressListener{
        void onProgress(float progress);
    }

    /**
     *  make a drawable to a bitmap
     * @param drawable drawable you want convert
     * @return converted bitmap
     */
    private Bitmap drawableToBitmap(int size, Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && bitmap.getHeight() > 0) {
                Matrix matrix = new Matrix();
                float scaleHeight = size * 1.0f / bitmapDrawable.getIntrinsicHeight();
                matrix.postScale(scaleHeight, scaleHeight);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                return bitmap;
            }
        }
        bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
