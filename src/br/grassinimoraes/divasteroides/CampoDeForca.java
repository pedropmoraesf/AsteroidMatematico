package br.grassinimoraes.divasteroides;

import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.ColorModifier;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.align.HorizontalAlign;

import java.util.ArrayList;

public class CampoDeForca extends Rectangle {

    private Rectangle barra_saude;
    private Rectangle barra_saude_dinamica;
    private float comprimento;
    private Text saude_visivel;

    private float saude = 100;
    private final float pos_x_def;
    private final float pos_y_def;
    private boolean ativado;

    private final MainActivity atividade;
    private Cidade cidade;

    CampoDeForca(float x, float y, float largura, float altura,
                 float posx, float posy, MainActivity atividade, VertexBufferObjectManager vert) {
        super(x, y, largura, altura, vert);
        this.pos_x_def = posx;
        this.pos_y_def = posy;
        this.atividade = atividade;
        this.setAnchorCenter(.5f, .5f);
        this.setColor(.15f, .70f, 1f);
        this.setAlpha(.75f);
        this.setVisible(false);

        this.registerEntityModifier(new LoopEntityModifier(
                new ParallelEntityModifier(
                        new ColorModifier(1.2f, .15f, .55f, .70f, 1f, 1f, 1f),
                        new AlphaModifier(1.2f, .45f, .85f))));
    }

    public void ativar(Cidade cidade) {
        this.cidade = cidade;
        this.saude = 100;
        this.ativado = true;
        this.setVisible(true);
        this.setPosition(pos_x_def, pos_y_def);
        this.setScale(0.05f, 1f);
        this.registerEntityModifier(new ScaleModifier(.35f, .05f, 1f, 1f, 1f));
        if (barra_saude != null) {
            barra_saude.setVisible(true);
        }
        atividade.texto_animado("ESCUDO 100%", .2f, .8f, 1f, null, 1);
    }

    private void desativar() {
        if (!ativado) {
            return;
        }
        ativado = false;
        if (barra_saude != null) {
            barra_saude.setVisible(false);
        }
        this.setVisible(false);
        this.setPosition(-100, -100);
        atividade.texto_animado("Escudo esgotado", 1f, .45f, .1f, null, 1);
    }

    void criar_barra_saude(HUD hud, float x, float y, float largura, float altura, Font fonte) {
        barra_saude = new Rectangle(0, 0, largura, altura, this.getVertexBufferObjectManager());
        barra_saude.setColor(.05f, .15f, .25f);
        barra_saude.setAlpha(.70f);
        hud.attachChild(barra_saude);
        barra_saude.setPosition(x, y);

        barra_saude_dinamica = new Rectangle(barra_saude.getWidth(), barra_saude.getHeight() / 2,
                barra_saude.getWidth(), barra_saude.getHeight(), this.getVertexBufferObjectManager());
        barra_saude_dinamica.setAnchorCenter(1, .5f);
        barra_saude_dinamica.setColor(.15f, .75f, 1f);
        barra_saude_dinamica.setAlpha(.9f);
        barra_saude.attachChild(barra_saude_dinamica);
        this.comprimento = barra_saude_dinamica.getWidth();

        saude_visivel = new Text(0, 0, fonte, "ESCUDO 100%", 100, this.getVertexBufferObjectManager());
        saude_visivel.setScale(.55f);
        saude_visivel.setPosition(barra_saude.getWidth() / 2, barra_saude.getHeight() / 2);
        saude_visivel.setHorizontalAlign(HorizontalAlign.CENTER);
        barra_saude.attachChild(saude_visivel);
        barra_saude.setVisible(false);
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (ativado) {
            // Duracao aproximada de 25 s sem impactos, independente do FPS.
            saude -= 4f * pSecondsElapsed;

            ArrayList<Meteoro> copia = new ArrayList<Meteoro>(Meteoro.meteoro_lista);
            for (final Meteoro meteoro : copia) {
                if (meteoro != null && meteoro.getCurrentTileIndex() < 4 && this.collidesWith(meteoro)) {
                    saude -= Math.max(8f, GameRules.cityDamageFor(meteoro.getValor()) * .75f);
                    if (cidade != null) {
                        cidade.atingir_cidade(1, meteoro);
                    }
                    atividade.runOnUpdateThread(new Runnable() {
                        @Override
                        public void run() {
                            meteoro.destruir_corpo(false);
                        }
                    });
                }
            }

            if (barra_saude_dinamica != null) {
                saude = Math.max(0, Math.min(100, saude));
                saude_visivel.setText("ESCUDO " + String.format("%.0f", saude) + "%");
                barra_saude_dinamica.setWidth(comprimento * saude / 100f);
                if (saude > 45) {
                    barra_saude_dinamica.setColor(.15f, .75f, 1f);
                } else {
                    barra_saude_dinamica.setColor(1f, .55f, .10f);
                }
            }

            if (saude <= 0) {
                desativar();
            }
        }

        super.onManagedUpdate(pSecondsElapsed);
    }
}
