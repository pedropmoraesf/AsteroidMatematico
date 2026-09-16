package br.grassinimoraes.divasteroides;
import org.andengine.opengl.vbo.*;
import org.andengine.entity.sprite.*;
import org.andengine.opengl.texture.region.*;
import org.andengine.entity.primitive.*;
import org.andengine.entity.text.*;
import org.andengine.util.adt.align.*;
import org.andengine.entity.modifier.*;
import org.andengine.entity.*;

public class Canhao extends AnimatedSprite
{

  private MainActivity atividade;

  private Rectangle barra_saude;

  private Rectangle barra_saude_dinamica;

  private Text saude_visivel;

  boolean canhao_desligado = false;

  private SequenceEntityModifier seqm2;

  private float periodo_delay;

  private float periodo_total;
  
  float r,g,b;

  private ScaleAtModifier scm;
  Canhao(float x,float y,TiledTextureRegion text, MainActivity atividade, VertexBufferObjectManager vert){
	super(x,y,text,vert);
	this.atividade=atividade;
	this.setScale(4);
	r=this.getRed();
	g=this.getGreen();
	b=this.getBlue();
	
  }

  @Override
  public void onAttached()
  {
	this.criar_barrinha_saude();
	// TODO: Implement this method
	super.onAttached();
  };
  
  
  
  void criar_barrinha_saude(){
	barra_saude = new Rectangle(0,0,this.getWidthScaled(),5,this.getVertexBufferObjectManager());
	barra_saude.setColor(1,1,1);
	this.getParent().attachChild(barra_saude);
	barra_saude.setPosition(this.getX(),1.5f*this.getY());

	barra_saude_dinamica = new Rectangle(barra_saude.getWidth(),barra_saude.getHeight()/2,barra_saude.getWidth(),barra_saude.getHeight(),this.getVertexBufferObjectManager());
	barra_saude_dinamica.setAnchorCenter(1,.5f);
	barra_saude_dinamica.setColor(1,1,0);
	barra_saude.attachChild(barra_saude_dinamica);
	barra_saude.setAlpha(.5f);
	barra_saude_dinamica.setAlpha(.5f);

	//texto

	saude_visivel = new Text(0, 0, atividade.fonte, "" + String.format("%.0f",this.getBlue())+" %", 100, this.getVertexBufferObjectManager());;
	saude_visivel.setScale(.5f);
	saude_visivel.setPosition((barra_saude.getWidth()) / 2, (barra_saude.getHeight()) / 2);

	saude_visivel.setHorizontalAlign(HorizontalAlign.CENTER);
	barra_saude.attachChild(saude_visivel);

	barra_saude.setVisible(false);
  }

  @Override
  protected void onManagedUpdate(float pSecondsElapsed)
  {
	
	if(barra_saude.isVisible()){
	//barra_saude_dinamica.setWidth(barra_saude_dinamica.getWidth()+ barra_saude.getWidth()/periodo_total);
	  saude_visivel.setText("" + String.format("%.0f",barra_saude_dinamica.getScaleX()*100)+" %");
	}
	// TODO: Implement this method
	super.onManagedUpdate(pSecondsElapsed);
  }

  void desativar_canhao(final Meteoro m)
  
  {
	
	  atividade.runOnUpdateThread(new Runnable(){

		  public void run(){
            Canhao.this.clearEntityModifiers();
			barra_saude_dinamica.clearEntityModifiers();
		  
	
	
    periodo_delay=m.getScaleX()/4;
	periodo_total=(2+periodo_delay);
	
	scm=new ScaleAtModifier(periodo_total,0,1,1,1,1,1);
	scm.setAutoUnregisterWhenFinished(true);
	
	seqm2=new SequenceEntityModifier(
	  new ColorModifier(1,r,0,g,0,b,0),
	  new DelayModifier(periodo_delay),
	  new ColorModifier(1,0,r,0,g,0,b)){
	  @Override
	  protected void onModifierStarted(IEntity c){
		((AnimatedSprite)c).stopAnimation(0);
		BotaoDisparo.canhao_desligado=true;
		barra_saude.setVisible(true);
		barra_saude_dinamica.registerEntityModifier(scm);
		super.onModifierStarted(c);
	  }
	  @Override
	  protected void onModifierFinished(IEntity c){
		c.setBlue(b);
		c.setRed(r);
		c.setGreen(g);
		((AnimatedSprite)c).animate(new long[]{50,50,50,50,50,50,50,50},8,15,true);
		BotaoDisparo.canhao_desligado=false;
		barra_saude.setVisible(false);
		super.onModifierFinished(c);
	  }
	  
	};


	seqm2.setAutoUnregisterWhenFinished(true);
	Canhao.this.registerEntityModifier(seqm2);
	
	}});
	
	// TODO: Implement this method
  }
  
  
}
