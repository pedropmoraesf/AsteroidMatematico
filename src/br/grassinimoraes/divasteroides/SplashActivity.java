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
import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new SplashView());
    }

    void openGame() {
        if (isFinishing()) return;
        Intent intent=new Intent(SplashActivity.this,GameV2Activity.class);
        intent.putExtra("fromSplash",true);
        startActivity(intent);
        overridePendingTransition(0,0);
        finish();
    }

    final class SplashView extends View implements Runnable {
        static final long FADE_MS=280L;

        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pixel=new Paint();
        Bitmap bg,meteorSheet;
        Typeface font;

        volatile int targetProgress=0;
        volatile boolean loadingDone=false;
        volatile boolean cancelled=false;

        int displayProgress=0;
        long fadeStart=0L;
        boolean launched=false;
        Thread loaderThread;

        SplashView() {
            super(SplashActivity.this);
            pixel.setFilterBitmap(false);
            loadVisualAssets();
            targetProgress=5;
        }

        void loadVisualAssets() {
            try {
                InputStream in=getAssets().open("graficos/menu_cena.png");
                bg=BitmapFactory.decodeStream(in);
                in.close();
            } catch(Exception ignored) {}
            try {
                InputStream in=getAssets().open("graficos/meteoro e itens.png");
                meteorSheet=BitmapFactory.decodeStream(in);
                in.close();
            } catch(Exception ignored) {}
            try { font=Typeface.createFromAsset(getAssets(),"fnt/ALEAWB__.TTF"); }
            catch(Exception ignored) { font=Typeface.MONOSPACE; }
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            cancelled=false;
            post(this);
            startLoader();
        }

        @Override protected void onDetachedFromWindow() {
            cancelled=true;
            removeCallbacks(this);
            super.onDetachedFromWindow();
        }

        void startLoader() {
            loaderThread=new Thread(new Runnable() {
                @Override public void run() {
                    List<String> assets=new ArrayList<String>();
                    collectAssets("graficos",assets);
                    collectAssets("audio",assets);
                    collectAssets("fnt",assets);

                    int total=Math.max(1,assets.size());
                    for(int i=0;i<assets.size()&&!cancelled;i++) {
                        warmAsset(assets.get(i));
                        targetProgress=5+(int)(90f*(i+1)/total);
                    }
                    if(!cancelled) {
                        targetProgress=100;
                        loadingDone=true;
                    }
                }
            },"SplashLoader");
            loaderThread.start();
        }

        void collectAssets(String dir,List<String> out) {
            try {
                String[] names=getAssets().list(dir);
                if(names==null)return;
                for(String name:names) {
                    String path=dir+"/"+name;
                    String[] children=getAssets().list(path);
                    if(children!=null&&children.length>0)collectAssets(path,out);
                    else out.add(path);
                }
            } catch(Exception ignored) {}
        }

        void warmAsset(String path) {
            InputStream in=null;
            try {
                in=getAssets().open(path);
                byte[] buffer=new byte[8192];
                while(!cancelled&&in.read(buffer)!=-1) {
                    // Leitura real dos assets aquece o cache e o percentual acompanha o trabalho concluido.
                }
            } catch(Exception ignored) {
            } finally {
                if(in!=null)try{in.close();}catch(Exception ignored){}
            }
        }

        @Override public void run() {
            if(cancelled)return;

            if(displayProgress<targetProgress) {
                displayProgress=Math.min(targetProgress,displayProgress+1);
            }

            if(loadingDone&&displayProgress>=100&&fadeStart==0L) {
                fadeStart=SystemClock.uptimeMillis();
            }

            if(fadeStart>0L&&!launched) {
                long elapsed=SystemClock.uptimeMillis()-fadeStart;
                if(elapsed>=FADE_MS) {
                    launched=true;
                    openGame();
                    return;
                }
            }

            invalidate();
            postDelayed(this,16);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w=getWidth(),h=getHeight();

            p.setColor(Color.rgb(3,10,25));
            c.drawRect(0,0,w,h,p);
            if(bg!=null)drawCenterCrop(c,bg,new RectF(0,0,w,h),.56f);

            p.setColor(Color.argb(45,0,5,16));
            c.drawRect(0,0,w,h,p);

            drawLoading(c,w,h);

            if(fadeStart>0L) {
                float t=Math.min(1f,(SystemClock.uptimeMillis()-fadeStart)/(float)FADE_MS);
                t=1f-(1f-t)*(1f-t);
                p.setColor(Color.argb((int)(255*t),255,255,255));
                c.drawRect(0,0,w,h,p);
            }
        }

        void drawLoading(Canvas c,int w,int h) {
            float marginX=Math.max(22f,w*.028f);
            float marginY=Math.max(18f,h*.035f);
            float textSize=Math.max(20f,h*.052f);
            float meteorSize=Math.max(58f,h*.145f);

            p.setTypeface(font);
            p.setFakeBoldText(true);
            p.setTextSize(textSize);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(Color.WHITE);
            p.setShadowLayer(4f,2f,2f,Color.BLACK);

            float textWidth=p.measureText("Carregando");
            Paint.FontMetrics fm=p.getFontMetrics();
            float centerY=marginY+meteorSize*.5f;
            float baseline=centerY-(fm.ascent+fm.descent)*.5f;
            c.drawText("Carregando",marginX,baseline,p);
            p.clearShadowLayer();

            float centerX=marginX+textWidth+18f+meteorSize*.5f;
            drawRotatingMeteor(c,centerX,centerY,meteorSize);

            String percent=displayProgress+"%";
            p.setTypeface(font);
            p.setFakeBoldText(true);
            p.setTextSize(Math.max(12f,meteorSize*.22f));
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setShadowLayer(3f,1.5f,1.5f,Color.BLACK);
            Paint.FontMetrics pfm=p.getFontMetrics();
            float py=centerY-(pfm.ascent+pfm.descent)*.5f;
            c.drawText(percent,centerX,py,p);
            p.clearShadowLayer();
        }

        void drawRotatingMeteor(Canvas c,float cx,float cy,float size) {
            if(meteorSheet==null) {
                p.setColor(Color.rgb(95,88,78));
                c.drawCircle(cx,cy,size*.43f,p);
                return;
            }

            int cols=8;
            int tw=meteorSheet.getWidth()/cols;
            int th=meteorSheet.getHeight();
            int frame=(int)((SystemClock.uptimeMillis()/125L)%4L);
            Rect src=new Rect(frame*tw,0,frame*tw+tw,th);
            RectF dst=new RectF(cx-size*.5f,cy-size*.5f,cx+size*.5f,cy+size*.5f);

            float rotation=(SystemClock.uptimeMillis()%1200L)*360f/1200f;
            c.save();
            c.rotate(rotation,cx,cy);
            c.drawBitmap(meteorSheet,src,dst,pixel);
            c.restore();
        }

        void drawCenterCrop(Canvas c,Bitmap b,RectF dst,float biasY) {
            float srcRatio=b.getWidth()/(float)b.getHeight(),dstRatio=dst.width()/dst.height();
            Rect src;
            if(srcRatio>dstRatio) {
                int sw=Math.round(b.getHeight()*dstRatio);
                int left=(b.getWidth()-sw)/2;
                src=new Rect(left,0,left+sw,b.getHeight());
            } else {
                int sh=Math.round(b.getWidth()/dstRatio);
                int extra=b.getHeight()-sh;
                int top=Math.round(extra*Math.max(0,Math.min(1,biasY)));
                src=new Rect(0,top,b.getWidth(),top+sh);
            }
            c.drawBitmap(b,src,dst,pixel);
        }
    }
}
