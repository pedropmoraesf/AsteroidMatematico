package br.grassinimoraes.divasteroides;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
 * A logica nova (ondas, quizzes, subtrator, bonus e avioes) fica separada dos sprites
 * historicos para que o projeto possa continuar evoluindo sem perder o jogo de 2015.
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
        void release(){ stopLong(); pool.release(); }
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
        MeteorMathV2.Quiz quiz; boolean dead, targetBlink; float blinkTime;
        RectF bounds(){return new RectF(x-radius,y-radius,x+radius,y+radius);}
    }

    static final class Plane {
        float x,y,speed; boolean military,active=true; Meteor target; float strikeTimer;
        RectF bounds(){ return new RectF(x-48,y-16,x+48,y+16); }
    }

    final class GameView extends View implements Runnable {
        static final int MENU_MAIN=0, MENU_HOW=1, MENU_OPTIONS=2, MENU_CREDITS=3;
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
        final RectF subMinus=new RectF(),subPlus=new RectF(),subUse=new RectF(),bombRect=new RectF(),repairRect=new RectF();
        final RectF[] menuButtons={new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};

        Bitmap menuBg,lane,moneyImg,subImg,planeCommercial,planeMilitary,bombImg,tailImg,targetImg;
        Bitmap cityImg,turretSheet,cannonSheet,meteorSheet,projectileSheet,fireImg,smokeImg,molduraSheet;
        Typeface gameFont;

        long last=SystemClock.uptimeMillis();
        float spawnTimer;
        float cityHealth=100;
        int score=0;
        boolean running=false, preWave=false, quizOpen=false, gameOver=false;
        float preWaveTimer=0;
        Meteor quizMeteor;
        int quizAttempts;
        Plane commercial,military;
        int selectedMode=0;
        int menuPage=MENU_MAIN;
        boolean vibrationEnabled=true;
        float logicalW=800,logicalH=480,scaleX=1,scaleY=1;

        final float cannonX=400f, cannonY=326f;
        float cannonAngle=-90f;
        float cannonAnim=0f;
        float shotTimer=0f, shotDuration=.17f, shotTargetX=400f, shotTargetY=200f;

        boolean cityShaking=false;
        float cityShakeT=0f, cityShakeT2=0f, cityShakeAlpha=0f, cityShakeGamma=0f, cityShakeOffset=0f;
        int lastImpactX=400;

        GameView(Context c){
            super(c); setFocusable(true); pixel.setAntiAlias(false); pixel.setFilterBitmap(false); tintPaint.setAntiAlias(false);
            audio=new AudioBank(c); loadAssets(); loadAudio();
        }

        void loadAssets(){
            menuBg=assetBitmap("graficos/menu_cena.png");
            lane=assetBitmap("graficos/lane_armas.png"); moneyImg=assetBitmap("graficos/dinheiro_bonus.png");
            subImg=assetBitmap("graficos/subtrator.png"); planeCommercial=assetBitmap("graficos/aviao_comercial.png");
            planeMilitary=assetBitmap("graficos/aviao_militar.png"); bombImg=assetBitmap("graficos/bomba0.png");
            tailImg=assetBitmap("graficos/rastro_meteoro.png"); targetImg=assetBitmap("graficos/alvo_quiz.png");
            cityImg=assetBitmap("graficos/cidade_grande.png"); turretSheet=assetBitmap("graficos/torre.png");
            cannonSheet=assetBitmap("graficos/canhao1.png"); meteorSheet=assetBitmap("graficos/meteoro e itens.png");
            projectileSheet=assetBitmap("graficos/projetil.png"); fireImg=assetBitmap("graficos/fogo.png");
            smokeImg=assetBitmap("graficos/fumaca1.png"); molduraSheet=assetBitmap("graficos/moldura.png");
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
            meteors.clear();particles.clear();cityHealth=100;score=0;running=false;preWave=true;gameOver=false;quizOpen=false;
            commercial=null;military=null;selectedMode=0;cityShaking=false;cityShakeOffset=0;cannonAngle=-90;cannonAnim=0;shotTimer=0;
            waves.wave=1;waves.destroyedThisWave=0;waves.targetThisWave=8;waves.difficulty=0;
            inv.divisors.clear();inv.divisors.add(2);inv.divisors.add(3);inv.selectedDivisor=2;
            inv.subtractorCharge=0;inv.subtractorValue=1;inv.money=0;inv.bombZero=0;inv.shieldSeconds=0;
            preWaveTimer=7.5f;spawnTimer=0;audio.playLong("sirene_80bpm_10.wav");
        }

        void beginNextWave(){running=false;preWave=true;preWaveTimer=7.5f;audio.playLong("sirene_80bpm_10.wav");}

        void update(float dt){
            updateCityShake(dt);
            if(cannonAnim>0)cannonAnim=Math.max(0,cannonAnim-dt);
            if(shotTimer>0)shotTimer=Math.max(0,shotTimer-dt);
            if(!running&&!preWave&&!gameOver)return;
            if(preWave){preWaveTimer-=dt;if(preWaveTimer<=0){preWave=false;running=true;audio.play("chuva_meteoros_inicio.wav");}return;}
            if(gameOver||quizOpen){updateParticles(dt);updateMilitary(dt);return;}
            if(inv.shieldSeconds>0)inv.shieldSeconds=Math.max(0,inv.shieldSeconds-dt);
            spawnTimer-=dt;if(spawnTimer<=0){spawnMeteor();spawnTimer=waves.spawnSeconds()*(.82f+rnd.nextFloat()*.36f);}
            if(commercial==null&&rnd.nextFloat()<waves.commercialPlaneChancePerSecond()*dt*60f)spawnCommercial();
            updatePlane(commercial,dt);updateMilitary(dt);updateMeteors(dt);updateParticles(dt);
            if(waves.complete()&&meteors.isEmpty()&&military==null){audio.play("fase_concluida.wav");waves.nextWave();beginNextWave();}
        }

        void updateCityShake(float dt){
            if(!cityShaking){cityShakeOffset=0;return;}
            cityShakeT+=dt*30f;
            if(cityShakeT<cityShakeT2){
                cityShakeOffset=(float)(Math.exp(-cityShakeGamma*cityShakeT)*cityShakeAlpha*Math.cos(20f*cityShakeT-10f));
            }else{cityShaking=false;cityShakeT=0;cityShakeOffset=0;}
        }

        void startCityShake(float damage, boolean nuclear){
            float escala=nuclear?5f:Math.max(2f,Math.min(6f,2f+damage/4f));
            cityShakeAlpha=escala*25f/4f;
            cityShakeGamma=(nuclear?.75f:escala/4f)*.025f;
            cityShakeT=0;cityShakeT2=nuclear?90f:18f*escala;cityShaking=true;
            if(vibrationEnabled){
                try{Vibrator v=(Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);if(v!=null)v.vibrate((long)(nuclear?650:65*escala));}
                catch(Exception ignored){}
            }
        }

        void spawnMeteor(){
            Meteor m=new Meteor();m.x=45+rnd.nextInt(710);m.y=-30;m.speed=waves.meteorSpeed()*(.85f+rnd.nextFloat()*.35f);m.radius=20;
            float roll=rnd.nextFloat();
            if(roll<waves.bonusChance())setupBonus(m);
            else if(roll<waves.bonusChance()+waves.specialChance()){
                boolean mult=rnd.nextBoolean();m.kind=mult?Kind.MULT:Kind.ADD;m.quiz=MeteorMathV2.generateQuiz(rnd,mult,waves.wave);
                m.value=m.quiz.answer;m.originalValue=m.value;m.radius=22;audio.play(mult?"meteoro_multiplicacao.wav":"meteoro_adicao.wav");
            }else{
                m.kind=Kind.NORMAL;
                boolean largePrime=waves.allowLargePrime()&&inv.subtractorCharge>0;
                m.value=MeteorMathV2.generateNormalValue(rnd,waves.maxMeteorValue(),inv.highestDivisor(),largePrime);m.originalValue=m.value;
            }
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
        void updateMilitary(float dt){
            if(military==null)return;military.x+=military.speed*dt;military.strikeTimer-=dt;
            if(military.target!=null){military.target.targetBlink=true;military.target.blinkTime+=dt;}
            if(military.strikeTimer<=0&&military.target!=null&&!military.target.dead){audio.play("aviao_militar_bomba.wav");explode(military.target,true,true);military.target=null;}
            if(military.x>880)military=null;
        }

        void updateMeteors(float dt){
            Iterator<Meteor> it=meteors.iterator();while(it.hasNext()){
                Meteor m=it.next();if(m.dead){it.remove();continue;}m.y+=m.speed*dt;
                if(commercial!=null&&commercial.active&&RectF.intersects(m.bounds(),commercial.bounds())){
                    commercial.active=false;commercial=null;audio.play("aviao_comercial_atingido.wav");damageCity(6,m.x,false);burst(m.x,m.y,Color.LTGRAY,18);m.dead=true;continue;
                }
                if(m.y>318){if(m.kind==Kind.BONUS)damageCity(3,m.x,false);else damageCity(Math.min(18,4+m.value/18f),m.x,false);m.dead=true;}
            }
        }

        void updateParticles(float dt){
            Iterator<Particle> it=particles.iterator();while(it.hasNext()){
                Particle q=it.next();q.life-=dt;if(q.life<=0){it.remove();continue;}q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=45*dt;
            }
        }

        void damageCity(float amount,float impactX,boolean nuclear){
            if(inv.shieldSeconds>0&&!nuclear){audio.play("bonus_escudo.wav");return;}
            cityHealth-=amount;lastImpactX=(int)impactX;audio.play("impacto_cidade.wav");startCityShake(amount,nuclear);
            if(cityHealth<=0){cityHealth=0;gameOver=true;running=false;audio.play("game_over.wav");}
        }

        void aimAndFire(Meteor m){
            cannonAngle=(float)Math.toDegrees(Math.atan2(m.y-cannonY,m.x-cannonX));
            cannonAnim=.40f;shotTimer=shotDuration;shotTargetX=m.x;shotTargetY=m.y;audio.play("disparo_canhao.wav");
        }

        void hitMeteor(Meteor m){
            if(m==null||m.dead)return;
            if(m.kind==Kind.BONUS){collectBonus(m);return;}
            if(m.kind==Kind.ADD||m.kind==Kind.MULT){openQuiz(m);return;}
            if(selectedMode==1){useSubtractor(m);return;}if(selectedMode==2){useBomb(m);return;}
            aimAndFire(m);int d=inv.selectedDivisor;
            if(MeteorMathV2.canDivide(m.value,d)){
                m.value/=d;audio.play("divisao_correta.wav");burst(m.x,m.y,Color.rgb(100,255,120),10);m.radius=Math.max(11,m.radius*.88f);
                if(m.value<=1)explode(m,true,false);
            }else{audio.play("divisao_errada.wav");burst(m.x,m.y,Color.rgb(255,80,60),7);score=Math.max(0,score-2);}
        }

        void collectBonus(Meteor m){
            switch(m.bonus){
                case AMMO:if(inv.unlockDivisor(m.ammoValue))audio.play("municao_desbloqueada.wav");else audio.play("bonus_municao.wav");break;
                case SUBTRACTOR:inv.addSubtractor(m.value);audio.play("bonus_subtrator.wav");break;
                case MONEY:inv.money+=m.value;audio.play("bonus_dinheiro.wav");break;
                case HEALTH:cityHealth=Math.min(100,cityHealth+m.value);audio.play("bonus_saude.wav");break;
                case SHIELD:inv.shieldSeconds=Math.max(inv.shieldSeconds,10);audio.play("bonus_escudo.wav");break;
                case BOMB0:inv.bombZero++;audio.play("bonus_municao.wav");break;
            }
            m.dead=true;burst(m.x,m.y,Color.YELLOW,12);
        }

        void useSubtractor(Meteor m){
            int amount=Math.min(inv.subtractorValue,m.value);if(amount<1||!inv.spendSubtractor(amount)){audio.play("divisao_errada.wav");return;}
            m.value-=amount;audio.play("subtrator_uso.wav");burst(m.x,m.y,Color.CYAN,10);if(m.value<=0)explode(m,true,false);
        }

        void useBomb(Meteor m){
            if(inv.bombZero<=0){audio.play("divisao_errada.wav");return;}inv.bombZero--;audio.play("bomba_zero.wav");damageCity(7,400,true);explode(m,true,false);
        }

        void explode(Meteor m,boolean count,boolean guaranteedBonus){
            if(m.dead)return;m.dead=true;audio.play("explosao_meteoro.wav");burst(m.x,m.y,Color.rgb(255,150,35),24);
            if(count){waves.countDestroyed();score+=10+Math.min(40,m.originalValue/3);if(guaranteedBonus)spawnBonusAt(m.x,m.y);}
        }

        void spawnBonusAt(float x,float y){Meteor b=new Meteor();b.x=x;b.y=y;b.speed=32;b.radius=18;setupBonus(b);meteors.add(b);}
        void openQuiz(Meteor m){quizOpen=true;quizMeteor=m;quizAttempts=0;audio.play("quiz_abre.wav");}
        void answerQuiz(int option){
            if(!quizOpen||quizMeteor==null)return;
            if(option==quizMeteor.quiz.answer){audio.play("quiz_acerto.wav");audio.play("alvo_trava.wav");quizOpen=false;startMilitaryStrike(quizMeteor);}
            else{quizAttempts++;audio.play("quiz_erro.wav");if(quizAttempts>=2){quizMeteor.kind=Kind.NORMAL;quizMeteor.value=quizMeteor.quiz.answer;quizMeteor.originalValue=quizMeteor.value;quizMeteor.quiz=null;quizOpen=false;quizMeteor=null;}}
        }
        void startMilitaryStrike(Meteor target){military=new Plane();military.military=true;military.x=-70;military.y=Math.max(55,target.y-70);military.speed=180;military.target=target;military.strikeTimer=1.15f;audio.play("aviao_militar_passagem.wav");quizMeteor=null;}
        void burst(float x,float y,int color,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float s=25+rnd.nextFloat()*70;particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,.35f+rnd.nextFloat()*.45f,color,2+rnd.nextFloat()*3));}}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.save();c.scale(scaleX,scaleY);
            if(!running&&!preWave&&!gameOver){drawMenu(c);}else{drawGame(c);}c.restore();
        }

        void drawMenu(Canvas c){
            p.setColor(Color.rgb(3,10,25));c.drawRect(0,0,800,480,p);if(menuBg!=null)drawCenterCrop(c,menuBg,new RectF(0,0,800,480),.56f);
            p.setColor(Color.argb(45,0,8,20));c.drawRect(0,0,800,480,p);
            if(menuPage==MENU_MAIN)drawMainMenu(c);else if(menuPage==MENU_HOW)drawHowTo(c);else if(menuPage==MENU_OPTIONS)drawOptions(c);else drawCredits(c);
        }

        void drawMainMenu(Canvas c){
            drawText(c,"ASTEROIDE MATEMATICO",400,65,34,Color.WHITE,true);
            drawText(c,"DEFENDA A CIDADE COM A MATEMATICA",400,91,14,Color.rgb(180,225,255),true);
            String[] labels={"INICIAR JOGO","COMO JOGAR","OPCOES","CREDITOS","SAIR"};
            float top=145;
            for(int i=0;i<labels.length;i++){
                RectF r=menuButtons[i];r.set(292,top+i*50,508,top+38+i*50);drawMenuButton(c,r,labels[i],i==0);
            }
        }

        void drawHowTo(Canvas c){
            drawDarkCard(c,105,55,695,405);drawText(c,"COMO JOGAR",400,90,28,Color.CYAN,true);
            drawText(c,"Comece com os divisores 2 e 3. Divida o numero do meteoro ate chegar a 1.",400,132,15,Color.WHITE,true);
            drawText(c,"Novos primos ate 31 aparecem como bonus e liberam novas municoes.",400,162,15,Color.WHITE,true);
            drawText(c,"SUBTRATOR: escolha um valor e gaste essa carga para transformar numeros dificeis.",400,192,15,Color.WHITE,true);
            drawText(c,"Meteoros verdes e amarelos abrem desafios de multiplicacao e adicao.",400,222,15,Color.WHITE,true);
            drawText(c,"Acertando o quiz, o alvo trava e um aviao militar elimina o meteoro.",400,252,15,Color.WHITE,true);
            drawText(c,"Proteja tambem os avioes comerciais e recolha dinheiro, vida e escudo.",400,282,15,Color.WHITE,true);
            drawText(c,"A bomba 0 destroi qualquer numero, mas o impacto tambem abala a cidade.",400,312,15,Color.YELLOW,true);
            drawMenuButton(c,new RectF(310,345,490,385),"VOLTAR",false);
        }

        void drawOptions(Canvas c){
            drawDarkCard(c,190,75,610,385);drawText(c,"OPCOES",400,115,30,Color.CYAN,true);
            drawMenuButton(c,new RectF(260,150,540,196),"SONS: "+(audio.enabled?"LIGADOS":"DESLIGADOS"),false);
            drawMenuButton(c,new RectF(260,215,540,261),"VIBRACAO: "+(vibrationEnabled?"LIGADA":"DESLIGADA"),false);
            drawText(c,"DIFICULDADE: PROGRESSIVA",400,300,17,Color.WHITE,true);
            drawText(c,"Os numeros, velocidade e eventos aumentam gradualmente por onda.",400,326,13,Color.LTGRAY,true);
            drawMenuButton(c,new RectF(310,340,490,380),"VOLTAR",false);
        }

        void drawCredits(Canvas c){
            drawDarkCard(c,165,70,635,390);drawText(c,"CREDITOS",400,112,30,Color.CYAN,true);
            drawText(c,"Asteroide Matematico",400,155,22,Color.WHITE,true);
            drawText(c,"Projeto original de conclusao de curso - preservado e modernizado.",400,190,14,Color.WHITE,true);
            drawText(c,"Graficos originais mantidos; novos elementos seguem a mesma identidade.",400,220,14,Color.WHITE,true);
            drawText(c,"Versao modernizada: novas operacoes, bonus, avioes e progressao.",400,250,14,Color.WHITE,true);
            drawText(c,"Sons gerados especificamente para esta versao do projeto.",400,280,14,Color.WHITE,true);
            drawMenuButton(c,new RectF(310,330,490,372),"VOLTAR",false);
        }

        void drawGame(Canvas c){
            p.setColor(Color.rgb(7,20,42));c.drawRect(0,0,800,480,p);drawStars(c);
            c.save();c.translate(0,cityShakeOffset);
            drawCity(c);
            for(Meteor m:meteors)if(!m.dead)drawMeteor(c,m);
            if(commercial!=null&&commercial.active)drawPlane(c,commercial);if(military!=null)drawPlane(c,military);
            drawCannon(c);drawProjectile(c);
            for(Particle q:particles){p.setColor(q.color);p.setAlpha((int)(255*q.life/q.maxLife));c.drawRect(q.x-q.size,q.y-q.size,q.x+q.size,q.y+q.size,p);p.setAlpha(255);}
            c.restore();
            drawHud(c);
            if(preWave){p.setColor(Color.argb(125+(int)(70*Math.abs(Math.sin(preWaveTimer*4))),180,0,0));c.drawRect(0,0,800,390,p);drawText(c,"ALERTA - ONDA "+waves.wave,400,185,34,Color.WHITE,true);drawText(c,"METEOROS SE APROXIMANDO",400,225,20,Color.YELLOW,true);drawText(c,"UON...  UON...  UON...",400,275,19,Color.WHITE,true);}
            if(quizOpen)drawQuiz(c);
            if(gameOver){p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,800,480,p);drawText(c,"FIM DE JOGO",400,205,38,Color.RED,true);drawText(c,"Pontos: "+score,400,245,24,Color.WHITE,true);drawMenuButton(c,new RectF(305,280,495,330),"REINICIAR",true);}
        }

        void drawStars(Canvas c){p.setColor(Color.rgb(120,160,205));for(int i=0;i<42;i++){int x=(i*97+31)%800;int y=(i*53+17)%305;c.drawRect(x,y,x+1,y+1,p);}}

        void drawCity(Canvas c){
            if(cityImg!=null)c.drawBitmap(cityImg,null,new RectF(0,315,800,390),pixel);else{p.setColor(Color.DKGRAY);c.drawRect(0,330,800,390,p);}
            int flames=cityHealth>=75?0:1+(int)((75-cityHealth)/13f);
            for(int i=0;i<flames;i++){float x=55+(i*137+lastImpactX/3)%690;drawFire(c,x,337+(i%2)*12);}
        }

        void drawFire(Canvas c,float x,float y){
            float pulse=1f+.12f*(float)Math.sin(SystemClock.uptimeMillis()/75.0+x);
            if(smokeImg!=null){p.setAlpha(115);c.drawBitmap(smokeImg,null,new RectF(x-9,y-34,x+13,y-12),pixel);p.setAlpha(255);}
            if(fireImg!=null)c.drawBitmap(fireImg,null,new RectF(x-13*pulse,y-25*pulse,x+13*pulse,y+1),pixel);
            else{p.setColor(Color.rgb(255,90,20));c.drawRect(x-4,y-18,x+5,y,p);}
        }

        void drawCannon(Canvas c){
            int towerFrame=(int)((SystemClock.uptimeMillis()/80)%8);
            if(turretSheet!=null)drawTile(c,turretSheet,8,1,towerFrame,new RectF(cannonX-20,292,cannonX+20,365),pixel);
            int frame=8;
            if(cannonAnim>0){float elapsed=.40f-cannonAnim;frame=Math.max(0,Math.min(7,(int)(elapsed/.05f)));}
            if(cannonSheet!=null){
                c.save();c.rotate(cannonAngle+90f,cannonX,cannonY);drawTile(c,cannonSheet,8,2,frame,new RectF(cannonX-34,cannonY-34,cannonX+34,cannonY+34),pixel);c.restore();
            }
        }

        void drawProjectile(Canvas c){
            if(shotTimer<=0)return;float t=1f-shotTimer/shotDuration;t=Math.max(0,Math.min(1,t));
            float x=cannonX+(shotTargetX-cannonX)*t;float y=cannonY+(shotTargetY-cannonY)*t;
            if(projectileSheet!=null)drawTile(c,projectileSheet,8,1,Math.min(7,(int)(t*8)),new RectF(x-10,y-10,x+10,y+10),pixel);
            else{p.setColor(Color.YELLOW);c.drawCircle(x,y,4,p);}
        }

        void drawMeteor(Canvas c,Meteor m){
            if(tailImg!=null&&m.kind!=Kind.BONUS){p.setAlpha(150);c.drawBitmap(tailImg,null,new RectF(m.x-7,m.y-m.radius-27,m.x+7,m.y-m.radius+5),pixel);p.setAlpha(255);}
            if(m.kind==Kind.BONUS){drawBonusMeteor(c,m);}
            else{
                Paint use=pixel;
                if(m.kind==Kind.MULT){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(80,205,105),PorterDuff.Mode.MULTIPLY));use=tintPaint;}
                else if(m.kind==Kind.ADD){tintPaint.setColorFilter(new PorterDuffColorFilter(Color.rgb(255,220,80),PorterDuff.Mode.MULTIPLY));use=tintPaint;}
                int frame=((int)(SystemClock.uptimeMillis()/130)+Math.abs(m.originalValue))%4;
                RectF dest=new RectF(m.x-m.radius,m.y-m.radius,m.x+m.radius,m.y+m.radius);
                if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,frame,dest,use);else{p.setColor(Color.rgb(100,88,68));c.drawCircle(m.x,m.y,m.radius,p);}
                tintPaint.setColorFilter(null);
                String text=(m.kind==Kind.ADD||m.kind==Kind.MULT)?m.quiz.expression():String.valueOf(m.value);drawText(c,text,m.x,m.y+5,15,Color.WHITE,true);
            }
            if(m.targetBlink&&((int)(m.blinkTime*10)%2==0)){
                if(targetImg!=null)c.drawBitmap(targetImg,null,new RectF(m.x-25,m.y-25,m.x+25,m.y+25),pixel);
                else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);c.drawCircle(m.x,m.y,m.radius+8,p);p.setStyle(Paint.Style.FILL);}
            }
        }

        void drawBonusMeteor(Canvas c,Meteor m){
            Bitmap icon=null;
            if(m.bonus==Bonus.MONEY)icon=moneyImg;else if(m.bonus==Bonus.SUBTRACTOR)icon=subImg;else if(m.bonus==Bonus.BOMB0)icon=bombImg;
            if(icon!=null)c.drawBitmap(icon,null,new RectF(m.x-17,m.y-17,m.x+17,m.y+17),pixel);
            else{
                int tile=m.bonus==Bonus.HEALTH?4:m.bonus==Bonus.SHIELD?5:1;
                if(meteorSheet!=null)drawTile(c,meteorSheet,8,1,tile,new RectF(m.x-17,m.y-17,m.x+17,m.y+17),pixel);
                else{p.setColor(Color.rgb(60,135,190));c.drawCircle(m.x,m.y,17,p);}
            }
            drawText(c,bonusText(m),m.x,m.y+5,13,Color.WHITE,true);
        }

        String bonusText(Meteor m){
            switch(m.bonus){case AMMO:return "+"+m.ammoValue;case SUBTRACTOR:return "SUB "+m.value;case MONEY:return "$"+m.value;case HEALTH:return "+"+m.value;case SHIELD:return "ESC";default:return "x0";}
        }

        void drawPlane(Canvas c,Plane pl){Bitmap b=pl.military?planeMilitary:planeCommercial;if(b!=null)c.drawBitmap(b,null,pl.bounds(),pixel);else{p.setColor(pl.military?Color.GREEN:Color.WHITE);c.drawRect(pl.bounds(),p);}}

        void drawHud(Canvas c){
            if(lane!=null)c.drawBitmap(lane,null,new RectF(0,390,800,480),pixel);else{p.setColor(Color.argb(235,0,16,35));c.drawRect(0,390,800,480,p);}
            int x=12;
            for(int i=0;i<MeteorMathV2.PRIMES.length;i++){
                int prime=MeteorMathV2.PRIMES[i];RectF r=new RectF(x,418,x+34,456);divisorRects[i]=r;boolean have=inv.divisors.contains(prime);
                if(molduraSheet!=null)drawTile(c,molduraSheet,4,1,have?0:2,r,pixel);
                else{p.setColor(have?(inv.selectedDivisor==prime&&selectedMode==0?Color.rgb(45,135,190):Color.rgb(20,65,90)):Color.rgb(35,40,48));c.drawRect(r,p);}
                if(have&&inv.selectedDivisor==prime&&selectedMode==0){p.setColor(Color.CYAN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(r,p);p.setStyle(Paint.Style.FILL);}
                drawText(c,String.valueOf(prime),r.centerX(),443,14,have?Color.WHITE:Color.GRAY,true);x+=40;
            }
            subMinus.set(465,418,493,454);subPlus.set(546,418,574,454);subUse.set(496,418,543,454);p.setColor(selectedMode==1?Color.rgb(65,120,150):Color.rgb(25,60,80));c.drawRect(459,405,580,462,p);
            if(subImg!=null)c.drawBitmap(subImg,null,new RectF(463,407,483,427),pixel);drawText(c,"-",479,445,20,Color.WHITE,true);drawText(c,"+",560,445,20,Color.WHITE,true);drawText(c,String.valueOf(inv.subtractorValue),520,444,18,Color.CYAN,true);drawText(c,"CARGA "+inv.subtractorCharge,523,412,11,Color.WHITE,true);
            bombRect.set(588,404,642,462);p.setColor(selectedMode==2?Color.rgb(150,85,35):Color.rgb(60,45,20));c.drawRect(bombRect,p);if(bombImg!=null)c.drawBitmap(bombImg,null,new RectF(598,411,630,443),pixel);drawText(c,"x"+inv.bombZero,616,458,11,Color.YELLOW,true);
            repairRect.set(650,404,792,462);p.setColor(Color.rgb(25,70,45));c.drawRect(repairRect,p);if(moneyImg!=null)c.drawBitmap(moneyImg,null,new RectF(658,411,690,443),pixel);drawText(c,"$"+inv.money+"  REPARAR",738,438,13,Color.WHITE,true);
            drawText(c,"ONDA "+waves.wave+"   "+waves.destroyedThisWave+"/"+waves.targetThisWave+"   MAX "+waves.maxMeteorValue(),12,22,14,Color.WHITE,false);drawText(c,"PONTOS "+score,12,43,14,Color.YELLOW,false);
            p.setColor(Color.rgb(60,20,20));c.drawRect(12,55,220,71,p);p.setColor(cityHealth>60?Color.GREEN:cityHealth>30?Color.YELLOW:Color.RED);c.drawRect(12,55,12+208*cityHealth/100f,71,p);drawText(c,"CIDADE "+(int)cityHealth+"%",116,68,11,Color.WHITE,true);if(inv.shieldSeconds>0)drawText(c,"ESCUDO "+(int)Math.ceil(inv.shieldSeconds),250,22,13,Color.CYAN,false);
        }

        void drawQuiz(Canvas c){
            p.setColor(Color.argb(215,0,0,0));c.drawRect(0,0,800,390,p);drawDarkCard(c,170,95,630,350);
            String q=quizMeteor.quiz.expression()+" = ?";drawText(c,q,400,158,32,quizMeteor.kind==Kind.MULT?Color.rgb(100,255,130):Color.YELLOW,true);
            drawText(c,"Escolha o resultado  (tentativa "+(quizAttempts+1)+"/2)",400,195,15,Color.WHITE,true);
            for(int i=0;i<3;i++){float left=225+i*125;RectF r=new RectF(left,235,left+100,300);drawMenuButton(c,r,String.valueOf(quizMeteor.quiz.options[i]),false);}
        }

        void drawCenterCrop(Canvas c,Bitmap b,RectF dst,float biasY){
            float srcRatio=b.getWidth()/(float)b.getHeight(),dstRatio=dst.width()/dst.height();Rect src;
            if(srcRatio>dstRatio){int sw=Math.round(b.getHeight()*dstRatio);int left=(b.getWidth()-sw)/2;src=new Rect(left,0,left+sw,b.getHeight());}
            else{int sh=Math.round(b.getWidth()/dstRatio);int extra=b.getHeight()-sh;int top=Math.round(extra*Math.max(0,Math.min(1,biasY)));src=new Rect(0,top,b.getWidth(),top+sh);}
            c.drawBitmap(b,src,dst,pixel);
        }

        void drawTile(Canvas c,Bitmap sheet,int cols,int rows,int index,RectF dst,Paint paint){
            if(sheet==null)return;int tw=sheet.getWidth()/cols,th=sheet.getHeight()/rows;index=Math.max(0,Math.min(cols*rows-1,index));int col=index%cols,row=index/cols;
            c.drawBitmap(sheet,new Rect(col*tw,row*th,col*tw+tw,row*th+th),dst,paint);
        }

        void drawDarkCard(Canvas c,float l,float t,float r,float b){p.setColor(Color.argb(218,0,13,30));c.drawRect(l,t,r,b,p);p.setColor(Color.rgb(35,130,185));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);}
        void drawMenuButton(Canvas c,RectF r,String label,boolean highlight){p.setColor(highlight?Color.argb(230,0,65,105):Color.argb(225,0,25,50));c.drawRect(r,p);p.setColor(highlight?Color.CYAN:Color.rgb(40,135,190));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(highlight?3:2);c.drawRect(r,p);p.setStyle(Paint.Style.FILL);drawText(c,label,r.centerX(),r.centerY()+7,18,Color.WHITE,true);}
        void drawText(Canvas c,String s,float x,float y,float size,int color,boolean center){p.setTypeface(gameFont);p.setFakeBoldText(true);p.setTextSize(size);p.setColor(color);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=lx(e.getX()),y=ly(e.getY());audio.play("ui_click.wav");
            if(!running&&!preWave&&!gameOver){handleMenuTouch(x,y);return true;}
            if(gameOver){if(x>=285&&x<=515&&y>=265&&y<=350)resetGame();return true;}
            if(preWave)return true;
            if(quizOpen){if(y>=225&&y<=315){for(int i=0;i<3;i++){float l=225+i*125;if(x>=l&&x<=l+100){answerQuiz(quizMeteor.quiz.options[i]);return true;}}}return true;}
            for(int i=0;i<divisorRects.length;i++){RectF r=divisorRects[i];if(r!=null&&r.contains(x,y)&&inv.divisors.contains(MeteorMathV2.PRIMES[i])){selectedMode=0;inv.selectedDivisor=MeteorMathV2.PRIMES[i];return true;}}
            if(subMinus.contains(x,y)){selectedMode=1;if(inv.subtractorValue>1)inv.subtractorValue--;return true;}
            if(subPlus.contains(x,y)){selectedMode=1;if(inv.subtractorValue<Math.max(1,inv.subtractorCharge)&&inv.subtractorValue<waves.maxMeteorValue())inv.subtractorValue++;return true;}
            if(subUse.contains(x,y)){selectedMode=1;return true;}
            if(bombRect.contains(x,y)){selectedMode=2;return true;}
            if(repairRect.contains(x,y)&&inv.repair(cityHealth)){cityHealth=Math.min(100,cityHealth+20);audio.play("reconstrucao_cidade.wav");return true;}
            Meteor hit=null;for(int i=meteors.size()-1;i>=0;i--){Meteor m=meteors.get(i);if(!m.dead&&m.bounds().contains(x,y)){hit=m;break;}}
            if(hit!=null)hitMeteor(hit);return true;
        }

        void handleMenuTouch(float x,float y){
            if(menuPage==MENU_MAIN){
                for(int i=0;i<menuButtons.length;i++)if(menuButtons[i].contains(x,y)){
                    if(i==0)resetGame();else if(i==1)menuPage=MENU_HOW;else if(i==2)menuPage=MENU_OPTIONS;else if(i==3)menuPage=MENU_CREDITS;else GameV2Activity.this.finish();return;
                }
            }else if(menuPage==MENU_HOW||menuPage==MENU_CREDITS){if(x>=290&&x<=510&&y>=320&&y<=405)menuPage=MENU_MAIN;}
            else if(menuPage==MENU_OPTIONS){
                if(x>=245&&x<=555&&y>=135&&y<=205){audio.enabled=!audio.enabled;if(!audio.enabled)audio.stopLong();}
                else if(x>=245&&x<=555&&y>=205&&y<=275)vibrationEnabled=!vibrationEnabled;
                else if(x>=290&&x<=510&&y>=325&&y<=400)menuPage=MENU_MAIN;
            }
        }
    }
}
