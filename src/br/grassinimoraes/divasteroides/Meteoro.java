package br.grassinimoraes.divasteroides;
import org.andengine.entity.sprite.*;
import org.andengine.opengl.texture.region.*;
import org.andengine.opengl.vbo.*;
import org.andengine.extension.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.*;
import java.util.*;
import org.andengine.entity.text.*;
import org.andengine.opengl.font.*;
import org.andengine.util.adt.align.*;
import org.andengine.entity.primitive.*;
import org.andengine.extension.physics.box2d.util.*;
import org.andengine.extension.physics.box2d.util.constants.*;
import com.badlogic.gdx.math.*;
import android.util.*;
import org.andengine.util.modifier.*;
import org.andengine.entity.modifier.*;
import org.andengine.entity.particle.emitter.*;
import org.andengine.entity.particle.*;
import org.andengine.entity.*;
import org.andengine.entity.particle.initializer.*;
import org.andengine.entity.particle.modifier.*;
import android.opengl.*;
import org.andengine.engine.*;
import org.andengine.engine.handler.timer.*;
import org.andengine.input.touch.*;

public class Meteoro extends AnimatedSprite
{

	private FixtureDef metf;

	private PhysicsConnector fc_met;

	private Body metb;

	private float velocidade_x;

	Random aleatorio = new Random();

	static int qtde_meteoro_destruida;

	int valor;

	Text valor_visivel;

	static ArrayList<Meteoro> meteoro_lista=new ArrayList<Meteoro>();

//	static Boolean destroi_tudo=false;

	//boolean destruido=false;

	Rectangle alca;

	AnimatedSprite alvo;

	float velocidade_y;

	private float sc_temp;

	private PointParticleEmitter ponto;

	private ParticleSystem pedacos;

	private CircleOutlineParticleEmitter particleEmitter;

	private SpriteParticleSystem explosao;

	private PhysicsWorld mundo;

  private Engine motor;

  private int voltar;

   int valor_int;
   static int valor_max=500,valor_min=Math.round(valor_max*.1f);

  int pontos;

  private int largura;

  private MainActivity atividade;

  private ParallelEntityModifier pem;

  private boolean ja_destruido=false;

  private long memoria_usada;

  private TiledTextureRegion text;

  private Sprite efeitos;

  private Font fonte;

  private Text texto_anim;

  private int plus;
  
  private boolean atingiu_cidade;

  private static int fator_pontuacao = (int)Math.round(Math.log(valor_max*Math.exp(8)));

  int pontuacao;

  private static boolean destroi_tudo = false;

  int fatores_primos[]={2,3,5,7,11,13};

  private int comprimento;

  private SequenceEntityModifier seqm2;

  private boolean atingiu_canhao=false;

  private Boolean setar_score;

  public static boolean cidade_intacta;

  public boolean dividiu;

  public int disparos;

  private int soma_expoentes;


  Meteoro(float x, float y, int qtde_met_dest,  TiledTextureRegion text, VertexBufferObjectManager vert)
	{
		super(x, y, text, vert);
	    this.valor = this.retornar_valor(qtde_met_dest);
		meteoro_lista.add(this);
		this.setScale((valor > 20) ?10: (valor < 8) ?4: valor / 2);		//scale muda conforme o numero
	    this.setCurrentTileIndex(aleatorio.nextInt(4));
	    this.text=text;
		//implementar um update para checar quando se choca contra o alvo
		//fazer o genericpool da classe meteoropool estender esta classe
		//criar uma classe projetil e fazer o mesmo com o generic pool da classe projetilpool
	}

  static void resetar_variaveis_estaticas(){
	qtde_meteoro_destruida=0;
	if(meteoro_lista.size()>0)meteoro_lista.removeAll(meteoro_lista);
	destroi_tudo=false;
	cidade_intacta=false;
  }

	void construir_corpo(MainActivity atividade, int CAMERA_WIDTH, int CAMERA_HEIGHT)
	{
	    destroi_tudo=false;
		ja_destruido=false;
	    this.atividade=atividade;
	    this.mundo = atividade.mundo;
		this.motor = atividade.motor;
		metf = atividade.metf;
		metb = PhysicsFactory.createBoxBody(mundo, this, BodyDef.BodyType.DynamicBody, metf);
		fc_met = new PhysicsConnector(this, metb);
	    mundo.registerPhysicsConnector(fc_met);
		metb.setUserData(this);
		
	  //qto maior o valor_int, menor sera valor_velocidade_cos, ou seja, dara um numero entre 0 e pi,
	  //consequntemente, maior sera o seu cos
	  //assim, qto maior o valor_int, menor sera a sua velocidad
	  //assim, ficara proporcionalmente mais facil para o jogador

		this.largura=CAMERA_WIDTH;
		this.comprimento=CAMERA_HEIGHT;
	    velocidade_y=.5f+(float)Math.abs(Math.sin((Math.PI)/(2+Math.log(valor))));
		velocidade_x = (aleatorio.nextInt(CAMERA_WIDTH) - this.getX()) / (CAMERA_HEIGHT / velocidade_y);//PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;
        
		
	    this.registerEntityModifier(new LoopEntityModifier(new RotationModifier(1,0,(velocidade_x<0)?-360:360)));
	

	}

	void mudar_tipo(final int i)
	{
				
					Meteoro.this.setCurrentTileIndex(i);
					Meteoro.this.meteoro_lista.remove(this);
					Meteoro.this.setScale(2);
					Meteoro.this.clearEntityModifiers();
					Meteoro.this.registerEntityModifier(new LoopEntityModifier(
															 new SequenceEntityModifier(
																 new ScaleModifier(.5f, Meteoro.this.getScaleX(), Meteoro.this.getScaleX() * 1.5f),
																 new ScaleModifier(.5f, 1.5f * Meteoro.this.getScaleX(), Meteoro.this.getScaleX()))));
					Meteoro.this.valor_visivel.clearUpdateHandlers();
					Meteoro.this.valor_visivel.clearEntityModifiers();
					Meteoro.this.valor_visivel.detachSelf();
				
		
	}

	


	void destruir_corpo(Boolean setar_score)
	{
			        //imforma q ja foi destruido, para nao entrar no loop do timer
		         	ja_destruido=true;
					this.setar_score=setar_score;
				    //animacao meteoro semdo dedtruido
				    animar_explosoes(Meteoro.this);
					
				
					
				    //remover o sprite
					Meteoro.this.clearUpdateHandlers();
					Meteoro.this.clearEntityModifiers();
					Meteoro.this.setVisible(false);
					Meteoro.this.detachSelf();
					
					//remover da lista
					meteoro_lista.remove(Meteoro.this);

					//remover fisica
					Meteoro.this.mundo.unregisterPhysicsConnector(Meteoro.this.fc_met);//mundo.getPhysicsConnectorManager().findPhysicsConnectorByShape(sprite));
					Meteoro.this.metb.setActive(false);
					Meteoro.this.mundo.destroyBody(Meteoro.this.metb);
					
					
					//remover o texto
					Meteoro.this.valor_visivel.clearUpdateHandlers();
					Meteoro.this.valor_visivel.clearEntityModifiers();
					Meteoro.this.valor_visivel.detachSelf();
					

	}

	@Override
	protected void onManagedUpdate(float pSecondsElapsed)
	{
	

		if (metb != null)
	  {    // velocidade_y=25f+(float)Math.abs(Math.sin((Math.PI)/(2+Math.log(valor))));
			metb.setLinearVelocity(velocidade_x, -velocidade_y);
			
		}

		valor_visivel.setText("" + valor);
		valor_visivel.setPosition(this);
	    valor_visivel.setAutoWrapWidth(this.getWidthScaled());
		if((destroi_tudo&&!ja_destruido||atingiu_canhao)){
		  
		  motor.runOnUpdateThread(new Runnable(){

			  public void run(){
				
				Meteoro.this.destruir_corpo(false);
			  }});
		  
		}
		
		if(this.collidesWith(atividade.canhao)){
		  atingir_canhao(atividade.canhao);
		  
		}

		super.onManagedUpdate(pSecondsElapsed);
	}

	private void atingir_canhao(Canhao co)
	{
	  
	   atividade.cidade_grande.atingir_cidade(1,this);
	   co.desativar_canhao(this);
	   atingiu_canhao=true;
	  // TODO: Implement this method
	}

	void valor_visivel(Font fonte)
	{
	    this.fonte=fonte;
		valor_visivel = new Text(0, 0, fonte, "" + valor, 100, this.getVertexBufferObjectManager());
		sc_temp = this.getScaleX() / 4;
		valor_visivel.setScale(2);
		valor_visivel.setPosition((this.getWidth()) / 2, (this.getHeight()) / 2);
        
		valor_visivel.setHorizontalAlign(HorizontalAlign.CENTER);


	}
	
  private int retornar_valor(int qtde_met_dest){
	  try{
       //expoentes
	   soma_expoentes=0;
	   valor_int=1;
	   //qtde_met_dest=(qtde_met_dest>26)?(30+qtde_met_dest):qtde_met_dest;
	   for(int i=0;i<fatores_primos.length;i++){
		  
		  
		  int p =(qtde_met_dest>30)?(fatores_primos[aleatorio.nextInt(fatores_primos.length)]):fatores_primos[i];
	      int pot=(qtde_met_dest<p)?0:(int)Math.round(Math.random()*qtde_met_dest/p);
		  int pot_max=(int)(Math.log(valor_max)/Math.log(p));
		  pot=(pot>pot_max)?Math.round(pot_max/2):pot;
	
		  //pot=(int) (pot>Math.log(valor_max)?Math.round(Math.log(valor_max)):pot);
		  
		  int v=(int) Math.pow(p,pot);
		  
		  if(valor_int*v>valor_max){
			if(fatores_primos[i]==13){
			  while(valor_int<valor_min){
			    valor_int*=fatores_primos[aleatorio.nextInt(fatores_primos.length)];
			    soma_expoentes+=1;
			  }
			  break;
			}
			continue;
			}
		  
		  valor_int*=v;//setar valores
		  
		  soma_expoentes+=pot;
		}
		valor_int=(valor_int<2)?2:(valor_int>valor_max)?valor_min:valor_int;
		this.pontuacao=((soma_expoentes<1)?1:(valor_int==valor_min)?4:soma_expoentes)*fator_pontuacao;//a pontuacao relativa a este meteoro e conforme a qtde de divisores primos de seu valor
		
	  }
	  catch(Exception e)
	  {valor_int=8;pontuacao=3*fator_pontuacao;e.printStackTrace();}
	  
	  return valor_int;
	}
	
  public void atingir_meteoro(final Projetil proj,final Cidade cid)
  {
//colocar uma pontuacao dinamica
	motor.runOnUpdateThread(new Runnable(){

		


		public void run(){
		  if (Meteoro.this.getCurrentTileIndex() < 4)
		  {
			proj.resetar_pos();
			if(proj.divisor==0){
			  destruicao_nuclear(cid);
			}
			else if (Meteoro.this.valor % proj.divisor == 0)
			{
			  Meteoro.this.disparos+=1;
			  Meteoro.this.valor /= proj.divisor;
			  float sc_temp2=Meteoro.this.getScaleX() * .90f;//diminuir a escala para um pouco mais de realismo
			  Meteoro.this.setScale((sc_temp2 < 4) ?4: sc_temp2);
              
			  
			  if (Meteoro.this.valor < 2 || proj.divisor==1)
			  {

				atividade.qtde_met_dest += 1;
				cid.atingir_cidade(1,Meteoro.this);
				//atividade.animar_explosoes(Meteoro.this);

				boolean saude=(Math.random() > Math.random());
				boolean plus=(6.25f * Math.random() > cid.saude);
				boolean escudo=(Math.random() > Math.random());
				boolean bombas=(Math.random() > Math.random());

				boolean pega_item=(Math.pow(Math.random(),Math.log(cid.saude))>Math.random())&&(100*Math.random()>cid.saude*Math.random());

				if(pega_item){
				  Meteoro.this.mudar_tipo((saude)?4:(escudo)?5:(plus)?6:(bombas)?7:Meteoro.this.getCurrentTileIndex());
				}
                
				//aqui sao criados textos de estimulo ao jogador
				Meteoro.qtde_meteoro_destruida+=1;
				if(Meteoro.qtde_meteoro_destruida%10==0){
				 // Meteoro.qtde_meteoro_destruida=0;
				  atividade.texto_animado("Você é um máximo!",0,0,1,null,1);//a cada dez meteoros destruidos sem q a cidade seja atingida
				}
				if(Meteoro.qtde_meteoro_destruida>50&&Meteoro.cidade_intacta){
				  Meteoro.qtde_meteoro_destruida=0;
				  atividade.texto_animado("Parabéns! Nenhum metoro atingiu a cidade!",0,0,1,null,1);//destruir maus de cinquenta meteoros sem.q a cidade tenha sido atingida
				}
				if(Meteoro.qtde_meteoro_destruida!=0&&disparos==soma_expoentes){//se o jogador destruir um meteoro dividindo pelos seus divisores
				  String texto=(Math.random()>Math.random())?"Voce é bom em divisao!":
					(Math.random()>Math.random())?"Continue Assim!":
					(Math.random()>Math.random())?"Raciocinio Rapido!":
					"Perfeito!";
				  atividade.texto_animado(texto,0,0,1,null,1);
				  
				}
				
				
				//ajustar score
			//	pontuacao=Meteoro.this.pontos*fator_pontuacao;
				atividade.setar_score(Meteoro.this);
				
				if(Meteoro.this.getCurrentTileIndex()<4){Meteoro.this.destruir_corpo(true);};// destruir_corpo(mundo);


			  }
			}else{Meteoro.this.disparos=-10;}}
		  else
		  {
			escolher_itens(cid);
		  }
		}});


	// TODO: Implement this method
  }

 public void destruir_meteoro_e_atingir_cidade(final Cidade u1)
  {
//mostrar quanto perdeu de saude e tombar a barra
   motor.runOnUpdateThread(new Runnable(){
		public void run(){
		  if(Meteoro.this.getCurrentTileIndex()<4){
	       atingiu_cidade=true;
		   u1.atingir_cidade(0,Meteoro.this);
           Meteoro.qtde_meteoro_destruida=0;
		   Meteoro.cidade_intacta=false;
		 //  atividade.animar_explosoes(Meteoro.this);
		   Meteoro.this.destruir_corpo(true);
		  }else{
			escolher_itens(u1);
		  }
		}});
	// TODO: Implement this method
  }

  void escolher_itens(Cidade c){
	int i=this.getCurrentTileIndex();
	c.aumentou_saude=false;
	switch(i){
	  case 4:
		//saude
		plus=5;
		c.saude+=plus;
		this.destruir_corpo(true);
		c.aumentou_saude=true;
		break;
	  case 5:
		//campo de forca
		atividade.campo_forca.ativar(c);
		this.destruir_corpo(true);
		break;
	  case 6:
		//saude maior
		plus=15;
		c.saude+=plus;
		this.destruir_corpo(true);
		c.aumentou_saude=true;
		break;
	  case 7:
		//bombas
		if(Math.random()>Math.random()){
		  atividade.bomba_um=(atividade.bomba_um)?false:true;
		}else{atividade.bomba_tudo=(atividade.bomba_tudo)?false:true;;}
		this.destruir_corpo(true);
		break;
	}
	//Log.e("teste",fogo_lista.size()+"/"+aumentou_saude);
	
  }
  
  void animar_explosoes(Meteoro met){
//	try{
//	  memoria_usada=((Runtime.getRuntime().freeMemory())/1024);
//	}catch(Exception e){e.printStackTrace();memoria_usada=4000;}
//	if(memoria_usada>.1*atividade.memoria_livre_minima){
	  final float x=met.getX();
	  final float y=met.getY();
	  final float c=met.getScaleX();
	  int i=met.getCurrentTileIndex();
	  if(i<4){
		if(setar_score){ atividade.texto_animado((atingiu_cidade)?("-"+String.format("%.0f",this.getScaleX())):("+"+this.pontuacao),1,(atingiu_cidade)?0:1,0,this,0);}
		
		IEntityFactory pedacosfact =new IEntityFactory(){
		  public Sprite create(float pX,float pY){
			AnimatedSprite pedacos=new AnimatedSprite(x,y,text,motor.getVertexBufferObjectManager());
			
			pedacos.animate(new long[]{50,50,50,50},0,3,true);

			return pedacos;
		  }};
		explodir(x,y,c,1,0,0,pedacosfact);
	  }
	  else if(i==4||i==6){
		//	
		atividade.texto_animado("+"+plus,0,1,0,this,0);
		IEntityFactory moedafact =new IEntityFactory(){
		  public Sprite create(float pX,float pY){
			Sprite moedinha=new Sprite(x,y,atividade.moeda,motor.getVertexBufferObjectManager());
			//moedinha.animate(25);
			moedinha.setScale(.5f);
			return moedinha;
		  }};


		explodir(x,y,c,1-(float)Math.random(),(float)Math.random(),(float)Math.random(),moedafact);


	  }
	  else if(i==5){
		IEntityFactory camposfact =new IEntityFactory(){
		  public AnimatedSprite create(float pX,float pY){
			AnimatedSprite campos=new AnimatedSprite(x,y,text,motor.getVertexBufferObjectManager());
			//moedinha.animate(25);
			campos.setScale(.5f);
			campos.setCurrentTileIndex(4);
			return campos;
		  }};


		explodir(x,y,c,1-(float)Math.random(),(float)Math.random(),(float)Math.random(),camposfact);

	  }
	  else if(i==7){
		IEntityFactory bombasfact =new IEntityFactory(){
		  public AnimatedSprite create(float pX,float pY){
			AnimatedSprite bombas=new AnimatedSprite(x,y,text,motor.getVertexBufferObjectManager());
			//moedinha.animate(25);
			bombas.setScale(.5f);
			bombas.setCurrentTileIndex(7);
			return bombas;
		  }};


		explodir(x,y,c,1-(float)Math.random(),(float)Math.random(),(float)Math.random(),bombasfact);

	  }
	

  }





  void explodir(final float x,final float y,float sc,float x1,float x2,float x3, IEntityFactory moedas){
	try{

	  efeitos=new Sprite(x,y,atividade.efeito,this.getVertexBufferObjectManager());
	  this.getParent().attachChild(efeitos);

	  efeitos.registerEntityModifier(new ParallelEntityModifier(new ScaleModifier(2,0,2*sc),new AlphaModifier(2.5f,1,0)));


	  particleEmitter = new CircleOutlineParticleEmitter(x, y, 10);
	  explosao = new SpriteParticleSystem(particleEmitter, 10, 50, 5, atividade.fumaca, this.getVertexBufferObjectManager());

	  //explosao
	  explosao.addParticleInitializer(new ColorParticleInitializer<Sprite>(x1, x2, x3));
	  explosao.addParticleInitializer(new AlphaParticleInitializer<Sprite>(0));
	  if(x1!=1){explosao.addParticleInitializer(new BlendFunctionParticleInitializer<Sprite>(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE));}
	  explosao.addParticleInitializer(new VelocityParticleInitializer<Sprite>(-2, 2, -10, -10));
	  explosao.addParticleInitializer(new RotationParticleInitializer<Sprite>(0.0f, 360.0f));
	  explosao.addParticleInitializer(new ExpireParticleInitializer<Sprite>(2f));
	  explosao.addParticleModifier(new ScaleParticleModifier<Sprite>(0, 0.5f, sc, 2*sc));
	  explosao.addParticleModifier(new ScaleParticleModifier<Sprite>(.75f, 1f, 2*sc, 0f));
	  explosao.addParticleModifier(new ColorParticleModifier<Sprite>(0, 0.2f, x1, x2, x3, 1-x3, 1-x2, 1-x1));
	  explosao.addParticleModifier(new ColorParticleModifier<Sprite>(0.3f, 1.5f, 1-x3,1- x2,1- x1, x1, x2, x3));
	  explosao.addParticleModifier(new AlphaParticleModifier<Sprite>(0, .5f, 0, 1));
	  explosao.addParticleModifier(new AlphaParticleModifier<Sprite>(2f, 5f, 1, 0));

	  //destrocos
	  ponto=new PointParticleEmitter(x,y);

	  pedacos=new ParticleSystem(moedas,ponto,5*sc, 12.5f*sc,Math.round(10*sc));

	  pedacos.addParticleInitializer(new VelocityParticleInitializer(-50,50,0,-100));
	  pedacos.addParticleInitializer(new AccelerationParticleInitializer(0,-9.8f));
	  pedacos.addParticleModifier(new RotationParticleModifier(0,2.5f,0,360));
	  pedacos.addParticleInitializer(new ExpireParticleInitializer<Sprite>(5f));
	  pedacos.addParticleModifier(new AlphaParticleModifier(0,5,1,0));
	  if(x1!=1){
		pedacos.addParticleModifier(new ColorParticleModifier<Sprite>(0, 0.2f, 0, 1, 1, 0, 0, 1));
		pedacos.addParticleModifier(new ColorParticleModifier<Sprite>(0.3f, 1.5f, 1, 0, 0, 1, 0, .5f));
		pedacos.addParticleInitializer(new BlendFunctionParticleInitializer<Sprite>(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE));
	  }
	  if(!explosao.hasParent()){this.getParent().attachChild(explosao);}
	  if(!pedacos.hasParent()){this.getParent().attachChild(pedacos);}

	  atividade.destruir_explosao(2,explosao,efeitos);
	  atividade.destruir_explosao(5,pedacos,efeitos);
	}catch(Exception e){
	  Log.e("Erro",e.toString()+e.getLocalizedMessage());
	}
  }
  private void destruicao_nuclear(Cidade c){
  SequenceEntityModifier seqm= new SequenceEntityModifier(
	new AlphaModifier(5,0,1),new AlphaModifier(5,1,0)){
	@Override
	protected void onModifierStarted(IEntity e){
	  e.setVisible(true);
	  e.setPosition(largura/2,comprimento/2);
	  //TODO Auto-generated method stub
	  super.onModifierFinished(e);

	}
	@Override
	protected void onModifierFinished(IEntity e){
	  e.setVisible(false);
	  e.setPosition(-largura,-comprimento);
	  //TODO Auto-generated method stub
	  super.onModifierFinished(e);

	}
  };
  seqm.setAutoUnregisterWhenFinished(true);


  atividade.efeito_rect.registerEntityModifier(seqm);

  c.atingir_cidade(2,null);
  atividade.qtde_met_dest+=this.meteoro_lista.size();
  Meteoro.destroi_tudo=true;
  }

  
  
  
}
