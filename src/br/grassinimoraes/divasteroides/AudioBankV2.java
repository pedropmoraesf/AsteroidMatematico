package br.grassinimoraes.divasteroides;

import android.util.Log;

import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.audio.sound.SoundManager;

import java.util.HashMap;
import java.util.Map;

final class AudioBankV2 {
    static final String SIREN = "sirene_80bpm_10.wav";
    static final String SHOT = "disparo_canhao.wav";
    static final String DIV_OK = "divisao_correta.wav";
    static final String DIV_BAD = "divisao_errada.wav";
    static final String EXPLOSION = "explosao_meteoro.wav";
    static final String BIG_EXPLOSION = "explosao_grande.wav";
    static final String BOMB_ZERO = "bomba_zero.wav";
    static final String CITY_HIT = "impacto_cidade.wav";
    static final String BONUS_AMMO = "bonus_municao.wav";
    static final String BONUS_MONEY = "bonus_dinheiro.wav";
    static final String BONUS_HEALTH = "bonus_saude.wav";
    static final String BONUS_SHIELD = "bonus_escudo.wav";
    static final String BONUS_SUB = "bonus_subtrator.wav";
    static final String AMMO_UNLOCK = "municao_desbloqueada.wav";
    static final String SUBTRACT = "subtrator_uso.wav";
    static final String QUIZ_OPEN = "quiz_abre.wav";
    static final String QUIZ_OK = "quiz_acerto.wav";
    static final String QUIZ_BAD = "quiz_erro.wav";
    static final String TARGET_LOCK = "alvo_trava.wav";
    static final String MILITARY_FLYBY = "aviao_militar_passagem.wav";
    static final String MILITARY_BOMB = "aviao_militar_bomba.wav";
    static final String COMMERCIAL_FLYBY = "aviao_comercial_passagem.wav";
    static final String COMMERCIAL_HIT = "aviao_comercial_atingido.wav";
    static final String METEOR_IN = "meteoro_entrada.wav";
    static final String RAIN_START = "chuva_meteoros_inicio.wav";
    static final String UI_CLICK = "ui_click.wav";
    static final String WAVE_DONE = "fase_concluida.wav";
    static final String GAME_OVER = "game_over.wav";
    static final String REPAIR = "reconstrucao_cidade.wav";
    static final String ADD_METEOR = "meteoro_adicao.wav";
    static final String MULT_METEOR = "meteoro_multiplicacao.wav";

    private static final String[] FILES = {
            SIREN, SHOT, DIV_OK, DIV_BAD, EXPLOSION, BIG_EXPLOSION, BOMB_ZERO, CITY_HIT,
            BONUS_AMMO, BONUS_MONEY, BONUS_HEALTH, BONUS_SHIELD, BONUS_SUB, AMMO_UNLOCK,
            SUBTRACT, QUIZ_OPEN, QUIZ_OK, QUIZ_BAD, TARGET_LOCK, MILITARY_FLYBY,
            MILITARY_BOMB, COMMERCIAL_FLYBY, COMMERCIAL_HIT, METEOR_IN, RAIN_START,
            UI_CLICK, WAVE_DONE, GAME_OVER, REPAIR, ADD_METEOR, MULT_METEOR
    };

    private final Map<String, Sound> sounds = new HashMap<String, Sound>();

    void load(SoundManager manager, GameActivityV2 activity) {
        SoundFactory.setAssetBasePath("audio/");
        for (String file : FILES) {
            try {
                Sound sound = SoundFactory.createSoundFromAsset(manager, activity, file);
                sounds.put(file, sound);
            } catch (Exception e) {
                Log.w("AudioBankV2", "Nao foi possivel carregar " + file + ": " + e.getMessage());
            }
        }
    }

    void play(String file) {
        play(file, 1f);
    }

    void play(String file, float volume) {
        Sound sound = sounds.get(file);
        if (sound == null) return;
        try {
            sound.setVolume(Math.max(0f, Math.min(1f, volume)));
            sound.setLooping(false);
            sound.play();
        } catch (Exception e) {
            Log.w("AudioBankV2", "Falha ao tocar " + file + ": " + e.getMessage());
        }
    }

    void stop(String file) {
        Sound sound = sounds.get(file);
        if (sound == null) return;
        try {
            sound.stop();
        } catch (Exception ignored) {
        }
    }
}
