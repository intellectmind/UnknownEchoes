package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class ThemedEchoArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    public enum Style {
        EXPLORER, ECHO_TRAVELER, LISTENER, WIND_WALKER, TIDE_STALKER,
        TRUE_SIGHT_SHADOW, SILENT_WATCH, ECHO_OATH, RESONATOR
    }

    private ThemedEchoArmorModel(ModelPart root) {
        super(root);
    }

    public static ThemedEchoArmorModel<LivingEntity> create(Style style, EquipmentSlot slot) {
        MeshDefinition mesh = EchoArmorGeometry.anchors();
        PartDefinition root = mesh.getRoot();
        EchoArmorGeometry.Atlas atlas = new EchoArmorGeometry.Atlas(slot);
        switch (slot) {
            case HEAD -> addHelmet(root, style, atlas);
            case CHEST -> {
                ChestShape shape = ChestShape.forStyle(style);
                EchoArmorGeometry.chest(root, atlas, shape.shoulderY(), shape.shoulderRoll(),
                        shape.shoulderDepth(), shape.corePitch());
            }
            case LEGS -> EchoArmorGeometry.leggings(root, atlas,
                    style == Style.WIND_WALKER || style == Style.LISTENER ? 0.12F : 0.06F);
            case FEET -> EchoArmorGeometry.boots(root, atlas,
                    style == Style.WIND_WALKER || style == Style.TIDE_STALKER
                            || style == Style.ECHO_TRAVELER || style == Style.TRUE_SIGHT_SHADOW
                            || style == Style.RESONATOR);
            default -> {
            }
        }
        return new ThemedEchoArmorModel<>(LayerDefinition.create(mesh, 256, 128).bakeRoot());
    }

    private static void addHelmet(PartDefinition root, Style style, EchoArmorGeometry.Atlas atlas) {
        EchoArmorGeometry.helmet(root, atlas);
        PartDefinition head = root.getChild("head");
        HelmetShape shape = HelmetShape.forStyle(style);
        if (shape.hasBrowGem()) {
            atlas.box(1.0F, 0.7F, 0.4F).add(head, "brow_shard", -0.5F, -7.7F, -5.35F);
        }
        if (shape.hasCrest()) {
            atlas.box(1.25F, 3.1F, 1.6F).add(head, "themed_crown", -0.625F, -3.1F, -0.8F,
                    false, PartPose.offsetAndRotation(0, shape.crownY() - 2.6F, 0,
                            shape.crownPitch(), 0, 0));
        }
        EchoArmorGeometry.Plate wing = atlas.box(1.05F, 4.1F, 1.6F);
        wing.add(head, "themed_right_wing", 0, -4.1F, -0.8F, false,
                PartPose.offsetAndRotation(4.2F, shape.wingY() - 0.5F, 0,
                        0, shape.wingYaw(), shape.wingRoll()));
        wing.add(head, "themed_left_wing", -1.05F, -4.1F, -0.8F, true,
                PartPose.offsetAndRotation(-4.2F, shape.wingY() - 0.5F, 0,
                        0, -shape.wingYaw(), -shape.wingRoll()));
        if (shape.hasTips()) {
            EchoArmorGeometry.Plate tip = atlas.box(0.65F, 2.6F, 0.8F);
            tip.add(head, "themed_right_tip", 0, -2.6F, -0.4F, false,
                    PartPose.offsetAndRotation(5.15F, shape.tipY(), 0,
                            0, shape.wingYaw(), shape.tipRoll()));
            tip.add(head, "themed_left_tip", -0.65F, -2.6F, -0.4F, true,
                    PartPose.offsetAndRotation(-5.15F, shape.tipY(), 0,
                            0, -shape.wingYaw(), -shape.tipRoll()));
        }
    }

    private record HelmetShape(boolean hasBrowGem, boolean hasCrest, boolean hasTips,
                               float crownY, float crownPitch, float wingY,
                               float wingYaw, float wingRoll, float tipY, float tipRoll) {
        private static HelmetShape forStyle(Style style) {
            return switch (style) {
                case EXPLORER -> new HelmetShape(true, false, false,
                        -6.0F, 0.0F, -2.8F, 0.10F, 0.18F, -3.5F, 0.18F);
                case ECHO_TRAVELER -> new HelmetShape(true, true, true,
                        -6.2F, -0.08F, -3.4F, 0.12F, 0.30F, -5.8F, 0.42F);
                case LISTENER -> new HelmetShape(false, true, true,
                        -6.0F, 0.12F, -3.8F, 0.24F, 0.62F, -5.6F, 0.92F);
                case WIND_WALKER -> new HelmetShape(true, true, true,
                        -6.4F, -0.16F, -4.0F, 0.18F, 0.48F, -6.2F, 0.72F);
                case TIDE_STALKER -> new HelmetShape(true, true, true,
                        -6.1F, 0.18F, -3.7F, 0.34F, 0.36F, -5.7F, 0.54F);
                case TRUE_SIGHT_SHADOW -> new HelmetShape(true, true, true,
                        -6.5F, -0.10F, -4.0F, 0.08F, 0.25F, -6.4F, 0.34F);
                case SILENT_WATCH -> new HelmetShape(true, true, true,
                        -5.9F, 0.24F, -2.9F, 0.14F, 0.78F, -4.5F, 1.02F);
                case ECHO_OATH -> new HelmetShape(true, true, true,
                        -6.3F, -0.10F, -3.5F, 0.16F, 0.54F, -5.6F, 0.78F);
                case RESONATOR -> new HelmetShape(true, true, true,
                        -6.7F, -0.14F, -4.1F, 0.22F, 0.66F, -6.5F, 0.88F);
            };
        }
    }

    private record ChestShape(float shoulderY, float shoulderRoll,
                              float shoulderDepth, float corePitch) {
        private static ChestShape forStyle(Style style) {
            return switch (style) {
                case EXPLORER -> new ChestShape(0.35F, 0.06F, 0.12F, 0.0F);
                case ECHO_TRAVELER -> new ChestShape(0.10F, 0.16F, 0.16F, -0.08F);
                case LISTENER -> new ChestShape(-0.45F, 0.36F, 0.18F, 0.08F);
                case WIND_WALKER -> new ChestShape(-0.55F, 0.44F, 0.14F, -0.14F);
                case TIDE_STALKER -> new ChestShape(-0.25F, 0.27F, 0.20F, 0.14F);
                case TRUE_SIGHT_SHADOW -> new ChestShape(-0.20F, 0.24F, 0.18F, -0.10F);
                case SILENT_WATCH -> new ChestShape(0.05F, 0.19F, 0.22F, 0.12F);
                case ECHO_OATH -> new ChestShape(-0.35F, 0.32F, 0.24F, -0.08F);
                case RESONATOR -> new ChestShape(-0.50F, 0.40F, 0.26F, -0.12F);
            };
        }
    }
}
