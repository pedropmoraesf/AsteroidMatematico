package br.grassinimoraes.divasteroides;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.SystemClock;
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
        AudioBank(Context c) { ctx=c; }
        void load(String name) {
            try {
                AssetFileDescriptor fd=ctx.getAssets().openFd("audio/"+name);
                int id=pool.load(fd,1); fd.close(); ids.put(name,id);
            } catch(Exception ignored) {}
        }
        void play(String name) {
            Integer id=ids.get(name); if(id!=null) pool.play(id,1,1,1,0,1);
        }
        void playLong(String name) {
            stopLong();
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
        Particle(float x,float y,float vx,float vy,float life,int color,float size){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.maxLife=life;this.color=color;this.size=size;}
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
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Paint pixel=new Paint();
        final Random rnd=new Random(); final PlayerInventory inv=new PlayerInventory(); final WaveManager waves=new WaveManager();
        final List<Meteor> meteors=new ArrayList<Meteor>(); final List<Particle> particles=new ArrayList<Particle>();
        final AudioBank audio;
        Bitmap menuBg,lane,moneyImg,subImg,planeCommercial,planeMilitary,bombImg,tailImg,targetImg,cityImg,turretImg;
        long last=SystemClock.uptimeMillis(); float spawnTimer;
        float cityHealth=100; int score=0; boolean running=false, preWave=false, quizOpen=false, gameOver=false;
        float preWaveTimer=0; Meteor quizMeteor; int quizAttempts; Plane commercial,military;
        int selectedMode=0;
        final RectF[] divisorRects=new RectF[MeteorMathV2.PRIMES.length];
        final RectF subMinus=new RectF(),subPlus=new RectF(),subUse=new RectF(),bombRect=new RectF(),repairRect=new RectF();
        float logicalW=800,logicalH=480,scaleX=1,scaleY=1;

        GameView(Context c){ super(c); setFocusable(true); pixel.setAntiAlias(false); audio=new AudioBank(c); loadAssets(); loadAudio(); }

        void loadAssets(){
            menuBg=assetBitmap("graficos/menu_cena.png"); lane=assetBitmap("graficos/lane_armas.png"); moneyImg=assetBitmap("graficos/dinheiro_bonus.png");
            subImg=assetBitmap("graficos/subtrator.png"); planeCommercial=assetBitmap("graficos/aviao_comercial.png"); planeMilitary=assetBitmap("graficos/aviao_militar.png");
            bombImg=assetBitmap("graficos/bomba0.png"); tailImg=assetBitmap("graficos/rastro_meteoro.png"); targetImg=assetBitmap("graficos/alvo_quiz.png");
            cityImg=assetBitmap("graficos/cidade_grande.png"); turretImg=assetBitmap("graficos/torre.png");
        }
        Bitmap assetBitmap(String path){ try{InputStream in=getContext().getAssets().open(path);Bitmap b=BitmapFactory.decodeStream(in);in.close();return b;}catch(IOException e){return null;} }
        void loadAudio(){
            String[] names={"disparo_canhao.wav","divisao_correta.wav","divisao_errada.wav","explosao_meteoro.wav","explosao_grande.wav","bomba_zero.wav","impacto_cidade.wav","bonus_municao.wav","bonus_dinheiro.wav","bonus_saude.wav","bonus_escudo.wav","bonus_subtrator.wav","municao_desbloqueada.wav","subtrator_uso.wav","quiz_abre.wav","quiz_acerto.wav","quiz_erro.wav","alvo_trava.wav","aviao_militar_passagem.wav","aviao_militar_bomba.wav","aviao_comercial_passagem.wav","aviao_comercial_atingido.wav","meteoro_entrada.wav","chuva_meteoros_inicio.wav","ui_click.wav","fase_concluida.wav","game_over.wav","reconstrucao_cidade.wav","meteoro_adicao.wav","meteoro_multiplicacao.wav"};
            for(String n:names) audio.load(n);
        }

        @Override protected void onAttachedToWindow(){super.onAttachedToWindow(); post(this);} 
        @Override protected void onDetachedFromWindow(){removeCallbacks(this);audio.release();super.onDetachedFromWindow();}
        @Override public void run(){ long now=SystemClock.uptimeMillis(); float dt=Math.min(.05f,(now-last)/1000f);last=now;update(dt);invalidate();postDelayed(this,16);}
        @Override protected void onSizeChanged(int w,int h,int ow,int oh){scaleX=w/logicalW;scaleY=h/logicalH;}
        float lx(float x){return x/scaleX;} float ly(float y){return y/scaleY;}

        void resetGame(){
            meteors.clear();particles.clear();cityHealth=100;score=0;running=false;preWave=true;gameOver=false;quizOpen=false;commercial=null;military=null;
            waves.wave=1;waves.destroyedThisWave=0;waves.targetThisWave=10;
            inv.divisors.clear();inv.divisors.add(2);inv.divisors.add(3);inv.selectedDivisor=2;inv.subtractorCharge=0;inv.subtractorValue=1;inv.money=0;inv.bombZero=0;inv.shieldSeconds=0;
            preWaveTimer=7.5f;spawnTimer=0;audio.playLong("sirene_80bpm_10.wav");
        }
        void beginNextWave(){ running=false;preWave=true;preWaveTimer=7.5f;audio.playLong("sirene_80bpm_10.wav"); }

        void update(float dt){
            if(!running && !preWave && !gameOver) return;
            if(preWave){ preWaveTimer-=dt; if(preWaveTimer<=0){preWave=false;running=true;audio.play("chuva_meteoros_inicio.wav");} return; }
            if(gameOver || quizOpen) { updateParticles(dt); updateMilitary(dt); return; }
            if(inv.shieldSeconds>0) inv.shieldSeconds=Math.max(0,inv.shieldSeconds-dt);
            spawnTimer-=dt; if(spawnTimer<=0){spawnMeteor();spawnTimer=waves.spawnSeconds()*(.75f+rnd.nextFloat()*.5f);}
            if(commercial==null && rnd.nextFloat()<waves.commercialPlaneChancePerSecond()*dt*60f) spawnCommercial();
            updatePlane(commercial,dt); updateMilitary(dt); updateMeteors(dt); updateParticles(dt);
            if(waves.complete() && meteors.isEmpty() && military==null){audio.play("fase_concluida.wav");waves.nextWave();beginNextWave();}
        }

        void spawnMeteor(){
            Meteor m=new Meteor();m.x=45+rnd.nextInt(710);m.y=-30;m.speed=waves.meteorSpeed()*(.82f+rnd.nextFloat()*.42f);m.radius=20;
            float roll=rnd.nextFloat();
            if(roll < waves.bonusChance()) { setupBonus(m); }
            else if(roll < waves.bonusChance()+waves.specialChance()) {
                boolean mult=rnd.nextBoolean();m.kind=mult?Kind.MULT:Kind.ADD;m.quiz=MeteorMathV2.generateQuiz(rnd,mult,waves.wave);m.value=m.quiz.answer;m.originalValue=m.value;m.radius=22;
                audio.play(mult?"meteoro_multiplicacao.wav":"meteoro_adicao.wav");
            } else { m.kind=Kind.NORMAL;m.value=MeteorMathV2.generateNormalValue(rnd,waves.maxMeteorValue(),inv.highestDivisor(),waves.allowLargePrime());m.originalValue=m.value; }
            meteors.add(m); if(rnd.nextFloat()<.28f) audio.play("meteoro_entrada.wav");
        }

        void setupBonus(Meteor m){
            m.kind=Kind.BONUS;m.radius=18;int r=rnd.nextInt(100);
            int candidate=nextLockedPrime();
            if(candidate>0 && r<32){m.bonus=Bonus.AMMO;m.ammoValue=candidate;m.value=candidate;}
            else if(r<50){m.bonus=Bonus.SUBTRACTOR;m.value=10+Math.min(20,waves.wave*2);}
            else if(r<68){m.bonus=Bonus.MONEY;m.value=25;}
            else if(r<82){m.bonus=Bonus.HEALTH;m.value=15;}
            else if(r<92){m.bonus=Bonus.SHIELD;m.value=10;}
            else {m.bonus=Bonus.BOMB0;m.value=0;}
        }
        int nextLockedPrime(){ int ceiling=waves.primeUnlockCeiling(); for(int x:MeteorMathV2.PRIMES) if(x<=ceiling&&!inv.divisors.contains(x)) return x; return -1; }

        void spawnCommercial(){commercial=new Plane();commercial.x=-60;commercial.y=95+rnd.nextInt(135);commercial.speed=70+waves.wave*2;commercial.military=false;audio.play("aviao_comercial_passagem.wav");}
        void updatePlane(Plane pl,float dt){ if(pl==null||!pl.active)return;pl.x+=pl.speed*dt;if(pl.x>860){pl.active=false;if(pl==commercial)commercial=null;} }
        void updateMilitary(float dt){ if(military==null)return; military.x+=military.speed*dt;military.strikeTimer-=dt; if(military.target!=null){military.target.targetBlink=true;military.target.blinkTime+=dt;} if(military.strikeTimer<=0&&military.target!=null&&!military.target.dead){audio.play("aviao_militar_bomba.wav");explode(military.target,true,true);military.target=null;} if(military.x>880){military=null;} }

        void updateMeteors(float dt){
            Iterator<Meteor> it=meteors.iterator(); while(it.hasNext()){
                Meteor m=it.next(); if(m.dead){it.remove();continue;} m.y+=m.speed*dt;
                if(commercial!=null&&commercial.active&&RectF.intersects(m.bounds(),commercial.bounds())){commercial.active=false;commercial=null;audio.play("aviao_comercial_atingido.wav");damageCity(6);burst(m.x,m.y,Color.LTGRAY,18);m.dead=true;continue;}
                if(m.y>390){ if(m.kind==Kind.BONUS) damageCity(3); else damageCity(Math.min(18,4+m.value/18f)); m.dead=true; }
            }
        }
        void updateParticles(float dt){Iterator<Particle> it=particles.iterator();while(it.hasNext()){Particle q=it.next();q.life-=dt;if(q.life<=0){it.remove();continue;}q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=45*dt;}}
        void damageCity(float amount){ if(inv.shieldSeconds>0){audio.play("bonus_escudo.wav");return;} cityHealth-=amount;audio.play("impacto_cidade.wav");if(cityHealth<=0){cityHealth=0;gameOver=true;running=false;audio.play("game_over.wav");} }

        void hitMeteor(Meteor m){
            if(m==null||m.dead)return;
            if(m.kind==Kind.BONUS){collectBonus(m);return;}
            if(m.kind==Kind.ADD||m.kind==Kind.MULT){openQuiz(m);return;}
            if(selectedMode==1){useSubtractor(m);return;} if(selectedMode==2){useBomb(m);return;}
            audio.play("disparo_canhao.wav");int d=inv.selectedDivisor;
            if(MeteorMathV2.canDivide(m.value,d)){m.value/=d;audio.play("divisao_correta.wav");burst(m.x,m.y,Color.rgb(100,255,120),10);m.radius=Math.max(11,m.radius*.88f); if(m.value<=1)explode(m,true,false);}
            else {audio.play("divisao_errada.wav");burst(m.x,m.y,Color.rgb(255,80,60),7);score=Math.max(0,score-2);}
        }
        void collectBonus(Meteor m){
            switch(m.bonus){
                case AMMO: if(inv.unlockDivisor(m.ammoValue)){audio.play("municao_desbloqueada.wav");}else audio.play("bonus_municao.wav");break;
                case SUBTRACTOR:inv.addSubtractor(m.value);audio.play("bonus_subtrator.wav");break;
                case MONEY:inv.money+=m.value;audio.play("bonus_dinheiro.wav");break;
                case HEALTH:cityHealth=Math.min(100,cityHealth+m.value);audio.play("bonus_saude.wav");break;
                case SHIELD:inv.shieldSeconds=Math.max(inv.shieldSeconds,10);audio.play("bonus_escudo.wav");break;
                case BOMB0:inv.bombZero++;audio.play("bonus_municao.wav");break;
            } m.dead=true;burst(m.x,m.y,Color.YELLOW,12);
        }
        void useSubtractor(Meteor m){int amount=Math.min(inv.subtractorValue,m.value);if(amount<1||!inv.spendSubtractor(amount)){audio.play("divisao_errada.wav");return;}m.value-=amount;audio.play("subtrator_uso.wav");burst(m.x,m.y,Color.CYAN,10);if(m.value<=0)explode(m,true,false);}
        void useBomb(Meteor m){if(inv.bombZero<=0){audio.play("divisao_errada.wav");return;}inv.bombZero--;audio.play("bomba_zero.wav");damageCity(7);explode(m,true,false);}
        void explode(Meteor m,boolean count,boolean guaranteedBonus){if(m.dead)return;m.dead=true;audio.play("explosao_meteoro.wav");burst(m.x,m.y,Color.rgb(255,150,35),24);if(count){waves.countDestroyed();score+=10+Math.min(40,m.originalValue/3);if(guaranteedBonus)spawnBonusAt(m.x,m.y);}}
        void spawnBonusAt(float x,float y){Meteor b=new Meteor();b.x=x;b.y=y;b.speed=32;b.radius=18;setupBonus(b);meteors.add(b);}

        void openQuiz(Meteor m){quizOpen=true;quizMeteor=m;quizAttempts=0;audio.play("quiz_abre.wav");}
        void answerQuiz(int option){if(!quizOpen||quizMeteor==null)return;if(option==quizMeteor.quiz.answer){audio.play("quiz_acerto.wav");audio.play("alvo_trava.wav");quizOpen=false;startMilitaryStrike(quizMeteor);}
            else {quizAttempts++;audio.play("quiz_erro.wav");if(quizAttempts>=2){quizMeteor.kind=Kind.NORMAL;quizMeteor.value=quizMeteor.quiz.answer;quizMeteor.originalValue=quizMeteor.value;quizMeteor.quiz=null;quizOpen=false;quizMeteor=null;}}
        }
        void startMilitaryStrike(Meteor target){military=new Plane();military.military=true;military.x=-70;military.y=Math.max(55,target.y-70);military.speed=180;military.target=target;military.strikeTimer=1.15f;audio.play("aviao_militar_passagem.wav");quizMeteor=null;}
        void burst(float x,float y,int color,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float s=25+rnd.nextFloat()*70;particles.add(new Particle(x,y,(float)Math.cos(a)*s,(float)Math.sin(a)*s,.35f+rnd.nextFloat()*.45f,color,2+rnd.nextFloat()*3));}}

        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(scaleX,scaleY);if(!running&&!preWave&&!gameOver){drawMenu(c);}else{drawGame(c);}c.restore();}
        void drawMenu(Canvas c){p.setColor(Color.rgb(3,10,25));c.drawRect(0,0,800,480,p);if(menuBg!=null)c.drawBitmap(menuBg,null,new RectF(0,0,800,480),pixel);drawText(c,"ASTEROIDE MATEMATICO",400,92,34,Color.WHITE,true);drawPanel(c,265,275,535,338);drawText(c,"INICIAR",400,317,28,Color.rgb(110,220,255),true);drawText(c,"Divida, subtraia, some e multiplique para defender a cidade",400,365,16,Color.WHITE,true);}
        void drawGame(Canvas c){
            p.setColor(Color.rgb(7,20,42));c.drawRect(0,0,800,480,p);
            drawStars(c);drawCity(c);
            for(Meteor m:meteors)if(!m.dead)drawMeteor(c,m);
            if(commercial!=null&&commercial.active)drawPlane(c,commercial);if(military!=null)drawPlane(c,military);
            for(Particle q:particles){p.setColor(q.color);p.setAlpha((int)(255*q.life/q.maxLife));c.drawRect(q.x-q.size,q.y-q.size,q.x+q.size,q.y+q.size,p);p.setAlpha(255);}
            drawHud(c);
            if(preWave){p.setColor(Color.argb(125+(int)(70*Math.abs(Math.sin(preWaveTimer*4))),180,0,0));c.drawRect(0,0,800,480,p);drawText(c,"ALERTA - ONDA "+waves.wave,400,210,34,Color.WHITE,true);drawText(c,"DEFENDA A CIDADE",400,250,20,Color.YELLOW,true);}
            if(quizOpen)drawQuiz(c);
            if(gameOver){p.setColor(Color.argb(195,0,0,0));c.drawRect(0,0,800,480,p);drawText(c,"FIM DE JOGO",400,205,38,Color.RED,true);drawText(c,"Pontos: "+score,400,245,24,Color.WHITE,true);drawPanel(c,305,280,495,330);drawText(c,"REINICIAR",400,315,22,Color.CYAN,true);}
        }
        void drawStars(Canvas c){p.setColor(Color.rgb(120,160,205));for(int i=0;i<38;i++){int x=(i*97+31)%800;int y=(i*53+17)%280;c.drawRect(x,y,x+1,y+1,p);}}
        void drawCity(Canvas c){if(cityImg!=null)c.drawBitmap(cityImg,null,new RectF(0,392,800,480),pixel);else{p.setColor(Color.DKGRAY);c.drawRect(0,405,800,480,p);}if(turretImg!=null)c.drawBitmap(turretImg,null,new RectF(360,342,440,405),pixel);if(cityHealth<70){int flames=1+(int)((70-cityHealth)/12);for(int i=0;i<flames;i++)drawFlame(c,80+i*120+(i%2)*25,405);}}
        void drawFlame(Canvas c,float x,float y){p.setColor(Color.rgb(255,90,20));c.drawRect(x,y-14,x+5,y,p);p.setColor(Color.YELLOW);c.drawRect(x+1,y-9,x+4,y,p);}
        void drawMeteor(Canvas c,Meteor m){
            if(tailImg!=null)c.drawBitmap(tailImg,null,new RectF(m.x-8,m.y-m.radius-34,m.x+8,m.y-m.radius+5),pixel);
            int col=m.kind==Kind.MULT?Color.rgb(65,160,85):m.kind==Kind.ADD?Color.rgb(210,180,55):m.kind==Kind.BONUS?Color.rgb(70,130,190):Color.rgb(100,88,68);
            p.setColor(col);c.drawCircle(m.x,m.y,m.radius,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(180,160,115));c.drawCircle(m.x,m.y,m.radius,p);p.setStyle(Paint.Style.FILL);
            String t=m.kind==Kind.BONUS?bonusText(m):m.kind==Kind.ADD||m.kind==Kind.MULT?m.quiz.expression():String.valueOf(m.value);drawText(c,t,m.x,m.y+5,15,Color.WHITE,true);
            if(m.targetBlink&&((int)(m.blinkTime*10)%2==0)){if(targetImg!=null)c.drawBitmap(targetImg,null,new RectF(m.x-25,m.y-25,m.x+25,m.y+25),pixel);else{p.setColor(Color.RED);p.setStyle(Paint.Style.STROKE);c.drawCircle(m.x,m.y,m.radius+8,p);p.setStyle(Paint.Style.FILL);}}
        }
        String bonusText(Meteor m){switch(m.bonus){case AMMO:return "+"+m.ammoValue;case SUBTRACTOR:return "SUB "+m.value;case MONEY:return "$"+m.value;case HEALTH:return "+VIDA";case SHIELD:return "ESC";default:return "x0";}}
        void drawPlane(Canvas c,Plane pl){Bitmap b=pl.military?planeMilitary:planeCommercial;if(b!=null)c.drawBitmap(b,null,pl.bounds(),pixel);else{p.setColor(pl.military?Color.GREEN:Color.WHITE);c.drawRect(pl.bounds(),p);}}

        void drawHud(Canvas c){
            if(lane!=null)c.drawBitmap(lane,null,new RectF(0,384,800,480),pixel);else{p.setColor(Color.argb(230,0,16,35));c.drawRect(0,384,800,480,p);} 
            int x=14;for(int i=0;i<MeteorMathV2.PRIMES.length;i++){int prime=MeteorMathV2.PRIMES[i];RectF r=new RectF(x,414,x+34,452);divisorRects[i]=r;boolean have=inv.divisors.contains(prime);p.setColor(have?(inv.selectedDivisor==prime&&selectedMode==0?Color.rgb(45,135,190):Color.rgb(20,65,90)):Color.rgb(35,40,48));c.drawRect(r,p);drawText(c,String.valueOf(prime),r.centerX(),439,14,have?Color.WHITE:Color.GRAY,true);x+=40;}
            subMinus.set(470,414,498,448);subPlus.set(548,414,576,448);subUse.set(500,414,546,448);p.setColor(selectedMode==1?Color.rgb(65,120,150):Color.rgb(25,60,80));c.drawRect(465,403,582,456,p);drawText(c,"-",484,438,20,Color.WHITE,true);drawText(c,"+",562,438,20,Color.WHITE,true);drawText(c,String.valueOf(inv.subtractorValue),523,438,18,Color.CYAN,true);drawText(c,"SUB "+inv.subtractorCharge,523,408,12,Color.WHITE,true);
            bombRect.set(590,405,642,456);p.setColor(selectedMode==2?Color.rgb(150,85,35):Color.rgb(60,45,20));c.drawRect(bombRect,p);if(bombImg!=null)c.drawBitmap(bombImg,null,new RectF(600,411,632,443),pixel);drawText(c,""+inv.bombZero,632,453,12,Color.YELLOW,true);
            repairRect.set(650,405,792,456);p.setColor(Color.rgb(25,70,45));c.drawRect(repairRect,p);if(moneyImg!=null)c.drawBitmap(moneyImg,null,new RectF(658,411,690,443),pixel);drawText(c,"$"+inv.money+"  REPARAR",738,434,14,Color.WHITE,true);
            drawText(c,"ONDA "+waves.wave+"   "+waves.destroyedThisWave+"/"+waves.targetThisWave+"   MAX "+waves.maxMeteorValue(),12,25,15,Color.WHITE,false);drawText(c,"PONTOS "+score,12,47,15,Color.YELLOW,false);
            p.setColor(Color.rgb(60,20,20));c.drawRect(12,60,220,76,p);p.setColor(cityHealth>60?Color.GREEN:cityHealth>30?Color.YELLOW:Color.RED);c.drawRect(12,60,12+208*cityHealth/100f,76,p);drawText(c,"CIDADE "+(int)cityHealth+"%",116,73,12,Color.WHITE,true);if(inv.shieldSeconds>0)drawText(c,"ESCUDO "+(int)Math.ceil(inv.shieldSeconds),260,25,14,Color.CYAN,false);
        }
        void drawQuiz(Canvas c){p.setColor(Color.argb(210,0,0,0));c.drawRect(0,0,800,480,p);drawPanel(c,170,105,630,360);String q=quizMeteor.quiz.expression()+" = ?";drawText(c,q,400,170,32,quizMeteor.kind==Kind.MULT?Color.rgb(100,255,130):Color.YELLOW,true);drawText(c,"Escolha o resultado  (chance "+(quizAttempts+1)+"/2)",400,205,16,Color.WHITE,true);for(int i=0;i<3;i++){float left=225+i*125;drawPanel(c,left,245,left+100,310);drawText(c,String.valueOf(quizMeteor.quiz.options[i]),left+50,286,24,Color.CYAN,true);}}
        void drawPanel(Canvas c,float l,float t,float r,float b){p.setColor(Color.argb(220,0,18,38));c.drawRect(l,t,r,b,p);p.setColor(Color.rgb(35,130,185));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);}
        void drawText(Canvas c,String s,float x,float y,float size,int color,boolean center){p.setTypeface(android.graphics.Typeface.MONOSPACE);p.setFakeBoldText(true);p.setTextSize(size);p.setColor(color);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}

        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=lx(e.getX()),y=ly(e.getY());audio.play("ui_click.wav");
            if(!running&&!preWave&&!gameOver){if(x>=250&&x<=550&&y>=250&&y<=355)resetGame();return true;}
            if(gameOver){if(x>=285&&x<=515&&y>=265&&y<=350)resetGame();return true;}
            if(preWave)return true;
            if(quizOpen){if(y>=235&&y<=325){for(int i=0;i<3;i++){float l=225+i*125;if(x>=l&&x<=l+100){answerQuiz(quizMeteor.quiz.options[i]);return true;}}}return true;}
            for(int i=0;i<divisorRects.length;i++){RectF r=divisorRects[i];if(r!=null&&r.contains(x,y)&&inv.divisors.contains(MeteorMathV2.PRIMES[i])){selectedMode=0;inv.selectedDivisor=MeteorMathV2.PRIMES[i];return true;}}
            if(subMinus.contains(x,y)){selectedMode=1;if(inv.subtractorValue>1)inv.subtractorValue--;return true;}if(subPlus.contains(x,y)){selectedMode=1;if(inv.subtractorValue<Math.max(1,inv.subtractorCharge)&&inv.subtractorValue<waves.maxMeteorValue())inv.subtractorValue++;return true;}if(subUse.contains(x,y)){selectedMode=1;return true;}
            if(bombRect.contains(x,y)){selectedMode=2;return true;}if(repairRect.contains(x,y)&&inv.repair(cityHealth)){cityHealth=Math.min(100,cityHealth+20);audio.play("reconstrucao_cidade.wav");return true;}
            Meteor hit=null;for(int i=meteors.size()-1;i>=0;i--){Meteor m=meteors.get(i);if(!m.dead&&m.bounds().contains(x,y)){hit=m;break;}}if(hit!=null)hitMeteor(hit);return true; }
    }
}
