package org.mikrowellentoast.NMCL.utils;

import org.bukkit.Location;

public class SafeZone {
    public final String name;
    public final String world;
    public final double x, y, z;
    public final double radius;

    public SafeZone(String name, String world, double x, double y, double z, double radius) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public String getName() { return name; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getRadius() { return radius; }

    public boolean isInSafeZone(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;

        double dx = loc.getX() - x;
        double dz = loc.getZ() - z;

        return (dx * dx + dz * dz) <= (radius * radius);
    }
}
