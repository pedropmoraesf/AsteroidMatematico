package br.grassinimoraes.divasteroides;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Random;

import org.json.JSONObject;

/**
 * Gameplay moderno preservando a identidade visual e o comportamento da versao original.
 * A logica nova fica separada dos sprites historicos para o projeto poder evoluir
 * sem perder o estilo do jogo original.
 */
public class GameV2Activity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(new GameView(this));
    }

    static final class AudioBank {
        final Context ctx;
        final SoundPool pool = new SoundPool(12, AudioManager.STREAM_MUSIC, 0);
        final java.util.HashMap<String,Integer> ids = new java.util.HashMap<String,Integer>();
        MediaPlayer longPlayer;
        MediaPlayer musicPlayer;
        String currentMusic;
        boolean musicPaused;
        boolean enabled = true;
        AudioBank(Context c) { ctx=c; }
        void load(String name) {
            try {
                AssetFileDescriptor fd=ctx.getAssets().openFd("audio/"+name);
                int id=pool.load(fd,1); fd.close(); ids.put(name,id);
            } catch(Exception ignored) {}
        }
        void play(String name) {
            if(!enabled) return;
            Integer id=ids.get(name); if(id!=null) pool.play(id,1,1,1,0,1);
        }
        void playLong(String name) {
            stopLong(); if(!enabled) return;
            try {
                AssetFileDescriptor fd=ctx.getAssets().openFd("audio/"+name);
                longPlayer=new MediaPlayer();
                longPlayer.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
                fd.close(); longPlayer.prepare(); longPlayer.start();
            } catch(Exception ignored) { stopLong(); }
        }
        void stopLong() {
            if(longPlayer!=null){ try{longPlayer.stop();}catch(Exception ignored){} longPlayer.release(); longPlayer=null; }
        }
        void playMusic(String name,float volume) {
            if(!enabled)return;
            if(name!=null&&name.equals(currentMusic)&&musicPlayer!=null){
                if(!musicPlayer.isPlaying()&&!musicPaused){try{musicPlayer.start();}catch(Exception ignored){}}
                return;
            }
            stopMusic();
            try{
                AssetFileDescriptor fd=ctx.getAssets().openFd("audio/"+name);
                musicPlayer=new MediaPlayer();
                musicPlayer.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
                fd.close();
                musicPlayer.setLooping(true);
                musicPlayer.setVolume(volume,volume);
                musicPlayer.prepare();
                currentMusic=name;
                musicPaused=false;
                musicPlayer.start();
            }catch(Exception ignored){stopMusic();}
        }
        void playMusicOnce(String name,float volume) {
            if(!enabled)return;
            stopMusic();
            try{
                AssetFileDescriptor fd=ctx.getAssets().openFd("audio/"+name);
                musicPlayer=new MediaPlayer();
                musicPlayer.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
                fd.close();
                musicPlayer.setLooping(false);
                musicPlayer.setVolume(volume,volume);
                musicPlayer.prepare();
                currentMusic=name;
                musicPaused=false;
                musicPlayer.start();
            }catch(Exception ignored){stopMusic();}
        }
        void setMusicPaused(boolean pause){
            musicPaused=pause;
            if(musicPlayer==null)return;
            try{
                if(pause&&musicPlayer.isPlaying())musicPlayer.pause();
                else if(!pause&&!musicPlayer.isPlaying())musicPlayer.start();
            }catch(Exception ignored){}
        }
        void stopMusic(){
            if(musicPlayer!=null){try{musicPlayer.stop();}catch(Exception ignored){}musicPlayer.release();musicPlayer=null;}
            currentMusic=null;musicPaused=false;
        }
        void release(){ stopLong(); stopMusic(); pool.release(); }
    }

    static final class Particle {
        float x,y,vx,vy,life,maxLife; int color; float size;
        Particle(float x,float y,float vx,float vy,float life,int color,float size){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.maxLife=life;this.color=color;this.size=size;
        }
    }

    static final class HudNotice {
        final String text;
        final int color;
        float life;
        final float maxLife;
        HudNotice(String text,int color,float life){
            this.text=text;this.color=color;this.life=life;this.maxLife=life;
        }
    }

    static final class WorldText {
        final String text;
        final int color;
        float x,y,life;
        final float maxLife;
        WorldText(String text,float x,float y,int color,float life){
            this.text=text;this.x=x;this.y=y;this.color=color;this.life=life;this.maxLife=life;
        }
    }

    static final class PickupFly {
        final Bonus bonus;
        final int projectileIndex;
        final float startX,startY,endX,endY,maxLife;
        float life;
        PickupFly(Bonus bonus,int projectileIndex,float startX,float startY,float endX,float endY,float life){
            this.bonus=bonus;this.projectileIndex=projectileIndex;this.startX=startX;this.startY=startY;this.endX=endX;this.endY=endY;this.life=life;this.maxLife=life;
        }
    }

    enum Kind { NORMAL, ADD, SUB, MULT, DIV, BONUS }
    enum Bonus { AMMO, HYPER, MONEY, HEALTH, SHIELD, BOMB0 }

    static final class Meteor {
        float x,y,radius,speed; int value, originalValue, rewardCap, moneyEarned; Kind kind; Bonus bonus; int ammoValue;
        MeteorMathV2.Quiz quiz; boolean dead, targetBlink; float blinkTime, tailTimer;
        RectF bounds(){return new RectF(x-radius,y-radius,x+radius,y+radius);}
    }

    static final class Plane {
        float x,y,baseY,speed,bobPhase,trailTimer; boolean military,active=true; Meteor target; float strikeTimer;
        boolean bombActive; float bombX,bombY,bombVY,bombAngle;
        RectF bounds(){ return new RectF(x-(military?50:56),y-(military?17:19),x+(military?50:56),y+(military?17:19)); }
    }

    static final class PlaneFragment {
        final Rect src;
        float x,y,vx,vy,w,h,angle,spin,life=5.5f;
        boolean dead;
        PlaneFragment(Rect src,float x,float y,float vx,float vy,float w,float h,float angle,float spin){
            this.src=src;this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.w=w;this.h=h;this.angle=angle;this.spin=spin;
        }
    }

    final class GameView extends View implements Runnable {
        static final int MENU_MAIN=0, MENU_SCORE=1, MENU_OPTIONS=2, MENU_MANUAL=3;
        static final float PRE_WAVE_DURATION=4.0f;
        static final float BOMB_FLASH_DURATION=.55f;
        static final int SHOP_H_COST=400, SHOP_BOMB_COST=800;
        static final int CITY_BITMAP_W=800, CITY_BITMAP_H=220;
        static final float CITY_TOP=170f, CITY_BOTTOM=390f;
        static final float TARGET_TILE_MM=1.0f;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pixel=new Paint();
        final Paint tintPaint=new Paint();
        final Random rnd=new Random();
        final PlayerInventory inv=new PlayerInventory();
        final WaveManager waves=new WaveManager();
        final List<Meteor> meteors=new ArrayList<Meteor>();
        final List<Particle> particles=new ArrayList<Particle>();
        final List<Particle> uiParticles=new ArrayList<Particle>();
        final List<HudNotice> hudNotices=new ArrayList<HudNotice>();
        final List<WorldText> worldTexts=new ArrayList<WorldText>();
        final List<PickupFly> pickupFlights=new ArrayList<PickupFly>();
        final List<PlaneFragment> planeDebris=new ArrayList<PlaneFragment>();
        final Set<String> usedQuizExpressions=new HashSet<String>();
        final List<Integer> phaseAmmoPlan=new ArrayList<Integer>();
        final AudioBank audio;
        final RectF[] divisorRects=new RectF[MeteorMathV2.PRIMES.length];
        final RectF subMinus=new RectF(),subPlus=new RectF(),subUse=new RectF(),bombRect=new RectF(),repairRect=new RectF(),pauseRect=new RectF();
        final RectF[] menuButtons={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        final RectF[] pauseButtons={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        final RectF victoryMenuRect=new RectF(),victoryExitRect=new RectF();
        final RectF shopWeaponRect=new RectF(),shopAmmoRect=new RectF(),shopHyperRect=new RectF(),shopBombRect=new RectF(),shopNextRect=new RectF();

        Bitmap menuBg,lane,moneyImg,subImg,planeCommercial,planeMilitary,bombImg,targetImg,aimTargetSheet,planeBombImg;
        Bitmap cityImg,baseCityImg,cityAtlas,cityRuntime,turretSheet,cannonSheet,meteorSheet,projectileSheet,smokeImg,molduraSheet;
        Canvas cityRuntimeCanvas;
        Typeface gameFont;

        long last=SystemClock.uptimeMillis();
        float spawnTimer;
        float cityHealth=100;
        int score=0;
        boolean running=false, preWave=false, quizOpen=false, gameOver=false, paused=false;
        float preWaveTimer=0;
        Meteor quizMeteor;
        int quizAttempts;
        Plane commercial,military;
        int selectedMode=0;
        int menuPage=MENU_MAIN;
        boolean vibrationEnabled=true;
        boolean laserEnabled=false;
        boolean dynamicWeatherEnabled=false;
        boolean subtractionMechanic=true;
        int selectedDifficulty=0;
        boolean scoreClearedNotice=false;
        float scoreNoticeTimer=0f;
        float introWhiteFade=0f;
        float logicalW=800,logicalH=480,scaleX=1,scaleY=1;

        final float cannonX=400f, cannonY=228f;
        final float towerBaseY=390f;
        float cannonAngle=-45f;
        float cannonAnim=0f;
        float cannonDeploy=1f;
        float shotTimer=0f, shotDuration=.17f, shotStartX=400f, shotStartY=228f, shotTargetX=400f, shotTargetY=200f;
        int shotColor=Color.YELLOW;
        float fireParticleTimer=0f;
        float shieldVisualAge=0f;
        boolean shieldWasActive=false;
        int destroyedTotal=0;
        boolean scoreSaved=false;
        boolean waveClear=false;
        float waveClearTimer=0f;
        int completedWave=0;
        boolean victory=false;
        float fireworkTimer=0f;
        boolean saveNotice=false;
        float saveNoticeTimer=0f;
        boolean intermission=false;
        boolean manualFromPause=false;
        boolean bombSequence=false;
        float bombSequenceTimer=0f,bombFlashTimer=0f;
        Meteor pendingBonusTarget;
        float pendingBonusTimer=0f;
        int lastDivisorAcquired=0;
        boolean aiming=false;
        float aimX=400f,aimY=180f,aimTargetTimer=0f,aimCharge=0f,chargeParticleTimer=0f;
        long aimDownTime=0L;
        int aimTargetIndex=0,shotProjectileIndex=0,chargedProjectileValue=2;
        boolean aimCharged=false,shotWasCharged=false,shotHyper=false;
        float projectileParticleTimer=0f,hudChargeFlashTimer=0f;
        int shotProjectileValue=2;
        int manualPage=0;
        float dirtParticleTimer=0f;
        int phaseAmmoDropIndex=0;
        float phaseAmmoDropTimer=0f;

        float protectedSiteHealth=100f,protectedFireTimer=0f;
        boolean protectedSiteDestroyed=false,protectedSiteBonusAwarded=false;
        int lastProtectedBonus=0;
        float phaseActiveSeconds=0f;
        int phaseTimeBonus=0;
        String phaseClock="12:00";
        boolean phaseDay=true,phaseRain=false,phaseWeatherLoading=false;
        int phaseCloudCover=0,phaseWeatherCode=0;
        float rainTimer=0f;
        int damageCols=154,damageRows=11;
        float damageCellW=CITY_BITMAP_W/154f,damageCellH=CITY_BITMAP_H/11f;
        boolean[][] citySolid,cityDestroyed,protectedCells;

        boolean cityShaking=false;
        float cityShakeT=0f, cityShakeT2=0f, cityShakeAlpha=0f, cityShakeGamma=0f, cityShakeOffset=0f;
        int lastImpactX=400;

        GameView(Context c){
            super(c); setFocusable(true); pixel.setAntiAlias(false); pixel.setFilterBitmap(false); tintPaint.setAntiAlias(false);
            audio=new AudioBank(c); loadAssets(); loadAudio(); loadSettings();
            introWhiteFade=GameV2Activity.this.getIntent().getBooleanExtra("fromSplash",false)?.30f:0f;
            audio.playMusic("musica_menu.ogg",.42f);
        }

        void loadSettings(){
            SharedPreferences sp=getContext().getSharedPreferences("config",Context.MODE_PRIVATE);
            dynamicWeatherEnabled=sp.getBoolean("dynamicWeatherEnabled",false);
            subtractionMechanic=sp.getBoolean("subtractionMechanic",true);
        }

        void saveSettings(){
            getContext().getSharedPreferences("config",Context.MODE_PRIVATE).edit()
                    .putBoolean("dynamicWeatherEnabled",dynamicWeatherEnabled)
                    .putBoolean("subtractionMechanic",subtractionMechanic)
                    .apply();
        }

        void loadAssets(){
            menuBg=assetBitmap("graficos/menu_cena.png");
            lane=assetBitmap("graficos/lane_armas.png"); moneyImg=assetBitmap("graficos/dinheiro_bonus.png");
            subImg=assetBitmap("graficos/subtrator.png"); planeCommercial=assetBitmap("graficos/aviao_comercial.png");
            planeMilitary=assetBitmap("graficos/aviao_militar.png"); bombImg=assetBitmap("graficos/bomba0.png");
            planeBombImg=assetBitmap("graficos/bomba_aviao_militar.png");
            targetImg=assetBitmap("graficos/alvo_quiz.png"); aimTargetSheet=assetBitmap("graficos/alvo1.png");
            baseCityImg=assetBitmap("graficos/cidade_grande.png");
            cityAtlas=assetBitmap("graficos/cidades_brasil_referencia.png");
            cityImg=baseCityImg; turretSheet=assetBitmap("graficos/torre.png");
            cannonSheet=assetBitmap("graficos/canhao1.png"); meteorSheet=assetBitmap("graficos/meteoro e itens.png");
            projectileSheet=assetBitmap("graficos/projetil.png"); smokeImg=assetBitmap("graficos/fumaca1.png");
            molduraSheet=assetBitmap("graficos/moldura.png");
            try { gameFont=Typeface.createFromAsset(getContext().getAssets(),"fnt/ALEAWB__.TTF"); }
            catch(Exception ignored){ gameFont=Typeface.MONOSPACE; }
        }

        Bitmap assetBitmap(String path){
            try{InputStream in=getContext().getAssets().open(path);Bitmap b=BitmapFactory.decodeStream(in);in.close();return b;}
            catch(IOException e){return null;}
        }

        void loadAudio(){
            String[] names={"disparo_canhao.wav","divisao_correta.wav","divisao_errada.wav","explosao_meteoro.wav","explosao_grande.wav","bomba_zero.wav","impacto_cidade.wav","bonus_municao.wav","bonus_dinheiro.wav","bonus_saude.wav","bonus_escudo.wav","bonus_subtrator.wav","municao_desbloqueada.wav","subtrator_uso.wav","quiz_abre.wav","quiz_acerto.wav","quiz_erro.wav","alvo_trava.wav","aviao_militar_passagem.wav","aviao_militar_bomba.wav","aviao_comercial_passagem.wav","aviao_comercial_atingido.wav","meteoro_entrada.wav","chuva_meteoros_inicio.wav","ui_click.wav","fase_concluida.wav","game_over.wav","reconstrucao_cidade.wav","meteoro_adicao.wav","meteoro_multiplicacao.wav"};
            for(String n:names) audio.load(n);
        }

        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();post(this);}
        @Override protected void onDetachedFromWindow(){removeCallbacks(this);audio.release();super.onDetachedFromWindow();}
        @Override public void run(){
            long now=SystemClock.uptimeMillis();float dt=Math.min(.05f,(now-last)/1000f);last=now;
            update(dt);invalidate();postDelayed(this,16);
        }
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){
            scaleX=w/logicalW;scaleY=h/logicalH;
            configurePhysicalDamageGrid();
        }
        float lx(float x){return x/scaleX;} float ly(float y){return y/scaleY;}

        void resetGame(){
            meteors.clear();particles.clear();uiParticles.clear();hudNotices.clear();planeDebris.clear();usedQuizExpressions.clear();phaseAmmoPlan.clear();
            cityHealth=100;score=0;running=false;preWave=true;gameOver=false;quizOpen=false;paused=false;
            commercial=null;military=null;selectedMode=0;cityShaking=false;cityShakeOffset=0;cannonAngle=-45;cannonAnim=0;shotTimer=0;
            destroyedTotal=0;scoreSaved=false;fireParticleTimer=0;shieldVisualAge=0;shieldWasActive=false;cannonDeploy=0;
            waveClear=false;waveClearTimer=0;completedWave=0;shotColor=Color.YELLOW;
            victory=false;fireworkTimer=0;saveNotice=false;saveNoticeTimer=0;
            intermission=false;manualFromPause=false;bombSequence=false;bombSequenceTimer=0;bombFlashTimer=0;pendingBonusTarget=null;pendingBonusTimer=0;lastDivisorAcquired=0;
            aiming=false;aimTargetTimer=0;aimCharge=0;chargeParticleTimer=0;aimDownTime=0;aimTargetIndex=0;shotProjectileIndex=0;chargedProjectileValue=2;manualPage=0;dirtParticleTimer=0;
            aimCharged=false;shotWasCharged=false;shotHyper=false;projectileParticleTimer=0;hudChargeFlashTimer=0;shotProjectileValue=2;
            phaseAmmoDropIndex=0;phaseAmmoDropTimer=0f;
            protectedSiteHealth=100f;protectedSiteDestroyed=false;protectedSiteBonusAwarded=false;protectedFireTimer=0;lastProtectedBonus=0;
            waves.wave=1;waves.destroyedThisWave=0;waves.targetThisWave=WaveManager.targetForWave(1);waves.difficulty=selectedDifficulty;
            inv.resetArsenal();inv.money=0;inv.bombZero=0;inv.shieldSeconds=0;
            preWaveTimer=PRE_WAVE_DURATION;spawnTimer=0;
            preparePhase();
            playWaveMusic();
            audio.playLong("sirene_80bpm_10.wav");
        }

        void playWaveMusic(){
            if(waves.wave<=1)audio.playMusic("musica_jogo.ogg",.34f);
            else audio.playMusic(String.format(java.util.Locale.US,"onda_%02d.ogg",waves.wave),.34f);
        }

        void beginNextWave(){
            running=false;preWave=true;paused=false;waveClear=false;intermission=false;preWaveTimer=PRE_WAVE_DURATION;cannonDeploy=0;cannonAngle=-45f;
            aiming=false;aimTargetTimer=0;aimCharge=0;aimCharged=false;shotHyper=false;hudChargeFlashTimer=0;dirtParticleTimer=0;
            preparePhase();
            audio.setMusicPaused(false);playWaveMusic();audio.playLong("sirene_80bpm_10.wav");
        }

        String phaseCityNameFor(int phase){
            String[] names={"","SAO GONCALO/RJ","RIO DE JANEIRO","SAO PAULO","BELO HORIZONTE","SALVADOR","RECIFE","FORTALEZA","CURITIBA","PORTO ALEGRE","MANAUS","CAMPOS DOS GOYTACAZES","BELEM","SAO LUIS","TERESINA","NATAL","JOAO PESSOA","MACEIO","ARACAJU","CUIABA","GOIANIA","VITORIA","FLORIANOPOLIS","CAMPO GRANDE","PALMAS","BRASILIA"};
            return names[Math.max(1,Math.min(25,phase))];
        }

        String phaseCityName(){
            return phaseCityNameFor(waves.wave);
        }

        String phaseObjective(){
            if(waves.wave<=1)return "A CIDADE DEVE SOBREVIVER";
            return "O "+protectedSiteName()+" DEVE SOBREVIVER";
        }

        String phaseTrainingTip(){
            switch(waves.wave){
                case 1:return subtractionMechanic?"TREINO: REDUZA O VALOR DOS METEOROS COM OS SUBTRATORES":"TREINO: DIVIDA O VALOR ATE DESTRUIR O METEORO";
                case 2:return subtractionMechanic?"NOVO FOCO: COMPRE A ARMA 4 E PROTEJA O MONUMENTO":"NOVO FOCO: PROTEJA O MONUMENTO E USE O NOVO DIVISOR";
                case 3:return subtractionMechanic?"MUNICAO CAI DO CEU: ATIRE NO BONUS PARA RECEBER +2":"BONUS E RECURSOS PASSAM A SER MAIS IMPORTANTES";
                case 4:return "ECONOMIA: O SALDO E COMPARTILHADO ENTRE REPARO E ARMAS";
                case 5:return "ARMA H: DESTRUICAO INSTANTANEA DE UM METEORO";
                case 6:return "BOMBA x0: CARA, MAS LIMPA A TELA E FACILITA OS QUIZZES";
                default:return "PLANEJE O SALDO: MUNICAO, H, x0 E DEFESA DA CIDADE";
            }
        }

        int scheduledDivisorForPhase(){
            switch(waves.wave){
                case 2:return 5;
                case 5:return 7;
                case 8:return 11;
                case 11:return 13;
                case 14:return 17;
                default:return 0;
            }
        }

        int subtractorForPhase(int phase){
            int idx=Math.max(0,Math.min(MeteorMathV2.SUBTRACTORS.length-1,phase-1));
            return MeteorMathV2.SUBTRACTORS[idx];
        }

        int targetForPhase(int phase){
            return WaveManager.targetForWave(phase);
        }

        int weaponPurchaseCost(int weapon){
            return Math.max(40,weapon*2);
        }

        int ammoPackCost(int weapon){
            return Math.max(12,10+weapon/2);
        }

        int nextShopPhase(){
            return Math.min(WaveManager.MAX_WAVE,waves.wave+1);
        }

        int weaponUnlockPhase(int weapon){
            for(int i=0;i<MeteorMathV2.SUBTRACTORS.length;i++)if(MeteorMathV2.SUBTRACTORS[i]==weapon)return i+1;
            return 1;
        }

        int nextPurchasableWeapon(){
            int maxAvailable=subtractorForPhase(nextShopPhase());
            for(int w:MeteorMathV2.SUBTRACTORS){
                if(w<=2)continue;
                if(w>maxAvailable)break;
                if(!inv.hasWeapon(w))return w;
            }
            return maxAvailable;
        }

        int shopAmmoWeapon(){
            int w=inv.preferredFiniteWeapon();
            if(w>2)return w;
            int next=nextPurchasableWeapon();
            return next>2?next:4;
        }

        void configureAmmoDropPlan(){
            phaseAmmoPlan.clear();
            phaseAmmoDropIndex=0;
            phaseAmmoDropTimer=5.5f;
            if(!subtractionMechanic||waves.wave<2)return;

            int current=subtractorForPhase(waves.wave);
            if(waves.wave==2){
                phaseAmmoPlan.add(4);
                return;
            }

            ArrayList<Integer> previous=new ArrayList<Integer>();
            for(int w:MeteorMathV2.SUBTRACTORS){
                if(w<=2)continue;
                if(w<current || waves.wave>=8&&w<128)previous.add(w);
            }
            if(!previous.isEmpty())phaseAmmoPlan.add(previous.get(rnd.nextInt(previous.size())));
            phaseAmmoPlan.add(current);
        }

        String serializeIntList(List<Integer> values){
            StringBuilder b=new StringBuilder();
            for(Integer v:values){if(b.length()>0)b.append(',');b.append(v);}
            return b.toString();
        }

        void restoreIntList(String raw,List<Integer> out){
            out.clear();
            if(raw==null||raw.length()==0)return;
            for(String p:raw.split(",")){try{out.add(Integer.parseInt(p.trim()));}catch(Exception ignored){}}
        }

        String serializeQuizKeys(){
            StringBuilder b=new StringBuilder();
            for(String k:usedQuizExpressions){if(b.length()>0)b.append(';');b.append(k);}
            return b.toString();
        }

        void restoreQuizKeys(String raw){
            usedQuizExpressions.clear();
            if(raw==null||raw.length()==0)return;
            for(String k:raw.split(";"))if(k.length()>0)usedQuizExpressions.add(k);
        }

        String protectedSiteName(){
            String[] names={"","",
                    "CRISTO REDENTOR","PONTE ESTAIADA","PAMPULHA","ELEVADOR LACERDA","PONTE MAURICIO DE NASSAU",
                    "BEIRA-MAR","JARDIM BOTANICO","PONTE DO GUAIBA","TEATRO AMAZONAS",
                    "BASILICA DO SANTISSIMO SALVADOR","VER-O-PESO","CENTRO HISTORICO","PONTE ESTAIADA",
                    "FORTE DOS REIS MAGOS","FAROL DO CABO BRANCO","FAROL DE PONTA VERDE",
                    "PONTE ARACAJU-BARRA","IGREJA DO ROSARIO","MONUMENTO AS TRES RACAS",
                    "CONVENTO DA PENHA","PONTE HERCILIO LUZ","OBELISCO","PALACIO ARAGUAIA",
                    "CONGRESSO NACIONAL"};
            return names[Math.max(1,Math.min(25,waves.wave))];
        }

        float protectedCenterFractionForPhase(){
            float[] v={0f,0f,.43f,.61f,.44f,.44f,.58f,.42f,.45f,.70f,.47f,.87f,.45f,.75f,.42f,.55f,.88f,.85f,.56f,.43f,.60f,.68f,.58f,.57f,.60f,.62f};
            return v[Math.max(1,Math.min(25,waves.wave))];
        }

        float protectedWidthFractionForPhase(){
            float[] v={0f,0f,.10f,.16f,.12f,.14f,.20f,.18f,.12f,.23f,.12f,.10f,.14f,.18f,.17f,.20f,.10f,.12f,.20f,.12f,.12f,.15f,.25f,.13f,.18f,.22f};
            return v[Math.max(1,Math.min(25,waves.wave))];
        }

        void configurePhysicalDamageGrid(){
            android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
            float xdpi=(dm.xdpi>100f&&dm.xdpi<1000f)?dm.xdpi:385f;
            float ydpi=(dm.ydpi>100f&&dm.ydpi<1000f)?dm.ydpi:385f;
            float pxPerMmX=xdpi/25.4f;
            float pxPerMmY=ydpi/25.4f;
            float physicalW=CITY_BITMAP_W*Math.max(.01f,scaleX);
            float physicalH=CITY_BITMAP_H*Math.max(.01f,scaleY);
            damageCols=Math.max(72,Math.min(256,Math.round(physicalW/(pxPerMmX*TARGET_TILE_MM))));
            damageRows=Math.max(7,Math.min(48,Math.round(physicalH/(pxPerMmY*TARGET_TILE_MM))));
            damageCellW=CITY_BITMAP_W/(float)damageCols;
            damageCellH=CITY_BITMAP_H/(float)damageRows;
        }

        Rect atlasSourceRectForPhase(int phase){
            int idx=Math.max(0,Math.min(23,phase-2));
            boolean right=idx>=12;
            int row=idx%12;
            int[] leftTop={37,120,206,308,399,490,580,662,752,841,911,989};
            int[] leftBottom={92,182,272,360,447,539,630,718,805,884,960,1024};
            int[] rightTop={39,127,216,307,396,477,574,655,741,833,906,980};
            int[] rightBottom={92,179,269,357,447,535,626,713,796,878,949,1024};
            int refLeft=right?788:8;
            int refRight=right?1527:747;
            int refTop=right?rightTop[row]:leftTop[row];
            int refBottom=right?rightBottom[row]:leftBottom[row];
            if(phase==2)refTop=8;
            if(cityAtlas==null)return new Rect(0,0,1,1);
            float sx=cityAtlas.getWidth()/1536f;
            float sy=cityAtlas.getHeight()/1024f;
            int l=Math.max(0,Math.round(refLeft*sx));
            int r=Math.min(cityAtlas.getWidth(),Math.round(refRight*sx));
            int t=Math.max(0,Math.round(refTop*sy));
            int b=Math.min(cityAtlas.getHeight(),Math.round(refBottom*sy));
            return new Rect(l,t,Math.max(l+1,r),Math.max(t+1,b));
        }

        void clearAtlasBackground(Bitmap b){
            if(b==null)return;
            int w=b.getWidth(),h=b.getHeight();
            int[] src=new int[w*h];
            int[] out=new int[w*h];
            b.getPixels(src,0,w,0,0,w,h);
            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    int i=y*w+x;
                    int c=src[i];
                    int rr=(c>>16)&255,gg=(c>>8)&255,bb=c&255;
                    boolean black=rr<8&&gg<8&&bb<8;
                    if(!black){out[i]=c;continue;}
                    boolean touchesArt=false;
                    for(int yy=Math.max(0,y-1);yy<=Math.min(h-1,y+1)&&!touchesArt;yy++){
                        for(int xx=Math.max(0,x-1);xx<=Math.min(w-1,x+1);xx++){
                            int n=src[yy*w+xx];
                            int nr=(n>>16)&255,ng=(n>>8)&255,nb=n&255;
                            if(nr>=12||ng>=12||nb>=12){touchesArt=true;break;}
                        }
                    }
                    out[i]=touchesArt?Color.BLACK:Color.TRANSPARENT;
                }
            }
            b.setPixels(out,0,w,0,0,w,h);
        }

        void buildRuntimeCity(Bitmap source,Rect src,boolean atlasSource){
            cityRuntime=Bitmap.createBitmap(CITY_BITMAP_W,CITY_BITMAP_H,Bitmap.Config.ARGB_8888);
            cityRuntimeCanvas=new Canvas(cityRuntime);
            cityRuntimeCanvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
            if(source!=null){
                Rect sourceRect=src!=null?src:new Rect(0,0,source.getWidth(),source.getHeight());
                float scale=CITY_BITMAP_W/(float)Math.max(1,sourceRect.width());
                int destH=Math.max(1,Math.min(CITY_BITMAP_H,Math.round(sourceRect.height()*scale)));
                int destTop=CITY_BITMAP_H-destH;
                cityRuntimeCanvas.drawBitmap(source,sourceRect,new Rect(0,destTop,CITY_BITMAP_W,CITY_BITMAP_H),pixel);
                if(atlasSource){
                    clearAtlasBackground(cityRuntime);
                    if(waves.wave==2){
                        Paint erase=new Paint();
                        erase.setXfermode(new android.graphics.PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                        cityRuntimeCanvas.drawRect(0,destTop,225,Math.min(CITY_BITMAP_H,destTop+34),erase);
                        erase.setXfermode(null);
                    }
                }
            }
            cityImg=cityRuntime;
        }

        void loadCityForPhase(){
            if(waves.wave<=1){
                buildRuntimeCity(baseCityImg,null,false);
            }else if(cityAtlas!=null){
                buildRuntimeCity(cityAtlas,atlasSourceRectForPhase(waves.wave),true);
            }else{
                buildRuntimeCity(baseCityImg,null,false);
            }
        }

        void initializeDamageGrid(){
            configurePhysicalDamageGrid();
            citySolid=new boolean[damageRows][damageCols];
            cityDestroyed=new boolean[damageRows][damageCols];
            protectedCells=new boolean[damageRows][damageCols];
            if(cityRuntime==null)return;
            int[] pixels=new int[CITY_BITMAP_W*CITY_BITMAP_H];
            cityRuntime.getPixels(pixels,0,CITY_BITMAP_W,0,0,CITY_BITMAP_W,CITY_BITMAP_H);
            for(int row=0;row<damageRows;row++){
                int py0=Math.max(0,(int)Math.floor(row*damageCellH));
                int py1=Math.min(CITY_BITMAP_H,(int)Math.ceil((row+1)*damageCellH));
                for(int col=0;col<damageCols;col++){
                    int px0=Math.max(0,(int)Math.floor(col*damageCellW));
                    int px1=Math.min(CITY_BITMAP_W,(int)Math.ceil((col+1)*damageCellW));
                    int visible=0;
                    for(int py=py0;py<py1&&visible<2;py++){
                        for(int px=px0;px<px1;px++){
                            if(((pixels[py*CITY_BITMAP_W+px]>>>24)&255)>20){visible++;if(visible>=2)break;}
                        }
                    }
                    citySolid[row][col]=visible>0;
                }
            }
            configureProtectedCells();
        }

        void configureProtectedCells(){
            if(protectedCells==null)return;
            for(int r=0;r<damageRows;r++)java.util.Arrays.fill(protectedCells[r],false);
            if(waves.wave<=1)return;
            float center=protectedCenterFractionForPhase();
            float width=protectedWidthFractionForPhase();
            int c0=Math.max(0,(int)Math.floor((center-width*.5f)*damageCols));
            int c1=Math.min(damageCols-1,(int)Math.ceil((center+width*.5f)*damageCols));
            for(int r=0;r<damageRows;r++){
                for(int c=c0;c<=c1;c++){
                    if(citySolid!=null&&citySolid[r][c])protectedCells[r][c]=true;
                }
            }
        }

        double phaseLatitude(){
            double[] v={0,-22.8268,-22.9068,-23.5505,-19.9167,-12.9777,-8.0476,-3.7319,-25.4284,-30.0346,-3.1190,-21.7622,-1.4558,-2.5307,-5.0919,-5.7945,-7.1195,-9.6498,-10.9472,-15.6014,-16.6869,-20.3155,-27.5954,-20.4697,-10.2491,-15.7939};
            return v[Math.max(1,Math.min(25,waves.wave))];
        }

        double phaseLongitude(){
            double[] v={0,-43.0634,-43.1729,-46.6333,-43.9345,-38.5016,-34.8770,-38.5267,-49.2733,-51.2177,-60.0217,-41.3181,-48.4902,-44.3068,-42.8034,-35.2110,-34.8450,-35.7089,-37.0731,-56.0979,-49.2648,-40.3128,-48.5480,-54.6201,-48.3243,-47.8828};
            return v[Math.max(1,Math.min(25,waves.wave))];
        }

        boolean rainyWeatherCode(int code){
            return (code>=51&&code<=67)||(code>=80&&code<=82)||(code>=95&&code<=99);
        }

        String localDeviceClock(){
            Calendar c=Calendar.getInstance();
            return String.format(Locale.US,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
        }

        void setLocalClearEnvironment(){
            Calendar c=Calendar.getInstance();
            int hour=c.get(Calendar.HOUR_OF_DAY);
            phaseClock=localDeviceClock();
            phaseDay=hour>=6&&hour<18;
            phaseRain=false;phaseCloudCover=0;phaseWeatherCode=0;phaseWeatherLoading=false;
        }

        void setWeatherFallback(){
            phaseClock="12:00";
            phaseDay=true;
            phaseRain=false;
            phaseCloudCover=0;
            phaseWeatherCode=0;
            phaseWeatherLoading=false;
        }

        void capturePhaseEnvironment(){
            rainTimer=0f;
            if(!dynamicWeatherEnabled){
                setLocalClearEnvironment();
                return;
            }
            setWeatherFallback();
            phaseWeatherLoading=true;
            final int requestedPhase=waves.wave;
            final double lat=phaseLatitude(),lon=phaseLongitude();
            new Thread(new Runnable(){
                @Override public void run(){
                    HttpURLConnection conn=null;
                    try{
                        String u="https://api.open-meteo.com/v1/forecast?latitude="+lat+
                                "&longitude="+lon+
                                "&current=weather_code,is_day,cloud_cover,rain,showers&timezone=auto";
                        conn=(HttpURLConnection)new URL(u).openConnection();
                        conn.setConnectTimeout(3500);
                        conn.setReadTimeout(3500);
                        conn.setRequestMethod("GET");
                        BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
                        StringBuilder body=new StringBuilder();
                        String line;
                        while((line=br.readLine())!=null)body.append(line);
                        br.close();
                        JSONObject current=new JSONObject(body.toString()).getJSONObject("current");
                        final int code=current.optInt("weather_code",0);
                        final int isDay=current.optInt("is_day",1);
                        final int cloud=current.optInt("cloud_cover",0);
                        final double rain=current.optDouble("rain",0)+current.optDouble("showers",0);
                        String time=current.optString("time","2026-01-01T12:00");
                        final String clock=time.length()>=16?time.substring(11,16):"12:00";
                        post(new Runnable(){
                            @Override public void run(){
                                if(waves.wave!=requestedPhase)return;
                                phaseClock=clock;
                                phaseDay=isDay==1;
                                phaseWeatherCode=code;
                                phaseCloudCover=Math.max(0,Math.min(100,cloud));
                                phaseRain=rain>0.01||rainyWeatherCode(code);
                                phaseWeatherLoading=false;
                            }
                        });
                    }catch(Exception ignored){
                        post(new Runnable(){
                            @Override public void run(){
                                if(waves.wave==requestedPhase)setWeatherFallback();
                            }
                        });
                    }finally{
                        if(conn!=null)conn.disconnect();
                    }
                }
            }).start();
        }

        String phaseWeatherLabel(){
            if(!dynamicWeatherEnabled)return "CLIMA LOCAL OFF";
            if(phaseWeatherLoading)return "CLIMA...";
            if(phaseRain)return "CHUVA";
            if(phaseCloudCover>=65)return "NUBLADO";
            if(phaseCloudCover>=25)return "PARCIAL";
            return "CEU LIMPO";
        }

        void updateRain(float dt){
            if(!phaseRain||(!running&&!preWave))return;
            rainTimer-=dt;
            if(rainTimer>0)return;
            rainTimer=.018f;
            for(int i=0;i<3;i++){
                float x=rnd.nextFloat()*800f;
                float y=-8-rnd.nextFloat()*50f;
                particles.add(new Particle(x,y,-8+rnd.nextFloat()*16,185+rnd.nextFloat()*55,
                        1.8f,Color.rgb(145,195,235),1.0f));
            }
        }

        void preparePhase(){
            phaseActiveSeconds=0f;phaseTimeBonus=0;
            // Cada fase representa uma cidade diferente: a nova cidade começa íntegra.
            cityHealth=100f;
            capturePhaseEnvironment();
            if(subtractionMechanic){
                lastDivisorAcquired=0;
                configureAmmoDropPlan();
            }else{
                phaseAmmoPlan.clear();phaseAmmoDropIndex=0;phaseAmmoDropTimer=0f;
                lastDivisorAcquired=scheduledDivisorForPhase();
                if(lastDivisorAcquired>0)inv.unlockDivisor(lastDivisorAcquired);
            }
            loadCityForPhase();
            initializeDamageGrid();
            protectedSiteHealth=100f;
            protectedSiteDestroyed=false;
            protectedSiteBonusAwarded=false;
            protectedFireTimer=0f;
            lastProtectedBonus=0;
        }

        void startIntermission(){
            intermission=true;running=false;preWave=false;paused=false;cannonAngle=-45f;
            audio.setMusicPaused(true);
        }

        void continueFromShop(){
            if(!intermission)return;
            intermission=false;
            waves.nextWave();
            beginNextWave();
        }

        void buyCurrentWeapon(){
            if(!subtractionMechanic)return;
            int weapon=nextPurchasableWeapon();
            if(weapon<=2)return;
            if(inv.hasWeapon(weapon)){inv.selectedWeapon=weapon;addHudNotice("ARMA "+weapon+" SELECIONADA",Color.CYAN);return;}
            int cost=weaponPurchaseCost(weapon);
            if(inv.money<cost){addHudNotice("SALDO INSUFICIENTE PARA ARMA "+weapon,Color.RED);return;}
            inv.money-=cost;
            int unlockPhase=weaponUnlockPhase(weapon);
            int initial=3*targetForPhase(unlockPhase);
            inv.unlockWeapon(weapon,initial);
            inv.selectedWeapon=weapon;
            addHudNotice("-R$ "+cost+"  ARMA "+weapon+" +"+initial,Color.YELLOW);
            audio.play("municao_desbloqueada.wav");
        }

        void buyAmmoPack(){
            if(!subtractionMechanic)return;
            int weapon=shopAmmoWeapon();
            if(weapon<=2||!inv.hasWeapon(weapon))return;
            int cost=ammoPackCost(weapon);
            if(inv.money<cost){addHudNotice("SALDO INSUFICIENTE PARA MUNICAO",Color.RED);return;}
            inv.money-=cost;
            inv.addWeaponAmmo(weapon,2);
            addHudNotice("-R$ "+cost+"  MUNICAO "+weapon+" +2",Color.YELLOW);
            audio.play("bonus_municao.wav");
        }

        void buyHyper(){
            if(inv.money<SHOP_H_COST){addHudNotice("SALDO INSUFICIENTE PARA H",Color.RED);return;}
            inv.money-=SHOP_H_COST;
            inv.addHyper(1);
            addHudNotice("-R$ "+SHOP_H_COST+"  H +1",Color.CYAN);
            audio.play("bonus_municao.wav");
        }

        void buyBomb(){
            if(inv.money<SHOP_BOMB_COST){addHudNotice("SALDO INSUFICIENTE PARA x0",Color.RED);return;}
            inv.money-=SHOP_BOMB_COST;
            inv.bombZero++;
            addHudNotice("-R$ "+SHOP_BOMB_COST+"  x0 +1",Color.rgb(255,150,110));
            audio.play("bonus_municao.wav");
        }

        void startBombSequence(){
            if(inv.bombZero<=0||bombSequence)return;
            boolean hasTarget=false;
            for(Meteor m:meteors)if(!m.dead){hasTarget=true;break;}
            if(!hasTarget){audio.play("divisao_errada.wav");return;}
            inv.bombZero--;
            bombSequence=true;bombSequenceTimer=1.05f;bombFlashTimer=BOMB_FLASH_DURATION;
            addHudNotice("BOMBA x0 ACIONADA",Color.rgb(255,150,110));
            audio.play("bomba_zero.wav");
            for(Meteor m:meteors)if(!m.dead){m.targetBlink=true;m.blinkTime=0;}
        }

        void updateBombSequence(float dt){
            bombSequenceTimer-=dt;
            bombFlashTimer=Math.max(0f,bombFlashTimer-dt);
            for(Meteor m:meteors)if(!m.dead)m.blinkTime+=dt;
            if(bombSequenceTimer>0)return;
            bombSequence=false;
            ArrayList<Meteor> targets=new ArrayList<Meteor>();
            for(Meteor m:meteors)if(!m.dead)targets.add(m);
            for(Meteor m:targets){
                m.targetBlink=false;
                if(isQuizMeteor(m)&&m.quiz!=null){
                    // A x0 não elimina quiz: reduz a conta para operandos de um algarismo.
                    m.quiz=MeteorMathV2.simplifyQuizToOneDigit(rnd,m.quiz,usedQuizExpressions);
                    m.value=m.quiz.answer;
                    addHudNotice("x0: QUIZ REDUZIDO PARA 1 ALGARISMO",Color.CYAN);
                    burst(m.x,m.y,Color.CYAN,14);
                }else if(m.kind==Kind.BONUS){
                    // Bônus atingidos pela x0 são creditados normalmente.
                    collectBonus(m);
                }else{
                    explode(m,true,false);
                }
            }
        }

        void updatePendingBonus(float dt){
            if(pendingBonusTarget==null)return;
            pendingBonusTimer-=dt;
            if(pendingBonusTimer<=0){
                Meteor m=pendingBonusTarget;pendingBonusTarget=null;
                if(m!=null&&!m.dead)collectBonus(m);
            }
        }

        int projectileIndexForDivisor(int d){
            for(int i=0;i<MeteorMathV2.PRIMES.length;i++)if(MeteorMathV2.PRIMES[i]==d)return i;
            return 0;
        }

        int projectileIndexForSubtractor(int value){
            for(int i=0;i<MeteorMathV2.SUBTRACTORS.length;i++)if(MeteorMathV2.SUBTRACTORS[i]==value)return i;
            return 0;
        }

        int currentAimIndex(){
            if(selectedMode==1)return 7;
            return subtractionMechanic?projectileIndexForSubtractor(inv.selectedWeapon):projectileIndexForDivisor(inv.selectedDivisor);
        }

        int currentChargedDivisor(){
            if(selectedMode==1)return MeteorMathV2.MAX_METEOR_VALUE;
            if(subtractionMechanic)return Math.max(2,inv.selectedWeapon);
            int base=Math.max(2,inv.selectedDivisor);
            return aimCharged?base*base:base;
        }

        void pointCannonAt(float tx,float ty){
            aimX=Math.max(0,Math.min(800,tx));aimY=Math.max(0,Math.min(389,ty));
            cannonAngle=(float)Math.toDegrees(Math.atan2(aimY-deployedCannonY(),aimX-cannonX));
        }

        void emitHudChargeBurst(){
            int idx=projectileIndexForDivisor(inv.selectedDivisor);
            RectF r=(idx>=0&&idx<divisorRects.length)?divisorRects[idx]:null;
            float cx=r!=null?r.centerX():30f+idx*36f;
            float cy=r!=null?r.centerY():440f;
            for(int i=0;i<24;i++){
                double a=rnd.nextDouble()*Math.PI*2;
                float sp=18+rnd.nextFloat()*55;
                int col=rnd.nextBoolean()?Color.CYAN:Color.WHITE;
                uiParticles.add(new Particle(cx,cy,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,.30f+rnd.nextFloat()*.38f,col,1.2f+rnd.nextFloat()*2.4f));
            }
        }

        void updateAimCharge(float dt){
            if(!aiming)return;
            long held=SystemClock.uptimeMillis()-aimDownTime;
            if(!subtractionMechanic&&selectedMode==0&&held>=520){
                if(!aimCharged){
                    aimCharged=true;
                    aimCharge=1f;
                    chargedProjectileValue=inv.selectedDivisor*inv.selectedDivisor;
                    hudChargeFlashTimer=.55f;
                    emitHudChargeBurst();
                    float cy=deployedCannonY();
                    double rad=Math.toRadians(cannonAngle);
                    float mx=cannonX+(float)Math.cos(rad)*25f;
                    float my=cy+(float)Math.sin(rad)*25f;
                    burst(mx,my,Color.CYAN,24);
                }
                chargeParticleTimer-=dt;
                if(chargeParticleTimer<=0){
                    chargeParticleTimer=.035f;
                    float cy=deployedCannonY();
                    double rad=Math.toRadians(cannonAngle);
                    float mx=cannonX+(float)Math.cos(rad)*25f;
                    float my=cy+(float)Math.sin(rad)*25f;
                    for(int i=0;i<4;i++){
                        double a=rnd.nextDouble()*Math.PI*2;
                        float sp=22+rnd.nextFloat()*48;
                        int col=rnd.nextBoolean()?Color.CYAN:Color.WHITE;
                        particles.add(new Particle(mx,my,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,.22f+rnd.nextFloat()*.25f,col,1.4f+rnd.nextFloat()*2.5f));
                    }
                }
            }else if(!aimCharged){
                aimCharge=0f;
                chargedProjectileValue=currentChargedDivisor();
            }
        }

        void emitCannonDirt(float dt){
            if(cannonDeploy<=0f||cannonDeploy>=.98f)return;
            dirtParticleTimer-=dt;
            if(dirtParticleTimer>0)return;
            dirtParticleTimer=.028f;
            int n=5+rnd.nextInt(4);
            int[] earth={Color.rgb(111,78,44),Color.rgb(142,102,58),Color.rgb(83,61,39),Color.rgb(166,127,75)};
            for(int i=0;i<n;i++){
                float x=cannonX-31+rnd.nextFloat()*62f;
                float y=towerBaseY-2-rnd.nextFloat()*5f;
                float vx=-38+rnd.nextFloat()*76f;
                float vy=-42-rnd.nextFloat()*75f;
                particles.add(new Particle(x,y,vx,vy,.34f+rnd.nextFloat()*.42f,earth[rnd.nextInt(earth.length)],1.5f+rnd.nextFloat()*3.2f));
            }
        }

        void addWorldMoney(float x,float y,int amount){
            if(amount<=0)return;
            worldTexts.add(new WorldText("+R$ "+amount,x,y,Color.rgb(255,190,45),.78f));
        }

        void updateWorldTexts(float dt){
            Iterator<WorldText> it=worldTexts.iterator();
            while(it.hasNext()){
                WorldText q=it.next();
                q.life-=dt;
                if(q.life<=0){it.remove();continue;}
                q.y-=22f*dt;
            }
        }

        void drawWorldTexts(Canvas c){
            for(WorldText q:worldTexts){
                float t=Math.max(0f,Math.min(1f,q.life/q.maxLife));
                int alpha=Math.max(0,Math.min(255,Math.round(255f*t*t)));
                int col=Color.argb(alpha,Color.red(q.color),Color.green(q.color),Color.blue(q.color));
                p.setAlpha(alpha);
                drawOutlinedText(c,q.text,q.x,q.y,14,col);
                p.setAlpha(255);
            }
        }

        float pickupHudX(int projectileIndex){
            int idx=Math.max(0,Math.min(6,projectileIndex));
            return 14f+idx*52f+22.5f;
        }

        void startPickupFlight(Meteor m){
            if(m==null)return;
            int idx=0;
            float endX,endY=430f;
            if(m.bonus==Bonus.HYPER){
                idx=7;endX=446f;
            }else{
                idx=subtractionMechanic?projectileIndexForSubtractor(m.ammoValue):projectileIndexForDivisor(m.ammoValue);
                endX=pickupHudX(idx);
            }
            pickupFlights.add(new PickupFly(m.bonus,idx,m.x,m.y,endX,endY,.42f));
        }

        void updatePickupFlights(float dt){
            Iterator<PickupFly> it=pickupFlights.iterator();
            while(it.hasNext()){
                PickupFly q=it.next();
                q.life-=dt;
                if(q.life<=0)it.remove();
            }
        }

        void drawPickupFlights(Canvas c){
            for(PickupFly q:pickupFlights){
                float t=1f-Math.max(0f,Math.min(1f,q.life/q.maxLife));
                float eased=1f-(1f-t)*(1f-t)*(1f-t);
                float arc=(float)Math.sin(Math.PI*t)*32f;
                float x=q.startX+(q.endX-q.startX)*eased;
                float y=q.startY+(q.endY-q.startY)*eased-arc;
                float half=10f-3f*t;
                if(projectileSheet!=null){
                    drawTile(c,projectileSheet,8,1,q.projectileIndex,new RectF(x-half,y-half,x+half,y+half),pixel);
                }else{
                    p.setColor(q.bonus==Bonus.HYPER?Color.CYAN:Color.YELLOW);
                    c.drawCircle(x,y,half*.55f,p);
                }
            }
        }

        void addHudNotice(String text,int color){
            if(text==null||text.length()==0)return;
            hudNotices.add(0,new HudNotice(text,color,1.65f));
            while(hudNotices.size()>4)hudNotices.remove(hudNotices.size()-1);
        }

        void updateHudNotices(float dt){
            Iterator<HudNotice> it=hudNotices.iterator();
            while(it.hasNext()){
                HudNotice n=it.next();
                n.life-=dt;
                if(n.life<=0)it.remove();
            }
        }

        void drawHudNotices(Canvas c){
            int shown=0;
            for(HudNotice n:hudNotices){
                if(shown>=3)break;
                float t=Math.max(0f,Math.min(1f,n.life/n.maxLife));
                int alpha=Math.max(0,Math.min(255,Math.round(255f*Math.min(1f,t*2f))));
                int base=n.color;
                int col=Color.argb(alpha,Color.red(base),Color.green(base),Color.blue(base));
                float y=96+shown*20;
                p.setColor(Color.argb(Math.min(150,alpha),0,0,0));
                c.drawRect(250,y-15,550,y+4,p);
                drawText(c,n.text,400,y,12,col,true);
                shown++;
            }
        }

        void updateUiParticles(float dt){
            Iterator<Particle> it=uiParticles.iterator();
            while(it.hasNext()){
                Particle q=it.next();q.life-=dt;
                if(q.life<=0){it.remove();continue;}
                q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=28f*dt;
            }
        }

        void updateProjectileEffects(float dt){
            if(shotTimer<=0)return;
            projectileParticleTimer-=dt;
            if(projectileParticleTimer>0)return;
            projectileParticleTimer=shotHyper?.008f:shotWasCharged?.012f:.022f;
            float t=1f-shotTimer/shotDuration;t=Math.max(0,Math.min(1,t));
            float x=shotStartX+(shotTargetX-shotStartX)*t;
            float y=shotStartY+(shotTargetY-shotStartY)*t;
            float dx=shotTargetX-shotStartX,dy=shotTargetY-shotStartY;
            float len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1)len=1;
            float ux=dx/len,uy=dy/len;
            int count=shotHyper?8:shotWasCharged?5:2;
            for(int i=0;i<count;i++){
                float side=-5+rnd.nextFloat()*10;
                float px=x-ux*(4+rnd.nextFloat()*10)-uy*side;
                float py=y-uy*(4+rnd.nextFloat()*10)+ux*side;
                int col=shotHyper?(rnd.nextBoolean()?Color.WHITE:Color.CYAN):shotWasCharged?(rnd.nextBoolean()?Color.CYAN:Color.WHITE):Color.rgb(185,215,235);
                particles.add(new Particle(px,py,-ux*(20+rnd.nextFloat()*25)-uy*side*2,-uy*(20+rnd.nextFloat()*25)+ux*side*2,.16f+rnd.nextFloat()*.22f,col,1.0f+rnd.nextFloat()*2.1f));
            }
            if(shotWasCharged){
                double spin=SystemClock.uptimeMillis()/45.0;
                for(int i=0;i<3;i++){
                    double a=spin+i*Math.PI*2/3.0;
                    float rr=8+rnd.nextFloat()*4;
                    particles.add(new Particle(x+(float)Math.cos(a)*rr,y+(float)Math.sin(a)*rr,0,0,.10f,Color.CYAN,1.5f+rnd.nextFloat()*1.8f));
                }
            }
        }

        boolean protectedSiteActive(){
            if(waves.wave<=1||protectedCells==null)return false;
            for(int r=0;r<damageRows;r++)for(int c=0;c<damageCols;c++)if(protectedCells[r][c])return true;
            return false;
        }

        float protectedRewardFraction(){
            if(!protectedSiteActive())return 0f;
            float physical=protectedSiteRemainingFraction();
            return Math.max(0f,Math.min(physical,protectedSiteHealth/100f));
        }

        int projectedProtectedReward(){
            if(!protectedSiteActive()||protectedSiteDestroyed)return 0;
            return Math.max(0,Math.round(WaveManager.monumentFullBonus(waves.wave)*protectedRewardFraction()));
        }

        int projectedProtectedPenalty(){
            return Math.min(inv.money,WaveManager.monumentPenalty(selectedDifficulty,waves.wave));
        }

        float protectedSiteRemainingFraction(){
            if(protectedCells==null||cityDestroyed==null)return 0f;
            int total=0,remaining=0;
            for(int r=0;r<damageRows;r++)for(int c=0;c<damageCols;c++){
                if(!protectedCells[r][c])continue;
                total++;
                if(!cityDestroyed[r][c])remaining++;
            }
            return total<=0?0f:remaining/(float)total;
        }

        RectF cellWorldRect(int row,int col){
            float l=col*damageCellW;
            float t=CITY_TOP+row*damageCellH;
            return new RectF(l,t,Math.min(CITY_BITMAP_W,l+damageCellW),Math.min(CITY_BOTTOM,t+damageCellH));
        }

        RectF protectedBoundsWorld(){
            if(!protectedSiteActive())return new RectF(0,CITY_TOP,0,CITY_TOP);
            int minC=damageCols,maxC=-1,minR=damageRows,maxR=-1;
            for(int r=0;r<damageRows;r++)for(int c=0;c<damageCols;c++){
                if(protectedCells[r][c]){minC=Math.min(minC,c);maxC=Math.max(maxC,c);minR=Math.min(minR,r);maxR=Math.max(maxR,r);}
            }
            if(maxC<0)return new RectF(0,CITY_TOP,0,CITY_TOP);
            return new RectF(minC*damageCellW,CITY_TOP+minR*damageCellH,
                    Math.min(CITY_BITMAP_W,(maxC+1)*damageCellW),
                    Math.min(CITY_BOTTOM,CITY_TOP+(maxR+1)*damageCellH));
        }

        void eraseRuntimeCell(int row,int col){
            if(cityRuntimeCanvas==null)return;
            float l=col*damageCellW,t=row*damageCellH;
            Paint erase=new Paint();
            erase.setStyle(Paint.Style.FILL);
            erase.setXfermode(new android.graphics.PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            cityRuntimeCanvas.drawRect(l,t,Math.min(CITY_BITMAP_W,l+damageCellW+.5f),Math.min(CITY_BITMAP_H,t+damageCellH+.5f),erase);
            erase.setXfermode(null);
        }

        float cityCollisionYAt(float x){
            if(citySolid==null||cityDestroyed==null)return CITY_TOP;
            int col=Math.max(0,Math.min(damageCols-1,(int)(x/damageCellW)));
            for(int row=0;row<damageRows;row++){
                if(citySolid[row][col]&&!cityDestroyed[row][col])return CITY_TOP+row*damageCellH;
            }
            return CITY_BOTTOM;
        }

        void spawnCellDebris(int row,int col,boolean protectedCell){
            RectF r=cellWorldRect(row,col);
            int[] rubble=protectedCell
                    ?new int[]{Color.rgb(205,205,195),Color.rgb(150,150,145),Color.rgb(105,105,102),Color.rgb(225,210,185)}
                    :new int[]{Color.rgb(125,108,82),Color.rgb(155,135,100),Color.rgb(90,85,76),Color.LTGRAY};
            int count=protectedCell?10:7;
            for(int i=0;i<count;i++){
                float x=r.left+rnd.nextFloat()*Math.max(1f,r.width());
                float y=r.top+rnd.nextFloat()*Math.max(1f,r.height());
                particles.add(new Particle(x,y,-55+rnd.nextFloat()*110,-65-rnd.nextFloat()*105,
                        .48f+rnd.nextFloat()*.68f,rubble[rnd.nextInt(rubble.length)],1.8f+rnd.nextFloat()*3.8f));
            }
        }

        void applyCityImpact(float impactX,float impactY,float amount,float meteorRadius,int meteorValue){
            if(inv.shieldSeconds>0)return;
            if(citySolid==null||cityDestroyed==null)return;
            float localY=Math.max(0f,Math.min(CITY_BITMAP_H-1f,impactY-CITY_TOP));
            int centerC=Math.max(0,Math.min(damageCols-1,(int)(impactX/damageCellW)));
            int centerR=Math.max(0,Math.min(damageRows-1,(int)(localY/damageCellH)));
            float radius=Math.max(Math.max(damageCellW,damageCellH),meteorRadius*.72f);
            int rc=Math.max(1,(int)Math.ceil(radius/damageCellW));
            int rr=Math.max(1,(int)Math.ceil(radius/damageCellH));
            boolean touchedProtected=false;
            int removedCells=0;
            for(int row=Math.max(0,centerR-rr);row<=Math.min(damageRows-1,centerR+rr);row++){
                for(int col=Math.max(0,centerC-rc);col<=Math.min(damageCols-1,centerC+rc);col++){
                    float cx=(col+.5f)*damageCellW;
                    float cy=(row+.5f)*damageCellH;
                    float dx=cx-impactX,dy=cy-localY;
                    if(dx*dx+dy*dy>radius*radius)continue;
                    if(!citySolid[row][col]||cityDestroyed[row][col])continue;
                    cityDestroyed[row][col]=true;
                    removedCells++;
                    boolean protectedCell=protectedCells!=null&&protectedCells[row][col];
                    touchedProtected|=protectedCell;
                    eraseRuntimeCell(row,col);
                    spawnCellDebris(row,col,protectedCell);
                }
            }
            if(removedCells>0){
                burst(impactX,impactY,Color.LTGRAY,Math.min(34,12+removedCells*2));
            }
            if(touchedProtected&&inv.shieldSeconds<=0&&!protectedSiteDestroyed){
                float targetDamage=Math.max(6f,Math.min(55f,meteorValue*.55f));
                protectedSiteHealth=Math.max(0f,protectedSiteHealth-targetDamage);
                float physicalHealth=protectedSiteRemainingFraction()*100f;
                protectedSiteHealth=Math.min(protectedSiteHealth,physicalHealth);
                if(protectedSiteHealth<=0f||physicalHealth<=0f)destroyProtectedSite();
            }
        }

        void destroyProtectedSite(){
            if(protectedSiteDestroyed)return;
            protectedSiteDestroyed=true;protectedSiteHealth=0;lastProtectedBonus=0;
            if(protectedCells!=null){
                for(int r=0;r<damageRows;r++)for(int c=0;c<damageCols;c++){
                    if(!protectedCells[r][c])continue;
                    if(!cityDestroyed[r][c]){
                        cityDestroyed[r][c]=true;
                        eraseRuntimeCell(r,c);
                        spawnCellDebris(r,c,true);
                    }
                }
            }
            RectF bounds=protectedBoundsWorld();
            burst(bounds.centerX(),bounds.centerY(),Color.LTGRAY,48);
            startCityShake(18f,false);
        }

        void updateProtectedSiteFire(float dt){
            if(!protectedSiteDestroyed||protectedCells==null)return;
            protectedFireTimer-=dt;
            if(protectedFireTimer>0)return;
            protectedFireTimer=.018f+.025f*rnd.nextFloat();
            int tries=0,row=0,col=0;
            do{
                row=rnd.nextInt(Math.max(1,damageRows));
                col=rnd.nextInt(Math.max(1,damageCols));
                tries++;
            }while(tries<40&&!(protectedCells[row][col]&&cityDestroyed[row][col]));
            if(!(protectedCells[row][col]&&cityDestroyed[row][col]))return;
            RectF r=cellWorldRect(row,col);
            int n=2+rnd.nextInt(3);
            for(int i=0;i<n;i++){
                float x=r.left+rnd.nextFloat()*Math.max(1f,r.width());
                float y=r.bottom-rnd.nextFloat()*Math.max(2f,r.height()*.7f);
                int color=rnd.nextBoolean()?Color.rgb(255,75,12):Color.rgb(255,190,25);
                particles.add(new Particle(x,y,-10+rnd.nextFloat()*20,-45-rnd.nextFloat()*70,
                        .34f+rnd.nextFloat()*.40f,color,1.5f+rnd.nextFloat()*3f));
            }
        }

        void update(float dt){
            updateHudNotices(dt);
            updateWorldTexts(dt);
            updatePickupFlights(dt);
            if(saveNoticeTimer>0){saveNoticeTimer=Math.max(0,saveNoticeTimer-dt);if(saveNoticeTimer==0)saveNotice=false;}
            if(victory){updateVictory(dt);updateParticles(dt);updatePlaneDebris(dt);return;}
            if(intermission){updateParticles(dt);updatePlaneDebris(dt);return;}
            if(bombSequence){updateBombSequence(dt);updateParticles(dt);updatePlaneDebris(dt);return;}
            if(paused)return;
            updateCityShake(dt);
            if(cannonAnim>0)cannonAnim=Math.max(0,cannonAnim-dt);
            else if(shotTimer<=0&&!aiming)cannonAngle=-45f;
            if(shotTimer>0){updateProjectileEffects(dt);shotTimer=Math.max(0,shotTimer-dt);}
            else{shotWasCharged=false;shotHyper=false;}
            if(!aiming&&aimTargetTimer>0)aimTargetTimer=Math.max(0,aimTargetTimer-dt);
            if(hudChargeFlashTimer>0)hudChargeFlashTimer=Math.max(0,hudChargeFlashTimer-dt);
            updateUiParticles(dt);
            updateAimCharge(dt);
            updatePendingBonus(dt);
            updateProtectedSiteFire(dt);
            updateRain(dt);
            updatePlaneDebris(dt);
            if(introWhiteFade>0)introWhiteFade=Math.max(0,introWhiteFade-dt);
            if(scoreNoticeTimer>0){scoreNoticeTimer=Math.max(0,scoreNoticeTimer-dt);if(scoreNoticeTimer==0)scoreClearedNotice=false;}
            if(waveClear){
                updateParticles(dt);
                waveClearTimer-=dt;
                if(waveClearTimer<=0){
                    waveClear=false;
                    if(completedWave>=WaveManager.MAX_WAVE)startVictory();
                    else startIntermission();
                }
                return;
            }
            if(!running&&!preWave&&!gameOver)return;
            if(preWave){
                preWaveTimer-=dt;
                float raw=1f-Math.max(0,preWaveTimer)/PRE_WAVE_DURATION;
                cannonDeploy=1f-(float)Math.pow(1f-Math.max(0,Math.min(1,raw)),3);
                emitCannonDirt(dt);
                updateParticles(dt);
                if(preWaveTimer<=0){preWave=false;running=true;cannonDeploy=1f;audio.stopLong();audio.play("chuva_meteoros_inicio.wav");}
                return;
            }
            updateCityFireParticles(dt);updateShield(dt);
            phaseActiveSeconds+=dt;
            if(gameOver||quizOpen){updateParticles(dt);updateMilitary(dt);return;}
            updateAmmoDrops(dt);
            if(!waves.complete()){
                spawnTimer-=dt;
                if(spawnTimer<=0){spawnMeteor();spawnTimer=waves.spawnSeconds()*(.82f+rnd.nextFloat()*.36f);}
                if(commercial==null&&rnd.nextFloat()<waves.commercialPlaneChancePerSecond()*dt*60f)spawnCommercial();
            }
            updatePlane(commercial,dt);updateMilitary(dt);updateMeteors(dt);updateParticles(dt);
            if(waves.complete()&&meteors.isEmpty()&&military==null&&(!subtractionMechanic||phaseAmmoDropIndex>=phaseAmmoPlan.size())){
                audio.play("fase_concluida.wav");
                if(protectedSiteActive()&&!protectedSiteBonusAwarded){
                    float remaining=protectedRewardFraction();
                    if(protectedSiteDestroyed||remaining<=0f){
                        int loss=projectedProtectedPenalty();
                        inv.money-=loss;
                        lastProtectedBonus=-loss;
                        addHudNotice(loss>0?"MONUMENTO -R$ "+loss:"MONUMENTO DESTRUIDO",Color.RED);
                    }else{
                        int fullBonus=WaveManager.monumentFullBonus(waves.wave);
                        lastProtectedBonus=Math.max(1,Math.round(fullBonus*remaining));
                        inv.money+=lastProtectedBonus;
                        addHudNotice("MONUMENTO +R$ "+lastProtectedBonus,Color.YELLOW);
                    }
                    protectedSiteBonusAwarded=true;
                }else lastProtectedBonus=0;
                int maxTimeBonus=300+waves.targetThisWave*40+waves.wave*5;
                phaseTimeBonus=Math.max(0,Math.round(maxTimeBonus-phaseActiveSeconds*4f));
                score+=phaseTimeBonus;
                running=false;
                completedWave=waves.wave;
                waveClear=true;
                waveClearTimer=2.6f;
            }
        }

        void startVictory(){
            victory=true;running=false;preWave=false;paused=false;quizOpen=false;waveClear=false;gameOver=false;
            cityHealth=100f;fireworkTimer=0f;audio.stopLong();audio.playMusicOnce("vitoria_final.ogg",.42f);saveScore();
        }

        void updateVictory(float dt){
            fireworkTimer-=dt;
            if(fireworkTimer<=0){
                spawnFirework();
                fireworkTimer=.22f+rnd.nextFloat()*.38f;
            }
        }

        void spawnFirework(){
            float x=80+rnd.nextFloat()*640f;
            float y=55+rnd.nextFloat()*205f;
            int[] colors={Color.CYAN,Color.YELLOW,Color.MAGENTA,Color.rgb(255,95,55),Color.rgb(100,255,130),Color.WHITE};
            int color=colors[rnd.nextInt(colors.length)];
            for(int i=0;i<46;i++){
                double a=rnd.nextDouble()*Math.PI*2;
                float speed=38+rnd.nextFloat()*105f;
                particles.add(new Particle(x,y,(float)Math.cos(a)*speed,(float)Math.sin(a)*speed,
                        .75f+rnd.nextFloat()*.65f,color,1.5f+rnd.nextFloat()*3.2f));
            }
            for(int i=0;i<8;i++)particles.add(new Particle(x,y,-12+rnd.nextFloat()*24,-12+rnd.nextFloat()*24,.28f,Color.WHITE,2.6f));
        }

        SharedPreferences savePrefs(){return getContext().getSharedPreferences("jogo_salvo",Context.MODE_PRIVATE);}

        boolean hasSavedGame(){return savePrefs().getBoolean("exists",false);}

        String serializeCityDamage(){
            if(cityDestroyed==null)return "";
            StringBuilder out=new StringBuilder();
            out.append(damageCols).append('x').append(damageRows).append('|');
            boolean first=true;
            for(int r=0;r<damageRows;r++)for(int c=0;c<damageCols;c++){
                if(!cityDestroyed[r][c])continue;
                if(!first)out.append(',');
                out.append(r*damageCols+c);
                first=false;
            }
            return out.toString();
        }

        void restoreCityDamage(String encoded){
            if(encoded==null||encoded.length()==0||cityDestroyed==null)return;
            try{
                int bar=encoded.indexOf('|');
                if(bar<0)return;
                String[] dim=encoded.substring(0,bar).split("x");
                if(dim.length!=2)return;
                int cols=Integer.parseInt(dim[0]),rows=Integer.parseInt(dim[1]);
                if(cols!=damageCols||rows!=damageRows)return;
                String body=encoded.substring(bar+1);
                if(body.length()==0)return;
                for(String token:body.split(",")){
                    int idx=Integer.parseInt(token);
                    int r=idx/damageCols,c=idx%damageCols;
                    if(r>=0&&r<damageRows&&c>=0&&c<damageCols){
                        cityDestroyed[r][c]=true;
                        eraseRuntimeCell(r,c);
                    }
                }
            }catch(Exception ignored){}
        }

        void saveGame(){
            StringBuilder divs=new StringBuilder();
            for(Integer d:inv.divisors){if(divs.length()>0)divs.append(',');divs.append(d);}
            savePrefs().edit()
                    .putBoolean("exists",true)
                    .putInt("wave",waves.wave)
                    .putInt("destroyedThisWave",waves.destroyedThisWave)
                    .putInt("targetThisWave",waves.targetThisWave)
                    .putInt("difficulty",selectedDifficulty)
                    .putBoolean("subtractionMechanic",subtractionMechanic)
                    .putFloat("cityHealth",cityHealth)
                    .putInt("score",score)
                    .putInt("destroyedTotal",destroyedTotal)
                    .putString("divisors",divs.toString())
                    .putInt("selectedDivisor",inv.selectedDivisor)
                    .putString("weapons",inv.serializeWeapons())
                    .putString("weaponAmmo",inv.serializeWeaponAmmo())
                    .putInt("selectedWeapon",inv.selectedWeapon)
                    .putInt("hyperAmmo",inv.hyperAmmo)
                    .putInt("money",inv.money)
                    .putInt("bombZero",inv.bombZero)
                    .putFloat("shieldSeconds",inv.shieldSeconds)
                    .putFloat("protectedSiteHealth",protectedSiteHealth)
                    .putBoolean("protectedSiteDestroyed",protectedSiteDestroyed)
                    .putBoolean("protectedSiteBonusAwarded",protectedSiteBonusAwarded)
                    .putString("cityDamageGrid",serializeCityDamage())
                    .putFloat("phaseActiveSeconds",phaseActiveSeconds)
                    .putString("usedQuizExpressions",serializeQuizKeys())
                    .putString("phaseAmmoPlan",serializeIntList(phaseAmmoPlan))
                    .putInt("phaseAmmoDropIndex",phaseAmmoDropIndex)
                    .putFloat("phaseAmmoDropTimer",phaseAmmoDropTimer)
                    .apply();
            saveNotice=true;saveNoticeTimer=1.6f;
        }

        boolean loadSavedGame(){
            SharedPreferences sp=savePrefs();
            if(!sp.getBoolean("exists",false))return false;
            meteors.clear();particles.clear();uiParticles.clear();hudNotices.clear();planeDebris.clear();commercial=null;military=null;quizMeteor=null;
            running=false;preWave=true;gameOver=false;quizOpen=false;paused=false;victory=false;waveClear=false;
            cityShaking=false;cityShakeOffset=0;cannonAngle=-45;cannonAnim=0;shotTimer=0;cannonDeploy=0;
            scoreSaved=false;fireParticleTimer=0;shieldVisualAge=0;shieldWasActive=false;selectedMode=0;
            intermission=false;manualFromPause=false;bombSequence=false;bombSequenceTimer=0;bombFlashTimer=0;pendingBonusTarget=null;pendingBonusTimer=0;lastDivisorAcquired=0;
            aiming=false;aimTargetTimer=0;aimCharge=0;chargeParticleTimer=0;aimDownTime=0;manualPage=0;dirtParticleTimer=0;
            aimCharged=false;shotWasCharged=false;shotHyper=false;projectileParticleTimer=0;hudChargeFlashTimer=0;lastProtectedBonus=0;
            waves.wave=Math.max(1,Math.min(WaveManager.MAX_WAVE,sp.getInt("wave",1)));
            waves.destroyedThisWave=Math.max(0,sp.getInt("destroyedThisWave",0));
            waves.targetThisWave=Math.max(1,sp.getInt("targetThisWave",WaveManager.targetForWave(waves.wave)));
            selectedDifficulty=Math.max(0,Math.min(5,sp.getInt("difficulty",0)));waves.difficulty=selectedDifficulty;
            subtractionMechanic=sp.getBoolean("subtractionMechanic",subtractionMechanic);
            cityHealth=Math.max(.1f,Math.min(100f,sp.getFloat("cityHealth",100f)));
            score=Math.max(0,sp.getInt("score",0));destroyedTotal=Math.max(0,sp.getInt("destroyedTotal",0));

            inv.resetArsenal();
            inv.divisors.clear();
            String raw=sp.getString("divisors","2,3");
            if(raw!=null)for(String part:raw.split(",")){try{int d=Integer.parseInt(part);if(d>=2&&d<=17&&MeteorMathV2.isPrime(d))inv.divisors.add(d);}catch(Exception ignored){}}
            if(inv.divisors.isEmpty()){inv.divisors.add(2);inv.divisors.add(3);}
            inv.selectedDivisor=sp.getInt("selectedDivisor",2);
            if(!inv.divisors.contains(inv.selectedDivisor))inv.selectedDivisor=2;

            inv.selectedWeapon=sp.getInt("selectedWeapon",2);
            inv.restoreWeapons(sp.getString("weapons","2"));
            if(!inv.hasWeapon(inv.selectedWeapon))inv.selectedWeapon=2;
            inv.restoreWeaponAmmo(sp.getString("weaponAmmo",""));
            inv.hyperAmmo=Math.max(0,sp.getInt("hyperAmmo",0));

            inv.money=Math.max(0,sp.getInt("money",0));inv.bombZero=Math.max(0,sp.getInt("bombZero",0));
            inv.shieldSeconds=Math.max(0,sp.getFloat("shieldSeconds",0));
            loadCityForPhase();initializeDamageGrid();
            restoreCityDamage(sp.getString("cityDamageGrid",""));
            protectedSiteHealth=waves.wave>1?Math.max(0f,Math.min(100f,sp.getFloat("protectedSiteHealth",100f))):100f;
            protectedSiteDestroyed=waves.wave>1&&sp.getBoolean("protectedSiteDestroyed",false);
            protectedSiteBonusAwarded=sp.getBoolean("protectedSiteBonusAwarded",false);
            protectedFireTimer=0f;
            phaseActiveSeconds=Math.max(0f,sp.getFloat("phaseActiveSeconds",0f));
            phaseTimeBonus=0;
            restoreQuizKeys(sp.getString("usedQuizExpressions",""));
            restoreIntList(sp.getString("phaseAmmoPlan",""),phaseAmmoPlan);
            phaseAmmoDropIndex=Math.max(0,Math.min(phaseAmmoPlan.size(),sp.getInt("phaseAmmoDropIndex",0)));
            phaseAmmoDropTimer=Math.max(0f,sp.getFloat("phaseAmmoDropTimer",5.5f));
            if(subtractionMechanic&&waves.wave>=2&&phaseAmmoPlan.isEmpty())configureAmmoDropPlan();
            capturePhaseEnvironment();
            preWaveTimer=PRE_WAVE_DURATION;spawnTimer=0;fireworkTimer=0;saveNotice=false;saveNoticeTimer=0;
            playWaveMusic();audio.playLong("sirene_80bpm_10.wav");
            return true;
        }

        void returnToMainMenu(){
            running=false;preWave=false;gameOver=false;quizOpen=false;paused=false;victory=false;waveClear=false;intermission=false;manualFromPause=false;bombSequence=false;bombSequenceTimer=0;bombFlashTimer=0;
            pendingBonusTarget=null;meteors.clear();particles.clear();uiParticles.clear();hudNotices.clear();commercial=null;military=null;quizMeteor=null;pauseRect.setEmpty();
            cityImg=baseCityImg;aimCharged=false;shotWasCharged=false;shotHyper=false;hudChargeFlashTimer=0;protectedSiteDestroyed=false;protectedSiteHealth=100f;
            audio.stopLong();audio.setMusicPaused(false);audio.playMusic("musica_menu.ogg",.42f);menuPage=MENU_MAIN;
        }

        void updateShield(float dt){
            boolean active=inv.shieldSeconds>0;
            if(active){if(!shieldWasActive)shieldVisualAge=0;shieldVisualAge+=dt;inv.shieldSeconds=Math.max(0,inv.shieldSeconds-dt);}else shieldVisualAge=0;
            shieldWasActive=active;
        }

        void updateCityShake(float dt){
            if(!cityShaking){cityShakeOffset=0;return;}cityShakeT+=dt*30f;
            if(cityShakeT<cityShakeT2)cityShakeOffset=(float)(Math.exp(-cityShakeGamma*cityShakeT)*cityShakeAlpha*Math.cos(20f*cityShakeT-10f));
            else{cityShaking=false;cityShakeT=0;cityShakeOffset=0;}
        }

        void startCityShake(float damage, boolean nuclear){
            float escala=nuclear?5f:Math.max(2f,Math.min(6f,2f+damage/4f));cityShakeAlpha=escala*25f/4f;cityShakeGamma=(nuclear?.75f:escala/4f)*.025f;
            cityShakeT=0;cityShakeT2=nuclear?90f:18f*escala;cityShaking=true;
            if(vibrationEnabled){try{Vibrator v=(Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);if(v!=null)v.vibrate((long)(nuclear?650:65*escala));}catch(Exception ignored){}}
        }

        void spawnMeteor(){
            Meteor m=new Meteor();m.y=-30;m.speed=waves.meteorSpeed()*(.85f+rnd.nextFloat()*.35f);m.radius=20;
            float roll=rnd.nextFloat();
            if(roll<waves.bonusChance())setupBonus(m);
            else if(roll<waves.bonusChance()+waves.specialChance()){
                m.quiz=MeteorMathV2.generateQuiz(rnd,selectedDifficulty,waves.wave,usedQuizExpressions);
                switch(m.quiz.operation){
                    case SUBTRACT:m.kind=Kind.SUB;break;
                    case MULTIPLY:m.kind=Kind.MULT;break;
                    case DIVIDE:m.kind=Kind.DIV;break;
                    default:m.kind=Kind.ADD;break;
                }
                m.value=m.quiz.answer;m.originalValue=m.value;m.radius=22;
                if(m.kind==Kind.MULT)audio.play("meteoro_multiplicacao.wav");
                else if(m.kind==Kind.ADD)audio.play("meteoro_adicao.wav");
            }else{
                m.kind=Kind.NORMAL;
                if(subtractionMechanic){
                    m.value=MeteorMathV2.generateSubtractionValue(rnd,waves.maxMeteorValue(),waves.wave);
                }else{
                    boolean largePrime=waves.allowLargePrime()&&inv.hyperAmmo>0;
                    m.value=MeteorMathV2.generateNormalValue(rnd,waves.maxMeteorValue(),inv.highestDivisor(),largePrime);
                }
                m.originalValue=m.value;
            }

            if(m.kind!=Kind.BONUS){
                m.rewardCap=Math.max(0,m.originalValue);
                m.moneyEarned=0;
            }

            boolean canPrefer=m.kind!=Kind.BONUS&&protectedSiteActive()&&!protectedSiteDestroyed;
            float preferChance=Math.min(.44f,.28f+waves.wave*.006f);
            if(canPrefer&&rnd.nextFloat()<preferChance){
                RectF target=protectedBoundsWorld();
                float spread=Math.max(24f,target.width()*.65f);
                m.x=Math.max(30f,Math.min(770f,target.centerX()+(rnd.nextFloat()-.5f)*spread));
            }else{
                m.x=45+rnd.nextInt(710);
            }

            meteors.add(m);
            if(rnd.nextFloat()<.24f)audio.play("meteoro_entrada.wav");
        }

        void setupBonus(Meteor m){
            m.kind=Kind.BONUS;m.radius=18;int r=rnd.nextInt(100);
            if(!subtractionMechanic){
                int candidate=nextLockedPrime();
                if(candidate>0&&r<22){m.bonus=Bonus.AMMO;m.ammoValue=candidate;m.value=candidate;return;}
                if(r<30){m.bonus=Bonus.HYPER;m.value=1;}
                else if(r<57){m.bonus=Bonus.MONEY;m.value=25+Math.min(50,waves.wave*3);}
                else if(r<74){m.bonus=Bonus.HEALTH;m.value=10;}
                else if(r<89){m.bonus=Bonus.SHIELD;m.value=10;}
                else{m.bonus=Bonus.BOMB0;m.value=0;}
                return;
            }
            // H é deliberadamente raro: é o único disparo que ignora o valor do meteoro.
            if(r<10){m.bonus=Bonus.HYPER;m.value=1;}
            else if(r<45){m.bonus=Bonus.MONEY;m.value=25+Math.min(50,waves.wave*3);}
            else if(r<65){m.bonus=Bonus.HEALTH;m.value=10;}
            else if(r<82){m.bonus=Bonus.SHIELD;m.value=10;}
            else{m.bonus=Bonus.BOMB0;m.value=0;}
        }

        int nextLockedPrime(){
            int ceiling=waves.primeUnlockCeiling();
            for(int x:MeteorMathV2.PRIMES)if(x<=ceiling&&!inv.divisors.contains(x))return x;
            return -1;
        }

        void spawnPhaseAmmoDrop(int weapon){
            Meteor m=new Meteor();
            m.kind=Kind.BONUS;m.bonus=Bonus.AMMO;m.ammoValue=weapon;m.value=2;
            m.radius=18;m.x=55+rnd.nextInt(690);m.y=-24;m.speed=Math.max(26f,waves.meteorSpeed()*.72f);
            meteors.add(m);
        }

        void updateAmmoDrops(float dt){
            if(!subtractionMechanic||phaseAmmoDropIndex>=phaseAmmoPlan.size())return;
            if(waves.complete())phaseAmmoDropTimer=Math.min(phaseAmmoDropTimer,.35f);
            phaseAmmoDropTimer-=dt;
            if(phaseAmmoDropTimer>0)return;
            spawnPhaseAmmoDrop(phaseAmmoPlan.get(phaseAmmoDropIndex));
            phaseAmmoDropIndex++;
            phaseAmmoDropTimer=7.5f+rnd.nextFloat()*3.5f;
        }
        void spawnCommercial(){
            commercial=new Plane();
            commercial.x=-70;commercial.baseY=78+rnd.nextInt(145);commercial.y=commercial.baseY;
            commercial.speed=72+waves.wave*2.2f;commercial.military=false;commercial.bobPhase=rnd.nextFloat()*6.28f;
            audio.play("aviao_comercial_passagem.wav");
        }

        void emitPlaneTrail(Plane pl,float dt){
            if(pl==null)return;
            pl.trailTimer-=dt;
            if(pl.trailTimer>0)return;
            pl.trailTimer=pl.military?.035f:.07f;
            int count=pl.military?2:1;
            for(int i=0;i<count;i++){
                float px=pl.x-(pl.military?46f:54f)+rnd.nextFloat()*5f;
                float py=pl.y+(rnd.nextFloat()-.5f)*8f;
                int col=pl.military?Color.rgb(255,150,55):Color.rgb(190,205,220);
                particles.add(new Particle(px,py,-25-rnd.nextFloat()*35,-4+rnd.nextFloat()*8,.22f+rnd.nextFloat()*.20f,col,1.2f+rnd.nextFloat()*1.8f));
            }
        }

        void updatePlane(Plane pl,float dt){
            if(pl==null||!pl.active)return;
            pl.x+=pl.speed*dt;
            pl.y=pl.baseY+(float)Math.sin(pl.bobPhase+pl.x*.026f)*3.2f;
            emitPlaneTrail(pl,dt);
            if(pl.x>870){pl.active=false;if(pl==commercial)commercial=null;}
        }

        boolean visiblePlaneTile(Rect src){
            if(planeCommercial==null||src.width()<=0||src.height()<=0)return false;
            int stepX=Math.max(1,src.width()/4),stepY=Math.max(1,src.height()/4);
            for(int y=src.top;y<src.bottom;y+=stepY)for(int x=src.left;x<src.right;x+=stepX){
                if(((planeCommercial.getPixel(Math.min(planeCommercial.getWidth()-1,x),Math.min(planeCommercial.getHeight()-1,y))>>>24)&255)>20)return true;
            }
            return false;
        }

        void shatterCommercialPlane(Plane pl){
            if(pl==null)return;
            RectF b=pl.bounds();
            if(planeCommercial==null){burst(pl.x,pl.y,Color.LTGRAY,32);return;}

            android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
            float xdpi=(dm.xdpi>100f&&dm.xdpi<1000f)?dm.xdpi:385f;
            float ydpi=(dm.ydpi>100f&&dm.ydpi<1000f)?dm.ydpi:385f;
            float mmWorldX=Math.max(2.5f,(xdpi/25.4f)/Math.max(.01f,scaleX));
            float mmWorldY=Math.max(2.5f,(ydpi/25.4f)/Math.max(.01f,scaleY));
            int cols=Math.max(4,(int)Math.ceil(b.width()/mmWorldX));
            int rows=Math.max(2,(int)Math.ceil(b.height()/mmWorldY));
            float fw=b.width()/cols,fh=b.height()/rows;

            for(int row=0;row<rows;row++){
                for(int col=0;col<cols;col++){
                    int sl=Math.round(col*planeCommercial.getWidth()/(float)cols);
                    int st=Math.round(row*planeCommercial.getHeight()/(float)rows);
                    int sr=Math.max(sl+1,Math.round((col+1)*planeCommercial.getWidth()/(float)cols));
                    int sb=Math.max(st+1,Math.round((row+1)*planeCommercial.getHeight()/(float)rows));
                    Rect src=new Rect(sl,st,Math.min(planeCommercial.getWidth(),sr),Math.min(planeCommercial.getHeight(),sb));
                    if(!visiblePlaneTile(src))continue;
                    float x=b.left+(col+.5f)*fw;
                    float y=b.top+(row+.5f)*fh;
                    float vx=pl.speed*.28f-85f+rnd.nextFloat()*170f;
                    float vy=-80f+rnd.nextFloat()*95f;
                    planeDebris.add(new PlaneFragment(src,x,y,vx,vy,fw,fh,rnd.nextFloat()*360f,-220f+rnd.nextFloat()*440f));
                }
            }
            burst(pl.x,pl.y,Color.LTGRAY,28);
        }

        void updatePlaneDebris(float dt){
            if(planeDebris.isEmpty())return;
            float totalDamage=0f,lastX=400f;
            Iterator<PlaneFragment> it=planeDebris.iterator();
            while(it.hasNext()){
                PlaneFragment q=it.next();
                q.life-=dt;
                if(q.life<=0||q.dead){it.remove();continue;}
                q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=150f*dt;q.angle+=q.spin*dt;
                if(q.x>=0&&q.x<=800&&q.vy>0){
                    float ground=cityCollisionYAt(q.x);
                    if(q.y+q.h*.5f>=ground){
                        totalDamage+=.18f;
                        lastX=q.x;
                        burst(q.x,ground,Color.LTGRAY,3);
                        it.remove();
                        continue;
                    }
                }
                if(q.y>CITY_BOTTOM+70||q.x<-120||q.x>920)it.remove();
            }
            if(totalDamage>0)damageCity(totalDamage,lastX,false);
        }

        void updateMilitary(float dt){
            if(military==null)return;
            military.x+=military.speed*dt;military.strikeTimer-=dt;
            float desired=military.baseY;
            if(military.target!=null&&!military.target.dead){
                desired=Math.max(48f,Math.min(235f,military.target.y-62f));
                military.target.targetBlink=true;military.target.blinkTime+=dt;
            }
            military.y+=(desired-military.y)*Math.min(1f,dt*3.5f);
            military.y+=(float)Math.sin(military.bobPhase+military.x*.04f)*.7f;
            emitPlaneTrail(military,dt);

            if(!military.bombActive&&military.strikeTimer<=.56f&&military.target!=null&&!military.target.dead){
                military.bombActive=true;
                military.bombX=military.x+8f;
                military.bombY=military.y+13f;
                military.bombVY=45f;
                military.bombAngle=0f;
            }

            if(military.bombActive){
                Meteor t=military.target;
                military.bombVY+=190f*dt;
                military.bombY+=military.bombVY*dt;
                military.bombAngle+=210f*dt;
                if(t!=null&&!t.dead){
                    military.bombX+=(t.x-military.bombX)*Math.min(1f,dt*2.2f);
                    if(military.bombY>=t.y-8f){
                        military.bombActive=false;
                        audio.play("aviao_militar_bomba.wav");
                        explode(t,true,true);
                        military.target=null;
                    }
                }else{
                    military.bombActive=false;
                    military.target=null;
                }
            }

            if(military.x>890&&!military.bombActive)military=null;
        }

        void updateMeteors(float dt){
            Iterator<Meteor> it=meteors.iterator();
            while(it.hasNext()){
                Meteor m=it.next();
                if(m.dead){it.remove();continue;}
                if(m==pendingBonusTarget)continue;
                emitMeteorTrail(m,dt);
                m.y+=m.speed*dt;

                if(commercial!=null&&commercial.active&&RectF.intersects(m.bounds(),commercial.bounds())){
                    Plane hitPlane=commercial;
                    hitPlane.active=false;
                    shatterCommercialPlane(hitPlane);
                    commercial=null;
                    audio.play("aviao_comercial_atingido.wav");
                    m.dead=true;continue;
                }

                float collisionY=cityCollisionYAt(m.x);
                if(m.y+m.radius>=collisionY){
                    if(m.kind==Kind.BONUS){
                        // Itens perdidos simplesmente saem da fase; não ferem a cidade.
                    }else if(isQuizMeteor(m)){
                        applyCityImpact(m.x,collisionY,Math.max(8f,cityHealth*.12f),m.radius,m.originalValue);
                        damageCity(cityHealth*.5f,m.x,false);
                    }else{
                        float impact=Math.min(18,4+m.value/18f);
                        applyCityImpact(m.x,collisionY,impact,m.radius,m.originalValue);
                        damageCity(impact,m.x,false);
                    }
                    m.dead=true;
                }
            }
        }

        void updateParticles(float dt){Iterator<Particle> it=particles.iterator();while(it.hasNext()){Particle q=it.next();q.life-=dt;if(q.life<=0){it.remove();continue;}q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=45*dt;}}

        void damageCity(float amount,float impactX,boolean nuclear){if(inv.shieldSeconds>0&&!nuclear){audio.play("bonus_escudo.wav");return;}cityHealth-=amount;lastImpactX=(int)impactX;audio.play("impacto_cidade.wav");startCityShake(amount,nuclear);if(cityHealth<=0){cityHealth=0;gameOver=true;running=false;audio.play("game_over.wav");saveScore();}}

        void fireProjectileAt(float tx,float ty,int projectileIndex){
            pointCannonAt(tx,ty);
            double rad=Math.toRadians(cannonAngle);
            float muzzleOffset=12f;
            float cy=deployedCannonY();
            shotStartX=cannonX+(float)Math.cos(rad)*muzzleOffset;
            shotStartY=cy+(float)Math.sin(rad)*muzzleOffset;
            shotProjectileIndex=Math.max(0,Math.min(7,projectileIndex));
            shotWasCharged=aimCharged&&selectedMode==0;
            shotHyper=selectedMode==1;
            shotProjectileValue=chargedProjectileValue;
            projectileParticleTimer=0f;
            cannonAnim=.40f;shotTimer=shotDuration;shotTargetX=aimX;shotTargetY=aimY;
            audio.play("disparo_canhao.wav");
        }

        void queueBonusCollection(Meteor m){
            if(m==null||m.dead||pendingBonusTarget!=null)return;
            pendingBonusTarget=m;pendingBonusTimer=shotDuration;
        }

        void applyDivisorShot(Meteor m,int divisor){
            if(MeteorMathV2.canDivide(m.value,divisor)){
                m.value/=divisor;
                audio.play("divisao_correta.wav");
                burst(m.x,m.y,Color.rgb(100,255,120),10);
                m.radius=Math.max(11,m.radius*.88f);
                if(m.value<=1)explode(m,true,false);
            }else{
                audio.play("divisao_errada.wav");
                burst(m.x,m.y,Color.rgb(255,80,60),7);
                score=Math.max(0,score-2);
            }
        }

        void applySubtractorShot(Meteor m,int amount){
            audio.play("subtrator_uso.wav");
            burst(m.x,m.y,Color.CYAN,10);
            m.value-=Math.max(1,amount);
            if(m.value<=0){m.value=0;explode(m,true,false);}
            else m.radius=Math.max(11,m.radius*.92f);
        }

        boolean isQuizMeteor(Meteor m){
            return m!=null&&(m.kind==Kind.ADD||m.kind==Kind.SUB||m.kind==Kind.MULT||m.kind==Kind.DIV);
        }

        void resolveShotAt(float x,float y,int projectileValue){
            Meteor hit=null;
            for(int i=meteors.size()-1;i>=0;i--){
                Meteor m=meteors.get(i);
                if(!m.dead&&WaveManager.hitsMeteor(m.x,m.y,m.radius,x,y,selectedDifficulty)){hit=m;break;}
            }
            if(hit==null)return;
            if(hit.kind==Kind.BONUS){queueBonusCollection(hit);return;}

            // O total de dinheiro obtido de um mesmo meteoro nunca excede
            // o valor que ele tinha ao entrar na fase.
            int income=WaveManager.meteorHitIncome(hit.rewardCap,hit.moneyEarned,hit.value);
            if(income>0){
                hit.moneyEarned+=income;
                inv.money+=income;
            }

            if(selectedMode==1){
                burst(hit.x,hit.y,Color.WHITE,34);
                burst(hit.x,hit.y,Color.CYAN,22);
                addHudNotice("H: DESTRUICAO TOTAL DO ALVO",Color.CYAN);
                explode(hit,true,false);
                return;
            }

            if(isQuizMeteor(hit)){openQuiz(hit);return;}

            if(subtractionMechanic)applySubtractorShot(hit,projectileValue);
            else applyDivisorShot(hit,projectileValue);
        }

        void releaseAimedShot(){
            updateAimCharge(0f);
            int value=currentChargedDivisor();

            if(selectedMode==1){
                if(!inv.spendHyper()){
                    audio.play("divisao_errada.wav");
                    aiming=false;aimTargetTimer=.20f;aimCharge=0f;aimCharged=false;chargeParticleTimer=0f;
                    return;
                }
            }else if(subtractionMechanic){
                value=Math.max(2,inv.selectedWeapon);
                if(!inv.spendWeaponAmmo(value)){
                    audio.play("divisao_errada.wav");
                    aiming=false;aimTargetTimer=.20f;aimCharge=0f;aimCharged=false;chargeParticleTimer=0f;
                    return;
                }
            }

            int projectileIndex=aimTargetIndex;
            chargedProjectileValue=value;
            fireProjectileAt(aimX,aimY,projectileIndex);
            resolveShotAt(aimX,aimY,value);
            aiming=false;
            aimTargetTimer=.34f;
            aimCharge=0f;
            aimCharged=false;
            chargeParticleTimer=0f;
        }

        void collectBonus(Meteor m){
            switch(m.bonus){
                case AMMO:
                    startPickupFlight(m);
                    if(subtractionMechanic){
                        inv.addWeaponAmmo(m.ammoValue,2);
                        audio.play("bonus_municao.wav");
                    }else{
                        boolean unlocked=inv.unlockDivisor(m.ammoValue);
                        if(unlocked)audio.play("municao_desbloqueada.wav");
                        else audio.play("bonus_municao.wav");
                    }
                    break;
                case HYPER:
                    startPickupFlight(m);
                    inv.addHyper(1);audio.play("bonus_municao.wav");break;
                case MONEY:
                    inv.money+=m.value;
                    addWorldMoney(m.x,m.y,m.value);
                    audio.play("bonus_dinheiro.wav");break;
                case HEALTH:
                    float before=cityHealth;cityHealth=Math.min(100,cityHealth+m.value);
                    addHudNotice("CIDADE +"+Math.round(cityHealth-before)+"%",Color.GREEN);audio.play("bonus_saude.wav");break;
                case SHIELD:
                    inv.shieldSeconds=Math.max(inv.shieldSeconds,10);shieldVisualAge=0;shieldWasActive=false;
                    addHudNotice("ESCUDO 10s",Color.CYAN);audio.play("bonus_escudo.wav");break;
                case BOMB0:
                    inv.bombZero++;addHudNotice("BOMBA x0 +1",Color.rgb(255,150,110));audio.play("bonus_municao.wav");break;
            }
            m.dead=true;burst(m.x,m.y,Color.YELLOW,12);
        }

        void explode(Meteor m,boolean count,boolean guaranteedBonus){
            if(m.dead)return;
            float ex=m.x,ey=m.y;
            m.dead=true;
            audio.play("explosao_meteoro.wav");
            burst(ex,ey,Color.rgb(255,150,35),24);
            if(count){
                waves.countDestroyed();destroyedTotal++;score+=10+Math.min(40,m.originalValue/3);
                if(m.moneyEarned>0)addWorldMoney(ex,ey,m.moneyEarned);
                if(guaranteedBonus)spawnBonusAt(ex,ey);
            }
        }
        void spawnBonusAt(float x,float y){Meteor b=new Meteor();b.x=x;b.y=y;b.speed=30;b.radius=18;setupBonus(b);meteors.add(b);}
        void openQuiz(Meteor m){quizOpen=true;quizMeteor=m;quizAttempts=0;audio.play("quiz_abre.wav");}
        void answerQuiz(int option){
            if(!quizOpen||quizMeteor==null)return;
            if(option==quizMeteor.quiz.answer){
                audio.play("quiz_acerto.wav");audio.play("alvo_trava.wav");
                quizOpen=false;startMilitaryStrike(quizMeteor);
            }else{
                quizAttempts++;audio.play("quiz_erro.wav");
                if(quizAttempts>=2){
                    quizMeteor.kind=Kind.NORMAL;
                    quizMeteor.value=quizMeteor.quiz.answer;
                    quizMeteor.quiz=null;
                    quizOpen=false;quizMeteor=null;
                }
            }
        }
        void startMilitaryStrike(Meteor target){
            military=new Plane();military.military=true;military.x=-75;
            military.baseY=Math.max(52,target.y-72);military.y=military.baseY;
            military.speed=190;military.target=target;military.strikeTimer=1.12f;military.bobPhase=rnd.nextFloat()*6.28f;
            military.bombActive=false;military.bombX=military.x;military.bombY=military.y;
            audio.play("aviao_militar_passagem.wav");quizMeteor=null;
        }
        void burst(float x,float y,int color,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float s=25+rnd.nextFloat()*70;particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,.35f+rnd.nextFloat()*.45f,color,2+rnd.nextFloat()*3));}}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            c.save();
            c.scale(scaleX,scaleY);
            if(!running&&!preWave&&!gameOver&&!waveClear&&!victory&&!intermission)drawMenu(c);else drawGame(c);
            if(introWhiteFade>0){
                float t=Math.max(0,Math.min(1,introWhiteFade/.30f));
                p.setColor(Color.argb((int)(255*t),255,255,255));
                c.drawRect(0,0,800,480,p);
            }
            c.restore();
        }
        void drawMenu(Canvas c){p.setColor(Color.rgb(3,10,25));c.drawRect(0,0,800,480,p);if(menuBg!=null)drawCenterCrop(c,menuBg,new RectF(0,0,800,480),.56f);p.setColor(Color.argb(45,0,8,20));c.drawRect(0,0,800,480,p);if(menuPage==MENU_MAIN)drawMainMenu(c);else if(menuPage==MENU_SCORE)drawScores(c);else if(menuPage==MENU_OPTIONS)drawOptions(c);else drawManual(c,false);}
        void drawMainMenu(Canvas c){
            drawText(c,"ASTEROIDE MATEMATICO",400,70,34,Color.WHITE,true);
            String[] labels={"INICIAR","CARREGAR JOGO SALVO","VER PONTUACAO","OPCOES","SAIR"};
            float top=142;
            for(int i=0;i<labels.length;i++){
                RectF r=menuButtons[i];r.set(270,top+i*54,530,top+40+i*54);
                drawMenuButton(c,r,labels[i],i==0);
            }
            if(!hasSavedGame())drawText(c,"Nenhum jogo salvo",400,424,10,Color.LTGRAY,true);
        }

        void drawScores(Canvas c){
            drawDarkCard(c,115,55,685,410);drawText(c,"PONTUACAO",400,92,30,Color.CYAN,true);drawText(c,"DATA",195,126,14,Color.LTGRAY,true);drawText(c,"PONTOS",410,126,14,Color.LTGRAY,true);drawText(c,"METEOROS",570,126,14,Color.LTGRAY,true);java.util.ArrayList<String[]> rows=new java.util.ArrayList<String[]>();
            try{SharedPreferences sp=getContext().getSharedPreferences("pontuacao",Context.MODE_PRIVATE);for(Object o:sp.getAll().values()){String[] v=String.valueOf(o).split("\\|");if(v.length>=3)rows.add(v);}java.util.Collections.sort(rows,new java.util.Comparator<String[]>(){public int compare(String[] a,String[] b){try{return Integer.parseInt(b[1])-Integer.parseInt(a[1]);}catch(Exception e){return 0;}}});}catch(Exception ignored){}
            if(rows.isEmpty())drawText(c,"NENHUMA PONTUACAO SALVA",400,205,17,Color.WHITE,true);else{int max=Math.min(7,rows.size());for(int i=0;i<max;i++){String[] v=rows.get(i);float y=158+i*29;drawText(c,v[0],195,y,12,Color.WHITE,true);drawText(c,v[1],410,y,14,Color.YELLOW,true);drawText(c,v[2],570,y,14,Color.WHITE,true);}}drawMenuButton(c,new RectF(310,355,490,395),"VOLTAR",false);
        }

        String difficultyName(){String[] n={"MUITO FACIL","FACIL","MEDIO","DIFICIL","MUITO DIFICIL","INSANO"};return n[Math.max(0,Math.min(n.length-1,selectedDifficulty))];}
        void drawOptions(Canvas c){
            drawDarkCard(c,160,16,640,458);drawText(c,"OPCOES",400,48,27,Color.CYAN,true);
            drawMenuButton(c,new RectF(235,60,565,96),"DIFICULDADE: "+difficultyName(),false);
            drawMenuButton(c,new RectF(235,101,565,137),"MECANICA: "+(subtractionMechanic?"SUBTRACAO":"DIVISORES"),false);
            drawMenuButton(c,new RectF(235,142,565,178),"MIRA LASER: "+(laserEnabled?"LIGADA":"DESLIGADA"),false);
            drawMenuButton(c,new RectF(235,183,565,219),"VIBRACAO: "+(vibrationEnabled?"LIGADA":"DESLIGADA"),false);
            drawMenuButton(c,new RectF(235,224,565,260),"CLIMA ONLINE: "+(dynamicWeatherEnabled?"LIGADO":"DESLIGADO"),false);
            drawText(c,"Consulta unica por fase - dados Open-Meteo",400,274,9,Color.LTGRAY,true);
            drawMenuButton(c,new RectF(235,286,565,322),"MANUAL",false);
            drawMenuButton(c,new RectF(235,327,565,363),"APAGAR SCORE",false);
            if(scoreClearedNotice)drawText(c,"SCORE APAGADO",400,379,11,Color.YELLOW,true);
            drawMenuButton(c,new RectF(310,397,490,433),"VOLTAR",false);
        }

        void drawSky(Canvas c){
            if(phaseDay){
                p.setColor(Color.rgb(92,174,232));
                c.drawRect(0,0,800,480,p);
                p.setColor(Color.argb(45,255,238,170));
                c.drawCircle(700,70,34,p);
            }else{
                p.setColor(Color.rgb(7,20,42));
                c.drawRect(0,0,800,480,p);
                drawStars(c);
            }
            drawWeatherClouds(c);
        }

        void drawWeatherClouds(Canvas c){
            if(!dynamicWeatherEnabled)return;
            int cover=Math.max(phaseRain?55:0,phaseCloudCover);
            if(cover<20)return;
            int clouds=1+cover/22;
            int base=phaseDay?Color.rgb(225,232,238):Color.rgb(72,82,98);
            for(int i=0;i<clouds;i++){
                float x=70+(i*157+(waves.wave*31)%90)%720;
                float y=38+(i*43)%125;
                float w=75+(i%3)*22;
                p.setColor(Color.argb(phaseRain?205:175,Color.red(base),Color.green(base),Color.blue(base)));
                c.drawRect(x,y,x+w,y+14,p);
                c.drawRect(x+12,y-9,x+w-18,y+14,p);
                c.drawRect(x+30,y-16,x+w-34,y+14,p);
            }
        }

        void drawGame(Canvas c){
            drawSky(c);
            c.save();c.translate(0,cityShakeOffset);
            drawCity(c);drawProtectedSiteDamage(c);drawShieldDome(c);
            for(Particle q:particles){
                p.setColor(q.color);p.setAlpha((int)(255*q.life/q.maxLife));
                c.drawRect(q.x-q.size,q.y-q.size,q.x+q.size,q.y+q.size,p);p.setAlpha(255);
            }
            drawPlaneDebris(c);
            for(Meteor m:meteors)if(!m.dead)drawMeteor(c,m);
            if(commercial!=null&&commercial.active)drawPlane(c,commercial);
            if(military!=null)drawPlane(c,military);
            drawCannon(c);drawProjectile(c);
            c.restore();
            drawWorldTexts(c);
            drawAimTarget(c);drawHud(c);drawUiParticles(c);drawPickupFlights(c);

            if(bombFlashTimer>0f){
                float t=Math.max(0f,Math.min(1f,bombFlashTimer/BOMB_FLASH_DURATION));
                int alpha=Math.max(0,Math.min(128,Math.round(128f*t*t)));
                p.setColor(Color.argb(alpha,255,105,105));
                c.drawRect(0,0,800,480,p);
            }

            if(preWave){
                p.setColor(Color.argb(145+(int)(55*Math.abs(Math.sin(preWaveTimer*4))),120,0,0));c.drawRect(0,0,800,390,p);
                drawText(c,"FASE "+waves.wave,400,112,22,Color.YELLOW,true);
                drawText(c,"PROTEJA "+phaseCityName()+"!",400,154,31,Color.WHITE,true);
                drawText(c,phaseObjective(),400,194,15,Color.CYAN,true);
                if(subtractionMechanic){
                    int weapon=subtractorForPhase(waves.wave);
                    if(weapon==2)drawText(c,"SUBTRATOR 2: MUNICAO INFINITA",400,230,16,Color.YELLOW,true);
                    else if(inv.hasWeapon(weapon))drawText(c,"SUBTRATOR "+weapon+"  MUNICAO "+inv.ammoForWeapon(weapon),400,230,16,Color.YELLOW,true);
                    else drawText(c,"SUBTRATOR "+weapon+" AINDA NAO ADQUIRIDO",400,230,15,Color.LTGRAY,true);
                }else if(lastDivisorAcquired>0){
                    drawText(c,"DIVISOR "+lastDivisorAcquired+" ADQUIRIDO",400,230,17,Color.YELLOW,true);
                }
                drawText(c,"INICIO EM "+Math.max(1,(int)Math.ceil(preWaveTimer)),400,274,18,Color.WHITE,true);
                drawText(c,phaseTrainingTip(),400,315,11,Color.LTGRAY,true);
            }

            if(waveClear){
                p.setColor(Color.argb(175,0,20,38));c.drawRect(0,0,800,390,p);
                drawText(c,"FASE "+completedWave+" CONCLUIDA",400,145,32,Color.CYAN,true);
                drawText(c,"SAUDE DA CIDADE: "+Math.round(cityHealth)+"%",400,184,17,cityHealth>50?Color.WHITE:Color.YELLOW,true);
                if(completedWave>1){
                    if(lastProtectedBonus>0){
                        String status=protectedSiteHealth>=99.5f?"MONUMENTO INTACTO":"MONUMENTO PARCIAL "+Math.round(protectedSiteHealth)+"%";
                        drawText(c,status+"  +R$ "+lastProtectedBonus,400,220,14,Color.YELLOW,true);
                    }else if(lastProtectedBonus<0){
                        drawText(c,"MONUMENTO DESTRUIDO  -R$ "+(-lastProtectedBonus),400,220,14,Color.RED,true);
                    }else{
                        drawText(c,"MONUMENTO DESTRUIDO  SEM SALDO PARA DESCONTO",400,220,13,Color.RED,true);
                    }
                }
                drawText(c,"BONUS DE TEMPO  +"+phaseTimeBonus+" PONTOS",400,254,15,Color.YELLOW,true);
                drawText(c,"TEMPO DE COMBATE  "+Math.round(phaseActiveSeconds)+"s",400,282,13,Color.WHITE,true);
            }
            if(intermission)drawIntermission(c);
            if(quizOpen)drawQuiz(c);
            if(paused)drawPauseMenu(c);
            if(victory)drawVictory(c);
            if(gameOver){
                p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,800,480,p);
                drawText(c,"FIM DE JOGO",400,205,38,Color.RED,true);
                drawText(c,"Pontos: "+score,400,245,24,Color.WHITE,true);
                drawMenuButton(c,new RectF(305,280,495,330),"REINICIAR",true);
            }
            drawHudNotices(c);
        }

        void drawPauseMenu(Canvas c){
            if(manualFromPause){drawManual(c,true);return;}
            p.setColor(Color.argb(205,0,0,0));c.drawRect(0,0,800,390,p);
            drawDarkCard(c,210,24,590,378);drawText(c,"PAUSADO",400,58,30,Color.WHITE,true);
            String[] labels={"CONTINUAR","MENU INICIAL","SALVAR JOGO","MANUAL","SAIR DO JOGO"};
            float top=78;
            for(int i=0;i<labels.length;i++){
                RectF r=pauseButtons[i];r.set(270,top+i*55,530,top+40+i*55);
                drawMenuButton(c,r,labels[i],i==0);
            }
            if(saveNotice)drawText(c,"JOGO SALVO",400,368,12,Color.YELLOW,true);
        }

        void drawManual(Canvas c,boolean overlay){
            if(overlay){p.setColor(Color.argb(225,0,0,0));c.drawRect(0,0,800,390,p);}
            drawDarkCard(c,62,20,738,452);
            p.setColor(Color.rgb(72,94,54));c.drawRect(72,30,728,72,p);
            drawText(c,"BRIEFING MILITAR // OPERACAO ESCUDO",400,58,23,Color.rgb(235,235,190),true);
            drawText(c,"DOCUMENTO DE CAMPO  "+(manualPage+1)+"/3",400,84,11,Color.LTGRAY,true);

            if(manualPage==0){
                drawText(c,"COMBATE E ARSENAL",92,112,14,Color.YELLOW,false);
                drawText(c,"SUBTRACAO: 2, 4, 8, 16, 32, 64 e 128 reduzem o valor do meteoro.",92,136,11,Color.WHITE,false);
                drawText(c,"A arma 2 tem municao infinita; as demais usam cargas limitadas.",92,158,11,Color.WHITE,false);
                drawText(c,"DIVISORES: 2, 3, 5, 7, 11, 13 e 17 preservam a mecanica classica.",92,180,11,Color.WHITE,false);
                drawText(c,"H custa R$ "+SHOP_H_COST+" e destrói instantaneamente um meteoro atingido.",92,208,11,Color.CYAN,false);
                drawText(c,"x0 custa R$ "+SHOP_BOMB_COST+" e elimina meteoros normais e coleta bonus da tela.",92,230,11,Color.WHITE,false);
                drawText(c,"A x0 NAO destrói quiz: ela reduz a conta para operandos de 1 algarismo.",92,252,11,Color.YELLOW,false);
                drawText(c,"MIRA: MUITO FACIL +30%, FACIL +25%, MEDIO +20%, DIFICIL +15%.",92,280,10,Color.LTGRAY,false);
                drawText(c,"MUITO DIFICIL +10%; INSANO usa somente o tamanho real do meteoro.",92,300,10,Color.LTGRAY,false);
                drawText(c,"A dificuldade tambem aumenta velocidade, valores e pressao dos quizzes.",92,326,10,Color.WHITE,false);
                drawMenuButton(c,new RectF(475,382,690,421),"ECONOMIA >",false);
            }else if(manualPage==1){
                drawText(c,"ECONOMIA E DEFESA",92,112,14,Color.YELLOW,false);
                drawText(c,"Cada acerto pode render dinheiro, mas cada meteoro tem um teto total.",92,136,11,Color.WHITE,false);
                drawText(c,"Esse teto e o valor original do meteoro: tiros fracos nao geram renda infinita.",92,158,11,Color.LTGRAY,false);
                drawText(c,"REPARAR custa 10% do saldo atual e recupera 10% da cidade.",92,186,11,Color.WHITE,false);
                drawText(c,"Cada fase e outra cidade: a nova cidade sempre comeca em 100%.",92,208,11,Color.WHITE,false);
                drawText(c,"Municao pode cair do ceu (+2) ou ser comprada no intervalo.",92,230,11,Color.WHITE,false);
                drawText(c,"O HUD mostra em tempo real quanto o monumento ainda pode render.",92,258,11,Color.CYAN,false);
                drawText(c,"Se o monumento for destruido, pode haver perda de dinheiro por dificuldade.",92,280,10,Color.LTGRAY,false);
                drawText(c,"Destrocos do aviao comercial podem atingir e danificar a cidade.",92,302,10,Color.LTGRAY,false);
                drawText(c,"No intervalo, o mesmo saldo disputa arma, municao, H e x0.",92,326,11,Color.YELLOW,false);
                drawMenuButton(c,new RectF(110,382,325,421),"< COMBATE",false);
                drawMenuButton(c,new RectF(475,382,690,421),"QUIZ >",false);
            }else{
                drawText(c,"QUIZ E DIFICULDADE",92,112,14,Color.YELLOW,false);
                drawText(c,"Desde a fase 1 podem aparecer +, -, x e /. A mesma conta nao se repete.",92,136,11,Color.WHITE,false);
                drawText(c,"Divisoes sao exatas e subtracoes nunca resultam em numero negativo.",92,158,11,Color.LTGRAY,false);
                drawText(c,"MUITO FACIL: contas menores e a maior tolerancia de mira.",92,184,11,Color.WHITE,false);
                drawText(c,"FACIL: x e / nas unidades; + e - ate dezenas.",92,206,11,Color.WHITE,false);
                drawText(c,"MEDIO: primeiro termo de x e / com 2 algarismos; + e - nas centenas.",92,228,10,Color.WHITE,false);
                drawText(c,"DIFICIL: primeiro termo de x e / nas centenas; + e - nos milhares.",92,250,10,Color.WHITE,false);
                drawText(c,"MUITO DIFICIL: x e / nos milhares; + e - nas dezenas de milhares.",92,272,10,Color.WHITE,false);
                drawText(c,"INSANO: numeros maiores e mira sem tolerancia extra.",92,294,10,Color.WHITE,false);
                drawText(c,"Quiz que atinge a cidade tira metade da saude atual.",92,320,11,Color.RED,false);
                drawText(c,"Resposta correta chama o ataque aereo militar contra o meteoro.",92,342,11,Color.CYAN,false);
                drawMenuButton(c,new RectF(110,382,325,421),"< ECONOMIA",false);
            }
            drawMenuButton(c,new RectF(315,423,485,446),"VOLTAR",false);
        }

        void drawIntermission(Canvas c){
            p.setColor(Color.argb(205,0,0,0));c.drawRect(0,0,800,390,p);
            drawDarkCard(c,145,18,655,378);
            drawText(c,"INTERVALO ENTRE FASES",400,52,24,Color.CYAN,true);
            drawText(c,"DINHEIRO: R$ "+inv.money+"   H x"+inv.hyperAmmo+"   x0 x"+inv.bombZero,400,82,14,Color.YELLOW,true);

            shopWeaponRect.set(205,104,595,139);
            shopAmmoRect.set(205,146,595,181);
            shopHyperRect.set(205,188,595,223);
            shopBombRect.set(205,230,595,265);
            shopNextRect.set(285,320,515,357);

            if(subtractionMechanic){
                int weapon=nextPurchasableWeapon();
                int initial=3*targetForPhase(weaponUnlockPhase(weapon));
                String weaponLabel;
                if(weapon<=2)weaponLabel="ARMA 2 INFINITA";
                else if(inv.hasWeapon(weapon))weaponLabel="ARMAS DISPONIVEIS ADQUIRIDAS";
                else weaponLabel="COMPRAR ARMA "+weapon+" +"+initial+"   R$ "+weaponPurchaseCost(weapon);
                drawMenuButton(c,shopWeaponRect,weaponLabel,false);

                int ammoWeapon=shopAmmoWeapon();
                String ammoLabel=(ammoWeapon>2&&inv.hasWeapon(ammoWeapon))
                        ?"MUNICAO "+ammoWeapon+" +2   R$ "+ammoPackCost(ammoWeapon)
                        :"MUNICAO: ADQUIRA UMA ARMA";
                drawMenuButton(c,shopAmmoRect,ammoLabel,false);
            }else{
                drawMenuButton(c,shopWeaponRect,"MECANICA DIVISOR ATIVA",false);
                drawMenuButton(c,shopAmmoRect,"DIVISORES SAO LIBERADOS POR FASE",false);
            }

            drawMenuButton(c,shopHyperRect,"ARMA H +1   R$ "+SHOP_H_COST,false);
            drawMenuButton(c,shopBombRect,"BOMBA x0 +1   R$ "+SHOP_BOMB_COST,false);
            drawText(c,"SALDO COMPARTILHADO: ARMA / MUNICAO / H / x0",400,288,10,Color.LTGRAY,true);
            int nextPhase=Math.min(WaveManager.MAX_WAVE,waves.wave+1);
            drawText(c,"PROXIMA CIDADE: "+phaseCityNameFor(nextPhase)+"   META "+targetForPhase(nextPhase),400,306,10,Color.CYAN,true);
            drawMenuButton(c,shopNextRect,"PROXIMA FASE",true);
        }

        void drawVictory(Canvas c){
            p.setColor(Color.argb(100,0,8,22));c.drawRect(0,0,800,480,p);
            drawText(c,"VITORIA!",400,118,46,Color.YELLOW,true);
            drawText(c,"25 FASES CONCLUIDAS",400,158,24,Color.WHITE,true);
            drawText(c,"PONTOS: "+score,400,193,19,Color.CYAN,true);
            victoryMenuRect.set(285,255,515,302);victoryExitRect.set(285,320,515,367);
            drawMenuButton(c,victoryMenuRect,"MENU INICIAL",true);
            drawMenuButton(c,victoryExitRect,"SAIR DO JOGO",false);
        }

        void drawStars(Canvas c){p.setColor(Color.rgb(120,160,205));for(int i=0;i<42;i++){int x=(i*97+31)%800;int y=(i*53+17)%305;c.drawRect(x,y,x+1,y+1,p);}}
        void drawCity(Canvas c){
            if(cityImg!=null)c.drawBitmap(cityImg,null,new RectF(0,CITY_TOP,800,CITY_BOTTOM),pixel);
            else{p.setColor(Color.DKGRAY);c.drawRect(0,CITY_TOP,800,CITY_BOTTOM,p);}
            int smokes=cityHealth>=75?0:1+(int)((75-cityHealth)/13f);
            for(int i=0;i<smokes;i++){
                float x=55+(i*137+lastImpactX/3)%690;
                drawSmoke(c,x,CITY_TOP+22+(i%2)*12);
            }
        }

        void drawProtectedSiteDamage(Canvas c){
            if(!protectedSiteActive()||protectedCells==null||cityDestroyed==null)return;
            int flicker=(int)(SystemClock.uptimeMillis()/85)%3;
            long blinkCycle=SystemClock.uptimeMillis()%5000L;
            boolean monumentBlink=blinkCycle<650L&&((blinkCycle/120L)&1L)==0L;

            for(int row=0;row<damageRows;row++){
                for(int col=0;col<damageCols;col++){
                    if(!protectedCells[row][col])continue;
                    RectF r=cellWorldRect(row,col);

                    if(!cityDestroyed[row][col]){
                        if(monumentBlink){
                            p.setColor(Color.argb(125,235,250,255));
                            c.drawRect(r,p);
                        }
                        continue;
                    }

                    p.setColor(Color.argb(205,28,25,22));c.drawRect(r,p);
                    p.setColor(Color.rgb(92,88,82));
                    c.drawRect(r.left,r.bottom-Math.max(1f,r.height()*.30f),r.right,r.bottom,p);
                    if(protectedSiteDestroyed){
                        p.setColor(((row+col)&1)==0?Color.rgb(255,70,10):Color.rgb(255,190,25));
                        float flameTop=r.top+Math.max(0f,r.height()*.12f-flicker);
                        c.drawRect(r.left+1,flameTop,r.right-1,r.bottom-1,p);
                    }
                }
            }
        }

        void drawUiParticles(Canvas c){
            for(Particle q:uiParticles){
                p.setColor(q.color);p.setAlpha((int)(255*q.life/q.maxLife));
                c.drawRect(q.x-q.size,q.y-q.size,q.x+q.size,q.y+q.size,p);
            }
            p.setAlpha(255);
        }
        void drawSmoke(Canvas c,float x,float y){if(smokeImg!=null){p.setAlpha(cityHealth<=30?155:115);c.drawBitmap(smokeImg,null,new RectF(x-9,y-34,x+13,y-12),pixel);p.setAlpha(255);}}

        void drawShieldDome(Canvas c){
            if(inv.shieldSeconds<=0)return;
            boolean ending=inv.shieldSeconds<=2.5f;
            if(ending&&((int)(inv.shieldSeconds*7f)&1)==0)return;

            float reveal=Math.min(1f,shieldVisualAge/.70f);
            reveal=1f-(1f-reveal)*(1f-reveal);
            float left=-55f,right=855f,base=306f,apex=125f;
            float half=(right-left)*.5f*reveal;

            Path edge=new Path();
            edge.moveTo(left,base);
            edge.cubicTo(135f,300f,260f,205f,400f,apex);
            edge.cubicTo(540f,205f,665f,300f,right,base);

            Path fill=new Path();
            fill.moveTo(left,base);
            fill.cubicTo(135f,300f,260f,205f,400f,apex);
            fill.cubicTo(540f,205f,665f,300f,right,base);
            fill.lineTo(right,390f);
            fill.lineTo(left,390f);
            fill.close();

            c.save();
            c.clipRect(Math.max(0f,400f-half),105f,Math.min(800f,400f+half),391f);
            int pulse=(int)(6+5*Math.abs(Math.sin(SystemClock.uptimeMillis()/210.0)));

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(18+pulse,245,250,255));
            c.drawPath(fill,p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3.2f);
            p.setColor(Color.argb(180,235,248,255));
            c.drawPath(edge,p);
            p.setStrokeWidth(1.1f);
            p.setColor(Color.argb(80,255,255,255));
            c.drawPath(edge,p);

            p.setStyle(Paint.Style.FILL);
            c.restore();
        }

        void updateCityFireParticles(float dt){if(cityHealth>30)return;fireParticleTimer-=dt;if(fireParticleTimer>0)return;fireParticleTimer=.045f+.035f*rnd.nextFloat();int count=2+(cityHealth<15?2:0);for(int i=0;i<count;i++){float x=55+rnd.nextFloat()*690f;float y=350+rnd.nextFloat()*25f;int col=rnd.nextBoolean()?Color.rgb(255,80,15):Color.rgb(255,190,30);particles.add(new Particle(x,y,-12+rnd.nextFloat()*24,-55-rnd.nextFloat()*55,.42f+rnd.nextFloat()*.35f,col,2+rnd.nextFloat()*3));}}
        void emitMeteorTrail(Meteor m,float dt){
            if(m.kind==Kind.BONUS)return;
            m.tailTimer-=dt;if(m.tailTimer>0)return;
            m.tailTimer=.030f+.030f*rnd.nextFloat();
            int col=Color.rgb(255,145,30);
            if(m.kind==Kind.MULT)col=Color.rgb(75,220,105);
            else if(m.kind==Kind.ADD)col=Color.rgb(255,215,55);
            else if(m.kind==Kind.SUB)col=Color.rgb(255,105,85);
            else if(m.kind==Kind.DIV)col=Color.rgb(80,205,255);
            for(int i=0;i<3;i++){
                float tx=m.x-m.radius+rnd.nextFloat()*(m.radius*2f);
                float ty=m.y-m.radius-rnd.nextFloat()*(m.radius*2f);
                particles.add(new Particle(tx,ty,-8+rnd.nextFloat()*16,-5-rnd.nextFloat()*18,.22f+rnd.nextFloat()*.24f,col,1.4f+rnd.nextFloat()*2.4f));
            }
        }
        void saveScore(){if(scoreSaved||score<=0||destroyedTotal<=0)return;scoreSaved=true;try{SharedPreferences sp=getContext().getSharedPreferences("pontuacao",Context.MODE_PRIVATE);String date=new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss",java.util.Locale.getDefault()).format(new java.util.Date());int check=score%(destroyedTotal+1);sp.edit().putString(date,date+"|"+score+"|"+destroyedTotal+"|"+check).apply();}catch(Exception ignored){}}
        float deployedCannonY(){return cannonY+(1f-cannonDeploy)*(towerBaseY-cannonY);}

        void drawCannon(Canvas c){
            float cy=deployedCannonY();
            int towerFrame=(int)((SystemClock.uptimeMillis()/80)%8);
            c.save();c.clipRect(0,0,800,towerBaseY);
            float towerHeight=towerBaseY-cannonY;
            if(turretSheet!=null)drawTile(c,turretSheet,8,1,towerFrame,new RectF(cannonX-21,cy,cannonX+21,cy+towerHeight),pixel);
            int frame=8;
            if(aiming&&aimCharge>0)frame=8+Math.min(7,(int)(aimCharge*7f));
            if(cannonAnim>0){float elapsed=.40f-cannonAnim;frame=Math.max(0,Math.min(7,(int)(elapsed/.05f)));}
            if(cannonSheet!=null){
                c.save();c.rotate(cannonAngle,cannonX,cy);
                drawTile(c,cannonSheet,8,2,frame,new RectF(cannonX-33,cy-33,cannonX+33,cy+33),pixel);
                c.restore();
            }
            c.restore();
        }
        void drawProjectile(Canvas c){
            if(shotTimer<=0)return;
            float t=1f-shotTimer/shotDuration;t=Math.max(0,Math.min(1,t));
            if(laserEnabled){p.setColor(Color.argb(100,220,245,255));p.setStrokeWidth(1.2f);c.drawLine(shotStartX,shotStartY,shotTargetX,shotTargetY,p);}
            float x=shotStartX+(shotTargetX-shotStartX)*t;
            float y=shotStartY+(shotTargetY-shotStartY)*t;
            if(shotHyper){
                float pulse=16f+3f*(float)Math.sin(SystemClock.uptimeMillis()/35.0);
                p.setColor(Color.argb(80,180,245,255));c.drawCircle(x,y,pulse,p);
                p.setColor(Color.argb(165,255,255,255));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);c.drawCircle(x,y,pulse+4f,p);p.setStyle(Paint.Style.FILL);
            }
            float half=shotHyper?16f:10f;
            if(projectileSheet!=null){
                drawTile(c,projectileSheet,8,1,shotProjectileIndex,new RectF(x-half,y-half,x+half,y+half),pixel);
            }else{
                p.setColor(Color.WHITE);c.drawCircle(x,y,shotHyper?7f:4f,p);
            }
        }

        void drawAimTarget(Canvas c){
            if(!aiming&&aimTargetTimer<=0)return;
            float pulse=1f+.08f*(float)Math.sin(SystemClock.uptimeMillis()/75.0);
            float r=25f*pulse;
            if(aimTargetSheet!=null)drawTile(c,aimTargetSheet,8,1,aimTargetIndex,new RectF(aimX-r,aimY-r,aimX+r,aimY+r),pixel);
            else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(aimX,aimY,r,p);p.setStyle(Paint.Style.FILL);}
            if(aiming){
                String v;
                if(selectedMode==1)v="H "+MeteorMathV2.MAX_METEOR_VALUE;
                else if(subtractionMechanic)v="SUB "+inv.selectedWeapon;
                else v="DIV "+currentChargedDivisor();
                drawOutlinedText(c,v,aimX,Math.max(18,aimY-r-7),13,aimCharge>0?Color.CYAN:Color.WHITE);
            }
        }

        void drawMeteor(Canvas c,Meteor m){
            if(m.kind==Kind.BONUS){
                drawBonusMeteor(c,m);
            }else{
                Paint use=pixel;
                if(m.kind==Kind.MULT){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(80,205,105),PorterDuff.Mode.MULTIPLY));use=tintPaint;}
                else if(m.kind==Kind.ADD){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(255,220,80),PorterDuff.Mode.MULTIPLY));use=tintPaint;}
                else if(m.kind==Kind.SUB){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(255,115,95),PorterDuff.Mode.MULTIPLY));use=tintPaint;}
                else if(m.kind==Kind.DIV){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(100,210,255),PorterDuff.Mode.MULTIPLY));use=tintPaint;}
                int frame=((int)(SystemClock.uptimeMillis()/130)+Math.abs(m.originalValue))%4;
                RectF dest=new RectF(m.x-m.radius,m.y-m.radius,m.x+m.radius,m.y+m.radius);
                if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,frame,dest,use);
                else{p.setColor(Color.rgb(100,88,68));c.drawCircle(m.x,m.y,m.radius,p);}
                tintPaint.setColorFilter(null);
                String text;
                if(bombSequence&&!isQuizMeteor(m))text="0x"+m.value;
                else text=isQuizMeteor(m)&&m.quiz!=null?m.quiz.expression():String.valueOf(m.value);
                drawText(c,text,m.x,m.y+5,15,Color.WHITE,true);
            }
            if(m.targetBlink&&((int)(m.blinkTime*10)%2==0)){
                if(targetImg!=null)c.drawBitmap(targetImg,null,new RectF(m.x-25,m.y-25,m.x+25,m.y+25),pixel);
                else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);c.drawCircle(m.x,m.y,m.radius+8,p);p.setStyle(Paint.Style.FILL);}
            }
        }

        void drawBonusMeteor(Canvas c,Meteor m){
            RectF dst=new RectF(m.x-17,m.y-17,m.x+17,m.y+17);
            if(m.bonus==Bonus.AMMO&&projectileSheet!=null){
                int idx=subtractionMechanic?projectileIndexForSubtractor(m.ammoValue):projectileIndexForDivisor(m.ammoValue);
                drawTile(c,projectileSheet,8,1,idx,dst,pixel);
            }else if(m.bonus==Bonus.HYPER&&projectileSheet!=null){
                drawTile(c,projectileSheet,8,1,7,dst,pixel);
            }else{
                Bitmap icon=null;
                if(m.bonus==Bonus.MONEY)icon=moneyImg;
                else if(m.bonus==Bonus.BOMB0)icon=bombImg;
                if(icon!=null)c.drawBitmap(icon,null,dst,pixel);
                else{
                    int tile=m.bonus==Bonus.HEALTH?4:m.bonus==Bonus.SHIELD?5:1;
                    if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,tile,dst,pixel);
                    else{p.setColor(Color.rgb(60,135,190));c.drawCircle(m.x,m.y,17,p);}
                }
            }
            drawText(c,bonusText(m),m.x,m.y+5,12,Color.WHITE,true);
        }
        String bonusText(Meteor m){
            switch(m.bonus){
                case AMMO:return subtractionMechanic?"+2 /"+m.ammoValue:"DIV "+m.ammoValue;
                case HYPER:return "H +1";
                case MONEY:return "$"+m.value;
                case HEALTH:return "+"+m.value;
                case SHIELD:return "ESC";
                default:return "x0";
            }
        }
        void drawPlaneDebris(Canvas c){
            if(planeCommercial==null)return;
            for(PlaneFragment q:planeDebris){
                c.save();
                c.rotate(q.angle,q.x,q.y);
                RectF dst=new RectF(q.x-q.w*.5f,q.y-q.h*.5f,q.x+q.w*.5f,q.y+q.h*.5f);
                c.drawBitmap(planeCommercial,q.src,dst,pixel);
                c.restore();
            }
        }

        void drawPlane(Canvas c,Plane pl){
            Bitmap b=pl.military?planeMilitary:planeCommercial;
            RectF r=pl.bounds();
            if(b!=null)c.drawBitmap(b,null,r,pixel);
            else{p.setColor(pl.military?Color.GREEN:Color.WHITE);c.drawRect(r,p);}
            boolean blink=((SystemClock.uptimeMillis()/180)%2)==0;
            if(blink){
                p.setColor(pl.military?Color.RED:Color.rgb(80,220,255));
                c.drawCircle(r.left+8,r.centerY(),2.2f,p);
                p.setColor(pl.military?Color.rgb(80,255,100):Color.RED);
                c.drawCircle(r.right-8,r.centerY(),2.2f,p);
            }
            if(pl.military&&pl.target!=null&&!pl.target.dead){
                p.setColor(Color.argb(95,255,70,50));p.setStrokeWidth(1.2f);
                c.drawLine(pl.x,pl.y+10,pl.target.x,pl.target.y,p);
            }
            if(pl.military&&pl.bombActive){
                c.save();
                c.rotate(pl.bombAngle,pl.bombX,pl.bombY);
                RectF br=new RectF(pl.bombX-9,pl.bombY-5,pl.bombX+9,pl.bombY+5);
                if(planeBombImg!=null)c.drawBitmap(planeBombImg,null,br,pixel);
                else{
                    p.setColor(Color.rgb(72,82,48));c.drawRoundRect(br,4,4,p);
                    p.setColor(Color.YELLOW);c.drawRect(pl.bombX+2,pl.bombY-5,pl.bombX+4,pl.bombY+5,p);
                }
                c.restore();
            }
        }

        void drawHud(Canvas c){
            p.setColor(Color.rgb(3,18,34));c.drawRect(0,390,800,480,p);
            p.setColor(Color.rgb(28,112,158));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(3,393,797,477,p);p.setStyle(Paint.Style.FILL);

            RectF arsenalPanel=new RectF(8,397,386,472);
            RectF hPanel=new RectF(392,397,500,472);
            RectF bombPanel=new RectF(506,397,580,472);
            RectF repairPanel=new RectF(586,397,792,472);
            RectF[] panels={arsenalPanel,hPanel,bombPanel,repairPanel};
            for(RectF panel:panels){
                p.setColor(Color.rgb(7,32,50));c.drawRect(panel,p);
                p.setColor(Color.rgb(25,92,127));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawRect(panel,p);p.setStyle(Paint.Style.FILL);
            }

            drawText(c,subtractionMechanic?"SUBTRATOR":"DIVISOR",16,409,10,Color.LTGRAY,false);
            int x=14;
            for(int i=0;i<divisorRects.length;i++){
                int value=subtractionMechanic?MeteorMathV2.SUBTRACTORS[i]:MeteorMathV2.PRIMES[i];
                RectF r=new RectF(x,413,x+45,467);divisorRects[i]=r;
                boolean have=subtractionMechanic?inv.hasWeapon(value):inv.divisors.contains(value);
                boolean selected=have&&selectedMode==0&&(subtractionMechanic?inv.selectedWeapon==value:inv.selectedDivisor==value);
                p.setColor(have?Color.rgb(16,45,62):Color.rgb(28,31,35));c.drawRect(r,p);
                if(selected){
                    p.setColor(Color.CYAN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(r,p);p.setStyle(Paint.Style.FILL);
                }
                if(projectileSheet!=null){
                    p.setAlpha(have?255:65);
                    drawTile(c,projectileSheet,8,1,i,new RectF(r.centerX()-10,r.top+3,r.centerX()+10,r.top+23),pixel);
                    p.setAlpha(255);
                }
                if(subtractionMechanic){
                    drawText(c,String.valueOf(value),r.centerX(),r.top+34,9,have?Color.WHITE:Color.GRAY,true);
                    String qty=value==2?"∞":String.valueOf(inv.ammoForWeapon(value));
                    drawText(c,qty,r.centerX(),r.bottom-3,11,have?Color.YELLOW:Color.DKGRAY,true);
                }else{
                    drawText(c,String.valueOf(value),r.centerX(),r.bottom-5,13,have?Color.WHITE:Color.GRAY,true);
                }
                x+=52;
            }

            subMinus.setEmpty();subPlus.setEmpty();subUse.set(397,402,495,467);
            p.setColor(selectedMode==1?Color.rgb(38,92,120):Color.rgb(14,51,69));c.drawRoundRect(subUse,4,4,p);
            drawText(c,"H  "+MeteorMathV2.MAX_METEOR_VALUE,446,412,9,Color.LTGRAY,true);
            if(projectileSheet!=null)drawTile(c,projectileSheet,8,1,7,new RectF(436,418,456,438),pixel);
            drawText(c,"x"+inv.hyperAmmo,446,461,12,Color.YELLOW,true);

            bombRect.set(511,402,575,467);
            p.setColor(Color.rgb(64,47,18));c.drawRoundRect(bombRect,4,4,p);
            drawText(c,"BOMBA x0",543,412,8,Color.LTGRAY,true);
            if(bombImg!=null)c.drawBitmap(bombImg,null,new RectF(527,419,559,451),pixel);
            drawText(c,"x"+inv.bombZero,543,463,10,Color.YELLOW,true);

            repairRect.set(592,402,786,467);
            p.setColor(Color.rgb(23,72,44));c.drawRoundRect(repairRect,4,4,p);
            if(moneyImg!=null)c.drawBitmap(moneyImg,null,new RectF(600,414,628,442),pixel);
            drawText(c,"R$ "+inv.money,706,420,11,Color.YELLOW,true);
            drawText(c,"REPARAR +10%",706,440,10,Color.WHITE,true);
            drawText(c,"CUSTO R$ "+inv.repairCost10Percent(),706,458,9,Color.LTGRAY,true);

            drawText(c,"FASE "+waves.wave+"   "+waves.destroyedThisWave+"/"+waves.targetThisWave+"   MAX "+waves.maxMeteorValue(),12,22,14,Color.WHITE,false);
            drawText(c,"PONTOS "+score,12,43,14,Color.YELLOW,false);
            p.setColor(Color.rgb(60,20,20));c.drawRect(12,55,220,71,p);
            p.setColor(cityHealth>60?Color.GREEN:cityHealth>30?Color.YELLOW:Color.RED);c.drawRect(12,55,12+208*cityHealth/100f,71,p);
            drawText(c,"CIDADE "+Math.round(cityHealth)+"%",116,68,11,Color.WHITE,true);
            if(inv.shieldSeconds>0)drawText(c,"ESCUDO "+(int)Math.ceil(inv.shieldSeconds),250,22,13,Color.CYAN,false);
            drawText(c,"HORA "+phaseClock,650,22,13,Color.WHITE,false);
            drawText(c,phaseWeatherLabel(),650,43,10,phaseRain?Color.CYAN:Color.LTGRAY,false);
            if(protectedSiteActive()){
                drawText(c,"ALVO "+protectedSiteName(),250,43,10,protectedSiteDestroyed?Color.RED:Color.WHITE,false);
                p.setColor(Color.rgb(55,22,18));c.drawRect(250,55,480,68,p);
                p.setColor(protectedSiteHealth>60?Color.CYAN:protectedSiteHealth>30?Color.YELLOW:Color.RED);
                c.drawRect(250,55,250+230*protectedSiteHealth/100f,68,p);
                if(protectedSiteDestroyed)drawText(c,"RISCO -R$ "+projectedProtectedPenalty(),490,67,10,Color.RED,false);
                else drawText(c,"BONUS R$ "+projectedProtectedReward(),490,67,10,Color.YELLOW,false);
            }

            if(running&&!preWave&&!gameOver){
                pauseRect.set(746,351,792,386);
                p.setColor(Color.argb(220,72,58,28));c.drawRoundRect(pauseRect,5,5,p);
                p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);c.drawRoundRect(pauseRect,5,5,p);p.setStyle(Paint.Style.FILL);
                drawText(c,paused?">":"II",pauseRect.centerX(),376,18,Color.WHITE,true);
            }else pauseRect.setEmpty();
        }

        void drawOutlinedText(Canvas c,String s,float x,float y,float size,int color){drawText(c,s,x+1,y+1,size,Color.BLACK,true);drawText(c,s,x,y,size,color,true);}
        void drawQuiz(Canvas c){
            p.setColor(Color.argb(215,0,0,0));c.drawRect(0,0,800,390,p);
            drawDarkCard(c,170,95,630,350);
            String q=quizMeteor.quiz.expression()+" = ?";
            int qColor=Color.YELLOW;
            if(quizMeteor.kind==Kind.MULT)qColor=Color.rgb(100,255,130);
            else if(quizMeteor.kind==Kind.SUB)qColor=Color.rgb(255,130,110);
            else if(quizMeteor.kind==Kind.DIV)qColor=Color.CYAN;
            drawText(c,q,400,158,32,qColor,true);
            drawText(c,"Escolha o resultado  (tentativa "+(quizAttempts+1)+"/2)",400,195,15,Color.WHITE,true);
            for(int i=0;i<3;i++){
                float left=225+i*125;
                RectF r=new RectF(left,235,left+100,300);
                drawMenuButton(c,r,String.valueOf(quizMeteor.quiz.options[i]),false);
            }
        }
        void drawCenterCrop(Canvas c,Bitmap b,RectF dst,float biasY){float srcRatio=b.getWidth()/(float)b.getHeight(),dstRatio=dst.width()/dst.height();Rect src;if(srcRatio>dstRatio){int sw=Math.round(b.getHeight()*dstRatio);int left=(b.getWidth()-sw)/2;src=new Rect(left,0,left+sw,b.getHeight());}else{int sh=Math.round(b.getWidth()/dstRatio);int extra=b.getHeight()-sh;int top=Math.round(extra*Math.max(0,Math.min(1,biasY)));src=new Rect(0,top,b.getWidth(),top+sh);}c.drawBitmap(b,src,dst,pixel);}
        void drawTile(Canvas c,Bitmap sheet,int cols,int rows,int index,RectF dst,Paint paint){if(sheet==null)return;int tw=sheet.getWidth()/cols,th=sheet.getHeight()/rows;index=Math.max(0,Math.min(cols*rows-1,index));int col=index%cols,row=index/cols;c.drawBitmap(sheet,new Rect(col*tw,row*th,col*tw+tw,row*th+th),dst,paint);}
        void drawDarkCard(Canvas c,float l,float t,float r,float b){p.setColor(Color.argb(218,0,13,30));c.drawRect(l,t,r,b,p);p.setColor(Color.rgb(35,130,185));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);}
        void drawMenuButton(Canvas c,RectF r,String label,boolean highlight){p.setColor(highlight?Color.argb(230,0,65,105):Color.argb(225,0,25,50));c.drawRect(r,p);p.setColor(highlight?Color.CYAN:Color.rgb(40,135,190));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(highlight?3:2);c.drawRect(r,p);p.setStyle(Paint.Style.FILL);drawText(c,label,r.centerX(),r.centerY()+7,18,Color.WHITE,true);}
        void drawText(Canvas c,String s,float x,float y,float size,int color,boolean center){p.setTypeface(gameFont);p.setFakeBoldText(true);p.setTextSize(size);p.setColor(color);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}

        boolean canAimAt(float x,float y){
            return running&&!preWave&&!paused&&!quizOpen&&!gameOver&&!waveClear&&!bombSequence&&!intermission&&!victory
                    &&y<390&&!pauseRect.contains(x,y);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=lx(e.getX()),y=ly(e.getY());
            int action=e.getActionMasked();

            if(action==MotionEvent.ACTION_DOWN){
                if(canAimAt(x,y)){
                    aiming=true;aimDownTime=SystemClock.uptimeMillis();aimCharge=0f;chargeParticleTimer=0f;
                    aimTargetIndex=currentAimIndex();
                    chargedProjectileValue=currentChargedDivisor();
                    pointCannonAt(x,y);
                    audio.play("alvo_trava.wav");
                }
                return true;
            }

            if(action==MotionEvent.ACTION_MOVE){
                if(aiming)pointCannonAt(x,y);
                return true;
            }

            if(action==MotionEvent.ACTION_CANCEL){
                aiming=false;aimCharge=0f;aimTargetTimer=0f;
                if(shotTimer<=0)cannonAngle=-45f;
                return true;
            }

            if(action!=MotionEvent.ACTION_UP)return true;

            if(aiming){
                pointCannonAt(x,y);
                releaseAimedShot();
                return true;
            }

            audio.play("ui_click.wav");
            if(victory){if(victoryMenuRect.contains(x,y))returnToMainMenu();else if(victoryExitRect.contains(x,y))GameV2Activity.this.finish();return true;}
            if(intermission){handleIntermissionTouch(x,y);return true;}
            if(!running&&!preWave&&!gameOver&&!waveClear){handleMenuTouch(x,y);return true;}
            if(gameOver){if(x>=285&&x<=515&&y>=265&&y<=350)resetGame();return true;}
            if(waveClear)return true;
            if(running&&!preWave&&pauseRect.contains(x,y)){paused=!paused;audio.setMusicPaused(paused);aiming=false;return true;}
            if(paused){handlePauseTouch(x,y);return true;}
            if(bombSequence)return true;
            if(preWave)return true;
            if(quizOpen){
                if(y>=225&&y<=315){
                    for(int i=0;i<3;i++){
                        float l=225+i*125;
                        if(x>=l&&x<=l+100){answerQuiz(quizMeteor.quiz.options[i]);return true;}
                    }
                }
                return true;
            }

            for(int i=0;i<divisorRects.length;i++){
                RectF r=divisorRects[i];
                if(r==null||!r.contains(x,y))continue;
                if(subtractionMechanic){
                    int weapon=MeteorMathV2.SUBTRACTORS[i];
                    if(inv.hasWeapon(weapon)){selectedMode=0;inv.selectedWeapon=weapon;return true;}
                }else{
                    int divisor=MeteorMathV2.PRIMES[i];
                    if(inv.divisors.contains(divisor)){selectedMode=0;inv.selectedDivisor=divisor;return true;}
                }
            }

            if(subUse.contains(x,y)){selectedMode=1;return true;}
            if(bombRect.contains(x,y)){startBombSequence();return true;}
            if(repairRect.contains(x,y)){
                int cost=inv.repairCost10Percent();
                if(inv.repair10Percent(cityHealth)){
                    cityHealth=Math.min(100f,cityHealth+10f);
                    addHudNotice("-R$ "+cost+"  REPARO +10%",Color.GREEN);
                    audio.play("reconstrucao_cidade.wav");
                }else if(cityHealth>=100f)addHudNotice("CIDADE JA ESTA 100%",Color.LTGRAY);
                else addHudNotice("SEM SALDO PARA REPARAR",Color.RED);
                return true;
            }
            return true;
        }

        void handleIntermissionTouch(float x,float y){
            if(subtractionMechanic&&shopWeaponRect.contains(x,y)){buyCurrentWeapon();return;}
            if(subtractionMechanic&&shopAmmoRect.contains(x,y)){buyAmmoPack();return;}
            if(shopHyperRect.contains(x,y)){buyHyper();return;}
            if(shopBombRect.contains(x,y)){buyBomb();return;}
            if(shopNextRect.contains(x,y)){continueFromShop();return;}
        }

        void handlePauseTouch(float x,float y){
            if(manualFromPause){
                if(manualPage==0&&x>=455&&x<=710&&y>=382&&y<=421){manualPage=1;return;}
                if(manualPage==1&&x>=90&&x<=345&&y>=382&&y<=421){manualPage=0;return;}
                if(manualPage==1&&x>=455&&x<=710&&y>=382&&y<=421){manualPage=2;return;}
                if(manualPage==2&&x>=90&&x<=345&&y>=382&&y<=421){manualPage=1;return;}
                if(x>=290&&x<=510&&y>=423&&y<=455){manualFromPause=false;manualPage=0;}
                return;
            }
            for(int i=0;i<pauseButtons.length;i++)if(pauseButtons[i].contains(x,y)){
                if(i==0){paused=false;audio.setMusicPaused(false);}
                else if(i==1)returnToMainMenu();
                else if(i==2)saveGame();
                else if(i==3){manualFromPause=true;manualPage=0;}
                else GameV2Activity.this.finish();
                return;
            }
        }

        void handleMenuTouch(float x,float y){
            if(menuPage==MENU_MAIN){
                for(int i=0;i<menuButtons.length;i++)if(menuButtons[i].contains(x,y)){
                    if(i==0)resetGame();
                    else if(i==1){if(loadSavedGame())menuPage=MENU_MAIN;}
                    else if(i==2)menuPage=MENU_SCORE;
                    else if(i==3)menuPage=MENU_OPTIONS;
                    else GameV2Activity.this.finish();
                    return;
                }
            }
            else if(menuPage==MENU_SCORE){
                if(x>=290&&x<=510&&y>=335&&y<=410)menuPage=MENU_MAIN;
            }
            else if(menuPage==MENU_OPTIONS){
                if(x>=235&&x<=565&&y>=60&&y<=96){selectedDifficulty=(selectedDifficulty+1)%6;waves.difficulty=selectedDifficulty;}
                else if(x>=235&&x<=565&&y>=101&&y<=137){subtractionMechanic=!subtractionMechanic;saveSettings();}
                else if(x>=235&&x<=565&&y>=142&&y<=178)laserEnabled=!laserEnabled;
                else if(x>=235&&x<=565&&y>=183&&y<=219)vibrationEnabled=!vibrationEnabled;
                else if(x>=235&&x<=565&&y>=224&&y<=260){dynamicWeatherEnabled=!dynamicWeatherEnabled;saveSettings();}
                else if(x>=235&&x<=565&&y>=286&&y<=322){menuPage=MENU_MANUAL;manualPage=0;}
                else if(x>=235&&x<=565&&y>=327&&y<=363){
                    getContext().getSharedPreferences("pontuacao",Context.MODE_PRIVATE).edit().clear().apply();
                    scoreClearedNotice=true;scoreNoticeTimer=1.5f;
                }
                else if(x>=290&&x<=510&&y>=397&&y<=433)menuPage=MENU_MAIN;
            }
            else if(menuPage==MENU_MANUAL){
                if(manualPage==0&&x>=455&&x<=710&&y>=382&&y<=421){manualPage=1;return;}
                if(manualPage==1&&x>=90&&x<=345&&y>=382&&y<=421){manualPage=0;return;}
                if(manualPage==1&&x>=455&&x<=710&&y>=382&&y<=421){manualPage=2;return;}
                if(manualPage==2&&x>=90&&x<=345&&y>=382&&y<=421){manualPage=1;return;}
                if(x>=290&&x<=510&&y>=423&&y<=455){menuPage=MENU_OPTIONS;manualPage=0;}
            }
        }
    }
}
