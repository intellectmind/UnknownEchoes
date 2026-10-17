package cn.kurt6.unknown_echoes.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;

/** 独立叠甲覆盖四肢，面部敞开，活动处保留窄接缝。 */
final class EchoArmorGeometry {
    private EchoArmorGeometry() {
    }

    static MeshDefinition anchors() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12, 0));
        return mesh;
    }

    /** 大件先入图集：头部行带只有 22 行，按声明顺序首次适配。 */
    static void helmet(PartDefinition root, Atlas atlas) {
        PartDefinition head = root.getChild("head");
        atlas.box(9.6F, 0.8F, 9.7F).add(head, "helmet_top", -4.8F, -8.75F, -4.85F);
        Plate side = atlas.box(0.65F, 6.3F, 8.1F);
        side.add(head, "helmet_right_side", -4.7F, -7.95F, -4.05F);
        side.add(head, "helmet_left_side", 4.05F, -7.95F, -4.05F, true, PartPose.ZERO);
        atlas.box(7.0F, 0.6F, 7.6F).add(head, "helmet_crown", -3.5F, -9.35F, -3.8F);
        atlas.box(1.1F, 0.8F, 8.6F).add(head, "helmet_ridge", -0.55F, -10.1F, -4.3F);
        atlas.box(9.6F, 6.4F, 0.6F).add(head, "helmet_back", -4.8F, -7.95F, 4.05F);
        atlas.box(8.8F, 1.3F, 0.9F).add(head, "helmet_brow", -4.4F, -7.95F, -4.95F);
        atlas.box(9.0F, 1.4F, 1.0F).add(head, "helmet_nape", -4.5F, 0, -0.5F, false,
                PartPose.offsetAndRotation(0, -1.9F, 4.55F, 0.4F, 0, 0));
        Plate cheek = atlas.box(1.2F, 4.5F, 0.75F);
        cheek.add(head, "helmet_right_cheek", -4.35F, -6.7F, -4.8F);
        cheek.add(head, "helmet_left_cheek", 3.15F, -6.7F, -4.8F, true, PartPose.ZERO);
        Plate ear = atlas.box(0.45F, 2.2F, 2.2F);
        ear.add(head, "helmet_right_ear", -0.225F, -1.1F, -1.1F, false,
                PartPose.offsetAndRotation(-4.85F, -3.9F, 0, 0.7853982F, 0, 0));
        ear.add(head, "helmet_left_ear", -0.225F, -1.1F, -1.1F, true,
                PartPose.offsetAndRotation(4.85F, -3.9F, 0, 0.7853982F, 0, 0));
    }

    static void chest(PartDefinition root, Atlas atlas, float shoulderY, float shoulderRoll,
                      float heft, float corePitch) {
        PartDefinition body = root.getChild("body");
        atlas.box(8.3F, 9.9F, 4.3F).add(body, "chest_shell", -4.15F, 0.2F, -2.15F);
        atlas.box(7.4F, 6.6F, 0.65F).add(body, "chest_plate", -3.7F, 0.9F, -2.9F);
        atlas.box(7.4F, 9.2F, 0.65F).add(body, "back_plate", -3.7F, 0.8F, 2.25F);
        atlas.box(7.2F, 2.2F, 0.7F).add(body, "chest_abdomen", -3.6F, 7.8F, -2.75F);
        atlas.box(1.1F, 1.1F, 0.5F).add(body, "chest_shard", -0.55F, -0.55F, -0.25F,
                false, PartPose.offsetAndRotation(0, 2.9F, -3.3F, corePitch, 0, 0.7853982F));
        Plate shoulder = atlas.box(5.5F + heft, 0.85F, 5.2F + heft);
        Plate shoulderLame = atlas.box(4.6F + heft, 1.0F, 4.9F);
        Plate shoulderRim = atlas.box(0.7F, 2.4F, 4.6F);
        Plate upper = atlas.box(3.6F, 4.0F, 0.65F);
        Plate upperSide = atlas.box(0.65F, 4.0F, 4.2F);
        Plate upperBack = atlas.box(3.4F, 4.0F, 0.65F);
        Plate forearm = atlas.box(3.85F, 1.85F, 0.75F);
        Plate forearmMid = atlas.box(3.4F, 1.7F, 0.75F);
        Plate forearmLower = atlas.box(2.95F, 1.45F, 0.7F);
        Plate forearmBack = atlas.box(3.15F, 4.5F, 0.65F);
        Plate forearmSide = atlas.box(0.65F, 4.5F, 4.2F);
        Plate wrist = atlas.box(2.9F, 0.75F, 0.65F);
        Plate collar = atlas.box(3.3F, 0.6F, 0.7F);
        Plate crest = atlas.box(0.5F, 1.8F, 3.2F);
        Plate couter = atlas.box(1.15F, 1.15F, 0.55F);
        Plate pectoral = atlas.box(3.1F, 1.3F, 0.65F);
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition arm = root.getChild(left ? "left_arm" : "right_arm");
            float center = left ? 1 : -1;
            shoulder.add(arm, "pauldron", -(5.5F + heft) / 2, -0.6F, -(5.2F + heft) / 2,
                    left, PartPose.offsetAndRotation(center, -1.75F + shoulderY * 0.25F, 0,
                            0, 0, left ? -0.25F - shoulderRoll * 0.35F : 0.25F + shoulderRoll * 0.35F));
            shoulderLame.add(arm, "pauldron_lame", -(4.6F + heft) / 2, -0.5F, -2.45F, left,
                    PartPose.offsetAndRotation(center, -0.6F, 0,
                            0, 0, left ? 0.14F : -0.14F));
            shoulderRim.add(arm, "pauldron_outer", -0.35F, -0.4F, -2.3F, left,
                    PartPose.offsetAndRotation(left ? 3.45F : -3.45F, -0.8F, 0,
                            0, 0, left ? 0.22F : -0.22F));
            crest.add(arm, "pauldron_crest", -0.25F, -1.8F, -1.6F, left,
                    PartPose.offsetAndRotation(left ? 3.0F : -3.0F, -1.65F, 0.35F,
                            -0.22F, 0, left ? 0.6F : -0.6F));
            upper.add(arm, "upper_arm_front", center - 1.8F, -0.15F, -2.75F, left, PartPose.ZERO);
            upperSide.add(arm, "upper_arm_outer", left ? 3.05F : -3.7F, -0.15F, -2.1F, left, PartPose.ZERO);
            upperBack.add(arm, "upper_arm_back", center - 1.7F, -0.15F, 2.1F, left, PartPose.ZERO);
            forearm.add(arm, "bracer_front", center - 1.925F, 4.15F, -3.0F, left, PartPose.ZERO);
            forearmMid.add(arm, "bracer_front_mid", center - 1.7F, 5.85F, -2.88F, left, PartPose.ZERO);
            forearmLower.add(arm, "bracer_front_lower", center - 1.475F, 7.4F, -2.76F, left, PartPose.ZERO);
            forearmBack.add(arm, "bracer_back", center - 1.575F, 4.15F, 2.1F, left, PartPose.ZERO);
            forearmSide.add(arm, "bracer_outer", -0.325F, -2.25F, -2.1F, left,
                    PartPose.offsetAndRotation(left ? 3.1F : -3.1F, 6.4F, 0,
                            0, 0, left ? 0.15F : -0.15F));
            wrist.add(arm, "bracer_wrist", center - 1.45F, 8.65F, -2.7F, left, PartPose.ZERO);
            couter.add(arm, "elbow_cap", -0.575F, -0.575F, -0.275F, left,
                    PartPose.offsetAndRotation(center, 4.0F, -3.0F, 0, 0, 0.7853982F));
            collar.add(body, left ? "left_collar" : "right_collar", -1.65F, -0.3F, -0.35F,
                    left, PartPose.offsetAndRotation(left ? 2.05F : -2.05F, 1.0F, -3.0F,
                            0, 0, left ? -0.20F : 0.20F));
            pectoral.add(body, left ? "left_pectoral" : "right_pectoral", -1.55F, -0.65F, -0.325F,
                    left, PartPose.offsetAndRotation(left ? 2.1F : -2.1F, 2.5F, -3.15F,
                            0, 0, left ? -0.17F : 0.17F));
        }
    }

    static void leggings(PartDefinition root, Atlas atlas, float flare) {
        PartDefinition body = root.getChild("body");
        atlas.box(8.3F, 1.1F, 4.3F).add(body, "waist_belt", -4.15F, 10.6F, -2.15F);
        Plate thigh = atlas.box(3.6F, 4.8F, 0.75F);
        Plate outer = atlas.box(0.65F, 4.8F, 4.2F);
        Plate back = atlas.box(3.4F, 4.8F, 0.65F);
        Plate knee = atlas.box(3.4F, 1.7F, 0.85F);
        Plate tasset = atlas.box(3.5F, 2.0F, 0.65F);
        Plate tassetLower = atlas.box(2.95F, 1.4F, 0.65F);
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition leg = root.getChild(left ? "left_leg" : "right_leg");
            thigh.add(leg, "thigh_front", -1.8F, 0.3F, -2.8F, left, PartPose.ZERO);
            outer.add(leg, "thigh_outer", left ? 2.05F : -2.7F, 0.3F, -2.1F, left,
                    PartPose.rotation(0, 0, left ? -flare : flare));
            back.add(leg, "thigh_back", -1.7F, 0.3F, 2.1F, left, PartPose.ZERO);
            knee.add(leg, "thigh_knee", -1.7F, 5.0F, -3.0F, left, PartPose.ZERO);
            tasset.add(leg, "thigh_tasset", -1.75F, 0, -0.325F, left,
                    PartPose.offsetAndRotation(0, -0.1F, -3.0F, -0.20F, 0, left ? -0.14F : 0.14F));
            tassetLower.add(leg, "thigh_tasset_lower", -1.475F, 0, -0.325F, left,
                    PartPose.offsetAndRotation(left ? 0.24F : -0.24F, 1.55F, -3.35F,
                            -0.12F, 0, left ? -0.14F : 0.14F));
        }
    }

    static void boots(PartDefinition root, Atlas atlas, boolean fins) {
        Plate shin = atlas.box(3.6F, 2.1F, 0.75F);
        Plate shinMid = atlas.box(3.25F, 1.8F, 0.75F);
        Plate shinLower = atlas.box(2.85F, 1.65F, 0.75F);
        Plate side = atlas.box(0.65F, 5.3F, 4.2F);
        Plate back = atlas.box(3.4F, 5.3F, 0.65F);
        Plate ankle = atlas.box(3.4F, 0.9F, 0.65F);
        Plate toe = atlas.box(3.8F, 0.9F, 4.9F);
        Plate fin = fins ? atlas.box(0.85F, 3.1F, 1.2F) : null;
        for (boolean left : new boolean[]{false, true}) {
            PartDefinition leg = root.getChild(left ? "left_leg" : "right_leg");
            shin.add(leg, "boot_greave", -1.8F, 6.45F, -3.05F, left, PartPose.ZERO);
            shinMid.add(leg, "boot_greave_mid", -1.625F, 8.4F, -2.9F, left, PartPose.ZERO);
            shinLower.add(leg, "boot_greave_lower", -1.425F, 10.0F, -2.75F, left, PartPose.ZERO);
            side.add(leg, "boot_outer", -0.325F, -2.65F, -2.1F, left,
                    PartPose.offsetAndRotation(left ? 2.25F : -2.25F, 8.25F, 0,
                            0, 0, left ? 0.12F : -0.12F));
            back.add(leg, "boot_back", -1.7F, 5.6F, 2.1F, left, PartPose.ZERO);
            ankle.add(leg, "boot_ankle", -1.7F, 10.5F, -2.95F, left, PartPose.ZERO);
            toe.add(leg, "boot_toe", -1.9F, 11.1F, -2.9F, left, PartPose.ZERO);
            if (fin != null) {
                fin.add(leg, "boot_fin", -0.425F, -2.5F, -0.6F, left,
                        PartPose.offsetAndRotation(left ? 2.65F : -2.65F, 9.3F, 0,
                                0, 0, left ? 0.25F : -0.25F));
            }
        }
    }

    /** 每个槽位独占图集行带；左右对称件共享同一个岛。 */
    static final class Atlas {
        private final boolean[][] occupied = new boolean[64][128];
        private final int start;
        private final int end;

        Atlas(EquipmentSlot slot) {
            start = switch (slot) {
                case CHEST -> 22;
                case FEET -> 49;
                default -> 0;
            };
            end = switch (slot) {
                case HEAD -> 22;
                case CHEST -> 49;
                default -> 64;
            };
        }

        Plate box(float width, float height, float depth) {
            int columns = (int) Math.ceil(2 * (width + depth)) + 1;
            int rows = (int) Math.ceil(height + depth) + 1;
            for (int y = start; y + rows <= end; y++) {
                for (int x = 0; x + columns <= 128; x++) {
                    if (!available(x, y, columns, rows)) {
                        continue;
                    }
                    for (int row = y; row < y + rows; row++) {
                        java.util.Arrays.fill(occupied[row], x, x + columns, true);
                    }
                    return new Plate(x, y, width, height, depth);
                }
            }
            throw new IllegalStateException("Armor UV band exhausted");
        }

        private boolean available(int x, int y, int columns, int rows) {
            for (int row = y; row < y + rows; row++) {
                for (int column = x; column < x + columns; column++) {
                    if (occupied[row][column]) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    record Plate(int u, int v, float width, float height, float depth) {
        void add(PartDefinition parent, String name, float x, float y, float z) {
            add(parent, name, x, y, z, false, PartPose.ZERO);
        }

        void add(PartDefinition parent, String name, float x, float y, float z,
                 boolean mirror, PartPose pose) {
            CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v);
            if (mirror) {
                cube.mirror();
            }
            parent.addOrReplaceChild(name, cube.addBox(x, y, z, width, height, depth,
                    new CubeDeformation(0.08F), 0.5F, 0.5F), pose);
        }
    }
}
