package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.ApexUtils.text;

public class ApexQGAmplifier extends BaseHullMod
{
    public static final float QGP_EMP_FRACTION = 0.5f;
    public static final float QGPD_EXTRA_ENERGY = 50f;

    // all of the actual effects are done in ApexQGOnHit

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return text("coamp1");
        if (index == 1)
            return text("coamp2");
        if (index == 2)
            return (int)(QGP_EMP_FRACTION * 100f) + "%";
        if (index == 3)
            return text("coamp3");
        if (index == 4)
            return (int)(QGPD_EXTRA_ENERGY) + " " + text("coamp4");
        return null;
    }
}
