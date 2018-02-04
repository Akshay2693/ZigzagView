package ir.beigirad.zigzagview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import static android.graphics.Bitmap.Config.ALPHA_8;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static android.graphics.PorterDuff.Mode.SRC_IN;

public class ZigzagView extends FrameLayout {
    private Path mPath = new Path();
    Paint paint;
    Paint shadowPaint;
    private float zigzagHeight;
    private float zigzagElevation;
    private int zigzagBackgroundColor;
    Bitmap mShadow;

    public ZigzagView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ZigzagView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ZigzagView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = 21)
    public ZigzagView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ZigzagView, defStyleAttr, defStyleRes);
        this.zigzagHeight = a.getDimension(R.styleable.ZigzagView_zigzagHeight, 0.0f);
        this.zigzagElevation = a.getDimension(R.styleable.ZigzagView_zigzagElevation, 0.0f);
        this.zigzagBackgroundColor = a.getColor(R.styleable.ZigzagView_zigzagBackgroundColor, Color.WHITE);
        a.recycle();

        this.paint = new Paint();
        this.paint.setColor(zigzagBackgroundColor);
        this.paint.setStyle(Style.FILL);
        this.paint.setAntiAlias(true);

        //shadowPaint
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColorFilter(new PorterDuffColorFilter(BLACK, SRC_IN));
        shadowPaint.setAlpha(51); // 20%

        zigzagElevation = Math.min(zigzagElevation, 25f);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);


        setWillNotDraw(false);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //calculate bounds
        float left = getPaddingLeft() + zigzagElevation;
        float right = getWidth() - getPaddingRight() - zigzagElevation;
        float top = getPaddingTop() + (zigzagElevation / 2);
        float bottom = getHeight() - getPaddingBottom() - zigzagElevation - (zigzagElevation / 2);
        int width = (int) (right - left);
        //int height = (int) (bottom-top);

        mPath.moveTo(right, bottom);
        mPath.lineTo(right, top);
        mPath.lineTo(left, top);
        mPath.lineTo(left, bottom);

        int h = (int) zigzagHeight;
        int seed = 2 * h;
        int count = width / seed;
        int diff = width - (seed * count);
        Log.d("diff", String.valueOf(diff));
        int sideDiff = diff / 2;


        float x = (float) (seed / 2);
        float upHeight = bottom - h;
        float downHeight = bottom;

        for (int i = 0; i < count; i++) {
            int startSeed = (i * seed) + sideDiff + (int) left;
            int endSeed = startSeed + seed;

            if (i == 0) {
                startSeed = (int) left + sideDiff;
            } else if (i == count - 1) {
                endSeed = endSeed + sideDiff;
            }

            this.mPath.lineTo(startSeed + x, upHeight);
            this.mPath.lineTo(endSeed, downHeight);
        }
        if (zigzagElevation > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            generateShadow();
            canvas.drawBitmap(mShadow, 0, zigzagElevation / 2, null);
        }

        canvas.drawPath(mPath, paint);

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void generateShadow() {
        mShadow = Bitmap.createBitmap(getWidth(), getHeight(), ALPHA_8);
        mShadow.eraseColor(TRANSPARENT);
        Canvas c = new Canvas(mShadow);
        c.drawPath(mPath, shadowPaint);

        RenderScript rs = RenderScript.create(getContext());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8(rs));
        Allocation input = Allocation.createFromBitmap(rs, mShadow);
        Allocation output = Allocation.createTyped(rs, input.getType());
        blur.setRadius(zigzagElevation);
        blur.setInput(input);
        blur.forEach(output);
        output.copyTo(mShadow);
        input.destroy();
        output.destroy();

    }
}