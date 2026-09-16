package br.grassinimoraes.divasteroides;

import android.opengl.GLES20;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import org.andengine.engine.Engine;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.ColorModifier;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.RotationModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.particle.IEntityFactory;
import org.andengine.entity.particle.ParticleSystem;
import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.CircleOutlineParticleEmitter;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer;
import org.andengine.entity.particle.initializer.AlphaParticleInitializer;
import org.andengine.entity.particle.initializer.BlendFunctionParticleInitializer;
import org.andengine.entity.particle.initializer.ColorParticleInitializer;
import org.andengine.entity.particle.initializer.ExpireParticleInitializer;
import org.andengine.entity.particle.initializer.RotationParticleInitializer;
import org.andengine.entity.particle.initializer.VelocityParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ColorParticleModifier;
import org.andengine.entity.particle.modifier.RotationParticleModifier;
import org.andengine.entity.particle.modifier.ScaleParticleModifier;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.adt.align.HorizontalAlign;

import java.util.ArrayList;
import java.util.Random;

public class Meteoro extends AnimatedSprite {

    static int qtde_meteoro_destruida;
    static final ArrayList<Meteoro> meteoro_lista = new ArrayList<Meteoro>();
    static int valor_max = GameRules.MAX_METEOR_VALUE; // mantido por compatibilidade com o codigo antigo
    public static boolean cidade_intacta;

    int valor;
    int pontuacao;
    public boolean dividiu;
    public int disparos;

    private final Random aleatorio = new Random();
    private final TiledTextureRegion text;
    private final int valorOriginal;
    private final int fatoresOriginais;

    private FixtureDef metf;
    private PhysicsConnector fc_met;
    private Body metb;
    private PhysicsWorld mundo;
    private Engine motor;
    private MainActivity atividade;

    private float velocidade_x;
    private float velocidade_y;
    private int largura;
    private int comprimento;

    Text valor_visivel;
    private Font fonte;

    private boolean ja_destruido;
    private boolean atingiu_cidade;
    private boolean atingiu_canhao;
    private boolean setar_score;
    private int disparosErrados;
    private boolean usouBonus;
    private int plus;

    private Sprite efeitos;
    private PointParticleEmitter ponto;
    private ParticleSystem pedacos;
    private CircleOutlineParticleEmitter particleEmitter;
    private SpriteParticleSystem explosao;

    Meteoro(float x, float y, int qtde_met_dest, TiledTextureRegion text, VertexBufferObjectManager vert) {
        super(x, y, text, vert);
        this.text = text;
        this.valor = GameRules.generateMeteorValue(aleatorio, qtde_met_dest);
        this.valorOriginal = this.valor;
        this.fatoresOriginais = Math.max(1, GameRules.primeFactorCount(this.valor));
        this.pontuacao = GameRules.scoreForMeteor(this.valorOriginal, 0, false);
        meteoro_lista.add(this);

        this.setScale(GameRules.meteorScaleFor(this.valor));
        this.setCurrentTileIndex(aleatorio.nextInt(4));
    }

    static void resetar_variaveis_estaticas() {
        qtde_meteoro_destruida = 0;
        meteoro_lista.clear();
        cidade_intacta = true;
        valor_max = GameRules.MAX_METEOR_VALUE;
    }

    int getValor() {
        return valor;
    }

    int getValorOriginal() {
        return valorOriginal;
    }

    void construir_corpo(MainActivity atividade, int cameraWidth, int cameraHeight) {
        ja_destruido = false;
        atingiu_cidade = false;
        atingiu_canhao = false;
        this.atividade = atividade;
        this.mundo = atividade.mundo;
        this.motor = atividade.motor;
        this.metf = atividade.metf;
        this.largura = cameraWidth;
        this.comprimento = cameraHeight;

        metb = PhysicsFactory.createBoxBody(mundo, this, BodyDef.BodyType.DynamicBody, metf);
        fc_met = new PhysicsConnector(this, metb);
        mundo.registerPhysicsConnector(fc_met);
        metb.setUserData(this);

        float baseSpeed = .52f + (float) Math.abs(Math.sin(Math.PI / (2 + Math.log(Math.max(2, valor)))));
        velocidade_y = baseSpeed * GameRules.getMeteorSpeedMultiplier();
        float destinoX = aleatorio.nextInt(cameraWidth);
        velocidade_x = (destinoX - this.getX()) / (cameraHeight / Math.max(.20f, velocidade_y));

        this.registerEntityModifier(new LoopEntityModifier(
                new RotationModifier(1f, 0, (velocidade_x < 0) ? -360 : 360)));
    }

    void mudar_tipo(final int tipo) {
        this.setCurrentTileIndex(tipo);
        meteoro_lista.remove(this);
        this.setScale(2.5f);
        this.clearEntityModifiers();
        this.registerEntityModifier(new LoopEntityModifier(
                new SequenceEntityModifier(
                        new ScaleModifier(.45f, 2.3f, 2.8f),
                        new ScaleModifier(.45f, 2.8f, 2.3f))));
        atualizarTextoVisivel();
    }

    void destruir_corpo(Boolean contabilizarEfeito) {
        if (ja_destruido) {
            return;
        }
        ja_destruido = true;
        this.setar_score = contabilizarEfeito != null && contabilizarEfeito;

        animar_explosoes(this);

        this.clearUpdateHandlers();
        this.clearEntityModifiers();
        this.setVisible(false);
        if (this.hasParent()) {
            this.detachSelf();
        }
        meteoro_lista.remove(this);

        try {
            if (mundo != null && fc_met != null) {
                mundo.unregisterPhysicsConnector(fc_met);
            }
            if (mundo != null && metb != null) {
                metb.setActive(false);
                mundo.destroyBody(metb);
            }
        } catch (Exception e) {
            Log.w("Meteoro", "Falha ao remover corpo fisico: " + e.getMessage());
        }

        if (valor_visivel != null) {
            valor_visivel.clearUpdateHandlers();
            valor_visivel.clearEntityModifiers();
            if (valor_visivel.hasParent()) {
                valor_visivel.detachSelf();
            }
        }
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (metb != null && !ja_destruido) {
            metb.setLinearVelocity(velocidade_x, -velocidade_y);
        }

        atualizarTextoVisivel();

        if (!ja_destruido && atividade != null && atividade.canhao != null && this.collidesWith(atividade.canhao)) {
            atingir_canhao(atividade.canhao);
        }

        super.onManagedUpdate(pSecondsElapsed);
    }

    private void atingir_canhao(Canhao canhao) {
        if (atingiu_canhao || ja_destruido || getCurrentTileIndex() >= 4) {
            return;
        }
        atividade.cidade_grande.atingir_cidade(1, this);
        canhao.desativar_canhao(this);
        atingiu_canhao = true;
        motor.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                destruir_corpo(false);
            }
        });
    }

    void valor_visivel(Font fonte) {
        this.fonte = fonte;
        valor_visivel = new Text(0, 0, fonte, String.valueOf(valor), 100, this.getVertexBufferObjectManager());
        valor_visivel.setScale(2);
        valor_visivel.setPosition(this);
        valor_visivel.setHorizontalAlign(HorizontalAlign.CENTER);
        atualizarTextoVisivel();
    }

    private void atualizarTextoVisivel() {
        if (valor_visivel == null) {
            return;
        }
        int tipo = getCurrentTileIndex();
        if (tipo < 4) {
            valor_visivel.setText(String.valueOf(valor));
        } else if (tipo == 4) {
            valor_visivel.setText("+10");
        } else if (tipo == 5) {
            valor_visivel.setText("ESCUDO");
        } else if (tipo == 6) {
            valor_visivel.setText("$25");
        } else {
            valor_visivel.setText("BOMBA");
        }
        valor_visivel.setPosition(this);
        valor_visivel.setAutoWrapWidth(Math.max(40, this.getWidthScaled() * 2));
    }

    public void atingir_meteoro(final Projetil proj, final Cidade cid) {
        if (ja_destruido) {
            return;
        }

        motor.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                if (ja_destruido) {
                    return;
                }

                if (getCurrentTileIndex() >= 4) {
                    proj.resetar_pos();
                    escolher_itens(cid);
                    return;
                }

                final int divisor = proj.divisor;
                proj.resetar_pos();

                if (divisor == 0) {
                    usouBonus = true;
                    destruicao_nuclear(cid);
                    return;
                }

                if (divisor == 1) {
                    usouBonus = true;
                    atividade.qtde_met_dest += 1;
                    atividade.texto_animado("Bomba de precisao!", 1, .75f, 0, Meteoro.this, 0);
                    destruir_corpo(false);
                    return;
                }

                final int antes = valor;
                if (GameRules.canDivide(valor, divisor)) {
                    dividiu = true;
                    disparos += 1;
                    valor /= divisor;
                    efeitoDaDivisao(true);
                    atividade.texto_animado(antes + " / " + divisor + " = " + valor,
                            .25f, 1f, .25f, Meteoro.this, 0);

                    float novaEscala = GameRules.meteorScaleFor(Math.max(GameRules.MIN_METEOR_VALUE, valor));
                    setScale(Math.max(3.0f, novaEscala));

                    if (valor <= 1) {
                        concluirDestruicaoMatematica(cid);
                    }
                } else {
                    dividiu = false;
                    disparosErrados += 1;
                    efeitoDaDivisao(false);
                    atividade.texto_animado(divisor + " nao divide " + antes,
                            1f, .2f, .2f, Meteoro.this, 0);
                    atividade.vibrar(25);
                }
            }
        });
    }

    private void concluirDestruicaoMatematica(Cidade cid) {
        atividade.qtde_met_dest += 1;
        Meteoro.qtde_meteoro_destruida += 1;
        pontuacao = GameRules.scoreForMeteor(valorOriginal, disparosErrados, usouBonus);

        if (pontuacao > 0) {
            atividade.setar_score(this);
        }

        if (disparosErrados == 0 && disparos == fatoresOriginais) {
            atividade.texto_animado("Fatoracao perfeita!", .2f, .8f, 1f, null, 1);
        } else if (Meteoro.qtde_meteoro_destruida > 0 && Meteoro.qtde_meteoro_destruida % 10 == 0) {
            atividade.texto_animado("10 meteoros! Continue assim!", .2f, .8f, 1f, null, 1);
        }

        if (sortearItem(cid)) {
            return;
        }

        destruir_corpo(true);
    }

    private boolean sortearItem(Cidade cid) {
        if (aleatorio.nextFloat() >= GameRules.itemDropChance(cid.saude)) {
            return false;
        }

        int tipo;
        int sorteio = aleatorio.nextInt(100);
        if (cid.saude <= 35 && sorteio < 55) {
            tipo = 6; // dinheiro/reconstrucao
        } else if (sorteio < 35) {
            tipo = 4; // reparo rapido
        } else if (sorteio < 55) {
            tipo = 5; // escudo
        } else if (sorteio < 82) {
            tipo = 6; // dinheiro/reconstrucao
        } else {
            tipo = 7; // bomba
        }

        mudar_tipo(tipo);
        return true;
    }

    public void destruir_meteoro_e_atingir_cidade(final Cidade cidade) {
        if (ja_destruido) {
            return;
        }
        motor.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                if (ja_destruido) {
                    return;
                }
                if (getCurrentTileIndex() < 4) {
                    atingiu_cidade = true;
                    cidade.atingir_cidade(0, Meteoro.this);
                    Meteoro.qtde_meteoro_destruida = 0;
                    Meteoro.cidade_intacta = false;
                    destruir_corpo(true);
                } else {
                    escolher_itens(cidade);
                }
            }
        });
    }

    void escolher_itens(Cidade cidade) {
        if (ja_destruido) {
            return;
        }

        int tipo = this.getCurrentTileIndex();
        cidade.aumentou_saude = false;
        switch (tipo) {
            case 4:
                plus = 10;
                cidade.recuperarSaude(plus);
                atividade.texto_animado("Reparo +10", .2f, 1f, .2f, this, 0);
                break;
            case 5:
                atividade.campo_forca.ativar(cidade);
                atividade.texto_animado("Escudo ativado", .2f, .8f, 1f, this, 0);
                break;
            case 6:
                plus = 25;
                cidade.recuperarSaude(plus);
                atividade.texto_animado("Reconstrucao +25", 1f, .85f, .1f, this, 0);
                break;
            case 7:
                if (aleatorio.nextBoolean()) {
                    atividade.bomba_um = true;
                    atividade.texto_animado("Bomba de precisao", 1f, .65f, .1f, this, 0);
                } else {
                    atividade.bomba_tudo = true;
                    atividade.texto_animado("Bomba nuclear", 1f, .35f, .1f, this, 0);
                }
                break;
            default:
                return;
        }
        destruir_corpo(true);
    }

    private void efeitoDaDivisao(boolean correto) {
        float r = correto ? .25f : 1f;
        float g = correto ? 1f : .15f;
        float b = correto ? .25f : .15f;

        this.clearEntityModifiers();
        this.registerEntityModifier(new SequenceEntityModifier(
                new ColorModifier(.08f, getRed(), r, getGreen(), g, getBlue(), b),
                new ColorModifier(.18f, r, 1f, g, 1f, b, 1f),
                new LoopEntityModifier(new RotationModifier(1f, 0, (velocidade_x < 0) ? -360 : 360), 1)));
    }

    void animar_explosoes(Meteoro met) {
        final float x = met.getX();
        final float y = met.getY();
        final float escala = Math.max(1.5f, met.getScaleX());
        int tipo = met.getCurrentTileIndex();

        if (tipo < 4) {
            if (setar_score) {
                String texto = atingiu_cidade
                        ? ("-" + String.format("%.0f", GameRules.cityDamageFor(valorOriginal)))
                        : (pontuacao > 0 ? ("+" + pontuacao) : "");
                if (texto.length() > 0) {
                    atividade.texto_animado(texto, 1, atingiu_cidade ? .15f : 1f, 0, this, 0);
                }
            }

            IEntityFactory<Sprite> pedacosFactory = new IEntityFactory<Sprite>() {
                @Override
                public Sprite create(float pX, float pY) {
                    AnimatedSprite fragmento = new AnimatedSprite(x, y, text, motor.getVertexBufferObjectManager());
                    fragmento.animate(new long[]{50, 50, 50, 50}, 0, 3, true);
                    fragmento.setScale(.65f);
                    return fragmento;
                }
            };
            explodir(x, y, escala, 1, .35f, .05f, pedacosFactory);
        } else {
            final int tile = tipo;
            IEntityFactory<Sprite> itemFactory = new IEntityFactory<Sprite>() {
                @Override
                public Sprite create(float pX, float pY) {
                    AnimatedSprite item = new AnimatedSprite(x, y, text, motor.getVertexBufferObjectManager());
                    item.setCurrentTileIndex(tile);
                    item.setScale(.7f);
                    return item;
                }
            };
            explodir(x, y, 2.5f, .15f, .8f, 1f, itemFactory);
        }
    }

    void explodir(final float x, final float y, float escala,
                  float r, float g, float b, IEntityFactory<Sprite> fabrica) {
        try {
            efeitos = new Sprite(x, y, atividade.efeito, this.getVertexBufferObjectManager());
            if (this.getParent() != null) {
                this.getParent().attachChild(efeitos);
            } else if (atividade.cena_raiz.getChildScene() != null) {
                atividade.cena_raiz.getChildScene().attachChild(efeitos);
            }
            efeitos.setColor(r, g, b);
            efeitos.registerEntityModifier(new ParallelEntityModifier(
                    new ScaleModifier(.65f, .25f, Math.max(2f, escala)),
                    new AlphaModifier(.75f, 1, 0)));

            particleEmitter = new CircleOutlineParticleEmitter(x, y, Math.max(6, escala * 1.2f));
            explosao = new SpriteParticleSystem(particleEmitter, 8, 28, 28, atividade.fumaca,
                    this.getVertexBufferObjectManager());
            explosao.addParticleInitializer(new ColorParticleInitializer<Sprite>(r, g, b));
            explosao.addParticleInitializer(new AlphaParticleInitializer<Sprite>(.85f));
            explosao.addParticleInitializer(new BlendFunctionParticleInitializer<Sprite>(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE));
            explosao.addParticleInitializer(new VelocityParticleInitializer<Sprite>(-18, 18, -10, 28));
            explosao.addParticleInitializer(new RotationParticleInitializer<Sprite>(0, 360));
            explosao.addParticleInitializer(new ExpireParticleInitializer<Sprite>(1.4f));
            explosao.addParticleModifier(new ScaleParticleModifier<Sprite>(0, .8f, .35f, 1.25f));
            explosao.addParticleModifier(new AlphaParticleModifier<Sprite>(.5f, 1.4f, .85f, 0));

            ponto = new PointParticleEmitter(x, y);
            int maxParticulas = Math.max(10, Math.min(42, Math.round(5 * escala)));
            pedacos = new ParticleSystem<Sprite>(fabrica, ponto, 8, 24, maxParticulas);
            pedacos.addParticleInitializer(new VelocityParticleInitializer<Sprite>(-55, 55, -20, 80));
            pedacos.addParticleInitializer(new AccelerationParticleInitializer<Sprite>(0, -24));
            pedacos.addParticleModifier(new RotationParticleModifier<Sprite>(0, 1.8f, 0, 360));
            pedacos.addParticleInitializer(new ExpireParticleInitializer<Sprite>(2.2f));
            pedacos.addParticleModifier(new AlphaParticleModifier<Sprite>(1.0f, 2.2f, 1, 0));

            if (!explosao.hasParent()) {
                atividade.cena_raiz.getChildScene().attachChild(explosao);
            }
            if (!pedacos.hasParent()) {
                atividade.cena_raiz.getChildScene().attachChild(pedacos);
            }

            atividade.destruir_explosao(1.5f, explosao, efeitos);
            atividade.destruir_explosao(2.4f, pedacos, null);
        } catch (Exception e) {
            Log.e("Meteoro", "Erro em particulas: " + e.getMessage());
        }
    }

    private void destruicao_nuclear(final Cidade cidade) {
        SequenceEntityModifier flash = new SequenceEntityModifier(
                new AlphaModifier(.18f, 0, .9f),
                new AlphaModifier(.55f, .9f, 0)) {
            @Override
            protected void onModifierStarted(IEntity e) {
                e.setVisible(true);
                e.setPosition(largura / 2, comprimento / 2);
                super.onModifierStarted(e);
            }

            @Override
            protected void onModifierFinished(IEntity e) {
                e.setVisible(false);
                e.setPosition(-largura, -comprimento);
                super.onModifierFinished(e);
            }
        };
        flash.setAutoUnregisterWhenFinished(true);
        atividade.efeito_rect.registerEntityModifier(flash);

        cidade.atingir_cidade(2, null);

        ArrayList<Meteoro> copia = new ArrayList<Meteoro>(meteoro_lista);
        int destruidos = 0;
        for (Meteoro meteoro : copia) {
            if (meteoro != null && !meteoro.ja_destruido && meteoro.getCurrentTileIndex() < 4) {
                meteoro.usouBonus = true;
                meteoro.pontuacao = 0;
                meteoro.destruir_corpo(false);
                destruidos++;
            }
        }
        atividade.qtde_met_dest += destruidos;
        atividade.texto_animado("NUCLEAR: " + destruidos + " meteoros", 1f, .35f, .1f, null, 1);
    }
}
