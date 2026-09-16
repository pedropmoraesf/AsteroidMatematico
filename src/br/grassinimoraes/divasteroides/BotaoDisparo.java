package br.grassinimoraes.divasteroides;
import org.andengine.entity.sprite.*;
import org.andengine.opengl.texture.region.*;
import org.andengine.opengl.vbo.*;
import org.andengine.entity.text.*;
import org.andengine.util.adt.align.*;
import org.andengine.opengl.font.*;
import org.andengine.input.touch.*;
import org.andengine.entity.modifier.*;
import org.andengine.entity.*;
import org.andengine.util.adt.color.*;

public class BotaoDisparo extends AnimatedSprite
{


  boolean cooldown_disp;

  private SequenceEntityModifier scm;

  private int primo;
  
  static int qtde=0;

  private int tipo;

  private AlphaModifier am1,am2;

  private AnimatedSprite canhao;

  private float azul;

  public static boolean canhao_desligado;

  private MainActivity atividade;

  private Color cor;
  
 // private boolean cooldown_bomba=true;
  
  
  BotaoDisparo(float x,float y,TiledTextureRegion text,AnimatedSprite canhao,MainActivity atividade, VertexBufferObjectManager vert){
	super(x,y,text,vert);
	
	this.setScale(8f);
	this.canhao=canhao;
	azul=canhao.getBlue();
	this.atividade=atividade;
	cor=this.getColor();

  }
  
 static void resetar_variaveis_estaticas(){
	canhao_desligado=false;
	qtde=0;
  }
  
   void setar_interior(int i,Font fonte){
    this.tipo=i;
	 this.setAlpha((i==6||i==7)?0:.25f);
	if(i<6){
	  this.setCurrentTileIndex(0);
	  String valor = (i==0)?"2":(i==1)?"3":(i==2)?"5":(i==3)?"7":(i==4)?"11":(i==5)?"13":"";
	  Text valor_disparo = new Text(0,0,fonte,valor,100,this.getVertexBufferObjectManager());

	  valor_disparo.setPosition((this.getWidth())/2, ( this.getHeight())/2);
	  valor_disparo.setScale(.25f);
	  valor_disparo.setHorizontalAlign(HorizontalAlign.CENTER);
	  this.attachChild(valor_disparo);
	  
	}else if(i==6){
	 this.setCurrentTileIndex(1);
	  
	}else if(i==7){
	  this.setCurrentTileIndex(2);
	}
  }
  @Override
  public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
  {
	if(pSceneTouchEvent.isActionUp()&&(cooldown_disp==false)&&this.getAlpha()>=.23f&&!canhao_desligado){
	  cooldown_disp=true;


	  scm=new SequenceEntityModifier(new ScaleModifier(.250f,this.getScaleX(),.75f*this.getScaleX()),new ScaleModifier(.125f,.75f*this.getScaleX(),this.getScaleX())){
		@Override
		public void onModifierFinished(final IEntity item){
		  cooldown_disp=false;

		}

	  };
	  scm.setAutoUnregisterWhenFinished(true);
	  
	  this.registerEntityModifier(scm);
	  //animacao do camhao disparando

	  canhao.animate (new long[]{50,50,50,50,50,50,50,50}, 0, 7, false, new AnimatedSprite.IAnimationListener(){

		  @Override
		  public void onAnimationStarted(AnimatedSprite p1, int p2)
		  {
			// TODO: Implement this method
		  }

		  @Override
		  public void onAnimationFrameChanged(AnimatedSprite p1, int p2, int p3)
		  {
			// TODO: Implement this method
		  }

		  @Override
		  public void onAnimationLoopFinished(AnimatedSprite p1, int p2, int p3)
		  {
			// TODO: Implement this method
		  }



		  @Override
		  public void onAnimationFinished(AnimatedSprite p1)
		  {
			p1.animate (new long[]{50,50,50,50,50,50,50,50},8,15,true);
			// TODO: Implement this method
		  }


		}
	  );
	  if(this.tipo<6){
		try{primo= Integer.parseInt(""+((Text)this.getChildByIndex(0)).getText());}
		catch(NumberFormatException e){primo=2;}
		
	  }else if(this.tipo==6||this.tipo==7){
		primo=(tipo==6)?1:0;

		am2 = new AlphaModifier(2, .25f, 0);
		

		am2.setAutoUnregisterWhenFinished(true);
		
		
		
		this.registerEntityModifier(am2);
		//this.getChildByIndex(0).registerEntityModifier(am2);
	  }
	  //criacao do projetil
	  
	  if(qtde>4){qtde=0;}
	  Projetil.projetil_lista.get(qtde).setar_velocidade(30,MainActivity.angulo);
	  Projetil.projetil_lista.get(qtde).setCurrentTileIndex((primo==2)?0:(primo==3)?1:(primo==5)?2:(primo==7)?3:(primo==11)?4:(primo==13)?5:(primo==1)?6:(primo==0)?7:0);
	  Projetil.projetil_lista.get(qtde).divisor=primo;
	  qtde+=1;
	  atividade.vibrar(6*primo);


	}
	return true;
  }

  @Override
  protected void onManagedUpdate(float pSecondsElapsed)
  {
	if(canhao_desligado&&this.getRotation()==0){
	  this.setRotation(1+89*(float)Math.random());
	  this.setColor(0,0,0);
	}else if(!canhao_desligado&&this.getRotation()!=0){
	  this.setRotation(0);
	  this.setColor(1,1,1);
	}
	if(this.tipo==6&&MainActivity.bomba_um){
	  MainActivity.bomba_um=false;
	  am1 = new AlphaModifier(2, 0, .25f);

	  am1.setAutoUnregisterWhenFinished(true);
	  this.registerEntityModifier(am1);
	  //this.getChildByIndex(0).registerEntityModifier(am1);
	}else if(this.tipo==7&&MainActivity.bomba_tudo){
	  MainActivity.bomba_tudo=false;
	  am1 = new AlphaModifier(2, 0, .25f);

	  am1.setAutoUnregisterWhenFinished(true);
	  this.registerEntityModifier(am1);
	 // this.getChildByIndex(0).registerEntityModifier(am1);
	}
	// TODO: Implement this method
	super.onManagedUpdate(pSecondsElapsed);
  }

  
}
