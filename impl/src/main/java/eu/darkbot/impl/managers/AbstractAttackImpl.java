package eu.darkbot.impl.managers;

import eu.darkbot.api.game.items.SelectableItem;
import eu.darkbot.api.game.other.Lockable;
import eu.darkbot.api.managers.AttackAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.HeroItemsAPI;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractAttackImpl implements AttackAPI {

    protected int DELAY_BETWEEN_ATTACK_ATTEMPTS = 750;

    protected final HeroItemsAPI heroItems;
    protected final HeroAPI hero;

    protected Lockable target;
    protected long lockTryTime, attackTryTime;

    protected AbstractAttackImpl(HeroItemsAPI heroItems, HeroAPI hero) {
        this.heroItems = heroItems;
        this.hero = hero;
    }

    @Override
    public @Nullable Lockable getTarget() {
        return target;
    }

    @Override
    public void setTarget(@Nullable Lockable target) {
        this.target = target;
    }

    @Override
    public boolean isLocked() {
        return hasTarget() && hero.getTarget() == target;
    }

    @Override
    public void tryLockTarget() {
        if (isLocked() || lockTryTime > System.currentTimeMillis()) return;

        this.lockTryTime = System.currentTimeMillis() + (this.target.trySelect(false) ? 500 : 250);
    }

    @Override
    public boolean isAttacking() {
        return hasTarget() && hero.isAttacking(target);
    }

    @Override
    public void tryLockAndAttack() {
        if (isLocked()) {
            SelectableItem.Laser laser = getBestLaserAmmo();
            if (laser == null) return;//should only attack if laser ammo is not null?

            if (hero.getLaser() != laser) {
                heroItems.useItem(laser, 500);
                if (isAttackViaSlotBarEnabled())
                    attackTryTime = System.currentTimeMillis();
            }

            attack();
        } else tryLockTarget();
    }

    private void attack() {
        if (hero.isAttacking() || attackTryTime > System.currentTimeMillis() - DELAY_BETWEEN_ATTACK_ATTEMPTS) return;
        if (hero.triggerLaserAttack()) attackTryTime = System.currentTimeMillis();
    }

    @Override
    public void stopAttack() {
        if (!hero.isAttacking() || attackTryTime > System.currentTimeMillis() - DELAY_BETWEEN_ATTACK_ATTEMPTS) return;
        if (hero.triggerLaserAttack()) attackTryTime = System.currentTimeMillis();
    }

    @Override
    public double modifyRadius(double radius) {
        return radius;
    }

    protected abstract boolean isAttackViaSlotBarEnabled();

    protected abstract SelectableItem.Laser getBestLaserAmmo();
}