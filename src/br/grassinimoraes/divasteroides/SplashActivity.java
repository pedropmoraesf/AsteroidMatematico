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
import java.util.HashMap;

/**
 * Splash com progresso real de pre-carregamento.
 * O meteoro usa o mesmo spritesheet do gameplay e o percentual avanca
 * conforme os recursos graficos principais sao efetivamente decodificados.
 */
public class SplashActivity extends Activity {
    private static final HashMap<String, Bitmap> PRELOADED_BITMAPS = new HashMap<String, Bitmap>();

    private static final String BG_PATH = "graficos/menu_cena.png";
    private static final String METEOR_PATH = "graficos/meteoro e itens.png";

    private static final String[] REMAINING_BITMAPS = {
            "graficos/lane_armas.png",
            "graficos/dinheiro_bonus.png",
            "graficos/subtrator.png",
            "graficos/aviao_comercial.png",
            "graficos/aviao_militar.png",
            "graficos/bomba0.png",
            "graficos/alvo_quiz.png",
            "graficos/cidade_grande.png",
            "graficos/torre.png",
            "graficos/canhao1.png",
            "graficos/projetil.png",
            "graficos/fumaca1.png",
            "graficos/moldura.png"
    };

    // 15 bitmaps usados no gameplay + a fonte = 16 etapas reais.
    private static final int TOTAL_STEPS = 16;

    static synchronized Bitmap takePreloadedBitmap(String path) {
        return PRELOADED_BITMAPS.remove(path);
    }

    private static synchronized Bitmap peekPreloadedBitmap(String path) {
        return PRELOADED_BITMAPS.get(path);
    }

    private static synchronized void cacheBitmap(String path, Bitmap bitmap) {
        if (bitmap != null) PRELOADED_BITMAPS.put(path, bitmap);
    }

    private void openGame() {
        if (isFinishing()) return;
        startActivity(new Intent(SplashActivity.this, GameV2Activity.class));
        finish();
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new SplashView());
    }

    final class SplashView extends View implements Runnable {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pixel = new Paint();

        Bitmap bg;
        Bitmap meteorSheet;
        Typeface font;

        volatile int progress = 0;
        volatile boolean loadingFinished = false;
        boolean loaderStarted = false;
        boolean gameOpening = false;
        final long splashStartedAt = SystemClock.uptimeMillis();

        SplashView() {
            super(SplashActivity.this);
            pixel.setFilterBitmap(false);
            pixel.setAntiAlias(false);

            bg = loadAndCache(BG_PATH);
            progress = percentForStep(1);

            meteorSheet = loadAndCache(METEOR_PATH);
            progress = percentForStep(2);

            try {
                font = Typeface.createFromAsset(getAssets(), "fnt/ALEAWB__.TTF");
            } catch(Exception ignored) {
                font = Typeface.MONOSPACE;
            }
            progress = percentForStep(3);
        }

        Bitmap loadAndCache(String path) {
            Bitmap cached = peekPreloadedBitmap(path);
            if (cached != null) return cached;
            try {
                InputStream in = getAssets().open(path);
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                in.close();
                cacheBitmap(path, bitmap);
                return bitmap;
            } catch(Exception ignored) {
                return null;
            }
        }

        int percentForStep(int completed) {
            return Math.min(100, Math.round(completed * 100f / TOTAL_STEPS));
        }

        void startLoader() {
            if (loaderStarted) return;
            loaderStarted = true;

            new Thread(new Runnable() {
                @Override public void run() {
                    int completed = 3;
                    for (String path : REMAINING_BITMAPS) {
                        loadAndCache(path);
                        completed++;
                        progress = percentForStep(completed);
                        postInvalidate();
                    }

                    progress = 100;
                    loadingFinished = true;
                    postInvalidate();
                }
            }, "SplashAssetLoader").start();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startLoader();
            post(this);
        }

        @Override protected void onDetachedFromWindow() {
            removeCallbacks(this);
            super.onDetachedFromWindow();
        }

        @Override public void run() {
            invalidate();

            // Evita um flash quase instantaneo em aparelhos muito rapidos,
            // sem falsificar o percentual: ao chegar a 100%, apenas segura
            // a tela por um instante antes de abrir o menu.
            if (loadingFinished && progress >= 100 && !gameOpening) {
                long elapsed = SystemClock.uptimeMillis() - splashStartedAt;
                long wait = Math.max(0L, 700L - elapsed);
                gameOpening = true;
                postDelayed(new Runnable() {
                    @Override public void run() {
                        openGame();
                    }
                }, wait);
                return;
            }

            postDelayed(this, 45);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);

            int w = getWidth();
            int h = getHeight();

            p.setColor(Color.rgb(3,10,25));
            c.drawRect(0,0,w,h,p);

            if (bg != null) {
                drawCenterCrop(c,bg,new RectF(0,0,w,h),.56f);
            }

            // Escurece levemente para o indicador continuar legivel.
            p.setColor(Color.argb(48,0,5,16));
            c.drawRect(0,0,w,h,p);

            float textX = Math.max(24f, w * .035f);
            float baseline = h - Math.max(34f, h * .065f);
            float labelSize = Math.max(22f, h * .052f);

            p.setTypeface(font);
            p.setFakeBoldText(true);
            p.setTextSize(labelSize);
            p.setTextAlign(Paint.Align.LEFT);
            p.setColor(Color.WHITE);
            p.setShadowLayer(4f,2f,2f,Color.BLACK);
            c.drawText("Carregando", textX, baseline, p);

            float labelWidth = p.measureText("Carregando");
            float meteorSize = Math.max(72f, h * .145f);
            float radius = meteorSize * .5f;
            float meteorX = textX + labelWidth + radius + Math.max(20f,w*.018f);
            float meteorY = baseline - labelSize * .36f;

            drawLoadingMeteor(c, meteorX, meteorY, meteorSize);

            // O numero fica separado da rotacao para permanecer legivel.
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.max(14f, meteorSize * .20f));
            p.setColor(Color.WHITE);
            p.setShadowLayer(3f,1.5f,1.5f,Color.BLACK);
            c.drawText(progress + "%", meteorX, meteorY + p.getTextSize() * .34f, p);
            p.clearShadowLayer();
        }

        void drawLoadingMeteor(Canvas c, float cx, float cy, float size) {
            if (meteorSheet == null) {
                p.setColor(Color.rgb(95,85,72));
                c.drawCircle(cx,cy,size*.42f,p);
                return;
            }

            int cols = 8;
            int frameWidth = meteorSheet.getWidth() / cols;
            int frameHeight = meteorSheet.getHeight();
            int frame = (int)((SystemClock.uptimeMillis() / 115L) % 4L);
            Rect src = new Rect(frame * frameWidth, 0, frame * frameWidth + frameWidth, frameHeight);
            RectF dst = new RectF(cx-size*.5f, cy-size*.5f, cx+size*.5f, cy+size*.5f);

            float rotation = (SystemClock.uptimeMillis() % 1600L) * (360f / 1600f);
            c.save();
            c.rotate(rotation,cx,cy);
            c.drawBitmap(meteorSheet,src,dst,pixel);
            c.restore();
        }

        void drawCenterCrop(Canvas c,Bitmap b,RectF dst,float biasY){
            float srcRatio=b.getWidth()/(float)b.getHeight();
            float dstRatio=dst.width()/dst.height();
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
