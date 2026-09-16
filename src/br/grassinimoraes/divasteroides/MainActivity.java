package br.grassinimoraes.divasteroides;

import com.badlogic.gdx.math.*;
import org.andengine.extension.physics.box2d.*;

import org.andengine.engine.camera.*;
import org.andengine.engine.options.*;
import org.andengine.engine.options.resolutionpolicy.*;
import org.andengine.entity.primitive.*;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.*;
import org.andengine.ui.activity.*;
import org.andengine.entity.util.*;
import android.view.*;
import com.badlogic.gdx.physics.box2d.*;
import android.util.*;
import java.lang.Math;
import org.andengine.opengl.texture.atlas.bitmap.*;
import org.andengine.opengl.texture.*;
import org.andengine.opengl.texture.region.*;
import java.util.*;
import org.andengine.entity.scene.*;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl.IOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.DigitalOnScreenControl;
import org.andengine.engine.camera.hud.*;
import javax.microedition.khronos.opengles.*;
import org.andengine.opengl.font.*;
import org.andengine.entity.text.Text;
import org.andengine.util.adt.map.*;
import org.andengine.engine.*;
import org.andengine.input.touch.*;
import java.io.*;
import org.andengine.entity.*;
import android.app.*;
import org.andengine.entity.modifier.*;
import android.widget.*;
import org.andengine.entity.scene.menu.*;
import org.andengine.entity.scene.menu.item.*;
import org.andengine.entity.scene.menu.item.decorator.*;
import org.andengine.entity.scene.menu.MenuScene.*;
import org.andengine.entity.sprite.*;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.particle.emitter.*;
import org.andengine.entity.particle.*;
import org.andengine.entity.particle.initializer.*;
import org.andengine.entity.particle.modifier.*;
import android.opengl.*;
import org.andengine.util.modifier.*;
import org.andengine.entity.text.*;
import org.andengine.util.adt.align.*;
import org.andengine.engine.handler.timer.*;
import org.andengine.extension.physics.box2d.util.constants.*;
import android.view.animation.*;
import android.animation.*;
import com.badlogic.gdx.physics.box2d.joints.*;
import android.os.*;
import android.content.*;
import org.andengine.util.adt.color.*;
import android.content.SharedPreferences.*;
import java.text.*;

public class MainActivity extends SimpleBaseGameActivity //implements IOnMenuItemClickListener
{
    //private SmothCamera camera;
    static final int CAMERA_WIDTH = 800;
    static final int CAMERA_HEIGHT = 480;
	
	//menu principal
	static final int iniciar1=0;
  static final int opcoes1=1;
  static final int sair1=2;
  
  //voltar ao menu principal
  static final int voltar1=3;
  
  //menu deseja_continuar
  static final int continuar1=4;
  static final int voltar_menu1=5;
  
  //menu fim de jogo
  static final int tentar_denovo1=6;
  
  static final int dificuldade1=7;
  static final int vibracao1=8;
  static final int mira_laser1=9;
  static final int apagar_pontos1=14;
  static final int score1=10;
  
  //menu score - ordenar por:
  static final int data1=11;
  static final int qtde1=12;
  static final int pontos1=13;
  
 
  
	float x=0, y=0;

	private BitmapTextureAtlas fogo_text,efeito_text,fumaca_text,moeda_text,torre_text,cidade_text,meteoro_text,moldura_text,alvo_text,controle_text,canhao_text,projetil_text;

	static TiledTextureRegion moldura,torre1,projetil,canhao1,alvo1,meteoro1;

	Scene cena_raiz;

	static TextureRegion fogo1,efeito,moeda, fumaca,cidade1,controle;
    
	static SmoothCamera camera;
	
	static Font fonte;

	static Text dinheiro;

	static HUD hud;
	
	static  Engine motor;

	static PhysicsWorld mundo;

	Random aleatorio = new Random();
	
	 AnalogOnScreenControl controle_e_cena_pause;

	static AnimatedSprite alvo;

	static Canhao canhao;

	private BotaoDisparo botao;


	float velocidade_x=0;
	
	
	 Cidade cidade_grande;
	
	Projetil disparo;
	
	Meteoro meteoros;
	
	static float angulo;

	MiraLaser miralaser;

	private AnimatedSprite torre_canhao;
	
	int qtde_met_dest=0;

	private float tempo_aleatorio_spawn;
	
  CampoDeForca campo_forca;
  
  
  private ParallelEntityModifier pem;

  long memoria_usada,memoria_livre_minima=3500;

  boolean aumentou_saude;
  
  int fator_pontuacao=10,total_pontos;

  Text score_visivel;

  static boolean bomba_um;

  static boolean bomba_tudo;

  private Text texto_anim;

  public FixtureDef metf;

  Rectangle efeito_rect;

  private ParallelEntityModifier pm;

  private SequenceEntityModifier seqm;

  //Scene opcoes;

  boolean Vibrar=true,mostra_mira_laser=true;

 // Scene deseja_continuar;

  private float y_cam_ini;

  private float x_cam_ini;

  private Text TituloDoJogo;

  private StrokeFont fonte2;

	private StrokeFont fonte3;
	
	ArrayList<String[]> temp=new ArrayList<String[]>();
	
	ArrayList<Text[]> linha_tabela=new ArrayList<Text[]>();

	private SimpleDateFormat dataFormato;

	private BotaoMenu data_chuva_met;

	private BotaoMenu qtde_meteoro;

	private BotaoMenu pontos;

	private Scene cidade;
  
	private ContactListener contactListener()
    {
		ContactListener contactListener = new ContactListener()
		{

			private Meteoro u2;

			private Cidade u1;

			private Projetil u3;

			private Rectangle u4;
			
			@Override
			public void beginContact(Contact contact)
			{
				final Body x1=contact.getFixtureA().getBody();//.getBody();
				final Body x2=contact.getFixtureB().getBody();//.getBody();
					Log.e("Debugger","tempn: " +"contato");//(String)contact.getFixtureA().getBody().getUserData()+"|"+(String) contact.getFixtureB().getBody().getUserData());
				if(x1.getUserData()!=null||x2.getUserData()!=null){
					if((x1.getUserData() instanceof Cidade)&&(x2.getUserData() instanceof Meteoro)){
							u1=(Cidade)x1.getUserData();
							u2=(Meteoro)x2.getUserData();
					    	u2.destruir_meteoro_e_atingir_cidade(u1);
					}else if((x2.getUserData() instanceof Cidade)&&(x1.getUserData() instanceof Meteoro)){
							u1=(Cidade)x2.getUserData();
							u2=(Meteoro)x1.getUserData();
					     	u2.destruir_meteoro_e_atingir_cidade(u1);
					}
					if((x2.getUserData() instanceof Projetil)&&(x1.getUserData() instanceof Meteoro)){
						u3=(Projetil)x2.getUserData();
						u2=(Meteoro)x1.getUserData();
						u2.atingir_meteoro(u3,cidade_grande);
					}else if((x1.getUserData() instanceof Projetil)&&(x2.getUserData() instanceof Meteoro)){
						u3=(Projetil)x1.getUserData();
						u2=(Meteoro)x2.getUserData();
						u2.atingir_meteoro(u3,cidade_grande);
					}
				  
					
							
		
			}



			}

			
			

			public void endContact(Contact contact)
			{

				//	Log.e("Debugger","tempn: " +(String)contact.getFixtureA().getBody().getUserData()+"|"+(String) contact.getFixtureB().getBody().getUserData());
				}


			public void preSolve(Contact contact, Manifold oldManifold)
			{

              
			}

			


			public void postSolve(Contact contact, ContactImpulse impulse)
			{

			}

		};
		return contactListener;
    }
    
	Scene inicial;
	
    
    
    @Override
    public EngineOptions onCreateEngineOptions() {
         camera = new SmoothCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT,1000,1000,0);
        
		EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new FillResolutionPolicy(), camera);
        engineOptions.getTouchOptions().setNeedsMultiTouch(true);
		return engineOptions;
    }
    
    @Override
    protected void onCreateResources() {
		
		
		mEngine.enableVibrator(this);
		
           ITexture fonte_text = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
	  fonte  = FontFactory.createStrokeFromAsset(this.getFontManager(), fonte_text, this.getAssets(), "fnt/ALEAWB__.TTF", 20, true, android.graphics.Color.BLACK, 2, android.graphics.Color.WHITE);
        fonte.load();
		
		ITexture fonte_text2 = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
	  fonte2  = FontFactory.createStrokeFromAsset(this.getFontManager(), fonte_text2, this.getAssets(), "fnt/ALEAWB__.TTF", 50, true, android.graphics.Color.BLUE, 4, android.graphics.Color.WHITE);
	  fonte2.load();
	  
	  ITexture fonte_text3 = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
	  fonte3  = FontFactory.createStrokeFromAsset(this.getFontManager(), fonte_text3, this.getAssets(), "fnt/ALEAWB__.TTF", 40, true, android.graphics.Color.GREEN, 2, android.graphics.Color.YELLOW);
	  fonte3.load();
	  
		
		
		moldura_text=new BitmapTextureAtlas(this.getTextureManager(),32,8,TextureOptions.NEAREST);
		moldura = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(moldura_text, this, "graficos/moldura.png", 0, 0,4,1);
		moldura_text.load();
		
  	    canhao_text=new BitmapTextureAtlas(this.getTextureManager(),128,32,TextureOptions.NEAREST);
		canhao1 = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(canhao_text, this.getAssets(), "graficos/canhao1.png", 0, 0,8,2);
		canhao_text.load();
		
		alvo_text=new BitmapTextureAtlas(this.getTextureManager(),256,32,TextureOptions.NEAREST);
		alvo1 = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(alvo_text, this.getAssets(), "graficos/alvo1.png", 0, 0,8,1);
		alvo_text.load();
//	
		controle_text=new BitmapTextureAtlas(this.getTextureManager(),64,64,TextureOptions.NEAREST);
		controle = BitmapTextureAtlasTextureRegionFactory.createFromAsset(controle_text, this, "graficos/controle.png", 0, 0);
		controle_text.load();
	
		projetil_text=new BitmapTextureAtlas(this.getTextureManager(),128,16,TextureOptions.NEAREST);
		projetil = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(projetil_text, this, "graficos/projetil.png", 0, 0,8,1);
		projetil_text.load();
		
		cidade_text=new BitmapTextureAtlas(this.getTextureManager(),256,32,TextureOptions.NEAREST);
		cidade1 = BitmapTextureAtlasTextureRegionFactory.createFromAsset(cidade_text, this, "graficos/cidade_grande.png", 0, 0);
		cidade_text.load();
		
		meteoro_text=new BitmapTextureAtlas(this.getTextureManager(),128,16,TextureOptions.NEAREST);
		meteoro1 = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(meteoro_text, this.getAssets(), "graficos/meteoro e itens.png", 0, 0,8,1);
		meteoro_text.load();
		
	    torre_text=new BitmapTextureAtlas(this.getTextureManager(),64,32,TextureOptions.NEAREST);
		torre1 = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(torre_text, this.getAssets(), "graficos/torre.png", 0, 0,8,1);
		torre_text.load();
		
		moeda_text=new BitmapTextureAtlas(this.getTextureManager(),16,16,TextureOptions.NEAREST);
		moeda = BitmapTextureAtlasTextureRegionFactory.createFromAsset(moeda_text, this, "graficos/moedas.png", 0, 0);
		moeda_text.load();
		
		fumaca_text=new BitmapTextureAtlas(this.getTextureManager(),16,16,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		fumaca = BitmapTextureAtlasTextureRegionFactory.createFromAsset(fumaca_text, this.getAssets(), "graficos/fumaca1.png", 0, 0);
		fumaca_text.load();
		
		fogo_text=new BitmapTextureAtlas(this.getTextureManager(),32,32,TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		fogo1= BitmapTextureAtlasTextureRegionFactory.createFromAsset(fogo_text, this.getAssets(), "graficos/fogo.png", 0, 0);
		fogo_text.load();
		
		efeito_text=new BitmapTextureAtlas(this.getTextureManager(),32,8,TextureOptions.NEAREST);
		efeito= BitmapTextureAtlasTextureRegionFactory.createFromAsset(efeito_text, this.getAssets(), "graficos/efeito_explosao.png", 0, 0);
		efeito_text.load();
		//efeito_explosao.png
		
		//tthis.mEngine.te
		
		}
    
    @Override
    public Scene onCreateScene(){
		motor=this.mEngine;
       this.mEngine.registerUpdateHandler(new FPSLogger());
	   //this.inicial=this.criar_menu();
	   cena_raiz= new Scene();
	   cena_raiz.setChildScene(this.criar_menu());
	   y_cam_ini=mEngine.getCamera().getCenterY();
	   x_cam_ini=mEngine.getCamera().getCenterX();
	   //carregar_score();
		// TODO: Implement this method
		return cena_raiz;
	}

	
	
 Scene construir_nivel(){
	 
	 cidade=new Scene();
   

	 //**************CONSTRUIR NIVEL***********************?*****************??**?*********
     hud=new HUD();
     camera.setHUD(hud);
	 camera.setCenter(0,0);
	 if(cidade.isIgnoreUpdate()){cidade.setIgnoreUpdate(false);}
	 cidade.setBackground(new Background(.1f, 0.6274f, 0.8784f));
	 mundo= new FixedStepPhysicsWorld(30,1,new Vector2(0, 0), false,8,1);
	 mundo.setContactListener(contactListener());
	 cidade.registerUpdateHandler(mundo);
	 camera.offsetCenter(0,0);
	 
	 //carregar sprite cidade
	 cidade_grande=new Cidade(0,0,cidade1,this,this.getVertexBufferObjectManager());
	 cidade.attachChild(cidade_grande);
	 cidade_grande.setHeight(cidade_grande.getHeight()*CAMERA_WIDTH/cidade_grande.getWidth());
	 cidade_grande.setWidth(CAMERA_WIDTH);
	 cidade_grande.setX(CAMERA_WIDTH/2);
	 //cidade_grande.setY(cidade_grande.getHeight()/2);
	 
	 //criando a fisica da cidade...
	 FixtureDef cidf = PhysicsFactory.createFixtureDef(0,0.2f,0);
     Body cidb=PhysicsFactory.createBoxBody(mundo,cidade_grande,BodyDef.BodyType.StaticBody,cidf);
	 PhysicsConnector fc_cid=new PhysicsConnector(cidade_grande,cidb);
	 mundo.registerPhysicsConnector(fc_cid);
	 cidb.setUserData(cidade_grande);
	 
	 //carregar limites da tels, para evitar que o asteroid saia da tela
	 Rectangle esq=new Rectangle(0,0,5,CAMERA_HEIGHT,this.getVertexBufferObjectManager());
	 Rectangle dir=new Rectangle(0,0,5,CAMERA_HEIGHT,this.getVertexBufferObjectManager());
	 cidade.attachChild(esq);
	 cidade.attachChild(dir);
	 esq.setPosition(-esq.getWidth(),CAMERA_HEIGHT/2);
	 dir.setPosition(CAMERA_WIDTH+dir.getWidth(),CAMERA_HEIGHT);
   Body esqb=PhysicsFactory.createBoxBody(mundo,esq,BodyDef.BodyType.StaticBody,cidf);
   PhysicsConnector fc_esq=new PhysicsConnector(esq,esqb);
   mundo.registerPhysicsConnector(fc_esq);
   Body dirb=PhysicsFactory.createBoxBody(mundo,dir,BodyDef.BodyType.StaticBody,cidf);
   PhysicsConnector fc_dir=new PhysicsConnector(dir,dirb);
   mundo.registerPhysicsConnector(fc_dir);
  
	 
	 //carregar base do camhao
	 
	 
	 

	 torre_canhao=new AnimatedSprite(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2, torre1, this.getVertexBufferObjectManager());
	 torre_canhao.setAnchorCenter(.5f,0);
	 float y_canhao=cidade_grande.getHeightScaled()*3/32;
	 float escala=(CAMERA_HEIGHT/2 -y_canhao)/torre_canhao.getHeight();
     torre_canhao.setPosition(CAMERA_WIDTH/2,y_canhao);
	 cidade.attachChild(torre_canhao);
	 torre_canhao.setScale(escala);
	 torre_canhao.animate(50);
	 
	 //carregar sprite do canhao
	 canhao= new Canhao(CAMERA_WIDTH/2,CAMERA_HEIGHT/2,canhao1,this,this.getVertexBufferObjectManager());
     cidade.attachChild(canhao);
	 canhao.animate (new long[]{50,50,50,50,50,50,50,50},8,15,true);
	 
	 //carregar miralaser invisivel ou visivel
	 miralaser = new MiraLaser(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2, CAMERA_WIDTH, 2, mostra_mira_laser, this.getVertexBufferObjectManager());
	 cidade.attachChild(miralaser);
     miralaser.construir_alvo(alvo1);
	 
	 // carregar efeito bomba nuclear
	 efeito_rect =new Rectangle(-CAMERA_WIDTH,-CAMERA_HEIGHT,CAMERA_WIDTH,CAMERA_HEIGHT,this.getVertexBufferObjectManager());
	 efeito_rect.setVisible(false);
	 hud.attachChild(efeito_rect);
	 
	 
	 
	 
	 //carregar campo de forca
	 campo_forca=new CampoDeForca(-100,-100,CAMERA_WIDTH,5,CAMERA_WIDTH/2,3*cidade_grande.getHeightScaled()/2, this,this.getVertexBufferObjectManager());
	 cidade.attachChild(campo_forca);
     
     metf=PhysicsFactory.createFixtureDef(0, 0.2f, 0);
     tempo_aleatorio_spawn=5f;
	 qtde_met_dest=0;
	 
	 cidade.registerUpdateHandler(new TimerHandler(tempo_aleatorio_spawn, true, new ITimerCallback(){

										  private int valor_int;

										  

										  
										  
	@Override
	 public void onTimePassed(TimerHandler tempo){
		 
	         meteoros = new Meteoro(0,0,qtde_met_dest,meteoro1,mEngine.getVertexBufferObjectManager()); //meteoro.obtainPoolItem(8);
		     meteoros.valor_visivel(fonte);
			
		 	 if(!meteoros.hasParent()){cidade.attachChild(meteoros);cidade.attachChild(meteoros.valor_visivel);}
			 
			 meteoros.setPosition(aleatorio.nextInt(CAMERA_WIDTH),CAMERA_HEIGHT);
			 	 
			 meteoros.construir_corpo(MainActivity.this, CAMERA_WIDTH,CAMERA_HEIGHT);//
		    // Log.e("Debugger","tempn: " + tempo_aleatorio_spawn);;
		     tempo_aleatorio_spawn=(float) (2+Math.log(meteoros.pontuacao)-((meteoros.pontuacao*Math.random()>Math.random())?0:2*Math.random()));
		     Log.e("qtde_met_dest",""+qtde_met_dest);
			 tempo.setTimerSeconds(tempo_aleatorio_spawn);
			}

	}));
	
   //*************CONSTRUIR CONTROLES***************************************

   
	
   controle_e_cena_pause = new AnalogOnScreenControl(100, 100, this.camera, this.controle, this.controle, 0.1f, 200, this.getVertexBufferObjectManager(), new IAnalogOnScreenControlListener() {

	   //private boolean tocado;





	   @Override
	   public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {


		 if(Math.abs(pValueX)>0f&&Math.abs(pValueY)>0f){
		   float	x1=pValueX*50;
		   float y1=pValueY*50;

		   angulo=-(float)( Math.atan2((y1),(x1))*180/Math.PI);
		   canhao.setRotation(angulo);
		   miralaser.setRotation(canhao.getRotation());}

	   }
	   public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl){


	   }

	 }

   );
   this.controle_e_cena_pause.getControlBase().setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
   this.controle_e_cena_pause.getControlBase().setAlpha(.75f);
   this.controle_e_cena_pause.getControlBase().setScale(2f);
   this.controle_e_cena_pause.getControlKnob().setScale(.75f);
   this.controle_e_cena_pause.getControlKnob().setAlpha(.50f);
   this.controle_e_cena_pause.getControlKnob().setBlue(0);
   this.controle_e_cena_pause.getControlKnob().setGreen(0);
   this.controle_e_cena_pause.getControlKnob().setRed(0);
   //	mDigitalOnScreenControl.r
   //	this.mDigitalOnScreenControl.refreshControlKnobPosition();
   this.controle_e_cena_pause.refreshControlKnobPosition();
//	 mDigitalOnScreenControl.setIgnoreUpdate(true);

   hud.setChildScene(controle_e_cena_pause);


   //preencher lista de projeteis

   for(int i=0;i<5;i++){
	 Projetil p=new Projetil(0,0,2,projetil,this.getVertexBufferObjectManager());
	 cidade.attachChild(p);
	 p.construir_corpo(mundo);
	 p.resetar_pos();
	 p.setZIndex(canhao.getZIndex()-1);
	 cidade.sortChildren();
   }

   //botoes tiro
   float x[] = new float[8];
   float y[]=new float[8];
   x[0]=CAMERA_WIDTH*30/32;// this.mDigitalOnScreenControl.getControlBase().getX()+this.mDigitalOnScreenControl.getControlBase().getWidthScaled()*3/4;
   y[0]= CAMERA_HEIGHT*29/32;  //this.mDigitalOnScreenControl.getControlBase().getY();


   for(int i=0; i<8; i++){


	 if(i>0&&i<6){
	   //botao[i].setPosition(botao[i-1].getX()+botao[i-1].getWidthScaled()/2+50,botao[i-1].getY());
	   x[i]=x[i-1];
	   y[i]=y[i-1]-botao.getHeightScaled()-5;
	 }
	 if(i==6){
	   //botoes bomba
	   x[i]=x[i-1]-botao.getWidthScaled()-5;
	   y[i]=y[i-1];//bomba q destroi um so asteroide

	 }else if(i==7){
	   x[i]=x[i-2]-botao.getWidthScaled()-5;
	   y[i]=y[i-3];

	 }

	 botao = new BotaoDisparo(x[i], y[i], moldura,canhao,this, this.getVertexBufferObjectManager());
	 botao.setar_interior(i,fonte);
	 hud.attachChild(botao);
	 hud.registerTouchArea(botao);


   }


   //barra de saude da cidade


   float largura=(y[0]+botao.getHeightScaled()/2)-(y[5]-botao.getHeightScaled()/2);
   float comprimento =.5f*botao.getWidthScaled();
   float x_barra= x[0]-botao.getWidthScaled()/2 -largura/2-10; //(botao[0].getX()+botao[0].getWidthScaled()/2+CAMERA_WIDTH)/2;
   float y_barra=y[0]+botao.getHeightScaled()/2-comprimento/2; //(botao[0].getY()+botao[0].getHeightScaled()/2)-comprimento/2;
   cidade_grande.criar_barra_saude(hud,comprimento,largura,x_barra,y_barra,fonte);

   // carregar scores

   score_visivel = new Text(0, 0, fonte, "Pontos: " + qtde_met_dest , 100, this.getVertexBufferObjectManager());
   score_visivel.setPosition(x_barra-largura-10 + score_visivel.getWidth()/2, y_barra);
   hud.attachChild(score_visivel);
   score_visivel.setHorizontalAlign(HorizontalAlign.CENTER);


   //barra de saude campo de forca

   largura/=2;
   comprimento/=2;
   x_barra= x[0]-botao.getWidthScaled()/2 -largura/2-10; //(botao[0].getX()+botao[0].getWidthScaled()/2+CAMERA_WIDTH)/2;
   y_barra-=2.5f*comprimento-10; //(botao[0].getY()+botao[0].getHeightScaled()/2)-comprimento/2;
   campo_forca.criar_barra_saude(hud,x_barra,y_barra,largura,comprimento,fonte);

   return cidade;
}

  public void vibrar(int tempo)
  {
	if(Vibrar){mEngine.vibrate(tempo);}//colocar nas opcoes uma opcao para desativá-lo
  }

void setar_score(Meteoro m){
                
                total_pontos+=m.pontuacao;
				
				score_visivel.setText("Pontos: "+total_pontos);
				
				//uma animadinha no score
				pem=new ParallelEntityModifier(
				  new SequenceEntityModifier(
					new ScaleModifier(.1f,score_visivel.getScaleX(),2f*score_visivel.getScaleX()),
					new ScaleModifier(.1f,2f*score_visivel.getScaleX(),score_visivel.getScaleX())),
				  new ColorModifier(.5f,(float)Math.random(),1,(float)Math.random(),1,(float)Math.random(),1));
				pem.setAutoUnregisterWhenFinished(true);
				score_visivel.registerEntityModifier(pem);

}

  void texto_animado(String valor, float r,float g, float b, Meteoro m,int tipo){
	texto_anim = new Text(0, 0, fonte, valor, 100, this.getVertexBufferObjectManager());

	//Rectangle rect;

	texto_anim.setPosition((tipo==0)?m:canhao);

	texto_anim.setHorizontalAlign(HorizontalAlign.CENTER);

	cena_raiz.getChildScene().attachChild(texto_anim);

	texto_anim.setColor(r,g,b);

	float sc_temp=texto_anim.getScaleX();
	float y_temp=texto_anim.getY();
     if(tipo==0){
	 MoveYModifier mm=  new MoveYModifier(1,y_temp,y_temp+100){
	  @Override
	  protected void onModifierFinished(final IEntity t){
		mEngine.runOnUpdateThread(new Runnable(){


			public void run(){
		t.clearUpdateHandlers();
		t.clearEntityModifiers();
		t.detachSelf();
		
		}});
		//TODO Auto-generated method stub
		super.onModifierFinished(t);
		
	  }
	};

	  pm=new ParallelEntityModifier(
	  new ScaleModifier(1,sc_temp,1.5f*sc_temp),
	  new AlphaModifier(1,1,0),mm);
	   pm.setAutoUnregisterWhenFinished(true);
	   texto_anim.registerEntityModifier(pm);
}else{
    

  seqm=new SequenceEntityModifier(new ParallelEntityModifier(new MoveYModifier(.25f,canhao.getY(),1.5f*canhao.getY()), new ScaleModifier(1,0,2),
	new AlphaModifier(.5f,0,.5f)),new DelayModifier(2),new AlphaModifier(.5f,.5f,0)){
	
	@Override
	protected void onModifierFinished(final IEntity t){
	  mEngine.runOnUpdateThread(new Runnable(){
		  public void run(){
			t.clearUpdateHandlers();
			t.clearEntityModifiers();
			t.detachSelf();

		  }});
	  //TODO Auto-generated method stub
	  super.onModifierFinished(t);

	}
  };
  
  seqm.setAutoUnregisterWhenFinished(true);
  texto_anim.registerEntityModifier(seqm);
	
}

	
  }

 void destruir_explosao(float t, final ParticleSystem p,final Sprite s){

		cidade.registerUpdateHandler(new TimerHandler(t, false, new ITimerCallback(){


										  @Override
										  public void onTimePassed(final TimerHandler tempo){
											  mEngine.runOnUpdateThread(new Runnable(){
													  public void run(){
														  try{
															  
															if(s!=null&&s.hasParent()){s.detachSelf();}
															  p.setParticlesSpawnEnabled(false);
															  p.detachSelf();
															  cidade.sortChildren();
															  cidade.unregisterUpdateHandler(tempo);
														  }catch(Exception e){
															  Log.e("Erro",e.toString());
														  }
													  }});

										  }

									  }));

	}
	


  protected Scene fim_de_jogo(){

	final Scene fim_jogo=new MenuScene(this.camera);
   
	Rectangle pano = new Rectangle(CAMERA_WIDTH/2,CAMERA_HEIGHT/2,CAMERA_WIDTH,CAMERA_HEIGHT,this.getVertexBufferObjectManager());
	pano.setColor(1,0,0);
	pano.setAlpha(.25f);
	  Text fim_jogo_visivel = new Text(CAMERA_WIDTH /2, .75f * CAMERA_HEIGHT, fonte2, "A CIDADE FOI DESTRUIDA!", 100, this.getVertexBufferObjectManager());
	  BotaoMenu voltar_menu=new BotaoMenu(voltar_menu1,this,CAMERA_WIDTH/2,.5f*CAMERA_HEIGHT,fonte3,"VOLTAR AO MENU",100,this.getVertexBufferObjectManager());
	  BotaoMenu reiniciar=new BotaoMenu( tentar_denovo1,this,CAMERA_WIDTH/2,.375f*CAMERA_HEIGHT,fonte3,"TENTAR DENOVO",100,this.getVertexBufferObjectManager());
	  BotaoMenu sair=new BotaoMenu(sair1,this,CAMERA_WIDTH/2,.25f*CAMERA_HEIGHT,fonte3,"SAIR DO JOGO",100,this.getVertexBufferObjectManager());
	
	fim_jogo.setBackgroundEnabled(false);
    
	fim_jogo.attachChild(pano);
	fim_jogo.attachChild(fim_jogo_visivel);
	fim_jogo.attachChild(voltar_menu);
	fim_jogo.attachChild(reiniciar);
	fim_jogo.attachChild(sair);
	fim_jogo.registerTouchArea(voltar_menu);
	fim_jogo.registerTouchArea(reiniciar);
	fim_jogo.registerTouchArea(sair);
	
	
	return fim_jogo;
  }
protected Scene criar_menu(){

	final Scene menu_inicial=new Scene();
 
  TituloDoJogo = new Text(CAMERA_WIDTH /2, .875f * CAMERA_HEIGHT, fonte2, "METEOROS NUMERIOS", 100, this.getVertexBufferObjectManager());
  
  // LoopEntityModifier lm=new LoopEntityModifier(new ColorModifier(5,1,0,1,1,0,0));
  
  //TituloDoJogo.setScale(10);
//  TituloDoJogo.registerEntityModifier(lm);
  menu_inicial.attachChild(TituloDoJogo);
  
  BotaoMenu iniciar=new BotaoMenu(iniciar1,this,CAMERA_WIDTH/2,.625f*CAMERA_HEIGHT,fonte3,"INICIAR",100,this.getVertexBufferObjectManager());
  BotaoMenu pontuacao=new BotaoMenu(score1,this,CAMERA_WIDTH/2,.5f*CAMERA_HEIGHT,fonte3,"VER PONTUACAO",100,this.getVertexBufferObjectManager());
  BotaoMenu opcoes=new BotaoMenu(opcoes1,this,CAMERA_WIDTH/2,.375f*CAMERA_HEIGHT,fonte3,"OPCOES",100,this.getVertexBufferObjectManager());
  BotaoMenu sair=new BotaoMenu(sair1,this,CAMERA_WIDTH/2,.25f*CAMERA_HEIGHT,fonte3,"SAIR",100,this.getVertexBufferObjectManager());
 
 
  menu_inicial.attachChild(iniciar);
  menu_inicial.attachChild(pontuacao);
  menu_inicial.attachChild(opcoes);
  menu_inicial.attachChild(sair);
  menu_inicial.registerTouchArea(iniciar);
  menu_inicial.registerTouchArea(pontuacao);
  menu_inicial.registerTouchArea(opcoes);
  menu_inicial.registerTouchArea(sair);
  
     menu_inicial.setBackground(new Background(0, 0, 0));
	
	return menu_inicial;
}

  protected Scene criar_menu_deseja_contimuar(){
	final Scene deseja_continuar1=new Scene();
	
	
	
	Rectangle pano = new Rectangle(CAMERA_WIDTH/2,CAMERA_HEIGHT/2,CAMERA_WIDTH,CAMERA_HEIGHT,this.getVertexBufferObjectManager());
	pano.setColor(0,0,0);
	pano.setAlpha(.25f);
	deseja_continuar1.attachChild(pano);
	
	
	BotaoMenu voltar_menu_inicial=new BotaoMenu(voltar_menu1,this,CAMERA_WIDTH/2,.625f*CAMERA_HEIGHT,fonte3,"VOLTAR AO MENU",100,this.getVertexBufferObjectManager());
	BotaoMenu continuar=new BotaoMenu(continuar1,this,CAMERA_WIDTH/2,.5f*CAMERA_HEIGHT,fonte3,"CONTINUAR",100,this.getVertexBufferObjectManager());
	BotaoMenu Vibracao=new BotaoMenu(vibracao1,this,CAMERA_WIDTH/2,.375f*CAMERA_HEIGHT,fonte3,"VIBRACAO: "+((Vibrar)?"Ligada":"Desligada"),100,this.getVertexBufferObjectManager());
	BotaoMenu sair=new BotaoMenu(sair1,this,CAMERA_WIDTH/2,.25f*CAMERA_HEIGHT,fonte3,"SAIR DO JOGO",100,this.getVertexBufferObjectManager());
	
	deseja_continuar1.registerTouchArea(voltar_menu_inicial);
	deseja_continuar1.registerTouchArea(continuar);
	deseja_continuar1.registerTouchArea(Vibracao);
	deseja_continuar1.registerTouchArea(sair);
	
	deseja_continuar1.attachChild(voltar_menu_inicial);
	deseja_continuar1.attachChild(continuar);
	deseja_continuar1.attachChild(Vibracao);
	deseja_continuar1.attachChild(sair);
	
	
	Text jogo_pausado = new Text(CAMERA_WIDTH/2, .75f * CAMERA_HEIGHT, fonte2, "JOGO PAUSADO", 100, this.getVertexBufferObjectManager());
	deseja_continuar1.attachChild(jogo_pausado);
	
	
	deseja_continuar1.setBackgroundEnabled(false);

//	deseja_continuar.setBackground(new Background(.5f, .5f, 0.5f));
//	deseja_continuar.setOnMenuItemClickListener(this);
	
//	deseja_continuar1.setBackground(new Background(0, 0, 0));
	
	return deseja_continuar1;
	}



  protected Scene criar_menu_opcoes(){

	final Scene menu_opcoes=new Scene();
    
	
	BotaoMenu Mira_laser =new BotaoMenu(mira_laser1,this,CAMERA_WIDTH/2,.625f*CAMERA_HEIGHT,fonte3,"MIRA LASER: Desligada",100,this.getVertexBufferObjectManager());
	BotaoMenu Dificuldade =new BotaoMenu(dificuldade1,this,CAMERA_WIDTH/2,.75f*CAMERA_HEIGHT,fonte3,"DIFICULDADE: Muito Facil",100,this.getVertexBufferObjectManager());
	BotaoMenu Vibracao =new BotaoMenu(vibracao1,this,CAMERA_WIDTH/2,.5f*CAMERA_HEIGHT,fonte3,"VIBRACAO: Ligada",100,this.getVertexBufferObjectManager());
	BotaoMenu apagar_score =new BotaoMenu(apagar_pontos1,this,CAMERA_WIDTH/2,.375f*CAMERA_HEIGHT,fonte3,"APAGAR SCORE",100,this.getVertexBufferObjectManager());
	BotaoMenu voltar_menu =new BotaoMenu(voltar1,this,CAMERA_WIDTH/2,.25f*CAMERA_HEIGHT,fonte3,"Voltar",100,this.getVertexBufferObjectManager());
	
	menu_opcoes.registerTouchArea(Mira_laser);
	menu_opcoes.attachChild(Mira_laser);
	menu_opcoes.registerTouchArea(Dificuldade);
	menu_opcoes.attachChild(Dificuldade);
	menu_opcoes.registerTouchArea(Vibracao);
	menu_opcoes.attachChild(Vibracao);
	menu_opcoes.registerTouchArea(apagar_score);
	menu_opcoes.attachChild(apagar_score);
	menu_opcoes.registerTouchArea(voltar_menu);
	menu_opcoes.attachChild(voltar_menu);
	
	  Text opcoes = new Text(CAMERA_WIDTH/2, .875f * CAMERA_HEIGHT, fonte3, "OPCOES", 100, this.getVertexBufferObjectManager());
	  menu_opcoes.attachChild(opcoes);
	  
	
    menu_opcoes.setBackground(new Background(.5f, .5f, .5f));

	return menu_opcoes;
  }

  protected Scene criar_menu_pontos(){

	final Scene menu_pontos=new Scene();
	
    

	data_chuva_met =new BotaoMenu(data1,this,10,.75f*CAMERA_HEIGHT,fonte,"DATA DA CHUVA DE METOROS",100,this.getVertexBufferObjectManager());
	qtde_meteoro =new BotaoMenu(qtde1,this,0,.75f*CAMERA_HEIGHT,fonte,"METEOROS DESTRUIDOS",100,this.getVertexBufferObjectManager());
	pontos =new BotaoMenu(pontos1,this,0,.75f*CAMERA_HEIGHT,fonte,"PONTOS",100,this.getVertexBufferObjectManager());
	BotaoMenu voltar_menu =new BotaoMenu(voltar1,this,CAMERA_WIDTH/2,.125f*CAMERA_HEIGHT/2,fonte,"Voltar",100,this.getVertexBufferObjectManager());
	
	float intervalo=(CAMERA_WIDTH-(data_chuva_met.getWidthScaled()+qtde_meteoro.getWidthScaled()+pontos.getWidthScaled()))/4;
	data_chuva_met.setX(intervalo+data_chuva_met.getWidthScaled()/2);
	pontos.setX(data_chuva_met.getX()+data_chuva_met.getWidthScaled()/2+pontos.getWidthScaled()/2+intervalo);
	qtde_meteoro.setX(pontos.getX()+pontos.getWidthScaled()/2+qtde_meteoro.getWidthScaled()/2+intervalo);
	
	menu_pontos.registerTouchArea(data_chuva_met);
	menu_pontos.attachChild(data_chuva_met);
	menu_pontos.registerTouchArea(qtde_meteoro);
	menu_pontos.attachChild(qtde_meteoro);
	menu_pontos.registerTouchArea(pontos);
	menu_pontos.attachChild(pontos);
	menu_pontos.registerTouchArea(voltar_menu);
	menu_pontos.attachChild(voltar_menu);
	
	  Text tela_pontuacao = new Text(CAMERA_WIDTH/2, .875f * CAMERA_HEIGHT, fonte2, "PONTUACAO", 100, this.getVertexBufferObjectManager());
	  menu_pontos.attachChild(tela_pontuacao);
	  
	carregar_score(menu_pontos);
		menu_pontos.setBackground(new Background(.5f, .5f, .5f));

	return menu_pontos;
  }
  
@Override
public boolean onKeyUp(int keyCode, KeyEvent event)
{
	if(keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_BACK){
		if(cena_raiz.getChildScene()==cidade){
			
		//  if(this.deseja_continuar==null){
			 // this.deseja_continuar=this.criar_menu_deseja_contimuar();
			  //}
		  
		  cidade.setIgnoreUpdate(true);
		  controle_e_cena_pause.setChildScene(this.criar_menu_deseja_contimuar(),false,true,true);
		 
		  
		}
	  
	  
	  
		return true;
	}
	// TODO: Implement this method
	return super.onKeyUp(keyCode, event);
}
void salvar_score(){
	if(total_pontos>0&&qtde_met_dest>0){
		SharedPreferences pontuacao = this.getSharedPreferences("pontuacao", Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = pontuacao.edit();
	
	   dataFormato = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	   Calendar cal = Calendar.getInstance();
       int teste_burlar_pontos=total_pontos%(qtde_met_dest+1);//para evitae q um usuario root nao altere o score
	   editor.putString( dataFormato.format(cal.getTime()),dataFormato.format(cal.getTime()) +"|"+total_pontos+"|" +qtde_met_dest+"|"+teste_burlar_pontos);
	   editor.commit();
	}
}
 void carregar_score(Scene m){
	 try{
		 
	 SharedPreferences pontuacao = this.getSharedPreferences("pontuacao", Context.MODE_PRIVATE);
	 
	 Map<String,?> chaves=pontuacao.getAll();
	 Iterator it =chaves.entrySet().iterator();
	 float y=.625f*CAMERA_HEIGHT;
	 temp.removeAll(temp);
	 linha_tabela.removeAll(linha_tabela);
	 while(it.hasNext()){
		 Map.Entry entrada=(Map.Entry) it.next();
		 String[] teste=((String)entrada.getValue()).split("\\|");
	     temp.add(teste);
		 Text data_chuva_met_visivel=new Text(data_chuva_met.getX(),y, fonte, "", 100, this.getVertexBufferObjectManager());
		 Text qtde_meteoro_visivel=new Text(qtde_meteoro.getX(),y, fonte, "", 100, this.getVertexBufferObjectManager());
		 Text pontos_visivel=new Text(pontos.getX(),y, fonte, "", 100, this.getVertexBufferObjectManager());
		 linha_tabela.add(new Text[]{
		 data_chuva_met_visivel,
		 pontos_visivel,
		 qtde_meteoro_visivel
		 });
		 m.attachChild(data_chuva_met_visivel);
		 m.attachChild(qtde_meteoro_visivel);
		 m.attachChild(pontos_visivel);
		 y-=.125*CAMERA_HEIGHT/2;
	  }
	 ordenar_score(1);
	 
	 }catch(Exception e){e.printStackTrace();}
 }

 void ordenar_score(final int p)
 {
	 Collections.sort(temp, new Comparator<String[]>(){

			 private Date valor1;

			 private Date valor2;
			 public int compare(String[] valor, String[] outro_valor){
                 switch(p){
					 case 0:
				        try
				 			{
					 			valor1= dataFormato.parse(valor[0]);
								 valor2 = dataFormato.parse(outro_valor[0]);

				 			 }
				 				catch (ParseException e)
				 				{}
				 			return - valor1.compareTo(valor2);
				 			
					  case 1:
						  return -valor[1].compareTo(outro_valor[1]);
			      }
			      return -valor[2].compareTo(outro_valor[2]);
			 }

		 });
		 
	for(int i=0;i<temp.size();i++){
		
		linha_tabela.get(i)[0].setText(temp.get(i)[0]);
		linha_tabela.get(i)[1].setText(temp.get(i)[1]);
		linha_tabela.get(i)[2].setText(temp.get(i)[2]);
		if(linha_tabela.get(i)[0].getY()<.125f*CAMERA_HEIGHT){break;}//limita a qtde de scores
		
		
	}	 //criar no botaomenu a ostras ordenacoes
		 //criar um sharedpreferences para guardar as opcoes
	//basta fazer o for e linha_tabela.get(i)[0]=temp.get(i)[0]
	 
	 // TODO: Implement this method
 }
 
 
		

void sair_jogo(){
  salvar_score();
  resetar_cena();
  System.exit(0);
  
}
void resetar_cena()
	{
		if (cidade!=null&&cena_raiz.getChildScene()==cidade)
		{
			try
			{
				this.camera.reset();
				cidade.reset();
				cidade.detachChildren();
				cidade.clearEntityModifiers();
				cidade.clearTouchAreas();
				cidade.clearUpdateHandlers();
				cidade.clearChildScene();
				hud.detachChildren();
				hud.clearEntityModifiers();
				hud.clearUpdateHandlers();
				hud.clearTouchAreas();
				hud.clearChildScene();
				hud = null;
				if (mundo != null)
				{
					mundo.clearForces();
					mundo.clearPhysicsConnectors();
					mundo.reset();
					mundo.dispose();
					mundo = null;
				}
				System.gc();

				BotaoDisparo.resetar_variaveis_estaticas();
				Meteoro.resetar_variaveis_estaticas();
				Projetil.resetar_variaveis_estaticas();
				bomba_um = bomba_tudo = false;
				angulo = 0;
				cidade.setIgnoreUpdate(false);
				total_pontos = 0;
				qtde_met_dest = 0;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
