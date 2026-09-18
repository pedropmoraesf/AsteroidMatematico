package br.grassinimoraes.divasteroides;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.InputStream;

public class SplashActivity extends Activity {
    private final Runnable openGame = new Runnable() {
        @Override public void run() {
            if (isFinishing()) return;
            startActivity(new Intent(SplashActivity.this, GameV2Activity.class));
            finish();
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SplashView v = new SplashView();
        setContentView(v);
        v.postDelayed(openGame, 1800);
    }

    @Override protected void onDestroy() {
        View v = getWindow().getDecorView();
        if (v != null) v.removeCallbacks(openGame);
        super.onDestroy();
    }

    final class SplashView extends View implements Runnable {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pixel = new Paint();
        Bitmap bg;
        Typeface font;

        SplashView() {
            super(SplashActivity.this);
            pixel.setFilterBitmap(false);
            try {
                InputStream in=getAssets().open("graficos/menu_cena.png");
                bg=BitmapFactory.decodeStream(in);
                in.close();
            } catch(Exception ignored) {}
            try { font=Typeface.createFromAsset(getAssets(),"fnt/ALEAWB__.TTF"); }
            catch(Exception ignored) { font=Typeface.MONOSPACE; }
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            post(this);
        }

        @Override protected void onDetachedFromWindow() {
            removeCallbacks(this);
            super.onDetachedFromWindow();
        }

        @Override public void run() {
            invalidate();
            postDelayed(this, 180);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w=getWidth(),h=getHeight();
            p.setColor(Color.rgb(3,10,25));
            c.drawRect(0,0,w,h,p);
            if(bg!=null)drawCenterCrop(c,bg,new RectF(0,0,w,h),.56f);

            p.setColor(Color.argb(52,0,5,16));
            c.drawRect(0,0,w,h,p);

            int dots=(int)((SystemClock.uptimeMillis()/320)%4);
            StringBuilder text=new StringBuilder("Carregando");
            for(int i=0;i<dots;i++)text.append('.');

            p.setTypeface(font);
            p.setFakeBoldText(true);
            p.setTextSize(Math.max(20f,h*.048f));
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(Color.WHITE);
            p.setShadowLayer(4f,2f,2f,Color.BLACK);
            c.drawText(text.toString(),Math.max(24f,w*.035f),h-Math.max(28f,h*.055f),p);
            p.clearShadowLayer();
        }

        void drawCenterCrop(Canvas c,Bitmap b,RectF dst,float biasY){
            float srcRatio=b.getWidth()/(float)b.getHeight(),dstRatio=dst.width()/dst.height();
            Rect src;
            if(srcRatio>dstRatio){
                int sw=Math.round(b.getHeight()*dstRatio);
                int left=(b.getWidth()-sw)/2;
                src=new Rect(left,0,left+sw,b.getHeight());
            }else{
                int sh=Math.round(b.getWidth()/dstRatio);
                int extra=b.getHeight()-sh;
                int top=Math.round(extra*Math.max(0,Math.min(1,biasY)));
                src=new Rect(0,top,b.getWidth(),top+sh);
            }
            c.drawBitmap(b,src,dst,pixel);
        }
    }
}
