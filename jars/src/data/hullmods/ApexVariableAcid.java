package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import static data.hullmods.ApexVariableWarheads.LEFT_SLOT;
import static data.hullmods.ApexVariableWarheads.RIGHT_SLOT;

public class ApexVariableAcid extends BaseHullMod
{
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
            return "nanoacid";
        if (index == 1)
            return "kinetic";
        if (index == 2)
            return "no CR penalty";
        return null;
    }
}
