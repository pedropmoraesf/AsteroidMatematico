package br.grassinimoraes.divasteroides;

import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.align.HorizontalAlign;

public class BotaoDisparo extends AnimatedSprite {

    static int qtde = 0;
    public static boolean canhao_desligado;

    private boolean cooldown_disp;
    private int tipo;
    private int municao;
    private final AnimatedSprite canhao;
    private final MainActivity atividade;

    BotaoDisparo(float x, float y, TiledTextureRegion text, AnimatedSprite canhao,
                 MainActivity atividade, VertexBufferObjectManager vert) {
        super(x, y, text, vert);
        this.setScale(8f);
        this.canhao = canhao;
        this.atividade = atividade;
    }

    static void resetar_variaveis_estaticas() {
        canhao_desligado = false;
        qtde = 0;
    }

    void setar_interior(int indice, Font fonte) {
        this.tipo = indice;
        this.setAlpha(indice >= 6 ? 0 : .30f);

        if (indice < GameRules.PRIME_AMMO.length) {
            this.municao = GameRules.PRIME_AMMO[indice];
            this.setCurrentTileIndex(0);
            adicionarRotulo(String.valueOf(municao), fonte, .28f);
        } else if (indice == 6) {
            // Bonus de alvo unico: conceitualmente multiplica o meteoro por zero.
            this.municao = 1; // codigo interno reservado para a bomba de precisao
            this.setCurrentTileIndex(1);
            adicionarRotulo("x0", fonte, .20f);
        } else {
            // Bonus nuclear: multiplica todos os meteoros por zero e danifica a cidade.
            this.municao = 0;
            this.setCurrentTileIndex(2);
            adicionarRotulo("x0*", fonte, .16f);
        }
    }

    private void adicionarRotulo(String valor, Font fonte, float escala) {
        Text texto = new Text(0, 0, fonte, valor, 16, this.getVertexBufferObjectManager());
        texto.setPosition(this.getWidth() / 2, this.getHeight() / 2);
        texto.setScale(escala);
        texto.setHorizontalAlign(HorizontalAlign.CENTER);
        this.attachChild(texto);
    }

    @Override
    public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
        if (!event.isActionUp() || cooldown_disp || this.getAlpha() < .23f || canhao_desligado) {
            return true;
        }

        cooldown_disp = true;
        SequenceEntityModifier press = new SequenceEntityModifier(
                new ScaleModifier(.10f, this.getScaleX(), .78f * this.getScaleX()),
                new ScaleModifier(.12f, .78f * this.getScaleX(), this.getScaleX())) {
            @Override
            public void onModifierFinished(IEntity item) {
                cooldown_disp = false;
                super.onModifierFinished(item);
            }
        };
        press.setAutoUnregisterWhenFinished(true);
        this.registerEntityModifier(press);

        animarCanhao();
        disparar();
        return true;
    }

    private void animarCanhao() {
        canhao.animate(new long[]{45, 45, 45, 45, 45, 45, 45, 45}, 0, 7, false,
                new AnimatedSprite.IAnimationListener() {
                    @Override
                    public void onAnimationStarted(AnimatedSprite sprite, int initialLoopCount) {
                    }

                    @Override
                    public void onAnimationFrameChanged(AnimatedSprite sprite, int oldFrameIndex, int newFrameIndex) {
                    }

                    @Override
                    public void onAnimationLoopFinished(AnimatedSprite sprite, int remainingLoopCount, int initialLoopCount) {
                    }

                    @Override
                    public void onAnimationFinished(AnimatedSprite sprite) {
                        sprite.animate(new long[]{50, 50, 50, 50, 50, 50, 50, 50}, 8, 15, true);
                    }
                });
    }

    private void disparar() {
        if (Projetil.projetil_lista.isEmpty()) {
            return;
        }
        if (qtde >= Projetil.projetil_lista.size()) {
            qtde = 0;
        }

        Projetil projetil = Projetil.projetil_lista.get(qtde);
        projetil.setar_velocidade(30, MainActivity.angulo);
        projetil.divisor = municao;

        int tile;
        if (municao == 2) tile = 0;
        else if (municao == 3) tile = 1;
        else if (municao == 5) tile = 2;
        else if (municao == 7) tile = 3;
        else if (municao == 11) tile = 4;
        else if (municao == 13) tile = 5;
        else if (municao == 1) tile = 6;
        else tile = 7;
        projetil.setCurrentTileIndex(tile);

        qtde++;
        atividade.vibrar(municao <= 1 ? 20 : Math.min(70, 5 * municao));

        // Bonus e consumivel: some depois do disparo.
        if (tipo >= 6) {
            AlphaModifier consumir = new AlphaModifier(.35f, this.getAlpha(), 0);
            consumir.setAutoUnregisterWhenFinished(true);
            this.registerEntityModifier(consumir);
        }
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (canhao_desligado && this.getRotation() == 0) {
            this.setRotation(1 + 89 * (float) Math.random());
            this.setColor(.25f, .25f, .25f);
        } else if (!canhao_desligado && this.getRotation() != 0) {
            this.setRotation(0);
            this.setColor(1, 1, 1);
        }

        if (tipo == 6 && MainActivity.bomba_um) {
            MainActivity.bomba_um = false;
            revelarBonus();
        } else if (tipo == 7 && MainActivity.bomba_tudo) {
            MainActivity.bomba_tudo = false;
            revelarBonus();
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    private void revelarBonus() {
        AlphaModifier aparecer = new AlphaModifier(.45f, 0, .30f);
        aparecer.setAutoUnregisterWhenFinished(true);
        this.registerEntityModifier(aparecer);
    }
}
