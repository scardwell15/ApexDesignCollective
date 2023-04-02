package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.weapons.ApexTachyonShredder.FINAL_COLOR;
import static data.weapons.ApexTachyonShredder.START_COLOR;

public class ApexTachyonShredderOnHit implements OnHitEffectPlugin
{
    private static final float BASE_EXP_RADIUS = 100f;
    private static final float RADIUS_BONUS_MULT = 0.75f; // 175 radius at maximum flux
    private static final float FRAG_FRACTION = 0.5f; // proj damage * this = frag exp damage

    // onhit graphical effect
    private static final Color CORE_EXPLOSION_COLOR = new Color(96,41,246, 255);
    private static final Color OUTER_EXPLOSION_COLOR = new Color(85, 0, 255, 25);
    private static final Color GLOW_COLOR = new Color(109,41,246, 150);
    private static final float CORE_EXP_RADIUS = 100f;
    private static final float CORE_EXP_DUR = 1f;
    private static final float OUTER_EXP_RADIUS = 150f;
    private static final float OUTER_EXP_DUR = 0.2f;
    private static final float GLOW_RADIUS = 200f;
    private static final float GLOW_DUR = 0.2f;
    private static final float VEL_MULT = 2f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (!(target instanceof MissileAPI) && proj.getSource().getFluxLevel() > 0.5f)
        {
            // damaging explosion
            float radius = BASE_EXP_RADIUS + RADIUS_BONUS_MULT * proj.getSource().getFluxLevel();
            DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f,
                    radius,
                    radius * 0.75f,
                    proj.getDamageAmount() * FRAG_FRACTION,
                    proj.getDamageAmount() * FRAG_FRACTION * 0.75f,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f,
                    10f,
                    0.2f,
                    20,
                    Color.WHITE,
                    null);
            spec.setDamageType(DamageType.FRAGMENTATION);
            spec.setShowGraphic(false);
            engine.spawnDamagingExplosion(spec, proj.getSource(), point, false);

            // graphics

            Color color = ApexUtils.blendColors(START_COLOR, FINAL_COLOR, proj.getSource().getFluxLevel());
                    //proj.getProjectileSpec().getFringeColor();
            color = Misc.setAlpha(color, 100);

            Vector2f vel = new Vector2f();
            if (target instanceof ShipAPI)
            {
                vel.set(target.getVelocity());
            }

            float sizeMult = Misc.getHitGlowSize(20f, proj.getDamage().getBaseDamage(), damageResult) / 100f;

            for (int i = 0; i < 7; i++)
            {
                //float size = projectile.getProjectileSpec().getWidth() * (0.75f + (float) Math.random() * 0.5f);
                float size = 10f * (0.75f + (float) Math.random() * 0.5f);

                float dur = 0.25f;
                //dur = 0.25f;
                float rampUp = 0f;
                Color c = Misc.scaleAlpha(color, proj.getBrightness());
                engine.addNebulaParticle(point, vel, size, 5f + 3f * sizeMult,
                        rampUp, 0f, dur, c, true);
            }
            /*
            // blatantly inspired by (and totally not stolen from) the scalartech ruffle
            engine.spawnExplosion(point, Misc.ZERO, CORE_EXPLOSION_COLOR, CORE_EXP_RADIUS, CORE_EXP_DUR / VEL_MULT);
            engine.spawnExplosion(point, Misc.ZERO, OUTER_EXPLOSION_COLOR, OUTER_EXP_RADIUS, OUTER_EXP_DUR / VEL_MULT);
            engine.addHitParticle(point, Misc.ZERO, GLOW_RADIUS, 1f, GLOW_DUR / VEL_MULT, GLOW_COLOR);

            MagicRender.battlespace(
                    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                    point,
                    Misc.ZERO,
                    new Vector2f(80, 80),
                    new Vector2f(240 * VEL_MULT, 240 * VEL_MULT),
                    MathUtils.getRandomNumberInRange(0, 360),
                    0,
                    GLOW_COLOR,
                    true,
                    0.125f / VEL_MULT,
                    0.0f,
                    0.125f / VEL_MULT
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                    point,
                    Misc.ZERO,
                    new Vector2f(120, 120),
                    new Vector2f(100 * VEL_MULT, 100 * VEL_MULT),
                    MathUtils.getRandomNumberInRange(0, 360),
                    0,
                    CORE_EXPLOSION_COLOR,
                    true,
                    0.2f / VEL_MULT,
                    0.0f,
                    0.2f / VEL_MULT
            );*/
        }
    }
}
