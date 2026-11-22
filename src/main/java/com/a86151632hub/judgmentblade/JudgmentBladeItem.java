package com.a86151632hub.judgmentblade;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class JudgmentBladeItem extends SwordItem {
	private static final int CRIMSON_COLOR = 0xFF2244;
	private static final int GOLD_COLOR = 0xFFD700;

	public JudgmentBladeItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
		super(material, attackDamage, attackSpeed, settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (!world.isClient && world instanceof ServerWorld serverWorld) {
			if (user.isSneaking()) {
				// [Shift + 우클릭] 차원 단절 베기 (Dimensional Severance)
				castDimensionalSeverance(serverWorld, user);
				user.getItemCooldownManager().set(stack, 50); // 2.5초 쿨타임
			} else {
				// [우클릭] 단죄의 결계 & 천벌 강림 (Domain of Judgment)
				castDomainOfJudgment(serverWorld, user);
				user.getItemCooldownManager().set(stack, 80); // 4초 쿨타임
			}
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * 스킬 1 [우클릭]: 단죄의 결계 (Domain of Judgment)
	 * - 반경 11블록 내 이중 회전 마법진 + 하늘에서 내리꽂히는 8개의 빛기둥 창 + 공중 속박 후 뇌격 처형
	 */
	private void castDomainOfJudgment(ServerWorld world, PlayerEntity caster) {
		Vec3d center = caster.getPos();
		double radius = 11.0;

		// 웅장한 결계 전개 사운드
		world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.4f, 0.7f);
		world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.5f, 1.3f);

		DustParticleEffect crimsonDust = new DustParticleEffect(CRIMSON_COLOR, 1.8f);
		DustParticleEffect goldDust = new DustParticleEffect(GOLD_COLOR, 1.5f);

		// 1. 바닥 이중 마법진 및 육망성 궤적 렌더링 (수학 좌표 계산)
		for (int i = 0; i < 120; i++) {
			double angle = (2 * Math.PI * i) / 120.0;
			double outerX = center.x + Math.cos(angle) * radius;
			double outerZ = center.z + Math.sin(angle) * radius;
			world.spawnParticles(crimsonDust, outerX, center.y + 0.15, outerZ, 2, 0.05, 0.05, 0.05, 0.0);

			double innerX = center.x + Math.cos(angle) * (radius * 0.65);
			double innerZ = center.z + Math.sin(angle) * (radius * 0.65);
			world.spawnParticles(goldDust, innerX, center.y + 0.25, innerZ, 1, 0.02, 0.02, 0.02, 0.0);
		}

		// 2. 결계 외곽에 8개의 천벌의 빛기둥 소환
		for (int p = 0; p < 8; p++) {
			double pillarAngle = (2 * Math.PI * p) / 8.0;
			double px = center.x + Math.cos(pillarAngle) * (radius * 0.85);
			double pz = center.z + Math.sin(pillarAngle) * (radius * 0.85);
			for (double yOff = 0; yOff <= 16.0; yOff += 0.6) {
				world.spawnParticles(ParticleTypes.END_ROD, px, center.y + yOff, pz, 2, 0.08, 0.08, 0.08, 0.01);
				world.spawnParticles(crimsonDust, px, center.y + yOff, pz, 1, 0.15, 0.1, 0.15, 0.0);
			}
		}

		// 3. 범위 내 모든 적 포박, 공중 부양 후 천벌 뇌격 타격
		Box area = caster.getBoundingBox().expand(radius);
		List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, area,
				entity -> entity != caster && entity.isAlive());

		for (LivingEntity target : targets) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 10));
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 80, 0));
			target.setVelocity(0, 0.85, 0);
			target.velocityModified = true;

			// 각 적의 머리 위에서 내리꽂히는 심판의 빛기둥과 번개
			for (double h = 0; h <= 12.0; h += 0.5) {
				world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + h, target.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
			}
			world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);

			LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world, net.minecraft.entity.SpawnReason.TRIGGERED);
			if (lightning != null) {
				lightning.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
				lightning.setCosmetic(true);
				world.spawnEntity(lightning);
			}

			target.damage(world, world.getDamageSources().playerAttack(caster), 28.0f);
		}
	}

	/**
	 * 스킬 2 [Shift + 우클릭]: 차원 단절 베기 (Dimensional Severance)
	 * - 전방 14블록 초고속 돌진 + 궤적 위의 모든 적 일섬 베기
	 */
	private void castDimensionalSeverance(ServerWorld world, PlayerEntity caster) {
		Vec3d start = caster.getPos();
		Vec3d look = caster.getRotationVec(1.0f).normalize();
		double dashDist = 14.0;

		world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.2f, 1.5f);

		DustParticleEffect crimsonDust = new DustParticleEffect(CRIMSON_COLOR, 2.0f);

		// 돌진 경로 궤적 생성 및 경로상 적 일격
		Vec3d safeTargetPos = start;
		for (double d = 1.0; d <= dashDist; d += 0.5) {
			Vec3d step = start.add(look.multiply(d));
			world.spawnParticles(crimsonDust, step.x, step.y + 1.0, step.z, 4, 0.25, 0.25, 0.25, 0.0);
			world.spawnParticles(ParticleTypes.END_ROD, step.x, step.y + 1.0, step.z, 2, 0.15, 0.15, 0.15, 0.02);
			world.spawnParticles(ParticleTypes.SWEEP_ATTACK, step.x, step.y + 1.0, step.z, 1, 0.3, 0.1, 0.3, 0.0);

			Box slashBox = new Box(step.x - 2.2, step.y - 1.0, step.z - 2.2, step.x + 2.2, step.y + 2.5, step.z + 2.2);
			List<LivingEntity> victims = world.getEntitiesByClass(LivingEntity.class, slashBox,
					e -> e != caster && e.isAlive());
			for (LivingEntity victim : victims) {
				victim.damage(world, world.getDamageSources().playerAttack(caster), 22.0f);
				victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1));
			}

			if (world.isAir(net.minecraft.util.math.BlockPos.ofFloored(step))) {
				safeTargetPos = step;
			}
		}

		caster.requestTeleport(safeTargetPos.x, safeTargetPos.y, safeTargetPos.z);
		caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 4));
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, safeTargetPos.x, safeTargetPos.y + 1.0, safeTargetPos.z, 1, 0, 0, 0, 0);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!attacker.getWorld().isClient && attacker.getWorld() instanceof ServerWorld serverWorld) {
			// 평타 적중 시 성흔 폭발 파티클 & 시전자 체력 회복
			serverWorld.spawnParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
			serverWorld.spawnParticles(new DustParticleEffect(GOLD_COLOR, 1.6f),
					target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.6, 0.6, 0.6, 0.05);
			attacker.heal(2.5f);
		}
		return super.postHit(stack, target, attacker);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("하늘의 심판을 대행하는 신화급 대검").formatted(Formatting.GOLD));
		tooltip.add(Text.literal("[우클릭] 단죄의 결계: 이중 마법진 전개 및 8개의 천벌 빛기둥 폭격").formatted(Formatting.RED));
		tooltip.add(Text.literal("[Shift + 우클릭] 차원 단절 베기: 전방 14m 초고속 일섬 돌진").formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.literal("[기본 공격] 성흔 흡수: 타격 시 황금 충격파 및 체력 회복").formatted(Formatting.YELLOW));
	}
}
