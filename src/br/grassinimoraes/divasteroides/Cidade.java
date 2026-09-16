package br.grassinimoraes.divasteroides;
import org.andengine.entity.sprite.*;
import org.andengine.opengl.texture.region.*;
import org.andengine.opengl.vbo.*;
import org.andengine.engine.camera.hud.*;
import org.andengine.entity.primitive.*;
import android.util.*;
import org.andengine.opengl.font.*;
import org.andengine.entity.text.*;
import org.andengine.util.adt.align.*;
import org.andengine.entity.particle.emitter.*;
import org.andengine.entity.particle.*;
import org.andengine.entity.particle.initializer.*;
import org.andengine.entity.particle.modifier.*;
import java.util.*;
import org.andengine.entity.scene.*;

public class Cidade extends Sprite
{
    private float fator_gama;
	float saude=100,pos_y,y_cam_ini=MainActivity.motor.getCamera().getCenterY(),escala_meteoro;
	private boolean terremoto=false,meteoro_destruido=false;
   
	

	private int t1,t2;

	private Rectangle barra_saude;

	private Rectangle barra_saude_dinamica;

  private float comprimento;

  private Text saude_visivel;

  private MainActivity atividade;

  private long memoria_usada;
  
  ArrayList<ParticleSystem> fogo_lista=new ArrayList<ParticleSystem>();

  private float mx;

  private float my;

  public boolean aumentou_saude;
  
  Random aleatorio=new Random();

  private float saude_minima_incendio=75;

  private boolean atingida;
  

   Cidade(float x, float y,TextureRegion text,MainActivity atividade, VertexBufferObjectManager vert){
	   super(x,y,text,vert);
	   this.atividade=atividade;
   }

   public void criar_barra_saude(HUD hud, float comprimento, float largura, float x_barra, float y_barra,Font fonte)
   {
	    barra_saude = new Rectangle(0,0,largura,comprimento,this.getVertexBufferObjectManager());
        barra_saude.setColor(1,0,0);
		hud.attachChild(barra_saude);
		barra_saude.setPosition(x_barra,y_barra);
		
	   barra_saude_dinamica = new Rectangle(barra_saude.getWidth(),barra_saude.getHeight()/2,barra_saude.getWidth(),barra_saude.getHeight(),this.getVertexBufferObjectManager());
	   barra_saude_dinamica.setAnchorCenter(1,.5f);
	   barra_saude_dinamica.setColor(0,1,0);
	   barra_saude.attachChild(barra_saude_dinamica);
	   this.comprimento=barra_saude_dinamica.getWidth();
	   barra_saude.setAlpha(.5f);
	   barra_saude_dinamica.setAlpha(.5f);
	   
	   //texto
	   
	 saude_visivel = new Text(0, 0, fonte, "" + String.format("%.0f",this.saude)+" %", 100, this.getVertexBufferObjectManager());;
	 saude_visivel.setScale(1);
	 saude_visivel.setPosition((barra_saude.getWidth()) / 2, (barra_saude.getHeight()) / 2);

	 saude_visivel.setHorizontalAlign(HorizontalAlign.CENTER);
	 barra_saude.attachChild(saude_visivel);
	 
		
	   // TODO: Implement this method
   }
   
	@Override
	protected void onManagedUpdate(float pSecondsElapsed){
		//dramatismo de tremor na cidade
        if(terremoto){
			if(t1<t2){tremer_cidade(escala_meteoro* 25/4,10,20,fator_gama* .025f,t1);t1++;}
			else{t1=0;terremoto=false;atividade.motor.getCamera().setCenter(atividade.motor.getCamera().getCenterX(),y_cam_ini);
		}}
		
		
	  if(this.saude<=75&&atingida){
        atingida=false;
		if(Math.round(saude)%5==0||saude<15){this.incendiar(mx,this.getHeightScaled()/4);}
		//game over
	  }
	  if(this.saude<=0){
		atividade.motor.getCamera().setCenter(atividade.motor.getCamera().getCenterX(),y_cam_ini);
		((Scene)this.getParent()).setIgnoreUpdate(true);
		atividade.controle_e_cena_pause.setChildScene(atividade.fim_de_jogo());
	  }
	  
	  
	  if(saude>75&&fogo_lista.size()>0&&fogo_lista.get(0)!=null){
		  saude_minima_incendio=75;
		  for(int j=0;j<fogo_lista.size();j++){
			atividade.destruir_explosao(6,fogo_lista.get(j),null);
			fogo_lista.remove(j);
		  }
		}
		
		if(aumentou_saude&&fogo_lista.size()>0&&fogo_lista.get(0)!=null){
		  int i=aleatorio.nextInt(fogo_lista.size());
		  atividade.destruir_explosao(6,fogo_lista.get(i),null);
		  fogo_lista.remove(i);
		  }
		  if(aumentou_saude){aumentou_saude=false;}
         
	  
	  
		if(barra_saude_dinamica!=null){
			//Log.e("saude",""+saude);
			saude=(saude>=100)?100:saude;
		    saude_visivel.setText("" + String.format("%.0f",this.saude)+" %");
			barra_saude_dinamica.setWidth((comprimento*saude/100>barra_saude.getWidth())?barra_saude.getHeight():comprimento*saude/100);
			}
		super.onManagedUpdate(pSecondsElapsed);
	}
	
	
   
	void tremer_cidade(float alpha,float w,float phi,float gama,int t){
		//float y=MainActivity.motor.getCamera().getCenterY();
		
		pos_y=y_cam_ini+(float)(Math.exp(-gama*t)*alpha*Math.cos(phi*t-w));
	    MainActivity.motor.getCamera().setCenter(MainActivity.motor.getCamera().getCenterX(),pos_y);
	}
	void atingir_cidade(int i, Meteoro m){
	  if(i==0){//meteoro atinge cidade
	    atingida=true;
	    mx=m.getX();
		my=m.getY();
		saude-=m.getScaleX();
		escala_meteoro=m.getScaleX();
		fator_gama=escala_meteoro/4;
		t1=0;
		t2=Math.round(25*escala_meteoro);
		terremoto=true;
		atividade.vibrar(Math.round(100*escala_meteoro));
	  }else if(i==1){//meteoro atingido
		escala_meteoro=m.getScaleX();
		fator_gama=escala_meteoro;
		t1=0;
		t2=Math.round(12.5f*escala_meteoro);
		terremoto=true;
		atividade.vibrar(Math.round(100*escala_meteoro));
	  }else if(i==2){//bomba nuclear
		saude-=5;
		fator_gama=.5f;
		t1=0;
		t2=200;
		terremoto=true;
		atividade.vibrar(1000);
		//atividade.efeito_explosao
		
	  }
	}
  void incendiar(float x,float y){
	/*try{
	  memoria_usada=((Runtime.getRuntime().freeMemory())/1024);
	}catch(Exception e){e.printStackTrace();memoria_usada=4000;}*/
	//if(memoria_usada>atividade.memoria_livre_minima){
	  
	  RectangleParticleEmitter emissor = new RectangleParticleEmitter(x,y, this.getWidthScaled()/4,1);

	  SpriteParticleSystem fogo=new SpriteParticleSystem(emissor,20,40,100,atividade.fumaca,this.getVertexBufferObjectManager());
	  fogo.addParticleInitializer(new VelocityParticleInitializer<Sprite>(-5,5,0,1));
	  fogo.addParticleInitializer(new AccelerationParticleInitializer<Sprite>(3,20));
	  fogo.addParticleInitializer(new RotationParticleInitializer<Sprite>(0,360));
	  //	fumacas.addParticleInitializer(new BlendFunctionParticleInitializer<Sprite>(GLES20.GL_SRC_ALPHA,GLES20.GL_NONE));	
	  fogo.addParticleInitializer((new ColorParticleInitializer<Sprite>(1,1,0f)));
	  fogo.addParticleModifier(new AlphaParticleModifier<Sprite>(1,5.5f,1,0f));
	  fogo.addParticleInitializer(new ExpireParticleInitializer<Sprite>(6f));
	  fogo.addParticleModifier(new ScaleParticleModifier<Sprite>(0f,5.5f,1f,0.5f));
	  fogo.addParticleModifier(new ColorParticleModifier<Sprite>(0f, 1.5f, 1f, 1f, 1f, 0f, 0f, 0f));
	  fogo.addParticleModifier(new ScaleParticleModifier<Sprite>(5.5f,6f,.5f,0f));
	  fogo.addParticleModifier(new ColorParticleModifier<Sprite>(1.5f, 5.5f, 1f, 0f, 0f, 0f, 0f, 0f));

	  fogo_lista.add(fogo);

	  this.getParent().attachChild(fogo);
//	}
  }
  
}
