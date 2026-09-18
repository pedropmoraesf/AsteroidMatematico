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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

    enum Kind { NORMAL, ADD, MULT, BONUS }
    enum Bonus { AMMO, SUBTRACTOR, MONEY, HEALTH, SHIELD, BOMB0 }

    static final class Meteor {
        float x,y,radius,speed; int value, originalValue; Kind kind; Bonus bonus; int ammoValue;
        MeteorMathV2.Quiz quiz; boolean dead, targetBlink; float blinkTime, tailTimer;
        RectF bounds(){return new RectF(x-radius,y-radius,x+radius,y+radius);}
    }

    static final class Plane {
        float x,y,baseY,speed,bobPhase,trailTimer; boolean military,active=true; Meteor target; float strikeTimer;
        boolean bombActive; float bombX,bombY,bombVY,bombAngle;
        RectF bounds(){ return new RectF(x-(military?50:56),y-(military?17:19),x+(military?50:56),y+(military?17:19)); }
    }

    final class GameView extends View implements Runnable {
        static final int MENU_MAIN=0, MENU_SCORE=1, MENU_OPTIONS=2, MENU_MANUAL=3;
        static final float PRE_WAVE_DURATION=4.0f;
        static final int SHOP_SUBTRACTOR_COST=50, SHOP_BOMB_COST=100;
        static final int CITY_TILE_W=32, CITY_TILE_H=15, CITY_TILE_COLS=25, CITY_TILE_ROWS=5;
        static final float CITY_TOP=315f, CITY_BOTTOM=390f;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pixel=new Paint();
        final Paint tintPaint=new Paint();
        final Random rnd=new Random();
        final PlayerInventory inv=new PlayerInventory();
        final WaveManager waves=new WaveManager();
        final List<Meteor> meteors=new ArrayList<Meteor>();
        final List<Particle> particles=new ArrayList<Particle>();
        final List<Particle> uiParticles=new ArrayList<Particle>();
        final AudioBank audio;
        final RectF[] divisorRects=new RectF[MeteorMathV2.PRIMES.length];
        final RectF subMinus=new RectF(),subPlus=new RectF(),subUse=new RectF(),bombRect=new RectF(),repairRect=new RectF(),pauseRect=new RectF();
        final RectF[] menuButtons={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        final RectF[] pauseButtons={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
        final RectF victoryMenuRect=new RectF(),victoryExitRect=new RectF();
        final RectF shopSubRect=new RectF(),shopBombRect=new RectF(),shopNextRect=new RectF();

        Bitmap menuBg,lane,moneyImg,subImg,planeCommercial,planeMilitary,bombImg,targetImg,aimTargetSheet,planeBombImg;
        Bitmap cityImg,baseCityImg,turretSheet,cannonSheet,meteorSheet,projectileSheet,smokeImg,molduraSheet;
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
        float bombSequenceTimer=0f;
        Meteor pendingBonusTarget;
        float pendingBonusTimer=0f;
        int lastDivisorAcquired=0;
        boolean aiming=false;
        float aimX=400f,aimY=180f,aimTargetTimer=0f,aimCharge=0f,chargeParticleTimer=0f;
        long aimDownTime=0L;
        int aimTargetIndex=0,shotProjectileIndex=0,chargedProjectileValue=2;
        boolean aimCharged=false,shotWasCharged=false;
        float projectileParticleTimer=0f,hudChargeFlashTimer=0f;
        int shotProjectileValue=2;
        int manualPage=0;
        float dirtParticleTimer=0f;

        float protectedSiteHealth=100f,protectedFireTimer=0f;
        boolean protectedSiteDestroyed=false,protectedSiteBonusAwarded=false;
        int protectedTileStart=0,protectedTileEnd=-1,lastProtectedBonus=0;

        boolean cityShaking=false;
        float cityShakeT=0f, cityShakeT2=0f, cityShakeAlpha=0f, cityShakeGamma=0f, cityShakeOffset=0f;
        int lastImpactX=400;

        GameView(Context c){
            super(c); setFocusable(true); pixel.setAntiAlias(false); pixel.setFilterBitmap(false); tintPaint.setAntiAlias(false);
            audio=new AudioBank(c); loadAssets(); loadAudio();
            introWhiteFade=GameV2Activity.this.getIntent().getBooleanExtra("fromSplash",false)?.30f:0f;
            audio.playMusic("musica_menu.ogg",.42f);
        }

        void loadAssets(){
            menuBg=assetBitmap("graficos/menu_cena.png");
            lane=assetBitmap("graficos/lane_armas.png"); moneyImg=assetBitmap("graficos/dinheiro_bonus.png");
            subImg=assetBitmap("graficos/subtrator.png"); planeCommercial=assetBitmap("graficos/aviao_comercial.png");
            planeMilitary=assetBitmap("graficos/aviao_militar.png"); bombImg=assetBitmap("graficos/bomba0.png");
            planeBombImg=assetBitmap("graficos/bomba_aviao_militar.png");
            targetImg=assetBitmap("graficos/alvo_quiz.png"); aimTargetSheet=assetBitmap("graficos/alvo1.png");
            baseCityImg=assetBitmap("graficos/cidade_grande.png"); cityImg=baseCityImg; turretSheet=assetBitmap("graficos/torre.png");
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
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){scaleX=w/logicalW;scaleY=h/logicalH;}
        float lx(float x){return x/scaleX;} float ly(float y){return y/scaleY;}

        void resetGame(){
            meteors.clear();particles.clear();cityHealth=100;score=0;running=false;preWave=true;gameOver=false;quizOpen=false;paused=false;
            commercial=null;military=null;selectedMode=0;cityShaking=false;cityShakeOffset=0;cannonAngle=-45;cannonAnim=0;shotTimer=0;
            destroyedTotal=0;scoreSaved=false;fireParticleTimer=0;shieldVisualAge=0;shieldWasActive=false;cannonDeploy=0;
            waveClear=false;waveClearTimer=0;completedWave=0;shotColor=Color.YELLOW;
            victory=false;fireworkTimer=0;saveNotice=false;saveNoticeTimer=0;
            intermission=false;manualFromPause=false;bombSequence=false;bombSequenceTimer=0;pendingBonusTarget=null;pendingBonusTimer=0;lastDivisorAcquired=0;
            aiming=false;aimTargetTimer=0;aimCharge=0;chargeParticleTimer=0;aimDownTime=0;aimTargetIndex=0;shotProjectileIndex=0;chargedProjectileValue=2;manualPage=0;dirtParticleTimer=0;
            waves.wave=1;waves.destroyedThisWave=0;waves.targetThisWave=5;waves.difficulty=selectedDifficulty;
            inv.divisors.clear();inv.divisors.add(2);inv.divisors.add(3);inv.selectedDivisor=2;
            inv.subtractorCharge=0;inv.subtractorValue=1;inv.money=0;inv.bombZero=0;inv.shieldSeconds=0;
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
            aiming=false;aimTargetTimer=0;aimCharge=0;dirtParticleTimer=0;
            preparePhase();
            audio.setMusicPaused(false);playWaveMusic();audio.playLong("sirene_80bpm_10.wav");
        }

        String phaseCityName(){
            String[] names={"","SAO GONCALO/RJ","RIO DE JANEIRO","SAO PAULO","BELO HORIZONTE","SALVADOR","RECIFE","FORTALEZA","CURITIBA","PORTO ALEGRE","MANAUS","BELEM","GOIANIA","CAMPINAS","VITORIA","FLORIANOPOLIS","NATAL","JOAO PESSOA","MACEIO","ARACAJU","SAO LUIS","CUIABA","CAMPO GRANDE","CAMPOS DOS GOYTACAZES","NITEROI","BRASILIA"};
            return names[Math.max(1,Math.min(25,waves.wave))];
        }

        String phaseObjective(){
            String[] objectives={"","A CIDADE DEVE SOBREVIVER","O CRISTO REDENTOR DEVE SOBREVIVER","A PONTE ESTAIADA DEVE SOBREVIVER","O MINEIRAO DEVE SOBREVIVER","O ELEVADOR LACERDA DEVE SOBREVIVER","O MARCO ZERO DEVE SOBREVIVER","O FAROL DO MUCURIPE DEVE SOBREVIVER","O JARDIM BOTANICO DEVE SOBREVIVER","A USINA DO GASOMETRO DEVE SOBREVIVER","O TEATRO AMAZONAS DEVE SOBREVIVER","O VER-O-PESO DEVE SOBREVIVER","O MONUMENTO AS TRES RACAS DEVE SOBREVIVER","A TORRE DO CASTELO DEVE SOBREVIVER","O CONVENTO DA PENHA DEVE SOBREVIVER","A PONTE HERCILIO LUZ DEVE SOBREVIVER","O FORTE DOS REIS MAGOS DEVE SOBREVIVER","O FAROL DO CABO BRANCO DEVE SOBREVIVER","O FAROL DA PONTA VERDE DEVE SOBREVIVER","A PONTE DO IMPERADOR DEVE SOBREVIVER","O PALACIO DOS LEOES DEVE SOBREVIVER","A IGREJA DO ROSARIO DEVE SOBREVIVER","O OBELISCO DEVE SOBREVIVER","A BASILICA DO SANTISSIMO SALVADOR DEVE SOBREVIVER","O MAC DEVE SOBREVIVER","O CONGRESSO NACIONAL DEVE SOBREVIVER"};
            return objectives[Math.max(1,Math.min(25,waves.wave))];
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

        void preparePhase(){
            lastDivisorAcquired=scheduledDivisorForPhase();
            if(lastDivisorAcquired>0)inv.unlockDivisor(lastDivisorAcquired);
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

        void buySubtractor(){
            if(inv.money<SHOP_SUBTRACTOR_COST)return;
            inv.money-=SHOP_SUBTRACTOR_COST;
            inv.addSubtractor(10);
            audio.play("bonus_subtrator.wav");
        }

        void buyBomb(){
            if(inv.money<SHOP_BOMB_COST)return;
            inv.money-=SHOP_BOMB_COST;
            inv.bombZero++;
            audio.play("bonus_municao.wav");
        }

        void startBombSequence(){
            if(inv.bombZero<=0||bombSequence)return;
            boolean hasTarget=false;
            for(Meteor m:meteors)if(!m.dead&&m.kind!=Kind.BONUS){hasTarget=true;break;}
            if(!hasTarget){audio.play("divisao_errada.wav");return;}
            inv.bombZero--;
            bombSequence=true;bombSequenceTimer=1.05f;
            audio.play("bomba_zero.wav");
            for(Meteor m:meteors)if(!m.dead&&m.kind!=Kind.BONUS){m.targetBlink=true;m.blinkTime=0;}
        }

        void updateBombSequence(float dt){
            bombSequenceTimer-=dt;
            for(Meteor m:meteors)if(!m.dead&&m.kind!=Kind.BONUS)m.blinkTime+=dt;
            if(bombSequenceTimer>0)return;
            bombSequence=false;
            ArrayList<Meteor> targets=new ArrayList<Meteor>();
            for(Meteor m:meteors)if(!m.dead&&m.kind!=Kind.BONUS)targets.add(m);
            for(Meteor m:targets){m.targetBlink=false;explode(m,true,false);}
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

        int currentAimIndex(){return selectedMode==1?7:projectileIndexForDivisor(inv.selectedDivisor);}

        int currentChargedDivisor(){
            int base=Math.max(2,inv.selectedDivisor);
            if(selectedMode==1)return Math.max(1,inv.subtractorValue);
            int max=base*base;
            return Math.max(base,Math.min(max,Math.round(base+(max-base)*aimCharge)));
        }

        void pointCannonAt(float tx,float ty){
            aimX=Math.max(0,Math.min(800,tx));aimY=Math.max(0,Math.min(389,ty));
            cannonAngle=(float)Math.toDegrees(Math.atan2(aimY-deployedCannonY(),aimX-cannonX));
        }

        void updateAimCharge(float dt){
            if(!aiming)return;
            long held=SystemClock.uptimeMillis()-aimDownTime;
            if(selectedMode==0&&held>360){
                aimCharge=Math.max(0f,Math.min(1f,(held-360)/1450f));
                chargedProjectileValue=currentChargedDivisor();
                chargeParticleTimer-=dt;
                if(chargeParticleTimer<=0){
                    chargeParticleTimer=.035f;
                    float cy=deployedCannonY();
                    double rad=Math.toRadians(cannonAngle);
                    float mx=cannonX+(float)Math.cos(rad)*25f;
                    float my=cy+(float)Math.sin(rad)*25f;
                    int count=2+(aimCharge>.65f?2:0);
                    for(int i=0;i<count;i++){
                        double a=rnd.nextDouble()*Math.PI*2;
                        float sp=18+rnd.nextFloat()*42;
                        int col=rnd.nextBoolean()?Color.CYAN:Color.WHITE;
                        particles.add(new Particle(mx,my,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,.22f+rnd.nextFloat()*.22f,col,1.2f+rnd.nextFloat()*2.2f));
                    }
                }
            }else{
                aimCharge=0f;
                chargedProjectileValue=selectedMode==1?Math.max(1,inv.subtractorValue):inv.selectedDivisor;
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

        void update(float dt){
            if(saveNoticeTimer>0){saveNoticeTimer=Math.max(0,saveNoticeTimer-dt);if(saveNoticeTimer==0)saveNotice=false;}
            if(victory){updateVictory(dt);updateParticles(dt);return;}
            if(intermission){updateParticles(dt);return;}
            if(bombSequence){updateBombSequence(dt);updateParticles(dt);return;}
            if(paused)return;
            updateCityShake(dt);
            if(cannonAnim>0)cannonAnim=Math.max(0,cannonAnim-dt);
            else if(shotTimer<=0&&!aiming)cannonAngle=-45f;
            if(shotTimer>0)shotTimer=Math.max(0,shotTimer-dt);
            if(!aiming&&aimTargetTimer>0)aimTargetTimer=Math.max(0,aimTargetTimer-dt);
            updateAimCharge(dt);
            updatePendingBonus(dt);
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
            if(gameOver||quizOpen){updateParticles(dt);updateMilitary(dt);return;}
            if(!waves.complete()){
                spawnTimer-=dt;
                if(spawnTimer<=0){spawnMeteor();spawnTimer=waves.spawnSeconds()*(.82f+rnd.nextFloat()*.36f);}
                if(commercial==null&&rnd.nextFloat()<waves.commercialPlaneChancePerSecond()*dt*60f)spawnCommercial();
            }
            updatePlane(commercial,dt);updateMilitary(dt);updateMeteors(dt);updateParticles(dt);
            if(waves.complete()&&meteors.isEmpty()&&military==null){
                audio.play("fase_concluida.wav");
                if(cityHealth<100f)audio.play("reconstrucao_cidade.wav");
                cityHealth=100f;
                running=false;
                completedWave=waves.wave;
                waveClear=true;
                waveClearTimer=2.2f;
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

        void saveGame(){
            StringBuilder divs=new StringBuilder();
            for(Integer d:inv.divisors){if(divs.length()>0)divs.append(',');divs.append(d);}
            savePrefs().edit()
                    .putBoolean("exists",true)
                    .putInt("wave",waves.wave)
                    .putInt("destroyedThisWave",waves.destroyedThisWave)
                    .putInt("targetThisWave",waves.targetThisWave)
                    .putInt("difficulty",selectedDifficulty)
                    .putFloat("cityHealth",cityHealth)
                    .putInt("score",score)
                    .putInt("destroyedTotal",destroyedTotal)
                    .putString("divisors",divs.toString())
                    .putInt("selectedDivisor",inv.selectedDivisor)
                    .putInt("subtractorCharge",inv.subtractorCharge)
                    .putInt("subtractorValue",inv.subtractorValue)
                    .putInt("money",inv.money)
                    .putInt("bombZero",inv.bombZero)
                    .putFloat("shieldSeconds",inv.shieldSeconds)
                    .apply();
            saveNotice=true;saveNoticeTimer=1.6f;
        }

        boolean loadSavedGame(){
            SharedPreferences sp=savePrefs();
            if(!sp.getBoolean("exists",false))return false;
            meteors.clear();particles.clear();commercial=null;military=null;quizMeteor=null;
            running=false;preWave=true;gameOver=false;quizOpen=false;paused=false;victory=false;waveClear=false;
            cityShaking=false;cityShakeOffset=0;cannonAngle=-45;cannonAnim=0;shotTimer=0;cannonDeploy=0;
            scoreSaved=false;fireParticleTimer=0;shieldVisualAge=0;shieldWasActive=false;selectedMode=0;
            intermission=false;manualFromPause=false;bombSequence=false;bombSequenceTimer=0;pendingBonusTarget=null;pendingBonusTimer=0;lastDivisorAcquired=0;
            aiming=false;aimTargetTimer=0;aimCharge=0;chargeParticleTimer=0;aimDownTime=0;manualPage=0;dirtParticleTimer=0;
            waves.wave=Math.max(1,Math.min(WaveManager.MAX_WAVE,sp.getInt("wave",1)));
            waves.destroyedThisWave=Math.max(0,sp.getInt("destroyedThisWave",0));
            waves.targetThisWave=Math.max(1,sp.getInt("targetThisWave",Math.min(12,4+waves.wave)));
            selectedDifficulty=Math.max(0,Math.min(5,sp.getInt("difficulty",0)));waves.difficulty=selectedDifficulty;
            cityHealth=Math.max(1f,Math.min(100f,sp.getFloat("cityHealth",100f)));
            score=Math.max(0,sp.getInt("score",0));destroyedTotal=Math.max(0,sp.getInt("destroyedTotal",0));
            inv.divisors.clear();
            String raw=sp.getString("divisors","2,3");
            if(raw!=null)for(String part:raw.split(",")){try{int d=Integer.parseInt(part);if(d>=2&&d<=17&&MeteorMathV2.isPrime(d))inv.divisors.add(d);}catch(Exception ignored){}}
            if(inv.divisors.isEmpty()){inv.divisors.add(2);inv.divisors.add(3);}
            inv.selectedDivisor=sp.getInt("selectedDivisor",2);
            if(!inv.divisors.contains(inv.selectedDivisor))inv.selectedDivisor=2;
            inv.subtractorCharge=Math.max(0,sp.getInt("subtractorCharge",0));
            inv.subtractorValue=Math.max(1,Math.min(Math.max(1,inv.subtractorCharge),sp.getInt("subtractorValue",1)));
            inv.money=Math.max(0,sp.getInt("money",0));inv.bombZero=Math.max(0,sp.getInt("bombZero",0));
            inv.shieldSeconds=Math.max(0,sp.getFloat("shieldSeconds",0));
            preWaveTimer=PRE_WAVE_DURATION;spawnTimer=0;fireworkTimer=0;saveNotice=false;saveNoticeTimer=0;
            playWaveMusic();audio.playLong("sirene_80bpm_10.wav");
            return true;
        }

        void returnToMainMenu(){
            running=false;preWave=false;gameOver=false;quizOpen=false;paused=false;victory=false;waveClear=false;intermission=false;manualFromPause=false;bombSequence=false;
            pendingBonusTarget=null;meteors.clear();particles.clear();commercial=null;military=null;quizMeteor=null;pauseRect.setEmpty();
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
            Meteor m=new Meteor();m.x=45+rnd.nextInt(710);m.y=-30;m.speed=waves.meteorSpeed()*(.85f+rnd.nextFloat()*.35f);m.radius=20;
            float roll=rnd.nextFloat();
            if(roll<waves.bonusChance())setupBonus(m);
            else if(roll<waves.bonusChance()+waves.specialChance()){
                boolean mult=rnd.nextBoolean();m.kind=mult?Kind.MULT:Kind.ADD;m.quiz=MeteorMathV2.generateQuiz(rnd,mult,waves.wave);m.value=m.quiz.answer;m.originalValue=m.value;m.radius=22;audio.play(mult?"meteoro_multiplicacao.wav":"meteoro_adicao.wav");
            }else{m.kind=Kind.NORMAL;boolean largePrime=waves.allowLargePrime()&&inv.subtractorCharge>0;m.value=MeteorMathV2.generateNormalValue(rnd,waves.maxMeteorValue(),inv.highestDivisor(),largePrime);m.originalValue=m.value;}
            meteors.add(m);if(rnd.nextFloat()<.24f)audio.play("meteoro_entrada.wav");
        }

        void setupBonus(Meteor m){
            m.kind=Kind.BONUS;m.radius=18;int r=rnd.nextInt(100);int candidate=nextLockedPrime();
            if(candidate>0&&r<34){m.bonus=Bonus.AMMO;m.ammoValue=candidate;m.value=candidate;}
            else if(r<53){m.bonus=Bonus.SUBTRACTOR;m.value=10+Math.min(20,waves.wave*2);}
            else if(r<70){m.bonus=Bonus.MONEY;m.value=25;}
            else if(r<84){m.bonus=Bonus.HEALTH;m.value=15;}
            else if(r<94){m.bonus=Bonus.SHIELD;m.value=10;}
            else{m.bonus=Bonus.BOMB0;m.value=0;}
        }

        int nextLockedPrime(){int ceiling=waves.primeUnlockCeiling();for(int x:MeteorMathV2.PRIMES)if(x<=ceiling&&!inv.divisors.contains(x))return x;return -1;}
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
            if(military.strikeTimer<=0&&military.target!=null&&!military.target.dead){
                audio.play("aviao_militar_bomba.wav");explode(military.target,true,true);military.target=null;
            }
            if(military.x>890)military=null;
        }

        void updateMeteors(float dt){
            Iterator<Meteor> it=meteors.iterator();while(it.hasNext()){
                Meteor m=it.next();if(m.dead){it.remove();continue;}if(m==pendingBonusTarget)continue;emitMeteorTrail(m,dt);m.y+=m.speed*dt;
                if(commercial!=null&&commercial.active&&RectF.intersects(m.bounds(),commercial.bounds())){commercial.active=false;commercial=null;audio.play("aviao_comercial_atingido.wav");damageCity(6,m.x,false);burst(m.x,m.y,Color.LTGRAY,18);m.dead=true;continue;}
                if(m.y>318){if(m.kind==Kind.BONUS)damageCity(3,m.x,false);else damageCity(Math.min(18,4+m.value/18f),m.x,false);m.dead=true;}
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

        void applySubtractorShot(Meteor m){
            int amount=inv.subtractorValue;
            if(amount<1||amount>inv.subtractorCharge||!inv.spendSubtractor(amount)){
                audio.play("divisao_errada.wav");return;
            }
            audio.play("subtrator_uso.wav");
            burst(m.x,m.y,Color.CYAN,10);
            if(amount>=m.value){m.value=0;explode(m,true,false);}
            else m.value-=amount;
        }

        void resolveShotAt(float x,float y,int projectileValue){
            Meteor hit=null;
            for(int i=meteors.size()-1;i>=0;i--){
                Meteor m=meteors.get(i);
                if(!m.dead&&m.bounds().contains(x,y)){hit=m;break;}
            }
            if(hit==null)return;
            if(hit.kind==Kind.BONUS){queueBonusCollection(hit);return;}
            if(hit.kind==Kind.ADD||hit.kind==Kind.MULT){openQuiz(hit);return;}
            if(selectedMode==1){applySubtractorShot(hit);return;}
            applyDivisorShot(hit,projectileValue);
        }

        void releaseAimedShot(){
            updateAimCharge(0f);
            int value=selectedMode==1?Math.max(1,inv.subtractorValue):currentChargedDivisor();
            int projectileIndex=aimTargetIndex;
            chargedProjectileValue=value;
            fireProjectileAt(aimX,aimY,projectileIndex);
            resolveShotAt(aimX,aimY,value);
            aiming=false;
            aimTargetTimer=.34f;
            aimCharge=0f;
            chargeParticleTimer=0f;
        }

        void collectBonus(Meteor m){
            switch(m.bonus){case AMMO:if(inv.unlockDivisor(m.ammoValue))audio.play("municao_desbloqueada.wav");else audio.play("bonus_municao.wav");break;case SUBTRACTOR:inv.addSubtractor(m.value);audio.play("bonus_subtrator.wav");break;case MONEY:inv.money+=m.value;audio.play("bonus_dinheiro.wav");break;case HEALTH:cityHealth=Math.min(100,cityHealth+m.value);audio.play("bonus_saude.wav");break;case SHIELD:inv.shieldSeconds=Math.max(inv.shieldSeconds,10);shieldVisualAge=0;shieldWasActive=false;audio.play("bonus_escudo.wav");break;case BOMB0:inv.bombZero++;audio.play("bonus_municao.wav");break;}
            m.dead=true;burst(m.x,m.y,Color.YELLOW,12);
        }

        void explode(Meteor m,boolean count,boolean guaranteedBonus){if(m.dead)return;m.dead=true;audio.play("explosao_meteoro.wav");burst(m.x,m.y,Color.rgb(255,150,35),24);if(count){waves.countDestroyed();destroyedTotal++;score+=10+Math.min(40,m.originalValue/3);if(guaranteedBonus)spawnBonusAt(m.x,m.y);}}
        void spawnBonusAt(float x,float y){Meteor b=new Meteor();b.x=x;b.y=y;b.speed=30;b.radius=18;setupBonus(b);meteors.add(b);}
        void openQuiz(Meteor m){quizOpen=true;quizMeteor=m;quizAttempts=0;audio.play("quiz_abre.wav");}
        void answerQuiz(int option){if(!quizOpen||quizMeteor==null)return;if(option==quizMeteor.quiz.answer){audio.play("quiz_acerto.wav");audio.play("alvo_trava.wav");quizOpen=false;startMilitaryStrike(quizMeteor);}else{quizAttempts++;audio.play("quiz_erro.wav");if(quizAttempts>=2){quizMeteor.kind=Kind.NORMAL;quizMeteor.value=quizMeteor.quiz.answer;quizMeteor.originalValue=quizMeteor.value;quizMeteor.quiz=null;quizOpen=false;quizMeteor=null;}}}
        void startMilitaryStrike(Meteor target){
            military=new Plane();military.military=true;military.x=-75;
            military.baseY=Math.max(52,target.y-72);military.y=military.baseY;
            military.speed=190;military.target=target;military.strikeTimer=1.12f;military.bobPhase=rnd.nextFloat()*6.28f;
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
            drawDarkCard(c,180,35,620,440);drawText(c,"OPCOES",400,70,30,Color.CYAN,true);
            drawMenuButton(c,new RectF(245,88,555,128),"DIFICULDADE: "+difficultyName(),false);
            drawMenuButton(c,new RectF(245,140,555,180),"MIRA LASER: "+(laserEnabled?"LIGADA":"DESLIGADA"),false);
            drawMenuButton(c,new RectF(245,192,555,232),"VIBRACAO: "+(vibrationEnabled?"LIGADA":"DESLIGADA"),false);
            drawMenuButton(c,new RectF(245,244,555,284),"MANUAL",false);
            drawMenuButton(c,new RectF(245,296,555,336),"APAGAR SCORE",false);
            if(scoreClearedNotice)drawText(c,"SCORE APAGADO",400,356,13,Color.YELLOW,true);
            drawMenuButton(c,new RectF(310,378,490,418),"VOLTAR",false);
        }

        void drawGame(Canvas c){
            p.setColor(Color.rgb(7,20,42));c.drawRect(0,0,800,480,p);drawStars(c);c.save();c.translate(0,cityShakeOffset);drawCity(c);drawShieldDome(c);
            for(Particle q:particles){p.setColor(q.color);p.setAlpha((int)(255*q.life/q.maxLife));c.drawRect(q.x-q.size,q.y-q.size,q.x+q.size,q.y+q.size,p);p.setAlpha(255);}for(Meteor m:meteors)if(!m.dead)drawMeteor(c,m);if(commercial!=null&&commercial.active)drawPlane(c,commercial);if(military!=null)drawPlane(c,military);drawCannon(c);drawProjectile(c);c.restore();drawAimTarget(c);drawHud(c);
            if(preWave){
                p.setColor(Color.argb(145+(int)(55*Math.abs(Math.sin(preWaveTimer*4))),120,0,0));c.drawRect(0,0,800,390,p);
                drawText(c,"FASE "+waves.wave,400,112,22,Color.YELLOW,true);
                drawText(c,"PROTEJA "+phaseCityName()+"!",400,154,31,Color.WHITE,true);
                drawText(c,phaseObjective(),400,194,15,Color.CYAN,true);
                if(lastDivisorAcquired>0)drawText(c,"DIVISOR "+lastDivisorAcquired+" ADQUIRIDO",400,230,17,Color.YELLOW,true);
                drawText(c,"INICIO EM "+Math.max(1,(int)Math.ceil(preWaveTimer)),400,274,18,Color.WHITE,true);
            }
            if(waveClear){p.setColor(Color.argb(175,0,20,38));c.drawRect(0,0,800,390,p);drawText(c,"FASE "+completedWave+" CONCLUIDA",400,190,36,Color.CYAN,true);drawText(c,"CIDADE REPARADA - 100%",400,232,18,Color.WHITE,true);}
            if(intermission)drawIntermission(c);
            if(quizOpen)drawQuiz(c);
            if(paused)drawPauseMenu(c);
            if(victory)drawVictory(c);
            if(gameOver){p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,800,480,p);drawText(c,"FIM DE JOGO",400,205,38,Color.RED,true);drawText(c,"Pontos: "+score,400,245,24,Color.WHITE,true);drawMenuButton(c,new RectF(305,280,495,330),"REINICIAR",true);}
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
            drawText(c,"DOCUMENTO DE CAMPO  "+(manualPage+1)+"/2",400,84,11,Color.LTGRAY,true);

            if(manualPage==0){
                drawText(c,"OBJETIVO PRIMARIO",92,112,14,Color.YELLOW,false);
                drawText(c,"Proteja a cidade e o ponto estrategico indicado em cada fase.",92,134,12,Color.WHITE,false);
                drawText(c,"CONTROLE DE TIRO",92,166,14,Color.YELLOW,false);
                drawText(c,"Toque: marca o alvo e dispara o projetil selecionado.",92,188,12,Color.WHITE,false);
                drawText(c,"Segure: a arma energiza; o divisor cresce ate seu quadrado.",92,210,12,Color.WHITE,false);
                drawText(c,"Arraste sem soltar: o canhao acompanha a mira.",92,232,12,Color.WHITE,false);
                drawText(c,"Solte: o disparo ocorre na posicao atual da mira.",92,254,12,Color.WHITE,false);
                drawText(c,"ARSENAL",92,286,14,Color.YELLOW,false);
                drawText(c,"7 municoes de divisor: 2, 3, 5, 7, 11, 13 e 17.",92,308,12,Color.WHITE,false);
                drawText(c,"8o alvo/projetil: SUBTRATOR. So consome carga se acertar meteoro.",92,330,12,Color.WHITE,false);
                drawText(c,"BOMBA 0 paralisa, marca e elimina todos os meteoros da tela.",92,352,12,Color.WHITE,false);
                drawMenuButton(c,new RectF(475,382,690,421),"REGRAS DE DIVISAO >",false);
            }else{
                drawText(c,"PROTOCOLO DE DIVISIBILIDADE",92,112,14,Color.YELLOW,false);
                drawText(c,"2  // ultimo algarismo par: 0, 2, 4, 6 ou 8.",92,138,12,Color.WHITE,false);
                drawText(c,"3  // soma dos algarismos divisivel por 3.",92,162,12,Color.WHITE,false);
                drawText(c,"5  // termina em 0 ou 5.",92,186,12,Color.WHITE,false);
                drawText(c,"7  // retire o ultimo algarismo e subtraia o dobro dele;",92,210,12,Color.WHITE,false);
                drawText(c,"     repita ate reconhecer um multiplo de 7.",92,230,12,Color.LTGRAY,false);
                drawText(c,"11 // diferenca entre as somas alternadas dos algarismos",92,254,12,Color.WHITE,false);
                drawText(c,"     deve ser 0 ou multiplo de 11.",92,274,12,Color.LTGRAY,false);
                drawText(c,"13 // retire o ultimo algarismo e some 4 vezes esse valor;",92,298,12,Color.WHITE,false);
                drawText(c,"     repita ate reconhecer um multiplo de 13.",92,318,12,Color.LTGRAY,false);
                drawText(c,"17 // retire o ultimo algarismo e subtraia 5 vezes esse valor;",92,342,12,Color.WHITE,false);
                drawText(c,"     repita ate reconhecer um multiplo de 17.",92,362,12,Color.LTGRAY,false);
                drawMenuButton(c,new RectF(110,382,325,421),"< BRIEFING",false);
            }
            drawMenuButton(c,new RectF(315,423,485,446),"VOLTAR",false);
        }

        void drawIntermission(Canvas c){
            p.setColor(Color.argb(205,0,0,0));c.drawRect(0,0,800,390,p);
            drawDarkCard(c,160,45,640,365);
            drawText(c,"INTERVALO ENTRE FASES",400,82,25,Color.CYAN,true);
            drawText(c,"DINHEIRO: $"+inv.money,400,112,16,Color.YELLOW,true);
            shopSubRect.set(215,140,585,190);
            shopBombRect.set(215,210,585,260);
            shopNextRect.set(285,300,515,346);
            drawMenuButton(c,shopSubRect,"SUBTRATOR +10   $"+SHOP_SUBTRACTOR_COST,false);
            drawMenuButton(c,shopBombRect,"BOMBA 0 +1   $"+SHOP_BOMB_COST,false);
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
        void drawCity(Canvas c){if(cityImg!=null)c.drawBitmap(cityImg,null,new RectF(0,315,800,390),pixel);else{p.setColor(Color.DKGRAY);c.drawRect(0,330,800,390,p);}int smokes=cityHealth>=75?0:1+(int)((75-cityHealth)/13f);for(int i=0;i<smokes;i++){float x=55+(i*137+lastImpactX/3)%690;drawSmoke(c,x,337+(i%2)*12);}}
        void drawSmoke(Canvas c,float x,float y){if(smokeImg!=null){p.setAlpha(cityHealth<=30?155:115);c.drawBitmap(smokeImg,null,new RectF(x-9,y-34,x+13,y-12),pixel);p.setAlpha(255);}}

        void drawShieldDome(Canvas c){
            if(inv.shieldSeconds<=0)return;
            boolean ending=inv.shieldSeconds<=2.5f;
            if(ending&&((int)(inv.shieldSeconds*7f)&1)==0)return;

            float reveal=Math.min(1f,shieldVisualAge/.70f);
            reveal=1f-(1f-reveal)*(1f-reveal);
            float left=-55f,right=855f,base=306f,apex=242f;
            float half=(right-left)*.5f*reveal;

            Path edge=new Path();
            edge.moveTo(left,base);
            edge.cubicTo(155f,318f,285f,286f,400f,apex);
            edge.cubicTo(515f,286f,645f,318f,right,base);

            Path fill=new Path();
            fill.moveTo(left,base);
            fill.cubicTo(155f,318f,285f,286f,400f,apex);
            fill.cubicTo(515f,286f,645f,318f,right,base);
            fill.lineTo(right,390f);
            fill.lineTo(left,390f);
            fill.close();

            c.save();
            c.clipRect(Math.max(0f,400f-half),225f,Math.min(800f,400f+half),391f);
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
        void emitMeteorTrail(Meteor m,float dt){if(m.kind==Kind.BONUS)return;m.tailTimer-=dt;if(m.tailTimer>0)return;m.tailTimer=.030f+.030f*rnd.nextFloat();int col=m.kind==Kind.MULT?Color.rgb(75,220,105):m.kind==Kind.ADD?Color.rgb(255,215,55):Color.rgb(255,145,30);for(int i=0;i<3;i++){float tx=m.x-m.radius+rnd.nextFloat()*(m.radius*2f);float ty=m.y-m.radius-rnd.nextFloat()*(m.radius*2f);particles.add(new Particle(tx,ty,-8+rnd.nextFloat()*16,-5-rnd.nextFloat()*18,.22f+rnd.nextFloat()*.24f,col,1.4f+rnd.nextFloat()*2.4f));}}
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
            if(projectileSheet!=null){
                drawTile(c,projectileSheet,8,1,shotProjectileIndex,new RectF(x-10,y-10,x+10,y+10),pixel);
            }else{
                p.setColor(Color.WHITE);c.drawCircle(x,y,4,p);
            }
        }

        void drawAimTarget(Canvas c){
            if(!aiming&&aimTargetTimer<=0)return;
            float pulse=1f+.08f*(float)Math.sin(SystemClock.uptimeMillis()/75.0);
            float r=25f*pulse;
            if(aimTargetSheet!=null)drawTile(c,aimTargetSheet,8,1,aimTargetIndex,new RectF(aimX-r,aimY-r,aimX+r,aimY+r),pixel);
            else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(aimX,aimY,r,p);p.setStyle(Paint.Style.FILL);}
            if(aiming){
                String v=selectedMode==1?"SUB "+Math.max(1,inv.subtractorValue):String.valueOf(currentChargedDivisor());
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
                int frame=((int)(SystemClock.uptimeMillis()/130)+Math.abs(m.originalValue))%4;
                RectF dest=new RectF(m.x-m.radius,m.y-m.radius,m.x+m.radius,m.y+m.radius);
                if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,frame,dest,use);
                else{p.setColor(Color.rgb(100,88,68));c.drawCircle(m.x,m.y,m.radius,p);}
                tintPaint.setColorFilter(null);
                String text;
                if(bombSequence)text="0x"+m.value;
                else text=(m.kind==Kind.ADD||m.kind==Kind.MULT)?m.quiz.expression():String.valueOf(m.value);
                drawText(c,text,m.x,m.y+5,15,Color.WHITE,true);
            }
            if(m.targetBlink&&((int)(m.blinkTime*10)%2==0)){
                if(targetImg!=null)c.drawBitmap(targetImg,null,new RectF(m.x-25,m.y-25,m.x+25,m.y+25),pixel);
                else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);c.drawCircle(m.x,m.y,m.radius+8,p);p.setStyle(Paint.Style.FILL);}
            }
        }

        void drawBonusMeteor(Canvas c,Meteor m){Bitmap icon=null;if(m.bonus==Bonus.MONEY)icon=moneyImg;else if(m.bonus==Bonus.SUBTRACTOR)icon=subImg;else if(m.bonus==Bonus.BOMB0)icon=bombImg;if(icon!=null)c.drawBitmap(icon,null,new RectF(m.x-17,m.y-17,m.x+17,m.y+17),pixel);else{int tile=m.bonus==Bonus.HEALTH?4:m.bonus==Bonus.SHIELD?5:1;if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,tile,new RectF(m.x-17,m.y-17,m.x+17,m.y+17),pixel);else{p.setColor(Color.rgb(60,135,190));c.drawCircle(m.x,m.y,17,p);}}drawText(c,bonusText(m),m.x,m.y+5,13,Color.WHITE,true);}
        String bonusText(Meteor m){switch(m.bonus){case AMMO:return "+"+m.ammoValue;case SUBTRACTOR:return "SUB "+m.value;case MONEY:return "$"+m.value;case HEALTH:return "+"+m.value;case SHIELD:return "ESC";default:return "x0";}}
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
        }

        void drawHud(Canvas c){
            // HUD redesenhado por blocos independentes para nenhum controle se sobrepor.
            p.setColor(Color.rgb(3,18,34));c.drawRect(0,390,800,480,p);
            p.setColor(Color.rgb(28,112,158));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(3,393,797,477,p);p.setStyle(Paint.Style.FILL);

            RectF divPanel=new RectF(8,397,414,472);
            RectF subPanel=new RectF(420,397,565,472);
            RectF bombPanel=new RectF(571,397,648,472);
            RectF repairPanel=new RectF(654,397,792,472);
            RectF[] panels={divPanel,subPanel,bombPanel,repairPanel};
            for(RectF panel:panels){p.setColor(Color.rgb(7,32,50));c.drawRect(panel,p);p.setColor(Color.rgb(25,92,127));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawRect(panel,p);p.setStyle(Paint.Style.FILL);}

            drawText(c,"DIVISORES",16,412,10,Color.LTGRAY,false);
            int x=14;
            for(int i=0;i<MeteorMathV2.PRIMES.length;i++){
                int prime=MeteorMathV2.PRIMES[i];
                RectF r=new RectF(x,421,x+30,459);divisorRects[i]=r;
                boolean have=inv.divisors.contains(prime);
                if(molduraSheet!=null){p.setAlpha(have?255:80);drawTile(c,molduraSheet,4,1,1,r,pixel);p.setAlpha(255);}
                else{p.setColor(have?Color.rgb(16,45,62):Color.rgb(28,31,35));c.drawRect(r,p);}
                if(have&&inv.selectedDivisor==prime&&selectedMode==0){p.setColor(Color.CYAN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(r,p);p.setStyle(Paint.Style.FILL);}
                drawOutlinedText(c,String.valueOf(prime),r.centerX(),446,14,have?Color.WHITE:Color.rgb(105,110,115));
                x+=36;
            }

            if(subImg!=null)c.drawBitmap(subImg,null,new RectF(427,402,447,422),pixel);
            drawText(c,"SUBTRATOR",493,410,10,Color.LTGRAY,true);
            drawText(c,"CARGA "+inv.subtractorCharge,522,423,10,Color.WHITE,true);
            subMinus.set(429,432,455,461);subUse.set(460,430,524,463);subPlus.set(530,432,556,461);
            p.setColor(selectedMode==1?Color.rgb(38,92,120):Color.rgb(14,51,69));c.drawRoundRect(subUse,4,4,p);
            drawText(c,"-",442,454,18,Color.WHITE,true);
            drawText(c,String.valueOf(inv.subtractorValue),492,454,18,Color.CYAN,true);
            drawText(c,"+",543,454,18,Color.WHITE,true);

            bombRect.set(576,404,643,466);
            p.setColor(Color.rgb(64,47,18));c.drawRoundRect(bombRect,4,4,p);
            drawText(c,"BOMBA 0",609,414,9,Color.LTGRAY,true);
            if(bombImg!=null)c.drawBitmap(bombImg,null,new RectF(593,419,625,451),pixel);
            drawText(c,"x"+inv.bombZero,609,464,10,Color.YELLOW,true);

            repairRect.set(660,404,786,466);
            p.setColor(Color.rgb(23,72,44));c.drawRoundRect(repairRect,4,4,p);
            if(moneyImg!=null)c.drawBitmap(moneyImg,null,new RectF(668,414,696,442),pixel);
            drawText(c,"$"+inv.money,738,425,12,Color.YELLOW,true);
            drawText(c,"REPARAR",738,448,11,Color.WHITE,true);

            drawText(c,"FASE "+waves.wave+"   "+waves.destroyedThisWave+"/"+waves.targetThisWave+"   MAX "+waves.maxMeteorValue(),12,22,14,Color.WHITE,false);
            drawText(c,"PONTOS "+score,12,43,14,Color.YELLOW,false);
            p.setColor(Color.rgb(60,20,20));c.drawRect(12,55,220,71,p);
            p.setColor(cityHealth>60?Color.GREEN:cityHealth>30?Color.YELLOW:Color.RED);c.drawRect(12,55,12+208*cityHealth/100f,71,p);
            drawText(c,"CIDADE "+(int)cityHealth+"%",116,68,11,Color.WHITE,true);
            if(inv.shieldSeconds>0)drawText(c,"ESCUDO "+(int)Math.ceil(inv.shieldSeconds),250,22,13,Color.CYAN,false);

            // Pause no canto direito da faixa marrom, fora da área útil do céu e fora do HUD azul.
            if(running&&!preWave&&!gameOver){
                pauseRect.set(746,351,792,386);
                p.setColor(Color.argb(220,72,58,28));c.drawRoundRect(pauseRect,5,5,p);
                p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);c.drawRoundRect(pauseRect,5,5,p);p.setStyle(Paint.Style.FILL);
                drawText(c,paused?">":"II",pauseRect.centerX(),376,18,Color.WHITE,true);
            }else pauseRect.setEmpty();
        }

        void drawOutlinedText(Canvas c,String s,float x,float y,float size,int color){drawText(c,s,x+1,y+1,size,Color.BLACK,true);drawText(c,s,x,y,size,color,true);}
        void drawQuiz(Canvas c){p.setColor(Color.argb(215,0,0,0));c.drawRect(0,0,800,390,p);drawDarkCard(c,170,95,630,350);String q=quizMeteor.quiz.expression()+" = ?";drawText(c,q,400,158,32,quizMeteor.kind==Kind.MULT?Color.rgb(100,255,130):Color.YELLOW,true);drawText(c,"Escolha o resultado  (tentativa "+(quizAttempts+1)+"/2)",400,195,15,Color.WHITE,true);for(int i=0;i<3;i++){float left=225+i*125;RectF r=new RectF(left,235,left+100,300);drawMenuButton(c,r,String.valueOf(quizMeteor.quiz.options[i]),false);}}
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
                    chargedProjectileValue=selectedMode==1?Math.max(1,inv.subtractorValue):inv.selectedDivisor;
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
                if(r!=null&&r.contains(x,y)&&inv.divisors.contains(MeteorMathV2.PRIMES[i])){
                    selectedMode=0;inv.selectedDivisor=MeteorMathV2.PRIMES[i];return true;
                }
            }
            if(subMinus.contains(x,y)){selectedMode=1;if(inv.subtractorValue>1)inv.subtractorValue--;return true;}
            if(subPlus.contains(x,y)){selectedMode=1;if(inv.subtractorValue<Math.max(1,inv.subtractorCharge)&&inv.subtractorValue<waves.maxMeteorValue())inv.subtractorValue++;return true;}
            if(subUse.contains(x,y)){selectedMode=1;return true;}
            if(bombRect.contains(x,y)){startBombSequence();return true;}
            if(repairRect.contains(x,y)&&inv.repair(cityHealth)){cityHealth=Math.min(100,cityHealth+20);audio.play("reconstrucao_cidade.wav");return true;}
            return true;
        }

        void handleIntermissionTouch(float x,float y){
            if(shopSubRect.contains(x,y)){buySubtractor();return;}
            if(shopBombRect.contains(x,y)){buyBomb();return;}
            if(shopNextRect.contains(x,y)){continueFromShop();return;}
        }

        void handlePauseTouch(float x,float y){
            if(manualFromPause){
                if(manualPage==0&&x>=455&&x<=710&&y>=365&&y<=430){manualPage=1;return;}
                if(manualPage==1&&x>=90&&x<=345&&y>=365&&y<=430){manualPage=0;return;}
                if(x>=290&&x<=510&&y>=415&&y<=455){manualFromPause=false;manualPage=0;}
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
            else if(menuPage==MENU_SCORE){if(x>=290&&x<=510&&y>=335&&y<=410)menuPage=MENU_MAIN;}
            else if(menuPage==MENU_OPTIONS){
                if(x>=235&&x<=565&&y>=80&&y<=134){selectedDifficulty=(selectedDifficulty+1)%6;waves.difficulty=selectedDifficulty;}
                else if(x>=235&&x<=565&&y>=134&&y<=186)laserEnabled=!laserEnabled;
                else if(x>=235&&x<=565&&y>=186&&y<=238)vibrationEnabled=!vibrationEnabled;
                else if(x>=235&&x<=565&&y>=238&&y<=290){menuPage=MENU_MANUAL;manualPage=0;}
                else if(x>=235&&x<=565&&y>=290&&y<=342){getContext().getSharedPreferences("pontuacao",Context.MODE_PRIVATE).edit().clear().apply();scoreClearedNotice=true;scoreNoticeTimer=1.5f;}
                else if(x>=290&&x<=510&&y>=365&&y<=430)menuPage=MENU_MAIN;
            }
            else if(menuPage==MENU_MANUAL){
                if(manualPage==0&&x>=455&&x<=710&&y>=365&&y<=430){manualPage=1;return;}
                if(manualPage==1&&x>=90&&x<=345&&y>=365&&y<=430){manualPage=0;return;}
                if(x>=290&&x<=510&&y>=415&&y<=455){menuPage=MENU_OPTIONS;manualPage=0;}
            }
        }
    }
}
