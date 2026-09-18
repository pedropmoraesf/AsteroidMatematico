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
        float x,y,speed; boolean military,active=true; Meteor target; float strikeTimer;
        RectF bounds(){ return new RectF(x-48,y-16,x+48,y+16); }
    }

    final class GameView extends View implements Runnable {
        static final int MENU_MAIN=0, MENU_SCORE=1, MENU_OPTIONS=2;
        static final float PRE_WAVE_DURATION=4.0f;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint pixel=new Paint();
        final Paint tintPaint=new Paint();
        final Random rnd=new Random();
        final PlayerInventory inv=new PlayerInventory();
        final WaveManager waves=new WaveManager();
        final List<Meteor> meteors=new ArrayList<Meteor>();
        final List<Particle> particles=new ArrayList<Particle>();
        final AudioBank audio;
        final RectF[] divisorRects=new RectF[MeteorMathV2.PRIMES.length];
        final RectF subMinus=new RectF(),subPlus=new RectF(),subUse=new RectF(),bombRect=new RectF(),repairRect=new RectF(),pauseRect=new RectF();
        final RectF[] menuButtons={new RectF(),new RectF(),new RectF(),new RectF()};

        Bitmap menuBg,lane,moneyImg,subImg,planeCommercial,planeMilitary,bombImg,targetImg;
        Bitmap cityImg,turretSheet,cannonSheet,meteorSheet,projectileSheet,smokeImg,molduraSheet;
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
        float cannonAngle=-90f;
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
            targetImg=assetBitmap("graficos/alvo_quiz.png");
            cityImg=assetBitmap("graficos/cidade_grande.png"); turretSheet=assetBitmap("graficos/torre.png");
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
            commercial=null;military=null;selectedMode=0;cityShaking=false;cityShakeOffset=0;cannonAngle=-90;cannonAnim=0;shotTimer=0;
            destroyedTotal=0;scoreSaved=false;fireParticleTimer=0;shieldVisualAge=0;shieldWasActive=false;cannonDeploy=0;
            waveClear=false;waveClearTimer=0;completedWave=0;shotColor=Color.YELLOW;
            waves.wave=1;waves.destroyedThisWave=0;waves.targetThisWave=5;waves.difficulty=selectedDifficulty;
            inv.divisors.clear();inv.divisors.add(2);inv.divisors.add(3);inv.selectedDivisor=2;
            inv.subtractorCharge=0;inv.subtractorValue=1;inv.money=0;inv.bombZero=0;inv.shieldSeconds=0;
            preWaveTimer=PRE_WAVE_DURATION;spawnTimer=0;
            audio.playMusic("musica_jogo.ogg",.34f);
            audio.playLong("sirene_80bpm_10.wav");
        }

        void beginNextWave(){running=false;preWave=true;paused=false;waveClear=false;preWaveTimer=PRE_WAVE_DURATION;cannonDeploy=0;audio.setMusicPaused(false);audio.playLong("sirene_80bpm_10.wav");}

        void update(float dt){
            if(paused)return;
            updateCityShake(dt);
            if(cannonAnim>0)cannonAnim=Math.max(0,cannonAnim-dt);
            if(shotTimer>0)shotTimer=Math.max(0,shotTimer-dt);
            if(introWhiteFade>0)introWhiteFade=Math.max(0,introWhiteFade-dt);
            if(scoreNoticeTimer>0){scoreNoticeTimer=Math.max(0,scoreNoticeTimer-dt);if(scoreNoticeTimer==0)scoreClearedNotice=false;}
            if(waveClear){
                updateParticles(dt);
                waveClearTimer-=dt;
                if(waveClearTimer<=0){waveClear=false;waves.nextWave();beginNextWave();}
                return;
            }
            if(!running&&!preWave&&!gameOver)return;
            if(preWave){
                preWaveTimer-=dt;
                float raw=1f-Math.max(0,preWaveTimer)/PRE_WAVE_DURATION;
                cannonDeploy=1f-(float)Math.pow(1f-Math.max(0,Math.min(1,raw)),3);
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
        void spawnCommercial(){commercial=new Plane();commercial.x=-60;commercial.y=80+rnd.nextInt(145);commercial.speed=68+waves.wave*2;commercial.military=false;audio.play("aviao_comercial_passagem.wav");}
        void updatePlane(Plane pl,float dt){if(pl==null||!pl.active)return;pl.x+=pl.speed*dt;if(pl.x>860){pl.active=false;if(pl==commercial)commercial=null;}}
        void updateMilitary(float dt){if(military==null)return;military.x+=military.speed*dt;military.strikeTimer-=dt;if(military.target!=null){military.target.targetBlink=true;military.target.blinkTime+=dt;}if(military.strikeTimer<=0&&military.target!=null&&!military.target.dead){audio.play("aviao_militar_bomba.wav");explode(military.target,true,true);military.target=null;}if(military.x>880)military=null;}

        void updateMeteors(float dt){
            Iterator<Meteor> it=meteors.iterator();while(it.hasNext()){
                Meteor m=it.next();if(m.dead){it.remove();continue;}emitMeteorTrail(m,dt);m.y+=m.speed*dt;
                if(commercial!=null&&commercial.active&&RectF.intersects(m.bounds(),commercial.bounds())){commercial.active=false;commercial=null;audio.play("aviao_comercial_atingido.wav");damageCity(6,m.x,false);burst(m.x,m.y,Color.LTGRAY,18);m.dead=true;continue;}
                if(m.y>318){if(m.kind==Kind.BONUS)damageCity(3,m.x,false);else damageCity(Math.min(18,4+m.value/18f),m.x,false);m.dead=true;}
            }
        }

        void updateParticles(float dt){Iterator<Particle> it=particles.iterator();while(it.hasNext()){Particle q=it.next();q.life-=dt;if(q.life<=0){it.remove();continue;}q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=45*dt;}}

        void damageCity(float amount,float impactX,boolean nuclear){if(inv.shieldSeconds>0&&!nuclear){audio.play("bonus_escudo.wav");return;}cityHealth-=amount;lastImpactX=(int)impactX;audio.play("impacto_cidade.wav");startCityShake(amount,nuclear);if(cityHealth<=0){cityHealth=0;gameOver=true;running=false;audio.play("game_over.wav");saveScore();}}

        void aimAndFire(Meteor m,int color){
            cannonAngle=(float)Math.toDegrees(Math.atan2(m.y-cannonY,m.x-cannonX));
            double rad=Math.toRadians(cannonAngle);
            float muzzleOffset=12f;
            shotStartX=cannonX+(float)Math.cos(rad)*muzzleOffset;
            shotStartY=cannonY+(float)Math.sin(rad)*muzzleOffset;
            shotColor=color;
            cannonAnim=.40f;shotTimer=shotDuration;shotTargetX=m.x;shotTargetY=m.y;
            audio.play("disparo_canhao.wav");
        }

        int projectileColorForDivisor(int d){
            switch(d){
                case 2:return Color.rgb(80,220,255);
                case 3:return Color.rgb(255,230,70);
                case 5:return Color.rgb(90,240,115);
                case 7:return Color.rgb(255,105,210);
                case 11:return Color.rgb(255,145,45);
                case 13:return Color.rgb(105,165,255);
                case 17:return Color.rgb(185,255,75);
                case 19:return Color.rgb(185,105,255);
                case 23:return Color.rgb(255,80,80);
                case 29:return Color.rgb(235,235,235);
                case 31:return Color.rgb(70,255,210);
                default:return Color.YELLOW;
            }
        }

        void hitMeteor(Meteor m){
            if(m==null||m.dead)return;if(m.kind==Kind.BONUS){collectBonus(m);return;}if(m.kind==Kind.ADD||m.kind==Kind.MULT){openQuiz(m);return;}if(selectedMode==1){useSubtractor(m);return;}if(selectedMode==2){useBomb(m);return;}
            int d=inv.selectedDivisor;aimAndFire(m,projectileColorForDivisor(d));if(MeteorMathV2.canDivide(m.value,d)){m.value/=d;audio.play("divisao_correta.wav");burst(m.x,m.y,Color.rgb(100,255,120),10);m.radius=Math.max(11,m.radius*.88f);if(m.value<=1)explode(m,true,false);}else{audio.play("divisao_errada.wav");burst(m.x,m.y,Color.rgb(255,80,60),7);score=Math.max(0,score-2);}
        }

        void collectBonus(Meteor m){
            switch(m.bonus){case AMMO:if(inv.unlockDivisor(m.ammoValue))audio.play("municao_desbloqueada.wav");else audio.play("bonus_municao.wav");break;case SUBTRACTOR:inv.addSubtractor(m.value);audio.play("bonus_subtrator.wav");break;case MONEY:inv.money+=m.value;audio.play("bonus_dinheiro.wav");break;case HEALTH:cityHealth=Math.min(100,cityHealth+m.value);audio.play("bonus_saude.wav");break;case SHIELD:inv.shieldSeconds=Math.max(inv.shieldSeconds,10);shieldVisualAge=0;shieldWasActive=false;audio.play("bonus_escudo.wav");break;case BOMB0:inv.bombZero++;audio.play("bonus_municao.wav");break;}
            m.dead=true;burst(m.x,m.y,Color.YELLOW,12);
        }

        void useSubtractor(Meteor m){
            int amount=inv.subtractorValue;
            if(amount<1||amount>inv.subtractorCharge||!inv.spendSubtractor(amount)){audio.play("divisao_errada.wav");return;}
            aimAndFire(m,Color.rgb(100,235,255));
            audio.play("subtrator_uso.wav");
            burst(m.x,m.y,Color.CYAN,10);
            if(amount>=m.value){m.value=0;explode(m,true,false);}
            else m.value-=amount;
        }
        void useBomb(Meteor m){if(inv.bombZero<=0){audio.play("divisao_errada.wav");return;}inv.bombZero--;audio.play("bomba_zero.wav");damageCity(7,400,true);explode(m,true,false);}
        void explode(Meteor m,boolean count,boolean guaranteedBonus){if(m.dead)return;m.dead=true;audio.play("explosao_meteoro.wav");burst(m.x,m.y,Color.rgb(255,150,35),24);if(count){waves.countDestroyed();destroyedTotal++;score+=10+Math.min(40,m.originalValue/3);if(guaranteedBonus)spawnBonusAt(m.x,m.y);}}
        void spawnBonusAt(float x,float y){Meteor b=new Meteor();b.x=x;b.y=y;b.speed=30;b.radius=18;setupBonus(b);meteors.add(b);}
        void openQuiz(Meteor m){quizOpen=true;quizMeteor=m;quizAttempts=0;audio.play("quiz_abre.wav");}
        void answerQuiz(int option){if(!quizOpen||quizMeteor==null)return;if(option==quizMeteor.quiz.answer){audio.play("quiz_acerto.wav");audio.play("alvo_trava.wav");quizOpen=false;startMilitaryStrike(quizMeteor);}else{quizAttempts++;audio.play("quiz_erro.wav");if(quizAttempts>=2){quizMeteor.kind=Kind.NORMAL;quizMeteor.value=quizMeteor.quiz.answer;quizMeteor.originalValue=quizMeteor.value;quizMeteor.quiz=null;quizOpen=false;quizMeteor=null;}}}
        void startMilitaryStrike(Meteor target){military=new Plane();military.military=true;military.x=-70;military.y=Math.max(55,target.y-70);military.speed=180;military.target=target;military.strikeTimer=1.15f;audio.play("aviao_militar_passagem.wav");quizMeteor=null;}
        void burst(float x,float y,int color,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float s=25+rnd.nextFloat()*70;particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,.35f+rnd.nextFloat()*.45f,color,2+rnd.nextFloat()*3));}}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            c.save();
            c.scale(scaleX,scaleY);
            if(!running&&!preWave&&!gameOver&&!waveClear)drawMenu(c);else drawGame(c);
            if(introWhiteFade>0){
                float t=Math.max(0,Math.min(1,introWhiteFade/.30f));
                p.setColor(Color.argb((int)(255*t),255,255,255));
                c.drawRect(0,0,800,480,p);
            }
            c.restore();
        }
        void drawMenu(Canvas c){p.setColor(Color.rgb(3,10,25));c.drawRect(0,0,800,480,p);if(menuBg!=null)drawCenterCrop(c,menuBg,new RectF(0,0,800,480),.56f);p.setColor(Color.argb(45,0,8,20));c.drawRect(0,0,800,480,p);if(menuPage==MENU_MAIN)drawMainMenu(c);else if(menuPage==MENU_SCORE)drawScores(c);else drawOptions(c);}
        void drawMainMenu(Canvas c){drawText(c,"ASTEROIDE MATEMATICO",400,72,34,Color.WHITE,true);String[] labels={"INICIAR","VER PONTUACAO","OPCOES","SAIR"};float top=176;for(int i=0;i<labels.length;i++){RectF r=menuButtons[i];r.set(292,top+i*55,508,top+40+i*55);drawMenuButton(c,r,labels[i],i==0);}}

        void drawScores(Canvas c){
            drawDarkCard(c,115,55,685,410);drawText(c,"PONTUACAO",400,92,30,Color.CYAN,true);drawText(c,"DATA",195,126,14,Color.LTGRAY,true);drawText(c,"PONTOS",410,126,14,Color.LTGRAY,true);drawText(c,"METEOROS",570,126,14,Color.LTGRAY,true);java.util.ArrayList<String[]> rows=new java.util.ArrayList<String[]>();
            try{SharedPreferences sp=getContext().getSharedPreferences("pontuacao",Context.MODE_PRIVATE);for(Object o:sp.getAll().values()){String[] v=String.valueOf(o).split("\\|");if(v.length>=3)rows.add(v);}java.util.Collections.sort(rows,new java.util.Comparator<String[]>(){public int compare(String[] a,String[] b){try{return Integer.parseInt(b[1])-Integer.parseInt(a[1]);}catch(Exception e){return 0;}}});}catch(Exception ignored){}
            if(rows.isEmpty())drawText(c,"NENHUMA PONTUACAO SALVA",400,205,17,Color.WHITE,true);else{int max=Math.min(7,rows.size());for(int i=0;i<max;i++){String[] v=rows.get(i);float y=158+i*29;drawText(c,v[0],195,y,12,Color.WHITE,true);drawText(c,v[1],410,y,14,Color.YELLOW,true);drawText(c,v[2],570,y,14,Color.WHITE,true);}}drawMenuButton(c,new RectF(310,355,490,395),"VOLTAR",false);
        }

        String difficultyName(){String[] n={"MUITO FACIL","FACIL","MEDIO","DIFICIL","MUITO DIFICIL","INSANO"};return n[Math.max(0,Math.min(n.length-1,selectedDifficulty))];}
        void drawOptions(Canvas c){drawDarkCard(c,180,45,620,430);drawText(c,"OPCOES",400,82,30,Color.CYAN,true);drawMenuButton(c,new RectF(245,105,555,148),"DIFICULDADE: "+difficultyName(),false);drawMenuButton(c,new RectF(245,163,555,206),"MIRA LASER: "+(laserEnabled?"LIGADA":"DESLIGADA"),false);drawMenuButton(c,new RectF(245,221,555,264),"VIBRACAO: "+(vibrationEnabled?"LIGADA":"DESLIGADA"),false);drawMenuButton(c,new RectF(245,279,555,322),"APAGAR SCORE",false);if(scoreClearedNotice)drawText(c,"SCORE APAGADO",400,346,13,Color.YELLOW,true);drawMenuButton(c,new RectF(310,365,490,407),"VOLTAR",false);}

        void drawGame(Canvas c){
            p.setColor(Color.rgb(7,20,42));c.drawRect(0,0,800,480,p);drawStars(c);c.save();c.translate(0,cityShakeOffset);drawCity(c);drawShieldDome(c);
            for(Particle q:particles){p.setColor(q.color);p.setAlpha((int)(255*q.life/q.maxLife));c.drawRect(q.x-q.size,q.y-q.size,q.x+q.size,q.y+q.size,p);p.setAlpha(255);}for(Meteor m:meteors)if(!m.dead)drawMeteor(c,m);if(commercial!=null&&commercial.active)drawPlane(c,commercial);if(military!=null)drawPlane(c,military);drawCannon(c);drawProjectile(c);c.restore();drawHud(c);
            if(preWave){p.setColor(Color.argb(125+(int)(70*Math.abs(Math.sin(preWaveTimer*4))),180,0,0));c.drawRect(0,0,800,390,p);drawText(c,"ALERTA - ONDA "+waves.wave,400,185,34,Color.WHITE,true);drawText(c,"METEOROS SE APROXIMANDO",400,225,20,Color.YELLOW,true);drawText(c,"INICIO EM "+Math.max(1,(int)Math.ceil(preWaveTimer)),400,270,18,Color.WHITE,true);}
            if(waveClear){p.setColor(Color.argb(175,0,20,38));c.drawRect(0,0,800,390,p);drawText(c,"ONDA "+completedWave+" CONCLUIDA",400,190,36,Color.CYAN,true);drawText(c,"CIDADE REPARADA - 100%",400,232,18,Color.WHITE,true);}
            if(quizOpen)drawQuiz(c);if(paused){p.setColor(Color.argb(180,0,0,0));c.drawRect(0,0,800,390,p);drawText(c,"PAUSADO",400,205,38,Color.WHITE,true);drawText(c,"Toque no botao para continuar",400,240,16,Color.LTGRAY,true);}if(gameOver){p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,800,480,p);drawText(c,"FIM DE JOGO",400,205,38,Color.RED,true);drawText(c,"Pontos: "+score,400,245,24,Color.WHITE,true);drawMenuButton(c,new RectF(305,280,495,330),"REINICIAR",true);}
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

        void drawCannon(Canvas c){float cy=deployedCannonY();int towerFrame=(int)((SystemClock.uptimeMillis()/80)%8);c.save();c.clipRect(0,0,800,towerBaseY);float towerHeight=towerBaseY-cannonY;if(turretSheet!=null)drawTile(c,turretSheet,8,1,towerFrame,new RectF(cannonX-21,cy,cannonX+21,cy+towerHeight),pixel);int frame=8;if(cannonAnim>0){float elapsed=.40f-cannonAnim;frame=Math.max(0,Math.min(7,(int)(elapsed/.05f)));}if(cannonSheet!=null){c.save();c.rotate(cannonAngle,cannonX,cy);drawTile(c,cannonSheet,8,2,frame,new RectF(cannonX-33,cy-33,cannonX+33,cy+33),pixel);c.restore();}c.restore();}
        void drawProjectile(Canvas c){
            if(shotTimer<=0)return;
            float t=1f-shotTimer/shotDuration;t=Math.max(0,Math.min(1,t));
            if(laserEnabled){p.setColor(Color.argb(115,shotColor>>16&255,shotColor>>8&255,shotColor&255));p.setStrokeWidth(1.5f);c.drawLine(shotStartX,shotStartY,shotTargetX,shotTargetY,p);}
            float x=shotStartX+(shotTargetX-shotStartX)*t;
            float y=shotStartY+(shotTargetY-shotStartY)*t;
            p.setColor(shotColor);p.setAlpha(110);c.drawCircle(x,y,7,p);p.setAlpha(255);
            if(projectileSheet!=null){
                tintPaint.setColorFilter(new PorterDuffColorFilter(shotColor,PorterDuff.Mode.SRC_ATOP));
                drawTile(c,projectileSheet,8,1,Math.min(7,(int)(t*8)),new RectF(x-10,y-10,x+10,y+10),tintPaint);
                tintPaint.setColorFilter(null);
            }else{p.setColor(shotColor);c.drawCircle(x,y,4,p);}
        }

        void drawMeteor(Canvas c,Meteor m){
            if(m.kind==Kind.BONUS)drawBonusMeteor(c,m);else{Paint use=pixel;if(m.kind==Kind.MULT){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(80,205,105),PorterDuff.Mode.MULTIPLY));use=tintPaint;}else if(m.kind==Kind.ADD){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(255,220,80),PorterDuff.Mode.MULTIPLY));use=tintPaint;}int frame=((int)(SystemClock.uptimeMillis()/130)+Math.abs(m.originalValue))%4;RectF dest=new RectF(m.x-m.radius,m.y-m.radius,m.x+m.radius,m.y+m.radius);if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,frame,dest,use);else{p.setColor(Color.rgb(100,88,68));c.drawCircle(m.x,m.y,m.radius,p);}tintPaint.setColorFilter(null);String text=(m.kind==Kind.ADD||m.kind==Kind.MULT)?m.quiz.expression():String.valueOf(m.value);drawText(c,text,m.x,m.y+5,15,Color.WHITE,true);}if(m.targetBlink&&((int)(m.blinkTime*10)%2==0)){if(targetImg!=null)c.drawBitmap(targetImg,null,new RectF(m.x-25,m.y-25,m.x+25,m.y+25),pixel);else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);c.drawCircle(m.x,m.y,m.radius+8,p);p.setStyle(Paint.Style.FILL);}}
        }

        void drawBonusMeteor(Canvas c,Meteor m){Bitmap icon=null;if(m.bonus==Bonus.MONEY)icon=moneyImg;else if(m.bonus==Bonus.SUBTRACTOR)icon=subImg;else if(m.bonus==Bonus.BOMB0)icon=bombImg;if(icon!=null)c.drawBitmap(icon,null,new RectF(m.x-17,m.y-17,m.x+17,m.y+17),pixel);else{int tile=m.bonus==Bonus.HEALTH?4:m.bonus==Bonus.SHIELD?5:1;if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,tile,new RectF(m.x-17,m.y-17,m.x+17,m.y+17),pixel);else{p.setColor(Color.rgb(60,135,190));c.drawCircle(m.x,m.y,17,p);}}drawText(c,bonusText(m),m.x,m.y+5,13,Color.WHITE,true);}
        String bonusText(Meteor m){switch(m.bonus){case AMMO:return "+"+m.ammoValue;case SUBTRACTOR:return "SUB "+m.value;case MONEY:return "$"+m.value;case HEALTH:return "+"+m.value;case SHIELD:return "ESC";default:return "x0";}}
        void drawPlane(Canvas c,Plane pl){Bitmap b=pl.military?planeMilitary:planeCommercial;if(b!=null)c.drawBitmap(b,null,pl.bounds(),pixel);else{p.setColor(pl.military?Color.GREEN:Color.WHITE);c.drawRect(pl.bounds(),p);}}

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
            p.setColor(selectedMode==2?Color.rgb(112,70,24):Color.rgb(64,47,18));c.drawRoundRect(bombRect,4,4,p);
            drawText(c,"BOMBA 0",609,414,9,Color.LTGRAY,true);
            if(bombImg!=null)c.drawBitmap(bombImg,null,new RectF(593,419,625,451),pixel);
            drawText(c,"x"+inv.bombZero,609,464,10,Color.YELLOW,true);

            repairRect.set(660,404,786,466);
            p.setColor(Color.rgb(23,72,44));c.drawRoundRect(repairRect,4,4,p);
            if(moneyImg!=null)c.drawBitmap(moneyImg,null,new RectF(668,414,696,442),pixel);
            drawText(c,"$"+inv.money,738,425,12,Color.YELLOW,true);
            drawText(c,"REPARAR",738,448,11,Color.WHITE,true);

            drawText(c,"ONDA "+waves.wave+"   "+waves.destroyedThisWave+"/"+waves.targetThisWave+"   MAX "+waves.maxMeteorValue(),12,22,14,Color.WHITE,false);
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

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=lx(e.getX()),y=ly(e.getY());audio.play("ui_click.wav");if(!running&&!preWave&&!gameOver&&!waveClear){handleMenuTouch(x,y);return true;}if(gameOver){if(x>=285&&x<=515&&y>=265&&y<=350)resetGame();return true;}if(waveClear)return true;if(running&&!preWave&&pauseRect.contains(x,y)){paused=!paused;audio.setMusicPaused(paused);return true;}if(paused)return true;if(preWave)return true;if(quizOpen){if(y>=225&&y<=315){for(int i=0;i<3;i++){float l=225+i*125;if(x>=l&&x<=l+100){answerQuiz(quizMeteor.quiz.options[i]);return true;}}}return true;}
            for(int i=0;i<divisorRects.length;i++){RectF r=divisorRects[i];if(r!=null&&r.contains(x,y)&&inv.divisors.contains(MeteorMathV2.PRIMES[i])){selectedMode=0;inv.selectedDivisor=MeteorMathV2.PRIMES[i];return true;}}if(subMinus.contains(x,y)){selectedMode=1;if(inv.subtractorValue>1)inv.subtractorValue--;return true;}if(subPlus.contains(x,y)){selectedMode=1;if(inv.subtractorValue<Math.max(1,inv.subtractorCharge)&&inv.subtractorValue<waves.maxMeteorValue())inv.subtractorValue++;return true;}if(subUse.contains(x,y)){selectedMode=1;return true;}if(bombRect.contains(x,y)){selectedMode=2;return true;}if(repairRect.contains(x,y)&&inv.repair(cityHealth)){cityHealth=Math.min(100,cityHealth+20);audio.play("reconstrucao_cidade.wav");return true;}Meteor hit=null;for(int i=meteors.size()-1;i>=0;i--){Meteor m=meteors.get(i);if(!m.dead&&m.bounds().contains(x,y)){hit=m;break;}}if(hit!=null)hitMeteor(hit);return true;
        }

        void handleMenuTouch(float x,float y){
            if(menuPage==MENU_MAIN){for(int i=0;i<menuButtons.length;i++)if(menuButtons[i].contains(x,y)){if(i==0)resetGame();else if(i==1)menuPage=MENU_SCORE;else if(i==2)menuPage=MENU_OPTIONS;else GameV2Activity.this.finish();return;}}
            else if(menuPage==MENU_SCORE){if(x>=290&&x<=510&&y>=335&&y<=410)menuPage=MENU_MAIN;}
            else if(menuPage==MENU_OPTIONS){if(x>=235&&x<=565&&y>=95&&y<=155){selectedDifficulty=(selectedDifficulty+1)%6;waves.difficulty=selectedDifficulty;}else if(x>=235&&x<=565&&y>=155&&y<=215)laserEnabled=!laserEnabled;else if(x>=235&&x<=565&&y>=215&&y<=273)vibrationEnabled=!vibrationEnabled;else if(x>=235&&x<=565&&y>=273&&y<=333){getContext().getSharedPreferences("pontuacao",Context.MODE_PRIVATE).edit().clear().apply();scoreClearedNotice=true;scoreNoticeTimer=1.5f;}else if(x>=290&&x<=510&&y>=350&&y<=420)menuPage=MENU_MAIN;}
        }
    }
}
