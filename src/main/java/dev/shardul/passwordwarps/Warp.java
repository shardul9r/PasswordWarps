package dev.shardul.passwordwarps;

/** One saved warp. Serialised to JSON by Gson. The password is never stored, only salt + SHA-256 hash. */
public class Warp {
    public String name;        // lower-case, [a-z0-9_-]
    public String dimension;   // e.g. "minecraft:overworld", "minecraft:the_nether"
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public String ownerUuid;
    public String ownerName;
    public String salt;        // Base64, 16 random bytes
    public String passwordHash; // Base64 SHA-256(salt || password UTF-8)
    public long createdAt;     // epoch millis

    public Warp() {
    }
}
