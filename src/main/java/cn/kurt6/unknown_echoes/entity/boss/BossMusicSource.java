package cn.kurt6.unknown_echoes.entity.boss;

/** 只在服务端读取公开战斗阶段；同步结果不含实体身份或机关答案。 */
public interface BossMusicSource {
    int musicStage();
}
