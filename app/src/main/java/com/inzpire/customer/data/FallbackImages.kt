package com.inzpire.customer.data

/**
 * Curated stock imagery shown on a design / material card when the admin hasn't
 * uploaded one, so the cards always read as image tiles instead of an empty box or
 * a bare icon. A card's fallback is picked deterministically from its row id, so a
 * given design/material always shows the same image (no flicker between recompositions).
 *
 * These reuse the same Unsplash CDN the rest of the app already loads through Coil;
 * while the image loads (or if the device is offline) [com.inzpire.customer.ui.components.RemoteImage]
 * shows its neutral placeholder, exactly as it does for admin-uploaded images.
 */
object FallbackImages {
    private const val PARAMS = "&q=80&auto=format&fit=crop"

    // Interior renders / room moodboards — used for design cards.
    private val designs = listOf(
        "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=900$PARAMS",
        "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=900$PARAMS",
        "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900$PARAMS",
        "https://images.unsplash.com/photo-1558877385-8c1f51fdbf2c?w=900$PARAMS",
        "https://images.unsplash.com/photo-1503387762-592deb58ef4e?w=900$PARAMS",
        "https://images.unsplash.com/photo-1503387837-b154d5074bd2?w=900$PARAMS",
    )

    // Material / finish swatches — used for material cards.
    private val materials = listOf(
        "https://images.unsplash.com/photo-1604147706283-d7119b5b822c?w=400$PARAMS",
        "https://images.unsplash.com/photo-1615873968403-89e068629265?w=400$PARAMS",
        "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=400$PARAMS",
        "https://images.unsplash.com/photo-1581858726788-75bc0f6a952d?w=400$PARAMS",
        "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400$PARAMS",
        "https://images.unsplash.com/photo-1562184552-997c461cc6f6?w=400$PARAMS",
    )

    /** A stable "nice" image for a design card with no admin-uploaded image. */
    fun forDesign(seed: String): String = designs[pick(seed, designs.size)]

    /** A stable "nice" image for a material card with no admin-uploaded swatch. */
    fun forMaterial(seed: String): String = materials[pick(seed, materials.size)]

    private fun pick(seed: String, size: Int): Int = ((seed.hashCode() % size) + size) % size
}
