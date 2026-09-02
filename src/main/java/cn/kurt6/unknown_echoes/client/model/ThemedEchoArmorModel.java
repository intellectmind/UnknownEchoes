package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** 按套装主题和装备槽位生成的回响护甲模型。 */
public final class ThemedEchoArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    private static final float TEXTURE_SCALE = 0.5F;

    public enum Style {
        EXPLORER,
        ECHO_TRAVELER,
        LISTENER,
        WIND_WALKER,
        TIDE_STALKER,
        TRUE_SIGHT_SHADOW,
        SILENT_WATCH,
        ECHO_OATH,
        RESONATOR
    }

    private ThemedEchoArmorModel(ModelPart root) {
        super(root);
    }

    public static ThemedEchoArmorModel<LivingEntity> create(Style style, EquipmentSlot slot) {
        CubeDeformation deformation = slot == EquipmentSlot.LEGS
                ? new CubeDeformation(0.34F)
                : new CubeDeformation(0.68F);
        return new ThemedEchoArmorModel<>(createLayer(style, slot, deformation).bakeRoot());
    }

    private static LayerDefinition createLayer(Style style, EquipmentSlot slot,
                                                CubeDeformation deformation) {
        MeshDefinition mesh = createHighResolutionMesh(deformation);
        PartDefinition root = mesh.getRoot();
        switch (slot) {
            case HEAD -> addHelmet(root, style, deformation);
            case CHEST -> addChest(root, style, deformation);
            case LEGS -> addLeggings(root, style, deformation);
            case FEET -> addBoots(root, style, deformation);
            default -> {
            }
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    private static MeshDefinition createHighResolutionMesh(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                deformation.extend(0.5F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                deformation, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return mesh;
    }

    private static void addHelmet(PartDefinition root, Style style, CubeDeformation deformation) {
        PartDefinition head = root.getChild("head");
        CubeDeformation plate = deformation.extend(0.10F);
        HelmetShape shape = HelmetShape.forStyle(style);

        if (shape.hasVisor()) {
            head.addOrReplaceChild("themed_visor",
                    CubeListBuilder.create().texOffs(64, 0)
                            .addBox(-4.0F, -1.5F, -0.5F, 8.0F, 3.0F, 1.0F,
                                    plate, TEXTURE_SCALE, TEXTURE_SCALE),
                    PartPose.offset(0.0F, -4.4F, -4.5F));
        }
        if (shape.hasCrest()) {
            head.addOrReplaceChild("themed_crown",
                    CubeListBuilder.create().texOffs(84, 0)
                            .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F,
                                    plate, TEXTURE_SCALE, TEXTURE_SCALE),
                    PartPose.offsetAndRotation(0.0F, shape.crownY(), -0.2F,
                            shape.crownPitch(), 0.0F, 0.0F));
        }

        head.addOrReplaceChild("themed_right_wing",
                CubeListBuilder.create().texOffs(92, 0)
                        .addBox(0.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offsetAndRotation(3.4F, shape.wingY(), 0.0F,
                        0.0F, shape.wingYaw(), -shape.wingRoll()));
        head.addOrReplaceChild("themed_left_wing",
                CubeListBuilder.create().texOffs(92, 0).mirror()
                        .addBox(-2.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offsetAndRotation(-3.4F, shape.wingY(), 0.0F,
                        0.0F, -shape.wingYaw(), shape.wingRoll()));

        if (shape.hasTips()) {
            head.addOrReplaceChild("themed_right_tip",
                    CubeListBuilder.create().texOffs(100, 0)
                            .addBox(0.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F,
                                    plate, TEXTURE_SCALE, TEXTURE_SCALE),
                    PartPose.offsetAndRotation(5.1F, shape.tipY(), 0.0F,
                            0.0F, shape.wingYaw(), -shape.tipRoll()));
            head.addOrReplaceChild("themed_left_tip",
                    CubeListBuilder.create().texOffs(100, 0).mirror()
                            .addBox(-1.0F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F,
                                    plate, TEXTURE_SCALE, TEXTURE_SCALE),
                    PartPose.offsetAndRotation(-5.1F, shape.tipY(), 0.0F,
                            0.0F, -shape.wingYaw(), shape.tipRoll()));
        }
    }

    private static void addChest(PartDefinition root, Style style, CubeDeformation deformation) {
        ChestShape shape = ChestShape.forStyle(style);
        CubeDeformation plate = deformation.extend(0.10F);
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("themed_chest_plate",
                CubeListBuilder.create().texOffs(84, 8)
                        .addBox(-4.0F, 0.8F, -2.75F, 8.0F, 8.0F, 1.0F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        body.addOrReplaceChild("themed_back_plate",
                CubeListBuilder.create().texOffs(102, 8)
                        .addBox(-4.0F, 1.4F, 1.75F, 8.0F, 7.0F, 1.0F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        body.addOrReplaceChild("themed_chest_shard",
                CubeListBuilder.create().texOffs(112, 18)
                        .addBox(-1.0F, 2.8F, -3.2F, 2.0F, 3.0F, 1.0F,
                                deformation.extend(0.16F), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.rotation(shape.corePitch(), 0.0F, 0.0F));

        CubeDeformation shoulder = deformation.extend(shape.shoulderDepth());
        root.getChild("right_arm").addOrReplaceChild("themed_right_pauldron",
                CubeListBuilder.create().texOffs(104, 0)
                        .addBox(-4.0F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F,
                                shoulder, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offsetAndRotation(0.0F, shape.shoulderY(), 0.0F,
                        0.0F, 0.0F, shape.shoulderRoll()));
        root.getChild("left_arm").addOrReplaceChild("themed_left_pauldron",
                CubeListBuilder.create().texOffs(104, 0).mirror()
                        .addBox(-1.0F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F,
                                shoulder, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.offsetAndRotation(0.0F, shape.shoulderY(), 0.0F,
                        0.0F, 0.0F, -shape.shoulderRoll()));

        root.getChild("right_arm").addOrReplaceChild("themed_right_bracer",
                CubeListBuilder.create().texOffs(64, 24)
                        .addBox(-3.1F, 5.2F, -2.5F, 4.0F, 4.0F, 1.0F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("themed_left_bracer",
                CubeListBuilder.create().texOffs(64, 24).mirror()
                        .addBox(-0.9F, 5.2F, -2.5F, 4.0F, 4.0F, 1.0F,
                                plate, TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
    }

    private static void addLeggings(PartDefinition root, Style style, CubeDeformation deformation) {
        float flare = switch (style) {
            case WIND_WALKER, LISTENER -> 0.18F;
            case TIDE_STALKER, TRUE_SIGHT_SHADOW -> 0.14F;
            case ECHO_OATH, RESONATOR -> 0.12F;
            default -> 0.08F;
        };
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("themed_waist_guard",
                CubeListBuilder.create().texOffs(64, 18)
                        .addBox(-5.0F, 9.0F, -2.65F, 10.0F, 3.0F, 1.0F,
                                deformation.extend(flare), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
        addThighPlate(root.getChild("right_leg"), false, deformation, flare);
        addThighPlate(root.getChild("left_leg"), true, deformation, flare);
    }

    private static void addThighPlate(PartDefinition leg, boolean mirror,
                                      CubeDeformation deformation, float flare) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(86, 18);
        if (mirror) {
            builder.mirror();
        }
        leg.addOrReplaceChild(mirror ? "themed_left_thigh" : "themed_right_thigh",
                builder.addBox(-2.0F, 0.7F, -2.55F, 4.0F, 5.0F, 1.0F,
                        deformation.extend(flare), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);
    }

    private static void addBoots(PartDefinition root, Style style, CubeDeformation deformation) {
        float flare = switch (style) {
            case WIND_WALKER, TIDE_STALKER, RESONATOR -> 0.22F;
            case ECHO_TRAVELER, LISTENER -> 0.17F;
            default -> 0.12F;
        };
        addBootPlate(root.getChild("right_leg"), style, false, deformation, flare);
        addBootPlate(root.getChild("left_leg"), style, true, deformation, flare);
    }

    private static void addBootPlate(PartDefinition leg, Style style, boolean mirror,
                                     CubeDeformation deformation, float flare) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(96, 18);
        if (mirror) {
            builder.mirror();
        }
        leg.addOrReplaceChild(mirror ? "themed_left_boot" : "themed_right_boot",
                builder.addBox(-2.0F, 7.0F, -2.65F, 4.0F, 5.0F, 1.0F,
                        deformation.extend(flare), TEXTURE_SCALE, TEXTURE_SCALE),
                PartPose.ZERO);

        if (style == Style.WIND_WALKER || style == Style.TIDE_STALKER
                || style == Style.ECHO_TRAVELER || style == Style.TRUE_SIGHT_SHADOW
                || style == Style.RESONATOR) {
            CubeListBuilder fin = CubeListBuilder.create().texOffs(106, 18);
            if (mirror) {
                fin.mirror();
            }
            float x = mirror ? 1.6F : -3.6F;
            float roll = mirror ? -0.24F : 0.24F;
            leg.addOrReplaceChild(mirror ? "themed_left_boot_fin" : "themed_right_boot_fin",
                    fin.addBox(0.0F, -2.0F, -0.5F, 2.0F, 4.0F, 1.0F,
                            deformation.extend(0.08F), TEXTURE_SCALE, TEXTURE_SCALE),
                    PartPose.offsetAndRotation(x, 8.7F, 0.0F, 0.0F, 0.0F, roll));
        }
    }

    private record HelmetShape(boolean hasVisor, boolean hasCrest, boolean hasTips,
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
