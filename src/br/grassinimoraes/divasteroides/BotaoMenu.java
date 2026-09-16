package br.grassinimoraes.divasteroides;

import android.content.Context;

import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.align.HorizontalAlign;

public class BotaoMenu extends Text {

    private final MainActivity atividade;
    private final int tipo;
    private boolean liberou = true;
    private int tipo_df;

    BotaoMenu(int tipo, MainActivity atividade, float x, float y, Font fonte,
              CharSequence texto, int maxCaracteres, VertexBufferObjectManager vert) {
        super(x, y, fonte, texto, maxCaracteres, vert);
        this.tipo = tipo;
        this.atividade = atividade;
        this.tipo_df = GameRules.getDifficulty();
        this.setHorizontalAlign(HorizontalAlign.CENTER);

        if (tipo == MainActivity.dificuldade1) {
            this.setText("DIFICULDADE: " + GameRules.getDifficultyName());
        } else if (tipo == MainActivity.vibracao1) {
            this.setText("VIBRACAO: " + (atividade.Vibrar ? "Ligada" : "Desligada"));
        } else if (tipo == MainActivity.mira_laser1) {
            this.setText("MIRA LASER: " + (atividade.mostra_mira_laser ? "Ligada" : "Desligada"));
        }
    }

    @Override
    public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
        if (!event.isActionUp() || !liberou) {
            return true;
        }

        liberou = false;
        SequenceEntityModifier animacao = new SequenceEntityModifier(
                new ScaleModifier(.125f, 1, .5f),
                new ScaleModifier(.125f, .5f, 1)) {
            @Override
            protected void onModifierFinished(IEntity item) {
                liberou = true;
                executarAcao();
                super.onModifierFinished(item);
            }
        };
        animacao.setAutoUnregisterWhenFinished(true);
        this.registerEntityModifier(animacao);
        return true;
    }

    private void executarAcao() {
        switch (tipo) {
            case MainActivity.iniciar1:
                atividade.cena_raiz.getChildScene().reset();
                atividade.cena_raiz.clearChildScene();
                atividade.cena_raiz.setChildScene(atividade.construir_nivel());
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
                tipo_df = (tipo_df >= 5) ? 0 : tipo_df + 1;
                GameRules.setDifficulty(tipo_df);
                this.setText("DIFICULDADE: " + GameRules.getDifficultyName());
                break;

            case MainActivity.vibracao1:
                atividade.Vibrar = !atividade.Vibrar;
                this.setText("VIBRACAO: " + (atividade.Vibrar ? "Ligada" : "Desligada"));
                break;

            case MainActivity.mira_laser1:
                atividade.mostra_mira_laser = !atividade.mostra_mira_laser;
                this.setText("MIRA LASER: " + (atividade.mostra_mira_laser ? "Ligada" : "Desligada"));
                break;

            case MainActivity.apagar_pontos1:
                atividade.getSharedPreferences("pontuacao", Context.MODE_PRIVATE)
                        .edit().clear().commit();
                this.setText("SCORE APAGADO");
                break;

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

            default:
                break;
        }
    }
}
