package br.grassinimoraes.divasteroides;
import org.andengine.entity.text.*;
import org.andengine.opengl.font.*;
import org.andengine.opengl.vbo.*;
import org.andengine.input.touch.*;
import org.andengine.entity.scene.menu.*;
import org.andengine.entity.modifier.*;
import org.andengine.entity.*;
import org.andengine.util.adt.align.*;
import org.andengine.entity.scene.*;

public class BotaoMenu extends Text
{

  private MainActivity atividade;

  private int tipo;

  private boolean liberou=true;

  private SequenceEntityModifier scm;

  private int tipo_df;
  BotaoMenu(int tipo, MainActivity m,  float x, float y, Font fonte,CharSequence c, int q,VertexBufferObjectManager vert){
	super(x,y,fonte,c,q,vert);
	this.tipo=tipo;
	this.atividade=m;
	this.setHorizontalAlign(HorizontalAlign.CENTER);
  }

  @Override
  public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY)
  {
	if(pSceneTouchEvent.isActionUp()&&liberou){
	  liberou=false;
	  scm= new SequenceEntityModifier(new ScaleModifier(.125f,1,.5f),new ScaleModifier(.125f,.5f,1)){
		@Override
		protected void onModifierFinished(IEntity t){
		  liberou=true;
		  switch(tipo){
			case MainActivity.iniciar1:
			  atividade.cena_raiz.getChildScene().reset();
			  atividade.cena_raiz.clearChildScene();
			  atividade.cena_raiz.setChildScene(atividade. construir_nivel());
			  break;
			 case MainActivity.opcoes1:
			  atividade.cena_raiz.getChildScene().reset();
			  atividade.cena_raiz.clearChildScene();
			  atividade.cena_raiz.setChildScene(atividade.criar_menu_opcoes());
			  break;
			 case MainActivity.sair1:
			   atividade.sair_jogo();
			   break;
			 case MainActivity.voltar1:
			  atividade.cena_raiz.getChildScene().reset();
			  atividade.cena_raiz.clearChildScene();
			  atividade.cena_raiz.setChildScene(atividade.criar_menu());
			  break;
			 case MainActivity.continuar1:
			  atividade.controle_e_cena_pause.clearChildScene();
			  atividade.cena_raiz.getChildScene().setIgnoreUpdate(false);
			  break;
			 case MainActivity.voltar_menu1:
			  atividade.salvar_score();
			  atividade.cena_raiz.getChildScene().reset();
			  atividade.resetar_cena();
			  atividade.cena_raiz.setChildScene(atividade.criar_menu());
			  break;
			 case MainActivity.tentar_denovo1:
			  atividade.resetar_cena();
			  atividade.cena_raiz.setChildScene(atividade.construir_nivel());
			  break;
			 case MainActivity.dificuldade1:
			  tipo_df=(tipo_df>=5)?0:tipo_df+1;
			  BotaoMenu.this.setText("DIFICULDADE: "+escolher_dificuldade(tipo_df));
			  break;
			 case MainActivity.vibracao1:
			  atividade.Vibrar=!atividade.Vibrar;
			  BotaoMenu.this.setText("VIBRACAO: "+((atividade.Vibrar)?"Ligada":"Desligada"));
			  break;
			 case MainActivity.mira_laser1:
			   atividade.mostra_mira_laser=!atividade.mostra_mira_laser;
			  BotaoMenu.this.setText("MIRA LASER:  "+ ((atividade.mostra_mira_laser)?"Ligada":"Desligada"));
             case MainActivity.score1:
				  atividade.cena_raiz.getChildScene().reset();
				  atividade.cena_raiz.clearChildScene();
				  atividade.cena_raiz.setChildScene(atividade.criar_menu_pontos());
				  break;
			 case MainActivity.data1:
			      atividade.ordenar_score(0);
			      break;
			 case MainActivity.qtde1:
				  atividade.ordenar_score(1);
				  break;
			 case MainActivity.pontos1:
				  atividade.ordenar_score(2);
				  break;
				  
			 
			  
		  }
		  
		  
		  super.onModifierFinished(t);
		}
	  };
	  scm.setAutoUnregisterWhenFinished(true);
	  this.registerEntityModifier(scm);
	  
	}
	
	
	// TODO: Implement this method
	return super.onAreaTouched(pSceneTouchEvent, pTouchAreaLocalX, pTouchAreaLocalY);
  }
  
  String escolher_dificuldade(int tipo){
	
	
	
		switch(tipo_df){
		  case 0:
			Meteoro.valor_max=500;
			return("Muto Facil");
		  case 1:
			Meteoro.valor_max=1000;
			return("Facil");
		  case 2:
			Meteoro.valor_max=5000;
			return("Medio");
		  case 3:
			Meteoro.valor_max=10000;
			return("Dificil");
		  case 4:
			Meteoro.valor_max=100000;
			return("Muito Dificil");
		  case 5:
			Meteoro.valor_max=1000000;
			return("Insano");
		}
	Meteoro.valor_max=500;
	return("Muto Facil");
  }
  
}
