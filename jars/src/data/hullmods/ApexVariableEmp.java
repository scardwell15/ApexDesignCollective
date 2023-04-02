package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import static data.ApexUtils.text;
import static data.hullmods.ApexVariableWarheads.LEFT_SLOT;
import static data.hullmods.ApexVariableWarheads.RIGHT_SLOT;

public class ApexVariableEmp extends BaseHullMod
{
    /*public static final String SUFFIX = "tachyon";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        ShipVariantAPI variant = stats.getVariant();
        if (variant.getWeaponId(LEFT_SLOT) == null || !variant.getWeaponId(LEFT_SLOT).equals("apex_vls_left_" + SUFFIX))
        {
            variant.clearSlot(LEFT_SLOT);
            variant.clearSlot(RIGHT_SLOT);
            variant.addWeapon(LEFT_SLOT, "apex_vls_left_" + SUFFIX);
            variant.addWeapon(RIGHT_SLOT, "apex_vls_right_" + SUFFIX);
        }
    }*/

    @Override
    public int getDisplaySortOrder()
    {
        return 100;
    }

    @Override
    public int getDisplayCategoryIndex()
    {
        return 3;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return text("vw4");
        if (index == 1)
            return text("vw1");
        if (index == 2)
            return text("vw3");
        return null;
    }
}
