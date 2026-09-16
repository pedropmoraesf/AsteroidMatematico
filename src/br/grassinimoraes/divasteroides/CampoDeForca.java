package br.grassinimoraes.divasteroides;
import org.andengine.entity.primitive.*;
import org.andengine.entity.IEntity;
import org.andengine.opengl.vbo.*;
import org.andengine.engine.camera.hud.*;
import org.andengine.opengl.font.*;
import org.andengine.entity.text.*;
import org.andengine.util.adt.align.*;
import org.andengine.entity.modifier.*;

public class CampoDeForca extends Rectangle
{

  private Rectangle barra_saude;

  private Rectangle barra_saude_dinamica;

  private float comprimento;

  private Text saude_visivel;

  private float saude=100;

  private float pos_x_def;

  private float pos_y_def;

  private boolean ativado;

  
  
  private MainActivity atividade;

  private ScaleModifier scm;

  private Cidade c;
  CampoDeForca(float x,float y,float l,float c,float posx,float posy,MainActivity atividade, VertexBufferObjectManager vert){
	super(x,y,l,c,vert);
	this.pos_x_def=0;
	this.pos_y_def=posy;
	this.setAnchorCenter(0,.5f);
	this.registerEntityModifier(
	  new LoopEntityModifier(
	  new ParallelEntityModifier(
	new ColorModifier(
	(float)Math.random(),
	(float)Math.random(),
    (float)Math.random(),
	1,
	1,
	(float)Math.random(),
	(float)Math.random()),
	new AlphaModifier((float)Math.random(),.75f+.25f*(float)Math.random(),.25f+.50f*(float)Math.random()))));
	this.atividade=atividade;
	
  }
private void ativar(boolean ativa){
  if(ativa){
	ativado=true;
	this.setPosition(0,pos_y_def);
	barra_saude.setVisible(true);//nao esta aparecendo!
	this.saude=100;
	this.setVisible(true);
	scm=new ScaleModifier(2,0,1);
	scm.setAutoUnregisterWhenFinished(true);
	this.registerEntityModifier(scm);
	
  }
  else{
	ativado=false;
	barra_saude.setVisible(false);
	
	scm=new ScaleModifier(2,1,0){
	  @Override
	  protected void onModifierFinished(final IEntity pItem){
		super.onModifierFinished(pItem);
		CampoDeForca cf=(CampoDeForca)pItem;
		cf.setPosition(-100,-100);
		cf.setVisible(false);
	  }
	};
	scm.setAutoUnregisterWhenFinished(true);
	this.registerEntityModifier(scm);
	
  }
}
  
  public void ativar(Cidade c)
  {
	this.c=c;
	
	this.ativar(true);
	
	
	// TODO: Implement this method
  }
  
  void criar_barra_saude(HUD hud, float x, float y, float l, float c, Font fonte){
	barra_saude = new Rectangle(0,0,l,c,this.getVertexBufferObjectManager());
	barra_saude.setColor(1,1,1);
	hud.attachChild(barra_saude);
	barra_saude.setPosition(x,y);

	barra_saude_dinamica = new Rectangle(barra_saude.getWidth(),barra_saude.getHeight()/2,barra_saude.getWidth(),barra_saude.getHeight(),this.getVertexBufferObjectManager());
	barra_saude_dinamica.setAnchorCenter(1,.5f);
	barra_saude_dinamica.setColor(0,0,1);
	barra_saude.attachChild(barra_saude_dinamica);
	this.comprimento=barra_saude_dinamica.getWidth();
	barra_saude.setAlpha(.5f);
	barra_saude_dinamica.setAlpha(.5f);

	//texto

	saude_visivel = new Text(0, 0, fonte, "" + String.format("%.0f",this.saude)+" %", 100, this.getVertexBufferObjectManager());;
	saude_visivel.setScale(.5f);
	saude_visivel.setPosition((barra_saude.getWidth()) / 2, (barra_saude.getHeight()) / 2);

	saude_visivel.setHorizontalAlign(HorizontalAlign.CENTER);
	barra_saude.attachChild(saude_visivel);
	
	barra_saude.setVisible(false);
  }
  
  @Override
  protected void onManagedUpdate(float pSecondsElapsed)
  {
	if(this.saude<=0){
	  saude=1;//para nao entrar aqui a cada update
	  ativar(false);
	  }
    if(ativado){
	  this.saude-=.1f;
	  if(barra_saude_dinamica!=null){
		//Log.e("saude",""+saude);
		saude_visivel.setText("" + String.format("%.0f",this.saude)+" %");
		barra_saude_dinamica.setWidth((comprimento*saude/100>barra_saude.getWidth())?barra_saude.getHeight():comprimento*saude/100);
	  }
	  
	for(int i=0;i<Meteoro.meteoro_lista.size();i++){
	  final Meteoro m=Meteoro.meteoro_lista.get(i);
	  if(m!=null&&this.collidesWith(m)){
		this.saude-=4*m.getScaleX();
	
	
		c.atingir_cidade(1,m);
		
		atividade.runOnUpdateThread(new Runnable(){
			public void run()

			{  
			  m.destruir_corpo(false);

			}});	
		
	  }

	}
	}
	super.onManagedUpdate(pSecondsElapsed);
  }
  
    
  
}
