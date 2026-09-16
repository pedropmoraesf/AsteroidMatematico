package br.grassinimoraes.divasteroides;
import org.andengine.entity.primitive.*;
import org.andengine.opengl.vbo.*;
import org.andengine.opengl.texture.region.*;
import org.andengine.entity.sprite.*;
import org.andengine.entity.modifier.*;

public class MiraLaser extends Rectangle
{

  private int ini,fin;

  private Meteoro m;

  private AnimatedSprite alvo;

  private boolean mostra;
  
  MiraLaser(float x, float y, float c, float l,boolean mostra, VertexBufferObjectManager vert)
  {
	super(x,y,c,l,vert);
	this.setColor(1,0,0);
	this.mostra=mostra;
	this.setAlpha((mostra)?.25f:0);//a torna invisivel
	this.setAnchorCenter(0,.5f);
	
	
	
  }

  void construir_alvo(TiledTextureRegion alvo1){
	alvo = new AnimatedSprite(0, 0, alvo1, this.getVertexBufferObjectManager());
	alvo.setVisible(false);
	this.getParent().attachChild(alvo);
	alvo.setScale(1.5f);
	alvo.setAlpha(.5f);
  }
  
  @Override
  protected void onManagedUpdate(float pSecondsElapsed)
  {
	if(mostra){
	
	
	for (int i=ini;i < fin + 1;i++)
	{
	  try
	  {
		if (m == null)
		{m = Meteoro.meteoro_lista.get(i);}
	  }
	  catch (Exception e)
	  {m = null;}
	  if (m != null && m.hasParent() && this.collidesWith(m) && m.getCurrentTileIndex() < 4&&!BotaoDisparo.canhao_desligado)
	  {
		ini = i;
		fin = i;
		for (int j=0;j < Meteoro.meteoro_lista.size();j++)
		{
		  Meteoro m_temp=Meteoro.meteoro_lista.get(j);
		  if (m_temp != m && this.collidesWith(m_temp) && m_temp.getCurrentTileIndex() < 4)
		  {
			ini = j;
			fin = j;
			m = m_temp;
			break;
		  }
		}//verifica se algum outro meteoro entrou no raio entr o canhao e m

		float x1=m.getX();
		float y1=m.getY();

		float x2=MainActivity. canhao.getX();
		float y2=MainActivity. canhao.getY();

		float dist=(float)Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));

		this.setWidth(dist);
		alvo.setPosition(m);

		if (!alvo.isAnimationRunning())
		{
		  float sc_alvo=alvo.getScaleX();
		  alvo.registerEntityModifier(
			new LoopEntityModifier(
			  new ParallelEntityModifier(
				new RotationModifier(1, 0, -360),
				new SequenceEntityModifier(
				  new ScaleModifier(1, sc_alvo, 2f * sc_alvo),
				  new ScaleModifier(1, 2f * sc_alvo, sc_alvo)))));
		  alvo.animate(50);alvo.setVisible(true);
		  alvo.setZIndex(m.getZIndex() + 1);
		  this.getParent().sortChildren();}

	  }
	  else
	  {

		ini = 0;
		fin = Meteoro.meteoro_lista.size(); 
		m = null;
		this.setWidth(MainActivity.CAMERA_WIDTH);
		alvo.setVisible(false);
		alvo.stopAnimation(0);
	  }

	}
	
	
	  this.setVisible(!BotaoDisparo.canhao_desligado);
	
	}
	// TODO: Implement this method
	super.onManagedUpdate(pSecondsElapsed);
  }
  
}
