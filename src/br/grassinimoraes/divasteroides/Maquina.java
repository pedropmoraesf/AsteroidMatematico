//package br.grassimimoraes;
//import org.andengine.entity.sprite.*;
//import org.andengine.opengl.texture.region.*;
//import org.andengine.opengl.texture.atlas.bitmap.*;
//import android.content.*;
//import org.andengine.opengl.vbo.*;
//import org.andengine.input.touch.*;
//import org.andengine.opengl.texture.*;
//import org.andengine.entity.primitive.*;
//import org.andengine..scene.*;
//import org.andengine.entity.scene.menu.item.*;animate (new long[]{tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_},tipo_maq+1,tipo_maq+6,true);}
		
//import org.andengine.engine.*;
//import org.andengine.entity.particle.*;
//import org.andengine.entity.particle.emitter.PointParticleEmitter;
//import org.andengine.entity.particle.initializer.*;
//import android.opengl.*;
//import org.andengine.entity.particle.modifier.*;
//import com.badlogic.gdx.physics.box2d.*;
//import org.andengine.extension.physics.box2d.*;
//import android.util.*;
//import org.andengine.opengl.font.*;
//import org.andengine.entity.text.*;
//import org.andengine.engine.camera.hud.*;
//import org.andengine.entity.*;
//import java.util.*;
//
//
//public class Maquina extends AnimatedSprite
//{
//	TiledTextureRegion chave,mesmo,relogio,ligades_text,ligades,moeda;
//	int tipo_maq=1,tipo_maq_def=0,valor_operacao=5,clock=0,t=0;
//	Boolean onoff=true;
//	
//	CustoAnimado custosElucros;
//	
//     Byte produzindo=0,ligado=0,vendido=0,direcao_lancamento=0;
//	 float saude_maq=100;
//	double confiabilidade_maq=.98,valor_maq=0;
//	 
//	
//	
//	AnimatedSprite manutencao, foto_maq,  botao_ligades,valor_mercado,lead_time;
//
//	VertexBufferObjectManager vert;
//    
//	Scene cena;
//	
//	Rectangle rect,espaco_maq,barra_saude_maq,barra_saude_maq_pai,tempo_producao_area,operador_area,barra_producao_pai;
//
//	Engine motor;
//
//	TextureRegion fumaca,mais_menos;
//
//	SpriteParticleSystem fumacas;
//
//	PointParticleEmitter ponto;
//
//    float alpha;
//	
//	long tempo_producao_=100;
//
//	PhysicsWorld mundo;
//    
//	String[] dados_maq=new String[8];
//	
//	String texto_maq,titulo,nome_maq_temp;
//
//	Body maqb;
//
//    Font fonte;
//
//	Text confiabilidade_maq_visivel,tempo_prod_visivel,valor_operacao_visivel;
//
//	Sprite botao_mais_menos_operador;
//
//	Text valor_maq_visivel;
//
//	PhysicsConnector fc;
//
//	HUD hud;
//	
//	Integer tile_empresario_temp=0;
//
//	TextureRegion cimabaixo;
//
//	Sprite botao_cima_baixo;
//
//	Maquina este;
//
//	Font fonte_anim;
//
//	ParticleSystem pedacos;
//
//	ParticleSystem dinheiro;
//	
//	Random r=new Random();
//
//	ParticleSystem numeros;
//
//	Rectangle maquininha;
//	
//	//criar espaco proibido para q o jogador nao ponha uma maquina sob a outra
//    
//	
//	
//	Maquina(float x,float y,int tm, TiledTextureRegion text, VertexBufferObjectManager vert){
//	super(x,y,text,vert);
//	tm=(tm>4)?4:tm;
//    this.tipo_maq_def=tm;
////	this.setScale(2f);
//	this.tipo_maq=8*tm-8;
//	this.setCurrentTileIndex(tipo_maq);
//	this.cena=MainActivity.fabrica;
//	this.cena.registerTouchArea(this);
//	this.motor=MainActivity.motor;
//	this.fumaca=MainActivity.fumaca;
//	this.ligades=MainActivity.ligades;
//	this.mundo=MainActivity.mundo;
//	this.fonte=MainActivity.fonte;
//	this.fonte_anim=MainActivity.fonte_anim;
//	this.mais_menos=MainActivity.mais_menos;
//	this.moeda=MainActivity.moeda;
//	this.hud=MainActivity.hud;
//	this.relogio=MainActivity.relogio;
//	this.mesmo=text;
//	this.chave=MainActivity.chave;
//	this.cimabaixo=MainActivity.cimabaixo;
//	this.setScale(4f);
//	
//	
//	este=this;
//	
//	
//		switch (tm){
//			case 1:
//				texto_maq="somar:";
//				valor_maq=500;
//				titulo = "Somar";
//				
//				break;
//			case 2:
//				texto_maq="subtrair:";
//				valor_maq=550;
//				titulo="Subtrair";
//				break;
//			case 3:
//				texto_maq="multiplicar:";
//				valor_maq=750;
//				titulo="Multiplicar";
//				break;
//			case 4:
//				texto_maq="dividir:";
//				valor_maq=1000;
//				titulo="Dividir";
//				break;
//
//		}
//		
//	
//		final Text nome_maq=new Text(0,0,fonte,"Maquina de "+titulo,100,this.getVertexBufferObjectManager());
//		nome_maq_temp=(String) nome_maq.getText();
//		nome_maq.setScale(.75f);
//		nome_maq.setAnchorCenter(0,0);
//		
//	    ponto=new PointParticleEmitter(this.getX(),this.getY());
// 
//		fumacas=new SpriteParticleSystem(ponto,8,12,70,fumaca,this.getVertexBufferObjectManager());
//		fumacas.addParticleInitializer(new VelocityParticleInitializer<Sprite>(0,5,0,2));
//		fumacas.addParticleInitializer(new AccelerationParticleInitializer<Sprite>(15,50));
//		fumacas.addParticleInitializer(new RotationParticleInitializer<Sprite>(0,360));
//		//	fumacas.addParticleInitializer(new BlendFunctionParticleInitializer<Sprite>(GLES20.GL_SRC_ALPHA,GLES20.GL_NONE));	
//	    fumacas.addParticleInitializer((new ColorParticleInitializer<Sprite>(0,0,0)));
//     	fumacas.addParticleModifier(new AlphaParticleModifier<Sprite>(1,0,2.5f,6.5f));
//		fumacas.addParticleInitializer(new ExpireParticleInitializer<Sprite>(3.5f));
//		fumacas.addParticleModifier(new ScaleParticleModifier<Sprite>(0f,4.5f,.6f,0f));
//		fumacas.addParticleModifier(new ColorParticleModifier<Sprite>(0,3,0f,.5f,0f,.5f,0f,.5f));
//		
//		fumacas.setParticlesSpawnEnabled(false);
//		cena.attachChild(fumacas);
//	
//	
//		float largura=MainActivity.hud.getCamera().getWidth()/2;
//		float altura=MainActivity.hud.getCamera().getHeight()/4;
//		
//
//	rect = new Rectangle(0,0,largura,altura,this.getVertexBufferObjectManager()){
//		
//		public void onDetached(){
//			MainActivity.flag=0;
//			espaco_maq.setColor(1,1,1);
//    		hud.unregisterTouchArea(botao_ligades);
//			hud.unregisterTouchArea(botao_mais_menos_operador);
//	     //   hud.unregisterTouchArea(botao_cima_baixo);
//		    hud.unregisterTouchArea(valor_mercado);
//		    hud.unregisterTouchArea(hud);
//		}
//		
//		public void onAttached(){
//			MainActivity.flag=1;
//			espaco_maq.setColor(0,0,0);
//			hud.registerTouchArea(botao_ligades);
//			hud.registerTouchArea(botao_mais_menos_operador);
//		//	hud.registerTouchArea(botao_cima_baixo);
//			hud.registerTouchArea(valor_mercado);
//			hud.registerTouchArea(hud);
//		}
//			
//
//	};
//	
//	/*	hud.setOnAreaTouchListener(new IOnAreaTouchListener() {
//				@Override
//				public boolean onAreaTouchEvent(Scene pScene,TouchEvent pSceneTouchEvent) {
//					
//					
//					return false;
//				}
//			});*/
//	
//	rect.setAlpha(.2f);
//	rect.setTag(10);
//	
//	
//	
//	
//	
//	botao_ligades=new AnimatedSprite(0,0,ligades,this.getVertexBufferObjectManager()){
//@Override
//public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
//{
//	if(pSceneTouchEvent.isActionUp()&&ligado<3){
//
//		if(this.getCurrentTileIndex()==1){
//			ligado=1;
//			t=0;
//			this.setCurrentTileIndex (0);
//					valor_mercado.setAlpha(.25f);
//			valor_mercado.stopAnimation(0);
//		//	botao_mais_menos_operador.setAlpha(.25f);
//		//	botao_mais_menos_tempo.setAlpha(.25f);
//			motor.runOnUpdateThread(new Runnable(){
//					public void run(){
//						cena.unregisterTouchArea(valor_mercado);
//					}});
//			}else{
//				ligado=0;
//				this.setCurrentTileIndex (1);
//				valor_mercado.setAlpha(1);
//				valor_mercado.animate(50);
//				botao_mais_menos_operador.setAlpha(1);
//			//	botao_mais_menos_tempo.setAlpha(1);
//				cena.registerTouchArea(valor_mercado);
//			}
//			
//		
//	}
//	// TODO: Implement this method
//	return true;
//}
//	};
//	
//	tempo_prod_visivel=new Text(0,0,fonte,""+tempo_producao_,100,this.getVertexBufferObjectManager());
//	tempo_prod_visivel.setScale(.5f);
//	
//	
//	
//	Text tempo_producao_label=new Text(0,0,fonte,"Tempo de Producao:",100,this.getVertexBufferObjectManager());
//    tempo_producao_label.setScale(.5f);
//	tempo_producao_label.setAnchorCenter(0,.5f);
//	tempo_prod_visivel.setPosition(tempo_producao_label.getX()+tempo_producao_label.getWidth()/2f+tempo_prod_visivel.getWidth()/2,tempo_producao_label.getHeightScaled());
//	
//	tempo_producao_area=new Rectangle(0,0,rect.getWidth()/2,rect.getHeight()/4,this.getVertexBufferObjectManager());
//	tempo_producao_area.setAnchorCenter(0,.5f);
//	tempo_producao_area.attachChild(tempo_producao_label);
//	
//	tempo_producao_label.setPosition(0,tempo_producao_area.getHeight()/2);
//	
//	rect.attachChild(tempo_producao_area);
//	tempo_producao_area.setAnchorCenter(0,1);
//	tempo_producao_area.setPosition(0,rect.getHeight()-botao_ligades.getHeightScaled()-tempo_producao_area.getHeight()/2);
//	tempo_producao_area.attachChild(tempo_prod_visivel);
//	
//	botao_mais_menos_operador = new Sprite(0,0,mais_menos,this.getVertexBufferObjectManager()){
//			@Override
//			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
//			{
//				if(pSceneTouchEvent.isActionDown()&&pTouchAreaLocalX>this.getWidthScaled()/2&&confiabilidade_maq>=.20&&(ligado==1||ligado==7||ligado==0)){
//				
//					valor_operacao+=1;
//					tempo_producao_+=20;
//					confiabilidade_maq-=.007f;
//			
//				}else if(pSceneTouchEvent.isActionDown()&&pTouchAreaLocalX<this.getWidthScaled()/2&&(ligado==1||ligado==7||ligado==0)){
//					if(valor_operacao<=1){valor_operacao=1;}else{
//					valor_operacao-=1;
//					tempo_producao_-=(tempo_producao_<=20)?(0):(20);
//					}
//					confiabilidade_maq+=(confiabilidade_maq>=.99)?(0):(.007f);
//				    
//					}
//
//				// TODO: Implement this method
//				return true;
//			}
//
//		};
//
//		botao_mais_menos_operador.setWidth(botao_mais_menos_operador.getHeight()*6);
//		valor_operacao_visivel=new Text(0,0,fonte,""+valor_operacao,100,this.getVertexBufferObjectManager());
//		valor_operacao_visivel.setScale(.5f);
//		botao_mais_menos_operador.attachChild(valor_operacao_visivel);
//		valor_operacao_visivel.setPosition(botao_mais_menos_operador.getWidthScaled()/2,botao_mais_menos_operador.getHeightScaled()/2);
//
//		final Text valor_operador_label=new Text(0,0,fonte,"Máximo a "+texto_maq,100,this.getVertexBufferObjectManager());
//		valor_operador_label.setScale(.5f);
//		valor_operador_label.setAnchorCenter(0,.5f);
//
//
//		operador_area=new Rectangle(0,0,rect.getWidth()/2,rect.getHeight()/4,this.getVertexBufferObjectManager());
//		operador_area.setAnchorCenter(0,.5f);
//		operador_area.attachChild(botao_mais_menos_operador);
//		operador_area.attachChild(valor_operador_label);
//
//		valor_operador_label.setPosition(0,operador_area.getHeight()/2);
//		botao_mais_menos_operador.setAnchorCenter(0,.5f);
//		botao_mais_menos_operador.setPosition(valor_operador_label.getX()+valor_operador_label.getWidth()/1.75f,operador_area.getHeight()/2);
//
//		rect.attachChild(operador_area);
//		operador_area.setAnchorCenter(0,1);
//		operador_area.setPosition(0,operador_area.getHeight()/1.2f +tempo_producao_area.getHeight()/2);
//		
//		operador_area.setAlpha(0);
//		tempo_producao_area.setAlpha(0);
//	
//	
//	
//	
//	botao_ligades.setCurrentTileIndex(1);
//	botao_ligades.setAnchorCenter(0,1);
//	botao_ligades.setPosition(0,rect.getHeight());
//	botao_ligades.setSize(rect.getHeight()/4,rect.getHeight()/4);
//	rect.attachChild(botao_ligades);
////	cena.attachChild(espaco_maq);
//	alpha=0f;
//	
//	
//	barra_saude_maq_pai=new Rectangle(0,0,saude_maq,rect.getHeight()/4,this.getVertexBufferObjectManager());
//	barra_saude_maq_pai.setAnchorCenter(0,1);
//	barra_saude_maq_pai.setColor(1,0,0);
//	
//	barra_saude_maq=new Rectangle(0,0,saude_maq,barra_saude_maq_pai.getHeight(),this.getVertexBufferObjectManager());
//	barra_saude_maq.setAnchorCenter(0,0);
//	barra_saude_maq.setColor(0,1,0);
//	
//	barra_saude_maq_pai.attachChild(barra_saude_maq);
//	rect.attachChild(barra_saude_maq_pai);
//	
//	barra_saude_maq_pai.setPosition(botao_ligades.getX()+botao_ligades.getWidthScaled()+20,rect.getHeight());
//	
//	confiabilidade_maq_visivel=	new Text(0,0,fonte,"Conf: "+String.format("%.2f",confiabilidade_maq),100,this.getVertexBufferObjectManager());
//	confiabilidade_maq_visivel.setScale(.5f);
//	//confiabilidade_maq_visivel.setAnchorCenter(0,0);
//	rect.attachChild(confiabilidade_maq_visivel);
//	confiabilidade_maq_visivel.setPosition(barra_saude_maq_pai.getX()+barra_saude_maq_pai.getWidthScaled()+confiabilidade_maq_visivel.getWidthScaled()/2+10,rect.getHeight()-confiabilidade_maq_visivel.getHeightScaled());
//    
//	rect.attachChild(nome_maq);
//	nome_maq.setPosition(0,rect.getHeight());
//	
//	
//		
//	
//	valor_mercado =new AnimatedSprite(0,0,moeda,this.getVertexBufferObjectManager()){
//		
//			@Override
//			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
//			{
//				if(pSceneTouchEvent.isActionUp()&&ligado==0){
//				
//					Empresario.fortuna+=(float)valor_maq;
//					Empresario.espacos.remove(espaco_maq);
//					Empresario.proibidos.remove(espaco_maq);
//					
//					
//					custosElucros=new CustoAnimado(este.getX(),este.getY(),fonte_anim,String.format("%.2f",valor_maq) ,this.getVertexBufferObjectManager(),0);
//					custosElucros.setScale(.5f);
//					cena.attachChild(custosElucros);
//					
//					motor.runOnUpdateThread(new Runnable(){
//							public void run(){
//						
//								este.destruir_corpo();
//								hud.detachChild(rect);
//								cena.detachChild(espaco_maq);
//								cena.unregisterTouchArea(este);
//								cena.unregisterTouchArea(manutencao);
//								
//							}});
//							
//							
//				vendido=1;
//
//				}
//
//				// TODO: Implement this method
//				return true;
//			}
//		
//	};
//	valor_mercado.animate(50);
////	valor_mercado.setAnchorCenter(0,0);
//	valor_mercado.setSize(barra_saude_maq_pai.getHeight(),barra_saude_maq_pai.getHeight());
//	rect.attachChild(valor_mercado);
//	valor_mercado.setPosition(confiabilidade_maq_visivel.getX()+confiabilidade_maq_visivel.getWidthScaled()/2+10+valor_mercado.getWidthScaled()/2,confiabilidade_maq_visivel.getY());
//	
//	valor_maq_visivel=new Text(0,0,fonte,"R$ "+ String.format("%.2f",valor_maq),this.getVertexBufferObjectManager());
//	valor_maq_visivel.setScale(.5f);
//	rect.attachChild(valor_maq_visivel);
//	valor_maq_visivel.setPosition(valor_mercado.getX()+valor_mercado.getWidthScaled()/2+10+valor_maq_visivel.getWidthScaled()/2,valor_mercado.getY());
//	
//		espaco_maq=new Rectangle(this.getX(),this.getY(),this.getWidthScaled()*2,this.getHeightScaled()*2,this.getVertexBufferObjectManager());
//
//		espaco_maq.setColor(1f,1f,1f);
//
//		espaco_maq.setAlpha(.1f);
//	
//	
//	lead_time=new AnimatedSprite(0,0,relogio,this.getVertexBufferObjectManager());
//	//lead_time.setScale(2);
//	
//    espaco_maq.attachChild(lead_time);
//	lead_time.setPosition(espaco_maq.getWidth(),espaco_maq.getHeight());
//	lead_time.animate(50);
//	lead_time.setVisible(false);
//	
//	foto_maq=new AnimatedSprite(0,0, mesmo,this.getVertexBufferObjectManager()){
//		float j=0;
//		@Override
//		protected void onManagedUpdate(float pSecondsElapsed){
//			
//			if(este.getCurrentTileIndex()==tipo_maq+7){
//			if(j==1){j=0;}
//			this.setAlpha((float)Math.abs(Math.sin(Math.toDegrees(j))));
//			j+=.001f;
//			}else if(this.isAnimationRunning()==false){this.setAlpha(1);this.animate (new long[]{tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_},tipo_maq+1,tipo_maq+6,true);}
//			
//			super.onManagedUpdate(pSecondsElapsed);
//			
//		}
//	};
//	foto_maq.setCurrentTileIndex(tipo_maq);
//	foto_maq.setScale(3f);
//	rect.attachChild(foto_maq);
//	foto_maq.setPosition(3*rect.getWidth()/3.5f,rect.getHeight()/2.5f);
//    foto_maq.animate(50);
//	foto_maq.animate (new long[]{tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_},tipo_maq+1,tipo_maq+6,true);
//		
//
//	manutencao=new AnimatedSprite(0,0,chave,this.getVertexBufferObjectManager()){
//			float j=0,custo_manutencao=0;
//			@Override
//			protected void onManagedUpdate(float pSecondsElapsed){
//				if(ligado==1&&saude_maq<=100&&saude_maq>=50&&this.getCurrentTileIndex()!=0){this.setCurrentTileIndex(0);}
//				if(ligado==1&&saude_maq<=50&&saude_maq>=25&&this.getCurrentTileIndex()!=1){this.setCurrentTileIndex(1);}
//				if(ligado==1&&saude_maq<=25&&saude_maq>=0&&this.getCurrentTileIndex()!=2){this.setCurrentTileIndex(2);}
//                if(ligado==0&&this.getCurrentTileIndex()!=3){this.setCurrentTileIndex(3);}
//				
//				if(ligado==3){
//					if(this.getCurrentTileIndex()!=3){this.setCurrentTileIndex(3);}
//					if(j==1){j=0;}
//					this.setAlpha((float)Math.abs(Math.sin(Math.toDegrees(j))));
//					j+=.001f;
//				}else if(ligado==4){
//					this.setAlpha(1);
//					if(j>=360){j=0;}
//					if(this.getRotation()==0){
//						custo_manutencao=(float)valor_maq*.01f;
//						Empresario.fortuna-=custo_manutencao;
//						custosElucros=new CustoAnimado(este.getX(),este.getY(),fonte_anim,String.format("%.2f",custo_manutencao) ,this.getVertexBufferObjectManager(),1);
//						custosElucros.setScale(.5f);
//						cena.attachChild(custosElucros);
//					}
//					this.setRotation(j);
//					j+=5f;saude_maq+=.1f;
//					if(saude_maq>=100){j=0f;this.setRotation(0); ligado=0;nome_maq.setText(nome_maq_temp);}
//					if(este.getCurrentTileIndex()==(tipo_maq+7)&&saude_maq>25){ligado=4;nome_maq.setText(nome_maq_temp+" - Manutencao"); este.setCurrentTileIndex(tipo_maq);}
//					}
//				
//
//				super.onManagedUpdate(pSecondsElapsed);
//
//			}
//			
//			@Override
//			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
//			{
//				if(pSceneTouchEvent.isActionUp()&&ligado==0&&saude_maq<100){
//					 
//					ligado=4;
//				//	botao_mais_menos_tempo.setAlpha(.25f);
//					botao_mais_menos_operador.setAlpha(.25f);
//					valor_mercado.setAlpha(.25f);
//					nome_maq.setText(nome_maq.getText()+" - Manutencao!");
//			
//				}else if(pSceneTouchEvent.isActionUp()&&ligado==4){
//					if(este.getCurrentTileIndex()==tipo_maq+7){
//						ligado=3;
//						nome_maq.setText(nome_maq_temp);
//						} 
//						else{
//							ligado=0;
//					     //   botao_mais_menos_tempo.setAlpha(1);
//							botao_mais_menos_operador.setAlpha(1);
//							valor_mercado.setAlpha(1);
//							this.setRotation(0);
//							nome_maq.setText(nome_maq_temp);}
//				}else if(pSceneTouchEvent.isActionUp()&&ligado==3){
//					if(saude_maq<=0){saude_maq=.001f;}
//					ligado=4;
//				//	botao_mais_menos_tempo.setAlpha(.25f);
//					botao_mais_menos_operador.setAlpha(.25f);
//					valor_mercado.setAlpha(.25f);
//				//	nome_maq_temp=(String) nome_maq.getText();
//					nome_maq.setText(nome_maq.getText()+" - Conserto");
//					
//					}
//				
//				
//
//				// TODO: Implement this method
//				return true;
//			}
//			
//			
//	};
//	cena.registerTouchArea(manutencao);
//	
//	manutencao.setCurrentTileIndex(3);
//	espaco_maq.attachChild(manutencao);
//	
//	manutencao.setPosition(espaco_maq.getWidth(),0);
//	
//	botao_cima_baixo = new Sprite(0,0,cimabaixo,this.getVertexBufferObjectManager()){
//			@Override
//			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
//			{
//				if(pSceneTouchEvent.isActionUp()){
//
//					//this.setRotation(this.getRotation()+120);j
//                    if(this.getRotation()==180){this.setRotation(300);direcao_lancamento=-1;}
//					else if(this.getRotation()==300){this.setRotation(420);direcao_lancamento=1;}
//					else{this.setRotation(180);direcao_lancamento=0;}
//
//				}
//
//				// TODO: Implement this method
//				return true;
//			}
//
//		};
//		botao_cima_baixo.setScale(1.5f,2);
//		botao_cima_baixo.setRotation(180);
//		espaco_maq.attachChild(botao_cima_baixo);
//		cena.registerTouchArea(botao_cima_baixo);
//	
//	
//    
//	//rect_proibido.setPosition(this.getWidthScaled()/2,this.getHeightScaled()/2);
//	this.criar_corpo();
//	
//	}
//
//	@Override
//	public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
//	{
//		if(pSceneTouchEvent.isActionUp()&&this.alpha>=1&&MainActivity.flag==0){
//		
//			if(rect.getParent()!=hud){
//			hud.attachChild(rect);
//			rect.setPosition(MainActivity.CAMERA_WIDTH-rect.getWidth()/2,rect.getHeight()/2);
//	
//			}
//		}
//		// TODO: Implement this method
//		return true;
//	}
//
//	
//	
//	@Override
//	protected void onManagedUpdate(float pSecondsElapsed)
//	{
//		try{
//	    if(vendido==1&&this.getParent()==cena){
//			if(this.getAlpha()>1){this.anim_venda(this.getX(),this.getY(),true);}
//			alpha-=.005f;
//			this.setAlpha(alpha);
//			if(this.getAlpha()<=0.05){
//				
//				motor.runOnUpdateThread(new Runnable(){
//						public void run(){
//							//este=null;l
//							dinheiro.setParticlesSpawnEnabled(false);
//							dinheiro=null;
//							cena.detachChild(este);
//							este.dispose();
//							este.clearUpdateHandlers();
//							este=null;
//							espaco_maq=null;
//							manutencao=null;
//							rect=null;
//							botao_cima_baixo=null;
//							lead_time=null;
//							pedacos=null;
//							fumacas=null;
//							numeros=null;
//						}});
//				}
//		
//		}else if(alpha<1){
//		alpha+=0.05f;
//		this.setAlpha(alpha);}
//		
//		if(ligado==1||ligado==7){
//			if(t<100){
//			t+=1;
//		    tremer_cena(15,10,15,.3f,t);
//			}
//			
//			if(!this.isAnimationRunning()){this.animate (new long[]{tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_,tempo_producao_},tipo_maq+1,tipo_maq+6,true);}
//			
//			if(ligado==7)
//				{if(numeros==null){
//					this.produzindo(this.getX(),this.getY(),true);}
//					else if(!numeros.isParticlesSpawnEnabled()){
//						numeros.setParticlesSpawnEnabled(true);}
//				}else if(numeros!=null&&numeros.isParticlesSpawnEnabled()){numeros.setParticlesSpawnEnabled(false);}
//			
//			if(clock>50){
//			
//				saude_maq-=(produzindo==1)?(.1f):(0.01f);//altera confiabilidade tb!
//			
//			    confiabilidade_maq-=(produzindo==1)?(.0001):(0.00001);
//			
//				valor_maq*=(produzindo==1)?(.9999f):(0.99999f);
//				
//			 
//			    clock=0;
//			
//			}else{clock++;}
//			
//		
//		}else if(this.isAnimationRunning()){this.stopAnimation(tipo_maq);}
//		
//		if(saude_maq<=25&&saude_maq>=0&&(ligado==1||ligado==7)){fumacas.setParticlesSpawnEnabled(true);}else{fumacas.setParticlesSpawnEnabled(false);}
//		
//		if(saude_maq<=0){ligado=3;
//		if(this.getCurrentTileIndex()!=tipo_maq+7){
//			t=0;
//		    valor_mercado.setAlpha(.25f);
//			botao_mais_menos_operador.setAlpha(.25f);
//		//	botao_mais_menos_tempo.setAlpha(.25f);
//			foto_maq.stopAnimation(tipo_maq+7);
//			valor_maq*=.75f;
//			confiabilidade_maq*=.98f;
//			botao_ligades.setCurrentTileIndex(1);
//			tile_empresario_temp=MainActivity.dono_fabrica.getCurrentTileIndex();
//			MainActivity.dono_fabrica.setCurrentTileIndex(2);
//			if(pedacos==null){this.quebrou(this.getX(),this.getY(),true);}else{pedacos.setParticlesSpawnEnabled(true);}
//		}
//		this.stopAnimation(tipo_maq+7);
//		
//			if(t<100){
//				t+=1;
//				tremer_cena(15,10,15,.2f,t);
//		        
//		}else if(tile_empresario_temp!=-1){pedacos.setParticlesSpawnEnabled(false); fumacas.setParticlesSpawnEnabled(false);MainActivity.dono_fabrica.setCurrentTileIndex(tile_empresario_temp);tile_empresario_temp=-1;}
//		
//		}
//		
//		barra_saude_maq.setWidth(saude_maq);
//		tempo_prod_visivel.setText(""+tempo_producao_);
//		valor_maq_visivel.setText("R$ "+ String.format("%.2f",valor_maq));
//		confiabilidade_maq_visivel.setText("Conf: "+String.format("%.2f",confiabilidade_maq));
//		valor_operacao_visivel.setText(""+valor_operacao);
//		
//		
//		
//		//maqb.setUserData("maquina;"+ligado+";"+produzindo+";"+tipo_maq+"-"+tempo_producao_+";"+valor_operacao+";"+saude_maq+";"+confiabilidade_maq);
//		
//		maqb.setUserData(this);
//		
//		
//		}catch(Exception e){}
//		super.onManagedUpdate(pSecondsElapsed);
//		
//
//	
//	}
//	public void criar_corpo(){
//		FixtureDef maqf = PhysicsFactory.createFixtureDef(0,0.2f,0);
//		maqb=PhysicsFactory.createBoxBody(mundo,this,BodyDef.BodyType.StaticBody,maqf);
//		fc=new PhysicsConnector(this,maqb);
//		mundo.registerPhysicsConnector(fc);
//		maqb.setUserData(this);
//	}
//	
//	public void destruir_corpo(){
//		MainActivity.mundo.unregisterPhysicsConnector(fc);
//		MainActivity.mundo.destroyBody(maqb);
//		maqb.setActive(false);
//	}
//	
//	void anim_venda(final float x,final float y,Boolean g){
//		ponto=new PointParticleEmitter(x,y);
//		
//		IEntityFactory rectfact =new IEntityFactory(){
//			public AnimatedSprite create(float pX,float pY){
//				AnimatedSprite rect=new AnimatedSprite(x,y,moeda,este.getVertexBufferObjectManager());
//				rect.setScale(.25f);
//				rect.animate(25);
//				return rect;
//			}};
//
//		dinheiro=new ParticleSystem(rectfact,ponto,1,2,5);
//		dinheiro.addParticleInitializer(new VelocityParticleInitializer(-10,10,0,100));
//		dinheiro.addParticleInitializer(new AccelerationParticleInitializer(0,-9.8f));
//		dinheiro.addParticleModifier(new RotationParticleModifier(0,2.5f,0,360));
//		dinheiro.addParticleModifier(new AlphaParticleModifier(0,2.5f,1,0));
//		dinheiro.setParticlesSpawnEnabled(g);
//		cena.attachChild(dinheiro);
//	}
//	
//	void quebrou(final float x,final float y, boolean g){
//		ponto=new PointParticleEmitter(x,y);
//		IEntityFactory rectfact =new IEntityFactory(){
//			public Rectangle create(float pX,float pY){
//				Rectangle rect=new Rectangle(x,y,10,10,este.getVertexBufferObjectManager());
//			    int c=255;
//				switch (tipo_maq_def){
//					case 1:
//						rect.setColor(36/c,92/c,36/c);
//						break;
//					case 2:
//						rect.setColor(41/c,64/c,109/c);
//						break;
//					case 3:
//						rect.setColor(74/c,23/c,23/c);
//						break;
//					case 4:
//						rect.setColor(81/c,81/c,81/c);
//						break;
//						
//				}
//				return rect;
//			}};
//
//		pedacos=new ParticleSystem(rectfact,ponto,1,10,5);
//		pedacos.addParticleInitializer(new VelocityParticleInitializer(-50,50,0,50));
//		pedacos.addParticleInitializer(new AccelerationParticleInitializer(0,-9.8f));
//		pedacos.addParticleModifier(new RotationParticleModifier(0,2.5f,0,360));
//		pedacos.addParticleModifier(new AlphaParticleModifier(0,2.5f,1,0));
//		pedacos.setParticlesSpawnEnabled(g);
//		cena.attachChild(pedacos);
//	}
//	
//	void produzindo(final float x,final float y, boolean g){
//		ponto=new PointParticleEmitter(x,y);
//		IEntityFactory rectfact =new IEntityFactory(){
//			public Text create(float pX,float pY){
//				Text rect=new Text(x,y,fonte_anim,""+r.nextInt(10),100,este.getVertexBufferObjectManager());
//				rect.setScale(.5f);
//				return rect;
//			}};
//
//		numeros=new ParticleSystem(rectfact,ponto,5,10,10);
//		
//		numeros.addParticleInitializer(new ColorParticleInitializer(0,1,0));
//		numeros.addParticleInitializer(new VelocityParticleInitializer(-20,20,0,100));
//		numeros.addParticleInitializer(new AccelerationParticleInitializer(0,-9.8f));
//		numeros.addParticleInitializer(new ExpireParticleInitializer(1));
//		numeros.addParticleModifier(new RotationParticleModifier(0,1f,0,360));
//		numeros.addParticleModifier(new AlphaParticleModifier(0,1f,1,0));
//		numeros.addParticleModifier(new ColorParticleModifier(0, 0.2f, 1, 1, 0, 0.5f, 0, 0));
//	    numeros.addParticleModifier(new ColorParticleModifier(0.3f, 1f, 1, 0, 0.5f, 1, 0, 1));
//		numeros.setParticlesSpawnEnabled(g);
//		cena.attachChild(numeros);
//	}
//
//	void tremer_cena(float alpha,float w,float phi,float gama,int t){
//		float y=motor.getCamera().getCenterY();
//		y+=(float)(Math.exp(-gama*t)*alpha*Math.cos(phi*t-alpha));
//		motor.getCamera().setCenter(motor.getCamera().getCenterX(),y);
//	}
//
//	@Override
//	public void onAttached()
//	{
//		maquininha=new Rectangle(this.getX()*MainActivity.fator2,MainActivity.fator* MainActivity. minimapa.getHeight()+ this.getY()*MainActivity.fator2,this.getWidthScaled()*MainActivity.fator2,this.getHeightScaled()*MainActivity. fator2,this.getVertexBufferObjectManager());
//		maquininha.setColor(0,1,0);
//		MainActivity.minimapa.attachChild(maquininha);
//		// TODO: Implement this method
//		super.onAttached();
//	}
//
//	@Override
//	public void onDetached()
//	{
//	//	motor.runOnUpdateThread(new Runnable(){
//					//	public void run(){
//						MainActivity.minimapa.detachChild(este.maquininha);
//						este.maquininha=null;
//					//	}});
//		// TODO: Implement this method
//		super.onDetached();
//	}
//	
//	
//}
