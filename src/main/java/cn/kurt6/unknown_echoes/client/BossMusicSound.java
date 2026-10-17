package cn.kurt6.unknown_echoes.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** 非位置音乐使用 MUSIC 音量通道；幅度曲线保留双曲过渡的总能量。 */
public class BossMusicSound extends AbstractTickableSoundInstance {
    static final int FADE_TICKS = 24;
    private float startGain;
    private int fadeAge;
    private boolean retiring;

    public BossMusicSound(SoundEvent music) {
        super(music, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    public void fadeOut() {
        this.startGain = this.volume;
        this.fadeAge = 0;
        this.retiring = true;
    }

    public void fadeIn() {
        this.startGain = this.volume;
        this.fadeAge = 0;
        this.retiring = false;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        this.fadeAge = Math.min(FADE_TICKS, this.fadeAge + 1);
        double angle = this.fadeAge * Math.PI / (2.0D * FADE_TICKS);
        double remaining = Math.cos(angle);
        double entering = Math.sin(angle);
        this.volume = this.retiring ? (float) (this.startGain * remaining)
                : (float) Math.sqrt(this.startGain * this.startGain * remaining * remaining + entering * entering);
        if (this.retiring && this.fadeAge == FADE_TICKS) {
            this.volume = 0.0F;
            this.stop();
        }
    }
}
