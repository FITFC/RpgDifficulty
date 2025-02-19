package net.rpgdifficulty.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.levelz.access.PlayerStatsManagerAccess;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.rpgdifficulty.RpgDifficultyMain;
import net.rpgdifficulty.access.EntityAccess;
import net.rpgdifficulty.api.MobStrengthener;

@Mixin(MobStrengthener.class)
public class ChangeAttributeMixin {

    @Inject(method = "changeAttributes", at = @At(value = "HEAD"), cancellable = true)
    private static void changeAttributesMixin(MobEntity mobEntity, ServerWorld world, CallbackInfo info) {
        if (RpgDifficultyMain.CONFIG.levelZLevelFactor > 0.001D && !RpgDifficultyMain.CONFIG.excludedEntity.contains(mobEntity.getType().toString().replace("entity.", ""))) {
            double x = mobEntity.getX();
            double y = mobEntity.getY();
            double z = mobEntity.getZ();

            int playerCount = 0;
            int totalPlayerLevel = 0;
            for (PlayerEntity playerEntity : world.getPlayers()) {
                if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(playerEntity))
                    continue;
                if (playerEntity.world.getDimension().equals(mobEntity.world.getDimension()) && Math.sqrt(playerEntity.squaredDistanceTo(x, y, z)) <= RpgDifficultyMain.CONFIG.levelZPlayerRadius) {
                    playerCount++;
                    totalPlayerLevel += ((PlayerStatsManagerAccess) playerEntity).getPlayerStatsManager().getLevel("level");
                }
            }
            if (playerCount == 0) {
                PlayerEntity playerEntity = world.getClosestPlayer(x, y, z, -1.0, false);
                if (playerEntity != null) {
                    playerCount++;
                    totalPlayerLevel += ((PlayerStatsManagerAccess) playerEntity).getPlayerStatsManager().getLevel("level");
                }
            }
            if (playerCount > 0) {
                double mobHealth = mobEntity.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
                // Check if hasAttributes necessary
                double mobDamage = 0.0F;
                double mobProtection = 0.0F;
                double mobSpeed = 0.0F;
                boolean hasAttackDamageAttribute = mobEntity.getAttributes().hasAttribute(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                boolean hasArmorAttribute = mobEntity.getAttributes().hasAttribute(EntityAttributes.GENERIC_ARMOR);
                boolean hasMovementSpeedAttribute = mobEntity.getAttributes().hasAttribute(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (hasAttackDamageAttribute) {
                    mobDamage = mobEntity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                }
                if (hasArmorAttribute) {
                    mobProtection = mobEntity.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
                }
                if (hasMovementSpeedAttribute) {
                    mobSpeed = mobEntity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                }

                // Factor
                double mobHealthFactor = RpgDifficultyMain.CONFIG.startingFactor;
                double mobDamageFactor = RpgDifficultyMain.CONFIG.startingFactor;
                double mobProtectionFactor = RpgDifficultyMain.CONFIG.startingFactor;
                // Cutoff
                double maxFactorHealth = RpgDifficultyMain.CONFIG.maxFactorHealth;
                double maxFactorDamage = RpgDifficultyMain.CONFIG.maxFactorDamage;
                double maxFactorProtection = RpgDifficultyMain.CONFIG.maxFactorProtection;

                // Calculate
                mobHealthFactor += (double) totalPlayerLevel / (double) playerCount * RpgDifficultyMain.CONFIG.levelZLevelFactor;
                mobDamageFactor += (double) totalPlayerLevel / (double) playerCount * RpgDifficultyMain.CONFIG.levelZLevelFactor;
                mobProtectionFactor += (double) totalPlayerLevel / (double) playerCount * RpgDifficultyMain.CONFIG.levelZLevelFactor;

                if (mobHealthFactor > maxFactorHealth) {
                    mobHealthFactor = maxFactorHealth;
                }
                if (mobDamageFactor > maxFactorDamage) {
                    mobDamageFactor = maxFactorDamage;
                }
                if (mobProtectionFactor > maxFactorProtection) {
                    mobProtectionFactor = maxFactorProtection;
                }

                // round factor
                mobHealthFactor = Math.round(mobHealthFactor * 100.0D) / 100.0D;
                mobProtectionFactor = Math.round(mobProtectionFactor * 100.0D) / 100.0D;
                mobDamageFactor = Math.round(mobDamageFactor * 100.0D) / 100.0D;

                // Setter
                mobHealth *= mobHealthFactor;
                mobDamage *= mobDamageFactor;
                mobProtection *= mobProtectionFactor;

                // Randomness
                if (RpgDifficultyMain.CONFIG.allowRandomValues) {
                    if (world.random.nextFloat() <= ((float) RpgDifficultyMain.CONFIG.randomChance / 100F)) {
                        float randomFactor = (float) RpgDifficultyMain.CONFIG.randomFactor / 100F;
                        mobHealth = mobHealth * (1 - randomFactor + (world.random.nextDouble() * randomFactor * 2F));
                        mobDamage = mobDamage * (1 - randomFactor + (world.random.nextDouble() * randomFactor * 2F));

                        // round value
                        mobHealth = Math.round(mobHealth * 100.0D) / 100.0D;
                        mobDamage = Math.round(mobDamage * 100.0D) / 100.0D;
                    }
                }

                // Big Zombie
                if (RpgDifficultyMain.CONFIG.allowSpecialZombie && mobEntity instanceof ZombieEntity) {
                    if (world.random.nextFloat() < ((float) RpgDifficultyMain.CONFIG.speedZombieChance / 100F)) {
                        mobHealth -= RpgDifficultyMain.CONFIG.speedZombieMalusLifePoints;
                        mobSpeed *= RpgDifficultyMain.CONFIG.speedZombieSpeedFactor;
                    } else if (world.random.nextFloat() < ((float) RpgDifficultyMain.CONFIG.bigZombieChance / 100F)) {
                        mobSpeed *= RpgDifficultyMain.CONFIG.bigZombieSlownessFactor;
                        mobHealth += RpgDifficultyMain.CONFIG.bigZombieBonusLifePoints;
                        mobDamage += RpgDifficultyMain.CONFIG.bigZombieBonusDamage;
                        ((EntityAccess) mobEntity).setBig();
                    }
                    // round value
                    mobHealth = Math.round(mobHealth * 100.0D) / 100.0D;
                    mobDamage = Math.round(mobDamage * 100.0D) / 100.0D;
                    mobSpeed = Math.round(mobSpeed * 1000.0D) / 1000.0D;
                }
                // Set Values
                mobEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(mobHealth);
                mobEntity.heal(mobEntity.getMaxHealth());
                if (hasAttackDamageAttribute) {
                    mobEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(mobDamage);
                }
                if (hasArmorAttribute) {
                    mobEntity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR).setBaseValue(mobProtection);
                }
                if (hasMovementSpeedAttribute) {
                    mobEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(mobSpeed);
                }
                info.cancel();
            }
        }
    }

    @Inject(method = "changeOnlyHealthAttribute", at = @At(value = "HEAD"), cancellable = true)
    private static void changeOnlyHealthAttributeMixin(MobEntity mobEntity, ServerWorld world, CallbackInfo info) {
        if (RpgDifficultyMain.CONFIG.levelZLevelFactor > 0.001D && !RpgDifficultyMain.CONFIG.excludedEntity.contains(mobEntity.getType().toString().replace("entity.", ""))) {
            double x = mobEntity.getX();
            double y = mobEntity.getY();
            double z = mobEntity.getZ();

            int playerCount = 0;
            int totalPlayerLevel = 0;
            for (PlayerEntity playerEntity : world.getPlayers()) {
                if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(playerEntity))
                    continue;
                if (playerEntity.world.getDimension().equals(mobEntity.world.getDimension()) && Math.sqrt(playerEntity.squaredDistanceTo(x, y, z)) <= RpgDifficultyMain.CONFIG.levelZPlayerRadius) {
                    playerCount++;
                    totalPlayerLevel += ((PlayerStatsManagerAccess) playerEntity).getPlayerStatsManager().getLevel("level");
                }
            }
            if (playerCount == 0) {
                PlayerEntity playerEntity = world.getClosestPlayer(x, y, z, -1.0, false);
                if (playerEntity != null) {
                    playerCount++;
                    totalPlayerLevel += ((PlayerStatsManagerAccess) playerEntity).getPlayerStatsManager().getLevel("level");
                }
            }
            if (playerCount == 0) {
                double mobHealth = mobEntity.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);

                // Factor
                double mobHealthFactor = RpgDifficultyMain.CONFIG.startingFactor;
                // Cutoff
                double maxFactorHealth = RpgDifficultyMain.CONFIG.maxFactorHealth;

                // Calculate
                mobHealthFactor += (double) totalPlayerLevel / (double) playerCount * RpgDifficultyMain.CONFIG.levelZLevelFactor;

                if (mobHealthFactor > maxFactorHealth) {
                    mobHealthFactor = maxFactorHealth;
                }

                // round factor
                mobHealthFactor = Math.round(mobHealthFactor * 100.0D) / 100.0D;

                // Setter
                mobHealth *= mobHealthFactor;

                // Randomness
                if (RpgDifficultyMain.CONFIG.allowRandomValues) {
                    if (world.random.nextFloat() <= ((float) RpgDifficultyMain.CONFIG.randomChance / 100F)) {
                        float randomFactor = (float) RpgDifficultyMain.CONFIG.randomFactor / 100F;
                        mobHealth = mobHealth * (1 - randomFactor + (world.random.nextDouble() * randomFactor * 2F));

                        // round value
                        mobHealth = Math.round(mobHealth * 100.0D) / 100.0D;
                    }
                }

                // Set Values
                mobEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(mobHealth);
                mobEntity.heal(mobEntity.getMaxHealth());
            }
            info.cancel();
        }
    }

    // @Inject(method = "changeBossAttributes", at = @At(value = "INVOKE", target =
    // "Lnet/minecraft/entity/mob/MobEntity;getAttributeInstance(Lnet/minecraft/entity/attribute/EntityAttribute;)Lnet/minecraft/entity/attribute/EntityAttributeInstance;", ordinal = 0), locals =
    // LocalCapture.CAPTURE_FAILSOFT)
    // private static void changeBossAttributesMixin(MobEntity mobEntity, ServerWorld world, CallbackInfo info, double mobHealthFactor) {
    // // setRpgStuff(mobEntity, mobHealthFactor);
    // }

    // @Inject(method = "changeEnderDragonAttribute", at = @At("TAIL"))
    // private static void changeEnderDragonAttributeMixin(MobEntity mobEntity, ServerWorld world, CallbackInfo info) {
    // // ((MobEntityAccess) mobEntity).setMobRpgLabel(false);
    // }

    // public static double getDamageFactor(Entity entity) {
    // }

}
