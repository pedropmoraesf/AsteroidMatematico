package br.grassinimoraes.divasteroides;

import org.andengine.engine.camera.hud.HUD;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.particle.ParticleSystem;
import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.RectangleParticleEmitter;
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer;
import org.andengine.entity.particle.initializer.ColorParticleInitializer;
import org.andengine.entity.particle.initializer.ExpireParticleInitializer;
import org.andengine.entity.particle.initializer.RotationParticleInitializer;
import org.andengine.entity.particle.initializer.VelocityParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ColorParticleModifier;
import org.andengine.entity.particle.modifier.ScaleParticleModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.align.HorizontalAlign;

import java.util.ArrayList;
import java.util.Random;

public class Cidade extends Sprite {
    float saude = 100;
    public boolean aumentou_saude;

    private final MainActivity atividade;
    private final Random aleatorio = new Random();
    private final ArrayList<ParticleSystem> fogo_lista = new ArrayList<ParticleSystem>();

    private Rectangle barra_saude;
    private Rectangle barra_saude_dinamica;
    private Text saude_visivel;
    private float comprimento;

    private float y_cam_ini;
    private float pos_y;
    private float fator_gama;
    private float escala_meteoro;
    private boolean terremoto;
    private int t1;
    private int t2;

    private boolean atingida;
    private float mx;
    private float proximoLimiarIncendio = 75f;

    Cidade(float x, float y, TextureRegion text, MainActivity atividade, VertexBufferObjectManager vert) {
        super(x, y, text, vert);
        this.atividade = atividade;
        this.y_cam_ini = MainActivity.motor.getCamera().getCenterY();
    }

    public void criar_barra_saude(HUD hud, float comprimento, float largura,
                                  float x_barra, float y_barra, Font fonte) {
        barra_saude = new Rectangle(0, 0, largura, comprimento, this.getVertexBufferObjectManager());
        barra_saude.setColor(.35f, .05f, .05f);
        barra_saude.setAlpha(.70f);
        hud.attachChild(barra_saude);
        barra_saude.setPosition(x_barra, y_barra);

        barra_saude_dinamica = new Rectangle(barra_saude.getWidth(), barra_saude.getHeight() / 2,
                barra_saude.getWidth(), barra_saude.getHeight(), this.getVertexBufferObjectManager());
        barra_saude_dinamica.setAnchorCenter(1, .5f);
        barra_saude_dinamica.setColor(0, 1, 0);
        barra_saude_dinamica.setAlpha(.85f);
        barra_saude.attachChild(barra_saude_dinamica);
        this.comprimento = barra_saude_dinamica.getWidth();

        saude_visivel = new Text(0, 0, fonte, "100 %", 100, this.getVertexBufferObjectManager());
        saude_visivel.setPosition(barra_saude.getWidth() / 2, barra_saude.getHeight() / 2);
        saude_visivel.setHorizontalAlign(HorizontalAlign.CENTER);
        barra_saude.attachChild(saude_visivel);
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (terremoto) {
            if (t1 < t2) {
                tremer_cidade(escala_meteoro * 25 / 4, 10, 20, fator_gama * .025f, t1);
                t1++;
            } else {
                t1 = 0;
                terremoto = false;
                atividade.motor.getCamera().setCenter(atividade.motor.getCamera().getCenterX(), y_cam_ini);
            }
        }

        if (atingida) {
            atingida = false;
            while (saude <= proximoLimiarIncendio && proximoLimiarIncendio >= 15f) {
                float x = Math.max(getWidthScaled() * .12f,
                        Math.min(getWidthScaled() * .88f, mx + aleatorio.nextInt(121) - 60));
                incendiar(x, this.getHeightScaled() / 4);
                proximoLimiarIncendio -= 15f;
            }
        }

        if (saude <= 0) {
            saude = 0;
            atividade.motor.getCamera().setCenter(atividade.motor.getCamera().getCenterX(), y_cam_ini);
            if (this.getParent() instanceof Scene) {
                ((Scene) this.getParent()).setIgnoreUpdate(true);
            }
            atividade.controle_e_cena_pause.setChildScene(atividade.fim_de_jogo());
        }

        if (saude > 75 && !fogo_lista.isEmpty()) {
            apagarIncendios(fogo_lista.size());
            proximoLimiarIncendio = 75f;
        }

        atualizarBarraSaude();
        super.onManagedUpdate(pSecondsElapsed);
    }

    private void atualizarBarraSaude() {
        if (barra_saude_dinamica == null) {
            return;
        }

        saude = Math.max(0, Math.min(100, saude));
        saude_visivel.setText(String.format("%.0f %%", saude));
        barra_saude_dinamica.setWidth(comprimento * saude / 100f);

        if (saude > 60) {
            barra_saude_dinamica.setColor(.10f, 1f, .15f);
        } else if (saude > 30) {
            barra_saude_dinamica.setColor(1f, .72f, .05f);
        } else {
            barra_saude_dinamica.setColor(1f, .08f, .05f);
        }
    }

    void recuperarSaude(float pontos) {
        if (pontos <= 0 || saude <= 0) {
            return;
        }
        float antes = saude;
        saude = Math.min(100, saude + pontos);
        aumentou_saude = saude > antes;

        int apagar = Math.max(1, Math.round(pontos / 10f));
        apagarIncendios(apagar);

        if (saude > 75) {
            proximoLimiarIncendio = 75f;
        } else {
            // Permite que novos danos voltem a produzir fogo em limiares ainda nao cruzados.
            proximoLimiarIncendio = Math.min(proximoLimiarIncendio,
                    Math.max(15f, ((float) Math.floor(saude / 15f)) * 15f));
        }
    }

    private void apagarIncendios(int quantidade) {
        int restante = Math.min(quantidade, fogo_lista.size());
        while (restante-- > 0 && !fogo_lista.isEmpty()) {
            ParticleSystem fogo = fogo_lista.remove(fogo_lista.size() - 1);
            if (fogo != null) {
                atividade.destruir_explosao(.8f, fogo, null);
            }
        }
    }

    void tremer_cidade(float alpha, float w, float phi, float gama, int t) {
        pos_y = y_cam_ini + (float) (Math.exp(-gama * t) * alpha * Math.cos(phi * t - w));
        MainActivity.motor.getCamera().setCenter(MainActivity.motor.getCamera().getCenterX(), pos_y);
    }

    void atingir_cidade(int tipo, Meteoro meteoro) {
        if (tipo == 0 && meteoro != null) {
            atingida = true;
            mx = meteoro.getX();
            float dano = GameRules.cityDamageFor(Math.max(2, meteoro.getValor()));
            saude -= dano;
            escala_meteoro = Math.max(2f, meteoro.getScaleX());
            fator_gama = escala_meteoro / 4;
            t1 = 0;
            t2 = Math.round(18 * escala_meteoro);
            terremoto = true;
            atividade.vibrar(Math.round(65 * escala_meteoro));
        } else if (tipo == 1 && meteoro != null) {
            escala_meteoro = Math.max(2f, meteoro.getScaleX());
            fator_gama = Math.max(.35f, escala_meteoro / 2f);
            t1 = 0;
            t2 = Math.round(6f * escala_meteoro);
            terremoto = true;
            atividade.vibrar(Math.round(22 * escala_meteoro));
        } else if (tipo == 2) {
            // A bomba nuclear salva a tela, mas tem custo real para a cidade.
            saude -= 12;
            atingida = true;
            mx = getWidthScaled() / 2;
            escala_meteoro = 5f;
            fator_gama = .75f;
            t1 = 0;
            t2 = 90;
            terremoto = true;
            atividade.vibrar(650);
        }
    }

    void incendiar(float x, float y) {
        RectangleParticleEmitter emissor = new RectangleParticleEmitter(
                x, y, Math.max(20, this.getWidthScaled() / 9), 2);

        SpriteParticleSystem fogo = new SpriteParticleSystem(
                emissor, 12, 28, 64, atividade.fogo1, this.getVertexBufferObjectManager());
        fogo.addParticleInitializer(new VelocityParticleInitializer<Sprite>(-8, 8, 8, 28));
        fogo.addParticleInitializer(new AccelerationParticleInitializer<Sprite>(0, 7));
        fogo.addParticleInitializer(new RotationParticleInitializer<Sprite>(-25, 25));
        fogo.addParticleInitializer(new ColorParticleInitializer<Sprite>(1f, .55f, .05f));
        fogo.addParticleInitializer(new ExpireParticleInitializer<Sprite>(2.4f));
        fogo.addParticleModifier(new ScaleParticleModifier<Sprite>(0, 1.8f, .45f, 1.15f));
        fogo.addParticleModifier(new AlphaParticleModifier<Sprite>(1.0f, 2.4f, 1, 0));
        fogo.addParticleModifier(new ColorParticleModifier<Sprite>(0, 1.3f,
                1f, .55f, .05f, .85f, .08f, .02f));

        fogo_lista.add(fogo);
        if (this.getParent() != null) {
            this.getParent().attachChild(fogo);
        }

        // Pequeno pulso visual no HUD quando um novo foco de incendio aparece.
        if (barra_saude != null) {
            barra_saude.registerEntityModifier(new SequenceEntityModifier(
                    new AlphaModifier(.12f, .70f, 1f),
                    new AlphaModifier(.28f, 1f, .70f)));
        }
    }
}
