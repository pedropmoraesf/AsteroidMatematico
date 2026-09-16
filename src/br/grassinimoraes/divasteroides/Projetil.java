package br.grassinimoraes.divasteroides;
import org.andengine.entity.sprite.*;
import org.andengine.opengl.texture.region.*;
import org.andengine.opengl.vbo.*;
import org.andengine.extension.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.*;
import java.util.*;
import com.badlogic.gdx.math.*;
import org.andengine.extension.physics.box2d.util.constants.*;

public class Projetil extends AnimatedSprite
{

	 int divisor;

	private FixtureDef projf;

	Body projb;

	private PhysicsConnector fc_proj;

	private float velocidade_x;

	private float velocidade_y;

	private PhysicsWorld mundo;
	
	static ArrayList<Projetil> projetil_lista=new ArrayList<Projetil>();
	
	static Vector2 pos_ini;

	float modulo_velocidade;
	
	Projetil(float x, float y, int divisor, TiledTextureRegion text, VertexBufferObjectManager vert){
		super(x,y,text,vert);
		projetil_lista.add(this);
		this.divisor=divisor;
	}
  
  static void resetar_variaveis_estaticas(){
	if(projetil_lista.size()>0){projetil_lista.removeAll(projetil_lista);}
	pos_ini=null;
  }
	
	void construir_corpo(PhysicsWorld mundo){
		this.mundo=mundo;
		projf = PhysicsFactory.createFixtureDef(0,0.2f,0);
		projb=PhysicsFactory.createBoxBody(mundo,this,BodyDef.BodyType.KinematicBody,projf);
		fc_proj=new PhysicsConnector(this,projb);
	    mundo.registerPhysicsConnector(fc_proj);
		pos_ini=projb.getPosition();
		projb.setUserData(this);
		

	}
	
	void setar_velocidade(float velocidade,float angulo){
	   // this.setVisible(true);
		this.modulo_velocidade=velocidade;
		projb.setTransform(new Vector2(MainActivity.CAMERA_WIDTH/(2*PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT),MainActivity.CAMERA_HEIGHT/(2*PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT)),0);
		velocidade_x=(float)(velocidade*Math.cos(Math.PI*angulo/180));
		velocidade_y=-(float)(velocidade*Math.sin(Math.PI*angulo/180));
		
	    projb.setTransform(projb.getPosition(),-(float)Math.PI*angulo/180);
		projb.setLinearVelocity(velocidade_x,velocidade_y);
		
	}
	
	void resetar_pos(){
		this.modulo_velocidade=0;
		//this.setVisible(false);
		projb.setLinearVelocity(0,0);
		projb.setTransform(new Vector2(-MainActivity.CAMERA_WIDTH/(PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT),-MainActivity.CAMERA_HEIGHT/(PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT)),0);
		
	}

	void destruir_corpo(PhysicsWorld mundo){

		this.clearUpdateHandlers();
		this.clearEntityModifiers();
		this.detachSelf();
		mundo.unregisterPhysicsConnector(fc_proj);
		mundo.destroyBody(projb);
		projb.setActive(false);
		

	}
	
	void destruir_corpo(){
		
		this.destruir_corpo(mundo);
		

	}
	@Override
	protected void onManagedUpdate(float pSecondsElapsed){

     	if( projb!=null&&this.modulo_velocidade!=0&&(this.getX()<0||this.getX()>MainActivity.CAMERA_WIDTH||this.getY()>MainActivity.CAMERA_HEIGHT||this.getY()<0)){
			this.resetar_pos();		
		}
		

		super.onManagedUpdate(pSecondsElapsed);
	}
}
