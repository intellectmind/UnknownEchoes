package cn.kurt6.unknown_echoes.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 通用 Boss 战 BGM:循环播放,Boss 死亡/消失或玩家远离(>56 格)时淡出停止。
 * 由 ClientGameEvents 在玩家靠近存活的主线 Boss(遗忘巨像/深渊观测者/镜像守护者)时启动。
 */
public class BossMusicSound extends AbstractTickableSoundInstance {
    private static final double STOP_DISTANCE_SQR = 56 * 56;
    private final LivingEntity boss;
    private float fade = 1.0F;

    public BossMusicSound(LivingEntity boss, SoundEvent music) {
        super(music, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.boss = boss;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        boolean shouldStop = this.boss.isRemoved() || !this.boss.isAlive()
                || mc.player == null
                || mc.player.distanceToSqr(this.boss) > STOP_DISTANCE_SQR;
        if (shouldStop) {
            this.fade -= 0.05F;
            this.volume = Math.max(0.0F, this.fade);
            if (this.fade <= 0.0F) {
                this.stop();
            }
        } else {
            this.fade = Math.min(1.0F, this.fade + 0.05F);
            this.volume = this.fade;
        }
    }
}
