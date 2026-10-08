package org.mikrowellentoast.NMCL.safezone;

import org.bukkit.Location;

public record SafeZone(String name, String world, SafeZoneType type,
                       double minX, double minY, double minZ,
                       double maxX, double maxY, double maxZ,
                       boolean preventCombat, boolean clearCombatOnEntry, boolean showMessage) {
    public static SafeZone sphere(String name, String world, double x, double y, double z, double radius,
                                  boolean prevent, boolean clear, boolean message) {
        if (radius <= 0) throw new IllegalArgumentException("radius must be positive");
        return new SafeZone(name, world, SafeZoneType.SPHERE, x, y, z, radius, 0, 0, prevent, clear, message);
    }

    public static SafeZone cuboid(String name, String world, double x1, double y1, double z1,
                                  double x2, double y2, double z2, boolean prevent, boolean clear, boolean message) {
        return new SafeZone(name, world, SafeZoneType.CUBOID, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), prevent, clear, message);
    }

    public boolean contains(Location location) {
        return location.getWorld() != null && world.equalsIgnoreCase(location.getWorld().getName())
                && contains(location.getX(), location.getY(), location.getZ());
    }

    public boolean contains(double x, double y, double z) {
        if (type == SafeZoneType.SPHERE) {
            double dx = x - minX, dy = y - minY, dz = z - minZ;
            return dx * dx + dy * dy + dz * dz <= maxX * maxX;
        }
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public double radius() { return type == SafeZoneType.SPHERE ? maxX : 0; }
}
